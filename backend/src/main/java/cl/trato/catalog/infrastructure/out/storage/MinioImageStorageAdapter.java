package cl.trato.catalog.infrastructure.out.storage;

import cl.trato.catalog.application.port.ImageStorage;
import java.net.URI;
import java.time.Duration;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/** Adaptador S3 para imágenes; MinIO se usa localmente (ADR-006). */
public class MinioImageStorageAdapter implements ImageStorage, AutoCloseable {

  private final S3Client client;
  private final S3Presigner presigner;
  private final String bucket;
  private final Duration urlExpiration;

  public MinioImageStorageAdapter(
      String endpoint,
      String bucket,
      String accessKey,
      String secretKey,
      String region,
      Duration urlExpiration) {
    this.bucket = bucket;
    this.urlExpiration = urlExpiration;
    Region awsRegion = Region.of(region);
    StaticCredentialsProvider credentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    this.client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(awsRegion)
            .credentialsProvider(credentials)
            .forcePathStyle(true)
            .build();
    this.presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .region(awsRegion)
            .credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
  }

  @Override
  public void store(String key, byte[] content, String contentType) {
    client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
        RequestBody.fromBytes(content));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                delete(key);
              }
            }
          });
    }
  }

  @Override
  public void delete(String key) {
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  @Override
  public void deleteAfterCommit(String key) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      delete(key);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            delete(key);
          }
        });
  }

  @Override
  public String getUrl(String key) {
    PresignedGetObjectRequest request =
        presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(urlExpiration)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build());
    return request.url().toExternalForm();
  }

  @Override
  public void close() {
    client.close();
    presigner.close();
  }
}
