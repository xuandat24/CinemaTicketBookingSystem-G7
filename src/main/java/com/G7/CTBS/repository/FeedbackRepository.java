package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // Sắp xếp tin nhắn mới nhất lên đầu
    List<Feedback> findAllByOrderByCreatedAtDesc();
}