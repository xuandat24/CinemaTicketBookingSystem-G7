package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "combos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private int price;

    private String image;
}