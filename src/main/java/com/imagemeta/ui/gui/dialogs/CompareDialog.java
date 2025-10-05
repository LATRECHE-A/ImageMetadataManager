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
 * {@summary Modal dialog that compares the current directory contents against the latest saved snapshot.}
 * <p>
 * The dialog runs a snapshot comparison using {@link SnapshotManager} and renders a simple
 * textual report (new/modified/deleted files) into a read-only {@link JTextArea}, wrapped
 * in a scroll pane for comfortable reading.
 *
 * @implNote The dialog is constructed as a modal {@link JDialog} and centers relative
 *           to its parent frame. Any I/O or snapshot errors are logged and summarized
 *           in the text area for the user. 
 */
public class CompareDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color BG = new Color(30, 30, 30);
    private static final Color TEXT = Color.WHITE;
    private static final Color GREEN = new Color(76, 175, 80);
    private static final Color YELLOW = new Color(255, 193, 7);
    private static final Color RED = new Color(244, 67, 54);
    
    /**
     * Creates a modal snapshot-comparison dialog and initializes its content.
     *
     * @param parent    owner frame for modality and centering
     * @param directory directory to analyze and compare with the saved snapshot
     */
    public CompareDialog(Frame parent, Path directory) {
        super(parent, Localization.tr("dialog.snapshot.title"), true);
        initUI(directory);
        setSize(600, 500);
        setLocationRelativeTo(parent);
    }
    
    /**
     * Builds the UI, performs the snapshot comparison, and renders the report.
     *
     * @param directory directory used as the comparison target
     */
    private void initUI(Path directory) {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(BG);
        area.setForeground(TEXT);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        try {
            DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
            List<ImageFile> current = analyzer.listImageFiles();
            SnapshotManager manager = new SnapshotManager(directory);
            Map<String, List<Path>> comparison = manager.compareSnapshots(current);
            
            if (comparison.isEmpty()) {
                area.setText("No snapshot found to compare.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Snapshot Comparison\n");
                sb.append("═".repeat(50)).append("\n\n");
                
                List<Path> newFiles = comparison.get("Nouveau");
                List<Path> modified = comparison.get("Modifie");
                List<Path> deleted = comparison.get("Supprime");
                
                sb.append(String.format("✓ New files: %d%n", newFiles.size()));
                newFiles.forEach(p -> sb.append("  + ").append(p.getFileName()).append("\n"));
                
                sb.append(String.format("%n~ Modified files: %d%n", modified.size()));
                modified.forEach(p -> sb.append("  ~ ").append(p.getFileName()).append("\n"));
                
                sb.append(String.format("%n✗ Deleted files: %d%n", deleted.size()));
                deleted.forEach(p -> sb.append("  - ").append(p.getFileName()).append("\n"));
                
                area.setText(sb.toString());
            }
        } catch (Exception e) {
            Logger.error("Comparison failed", e);
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
}
