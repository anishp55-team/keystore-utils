package sh.buildit.credhub;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public final class KeyStoreUtils {

    private KeyStoreUtils() {
    }

    public static void initialize() {
        final CfEnv cfEnv = new CfEnv();
        if (cfEnv.isInCf()) {
            final String credhub = readEnv("CREDHUB_NAME");
            final String keyStoreName = readEnv("CREDHUB_KEYSTORE");
            final String trustStoreName = readEnv("CREDHUB_TRUSTSTORE");
            final CfCredentials cfCredentials = cfEnv.findServicesByTag("credhub").stream()
                .filter(cfService -> cfService.getName().equals(credhub))
                .findFirst()
                .map(CfService::getCredentials)
                .orElseThrow(() -> new RuntimeException("Cannot find CredHub service with name " + credhub));
            try {
                extractKeyStore(cfCredentials, keyStoreName);
                extractKeyStore(cfCredentials, trustStoreName);
            } catch (Exception e) {
                throw new RuntimeException("Problem processing stores", e);
            }
        }
    }

    private static String readEnv(String var) {
        final String value = Optional.of(System.getenv(var))
            .orElseThrow(() -> new IllegalArgumentException("Environment variable " + var + " is required"));
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment variable " + var + " requires a value");
        }
        return value;
    }

    private static void extractKeyStore(CfCredentials cfCredentials, String storeName) {
        final Map<String, Object> content = cfCredentials.getMap();
        final byte[] decoded = Base64.getDecoder().decode(content.get(storeName).toString());
        final String path = content.get(storeName + "-path").toString();
        try {
            final Path keystore = Paths.get(path);
            Files.createDirectories(keystore.getParent());
            try (final InputStream inputStream = new ByteArrayInputStream(decoded)) {
                Files.copy(inputStream, keystore, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Problem writing out store " + storeName);
            throw new RuntimeException(e);
        }
    }

}
