package com.imagemeta.metadata;

import com.imagemeta.util.logging.Logger;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.*;
import com.drew.metadata.xmp.XmpDirectory;
import com.drew.lang.GeoLocation;
import java.io.File;

/**
 * {@summary Centralized metadata extraction with EXIF-first, XMP-fallback strategy.}
 * <p>
 * Uses the {@code metadata-extractor} library to read metadata from image files.
 * EXIF directories are attempted first; if insufficient, a minimal XMP read is
 * performed. Returns {@code null} when no usable metadata is available or an error occurs. :contentReference[oaicite:3]{index=3}
 *
 * @implNote EXIF access relies on {@link ExifIFD0Directory}, {@link ExifSubIFDDirectory},
 *           {@link GpsDirectory}, and {@link ExifThumbnailDirectory}. XMP access uses
 *           {@link XmpDirectory}. Availability varies per file. :contentReference[oaicite:4]{index=4}
 */
public final class MetadataExtractor {
    
    /** Prevents instantiation. */
    private MetadataExtractor() {}
    
    /**
     * Extracts image metadata from the given file.
     *
     * @param imageFile the image file; must exist
     * @return an {@link ImageMetadata} instance, or {@code null} if unavailable/invalid
     */
    public static ImageMetadata extract(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            Logger.warn("Invalid image file for metadata extraction");
            return null;
        }

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            
            // Try EXIF first
            ImageMetadata exif = extractExif(metadata);
            if (exif != null && exif.getWidth() > 0) {
                Logger.debug("Extracted EXIF metadata from {}", imageFile.getName());
                return exif;
            }
            
            // Fallback to XMP
            ImageMetadata xmp = extractXmp(metadata);
            if (xmp != null) {
                Logger.debug("Extracted XMP metadata from {}", imageFile.getName());
                return xmp;
            }
            
            Logger.info("No metadata available for {}", imageFile.getName());
            return null;
            
        } catch (Exception e) {
            Logger.error("Failed to extract metadata from {}", imageFile.getName(), e);
            return null;
        }
    }
    
    /**
     * Attempts to extract key fields from EXIF directories.
     * <ul>
     *   <li>Dimensions from {@link ExifSubIFDDirectory}.</li>
     *   <li>Resolution (DPI), title/description from {@link ExifIFD0Directory}.</li>
     *   <li>GPS from {@link GpsDirectory}.</li>
     *   <li>Thumbnail presence from {@link ExifThumbnailDirectory}.</li>
     * </ul>
     * @param metadata parsed metadata tree
     * @return populated {@link ImageMetadata}, or {@code null} if EXIF is missing/unreadable
     */
    private static ImageMetadata extractExif(Metadata metadata) {
        try {
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            ExifThumbnailDirectory thumb = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
            
            int width = 0, height = 0, dpi = 0;
            String title = null, description = null, gpsCoords = "N/A";
            boolean hasThumbnail = thumb != null;
            
            if (subIfd != null) {
                width = getIntTag(subIfd, ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
                height = getIntTag(subIfd, ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
            }
            
            if (ifd0 != null) {
                // Note: X/Y resolution units depend on the EXIF ResolutionUnit tag. :contentReference[oaicite:5]{index=5}
                dpi = getIntTag(ifd0, ExifIFD0Directory.TAG_X_RESOLUTION);
                title = getStringTag(ifd0, ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
                description = getStringTag(ifd0, ExifIFD0Directory.TAG_DOCUMENT_NAME);
            }
            
            if (gps != null) {
                GeoLocation location = gps.getGeoLocation();
                if (location != null) {
                    gpsCoords = String.format("%.6f, %.6f", 
                        location.getLatitude(), location.getLongitude());
                }
            }
            
            return new ImageMetadata(width, height, dpi, title, description, gpsCoords, hasThumbnail);
            
        } catch (Exception e) {
            Logger.debug("Failed to extract EXIF: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Minimal XMP extraction used when EXIF is absent or insufficient.
     * <p>
     * The {@link XmpDirectory} exposes XMP via {@code XMPMeta}; tag names and paths depend
     * on namespaces and may vary across tools. This implementation scans for common
     * {@code Title}/{@code Description} entries and leaves dimensions/DPI as unknown. :contentReference[oaicite:6]{index=6}
     *
     * @param metadata parsed metadata tree
     * @return an {@link ImageMetadata} with title/description if found; otherwise {@code null}
     */
    private static ImageMetadata extractXmp(Metadata metadata) {
        try {
            XmpDirectory xmp = metadata.getFirstDirectoryOfType(XmpDirectory.class);
            if (xmp == null) return null;
            
            String title = "N/A", description = "N/A";
            
            for (var tag : xmp.getTags()) {
                String name = tag.getTagName();
                if ("Title".equals(name)) {
                    title = tag.getDescription();
                } else if ("Description".equals(name)) {
                    description = tag.getDescription();
                }
            }
            
            return new ImageMetadata(0, 0, 0, title, description, "N/A", false);
            
        } catch (Exception e) {
            Logger.debug("Failed to extract XMP: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Safely reads an integer tag from a directory, returning {@code 0} if unavailable.
     *
     * @param dir the EXIF directory
     * @param tag the tag identifier
     * @return the tag value, or {@code 0} on error/missing
     */
    private static int getIntTag(com.drew.metadata.Directory dir, int tag) {
        try {
            if (dir.containsTag(tag)) {
                return dir.getInt(tag);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
    
    /**
     * Safely reads a string tag from a directory, returning {@code null} if unavailable.
     *
     * @param dir the directory
     * @param tag the tag identifier
     * @return the tag value, or {@code null} on error/missing
     */
    private static String getStringTag(com.drew.metadata.Directory dir, int tag) {
        try {
            return dir.getString(tag);
        } catch (Exception e) {
            return null;
        }
    }
}
