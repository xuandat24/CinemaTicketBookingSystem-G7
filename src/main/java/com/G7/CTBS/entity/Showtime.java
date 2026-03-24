package com.G7.CTBS.entity;

import com.G7.CTBS.enums.ShowtimeFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "showtime")
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
}