package com.G7.CTBS.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDTO {
    
    private Long movieId;
    
    @NotBlank(message = "Movie title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;
    
    private String bannerPath;
    private String trailerPath;
    
    private MultipartFile bannerFile;
    private MultipartFile trailerFile;
    
    private String omdbPosterUrl;
    private String omdbGenres;
    
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description is too long")
    private String description;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;
    
    @NotNull(message = "Release date is required")
    private LocalDate releaseDate;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotBlank(message = "Director is required")
    private String director;
    
    @NotBlank(message = "Actors are required")
    private String actors;
    
    @NotNull(message = "Rating is required")
    @Min(value = 0, message = "Rating must be at least 0")
    @Max(value = 10, message = "Rating cannot exceed 10")
    private Double rating;
    
    @NotBlank(message = "Language is required")
    private String language;
    
    private List<Long> categoryIds;
    private List<String> categoryNames;
}