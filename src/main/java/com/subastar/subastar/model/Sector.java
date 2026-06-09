package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sectores")
public class Sector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "nombreSector", nullable = false)
    private String nombreSector;

    @Column(name = "codigoSector")
    private String codigoSector;

    @Column(name = "responsableSector")
    private Integer responsableSectorId;
}
