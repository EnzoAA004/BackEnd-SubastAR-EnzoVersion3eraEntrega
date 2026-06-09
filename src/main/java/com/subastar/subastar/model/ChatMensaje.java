package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_mensajes")
public class ChatMensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "emisor", nullable = false)
    private String emisor;

    @Column(name = "contenido", nullable = false)
    private String contenido;

    @Column(name = "timestamp_msg", nullable = false)
    private LocalDateTime timestampMsg;

    @Column(name = "leido", nullable = false)
    private boolean leido;

    @PrePersist
    public void prePersist() {
        if (timestampMsg == null) timestampMsg = LocalDateTime.now();
    }
}
