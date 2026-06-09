package com.subastar.subastar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReenviarCodigoRequest {
    @NotBlank
    @Email
    private String email;
}
