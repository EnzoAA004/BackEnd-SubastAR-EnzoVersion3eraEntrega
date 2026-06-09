package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    @Column(name = "identificador")
    private Integer identificador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numeroPais")
    private Pais pais;

    @Column(name = "admitido")
    private String admitido;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "verificador", nullable = false)
    private Integer verificadorId;
}
