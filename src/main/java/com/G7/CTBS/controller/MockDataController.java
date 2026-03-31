package com.G7.CTBS.controller;

import com.G7.CTBS.entity.*;
import com.G7.CTBS.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/mock-data")
@RequiredArgsConstructor
public class MockDataController {
    
    //http://localhost:8080/api/admin/mock-data/clear
    //http://localhost:8080/api/admin/mock-data/generate
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ComboRepository comboRepository;
    
    // API 1: XÓA SẠCH DỮ LIỆU GIẢ CŨ (CHỮA LỖI TRÀN GHẾ)
    @GetMapping("/clear")
    @Transactional
    public ResponseEntity<String> clearMockData() {
        // Tìm và xóa tất cả các booking có mã bắt đầu bằng "MOCK-"
        List<Booking> mockBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingCode() != null && b.getBookingCode().startsWith("MOCK-"))
                .collect(Collectors.toList());
        
        bookingRepository.deleteAll(mockBookings);
        return ResponseEntity.ok("Thành công! Đã dọn dẹp sạch sẽ " + mockBookings.size() + " đơn đặt vé giả bị lỗi trong Database.");
    }
    
    // API 2: TẠO DỮ LIỆU GIẢ CHUẨN XÁC MỚI
    @GetMapping("/generate")
    @Transactional
    public ResponseEntity<String> generateMockData() {
        List<User> users = userRepository.findAll();
        List<Showtime> showtimes = showtimeRepository.findAll();
        List<Seat> allSeats = seatRepository.findAll();
        List<Combo> combos = comboRepository.findAll();
        
        if (users.isEmpty() || showtimes.isEmpty() || allSeats.isEmpty()) {
            return ResponseEntity.badRequest().body("Lỗi: Thiếu dữ liệu cơ bản (User, Showtime, Seat) trong DB.");
        }
        
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        int totalBookingsCreated = 0;
        
        // BẢN VÁ LỖI: Tạo Map để theo dõi các ghế chưa ai mua của TỪNG suất chiếu
        Map<Long, List<Seat>> availableSeatsPerShowtime = new HashMap<>();
        
        int[] weightedHours = {8, 10, 12, 12, 14, 14, 16, 16, 18, 18, 18, 20, 20, 20, 22};
        
        for (int i = 30; i >= 0; i--) {
            LocalDateTime currentDay = now.minusDays(i);
            int bookingsPerDay = 10 + random.nextInt(21); // Random 10-30 đơn/ngày
            
            for (int j = 0; j < bookingsPerDay; j++) {
                // Random 1 suất chiếu
                Showtime st = showtimes.get(random.nextInt(showtimes.size()));
                
                // Lấy danh sách ghế của đúng phòng đó và lọc những ghế CHƯA AI MUA
                List<Seat> availableSeats = availableSeatsPerShowtime.computeIfAbsent(st.getShowtimeId(), id -> {
                    return allSeats.stream()
                            .filter(s -> s.getRoom().getRoomId().equals(st.getTheaterRoom().getRoomId())) // Chỉ lấy ghế đúng phòng
                            .collect(Collectors.toList()); // Copy ra list mới
                });
                
                // Nếu suất chiếu này đã bán sạch ghế (Full 144 ghế) thì bỏ qua
                if (availableSeats.isEmpty()) {
                    continue;
                }
                
                Booking booking = new Booking();
                booking.setStatus("SUCCESS");
                booking.setBookingCode("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                
                int hour = weightedHours[random.nextInt(weightedHours.length)];
                int minute = random.nextInt(60);
                booking.setCreateTime(currentDay.withHour(hour).withMinute(minute).withSecond(0));
                
                booking.setUser(users.get(random.nextInt(users.size())));
                booking.setShowtime(st);
                
                double finalPrice = 0.0;
                
                // Chọn số vé muốn mua (1 đến 4 vé), nhưng không vượt quá số ghế còn lại
                int numSeatsToBook = Math.min(1 + random.nextInt(4), availableSeats.size());
                List<BookingSeat> bookingSeats = new ArrayList<>();
                
                for (int k = 0; k < numSeatsToBook; k++) {
                    // Lấy ngẫu nhiên 1 ghế và XÓA khỏi danh sách ghế trống của suất chiếu này
                    Seat randomSeat = availableSeats.remove(random.nextInt(availableSeats.size()));
                    
                    BookingSeat bs = new BookingSeat();
                    bs.setBooking(booking);
                    bs.setSeat(randomSeat);
                    
                    double seatPrice = 60000.0 * (randomSeat.getPriceFactor() != null ? randomSeat.getPriceFactor() : 1.0);
                    finalPrice += seatPrice;
                    
                    bookingSeats.add(bs);
                }
                booking.setBookingSeats(bookingSeats);
                
                // Add Combo (giữ nguyên logic cũ)
                List<BookingCombo> bookingCombos = new ArrayList<>();
                if (!combos.isEmpty() && random.nextInt(100) < 60) {
                    int numCombos = 1 + random.nextInt(2);
                    for (int k = 0; k < numCombos; k++) {
                        Combo randomCombo = combos.get(random.nextInt(combos.size()));
                        BookingCombo bc = new BookingCombo();
                        bc.setBooking(booking);
                        bc.setCombo(randomCombo);
                        int qty = 1 + random.nextInt(2);
                        bc.setQuantity(qty);
                        finalPrice += (randomCombo.getPrice() * qty);
                        bookingCombos.add(bc);
                    }
                }
                booking.setBookingCombos(bookingCombos);
                
                booking.setFinalPrice(finalPrice);
                bookingRepository.save(booking);
                totalBookingsCreated++;
            }
        }
        
        return ResponseEntity.ok("Thành công! Đã tạo " + totalBookingsCreated + " đơn đặt vé chuẩn xác (đã fix lỗi tràn ghế).");
    }
}