package test;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.xmp.XmpDirectory;
import com.drew.metadata.Tag;
import com.drew.metadata.MetadataException;
import com.drew.imaging.ImageProcessingException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * La classe XmpMetadata étend {@link ImageMetadata} pour inclure les métadonnées XMP spécifiques
 * des fichiers image. Elle permet  
 * 
 * @author DIALLO
 * @version 1.0
 * @see ImageMetadata
 * @see <a href="https://github.com/drewnoakes/metadata-extractor">Metadata Extractor</a>
 */
public class XmpMetadata extends ImageMetadata{
	/**
	 * Constructeur de la classe XmpMetadata.
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
    public XmpMetadata(int width, int height, int dpi, String title, String description, String gps, boolean hasThumbnail) {
    	super(width, height, dpi, title, description, gps, hasThumbnail);
    }

    /**
     * Extrait les métadonnées XMP d'un fichier image et crée une instance de {@link XmpMetadata}.
     *
     * <p>Cette méthode utilise la bibliothèque "Metadata Extractor" pour lire les métadonnées XMP,
     * telles que le titre et la description de l'image. Si les métadonnées XMP ne sont pas disponibles,
     * des valeurs par défaut ("N/A") sont utilisées pour le titre et la description.</p>
     *
     * @param imageFile le fichier image à analyser
     * @return une instance de {@link XmpMetadata} contenant les métadonnées XMP extraites
     * @throws IOException si une erreur de lecture se produit ou si le fichier est introuvable
     */
    public static XmpMetadata extractXmpMetadata(File imageFile) throws IOException {
        String title = "N/A"; 
        String description = "N/A";
        
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            XmpDirectory xmpDirectory = metadata.getFirstDirectoryOfType(XmpDirectory.class);
            
            if (xmpDirectory != null) {
                for (Tag tag : xmpDirectory.getTags()) {
                    if ("Title".equals(tag.getTagName())) {
                        title = tag.getDescription();
                    }
                    if ("Description".equals(tag.getTagName())) {
                        description = tag.getDescription();
                    }
                }
            }

            int width = 0;
            int height = 0;
            int dpi = 0;
            String gps = "N/A";
            boolean hasThumbnail = false;

            return new XmpMetadata(width, height, dpi, title, description, gps, hasThumbnail);
            
        } catch (ImageProcessingException | IOException e) {
            throw new IOException("Failed to extract XMP metadata: " + e.getMessage(), e);
        }
    }
}