package test;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.MetadataException;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;

import java.io.File;
import java.io.IOException;

public class ExifMetadata extends ImageMetadata {

    // Constructeur
    public ExifMetadata(int width, int height, int dpi, String gps, String title, String description, boolean hasThumbnail) {
        super(width, height, dpi, gps, title, description, hasThumbnail);
    }

    public static ExifMetadata extract(File imageFile) throws IOException {
        // Vérification de la validité du fichier
        if (imageFile == null) {
            throw new IllegalArgumentException("Image file cannot be null");
        }

        if (!imageFile.exists()) {
            throw new IOException("Image file does not exist: " + imageFile.getPath());
        }

        // Variables pour stocker les données EXIF
        int width = 0;
        int height = 0;
        int dpi = 0;
        String title = null;
        String description = null;
        String gpsCoordinates = "N/A";
        boolean hasThumbnail = false;

        try {
            // Lire les métadonnées de l'image
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            // Afficher toutes les informations de répertoire et tags
            for (com.drew.metadata.Directory directory : metadata.getDirectories()) {
                System.out.println("Directory: " + directory.getName());
                for (com.drew.metadata.Tag tag : directory.getTags()) {
                    System.out.println("Tag: " + tag);
                }
            }

            // Extraire les informations EXIF spécifiques
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (exifDirectory != null) {
                System.out.println("ExifIFD0Directory found!");

                // Extraire les informations EXIF
                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_IMAGE_WIDTH)) {
                    width = exifDirectory.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
                    System.out.println("Width: " + width);
                }

                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_IMAGE_HEIGHT)) {
                    height = exifDirectory.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
                    System.out.println("Height: " + height);
                }

                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_X_RESOLUTION)) {
                    dpi = exifDirectory.getInt(ExifIFD0Directory.TAG_X_RESOLUTION);
                    System.out.println("DPI: " + dpi);
                }

                // Extraire le titre et la description (si présents)
                title = exifDirectory.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
                description = exifDirectory.getString(ExifIFD0Directory.TAG_DOCUMENT_NAME);
                System.out.println("Title: " + title);
                System.out.println("Description: " + description);
            } else {
                System.out.println("No ExifIFD0Directory found in EXIF data!");
            }
            
         // Extraire les informations GPS
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                GeoLocation location = gpsDirectory.getGeoLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    gpsCoordinates = "Latitude: " + latitude + ", Longitude: " + longitude;
                    System.out.println("GPS Coordinates: " + gpsCoordinates);
                }
            }

            // Vérifier la présence d'un mini-thumbnail dans ExifThumbnailDirectory
//            ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
//            if (thumbnailDirectory != null) {
//                // Récupérer les données du mini-thumbnail via le tag approprié
//                byte[] thumbnailData = thumbnailDirectory.getByteArray(ExifThumbnailDirectory.TAG_THUMBNAIL_IMAGE);
//                if (thumbnailData != null && thumbnailData.length > 0) {
//                    hasThumbnail = true;
//                    System.out.println("Thumbnail found!");
//                }
//            }
            
        } catch (ImageProcessingException | IOException | MetadataException e) {
            e.printStackTrace();
        }

        // Retourner une nouvelle instance d'ExifMetadata (qui est une sous-classe de ImageMetadata)
        return new ExifMetadata(width, height, dpi, title, description, gpsCoordinates, hasThumbnail);
    }
}
