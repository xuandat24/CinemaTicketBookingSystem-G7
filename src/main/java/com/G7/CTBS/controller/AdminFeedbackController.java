package com.G7.CTBS.controller;

import com.G7.CTBS.entity.Feedback;
import com.G7.CTBS.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackRepository feedbackRepository;

    // Lấy toàn bộ phản hồi
    @GetMapping
    public ResponseEntity<?> getAllFeedbacks() {
        return ResponseEntity.ok(feedbackRepository.findAllByOrderByCreatedAtDesc());
    }

    // Đánh dấu là đã đọc
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Optional<Feedback> fbOpt = feedbackRepository.findById(id);
        if (fbOpt.isPresent()) {
            Feedback fb = fbOpt.get();
            fb.setRead(true);
            feedbackRepository.save(fb);
        }
        return ResponseEntity.ok(Map.of("message", "Marked as read."));
    }

    // Xóa phản hồi rác
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeedback(@PathVariable Long id) {
        if (feedbackRepository.existsById(id)) {
            feedbackRepository.deleteById(id);
        }
        return ResponseEntity.ok(Map.of("message", "Feedback deleted."));
    }
}