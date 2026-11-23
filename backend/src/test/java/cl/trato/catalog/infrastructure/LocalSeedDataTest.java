package cl.trato.catalog.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LocalSeedDataTest {

  @Test
  void placeholderImages_areGeneratedAsLocalSvgAssets() {
    LocalSeedData.PlaceholderImages images = LocalSeedData.placeholderImages();

    assertThat(images.originalKey()).isEqualTo("seed/placeholder.svg");
    assertThat(images.thumbnailKey()).isEqualTo("seed/placeholder-thumb.svg");
    assertThat(new String(images.original(), StandardCharsets.UTF_8))
        .contains("<svg", "Publicación de prueba");
    assertThat(new String(images.thumbnail(), StandardCharsets.UTF_8)).contains("<svg");
  }
}
