package com.G7.CTBS.controller;

import com.G7.CTBS.entity.Feedback;
import com.G7.CTBS.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/public/feedbacks")
@RequiredArgsConstructor
public class PublicFeedbackController {

    private final FeedbackRepository feedbackRepository;

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Feedback request) {
        request.setCreatedAt(LocalDateTime.now());
        request.setRead(false);
        feedbackRepository.save(request);

        return ResponseEntity.ok(Map.of("message", "Thank you! Your message has been sent to the administration."));
    }
}