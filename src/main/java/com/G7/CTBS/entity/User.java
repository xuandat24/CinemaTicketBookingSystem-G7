package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleId")
    private Role role;

    @Column(nullable = false,columnDefinition = "NVARCHAR(50)", length = 50)
    private String firstName;

    @Column(nullable = false,columnDefinition = "NVARCHAR(50)", length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 50)
    private String userName;

    @Column(nullable = false, length = 250)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20,columnDefinition = "NVARCHAR(10)")
    private String gender; // Ví dụ: "Nam", "Nữ", "Khác"

    @Column
    private LocalDate dob;

    @Column(name = "provider", length = 20)
    private String provider;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<Booking> bookings;

    public String getFullName() {
        return lastName + " " + firstName;
    }
}
