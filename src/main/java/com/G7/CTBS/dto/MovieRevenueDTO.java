package com.G7.CTBS.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRevenueDTO {
    private String title;
    private Double revenue;
}