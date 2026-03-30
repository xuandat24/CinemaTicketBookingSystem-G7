package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingSeatId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId")
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtimeId")
    private Showtime showtime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seatId")
    private Seat seat;
}
