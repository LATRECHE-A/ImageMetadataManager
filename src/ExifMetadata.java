package test;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.MetadataException;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

/**
 * La classe ExifMetadata étend {@link ImageMetadata} pour inclure les métadonnées EXIF spécifiques
 * des fichiers image. Elle permet d'extraire des informations détaillées telles que les dimensions,
 * la résolution, le titre, la description, les coordonnées GPS, et la présence d'une miniature.
 *
 * Cette classe s'appuie sur la bibliothèque "Metadata Extractor" pour lire les métadonnées EXIF.
 * 
 * @author DIALLO
 * @version 1.0
 * @see ImageMetadata
 * @see <a href="https://github.com/drewnoakes/metadata-extractor">Metadata Extractor</a>
 */
public class ExifMetadata extends ImageMetadata {

    /**
     * Constructeur de la classe ExifMetadata.
	 *
	 * @param width la largeur de l'image en pixels
	 * @param height la hauteur de l'image en pixels
	 * @param dpi la résolution de l'image en DPI (points par pouce)
	 * @param title le titre de l'image, s'il est défini dans les métadonnées
	 * @param description une description textuelle de l'image, s'il est définie
	 * @param gps les coordonnées GPS sous forme de chaîne, ou "N/A" si non disponibles
	 * @param hasThumbnail indique si une miniature est présente dans les métadonnées
	 * 
	 * @see test.ImageMetadata#ImageMetadata(int, int, int, String, String, String, boolean)
     */
    public ExifMetadata(int width, int height, int dpi, String title, String description, String gps, boolean hasThumbnail) {
        super(width, height, dpi, title, description, gps, hasThumbnail);
    }

    /**
     * Extrait les métadonnées EXIF d'un fichier image et crée une instance d'ExifMetadata.
     *
     * <p>Cette méthode utilise la bibliothèque "Metadata Extractor" pour analyser les métadonnées
     * du fichier image et récupérer des informations telles que les dimensions, la résolution,
     * le titre, la description, les coordonnées GPS, et la présence d'une miniature.</p>
     *
     * @param imageFile le fichier image à analyser
     * @return une instance d'ExifMetadata contenant les métadonnées extraites, ou {@code null} en cas d'échec
     * @throws IOException si le fichier est introuvable ou illisible
     * @throws IllegalArgumentException si le fichier est nul
     * 
     * @see <a href="https://github.com/drewnoakes/metadata-extractor">Metadata Extractor Documentation</a>
     * @see ExifMetadata
     */
    public static ExifMetadata extract(File imageFile) throws IOException {
        // Vérification de la validité du fichier
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
                // Get DPI if available
                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_X_RESOLUTION)) {
                    dpi = exifDirectory.getInt(ExifIFD0Directory.TAG_X_RESOLUTION);
                }
                
                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_IMAGE_WIDTH)) {
                    width = exifDirectory.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
                    //System.out.println("Width: " + width);
                }

                if (exifDirectory.containsTag(ExifIFD0Directory.TAG_IMAGE_HEIGHT)) {
                    height = exifDirectory.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
                    //System.out.println("Height: " + height);
                }
                
                // Get title and description if available
                title = exifDirectory.getString(ExifIFD0Directory.TAG_DOCUMENT_NAME);
                description = exifDirectory.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
            }
            
            ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFDDirectory != null) {
                if (subIFDDirectory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)) {
                    width = subIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
                    //System.out.println("Width (from SubIFD): " + width);
                }

                if (subIFDDirectory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)) {
                    height = subIFDDirectory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
                    //System.out.println("Height (from SubIFD): " + height);
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

            hasThumbnail = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class) != null;
            
            if (width == 0 || height == 0) {
                try {
                    BufferedImage bufferedImage = ImageIO.read(imageFile);
                    if (bufferedImage != null) {
                        width = bufferedImage.getWidth();
                        height = bufferedImage.getHeight();
                        //System.out.println("Dimensions récupérées avec ImageIO : " + width + "x" + height);
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors de la lecture des dimensions avec ImageIO : " + e.getMessage());
                }
            }
            
            return new ExifMetadata(width, height, dpi, title, description, gps, hasThumbnail);
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        }catch (MetadataException e) {
        	e.printStackTrace();
        }catch (IOException e) {
        	e.printStackTrace();
        }

        return null;
    }
}