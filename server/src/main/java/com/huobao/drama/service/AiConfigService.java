package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.AiServiceConfig;
import com.huobao.drama.mapper.AiServiceConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiServiceConfigMapper aiServiceConfigMapper;

    public List<AiServiceConfig> listConfigs(String serviceType) {
        LambdaQueryWrapper<AiServiceConfig> wrapper = new LambdaQueryWrapper<>();
        if (serviceType != null && !serviceType.isEmpty()) {
            wrapper.eq(AiServiceConfig::getServiceType, serviceType);
        }
        return aiServiceConfigMapper.selectList(wrapper);
    }

    public AiServiceConfig createConfig(Map<String, Object> body) {
        AiServiceConfig config = new AiServiceConfig();
        if (body.get("service_type") != null) config.setServiceType(body.get("service_type").toString());
        if (body.get("provider") != null) config.setProvider(body.get("provider").toString());
        if (body.get("name") != null) config.setName(body.get("name").toString());
        if (body.get("base_url") != null) config.setBaseUrl(body.get("base_url").toString());
        if (body.get("api_key") != null) config.setApiKey(body.get("api_key").toString());
        if (body.get("model") != null) config.setModel(body.get("model").toString());
        if (body.get("priority") != null) config.setPriority(Integer.valueOf(body.get("priority").toString()));
        aiServiceConfigMapper.insert(config);
        return config;
    }

    public AiServiceConfig getConfig(Long id) {
        return aiServiceConfigMapper.selectById(id);
    }

    public boolean updateConfig(Long id, Map<String, Object> body) {
        AiServiceConfig config = aiServiceConfigMapper.selectById(id);
        if (config == null) return false;

        if (body.containsKey("provider") && body.get("provider") != null) config.setProvider(body.get("provider").toString());
        if (body.containsKey("name") && body.get("name") != null) config.setName(body.get("name").toString());
        if (body.containsKey("base_url") && body.get("base_url") != null) config.setBaseUrl(body.get("base_url").toString());
        if (body.containsKey("api_key") && body.get("api_key") != null) config.setApiKey(body.get("api_key").toString());
        if (body.containsKey("model") && body.get("model") != null) config.setModel(body.get("model").toString());
        if (body.containsKey("endpoint") && body.get("endpoint") != null) config.setEndpoint(body.get("endpoint").toString());
        if (body.containsKey("query_endpoint") && body.get("query_endpoint") != null) config.setQueryEndpoint(body.get("query_endpoint").toString());
        if (body.containsKey("settings") && body.get("settings") != null) config.setSettings(body.get("settings").toString());
        if (body.containsKey("priority") && body.get("priority") != null)
            config.setPriority(Integer.valueOf(body.get("priority").toString()));
        if (body.containsKey("is_active") && body.get("is_active") != null)
            config.setIsActive(Boolean.valueOf(body.get("is_active").toString()));
        if (body.containsKey("is_default") && body.get("is_default") != null)
            config.setIsDefault(Boolean.valueOf(body.get("is_default").toString()));
        if (body.containsKey("access_key") && body.get("access_key") != null) config.setAccessKey(body.get("access_key").toString());
        if (body.containsKey("secret_key") && body.get("secret_key") != null) config.setSecretKey(body.get("secret_key").toString());

        aiServiceConfigMapper.updateById(config);
        return true;
    }

    public boolean deleteConfig(Long id) {
        return aiServiceConfigMapper.deleteById(id) > 0;
    }
}
