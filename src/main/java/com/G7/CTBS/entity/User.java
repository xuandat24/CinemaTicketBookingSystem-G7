package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleId")
    private Role role;
    
    private String firstName;
    private String lastName;
    private String password;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "user")
    private List<Booking> bookings;
}
