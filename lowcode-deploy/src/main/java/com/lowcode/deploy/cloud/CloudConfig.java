package com.lowcode.deploy.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudConfig {

    private String provider;

    private String accessKeyId;

    private String accessKeySecret;

    private String regionId;

    private String clusterId;

    private String endpoint;

    private String accountId;
}
