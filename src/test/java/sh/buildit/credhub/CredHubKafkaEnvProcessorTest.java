package sh.buildit.credhub;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import io.pivotal.cfenv.spring.boot.CfEnvProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CredHubKafkaEnvProcessorTest {

    private CfEnvProcessor processor;

    @BeforeEach
    public void setup() {
        processor = new CredHubKafkaEnvProcessor();
    }

    @Test
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_ENV_VAR, value = "my-credhub")
    public void acceptsService() {
        final Map<String, Object> serviceData = new HashMap<>();
        final List<String> tags = new ArrayList<>();
        tags.add("credhub");
        serviceData.put("tags", tags);
        serviceData.put("name", "my-credhub");
        final CfService service = new CfService(serviceData);
        final boolean accepts = processor.accept(service);
        assertTrue(accepts);
    }

    @Test
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_ENV_VAR, value = "not-my-credhub")
    public void notAcceptsService() {
        final Map<String, Object> serviceData = new HashMap<>();
        final List<String> tags = new ArrayList<>();
        tags.add("credhub");
        serviceData.put("tags", tags);
        serviceData.put("name", "my-credhub");
        final CfService service = new CfService(serviceData);
        final boolean accepts = processor.accept(service);
        assertFalse(accepts);
    }

    @Test
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_ENV_VAR, value = "my-credhub")
    public void processDefault() {
        final String keyStoreLocation = Base64.getEncoder().encodeToString("keyStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String keyStorePassword = "keyStorePassword";
        final String keyStoreType = "keyStoreType";

        final String trustStoreLocation = Base64.getEncoder().encodeToString("trustStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String trustStorePassword = "trustStorePassword";
        final String trustStoreType = "trustStoreType";

        final Map<String, Object> credentialsData = new HashMap<>();
        credentialsData.put(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION, keyStoreLocation);
        credentialsData.put(CredHubKafkaEnvProcessor.KEY_STORE_PASSWORD, keyStorePassword);
        credentialsData.put(CredHubKafkaEnvProcessor.KEY_STORE_TYPE, keyStoreType);
        credentialsData.put(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION, trustStoreLocation);
        credentialsData.put(CredHubKafkaEnvProcessor.TRUST_STORE_PASSWORD, trustStorePassword);
        credentialsData.put(CredHubKafkaEnvProcessor.TRUST_STORE_TYPE, trustStoreType);
        final CfCredentials credentials = new CfCredentials(credentialsData);
        final Map<String, Object> properties = new HashMap<>();

        processor.process(credentials, properties);
        verifyCommonProperties(properties);

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(keyStorePassword, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_PASSWORD_PROPERTY));
        assertEquals(keyStoreType, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_TYPE_PROPERTY));

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(trustStorePassword, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_PASSWORD_PROPERTY));
        assertEquals(trustStoreType, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_TYPE_PROPERTY));
    }

    @Test
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_ENV_VAR, value = "my-credhub")
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_KEYSTORE_ENV_VAR, value = "my-key-store")
    public void processKeyStore() {
        final String keyStoreLocation = Base64.getEncoder().encodeToString("keyStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String keyStorePassword = "keyStorePassword";
        final String keyStoreType = "keyStoreType";

        final String trustStoreLocation = Base64.getEncoder().encodeToString("trustStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String trustStorePassword = "trustStorePassword";
        final String trustStoreType = "trustStoreType";

        final Map<String, Object> credentialsData = new HashMap<>();
        credentialsData.put("my-key-store-location", keyStoreLocation);
        credentialsData.put("my-key-store-password", keyStorePassword);
        credentialsData.put("my-key-store-type", keyStoreType);
        credentialsData.put(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION, trustStoreLocation);
        credentialsData.put(CredHubKafkaEnvProcessor.TRUST_STORE_PASSWORD, trustStorePassword);
        credentialsData.put(CredHubKafkaEnvProcessor.TRUST_STORE_TYPE, trustStoreType);
        final CfCredentials credentials = new CfCredentials(credentialsData);
        final Map<String, Object> properties = new HashMap<>();

        processor.process(credentials, properties);
        verifyCommonProperties(properties);

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(keyStorePassword, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_PASSWORD_PROPERTY));
        assertEquals(keyStoreType, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_TYPE_PROPERTY));

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(trustStorePassword, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_PASSWORD_PROPERTY));
        assertEquals(trustStoreType, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_TYPE_PROPERTY));
    }

    @Test
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_ENV_VAR, value = "my-credhub")
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_TRUSTSTORE_ENV_VAR, value = "my-trust-store")
    public void processTrustStore() {
        final String keyStoreLocation = Base64.getEncoder().encodeToString("keyStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String keyStorePassword = "keyStorePassword";
        final String keyStoreType = "keyStoreType";

        final String trustStoreLocation = Base64.getEncoder().encodeToString("trustStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String trustStorePassword = "trustStorePassword";
        final String trustStoreType = "trustStoreType";

        final Map<String, Object> credentialsData = new HashMap<>();
        credentialsData.put(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION, keyStoreLocation);
        credentialsData.put(CredHubKafkaEnvProcessor.KEY_STORE_PASSWORD, keyStorePassword);
        credentialsData.put(CredHubKafkaEnvProcessor.KEY_STORE_TYPE, keyStoreType);
        credentialsData.put("my-trust-store-location", trustStoreLocation);
        credentialsData.put("my-trust-store-password", trustStorePassword);
        credentialsData.put("my-trust-store-type", trustStoreType);
        final CfCredentials credentials = new CfCredentials(credentialsData);
        final Map<String, Object> properties = new HashMap<>();

        processor.process(credentials, properties);
        verifyCommonProperties(properties);

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(keyStorePassword, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_PASSWORD_PROPERTY));
        assertEquals(keyStoreType, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_TYPE_PROPERTY));

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(trustStorePassword, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_PASSWORD_PROPERTY));
        assertEquals(trustStoreType, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_TYPE_PROPERTY));
    }

    @Test
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_ENV_VAR, value = "my-credhub")
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_KEYSTORE_ENV_VAR, value = "my-key-store")
    @SetEnvironmentVariable(key = CredHubKafkaEnvProcessor.CREDHUB_TRUSTSTORE_ENV_VAR, value = "my-trust-store")
    public void processKeyStoreTrustStore() {
        final String keyStoreLocation = Base64.getEncoder().encodeToString("keyStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String keyStorePassword = "keyStorePassword";
        final String keyStoreType = "keyStoreType";

        final String trustStoreLocation = Base64.getEncoder().encodeToString("trustStoreLocation".getBytes(StandardCharsets.UTF_8));
        final String trustStorePassword = "trustStorePassword";
        final String trustStoreType = "trustStoreType";

        final Map<String, Object> credentialsData = new HashMap<>();
        credentialsData.put("my-key-store-location", keyStoreLocation);
        credentialsData.put("my-key-store-password", keyStorePassword);
        credentialsData.put("my-key-store-type", keyStoreType);
        credentialsData.put("my-trust-store-location", trustStoreLocation);
        credentialsData.put("my-trust-store-password", trustStorePassword);
        credentialsData.put("my-trust-store-type", trustStoreType);
        final CfCredentials credentials = new CfCredentials(credentialsData);
        final Map<String, Object> properties = new HashMap<>();

        processor.process(credentials, properties);
        verifyCommonProperties(properties);

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.KEY_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(keyStorePassword, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_PASSWORD_PROPERTY));
        assertEquals(keyStoreType, properties.get(CredHubKafkaEnvProcessor.KEY_STORE_TYPE_PROPERTY));

        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY));
        assertFalse(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().isEmpty());
        assertTrue(properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_LOCATION_PROPERTY).toString().startsWith("file://"));
        assertEquals(trustStorePassword, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_PASSWORD_PROPERTY));
        assertEquals(trustStoreType, properties.get(CredHubKafkaEnvProcessor.TRUST_STORE_TYPE_PROPERTY));
    }

    @SuppressWarnings("unchecked")
    private void verifyCommonProperties(Map<String, Object> properties) {
        assertTrue(properties.containsKey(CredHubKafkaEnvProcessor.KAFKA_PROPERTIES_PROPERTY));
        final Map<String, Object> kafkaProperties = (Map<String, Object>) properties.get(CredHubKafkaEnvProcessor.KAFKA_PROPERTIES_PROPERTY);
        assertEquals("ssl", kafkaProperties.get(CredHubKafkaEnvProcessor.SECURITY_PROTOCOL_PROPERTY));
        assertNull(kafkaProperties.get(CredHubKafkaEnvProcessor.ENDPOINT_ALGORITHM_PROPERTY));

        assertEquals("ssl", properties.get(CredHubKafkaEnvProcessor.SSL_PROTOCOL_PROPERTY));
    }

}
