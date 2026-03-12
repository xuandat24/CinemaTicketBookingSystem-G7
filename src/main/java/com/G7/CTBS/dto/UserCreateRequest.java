package com.G7.CTBS.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Sửa file UserCreateRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequest {

    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$", message = "Hệ thống chỉ chấp nhận tài khoản @gmail.com")
    String email;

    @NotBlank(message = "First Name không được để trống") // Sửa lại message cho đúng trường
    String firstName;

    @NotBlank(message = "Last Name không được để trống")
    String lastName;

    @NotBlank(message = "Tên người dùng không được để trống")
    @Size(min = 6, message = "Tên người dùng phải có ít nhất 6 ký tự")
    String userName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
            message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 kí tự đặc biệt"
    )
    String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có đúng 10 chữ số")
    String phone; // THÊM TRƯỜNG NÀY ĐỂ HẾT LỖI 'getPhone' TRONG USER SERVICE

    Long roleId; // Đổi từ Integer sang Long để khớp với Role Entity
}