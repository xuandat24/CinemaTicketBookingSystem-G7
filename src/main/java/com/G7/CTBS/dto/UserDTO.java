package com.G7.CTBS.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    // Các trường do hệ thống tự sinh/quản lý, thường không bắt Validate ép người dùng nhập
    private Long userId;
    private Long roleId;

    // ĐÃ THÊM: Chặn số, chặn ký tự đặc biệt và giới hạn 50 ký tự chống tràn Database
    @NotBlank(message = "First Name cannot be empty")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "First Name can only contain letters!")
    @Size(max = 50, message = "First Name must not exceed 50 characters")
    private String firstName;

    // ĐÃ THÊM: Chặn số, chặn ký tự đặc biệt và giới hạn 50 ký tự
    @NotBlank(message = "Last Name cannot be empty")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Last Name can only contain letters!")
    @Size(max = 50, message = "Last Name must not exceed 50 characters")
    private String lastName;

    private String userName;

    // Tốt: Bạn đã khóa chặt định dạng sđt 10 số đầu 03/09
    @NotBlank(message = "Phone Number cannot be empty")
    @Pattern(regexp = "^(03|09)\\d{8}$", message = "Phone numbers must have 10 digits and start with 03 or 09.")
    private String phone;

    // ĐÃ THÊM: Ràng buộc chuẩn định dạng Email
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Gender cannot be empty")
    private String gender;

    @NotNull(message = "Date Of Birth cannot be empty")
    @Past(message = "Date Of Birth must be in the past")
    private LocalDate dob;

    // Ngày tạo tài khoản thường chỉ để hiển thị, không validate
    private LocalDateTime createdAt;

    private String avatar; // Đường dẫn ảnh
}