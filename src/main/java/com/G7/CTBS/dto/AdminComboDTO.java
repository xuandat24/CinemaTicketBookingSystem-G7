package com.G7.CTBS.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminComboDTO {
    private Long id;

    @NotBlank(message = "Combo name cannot be empty")
    @Size(min = 2, max = 100, message = "Combo name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private Integer price;

    private String image;
}
