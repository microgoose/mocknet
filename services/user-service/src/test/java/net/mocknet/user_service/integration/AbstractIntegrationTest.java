package net.mocknet.user_service.integration;

import lombok.Getter;
import net.mocknet.user_service.UserServiceApplication;
import net.mocknet.user_service.common.security.TokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = UserServiceApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Getter
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:9.6.12");
    private static final KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @LocalServerPort
    private int port;

    @Autowired
    protected RestTestClient restCli;

    @Autowired
    protected TokenGenerator tokenGenerator;

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}