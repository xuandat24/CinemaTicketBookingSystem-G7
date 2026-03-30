package com.G7.CTBS.entity;

import com.G7.CTBS.enums.ShowtimeFormat;
import com.G7.CTBS.enums.ShowtimeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "showtimes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long showtimeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movieId")
    private Movie movie;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId")
    private TheaterRoom theaterRoom;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double basePrice;
    
    @Enumerated(EnumType.STRING)
    private ShowtimeFormat format;
    
    @OneToMany(mappedBy = "showtime")
    private List<Booking> bookings;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowtimeStatus status;
    
    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = ShowtimeStatus.ACTIVE;
        }
    }
}