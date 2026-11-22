package cl.trato.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.trato.shared.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

/** Reglas de categorías de RF-CAT-01. */
class CategoryTest {

  @Test
  void creates_root_and_child_with_unique_name_based_slug_order_and_status() {
    Category root = Category.create("Electrónica", 1, true, null);
    Category child = Category.create("Audio y Vídeo", 2, true, root);

    assertThat(root.slug()).isEqualTo("electronica");
    assertThat(root.parentId()).isNull();
    assertThat(child.slug()).isEqualTo("audio-y-video");
    assertThat(child.parentId()).isEqualTo(root.id());
    assertThat(child.sortOrder()).isEqualTo(2);
    assertThat(Category.create("Oculta", 3, false, null).active()).isFalse();
  }

  @Test
  void rejects_child_of_level_two_category() {
    Category root = Category.create("Hogar", 1, true, null);
    Category child = Category.create("Cocina", 1, true, root);

    assertThatThrownBy(() -> Category.create("Ollas", 1, true, child))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void rejects_blank_names_and_negative_order() {
    assertThatThrownBy(() -> Category.create("  ", 0, true, null))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> Category.create("Hogar", -1, true, null))
        .isInstanceOf(DomainValidationException.class);
  }
}
