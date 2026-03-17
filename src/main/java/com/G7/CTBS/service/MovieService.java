package com.G7.CTBS.service;

import com.G7.CTBS.dto.MovieDTO;
import com.G7.CTBS.entity.Category;
import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.repository.CategoryRepository;
import com.G7.CTBS.repository.MovieRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieService {
    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    
    @Autowired
    public MovieService(MovieRepository movieRepository, CategoryRepository categoryRepository, FileStorageService fileStorageService) {
        this.movieRepository = movieRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
    }
    
    public List<MovieDTO> findAll() {
        try {
            return convertToDTO(movieRepository.findAll());
        } catch (Exception e) {
            throw new RuntimeException("Cannot find movie");
        }
    }
    
    public MovieDTO findById(@PathVariable Long id) throws EntityNotFoundException {
        try{
            return convertToDTO(movieRepository.findById(id).get());
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("Cannot find movie");
        }
    }
    
    public List<MovieDTO> getMoviesByStatus(String status) {
        List<Movie> movies = movieRepository.findByStatus(status);
        
        if(movies.isEmpty()) {
            throw new EntityNotFoundException("No movies found");
        }
        return movies.stream().map(this::convertToDTO).toList();
    }
    
    public List<MovieDTO> getMoviesByTitle(String title) {
        if(title == null || title.trim().isEmpty()) return new ArrayList<>();
        List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(title);
        
        if(movies.isEmpty()) {
            throw new EntityNotFoundException("No movies found");
        }
        return movies.stream().map(this::convertToDTO).toList();
    }
    
    public List<MovieDTO> getMoviesByCategory(String category) {
        if(category == null || category.trim().isEmpty()) return new ArrayList<>();
        List<Movie> movies = movieRepository.findByCategories_NameContainingIgnoreCase(category);
        
        if(movies.isEmpty()) {
            System.out.println("No movies found");
            return new ArrayList<>();
        }
        
        return movies.stream().map(this::convertToDTO).toList();
    }
    
    public MovieDTO getMovieById(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new EntityNotFoundException("No movies found");
        }
        Movie movie = movieRepository.findById(id).get();
        return convertToDTO(movie);
    }
    
    public List<MovieDTO> searchAndFilterMovies(String title, Long categoryId, LocalDate fromDate, LocalDate toDate, String sortBy) {
        Sort sort;
        
        // Cấu hình các tiêu chí sắp xếp
        if ("name".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "title"); // Theo tên A-Z
        } else if ("releaseDateDesc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "releaseDate"); // Ngày ra mắt mới nhất
        } else if ("releaseDateAsc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "releaseDate"); // Ngày ra mắt cũ nhất
        } else {
            // Mặc định cho "id" -> Phim mới thêm vào sẽ lên đầu
            sort = Sort.by(Sort.Direction.DESC, "movieId");
        }
        
        if (title != null && title.trim().isEmpty()) {
            title = null;
        }
        
        // Gọi Repository
        List<Movie> movies = movieRepository.searchMovies(title, categoryId, fromDate, toDate, sort);
        return movies.stream().map(this::convertToDTO).toList();
    }
    
    public void createMovie(MovieDTO movieDTO) {
        if(movieRepository.existsByTitleIgnoreCase(movieDTO.getTitle())) {
            throw new EntityExistsException("Movie already exists with title: " + movieDTO.getTitle());
        }
        
        Movie movie = new Movie();
        movie.setTitle(movieDTO.getTitle());
        movie.setDescription(movieDTO.getDescription());
        movie.setDuration(movieDTO.getDuration());
        movie.setReleaseDate(movieDTO.getReleaseDate());
        movie.setStatus(movieDTO.getStatus());
        movie.setDirector(movieDTO.getDirector());
        movie.setActors(movieDTO.getActors());
        movie.setRating(movieDTO.getRating() != null ? movieDTO.getRating() : 0.0);
        
        if (movieDTO.getBannerFile() != null && !movieDTO.getBannerFile().isEmpty()) {
            movie.setBannerPath(fileStorageService.storeFile(movieDTO.getBannerFile(), "banners", movie.getTitle()));
        } else if (movieDTO.getOmdbPosterUrl() != null && !movieDTO.getOmdbPosterUrl().isEmpty()) {
            movie.setBannerPath(fileStorageService.storeFileFromUrl(movieDTO.getOmdbPosterUrl(), "banners", movie.getTitle()));
        }
        
        if (movieDTO.getTrailerFile() != null && !movieDTO.getTrailerFile().isEmpty()) {
            movie.setTrailerPath(fileStorageService.storeFile(movieDTO.getTrailerFile(), "trailers", movie.getTitle()));
        }
        
        // Xử lý Thể loại (Lấy từ JS gửi lên OMDb, tách ra và lưu)
        List<Category> categories = new ArrayList<>();
        
        // Bước 1: Lấy danh sách các tag mà Admin đã tự chọn tay trên giao diện
        if (movieDTO.getCategoryIds() != null && !movieDTO.getCategoryIds().isEmpty()) {
            categories.addAll(categoryRepository.findAllById(movieDTO.getCategoryIds()));
        }
        
        // Bước 2: Quét qua chuỗi thể loại của OMDb (Ví dụ: "Action, Sci-Fi")
        if (movieDTO.getOmdbGenres() != null && !movieDTO.getOmdbGenres().trim().isEmpty()) {
            String[] genres = movieDTO.getOmdbGenres().split(",");
            
            for (String g : genres) {
                String cleanName = g.trim();
                
                // Kiểm tra xem thể loại OMDb này Admin đã lỡ chọn tay ở Bước 1 chưa (Tránh trùng lặp)
                boolean alreadyAdded = categories.stream()
                        .anyMatch(c -> c.getName().equalsIgnoreCase(cleanName));
                
                if (!alreadyAdded) {
                    // Nếu chưa có, tìm trong DB. Nếu DB chưa có luôn thì tạo mới.
                    Category cat = categoryRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> {
                                Category newCat = new Category();
                                newCat.setName(cleanName);
                                return categoryRepository.save(newCat);
                            });
                    // Thêm vào danh sách chung
                    categories.add(cat);
                }
            }
        }
        
        movie.setCategories(categories);
        movieRepository.save(movie);
    }
    
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("No movies found with id: " + id)
        );
        movie.setStatus("Disabled");
        movieRepository.save(movie);
    }
    
    public void updateMovie(MovieDTO movieDTO) {
        Movie movie = movieRepository.findById(movieDTO.getMovieId()).orElse(null);
        if (movie == null) {
            throw new EntityNotFoundException("There are some error when finding movie with id: " + movieDTO.getMovieId());
        }
        
        if (movieRepository.existsByTitleIgnoreCaseAndMovieIdNot(movieDTO.getTitle(), movieDTO.getMovieId())) {
            throw new EntityExistsException("Update failed: Another movie already exists with title '" + movieDTO.getTitle() + "'");
        }
        
        List<Category> categories = categoryRepository.findAllById(movieDTO.getCategoryIds());
        if (categories.isEmpty()) {
            throw new EntityNotFoundException("None of the provided categories were found");
        }
        
        if (movieDTO.getBannerFile() != null && !movieDTO.getBannerFile().isEmpty()) {
            fileStorageService.deleteFile(movie.getBannerPath());
            String bannerUrl = fileStorageService.storeFile(movieDTO.getBannerFile(), "banners", movieDTO.getTitle());
            movie.setBannerPath(bannerUrl);
        }
        
        if (movieDTO.getTrailerFile() != null && !movieDTO.getTrailerFile().isEmpty()) {
            fileStorageService.deleteFile(movie.getTrailerPath());
            String trailerUrl = fileStorageService.storeFile(movieDTO.getTrailerFile(), "trailers", movieDTO.getTitle());
            movie.setTrailerPath(trailerUrl);
        }
        
        movie.setTitle(movieDTO.getTitle());
        movie.setDescription(movieDTO.getDescription());
        movie.setDuration(movieDTO.getDuration());
        movie.setReleaseDate(movieDTO.getReleaseDate());
        movie.setStatus(movieDTO.getStatus());
        movie.setCategories(categories);
        
        // BỔ SUNG 3 TRƯỜNG NÀY ĐỂ LƯU DỮ LIỆU CHỈNH SỬA XUỐNG DB
        movie.setDirector(movieDTO.getDirector());
        movie.setActors(movieDTO.getActors());
        movie.setRating(movieDTO.getRating());
        
        movieRepository.save(movie);
    }
    
    private List<MovieDTO> convertToDTO(List<Movie> movies) {
        return movies.stream().map(this::convertToDTO).toList();
    }
    
    private MovieDTO convertToDTO(Movie movie) {
        List<Long> categoryIds = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        
        if (movie.getCategories() != null && !movie.getCategories().isEmpty()) {
            categoryIds = movie.getCategories().stream()
                    .map(Category::getCategoryId)
                    .toList();
            
            categoryNames = movie.getCategories().stream()
                    .map(Category::getName)
                    .toList();
        }
        
        return MovieDTO.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .bannerPath(movie.getBannerPath())
                .trailerPath(movie.getTrailerPath())
                .director(movie.getDirector())
                .actors(movie.getActors())
                .rating(movie.getRating())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .build();
    }
}
