package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroStep1Request {
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank @Email
    private String email;
    @NotBlank
    private String domicilio;
    @NotBlank
    @JsonProperty("pais_origen")
    private String paisOrigen;
}
