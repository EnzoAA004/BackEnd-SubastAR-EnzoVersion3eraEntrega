package com.subastar.subastar.dto.compra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class CompraDetalle extends CompraResumen {
    @JsonProperty("medio_pago")
    private String medioPago;
    @JsonProperty("costo_envio")
    private BigDecimal costoEnvio;
    private BigDecimal total;
    @JsonProperty("direccion_entrega")
    private String direccionEntrega;
    @JsonProperty("factura_url")
    private String facturaUrl;
}
