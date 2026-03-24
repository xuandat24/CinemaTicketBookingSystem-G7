package com.G7.CTBS.repository;

import com.G7.CTBS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByuserName(String userName);
    Optional<User> findByUserNameOrEmail(String userName, String email);

    boolean existsByuserName(String userName);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByPhone(String phone);
    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
}