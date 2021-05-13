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
    static final String CREDHUB_ENV_VAR = "CREDHUB_NAME";
    static final String CREDHUB_KEYSTORE_ENV_VAR = "CREDHUB_KEYSTORE";
    static final String CREDHUB_TRUSTSTORE_ENV_VAR = "CREDHUB_TRUSTSTORE";

    static final String SSL_PROTOCOL_PROPERTY = "spring.kafka.ssl.protocol";
    static final String KAFKA_PROPERTIES_PROPERTY = "spring.kafka.properties";
    static final String ENDPOINT_ALGORITHM_PROPERTY = "ssl.endpoint.identification.algorithm";
    static final String SECURITY_PROTOCOL_PROPERTY = "security.protocol";
    static final String TRUST_STORE_LOCATION_PROPERTY = "spring.kafka.ssl.trust-store-location";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "spring.kafka.ssl.trust-store-password";
    static final String TRUST_STORE_TYPE_PROPERTY = "spring.kafka.ssl.trust-store-type";
    static final String KEY_STORE_LOCATION_PROPERTY = "spring.kafka.ssl.key-store-location";
    static final String KEY_STORE_PASSWORD_PROPERTY = "spring.kafka.ssl.key-store-password";
    static final String KEY_STORE_TYPE_PROPERTY = "spring.kafka.ssl.key-store-type";

    static final String TRUST_STORE_LOCATION = "trust-store-location";
    static final String TRUST_STORE_PASSWORD = "trust-store-password";
    static final String TRUST_STORE_TYPE = "trust-store-type";
    static final String KEY_STORE_LOCATION = "key-store-location";
    static final String KEY_STORE_PASSWORD = "key-store-password";
    static final String KEY_STORE_TYPE = "key-store-type";

    @Override
    public boolean accept(CfService service) {
        final String credhub = readEnv(CREDHUB_ENV_VAR);
        return service.existsByTagIgnoreCase("credhub") && service.getName().equalsIgnoreCase(credhub);
    }

    @Override
    public void process(CfCredentials cfCredentials, Map<String, Object> properties) {
        final Map<String, Object> kafkaProperties = new HashMap<>();
        kafkaProperties.put(ENDPOINT_ALGORITHM_PROPERTY, null);
        kafkaProperties.put(SECURITY_PROTOCOL_PROPERTY, "ssl");
        properties.put(SSL_PROTOCOL_PROPERTY, "ssl");
        properties.put(KAFKA_PROPERTIES_PROPERTY, kafkaProperties);

        final String trustStoreLocation = getStoreLocationKey(CREDHUB_TRUSTSTORE_ENV_VAR, TRUST_STORE_LOCATION);
        final String trustStorePassword = getStorePasswordKey(CREDHUB_TRUSTSTORE_ENV_VAR, TRUST_STORE_PASSWORD);
        final String trustStoreType = getStoreTypeKey(CREDHUB_TRUSTSTORE_ENV_VAR, TRUST_STORE_TYPE);

        properties.put(TRUST_STORE_LOCATION_PROPERTY, extractKeyStore(cfCredentials, trustStoreLocation));
        properties.put(TRUST_STORE_PASSWORD_PROPERTY, cfCredentials.getMap().get(trustStorePassword).toString());
        properties.put(TRUST_STORE_TYPE_PROPERTY, cfCredentials.getMap().get(trustStoreType).toString());

        final String keyStoreLocation = getStoreLocationKey(CREDHUB_KEYSTORE_ENV_VAR, KEY_STORE_LOCATION);
        final String keyStorePassword = getStorePasswordKey(CREDHUB_KEYSTORE_ENV_VAR, KEY_STORE_PASSWORD);
        final String keyStoreType = getStoreTypeKey(CREDHUB_KEYSTORE_ENV_VAR, KEY_STORE_TYPE);

        properties.put(KEY_STORE_LOCATION_PROPERTY, extractKeyStore(cfCredentials, keyStoreLocation));
        properties.put(KEY_STORE_PASSWORD_PROPERTY, cfCredentials.getMap().get(keyStorePassword).toString());
        properties.put(KEY_STORE_TYPE_PROPERTY, cfCredentials.getMap().get(keyStoreType).toString());
    }

    @Override
    public CfEnvProcessorProperties getProperties() {
        return CfEnvProcessorProperties.builder()
                .propertyPrefixes("spring.kafka")
                .serviceName("Kafka using CredHub")
                .build();
    }

    private String getStoreLocationKey(String envVar, String defaultKey) {
        return getKey(envVar, defaultKey, "location");
    }

    private String getStorePasswordKey(String envVar, String defaultKey) {
        return getKey(envVar, defaultKey, "password");
    }

    private String getStoreTypeKey(String envVar, String defaultKey) {
        return getKey(envVar, defaultKey, "type");
    }

    private String getKey(String envVar, String defaultKey, String suffix) {
        return readOptionalEnv(envVar)
            .map(value -> value + "-" + suffix)
            .orElseGet(() -> defaultKey);
    }

    private static String readEnv(String var) {
        final String value = Optional.of(System.getenv(var))
            .orElseThrow(() -> new IllegalArgumentException("Environment variable " + var + " is required"));
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment variable " + var + " requires a value");
        }
        return value;
    }

    private static Optional<String> readOptionalEnv(String var) {
        return Optional.ofNullable(System.getenv(var));
    }

    private static String extractKeyStore(CfCredentials cfCredentials, String key) {
        final Map<String, Object> content = cfCredentials.getMap();
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(content.get(key).toString());
        } catch (Exception e) {
            throw new RuntimeException("Failure grabbing value " + key + " from CredHub");
        }
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
