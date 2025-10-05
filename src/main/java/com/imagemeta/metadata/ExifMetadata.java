package com.imagemeta.metadata;

/**
 * {@summary EXIF-specific {@link ImageMetadata} implementation.}
 * <p>
 * This type carries the same fields as {@link ImageMetadata} but indicates that the
 * values were sourced from EXIF directories (when available). It is useful for
 * downstream code that wants to preserve the provenance of extracted values.
 */
public class ExifMetadata extends ImageMetadata {
    
    /**
     * Creates an EXIF-backed metadata instance.
     *
     * @param width         image width in pixels
     * @param height        image height in pixels
     * @param dpi           dots-per-inch (if known; see notes below)
     * @param title         image title (may be {@code null})
     * @param description   image description (may be {@code null})
     * @param gps           GPS coordinates in a displayable form (e.g., {@code "lat, lon"}), or {@code "N/A"}
     * @param hasThumbnail  whether an EXIF thumbnail was detected
     * @implNote The effective unit of {@code dpi} depends on the EXIF {@code ResolutionUnit}
     *           (inches vs. centimeters). Some files omit Y-resolution or use different units. :contentReference[oaicite:2]{index=2}
     */
    public ExifMetadata(int width, int height, int dpi, String title, 
                       String description, String gps, boolean hasThumbnail) {
        super(width, height, dpi, title, description, gps, hasThumbnail);
    }

    /**
     * Returns a string whose prefix identifies this as EXIF-derived metadata,
     * followed by the standard {@link ImageMetadata} representation.
     */
    @Override
    public String toString() {
        return "ExifMetadata" + super.toString().substring(13);
    }
}
