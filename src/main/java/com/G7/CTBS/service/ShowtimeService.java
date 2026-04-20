package com.G7.CTBS.service;

import com.G7.CTBS.enums.ShowtimeStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import com.G7.CTBS.dto.*;

@Service
public interface ShowtimeService {
    ShowtimeResponse createShowtime(CreateShowtimeRequest request);
    ShowtimeResponse updateShowtime(Long showtimeId, UpdateShowtimeRequest request);
    ShowtimeResponse getShowtimeById(Long showtimeId);
    List<ShowtimeResponse> getShowtimeByMovie(Long movieId);
    List<ShowtimeResponse> getShowtimeByRoom(Long roomId);
    List<ShowtimeResponse> getShowtimeByDate(LocalDate date);
    List<ShowtimeResponse> getAllShowtime();
    List<ShowtimeResponse> getAvailableShowtime(Long movieId, LocalDate date);
    List<ShowtimeResponse> searchShowtime(
            Long movieId,
            Long roomId,
            LocalDate date,
            ShowtimeStatus status,
            String keyword
    );
}