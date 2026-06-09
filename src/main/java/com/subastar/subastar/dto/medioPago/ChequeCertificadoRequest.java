package com.subastar.subastar.dto.medioPago;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChequeCertificadoRequest {
    @NotBlank
    @JsonProperty("banco_emisor")
    private String bancoEmisor;

    @NotNull @Positive
    @JsonProperty("monto_certificado")
    private BigDecimal montoCertificado;

    @NotBlank
    @JsonProperty("numero_cheque")
    private String numeroCheque;
}
