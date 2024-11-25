package test;

public class ImageMetadata {
    private int width;
    private int height;
    private int dpi;
    private String title;
    private String description;
    private String gpsCoordinates;
    private boolean hasThumbnail;

    public ImageMetadata(int width, int height, int dpi, String title, String description, String gpsCoordinates, boolean hasThumbnail) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
        this.title = title;
        this.description = description;
        this.gpsCoordinates = gpsCoordinates;
        this.hasThumbnail = hasThumbnail;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDpi() { return dpi; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getGpsCoordinates() { return gpsCoordinates; }
    public boolean hasThumbnail() { return hasThumbnail; }

    @Override
    public String toString() {
        return String.format("Dimensions: %dx%d, DPI: %d, Title: %s, Description: %s, GPS: %s, Thumbnail: %b",
                width, height, dpi, title, description, gpsCoordinates, hasThumbnail);
    }
}
