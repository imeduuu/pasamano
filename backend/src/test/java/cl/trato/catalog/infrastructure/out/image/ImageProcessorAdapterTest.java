package cl.trato.catalog.infrastructure.out.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.catalog.application.port.ImageProcessor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** RN-12, RNF-SEG-09 y RNF-REN-04: validación y procesamiento de imágenes. */
class ImageProcessorAdapterTest {

  private final ImageProcessor processor = new ImageProcessorAdapter();

  @Test
  void rejects_pdf_named_jpg() {
    assertThatThrownBy(() -> processor.process("documento.pdf", "%PDF-1.7".getBytes()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void removes_exif_when_reencoding_jpeg() throws Exception {
    byte[] jpeg = jpegWithExif();

    var result = processor.process("foto.jpg", jpeg);

    assertThat(new String(result.original(), java.nio.charset.StandardCharsets.ISO_8859_1))
        .doesNotContain("Exif");
  }

  @Test
  void thumbnail_is_at_most_400_pixels() throws Exception {
    var result = processor.process("foto.png", png(900, 600));
    BufferedImage thumbnail = ImageIO.read(new java.io.ByteArrayInputStream(result.thumbnail()));

    assertThat(thumbnail.getWidth()).isLessThanOrEqualTo(400);
    assertThat(thumbnail.getHeight()).isLessThanOrEqualTo(400);
  }

  @Test
  void rejects_files_over_five_megabytes() {
    assertThatThrownBy(() -> processor.process("foto.png", new byte[6 * 1024 * 1024]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private byte[] png(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }

  private byte[] jpegWithExif() throws Exception {
    BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "jpeg", output);
    byte[] jpeg = output.toByteArray();
    byte[] exif = new byte[] {(byte) 0xff, (byte) 0xe1, 0, 12, 'E', 'x', 'i', 'f', 0, 0, 1, 2};
    byte[] result = new byte[jpeg.length + exif.length];
    System.arraycopy(jpeg, 0, result, 0, 2);
    System.arraycopy(exif, 0, result, 2, exif.length);
    System.arraycopy(jpeg, 2, result, 2 + exif.length, jpeg.length - 2);
    return result;
  }
}
