package com.imagemeta.metadata;

import java.util.Objects;

/**
 * {@summary Base, immutable container for common image metadata fields.}
 * <p>
 * Instances capture generic information that can be populated from EXIF, XMP, or other
 * sources. All fields are set via the constructor and are safe to share across threads.
 */
public class ImageMetadata {
    private final int width;
    private final int height;
    private final int dpi;
    private final String title;
    private final String description;
    private final String gpsCoordinates;
    private final boolean hasThumbnail;

    /**
     * Creates a new immutable metadata instance.
     *
     * @param width          image width in pixels (0 if unknown)
     * @param height         image height in pixels (0 if unknown)
     * @param dpi            dots-per-inch (0 if unknown)
     * @param title          optional title; may be {@code null}
     * @param description    optional description; may be {@code null}
     * @param gpsCoordinates optional coordinates as a human-readable string (e.g., {@code "48.856613, 2.352222"}), or {@code "N/A"}
     * @param hasThumbnail   whether a thumbnail was detected in the metadata
     */
    public ImageMetadata(int width, int height, int dpi, String title, 
                        String description, String gpsCoordinates, boolean hasThumbnail) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
        this.title = title;
        this.description = description;
        this.gpsCoordinates = gpsCoordinates;
        this.hasThumbnail = hasThumbnail;
    }

    /** @return image width in pixels (0 if unknown) */
    public int getWidth() { return width; }
    /** @return image height in pixels (0 if unknown) */
    public int getHeight() { return height; }
    /** @return dots-per-inch (0 if unknown) */
    public int getDpi() { return dpi; }
    /** @return title, or {@code null} */
    public String getTitle() { return title; }
    /** @return description, or {@code null} */
    public String getDescription() { return description; }
    /** @return GPS coordinates string, or {@code "N/A"} */
    public String getGpsCoordinates() { return gpsCoordinates; }
    /** @return {@code true} if a thumbnail is present in metadata */
    public boolean hasThumbnail() { return hasThumbnail; }

    /**
     * Returns a concise summary of key fields, suitable for logs or diagnostics.
     */
    @Override
    public String toString() {
        return String.format("ImageMetadata[%dx%d, DPI=%d, GPS=%s, thumbnail=%b]",
            width, height, dpi, gpsCoordinates, hasThumbnail);
    }

    /**
     * Structural equality across all fields. Titles/descriptions are compared
     * using {@link Objects#equals(Object, Object)} to tolerate {@code null}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageMetadata)) return false;
        ImageMetadata that = (ImageMetadata) o;
        return width == that.width && height == that.height && 
               dpi == that.dpi && hasThumbnail == that.hasThumbnail &&
               Objects.equals(title, that.title) &&
               Objects.equals(description, that.description) &&
               Objects.equals(gpsCoordinates, that.gpsCoordinates);
    }

    /** @return a hash across all fields */
    @Override
    public int hashCode() {
        return Objects.hash(width, height, dpi, title, description, gpsCoordinates, hasThumbnail);
    }
}
