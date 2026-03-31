package com.G7.CTBS.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardResponseDTO {
    private String totalRevenue;
    private Integer totalTickets;
    private String topMovieName;
    private Integer totalCombos;
    
    private ChartData topMovies;
    private ChartData combos;
    private ChartData peakHours;
    
    @Data
    public static class ChartData {
        private List<String> labels;
        private List<Double> data;
    }
}