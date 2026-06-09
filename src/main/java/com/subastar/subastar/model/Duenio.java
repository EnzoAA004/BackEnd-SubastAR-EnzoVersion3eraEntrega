package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "duenios")
public class Duenio {
    @Id
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "numeroPais")
    private Integer numeroPaisId;

    @Column(name = "verificacionFinanciera")
    private String verificacionFinanciera;

    @Column(name = "verificacionJudicial")
    private String verificacionJudicial;

    @Column(name = "calificacionRiesgo")
    private Integer calificacionRiesgo;

    @Column(name = "verificador", nullable = false)
    private Integer verificadorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;
}
