package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_combos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "combo_id")
    private Combo combo;

    private int quantity;
}