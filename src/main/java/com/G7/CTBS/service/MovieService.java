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
    
    public MovieDTO findById(@PathVariable Long id) throws EntityNotFoundException {
        try{
            return convertToDTO(movieRepository.findById(id).get());
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("Cannot find movie");
        }
    }
    
    public List<MovieDTO> getPublicMovies(String title, Long categoryId, String language, String status, String sortBy) {
        String backendSortBy = "id";
        if ("newest".equalsIgnoreCase(sortBy)) {
            backendSortBy = "releaseDateDesc";
        } else if ("oldest".equalsIgnoreCase(sortBy)) {
            backendSortBy = "releaseDateAsc";
        }
        
        if (status != null && !status.equals("Now Playing") && !status.equals("Coming Soon")) {
            status = null;
        }
        
        List<MovieDTO> movies = this.searchAndFilterMovies(title, categoryId, language, status, null, null, backendSortBy);
        
        if (status == null) {
            return movies.stream()
                    .filter(m -> "Now Playing".equals(m.getStatus()) || "Coming Soon".equals(m.getStatus()))
                    .toList();
        }
        
        return movies;
    }
    
    public List<MovieDTO> searchAndFilterMovies(String title, Long categoryId, String language, String status, LocalDate fromDate, LocalDate toDate, String sortBy) {
        Sort sort;
        if ("name".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "title");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "movieId");
        }
        
        if (title != null && title.trim().isEmpty()) {
            title = null;
        }
        if (language != null && language.trim().isEmpty()) {
            language = null;
        }
        if (status != null && status.trim().isEmpty()) {
            status = null;
        }
        
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
        
        if (movieDTO.getBannerFile() != null && !movieDTO.getBannerFile().isEmpty()) {
            movie.setBannerPath(fileStorageService.storeFile(movieDTO.getBannerFile(), "banners", movie.getTitle()));
        } else if (movieDTO.getOmdbPosterUrl() != null && !movieDTO.getOmdbPosterUrl().isEmpty()) {
            movie.setBannerPath(fileStorageService.storeFileFromUrl(movieDTO.getOmdbPosterUrl(), "banners", movie.getTitle()));
        }
        
        if (movieDTO.getTrailerFile() != null && !movieDTO.getTrailerFile().isEmpty()) {
            movie.setTrailerPath(fileStorageService.storeFile(movieDTO.getTrailerFile(), "trailers", movie.getTitle()));
        }
        
        List<Category> categories = new ArrayList<>();
        
        if (movieDTO.getCategoryIds() != null && !movieDTO.getCategoryIds().isEmpty()) {
            categories.addAll(categoryRepository.findAllById(movieDTO.getCategoryIds()));
        }
        
        if (movieDTO.getOmdbGenres() != null && !movieDTO.getOmdbGenres().trim().isEmpty()) {
            String[] genres = movieDTO.getOmdbGenres().split(",");
            
            for (String g : genres) {
                String cleanName = g.trim();
                
                boolean alreadyAdded = categories.stream()
                        .anyMatch(c -> c.getName().equalsIgnoreCase(cleanName));
                
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
        movie.setLanguage(movieDTO.getLanguage());
        
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
                .language(movie.getLanguage())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .build();
    }
}
