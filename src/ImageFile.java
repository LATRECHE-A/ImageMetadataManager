package test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.imageio.ImageIO;

/**
 * La classe ImageFile représente un fichier image et extrait ses métadonnées, 
 * telles que le type MIME, la taille, les dimensions, les dates de modification 
 * et de création, ainsi que son format.
 *
 * Cette classe est conçue pour analyser les propriétés des fichiers image, ce qui 
 * la rend utile dans les applications nécessitant des informations détaillées sur les images.
 *
 * @author DIALLO
 * @version 1.0 
 */
public class ImageFile {
    private final Path path;
    private final long size;
    private final String mimeType;
    private final LocalDateTime lastModified;
    private final LocalDateTime creationDate;
    private final String fileFormat;
    private final int width;
    private final int height;
    
    
    /**
     * Initialise une instance de ImageFile en lisant les métadonnées du fichier spécifié.
     * 
     * @param path le chemin du fichier image
     * @throws IOException si le fichier ne peut pas être lu ou si l'image est invalide
     */
    public ImageFile(Path path) throws IOException {
        this.path = path;
        this.size = Files.size(path);
        this.mimeType = Files.probeContentType(path);

        // File attributes
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        this.lastModified = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        this.creationDate = attr.creationTime() != null
                ? LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault())
                : null;

        // Image-specific attributes
        BufferedImage image = ImageIO.read(path.toFile());
        if (image != null) {
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.fileFormat = getFileExtension(path.toString());
        } else {
            this.width = 0;
            this.height = 0;
            this.fileFormat = "unknown";
        }
    }

    // Helper method to get the file extension
    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return (lastIndex == -1) ? "unknown" : fileName.substring(lastIndex + 1).toLowerCase();
    }

    /**
     * Retourne le chemin du fichier image.
     * 
     * @return le chemin du fichier sous forme d'objet {@link Path}
     */
    public Path getPath() {
        return path;
    }

    /**
     * Retourne la taille du fichier en octets
     * 
     * @return la taille du fichier 
     */
    public long getSize() {
        return size;
    }

    /**
     * Retourne le type MIME du fichier.
     * 
     * @return le type MIME
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Retourne la date de dernière modification du fichier.
     * 
     * @return un objet {@link LocalDateTime} représentant la dernière date de modification
     */
    public LocalDateTime getLastModified() {
        return lastModified;
    }

    /**
     * Retourne la date de création.
     * 
     * @return un objet {@link LocalDateTime} représentant la date de création
     */
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    /**
     * Retourne le format du fichier image.
     * 
     * @return le format du fichier sous forme de chaine
     */
    public String getFileFormat() {
        return fileFormat;
    }
    
    /**
     * Retourne la largeur de l'image en pixels.
     * 
     * @return la largeur de l'image
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retourne la hauteur de l'iamge en pixels.
     * 
     * @return la hauteur de l'image
     */
    public int getHeight() {
        return height;
    }

    /**
     * Retourne les dimensions de l'image sous la forme "largeur x hauteur".
     * 
     * @return une chaine représentant les dimensions de l'image
     */
    public String getDimensions() {
        return width + "x" + height;
    }
}