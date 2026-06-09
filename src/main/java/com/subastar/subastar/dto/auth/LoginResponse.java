package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subastar.subastar.dto.usuario.UsuarioResumen;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    private UsuarioResumen usuario;
}
