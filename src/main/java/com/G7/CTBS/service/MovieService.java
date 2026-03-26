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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    
    public MovieDTO findById(@PathVariable Long id) throws EntityNotFoundException {
        try {
            return convertToDTO(movieRepository.findById(id).get());
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("Cannot find movie");
        }
    }
    
    public List<MovieDTO> getPublicMovies(String title, Long categoryId, String language, String status, String sortBy) {
        if (status != null && !status.equals("Now Playing") && !status.equals("Coming Soon")) {
            status = null;
        }
        List<MovieDTO> movies = this.searchAndFilterMovies(title, categoryId, language, status, null, null, sortBy);
        if (status == null) {
            return movies.stream()
                    .filter(m -> "Now Playing".equals(m.getStatus()) || "Coming Soon".equals(m.getStatus()))
                    .toList();
        }
        
        return movies;
    }
    
    public List<MovieDTO> getNowShowingMovies() {
        List<Movie> movies = movieRepository.findByStatus("Now Playing");
        return convertToDTO(movies);
    }
    
    public List<MovieDTO> getComingSoonMovies() {
        List<Movie> movies = movieRepository.findByStatus("Coming Soon");
        return convertToDTO(movies);
    }
    
    public List<MovieDTO> getBannerMovies(int limit) {
        List<Movie> bannerCandidates = new ArrayList<>(movieRepository.findByStatus("Now Playing"));
        
        if (bannerCandidates.size() < limit) {
            bannerCandidates.addAll(movieRepository.findByStatus("Coming Soon"));
        }
        
        Collections.shuffle(bannerCandidates);
        
        List<Movie> result = bannerCandidates.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        return convertToDTO(result);
    }
    
    public List<MovieDTO> searchAndFilterMovies(String title, Long categoryId, String language, String status, LocalDate fromDate, LocalDate toDate, String sortBy) {
        Sort sort;
        if (sortBy == null) sortBy = "idDesc";
        
        sort = switch (sortBy) {
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "title");
            case "nameDesc" -> Sort.by(Sort.Direction.DESC, "title");
            case "releaseDateDesc" -> Sort.by(Sort.Direction.DESC, "releaseDate");
            case "releaseDateAsc" -> Sort.by(Sort.Direction.ASC, "releaseDate");
            case "durationDesc" -> Sort.by(Sort.Direction.DESC, "duration");
            case "durationAsc" -> Sort.by(Sort.Direction.ASC, "duration");
            case "idAsc" -> Sort.by(Sort.Direction.ASC, "movieId");
            default -> Sort.by(Sort.Direction.DESC, "movieId");
        };
        
        if (title != null && title.trim().isEmpty()) title = null;
        if (language != null && language.trim().isEmpty()) language = null;
        if (status != null && status.trim().isEmpty()) status = null;
        
        List<Movie> movies = movieRepository.searchMovies(title, categoryId, language, status, fromDate, toDate, sort);
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
        movie.setLanguage(movieDTO.getLanguage() != null ? movieDTO.getLanguage() : "Unknown");
        
        // --- 1. XỬ LÝ BANNER ---
        if (movieDTO.getBannerFile() != null && !movieDTO.getBannerFile().isEmpty()) {
            movie.setBannerPath(fileStorageService.storeFile(movieDTO.getBannerFile(), "banners", movieDTO.getTitle()));
        } else if (movieDTO.getBannerUrl() != null && !movieDTO.getBannerUrl().isEmpty()) {
            movie.setBannerPath(movieDTO.getBannerUrl());
        }
        
        // --- 2. XỬ LÝ POSTER ---
        if (movieDTO.getPosterFile() != null && !movieDTO.getPosterFile().isEmpty()) {
            movie.setPosterPath(fileStorageService.storeFile(movieDTO.getPosterFile(), "posters", movieDTO.getTitle()));
        } else if (movieDTO.getPosterUrl() != null && !movieDTO.getPosterUrl().isEmpty()) {
            movie.setPosterPath(movieDTO.getPosterUrl());
        }
        
        // --- 3. XỬ LÝ TRAILER ---
        if (movieDTO.getTrailerFile() != null && !movieDTO.getTrailerFile().isEmpty()) {
            movie.setTrailerPath(fileStorageService.storeFile(movieDTO.getTrailerFile(), "trailers", movieDTO.getTitle()));
        } else if (movieDTO.getTrailerPath() != null && !movieDTO.getTrailerPath().trim().isEmpty()) {
            // NẾU ADMIN KHÔNG UP FILE, NHƯNG CÓ LINK YOUTUBE TỪ TMDB -> LƯU LINK YOUTUBE
            movie.setTrailerPath(movieDTO.getTrailerPath());
        }
        
        // --- 4. XỬ LÝ THỂ LOẠI ---
        List<Category> categories = new ArrayList<>();
        if (movieDTO.getCategoryIds() != null && !movieDTO.getCategoryIds().isEmpty()) {
            categories.addAll(categoryRepository.findAllById(movieDTO.getCategoryIds()));
        }
        
        if (movieDTO.getGenres() != null && !movieDTO.getGenres().trim().isEmpty()) {
            String[] genres = movieDTO.getGenres().split(",");
            for (String g : genres) {
                String cleanName = g.trim();
                boolean alreadyAdded = categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(cleanName));
                
                if (!alreadyAdded) {
                    Category cat = categoryRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> {
                                Category newCat = new Category();
                                newCat.setName(cleanName);
                                return categoryRepository.save(newCat);
                            });
                    categories.add(cat);
                }
            }
        }
        movie.setCategories(categories);
        movieRepository.save(movie);
    }
    
    public void updateMovie(MovieDTO movieDTO) {
        Movie movie = movieRepository.findById(movieDTO.getMovieId())
                .orElseThrow(() -> new EntityNotFoundException("Cannot find movie"));
        
        movie.setTitle(movieDTO.getTitle());
        movie.setDescription(movieDTO.getDescription());
        movie.setDuration(movieDTO.getDuration());
        movie.setReleaseDate(movieDTO.getReleaseDate());
        movie.setStatus(movieDTO.getStatus());
        movie.setDirector(movieDTO.getDirector());
        movie.setActors(movieDTO.getActors());
        movie.setRating(movieDTO.getRating() != null ? movieDTO.getRating() : 0.0);
        movie.setLanguage(movieDTO.getLanguage() != null ? movieDTO.getLanguage() : "Unknown");
        
        // --- 1. CẬP NHẬT THỂ LOẠI ---
        List<Category> categories = new ArrayList<>();
        if (movieDTO.getCategoryIds() != null && !movieDTO.getCategoryIds().isEmpty()) {
            categories.addAll(categoryRepository.findAllById(movieDTO.getCategoryIds()));
        }
        
        if (movieDTO.getGenres() != null && !movieDTO.getGenres().trim().isEmpty()) {
            String[] genres = movieDTO.getGenres().split(",");
            for (String g : genres) {
                String cleanName = g.trim();
                boolean alreadyAdded = categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(cleanName));
                
                if (!alreadyAdded) {
                    Category cat = categoryRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> {
                                Category newCat = new Category();
                                newCat.setName(cleanName);
                                return categoryRepository.save(newCat);
                            });
                    categories.add(cat);
                }
            }
        }
        movie.setCategories(categories);
        
        // --- 2. CẬP NHẬT BANNER ---
        if (movieDTO.getBannerFile() != null && !movieDTO.getBannerFile().isEmpty()) {
            if (movie.getBannerPath() != null) fileStorageService.deleteFile(movie.getBannerPath());
            movie.setBannerPath(fileStorageService.storeFile(movieDTO.getBannerFile(), "banners", movieDTO.getTitle()));
        } else if (movieDTO.getBannerUrl() != null && !movieDTO.getBannerUrl().trim().isEmpty()) {
            movie.setBannerPath(movieDTO.getBannerUrl());
        }
        
        // --- 3. CẬP NHẬT POSTER ---
        if (movieDTO.getPosterFile() != null && !movieDTO.getPosterFile().isEmpty()) {
            if (movie.getPosterPath() != null) fileStorageService.deleteFile(movie.getPosterPath());
            movie.setPosterPath(fileStorageService.storeFile(movieDTO.getPosterFile(), "posters", movieDTO.getTitle()));
        } else if (movieDTO.getPosterUrl() != null && !movieDTO.getPosterUrl().trim().isEmpty()) {
            movie.setPosterPath(movieDTO.getPosterUrl());
        }
        
        // --- 4. CẬP NHẬT TRAILER ---
        if (movieDTO.getTrailerFile() != null && !movieDTO.getTrailerFile().isEmpty()) {
            if (movie.getTrailerPath() != null) fileStorageService.deleteFile(movie.getTrailerPath());
            movie.setTrailerPath(fileStorageService.storeFile(movieDTO.getTrailerFile(), "trailers", movieDTO.getTitle()));
        } else if (movieDTO.getTrailerPath() != null && !movieDTO.getTrailerPath().trim().isEmpty()) {
            // NẾU ADMIN ĐỔI SANG LINK YOUTUBE KHÁC (HOẶC VẪN GIỮ LINK YOUTUBE CŨ)
            if (movie.getTrailerPath() != null && !movie.getTrailerPath().equals(movieDTO.getTrailerPath())) {
                fileStorageService.deleteFile(movie.getTrailerPath()); // Hàm này đã an toàn, tự bỏ qua nếu là link Web
            }
            movie.setTrailerPath(movieDTO.getTrailerPath());
        }
        
        movieRepository.save(movie);
    }
    
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("No movies found with id: " + id)
        );
        movie.setStatus("Disabled");
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
                .posterPath(movie.getPosterPath())
                .trailerPath(movie.getTrailerPath())
                .director(movie.getDirector())
                .actors(movie.getActors())
                .rating(movie.getRating())
                .language(movie.getLanguage())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .build();
    }
}