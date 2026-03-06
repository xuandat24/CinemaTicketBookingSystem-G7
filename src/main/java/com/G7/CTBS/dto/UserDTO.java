package com.G7.CTBS.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long userId;
    private Long roleId; // Chỉ lấy ID của Role
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
}
