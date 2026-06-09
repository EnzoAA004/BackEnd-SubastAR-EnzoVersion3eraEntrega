package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "compras_extra")
public class CompraExtra {
    @Id
    @Column(name = "registro_id")
    private Integer registroId;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(name = "estado_pago", nullable = false)
    private String estadoPago;

    @Column(name = "estado_entrega", nullable = false)
    private String estadoEntrega;

    @Column(name = "medio_pago_id")
    private Integer medioPagoId;

    @Column(name = "costo_envio")
    private BigDecimal costoEnvio;

    @Column(name = "direccion_entrega")
    private String direccionEntrega;

    @Column(name = "factura_path")
    private String facturaPath;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_id", insertable = false, updatable = false)
    private RegistroDeSubasta registroDeSubasta;

    @PrePersist
    public void prePersist() {
        if (fechaCompra == null) fechaCompra = LocalDateTime.now();
        if (estadoPago == null) estadoPago = "pendiente";
        if (estadoEntrega == null) estadoEntrega = "coordinando";
    }
}
