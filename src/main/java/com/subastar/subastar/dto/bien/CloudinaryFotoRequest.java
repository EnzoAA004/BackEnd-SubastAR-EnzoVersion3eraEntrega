package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CloudinaryFotoRequest {
    private List<CloudinaryFotoItem> fotos;

    @Data
    public static class CloudinaryFotoItem {
        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("public_id")
        private String publicId;

        private Long version;
        private String format;

        @JsonProperty("resource_type")
        private String resourceType;

        private Long bytes;
        private Integer width;
        private Integer height;

        @JsonProperty("original_filename")
        private String originalFilename;

        @JsonProperty("secure_url")
        private String secureUrl;
    }
}
