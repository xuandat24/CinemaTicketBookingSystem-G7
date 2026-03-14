package com.G7.CTBS.entity;

import com.G7.CTBS.util.TextUtils;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movieId;
    private String title;
    private String bannerPath;
    private String trailerPath;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Integer duration; // Phút
    private LocalDate releaseDate;
    private String status; //Comming soon: chưa có suất chiếu, Pending: đang chờ chiếu, Now Playing: đang chiếu, Disabled: xoá mềm
    private String director;
    private String actors;
    private Double rating;
    
    @ManyToMany
    @JoinTable(
            name = "movie_category",
            joinColumns = @JoinColumn(name = "movieId"),
            inverseJoinColumns = @JoinColumn(name = "categoryId")
    )
    private List<Category> categories;
    
    @OneToMany(mappedBy = "movie")
    private List<Showtime> showtimes;
    
    @PrePersist
    public void prePersist() {
        this.title = TextUtils.formatTitleCase(this.title);
    }
    
    @PreUpdate
    public void preUpdate() {
        this.title = TextUtils.formatTitleCase(this.title);
    }
}
