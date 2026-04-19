package com.G7.CTBS.controller;

import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.*;
import com.G7.CTBS.repository.CouponRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.service.BookingService;
import com.G7.CTBS.service.ComboService;
import com.G7.CTBS.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BookingController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ComboService comboService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CouponRepository couponRepository;

    @GetMapping("/booking")
    public String bookingFromSeatPage(
            @RequestParam(value = "showtimeId", required = false) Long showtimeId,
            @RequestParam(value = "seatIds", required = false) String seatIdsParam,
            @RequestParam(value = "seatCodes", required = false) String seatCodesParam,
            @RequestParam(value = "username", required = false) String username,
            HttpServletRequest request, // BỔ SUNG REQUEST ĐỂ ĐỌC COOKIE
            Model model) {

        if (showtimeId == null || seatIdsParam == null || seatIdsParam.isBlank()) {
            return "redirect:/";
        }

        // TRUYỀN THÊM REQUEST VÀO HÀM NHẬN DIỆN
        User user = resolveCurrentUser(request, username);
        model.addAttribute("user", user);

        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (showtime == null || (showtime.getStartTime() != null && showtime.getStartTime().isBefore(java.time.LocalDateTime.now()))) {
            return "redirect:/?error=ShowtimeExpired";
        }

        Movie movie = showtime.getMovie();
        String showDate = (showtime.getStartTime() != null) ? showtime.getStartTime().format(DATE_FORMATTER) : "-";
        String showTime = (showtime.getStartTime() != null) ? showtime.getStartTime().format(TIME_FORMATTER) : "-";
        String roomName = (showtime.getTheaterRoom() != null && showtime.getTheaterRoom().getRoomName() != null)
                ? showtime.getTheaterRoom().getRoomName() : "Room --";

        String movieName = movie != null ? movie.getTitle() : "Selected Movie";
        String poster = (movie != null && movie.getBannerPath() != null) ? movie.getBannerPath() : "/img/1.jpg";

        List<Long> seatIds = parseCsv(seatIdsParam).stream()
                .map(this::toLongOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Seat> selectedSeats = seatIds.isEmpty() ? new ArrayList<>() : seatRepository.findAllById(seatIds);

        Map<String, List<Seat>> groupedSeats = selectedSeats.stream()
                .collect(Collectors.groupingBy(Seat::getSeatType));

        List<Map<String, Object>> seatSummary = new ArrayList<>();
        double basePrice = showtime.getBasePrice();
        double totalPrice = 0;

        for (String type : groupedSeats.keySet()) {
            List<Seat> seatsByType = groupedSeats.get(type);
            int count = seatsByType.size();
            double pricePerSeat = seatsByType.get(0).getPriceFactor() * basePrice;
            double total = count * pricePerSeat;
            totalPrice += total;

            Map<String, Object> item = new HashMap<>();
            item.put("type", type);
            item.put("count", count);
            item.put("price", pricePerSeat);
            item.put("total", total);
            seatSummary.add(item);
        }

        String seatDisplay;
        if (!selectedSeats.isEmpty()) {
            Map<String, List<String>> grouped = selectedSeats.stream()
                    .collect(Collectors.groupingBy(Seat::getSeatType, Collectors.mapping(Seat::getSeatCode, Collectors.toList())));
            seatDisplay = grouped.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                    .collect(Collectors.joining("\n"));
        } else {
            seatDisplay = seatCodesParam == null || seatCodesParam.isBlank() ? "Not selected" : seatCodesParam;
        }

        List<Booking> userBookings = (user != null) ? bookingService.findByUser(user) : new ArrayList<>();
        Set<String> usedCouponCodes = userBookings.stream()
                .filter(b -> "SUCCESS".equalsIgnoreCase(b.getStatus()) || "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .map(Booking::getCouponCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Coupon> availableCoupons = couponRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .filter(c -> c.getExpiryDate() == null || !c.getExpiryDate().isBefore(java.time.LocalDate.now()))
                .filter(c -> c.getMaxUsage() == null || (c.getUsedCount() != null && c.getUsedCount() < c.getMaxUsage()))
                .filter(c -> !usedCouponCodes.contains(c.getCode()))
                .collect(Collectors.toList());

        model.addAttribute("availableCoupons", availableCoupons);
        model.addAttribute("movieName", movieName);
        model.addAttribute("poster", poster);
        model.addAttribute("seatSummary", seatSummary);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("seatDisplay", seatDisplay);
        model.addAttribute("combos", comboService.getAllCombos());
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("seatIdsList", seatIds);
        model.addAttribute("showDate", showDate);
        model.addAttribute("showTime", showTime);
        model.addAttribute("roomName", roomName);

        return "booking";
    }

    @GetMapping("/movie-seats")
    public String showMovieSeatsPage(
            @RequestParam(value = "showtimeId", required = false) Long showtimeId,
            @RequestParam(value = "roomName", required = false) String roomName,
            @RequestParam(value = "username", required = false) String username,
            HttpServletRequest request, // BỔ SUNG REQUEST
            Model model) {

        if (showtimeId == null) {
            return "redirect:/";
        }

        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("roomName", roomName);

        User user = resolveCurrentUser(request, username);
        if (user != null) {
            model.addAttribute("user", user);
        }

        return "movie-seats";
    }

    @PostMapping("/api/booking/confirm")
    @ResponseBody
    public Map<String, Object> confirmBooking(@RequestBody BookingRequestDTO requestDto, HttpServletRequest request) {
        try {
            User user = resolveCurrentUser(request, null);
            if (user == null) {
                return Map.of("success", false, "message", "User not logged in or invalid token.");
            }

            requestDto.setUserId(user.getUserId());
            Long bookingId = bookingService.createBooking(requestDto);
            return Map.of("success", true, "bookingId", bookingId);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/booking/history")
    public String history(HttpServletRequest request, Model model) {
        // TRUYỀN THÊM REQUEST ĐỂ ĐỌC ĐƯỢC COOKIE KHI VNPAY REDIRECT VỀ
        User user = resolveCurrentUser(request, null);
        if (user == null) {
            return "redirect:/login";
        }

        List<Booking> history = bookingService.findByUser(user);
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        model.addAttribute("currentPage", "my-tickets");
        return "user/booking_history";
    }

    @GetMapping("/my-ticket")
    public String myTicketsPage(HttpServletRequest request, Model model) {
        // ĐÃ DỌN DẸP CÁC THAM SỐ THỪA VÀ TRUYỀN REQUEST VÀO
        User user = resolveCurrentUser(request, null);
        if (user == null) {
            return "redirect:/login";
        }

        List<Booking> myTickets = bookingService.findByUser(user).stream()
                .filter(b -> "SUCCESS".equalsIgnoreCase(b.getStatus()) || "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .sorted((b1, b2) -> {
                    if (b1.getCreateTime() == null && b2.getCreateTime() == null) return 0;
                    if (b1.getCreateTime() == null) return 1;
                    if (b2.getCreateTime() == null) return -1;
                    return b2.getCreateTime().compareTo(b1.getCreateTime());
                }).toList();

        model.addAttribute("user", user);
        model.addAttribute("myTickets", myTickets);
        model.addAttribute("currentPage", "my-ticket");
        return "user/my-ticket";
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Long toLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    // ==============================================================
    // TRÁI TIM CỦA VIỆC ĐỒNG BỘ: NHẬN DIỆN BẰNG COOKIE CHỐNG VNPAY
    // ==============================================================
    private User resolveCurrentUser(HttpServletRequest request, String usernameParam) {
        // 1. Kiểm tra tham số trên URL
        if (usernameParam != null && !usernameParam.isBlank()) {
            User user = userService.findByUsernameOrEmail(usernameParam);
            if (user != null) {
                return user;
            }
        }

        // 2. Kiểm tra Security Context (Local Login)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            User user = userService.findByUsernameOrEmail(auth.getName());
            if (user != null) return user;
        }

        // 3. Kiểm tra OAuth2 (Google Login)
        Object principal = auth != null ? auth.getPrincipal() : null;
        if (principal instanceof OAuth2User oauth) {
            String email = oauth.getAttribute("email");
            if (email != null) return userService.findByUsernameOrEmail(email);
        }

        // 4. LỚP BẢO VỆ CUỐI CÙNG: ĐỌC TỪ COOKIE TRÌNH DUYỆT
        // Giúp nhận diện User khi VNPay đá về mà mất sạch JWT Token
        if (request != null && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("username".equals(cookie.getName())) {
                    try {
                        String decoded = java.net.URLDecoder.decode(cookie.getValue(), java.nio.charset.StandardCharsets.UTF_8.name());
                        User user = userService.findByUsernameOrEmail(decoded);
                        if (user != null) return user;
                    } catch (Exception e) {}
                }
            }
        }

        return null;
    }
}