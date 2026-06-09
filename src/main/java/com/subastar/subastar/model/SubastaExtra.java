package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "subastas_extra")
public class SubastaExtra {
    @Id
    @Column(name = "subasta_id")
    private Integer subastaId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "moneda", nullable = false)
    private String moneda;

    @Column(name = "url_streaming")
    private String urlStreaming;

    @Column(name = "item_actual_id")
    private Integer itemActualId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subasta_id", insertable = false, updatable = false)
    private Subasta subasta;

    @PrePersist
    public void prePersist() {
        if (moneda == null) moneda = "ARS";
    }
}
