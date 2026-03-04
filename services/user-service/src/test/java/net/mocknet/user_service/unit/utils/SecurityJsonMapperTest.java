package net.mocknet.user_service.unit.utils;

import net.mocknet.user_service.utils.SecurityJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SecurityJsonMapperTest {

    private final SecurityJsonMapper mapper;
    private final Map<String, Object> attributes;

    public SecurityJsonMapperTest() {
        mapper = new SecurityJsonMapper();

        Map<String, Object> principal = new HashMap<>();
        principal.put("@class", "net.mocknet.user_service.model.security.SecurityUser");
        principal.put("accountNonExpired", true);
        principal.put("accountNonLocked", true);
        principal.put("credentialsNonExpired", true);
        principal.put("email", "test2@mail.ru");
        principal.put("enabled", true);
        principal.put("id", "4814e3ae-ecd2-4993-bd93-74def8634d28");
        principal.put("password", "$2a$10$mulfdC4cnx0Dj7ZHKL7EK.wz/6NPNxUhJ4nWXN51/AiGkLgWRT/9G");
        principal.put("username", "test2");
        principal.put("authorities", new ArrayList<>(List.of(
            "java.util.ArrayList",
            new ArrayList<>(List.of(
                new HashMap<>(Map.of(
                    "@class", "org.springframework.security.core.authority.SimpleGrantedAuthority",
                    "authority", "ROLE_USER"
                ))
            ))
        )));

        Map<String, Object> token = new HashMap<>();
        token.put("@class", "org.springframework.security.authentication.UsernamePasswordAuthenticationToken");
        token.put("principal", principal);

        this.attributes = new HashMap<>();
        this.attributes.put("@class", "java.util.Collections$UnmodifiableMap");
        this.attributes.put("java.security.Principal", token);
    }

    @Test
    void shouldSerializeAndDeserialize() {
        String json = mapper.writeMap(this.attributes);
        Map<String, Object> result = mapper.parseMap(json);
        assertEquals(this.attributes, result);
    }

    @Test
    void shouldThrowOnInvalidJson() {
        assertThrows(IllegalArgumentException.class, () ->
            mapper.parseMap("not a valid json {{{")
        );
    }

}