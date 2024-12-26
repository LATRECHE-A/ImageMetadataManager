package test;

/**
 * La classe ImageMetadata représente les métadonnées associées à une image, 
 * incluant ses dimensions, sa résolution, son titre, sa description, ses coordonnées GPS, 
 * et l'indication de la présence d'une miniature.
 * 
 * Cette classe fournit des getters pour accéder aux attributs des métadonnées
 * ainsi qu'une méthode {@code toString} pour une représentation textuelle.
 * 
 * @author DIALLO
 * @version 1.0
 */
public class ImageMetadata {
    private int width;
    private int height;
    private int dpi;
    private String title;
    private String description;
    private String gpsCoordinates;
    private boolean hasThumbnail;

    /**
     * Constructeur de la classe ImageMetadata
     * 
     * @param width la largeur de l'image en pixels
     * @param height la hauteur de l'image en pixels
     * @param dpi la résolution de l'image
     * @param title le titre de l'image
     * @param description une description de textuelle de l'image
     * @param gpsCoordinates les coordonnées GPS associées à l'image
     * @param hasThumbnail indique si l'image dispose d'une miniature
     */
    public ImageMetadata(int width, int height, int dpi, String title, String description, String gpsCoordinates, boolean hasThumbnail) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
        this.title = title;
        this.description = description;
        this.gpsCoordinates = gpsCoordinates;
        this.hasThumbnail = hasThumbnail;
    }

    /**
     * Retourne la larguer de l'image en pixels.
     * 
     * @return la largeur de l'image
     */
    public int getWidth() { return width; }
    
    /**
     * Retourne la hauteur de l'image en pixels.
     * 
     * @return la hauteur de l'image
     */
    public int getHeight() { return height; }
    
    /**
     * Retourne la résolution de l'image en points par pouce(DPI).
     * 
     * @return la résolution de l'image
     */
    public int getDpi() { return dpi; }
    
    /**
     * Retourne le titre de l'image.
     * 
     * @return le titre de l'image
     */
    public String getTitle() { return title; }
    
    /**
     * Retourne la description de l'image.
     * 
     * @return la description de l'image
     */
    public String getDescription() { return description; }
    
    /**
     * Retourne les coordonnées GPS associées à l'image, ou {@code null} si elles ne sont pas disponibles.
     * 
     * @return les cordonnées GPS sous forme de chaine
     */
    public String getGpsCoordinates() { return gpsCoordinates; }
    
    /**
     * Indique si l'image dispose d'une miniature.
     * 
     * @return {@code true} si une miniature est disponible, sinon {@code false}
     */
    public boolean hasThumbnail() { return hasThumbnail; }

    /**
     * Retourne une représentation textuelle des métadonnées de l'image.
     *
     * @return une chaîne décrivant les métadonnées de l'image
     */
    @Override
    public String toString() {
        return String.format("Dimensions: %dx%d, DPI: %d, Title: %s, Description: %s, GPS: %s, Thumbnail: %b",
                width, height, dpi, title, description, gpsCoordinates, hasThumbnail);
    }
}
