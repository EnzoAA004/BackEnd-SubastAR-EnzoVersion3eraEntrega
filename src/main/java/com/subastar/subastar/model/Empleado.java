package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "empleados")
public class Empleado {
    @Id
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "cargo")
    private String cargo;

    @Column(name = "sector")
    private Integer sectorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;
}
