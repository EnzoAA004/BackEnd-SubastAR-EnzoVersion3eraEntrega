package com.subastar.subastar.dto.usuario;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UsuarioDetalle extends UsuarioResumen {
    private String domicilio;
    private String paisOrigen;
    private String dni;
}
