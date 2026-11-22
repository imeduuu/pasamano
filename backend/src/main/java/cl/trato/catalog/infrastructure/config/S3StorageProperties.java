package cl.trato.catalog.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del almacenamiento compatible con S3. */
@ConfigurationProperties(prefix = "trato.storage.s3")
public record S3StorageProperties(
    String endpoint,
    String bucket,
    String accessKey,
    String secretKey,
    String region,
    Duration urlExpiration) {}
