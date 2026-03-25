package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId")
    private TheaterRoom room;

    private String seatCode; // VD: A1, A2
    private String seatType; // VD: VIP, Thường, Đôi
    private Double priceFactor; // Hệ số giá (vd: VIP = 1.5)
}