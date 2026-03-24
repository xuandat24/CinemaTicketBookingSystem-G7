package com.G7.CTBS.scheduler;

import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Showtime;
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

    //auto update for each 60s
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdate() {
        LocalDateTime now = LocalDateTime.now();
        List<Movie> moviesToUpdateStatus = movieRepository.findMoviesToUpdateStatus(now);
        boolean changeStatus = false;

        //if there is some movie meet the requirements, then check to update it
        if(!moviesToUpdateStatus.isEmpty()) {
            for(Movie movie : moviesToUpdateStatus) {
                movie.setStatus("Now Playing");
                System.out.println("Movie " + movie.getTitle() + " changed to new status: Now Playing");
                changeStatus = true;
            }
        }

        // 1. Lấy TẤT CẢ các phim đang chiếu lên
        List<Movie> nowPlayingMovies = movieRepository.findByStatus("Now Playing");

        for (Movie movie : nowPlayingMovies) {
            boolean hasFutureOrOngoingShowtime = false;

            // 2. Duyệt qua từng suất chiếu của bộ phim này
            for (Showtime showtime : movie.getShowtimes()) {
                // Tính thời gian kết thúc chính xác = Thời gian bắt đầu + thời lượng phim (phút)
                LocalDateTime exactEndTime = showtime.getStartTime().plusMinutes(movie.getDuration());

                // Nếu thời gian kết thúc của suất chiếu này vẫn lớn hơn hoặc bằng hiện tại
                // Nghĩa là suất chiếu này vẫn đang diễn ra, hoặc chưa diễn ra
                if (exactEndTime.isAfter(now) || exactEndTime.isEqual(now)) {
                    hasFutureOrOngoingShowtime = true;
                    break; // Chỉ cần 1 suất chiếu hợp lệ là đủ, thoát vòng lặp ngay
                }
            }

            // 3. Nếu không có bất kỳ suất chiếu nào đang/sắp diễn ra -> Đưa về Pending
            if (!hasFutureOrOngoingShowtime) {
                movie.setStatus("Pending");
                System.out.println("Movie "  + movie.getTitle() + " changed to new status: Pending");
                changeStatus = true;
            }
        }

        if(changeStatus) {
            movieRepository.saveAll(moviesToUpdateStatus);
            movieRepository.saveAll(nowPlayingMovies);
        }
    }


}