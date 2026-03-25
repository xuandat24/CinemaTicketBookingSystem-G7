package com.G7.CTBS.repository;

import com.G7.CTBS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByuserName(String userName);
    Optional<User> findByUserNameOrEmail(String userName, String email);

    boolean existsByuserName(String userName);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByPhone(String phone);
    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.userName = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmailWithRole(@Param("identifier") String identifier);


}