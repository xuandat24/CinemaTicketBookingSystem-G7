package com.G7.CTBS.scheduler;

import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.repository.MovieRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MovieStatusAutoUpdateScheduler {
    private final MovieRepository movieRepository;

    @Autowired
    public MovieStatusAutoUpdateScheduler(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // auto update for each 60s
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdate() {
        LocalDateTime now = LocalDateTime.now();
        boolean changeStatus = false;

        // 1. Pending -> Now Playing
        List<Movie> moviesToPlay = movieRepository.findMoviesToPlay(now);
        if (!moviesToPlay.isEmpty()) {
            for (Movie movie : moviesToPlay) {
                movie.setStatus("Now Playing");
            }
            changeStatus = true;
        }

        // 2. Now Playing -> Pending
        List<Movie> moviesToPending = movieRepository.findMoviesToPending(now);
        if (!moviesToPending.isEmpty()) {
            for (Movie movie : moviesToPending) {
                movie.setStatus("Pending");
            }
            changeStatus = true;
        }

        // 3. Save only when there is a change
        if (changeStatus) {
            movieRepository.saveAll(moviesToPlay);
            movieRepository.saveAll(moviesToPending);
        }
    }
}
