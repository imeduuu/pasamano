package cl.trato.catalog.application.port;

/** Procesa imágenes de publicaciones (RN-12, RNF-SEG-09 y RNF-REN-04). */
public interface ImageProcessor {

  /** Valida, recodifica sin metadatos y genera una miniatura. */
  ProcessedImage process(String filename, byte[] content);

  /** Imagen recodificada y miniatura lista para almacenar. */
  record ProcessedImage(byte[] original, byte[] thumbnail, String contentType) {}
}
