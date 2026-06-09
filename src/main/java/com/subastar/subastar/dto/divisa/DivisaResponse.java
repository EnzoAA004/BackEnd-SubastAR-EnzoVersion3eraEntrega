package com.subastar.subastar.dto.divisa;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DivisaResponse {
    private String codigo;
    private String nombre;
    private String simbolo;
}
