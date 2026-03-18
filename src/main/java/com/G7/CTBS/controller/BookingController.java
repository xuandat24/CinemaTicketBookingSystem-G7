package com.G7.CTBS.controller;

import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.entity.User;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    @Autowired
    private ComboService comboService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @GetMapping("/booking")
    public String bookingFromSeatPage(
            @RequestParam(value = "showtimeId", required = false) Long showtimeId,
            @RequestParam(value = "seatIds", required = false) String seatIdsParam,
            @RequestParam(value = "seatCodes", required = false) String seatCodesParam,
            @RequestParam(value = "username", required = false) String username,
            Model model) {

        User user = resolveCurrentUser(username);
        model.addAttribute("user", user);

        Showtime showtime = showtimeId != null ? showtimeRepository.findById(showtimeId).orElse(null) : null;
        Movie movie = showtime != null ? showtime.getMovie() : null;
        String movieName = movie != null && movie.getTitle() != null ? movie.getTitle() : "Selected Movie";
        String poster = movie != null && movie.getBannerPath() != null && !movie.getBannerPath().isBlank()
                ? movie.getBannerPath()
                : "/img/1.jpg";

        List<String> seatCodes = parseCsv(seatCodesParam);
        List<Long> seatIds = parseCsv(seatIdsParam).stream()
                .map(this::toLongOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (showtime != null && showtime.getRoom() != null && !seatCodes.isEmpty()) {
            Map<String, Long> roomSeatCodeToId = seatRepository.findByRoom_RoomId(showtime.getRoom().getRoomId())
                    .stream()
                    .collect(Collectors.toMap(
                            seat -> seat.getSeatCode().toUpperCase(),
                            Seat::getSeatId,
                            (first, ignored) -> first,
                            LinkedHashMap::new
                    ));

            List<Long> resolvedSeatIds = seatCodes.stream()
                    .map(code -> roomSeatCodeToId.get(code.toUpperCase()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!resolvedSeatIds.isEmpty()) {
                seatIds = resolvedSeatIds;
            }
        }

        String selectedSeatCodes = seatCodes.isEmpty() ? "Not selected" : String.join(", ", seatCodes);
        String selectedSeatIdsCsv = seatIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        model.addAttribute("movieName", movieName);
        model.addAttribute("poster", poster);
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("selectedSeatCodes", selectedSeatCodes);
        model.addAttribute("selectedSeatIdsCsv", selectedSeatIdsCsv);
        model.addAttribute("combos", comboService.getAllCombos());

        return "booking";
    }

    @GetMapping("/booking/{id}")
    public String bookingLegacy(
            @PathVariable("id") Long id,
            @RequestParam(value = "username", required = false) String username,
            Model model) {
        return bookingFromSeatPage(id, null, null, username, model);
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.toList());
    }

    private Long toLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private User resolveCurrentUser(String usernameParam) {
        if (usernameParam != null && !usernameParam.isBlank()) {
            User user = userService.findByUsernameOrEmail(usernameParam);
            if (user != null) {
                return user;
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principalName = authentication.getName();
        if (principalName != null && !"anonymousUser".equalsIgnoreCase(principalName)) {
            User user = userService.findByUsernameOrEmail(principalName);
            if (user != null) {
                return user;
            }
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                return userService.findByUsernameOrEmail(email);
            }
        }

        return null;
    }
}
