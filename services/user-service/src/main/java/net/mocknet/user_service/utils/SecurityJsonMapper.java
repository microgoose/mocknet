package net.mocknet.user_service.utils;

import net.mocknet.user_service.model.security.SecurityUser;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SecurityJsonMapper {

    private final JsonMapper jsonMapper;

    public SecurityJsonMapper() {
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(SecurityUser.class)
            .allowIfSubType(UUID.class);

        List<JacksonModule> securityModules = SecurityJacksonModules.getModules(
            getClass().getClassLoader(),
            typeValidatorBuilder
        );

        JsonMapper.Builder builder = JsonMapper.builder();
        securityModules.forEach(builder::addModule);

        this.jsonMapper = builder.build();
    }

    public Map<String, Object> parseMap(String data) {
        try {
            return this.jsonMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse JSON: " + data, ex);
        }
    }

    public String writeMap(Map<String, Object> data) {
        try {
            return this.jsonMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize map with keys: " + data.keySet(), ex);
        }
    }
}