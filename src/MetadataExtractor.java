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

        if (imageFile.exists()) {
            // Essayez d'extraire les métadonnées EXIF
            ExifMetadata exifMetadata = ExifMetadata.extract(imageFile);
            if (exifMetadata != null) {
            	System.out.println("Affichage des metadata exif");
                return exifMetadata;
            }

            XmpMetadata xmpMetadata = XmpMetadata.extractXmpMetadata(imageFile);
            if (xmpMetadata != null) {
            	System.out.println("Affichage des metadata xmp");
                return xmpMetadata;
            }
        }

        return null; 
    }
}
