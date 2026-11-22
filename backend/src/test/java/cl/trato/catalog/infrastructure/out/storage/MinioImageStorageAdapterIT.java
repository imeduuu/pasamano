package cl.trato.catalog.infrastructure.out.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/** Prueba el almacenamiento compatible con S3 contra MinIO real (ADR-006). */
@Testcontainers
class MinioImageStorageAdapterIT {

  private static final String BUCKET = "listings";
  private static final String ACCESS_KEY = "trato-minio";
  private static final String SECRET_KEY = "trato-minio-test";

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("cgr.dev/chainguard/minio:latest-dev")
          .withCommand("server", "/data", "--console-address", ":9001")
          .withEnv(Map.of("MINIO_ROOT_USER", ACCESS_KEY, "MINIO_ROOT_PASSWORD", SECRET_KEY))
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  @Test
  void stores_reads_and_deletes_an_image() throws Exception {
    String endpoint = "http://%s:%d".formatted(MINIO.getHost(), MINIO.getMappedPort(9000));
    var credentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    try (S3Client client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .forcePathStyle(true)
            .build()) {
      client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }

    MinioImageStorageAdapter storage =
        new MinioImageStorageAdapter(
            endpoint, BUCKET, ACCESS_KEY, SECRET_KEY, "us-east-1", Duration.ofMinutes(10));
    String key = "integration/imagen.txt";
    byte[] image = "contenido de prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    storage.store(key, image, "text/plain");

    HttpClient httpClient = HttpClient.newHttpClient();
    HttpResponse<byte[]> response =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(storage.getUrl(key))).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo(image);

    storage.delete(key);

    HttpResponse<byte[]> deletedResponse =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(storage.getUrl(key))).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertThat(deletedResponse.statusCode()).isBetween(400, 499);
    storage.close();
  }
}
