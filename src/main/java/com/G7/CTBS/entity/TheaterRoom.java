package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "theater_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheaterRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    private String roomName;
    private Integer totalSeats;

    @OneToMany(mappedBy = "room")
    private List<Seat> seats;

    @OneToMany(mappedBy = "theaterRoom")
    private List<Showtime> showtimes;
}