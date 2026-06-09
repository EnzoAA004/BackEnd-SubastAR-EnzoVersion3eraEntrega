package com.subastar.subastar.dto.subasta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubastaDetalle extends SubastaResumen {
    private String horario;
    @JsonProperty("url_streaming")
    private String urlStreaming;
}
