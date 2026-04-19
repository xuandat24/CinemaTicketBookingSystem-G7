package com.G7.CTBS.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

// Sửa file UserCreateRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequest {

    @NotBlank(message = "Email cannot be empty")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$", message = "Hệ thống chỉ chấp nhận tài khoản @gmail.com")
    String email;

    @NotBlank(message = "First Name cannot be empty") // Sửa lại message cho đúng trường
    String firstName;

    @NotBlank(message = "Last Name cannot be empty")
    String lastName;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 6, message = "Username must be at least 6 character long")
    String userName;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must contain at least 8 character long")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
            message = "Password must contain at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character"
    )
    String password;

    private String confirmPassword; // Chỉ dùng để validate, không lưu vào DB
    private String gender;
    private LocalDate dob;

    String provider;

    @NotBlank(message = "Phone Number cannot be empty")
    @Pattern(regexp = "^(03|09)\\d{8}$", message = "Phone Number must be 10 digits and start with 03 or 09")
    String phone;


    Long roleId; // Đổi từ Integer sang Long để khớp với Role Entity
}