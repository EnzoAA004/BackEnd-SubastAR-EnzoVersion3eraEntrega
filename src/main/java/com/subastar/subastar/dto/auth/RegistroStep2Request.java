package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroStep2Request {
    @NotBlank
    @JsonProperty("token_verificacion")
    private String tokenVerificacion;

    @NotBlank
    @Pattern(
            regexp = "^(?![0-9])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número, un carácter especial y no puede comenzar con un número"
    )
    private String password;

    @NotBlank
    @JsonProperty("password_confirmacion")
    private String passwordConfirmacion;
}
