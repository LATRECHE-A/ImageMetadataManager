package com.imagemeta.metadata;

/**
 * {@summary XMP-specific {@link ImageMetadata} implementation.}
 * <p>
 * This type mirrors {@link ImageMetadata} while indicating the values were sourced
 * from XMP data (when available). Useful for tracing provenance in pipelines.
 */
public class XmpMetadata extends ImageMetadata {
    
    /**
     * Creates an XMP-backed metadata instance.
     *
     * @param width         image width in pixels
     * @param height        image height in pixels
     * @param dpi           dots-per-inch (if known)
     * @param title         XMP title (may be {@code null} or {@code "N/A"})
     * @param description   XMP description (may be {@code null} or {@code "N/A"})
     * @param gps           GPS coordinates string, or {@code "N/A"}
     * @param hasThumbnail  whether a thumbnail was detected (typically {@code false} for XMP-only)
     */
    public XmpMetadata(int width, int height, int dpi, String title, 
                      String description, String gps, boolean hasThumbnail) {
        super(width, height, dpi, title, description, gps, hasThumbnail);
    }

    /**
     * Returns a string whose prefix identifies this as XMP-derived metadata,
     * followed by the standard {@link ImageMetadata} representation.
     */
    @Override
    public String toString() {
        return "XmpMetadata" + super.toString().substring(13);
    }
}
