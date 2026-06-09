package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pujos_extra")
public class PujoExtra {
    @Id
    @Column(name = "pujo_id")
    private Integer pujoId;

    @Column(name = "timestamp_puja", nullable = false)
    private LocalDateTime timestampPuja;

    @Column(name = "medio_pago_id")
    private Integer medioPagoId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pujo_id", insertable = false, updatable = false)
    private Pujo pujo;

    @PrePersist
    public void prePersist() {
        if (timestampPuja == null) timestampPuja = LocalDateTime.now();
    }
}
