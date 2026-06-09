package com.subastar.subastar.dto.medioPago;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TarjetaCreditoRequest {
    @NotBlank
    @JsonProperty("numero_tarjeta")
    private String numeroTarjeta;

    @NotBlank
    private String titular;

    @NotBlank
    private String vencimiento;

    @NotBlank
    @JsonProperty("codigo_seguridad")
    private String codigoSeguridad;

    @NotBlank
    @JsonProperty("dni_titular")
    private String dniTitular;

    @JsonProperty("es_internacional")
    private boolean esInternacional = false;
}
