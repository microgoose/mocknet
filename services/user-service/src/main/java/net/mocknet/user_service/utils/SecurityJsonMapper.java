package net.mocknet.user_service.utils;

import net.mocknet.user_service.model.security.SecurityUser;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SecurityJsonMapper {

    private final JsonMapper jsonMapper;

    public SecurityJsonMapper(ClassLoader classLoader) {
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(SecurityUser.class)
            .allowIfSubType(UUID.class);

        List<JacksonModule> securityModules = SecurityJacksonModules.getModules(classLoader, typeValidatorBuilder);
        JsonMapper.Builder builder = JsonMapper.builder();
        securityModules.forEach(builder::addModule);
        builder.addModule(new OAuth2AuthorizationServerJacksonModule());

        this.jsonMapper = builder.build();
    }

    public Map<String, Object> parseMap(String data) {
        try {
            return this.jsonMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    public String writeMap(Map<String, Object> data) {
        try {
            return this.jsonMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}