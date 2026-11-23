package cl.trato.catalog.application;

import cl.trato.catalog.application.port.ImageStorage;
import cl.trato.catalog.application.port.ListingRepository;
import cl.trato.catalog.domain.Listing;
import cl.trato.catalog.domain.ListingStatus;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.domain.UserId;
import cl.trato.shared.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Consultas públicas y del vendedor sobre publicaciones (RF-PUB-08, RF-BUS-05). */
public class GetListings {
  public record ImageView(String url, String thumbnailUrl) {}

  public record ListingView(
      UUID id,
      String title,
      String description,
      long price,
      int stock,
      String condition,
      UUID categoryId,
      String region,
      String commune,
      String status,
      String storeName,
      String reputation,
      List<ImageView> images) {}

  public record ListingPage(List<ListingView> items, int page, int size, boolean hasNext) {}

  private final ListingRepository listings;
  private final SellerProfileRepository sellers;
  private final ImageStorage images;

  public GetListings(
      ListingRepository listings, SellerProfileRepository sellers, ImageStorage images) {
    this.listings = listings;
    this.sellers = sellers;
    this.images = images;
  }

  @Transactional(readOnly = true)
  public ListingView get(UUID id, UserId viewer) {
    Listing listing = listings.findById(id).orElseThrow(GetListings::notFound);
    boolean owner = viewer != null && viewer.equals(listing.seller());
    if (listing.status() == ListingStatus.ELIMINADA
        || (!owner
            && listing.status() != ListingStatus.ACTIVA
            && listing.status() != ListingStatus.AGOTADA)) {
      throw notFound();
    }
    String storeName =
        sellers.findByUserId(listing.seller()).map(p -> p.storeName()).orElse("Vendedor nuevo");
    return view(listing, storeName);
  }

  @Transactional(readOnly = true)
  public ListingPage mine(UserId seller, ListingStatus status, int page, int size) {
    if (status == ListingStatus.ELIMINADA) {
      return new ListingPage(List.of(), page, size, false);
    }
    List<Listing> batch = listings.findBySeller(seller, status, page * size, size);
    boolean hasNext = listings.countBySeller(seller, status) > (long) (page + 1) * size;
    String storeName =
        sellers.findByUserId(seller).map(p -> p.storeName()).orElse("Vendedor nuevo");
    List<ListingView> items = batch.stream().map(listing -> view(listing, storeName)).toList();
    return new ListingPage(items, page, size, hasNext);
  }

  private ListingView view(Listing l, String storeName) {
    return new ListingView(
        l.id(),
        l.title(),
        l.description(),
        l.price().amount(),
        l.availableStock(),
        l.condition().name(),
        l.category().id(),
        l.region(),
        l.commune(),
        l.status().name(),
        storeName,
        "Vendedor nuevo",
        l.images().stream()
            .map(key -> new ImageView(images.getUrl(key), images.getUrl(thumbnailKey(key))))
            .toList());
  }

  private static String thumbnailKey(String key) {
    int dot = key.lastIndexOf('.');
    return dot < 0 ? key + "-thumb" : key.substring(0, dot) + "-thumb" + key.substring(dot);
  }

  private static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("LISTING_NOT_FOUND", "No se encontró la publicación.");
  }
}
