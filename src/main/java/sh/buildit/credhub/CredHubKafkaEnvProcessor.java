package sh.buildit.credhub;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import io.pivotal.cfenv.spring.boot.CfEnvProcessor;
import io.pivotal.cfenv.spring.boot.CfEnvProcessorProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CredHubKafkaEnvProcessor implements CfEnvProcessor {
    private static final String CREDHUB_ENV_VAR = "CREDHUB_NAME";

    private static final String SSL_PROTOCOL_PROPERTY = "spring.kafka.ssl.protocol";
    private static final String KAFKA_PROPERTIES_PROPERTY = "spring.kafka.properties";
    private static final String ENDPOINT_ALGORITHM_PROPERTY = "ssl.endpoint.identification.algorithm";
    private static final String SECURITY_PROTOCOL_PROPERTY = "security.protocol";
    private static final String TRUST_STORE_LOCATION_PROPERTY = "spring.kafka.ssl.trust-store-location";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "spring.kafka.ssl.trust-store-password";
    private static final String TRUST_STORE_TYPE_PROPERTY = "spring.kafka.ssl.trust-store-type";
    private static final String KEY_STORE_LOCATION_PROPERTY = "spring.kafka.ssl.key-store-location";
    private static final String KEY_STORE_PASSWORD_PROPERTY = "spring.kafka.ssl.key-store-password";
    private static final String KEY_STORE_TYPE_PROPERTY = "spring.kafka.ssl.key-store-type";

    private static final String TRUST_STORE_LOCATION = "trust-store-location";
    private static final String TRUST_STORE_PASSWORD = "trust-store-password";
    private static final String TRUST_STORE_TYPE = "trust-store-type";
    private static final String KEY_STORE_LOCATION = "key-store-location";
    private static final String KEY_STORE_PASSWORD = "key-store-password";
    private static final String KEY_STORE_TYPE = "key-store-type";

    @Override
    public boolean accept(CfService service) {
        final String credhub = readEnv(CREDHUB_ENV_VAR);
        return service.existsByTagIgnoreCase("credhub") && service.getName().equalsIgnoreCase(credhub);
    }

    private static String readEnv(String var) {
        final String value = Optional.of(System.getenv(var))
                .orElseThrow(() -> new IllegalArgumentException("Environment variable " + var + " is required"));
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment variable " + var + " requires a value");
        }
        return value;
    }

    @Override
    public void process(CfCredentials cfCredentials, Map<String, Object> properties) {
        final Map<String, Object> kafkaProperties = new HashMap<>();
        kafkaProperties.put(ENDPOINT_ALGORITHM_PROPERTY, null);
        kafkaProperties.put(SECURITY_PROTOCOL_PROPERTY, "ssl");
        properties.put(SSL_PROTOCOL_PROPERTY, "ssl");

        properties.put(KAFKA_PROPERTIES_PROPERTY, kafkaProperties);
        properties.put(TRUST_STORE_LOCATION_PROPERTY, extractKeyStore(cfCredentials, TRUST_STORE_LOCATION));
        properties.put(TRUST_STORE_PASSWORD_PROPERTY, cfCredentials.getMap().get(TRUST_STORE_PASSWORD).toString());
        properties.put(TRUST_STORE_TYPE_PROPERTY, cfCredentials.getMap().get(TRUST_STORE_TYPE).toString());
        properties.put(KEY_STORE_LOCATION_PROPERTY, extractKeyStore(cfCredentials, KEY_STORE_LOCATION));
        properties.put(KEY_STORE_PASSWORD_PROPERTY, cfCredentials.getMap().get(KEY_STORE_PASSWORD).toString());
        properties.put(KEY_STORE_TYPE_PROPERTY, cfCredentials.getMap().get(KEY_STORE_TYPE).toString());
    }

    @Override
    public CfEnvProcessorProperties getProperties() {
        return CfEnvProcessorProperties.builder()
                .propertyPrefixes("spring.kafka")
                .serviceName("Kafka using CredHub")
                .build();
    }

    private static String extractKeyStore(CfCredentials cfCredentials, String key) {
        final Map<String, Object> content = cfCredentials.getMap();
        final byte[] decoded = Base64.getDecoder().decode(content.get(key).toString());
        try {
            final Path tempFile = Files.createTempFile(null, ".jks");
            try (final InputStream inputStream = new ByteArrayInputStream(decoded)) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return "file://" + tempFile.toAbsolutePath();
        } catch (IOException e) {
            System.err.println("Problem writing out key " + key);
            throw new RuntimeException(e);
        }
    }
}
