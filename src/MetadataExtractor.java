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

/**
 * La classe MetadataExtractor fournit une méthode pour extraire les métadonnées
 * d'un fichier image, en retournant une instance de {@link ImageMetadata} ou
 * une de ses sous-classes ({@link ExifMetadata} ou {@link XmpMetadata}).
 *
 * Cette classe s'appuie sur la bibliothèque "Metadata Extractor" pour analyser
 * différents types de métadonnées, y compris les métadonnées EXIF et XMP.
 * 
 * @author DIALLO
 * @version 1.0
 */
public class MetadataExtractor {
	/**
	 * Extrait les métadonnées d'un fichier image. Si les métadonnées EXIF ne sont pas disponibles,
     * cette méthode tente d'extraire les métadonnées XMP.
     *
     * @param imageFile le fichier image dont les métadonnées doivent être extraites
     * @return une instance de {@link ImageMetadata} représentant les métadonnées extraites, 
     *         ou {@code null} si aucune métadonnée n'a pu être extraite
     * @throws IOException si le fichier est introuvable ou illisible
     * @throws IllegalArgumentException si le fichier fourni est {@code null}
	 */
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
