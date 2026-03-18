package com.G7.CTBS.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDTO {
    private Long movieId;
    private String title;
    private String bannerPath;
    private String trailerPath;
    private String description;
    private Integer duration;
    private LocalDate releaseDate;
    private String status;
    private List<Long> categoryIds; // Danh sách các ID thể loại phim
}
