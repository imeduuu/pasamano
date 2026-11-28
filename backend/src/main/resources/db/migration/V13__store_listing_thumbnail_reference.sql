ALTER TABLE listing_images ADD COLUMN thumbnail_reference varchar(500);

UPDATE listing_images
SET thumbnail_reference = regexp_replace(image_reference, '(\.[^./]+)?$', '-thumb\1');

ALTER TABLE listing_images ALTER COLUMN thumbnail_reference SET NOT NULL;
