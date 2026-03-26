package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtimeId")
    private Showtime showtime;

    private String bookingCode;
    private Double finalPrice;
    private String status; // VD: PENDING, CONFIRMED, CANCELLED
    private LocalDateTime createTime;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingSeat> bookingSeats;

    @OneToMany(mappedBy = "booking")
    private List<Payment> payments;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingCombo> bookingCombos;
}