package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AceptarCondicionesRequest {
    private boolean acepta;

    @JsonProperty("cuenta_cobro_id")
    private Integer cuentaCobroId;
}
