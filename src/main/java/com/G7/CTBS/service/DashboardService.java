package com.G7.CTBS.service;

import com.G7.CTBS.dto.ComboStatDTO;
import com.G7.CTBS.dto.DashboardResponseDTO;
import com.G7.CTBS.dto.MovieRevenueDTO;
import com.G7.CTBS.dto.ShowtimeOccupancyDTO;
import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.repository.BookingComboRepository;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private BookingRepository bookingRepository;
    private BookingComboRepository bookingComboRepository;
    private SeatRepository seatRepository;
    private ShowtimeRepository showtimeRepository;
    
    @Autowired
    public DashboardService(BookingRepository bookingRepository, BookingComboRepository bookingComboRepository, SeatRepository seatRepository, ShowtimeRepository showtimeRepository){
        this.bookingRepository = bookingRepository;
        this.bookingComboRepository = bookingComboRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }
    
    public DashboardResponseDTO getDashboardData(String filter) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = getStartDate(filter, now);
        LocalDateTime end = now;
        
        // 1. Dữ liệu thẻ tóm tắt (KPIs)
        Double rev = bookingRepository.getTotalRevenue(start, end);
        double totalRevenue = rev != null ? rev : 0.0;
        
        Long tkts = bookingRepository.getTotalTickets(start, end);
        int totalTickets = tkts != null ? tkts.intValue() : 0;
        
        Long cbos = bookingComboRepository.getTotalCombos(start, end);
        int totalCombos = cbos != null ? cbos.intValue() : 0;
        
        List<MovieRevenueDTO> topMoviesDb = bookingRepository.getTopMoviesByRevenue(start, end);
        String topMovieName = topMoviesDb.isEmpty() ? "No data available" : topMoviesDb.get(0).getTitle();
        // 2. Dữ liệu Biểu đồ Cột (Top Movies)
        List<String> movieLabels = new ArrayList<>();
        List<Double> movieData = new ArrayList<>();
        for (int i = 0; i < Math.min(5, topMoviesDb.size()); i++) { // Lấy top 5
            movieLabels.add(topMoviesDb.get(i).getTitle());
            movieData.add(topMoviesDb.get(i).getRevenue() / 1000000.0); // Đổi ra đơn vị Triệu VNĐ
        }
        
        // 3. Dữ liệu Biểu đồ Tròn (Combos)
        List<ComboStatDTO> comboStats = bookingComboRepository.getComboStats(start, end);
        List<String> comboLabels = new ArrayList<>();
        List<Double> comboData = new ArrayList<>();
        for (ComboStatDTO c : comboStats) {
            comboLabels.add(c.getName());
            comboData.add(c.getQuantity() != null ? c.getQuantity().doubleValue() : 0.0);
        }
        
        // 4. Dữ liệu Biểu đồ Đường (Peak Hours - Nhóm theo từng giờ trong 24h)
        List<Booking> bookings = bookingRepository.findByStatusInAndCreateTimeBetween(List.of("SUCCESS", "CONFIRMED"), start, end);
        Map<Integer, Integer> hourMap = new HashMap<>();
        
        for (int i = 0; i <= 23; i++) {
            hourMap.put(i, 0);
        }
        
        for (Booking b : bookings) {
            int h = b.getCreateTime().getHour();
            int seatCount = (b.getBookingSeats() != null) ? b.getBookingSeats().size() : 0;
            if (hourMap.containsKey(h)) {
                hourMap.put(h, hourMap.get(h) + seatCount);
            }
        }
        
        List<String> hourLabels = new ArrayList<>();
        List<Double> hourData = new ArrayList<>();
        for (int i = 0; i <= 23; i++) {
            hourLabels.add(String.format("%02d:00", i));
            hourData.add(Double.valueOf(hourMap.get(i)));
        }
        
        // Đóng gói trả về
        DashboardResponseDTO res = new DashboardResponseDTO();
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        res.setTotalRevenue(format.format(totalRevenue) + " VND");
        res.setTotalTickets(totalTickets);
        res.setTotalCombos(totalCombos);
        res.setTopMovieName(topMovieName);
        
        DashboardResponseDTO.ChartData cMovies = new DashboardResponseDTO.ChartData();
        cMovies.setLabels(movieLabels); cMovies.setData(movieData);
        res.setTopMovies(cMovies);
        
        DashboardResponseDTO.ChartData cCombos = new DashboardResponseDTO.ChartData();
        cCombos.setLabels(comboLabels); cCombos.setData(comboData);
        res.setCombos(cCombos);
        
        DashboardResponseDTO.ChartData cPeak = new DashboardResponseDTO.ChartData();
        cPeak.setLabels(hourLabels); cPeak.setData(hourData);
        res.setPeakHours(cPeak);
        
        return res;
    }
    
    private LocalDateTime getStartDate(String filter, LocalDateTime now) {
        if ("week".equalsIgnoreCase(filter)) {
            return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
        } else if ("month".equalsIgnoreCase(filter)) {
            return now.withDayOfMonth(1).with(LocalTime.MIN);
        }
        // Default là "day" (Hôm nay)
        return now.with(LocalTime.MIN);
    }
    
    public List<ShowtimeOccupancyDTO> getOccupancyData(String filter, String keyword) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = getStartDate(filter, now);
        LocalDateTime end = now.with(LocalTime.MAX);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<Showtime> showtimes = showtimeRepository.findShowtimesForOccupancy(start, end, searchKeyword);
        List<ShowtimeOccupancyDTO> result = new ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM");
        
        for (Showtime s : showtimes) {
            Long total = seatRepository.countSeatsByRoom(s.getTheaterRoom().getRoomId());
            if (total == null || total == 0) continue;
            
            Long booked = bookingRepository.countBookedSeatsByShowtime(s.getShowtimeId());
            if (booked == null) booked = 0L;
            
            // Tính %
            double rate = (booked.doubleValue() / total.doubleValue()) * 100;
            
            ShowtimeOccupancyDTO dto = new ShowtimeOccupancyDTO();
            dto.setMovieName(s.getMovie().getTitle());
            dto.setRoomName(s.getTheaterRoom().getRoomName());
            dto.setTime(s.getStartTime().format(dtf));
            dto.setTotalSeats(total.intValue());
            dto.setBookedSeats(booked.intValue());
            dto.setOccupancyRate(Math.round(rate * 10.0) / 10.0); // Làm tròn 1 chữ số thập phân
            
            result.add(dto);
        }
        
        // Sắp xếp theo Tỷ lệ lấp đầy Giảm dần, và chỉ lấy Top 10 suất nóng nhất
        return result.stream()
                .sorted(Comparator.comparingDouble(ShowtimeOccupancyDTO::getOccupancyRate).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}
