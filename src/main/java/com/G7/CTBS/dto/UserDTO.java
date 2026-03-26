package com.G7.CTBS.dto;

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

    @NotBlank(message = "First Name không được để trống")
    private String firstName;

    @NotBlank(message = "Last Name không được để trống")
    private String lastName;


    private String userName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(03|09)\\d{8}$", message = "Phone numbers must have 10 digits and start with 03 or 09.")
    private String phone;


    private String email;

    @NotBlank(message = "Giới tính không được để trống")
    private String gender;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dob;

    // Ngày tạo tài khoản thường chỉ để hiển thị, không validate
    private LocalDateTime createdAt;
}