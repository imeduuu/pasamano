package cl.trato.catalog.application.port;

/** Puerto para almacenar y servir imágenes de publicaciones (ADR-006, RF-PUB-02). */
public interface ImageStorage {

  /** Guarda la imagen bajo la clave indicada. */
  void store(String key, byte[] content, String contentType);

  /** Elimina la imagen asociada a la clave indicada. */
  void delete(String key);

  /** Elimina el objeto solo si la transacción de base de datos confirma. */
  void deleteAfterCommit(String key);

  /** Obtiene una URL temporal de lectura para la clave indicada. */
  String getUrl(String key);
}
