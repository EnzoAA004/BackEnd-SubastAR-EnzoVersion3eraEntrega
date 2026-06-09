package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class BienSolicitudResponse {
    @JsonProperty("codigo_solicitud")
    private String codigoSolicitud;
    private String tipo;
    private String estado;
    @JsonProperty("paso_actual")
    private String pasoActual;
    @JsonProperty("datos_completos")
    private boolean datosCompletos;
    @JsonProperty("fotos_cargadas")
    private int fotosCargadas;
    @JsonProperty("minimo_fotos_requeridas")
    private int minimoFotosRequeridas;
    @JsonProperty("maximo_fotos_permitidas")
    private Integer maximoFotosPermitidas;
    @JsonProperty("declaracion_propiedad_aceptada")
    private boolean declaracionPropiedadAceptada;
    @JsonProperty("documentacion_adjunta")
    private boolean documentacionAdjunta;
    @JsonProperty("puede_confirmar")
    private boolean puedeConfirmar;
    private BienDatosResumen bien;
    private List<BienArchivoResumen> fotos;
    private List<BienArchivoResumen> documentos;

    @Data
    public static class BienDatosResumen {
        private String nombre;
        @JsonProperty("descripcion_tecnica")
        private String descripcionTecnica;
        @JsonProperty("cantidad_elementos")
        private Integer cantidadElementos;
        @JsonProperty("epoca_origen")
        private String epocaOrigen;
        @JsonProperty("artista_disenador")
        private String artistaDisenador;
        @JsonProperty("fecha_creacion")
        private String fechaCreacion;
        @JsonProperty("datos_historicos")
        private String datosHistoricos;
        @JsonProperty("informacion_adicional")
        private String informacionAdicional;
    }
}
