package com.G7.CTBS.service;

import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingCleanupTaskService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VNPayService vnPayService;

    // Chạy ngầm tự động mỗi 1 phút (60000 milliseconds)
    @Scheduled(fixedRate = 60000)
    public void cleanupPhantomBookings() {
        // Lấy mốc thời gian: 15 phút trước so với hiện tại
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);

        // Truy vấn tất cả các đơn hàng đang bị treo PENDING quá 15 phút
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreateTimeBefore("PENDING", fifteenMinutesAgo);

        if (!expiredBookings.isEmpty()) {
            System.out.println(">> CRON JOB: Tìm thấy " + expiredBookings.size() + " đơn hàng ma. Đang tiến hành dọn dẹp và nhả ghế...");

            for (Booking booking : expiredBookings) {
                try {
                    // Gọi hàm xóa đơn -> Hàm này tự động kích hoạt logic Nhả ghế và Hoàn Coupon ở VNPayService
                    vnPayService.deleteBookingWithPayments(booking);
                } catch (Exception e) {
                    System.err.println("Lỗi khi dọn dẹp đơn hàng " + booking.getBookingId() + ": " + e.getMessage());
                }
            }
        }
    }
}
