package com.subastar.subastar.dto.puja;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PujaResumen {
    private Integer id;
    @JsonProperty("usuario_id")
    private Integer usuarioId;
    @JsonProperty("nombre_usuario")
    private String nombreUsuario;
    private BigDecimal monto;
    private LocalDateTime timestamp;
    @JsonProperty("es_ganadora")
    private boolean esGanadora;
}
