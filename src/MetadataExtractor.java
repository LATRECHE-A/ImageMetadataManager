package test;

import java.io.File;
import java.io.IOException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class MetadataExtractor {
	public static ImageMetadata extract(File imageFile) throws IOException {
        if (imageFile == null) {
            throw new IllegalArgumentException("Image file cannot be null");
        }
        
        if (!imageFile.exists()) {
            throw new IOException("Image file does not exist: " + imageFile.getPath());
        }

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            
            // Default values in case metadata is not available
            int width = 0;
            int height = 0;
            int dpi = 0;
            String title = null;
            String description = null;
            String gps = "N/A";
            boolean hasThumbnail = false;
            byte[] thumbnailData = null;
            
            if (exifDirectory != null) {
                // Get dimensions if available
//                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_IMAGE_WIDTH)) {
//                    width = exifDirectory.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
//                }
//                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_IMAGE_HEIGHT)) {
//                    height = exifDirectory.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
//                }
            	
            	for (com.drew.metadata.Directory directory : metadata.getDirectories()) {
                    System.out.println("Directory: " + directory.getName());
                    for (com.drew.metadata.Tag tag : directory.getTags()) {
                        System.out.println("Tag: " + tag);
                    }
                }
                
                // Get DPI if available
                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_X_RESOLUTION)) {
                    dpi = exifDirectory.getInt(ExifIFD0Directory.TAG_X_RESOLUTION);
                }
                
                // Get title and description if available
                title = exifDirectory.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
                description = exifDirectory.getString(ExifIFD0Directory.TAG_DOCUMENT_NAME);
            }
            
            ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFDDirectory != null) {
                if (subIFDDirectory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)) {
                    width = subIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
                    System.out.println("Width (from SubIFD): " + width);
                }

                if (subIFDDirectory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)) {
                    height = subIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
                    System.out.println("Height (from SubIFD): " + height);
                }
            } else {
                System.out.println("No ExifSubIFDDirectory found in EXIF data!");
            }
            
            // Handle GPS data
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                StringBuilder gpsBuilder = new StringBuilder();
                
                // Using the correct GPS tag constants
                if (gpsDirectory.containsTag(GpsDirectory.TAG_LATITUDE) && 
                    gpsDirectory.containsTag(GpsDirectory.TAG_LONGITUDE)) {
                    
                    String latitude = gpsDirectory.getDescription(GpsDirectory.TAG_LATITUDE);
                    String longitude = gpsDirectory.getDescription(GpsDirectory.TAG_LONGITUDE);
                    
                    if (latitude != null && longitude != null) {
                        gpsBuilder.append(latitude).append(", ").append(longitude);
                        gps = gpsBuilder.toString();
                    }
                }
            }

            // Check for thumbnail using ExifThumbnailDirectory
            hasThumbnail = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class) != null;
            
//            ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
//            if (thumbnailDirectory != null) {                  
//                thumbnailData = thumbnailDirectory.getByteArray(ExifThumbnailDirectory.TAG_THUMBNAIL_IMAGE);
//                hasThumbnail = thumbnailData != null;
//            }
            
            return new ImageMetadata(width, height, dpi, title, description, gps, hasThumbnail);
            
        } catch (ImageProcessingException e) {
            throw new IOException("Failed to process image metadata: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected error while extracting metadata: " + e.getMessage(), e);
        }
    }
}
