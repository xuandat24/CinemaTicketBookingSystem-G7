package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "PermissionCode", nullable = false, unique = true, length = 50)
    private String permissionCode;

    @Column(name = "Description", length = 255)
    private String description;
}
