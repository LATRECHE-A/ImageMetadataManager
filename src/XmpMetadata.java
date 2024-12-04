package test;

//import com.drew.imaging.ImageMetadataReader;
//import com.drew.metadata.Metadata;
//import com.drew.metadata.exif.ExifIFD0Directory;
//import com.drew.metadata.xmp.XMPMetadataDirectory;
//import com.drew.metadata.Tag;
//import com.drew.metadata.MetadataException;
//import com.adobe.xmp.XMPMeta;
//import com.adobe.xmp.options.ParseOptions;
//import com.adobe.xmp.impl.XMPMetaFactory;
//import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmpMetadata {

//    private List<String> xmpTags;
//
//    // Constructeur
//    public XmpMetadata(int width, int height, int dpi, String gps, boolean hasThumbnail, String title, String description, List<String> xmpTags) {
//        super(width, height, dpi, gps, title, description, hasThumbnail); // Assurez-vous que ImageMetadata attend ces paramètres
//        this.xmpTags = xmpTags;
//    }
//
//    // Getter pour les tags XMP
//    public List<String> getXmpTags() {
//        return xmpTags;
//    }
//
//    // Méthode d'extraction des métadonnées XMP
//    public static XmpMetadata extractXmpMetadata(File imageFile) throws IOException {
//        List<String> xmpTags = new ArrayList<>();
//        
//        // Par défaut, on initialise title et description
//        String title = "N/A";   // Valeur par défaut
//        String description = "N/A";   // Valeur par défaut
//        
//        try {
//            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
//            XMPMetadataDirectory xmpDirectory = metadata.getFirstDirectoryOfType(XMPMetadataDirectory.class);
//            
//            if (xmpDirectory != null) {
//                for (Tag tag : xmpDirectory.getTags()) {
//                    xmpTags.add(tag.getTagName() + ": " + tag.getDescription());
//                    
//                    // Exemple de récupération de title et description XMP
//                    if ("Title".equals(tag.getTagName())) {
//                        title = tag.getDescription();
//                    }
//                    if ("Description".equals(tag.getTagName())) {
//                        description = tag.getDescription();
//                    }
//                }
//            }
//
//            // Initialiser les autres métadonnées, comme la taille, le DPI, GPS, etc.
//            int width = 0;
//            int height = 0;
//            int dpi = 0;
//            String gps = "N/A";
//            boolean hasThumbnail = false;
//
//            return new XmpMetadata(width, height, dpi, title, description, gps, hasThumbnail, xmpTags);
//            
//        } catch (MetadataException e) {
//            throw new IOException("Failed to extract XMP metadata: " + e.getMessage(), e);
//        }
//    }
//
//    // Méthode pour afficher les tags XMP (pour tester)
//    public void printXmpTags() {
//        for (String tag : xmpTags) {
//            System.out.println(tag);
//        }
//    }
}