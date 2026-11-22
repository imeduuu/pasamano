package cl.trato.catalog.infrastructure.out.image;

import cl.trato.catalog.application.port.ImageProcessor;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Procesa formatos permitidos por su firma binaria, sin confiar en metadatos externos. */
public class ImageProcessorAdapter implements ImageProcessor {

  private static final int MAX_BYTES = 5 * 1024 * 1024;
  private static final int MAX_THUMBNAIL_EDGE = 400;
  private static final String OUTPUT_FORMAT = "jpeg";

  @Override
  public ProcessedImage process(String filename, byte[] content) {
    if (content == null || content.length == 0 || content.length > MAX_BYTES) {
      throw new IllegalArgumentException("La imagen debe pesar hasta 5 MB.");
    }
    String format = detectFormat(content);
    try {
      BufferedImage source = ImageIO.read(new ByteArrayInputStream(content));
      if (source == null) {
        throw new IllegalArgumentException("El archivo no contiene una imagen válida.");
      }
      BufferedImage normalized = toRgb(source);
      return new ProcessedImage(encode(normalized), encode(thumbnail(normalized)), "image/jpeg");
    } catch (IOException exception) {
      throw new IllegalArgumentException("No se pudo procesar la imagen.", exception);
    }
  }

  private String detectFormat(byte[] bytes) {
    if (matches(bytes, 0xff, 0xd8, 0xff)) {
      return "jpeg";
    }
    if (matches(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) {
      return "png";
    }
    if (bytes.length >= 12
        && matchesAt(bytes, 0, 'R', 'I', 'F', 'F')
        && matchesAt(bytes, 8, 'W', 'E', 'B', 'P')) {
      return "webp";
    }
    throw new IllegalArgumentException("Solo se permiten imágenes JPG, PNG o WebP.");
  }

  private boolean matches(byte[] bytes, int... signature) {
    return matchesAt(bytes, 0, signature);
  }

  private boolean matchesAt(byte[] bytes, int offset, int... signature) {
    if (bytes.length < offset + signature.length) {
      return false;
    }
    for (int index = 0; index < signature.length; index++) {
      if ((bytes[offset + index] & 0xff) != signature[index]) {
        return false;
      }
    }
    return true;
  }

  private BufferedImage toRgb(BufferedImage source) {
    BufferedImage image =
        new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.drawImage(source, 0, 0, null);
    graphics.dispose();
    return image;
  }

  private BufferedImage thumbnail(BufferedImage source) {
    double scale =
        Math.min(
            1.0, (double) MAX_THUMBNAIL_EDGE / Math.max(source.getWidth(), source.getHeight()));
    int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
    int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.drawImage(source, 0, 0, width, height, null);
    graphics.dispose();
    return image;
  }

  private byte[] encode(BufferedImage image) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(image, OUTPUT_FORMAT, output)) {
      throw new IOException("No hay codificador JPEG disponible.");
    }
    return output.toByteArray();
  }
}
