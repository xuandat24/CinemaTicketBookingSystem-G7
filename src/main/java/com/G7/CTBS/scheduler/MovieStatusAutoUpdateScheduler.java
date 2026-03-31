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
        
        // 1. Pending -> Now Playing (Đến giờ chiếu)
        List<Movie> moviesToPlay = movieRepository.findMoviesToPlay(now);
        if (!moviesToPlay.isEmpty()) {
            for (Movie movie : moviesToPlay) {
                movie.setStatus("Now Playing");
                System.out.println("Movie " + movie.getTitle() + " changed to new status: Now Playing");
            }
            changeStatus = true;
        }
        
        // 2. Now Playing -> Pending (Đã chiếu xong, không có suất nào đang diễn ra)
        List<Movie> moviesToPending = movieRepository.findMoviesToPending(now);
        if (!moviesToPending.isEmpty()) {
            for (Movie movie : moviesToPending) {
                movie.setStatus("Pending");
                System.out.println("Movie " + movie.getTitle() + " changed to new status: Pending");
            }
            changeStatus = true;
        }
        
        // 3. Chỉ lưu xuống DB nếu có sự thay đổi
        if (changeStatus) {
            movieRepository.saveAll(moviesToPlay);
            movieRepository.saveAll(moviesToPending);
        }
    }
}