package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BienDatosRequest {
    @NotBlank
    private String tipo;

    @NotBlank
    private String nombre;

    @NotBlank
    @JsonProperty("descripcion_tecnica")
    private String descripcionTecnica;

    @NotNull @Min(1)
    @JsonProperty("cantidad_elementos")
    private Integer cantidadElementos;

    @JsonProperty("epoca_origen")
    private String epocaOrigen;

    @JsonProperty("artista_disenador")
    private String artistaDisenador;

    @JsonProperty("fecha_creacion")
    private LocalDate fechaCreacion;

    @JsonProperty("datos_historicos")
    private String datosHistoricos;

    @JsonProperty("informacion_adicional")
    private String informacionAdicional;

    @JsonProperty("cuenta_cobro_banco")
    private String cuentaCobroBanco;

    @JsonProperty("cuenta_cobro_pais")
    private String cuentaCobroPais;

    @JsonProperty("cuenta_cobro_cbu_iban")
    private String cuentaCobroCbuIban;

    @JsonProperty("precio_base_sugerido")
    private BigDecimal precioBaseSugerido;

    @JsonProperty("divisa_precio_base_sugerido")
    private String divisaPrecioBaseSugerido;
}
