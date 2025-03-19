package com.example.autotradebot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorApiKeyDto {
    private Long id;
    private String emailPk;
    private String accessKey;
    private String secretKey;
    private String status;
    private String vendor;
    private Date createdDt;
}
