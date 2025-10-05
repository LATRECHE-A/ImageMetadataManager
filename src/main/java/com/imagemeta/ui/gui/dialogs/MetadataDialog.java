package com.imagemeta.ui.gui.dialogs;

import com.imagemeta.core.*;
import com.imagemeta.metadata.*;
import com.imagemeta.util.i18n.Localization;
import com.imagemeta.util.logging.Logger;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * {@summary Modal dialog that shows a preview thumbnail and detailed image metadata.}
 * <p>
 * Displays file-level attributes from {@link ImageFile} alongside EXIF/XMP fields obtained
 * via {@link MetadataExtractor}. A scaled preview is attempted using {@link ImageIO#read}
 * and rendered at the top of the dialog; metadata is presented in a read-only text area.
 */
public class MetadataDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color BG = new Color(30, 30, 30);
    private static final Color TEXT = Color.WHITE;
    
    /**
     * Creates a modal metadata dialog for the given image path and initializes its UI.
     *
     * @param parent    owner frame for modality and centering
     * @param imagePath path to the image to inspect
     */
    public MetadataDialog(Frame parent, Path imagePath) {
        super(parent, "Metadata: " + imagePath.getFileName(), true);
        initUI(imagePath);
        setSize(550, 650);
        setLocationRelativeTo(parent);
    }
    
    /**
     * Builds the dialog UI, attempts to load a preview thumbnail, and renders file/metadata details.
     *
     * @param imagePath image path used for preview and metadata extraction
     */
    private void initUI(Path imagePath) {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(BG);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Thumbnail
        try {
            Image img = ImageIO.read(imagePath.toFile());
            if (img != null) {
                ImageIcon icon = new ImageIcon(img.getScaledInstance(400, -1, Image.SCALE_SMOOTH));
                JLabel imgLabel = new JLabel(icon);
                imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                main.add(imgLabel, BorderLayout.NORTH);
            }
        } catch (Exception e) {
            Logger.warn("Failed to load thumbnail", e);
        }
        
        // Metadata
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(BG);
        area.setForeground(TEXT);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        try {
            ImageFile imgFile = new ImageFile(imagePath);
            ImageMetadata meta = MetadataExtractor.extract(imagePath.toFile());
            
            StringBuilder sb = new StringBuilder();
            sb.append("File Information\n");
            sb.append("─".repeat(40)).append("\n");
            sb.append(String.format("Size: %s%n", formatSize(imgFile.getSize())));
            sb.append(String.format("Format: %s%n", imgFile.getFileFormat()));
            sb.append(String.format("MIME: %s%n", imgFile.getMimeType()));
            sb.append(String.format("Dimensions: %dx%d%n", imgFile.getWidth(), imgFile.getHeight()));
            sb.append(String.format("Modified: %s%n", imgFile.getLastModified()));
            
            if (meta != null) {
                sb.append("\nEXIF Metadata\n");
                sb.append("─".repeat(40)).append("\n");
                sb.append(String.format("DPI: %d%n", meta.getDpi()));
                sb.append(String.format("Title: %s%n", meta.getTitle() != null ? meta.getTitle() : "N/A"));
                sb.append(String.format("Description: %s%n", meta.getDescription() != null ? meta.getDescription() : "N/A"));
                sb.append(String.format("GPS: %s%n", meta.getGpsCoordinates()));
                sb.append(String.format("Thumbnail: %s%n", meta.hasThumbnail() ? "Yes" : "No"));
            }
            
            area.setText(sb.toString());
        } catch (Exception e) {
            Logger.error("Metadata extraction failed", e);
            area.setText("Error: " + e.getMessage());
        }
        
        main.add(new JScrollPane(area), BorderLayout.CENTER);
        
        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(33, 150, 243));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(BG);
        btnPanel.add(closeBtn);
        main.add(btnPanel, BorderLayout.SOUTH);
        
        setContentPane(main);
    }
    
    /**
     * Formats a byte size into human-readable IEC units (KiB, MiB, ...).
     *
     * @param bytes raw byte count
     * @return formatted string (e.g., {@code 13.5 MB})
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }
}
