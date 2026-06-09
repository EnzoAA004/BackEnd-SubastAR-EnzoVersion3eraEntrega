package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bien_solicitudes")
public class BienSolicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "codigo_solicitud", nullable = false, unique = true)
    private String codigoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "paso_actual", nullable = false)
    private String pasoActual;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion_tecnica")
    private String descripcionTecnica;

    @Column(name = "cantidad_elementos")
    private Integer cantidadElementos;

    @Column(name = "epoca_origen")
    private String epocaOrigen;

    @Column(name = "artista_disenador")
    private String artistaDisenador;

    @Column(name = "fecha_creacion_obra")
    private LocalDate fechaCreacionObra;

    @Column(name = "datos_historicos")
    private String datosHistoricos;

    @Column(name = "informacion_adicional")
    private String informacionAdicional;

    @Column(name = "declara_propiedad", nullable = false)
    private boolean declaraPropiedad;

    @Column(name = "producto_id")
    private Integer productoId;

    @Column(name = "cuenta_cobro_banco")
    private String cuentaCobroBanco;

    @Column(name = "cuenta_cobro_pais")
    private String cuentaCobroPais;

    @Column(name = "cuenta_cobro_cbu_iban")
    private String cuentaCobroCbuIban;

    @Column(name = "precio_base_sugerido")
    private BigDecimal precioBaseSugerido;

    @Column(name = "divisa_precio_base_sugerido")
    private String divisaPrecioBaseSugerido;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
        if (estado == null) estado = "iniciada";
        if (pasoActual == null) pasoActual = "datos";
    }
}
