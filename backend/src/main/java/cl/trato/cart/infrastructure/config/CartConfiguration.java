package cl.trato.cart.infrastructure.config;

import cl.trato.cart.application.CartService;
import cl.trato.cart.application.port.CartRepository;
import cl.trato.cart.application.port.ListingAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CartConfiguration {
  @Bean
  CartService cartService(CartRepository carts, ListingAvailability listings) {
    return new CartService(carts, listings);
  }
}
