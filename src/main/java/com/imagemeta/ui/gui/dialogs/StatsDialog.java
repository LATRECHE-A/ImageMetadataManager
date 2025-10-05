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
 * {@summary Modal dialog that summarizes directory statistics.}
 * <p>
 * Computes and displays counts, sizes, and common file types for a selected directory
 * using {@link DirectoryAnalyzer}. Output is rendered into a read-only {@link JTextArea}
 * within a {@link JScrollPane} for readability on smaller windows.
 */
public class StatsDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color BG = new Color(30, 30, 30);
    private static final Color TEXT = Color.WHITE;
    
    /**
     * Creates a modal statistics dialog for the given directory and initializes its UI.
     *
     * @param parent    owner frame for modality and centering
     * @param directory directory to analyze for statistics
     */
    public StatsDialog(Frame parent, Path directory) {
        super(parent, Localization.tr("dialog.stats.title"), true);
        initUI(directory);
        setSize(600, 500);
        setLocationRelativeTo(parent);
    }
    
    /**
     * Builds the UI, gathers directory statistics, and renders them in a monospaced layout.
     *
     * @param directory directory to analyze
     */
    private void initUI(Path directory) {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(BG);
        area.setForeground(TEXT);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        try {
            DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
            StringBuilder stats = new StringBuilder();
            stats.append("Directory Statistics\n");
            stats.append("═".repeat(50)).append("\n\n");
            stats.append(String.format("Total files: %d%n", analyzer.getTotalFileCount()));
            stats.append(String.format("Image files: %d%n", analyzer.getImageFileCount()));
            stats.append(String.format("Total size: %s%n", formatSize(analyzer.getTotalFileSize())));
            stats.append(String.format("Average size: %s%n", formatSize(analyzer.getAverageFileSize())));
            stats.append(String.format("Subdirectories: %d%n", analyzer.getSubdirectoryCount()));
            stats.append(String.format("Empty files: %d%n", analyzer.getEmptyFileCount()));
            stats.append(String.format("Most common type: %s%n", analyzer.getMostCommonFileType()));
            stats.append("\nFile types: ");
            stats.append(String.join(", ", analyzer.getFileExtensions()));
            
            area.setText(stats.toString());
        } catch (Exception e) {
            Logger.error("Stats failed", e);
            area.setText("Error loading statistics: " + e.getMessage());
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
     * Formats a byte size into a human-friendly string.
     *
     * @param bytes raw byte count
     * @return formatted size (e.g., {@code 2.6 MB})
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }
}
