package com.G7.CTBS.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutheticationResponse {
    String token;
    String userName;
    String role; // Add the missing role field
    boolean authenticated; // This field might be for future use
}
