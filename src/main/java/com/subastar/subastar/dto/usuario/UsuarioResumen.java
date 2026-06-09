package com.subastar.subastar.dto.usuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UsuarioResumen {
    private Integer id;
    private String nombre;
    private String apellido;
    private String email;
    private String categoria;
    private String estado;
}
