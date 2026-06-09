package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cheques_certificados")
public class ChequeCertificado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "medio_pago_id", nullable = false, unique = true)
    private Integer medioPagoId;

    @Column(name = "banco_emisor", nullable = false)
    private String bancoEmisor;

    @Column(name = "monto_certificado", nullable = false)
    private BigDecimal montoCertificado;

    @Column(name = "numero_cheque", nullable = false)
    private String numeroCheque;

    @Lob
    @Column(name = "foto_cheque")
    private byte[] fotoCheque;
}
