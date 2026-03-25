package com.G7.CTBS.controller;

import com.G7.CTBS.entity.*;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.service.ComboService;
import com.G7.CTBS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.service.BookingService;

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

    @GetMapping("/booking")
    public String bookingFromSeatPage(
            @RequestParam(value = "showtimeId", required = false) Long showtimeId,
            @RequestParam(value = "seatIds", required = false) String seatIdsParam,
            @RequestParam(value = "seatCodes", required = false) String seatCodesParam,
            @RequestParam(value = "username", required = false) String username,
            Model model) {

        // USER
        User user = resolveCurrentUser(username);
        model.addAttribute("user", user);

        // MOVIE + SHOWTIME
        Showtime showtime = showtimeId != null
                ? showtimeRepository.findById(showtimeId).orElse(null)
                : null;

        Movie movie = showtime != null ? showtime.getMovie() : null;
        String showDate = (showtime != null && showtime.getStartTime() != null)
                ? showtime.getStartTime().format(DATE_FORMATTER)
                : "-";
        String showTime = (showtime != null && showtime.getStartTime() != null)
                ? showtime.getStartTime().format(TIME_FORMATTER)
                : "-";

        String movieName = movie != null ? movie.getTitle() : "Selected Movie";
        String poster = (movie != null && movie.getBannerPath() != null)
                ? movie.getBannerPath()
                : "/img/1.jpg";

        // PARSE SEAT IDS
        List<Long> seatIds = parseCsv(seatIdsParam).stream()
                .map(this::toLongOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Seat> selectedSeats = seatIds.isEmpty()
                ? new ArrayList<>()
                : seatRepository.findAllById(seatIds);

        // ================== GROUP + SUMMARY ==================
        Map<String, List<Seat>> groupedSeats = selectedSeats.stream()
                .collect(Collectors.groupingBy(Seat::getSeatType));

        List<Map<String, Object>> seatSummary = new ArrayList<>();

        double basePrice = showtime != null ? showtime.getBasePrice() : 0;
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

        // ================== SEAT DISPLAY ==================
        Map<String, List<String>> grouped = selectedSeats.stream()
                .collect(Collectors.groupingBy(
                        Seat::getSeatType,
                        Collectors.mapping(Seat::getSeatCode, Collectors.toList())
                ));

        String seatDisplay = grouped.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("\n"));

        // ================== MODEL ==================
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

        return "booking";
    }

    // ================= SUPPORT =================

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
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

    private User resolveCurrentUser(String usernameParam) {
        if (usernameParam != null && !usernameParam.isBlank()) {
            User user = userService.findByUsernameOrEmail(usernameParam);
            if (user != null) return user;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        String name = auth.getName();
        if (name != null && !"anonymousUser".equalsIgnoreCase(name)) {
            User user = userService.findByUsernameOrEmail(name);
            if (user != null) return user;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User oauth) {
            String email = oauth.getAttribute("email");
            if (email != null) {
                return userService.findByUsernameOrEmail(email);
            }
        }

        return null;
    }

    @PostMapping("/api/booking/confirm")
    @ResponseBody
    public Map<String, Object> confirmBooking(@RequestBody BookingRequestDTO request) {
        try {
            Long bId = bookingService.createBooking(request);
            return Map.of("success", true, "bookingId", bId);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/booking/history")
    public String history(@RequestParam(value = "username", required = false) String username,
                          Model model) {

        if (username == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(username);

        model.addAttribute("user", user);

        // TODO: lấy list booking theo user
        return "user/booking_history";
    }
}