package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.Prop;
import com.huobao.drama.mapper.PropMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PropService {

    private final PropMapper propMapper;

    public List<Prop> listByDrama(Long dramaId) {
        return propMapper.selectList(
                new LambdaQueryWrapper<Prop>().eq(Prop::getDramaId, dramaId));
    }

    public Prop create(Map<String, Object> body) {
        Prop prop = new Prop();
        Object dramaId = body.get("drama_id");
        if (dramaId instanceof Number) prop.setDramaId(((Number) dramaId).longValue());
        prop.setName((String) body.get("name"));
        prop.setType((String) body.get("type"));
        prop.setDescription((String) body.get("description"));
        prop.setPrompt((String) body.get("prompt"));
        propMapper.insert(prop);
        return prop;
    }

    public boolean update(Long id, Map<String, Object> body) {
        Prop prop = propMapper.selectById(id);
        if (prop == null) return false;

        if (body.containsKey("name")) prop.setName((String) body.get("name"));
        if (body.containsKey("type")) prop.setType((String) body.get("type"));
        if (body.containsKey("description")) prop.setDescription((String) body.get("description"));
        if (body.containsKey("prompt")) prop.setPrompt((String) body.get("prompt"));
        if (body.containsKey("image_url")) prop.setImageUrl((String) body.get("image_url"));

        propMapper.updateById(prop);
        return true;
    }

    public boolean delete(Long id) {
        Prop prop = propMapper.selectById(id);
        if (prop == null) return false;
        propMapper.deleteById(id);
        return true;
    }

    public Prop regeneratePrompt(Long id) {
        Prop prop = propMapper.selectById(id);
        if (prop == null) {
            return null;
        }

        // TODO: 调用 agent 重新生成 prompt
        // 暂时返回原对象，后续实现

        return prop;
    }
}
