package com.G7.CTBS.entity;

import com.G7.CTBS.util.TextUtils;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    private String name;

    @ManyToMany(mappedBy = "categories")
    private List<Movie> movies;

    @PrePersist
    public void prePersist() {
        this.name = TextUtils.formatTitleCase(this.name);
    }

    @PreUpdate
    public void preUpdate() {
        this.name = TextUtils.formatTitleCase(this.name);
    }
}