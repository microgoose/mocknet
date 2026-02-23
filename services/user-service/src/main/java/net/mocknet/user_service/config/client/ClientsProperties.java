package net.mocknet.user_service.config.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ClientsProperties {
    private Map<String, ClientProperties> clients = new HashMap<>();

    @Getter
    @Setter
    public static class ClientProperties {
        private RegistrationProperties registration;
        private boolean requireAuthorizationConsent;
        private boolean requireProofKey;

        @Getter
        @Setter
        public static class RegistrationProperties {
            private String clientId;
            private String clientSecret;
            private List<String> clientAuthenticationMethods;
            private List<String> authorizationGrantTypes;
            private List<String> redirectUris;
            private List<String> postLogoutRedirectUris;
            private List<String> scopes;
        }
    }
}