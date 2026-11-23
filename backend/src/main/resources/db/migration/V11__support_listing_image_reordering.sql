ALTER TABLE listing_images DROP CONSTRAINT uq_listing_images_position;
ALTER TABLE listing_images
  ADD CONSTRAINT uq_listing_images_position UNIQUE (listing_id, position);
ALTER TABLE listing_images DROP CONSTRAINT listing_images_position_check;
ALTER TABLE listing_images
  ADD CONSTRAINT listing_images_position_check CHECK (position BETWEEN 0 AND 15);
