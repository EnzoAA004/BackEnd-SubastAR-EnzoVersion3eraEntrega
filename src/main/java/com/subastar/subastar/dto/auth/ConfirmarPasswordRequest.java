package com.subastar.subastar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmarPasswordRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6)
    private String nuevaPassword;
}
