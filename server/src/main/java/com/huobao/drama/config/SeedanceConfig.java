package com.huobao.drama.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "seedance")
public class SeedanceConfig {
    /** Volcengine Access Key ID for asset API */
    private String accessKey = "";
    /** Volcengine Secret Access Key for asset API */
    private String secretKey = "";
    /** ARK API Key for video generation API (Bearer token) */
    private String arkApiKey = "";
    /** Asset API endpoint */
    private String assetEndpoint = "ark.cn-beijing.volcengineapi.com";
    /** Video generation API base URL */
    private String videoBaseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    /** Video generation model */
    private String videoModel = "doubao-seedance-2-0-260128";
    /** Region */
    private String region = "cn-beijing";
    /** Service name */
    private String service = "ark";
}
