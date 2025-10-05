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
import java.util.stream.Collectors;

/**
 * {@summary Modal dialog that gathers search criteria for filtering images.}
 * <p>
 * Collects name, year, and dimensions (WxH) filters and returns them to the caller
 * via a simple callback. A helper method is provided to execute the search immediately,
 * but typical usage is to pass the criteria back to the main UI for execution.
 */
public class SearchDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color BG = new Color(30, 30, 30);
    private static final Color PRIMARY = new Color(33, 150, 243);
    private transient final Path directory;
    private transient java.util.function.Consumer<Map<String, String>> searchCallback;
    
    /**
     * Creates a modal search dialog prefilled with any existing criteria.
     *
     * @param parent          owner frame for modality and centering
     * @param directory       directory used for optional internal searches
     * @param currentCriteria criteria to pre-populate in the form
     */
    public SearchDialog(Frame parent, Path directory, Map<String, String> currentCriteria) {
        super(parent, Localization.tr("dialog.search.title"), true);
        this.directory = directory;
        initUI(currentCriteria);
        setSize(500, 350);
        setLocationRelativeTo(parent);
    }
    
    /**
     * Registers a callback invoked when the user confirms a search.
     *
     * @param callback consumer receiving a map of criteria (name/year/dimensions)
     */
    public void setSearchCallback(java.util.function.Consumer<Map<String, String>> callback) {
        this.searchCallback = callback;
    }
    
    /**
     * Builds the form UI (name, year, dimensions) and confirm/close buttons.
     *
     * @param currentCriteria criteria used to prefill the fields
     */
    private void initUI(Map<String, String> currentCriteria) {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(BG);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel form = new JPanel(new GridLayout(4, 2, 10, 15));
        form.setBackground(BG);
        
        JTextField nameField = createField();
        JTextField yearField = createField();
        JTextField dimField = createField();
        
        // Pre-fill with current criteria
        if (currentCriteria.containsKey("name")) 
            nameField.setText(currentCriteria.get("name"));
        if (currentCriteria.containsKey("year")) 
            yearField.setText(currentCriteria.get("year"));
        if (currentCriteria.containsKey("dimensions")) 
            dimField.setText(currentCriteria.get("dimensions"));
        
        form.add(createLabel("File name:"));
        form.add(nameField);
        form.add(createLabel("Year (YYYY):"));
        form.add(yearField);
        form.add(createLabel("Dimensions (WxH):"));
        form.add(dimField);
        
        main.add(form, BorderLayout.CENTER);
        
        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(BG);
        
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(PRIMARY);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.addActionListener(e -> {
            Map<String, String> criteria = new HashMap<>();
            if (!nameField.getText().isEmpty()) 
                criteria.put("name", nameField.getText());
            if (!yearField.getText().isEmpty()) 
                criteria.put("year", yearField.getText());
            if (!dimField.getText().isEmpty()) 
                criteria.put("dimensions", dimField.getText());
            
            if (searchCallback != null) {
                searchCallback.accept(criteria);
            }
            dispose();
        });
        
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(100, 100, 100));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());
        
        btnPanel.add(searchBtn);
        btnPanel.add(closeBtn);
        main.add(btnPanel, BorderLayout.SOUTH);
        
        setContentPane(main);
    }
    
    /**
     * Creates a themed text field appropriate for dark backgrounds.
     *
     * @return configured {@link JTextField}
     */
    private JTextField createField() {
        JTextField field = new JTextField();
        field.setBackground(new Color(45, 45, 45));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }
    
    /**
     * Creates a simple label for the form.
     *
     * @param text label text
     * @return configured {@link JLabel}
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }
    
    /**
     * Executes a search immediately using the current directory (optional usage).
     * <p>
     * Typical flows return criteria to the owner via {@link #setSearchCallback(java.util.function.Consumer)}.
     *
     * @param criteria search filters to apply
     */
    private void performSearch(Map<String, String> criteria) {
        try {
            DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
            List<ImageFile> all = analyzer.listImageFiles();
            
            List<Path> results = all.stream()
                .filter(f -> matches(f, criteria))
                .map(ImageFile::getPath)
                .collect(Collectors.toList());
            
            showResults(results);
            
        } catch (Exception e) {
            Logger.error("Search failed", e);
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage());
        }
    }
    
    /**
     * Checks whether a file matches the provided filters.
     *
     * @param file     candidate image
     * @param criteria map with keys like {@code name}, {@code year}, {@code dimensions}
     * @return {@code true} if the file satisfies all filters
     */
    private boolean matches(ImageFile file, Map<String, String> criteria) {
        String name = criteria.get("name");
        if (name != null && !file.getPath().getFileName().toString()
                .toLowerCase().contains(name.toLowerCase())) {
            return false;
        }
        
        String year = criteria.get("year");
        if (year != null) {
            String fileYear = String.valueOf(file.getLastModified().getYear());
            if (!fileYear.equals(year)) return false;
        }
        
        String dims = criteria.get("dimensions");
        if (dims != null && dims.contains("x")) {
            String[] parts = dims.split("x");
            try {
                int w = Integer.parseInt(parts[0].trim());
                int h = Integer.parseInt(parts[1].trim());
                if (file.getWidth() != w || file.getHeight() != h) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Displays a simple dialog summarizing search results (first 20 entries).
     *
     * @param results matching file paths
     */
    private void showResults(List<Path> results) {
        StringBuilder msg = new StringBuilder("Found " + results.size() + " files:\n\n");
        results.stream().limit(20).forEach(p -> msg.append(p.getFileName()).append("\n"));
        if (results.size() > 20) msg.append("... and ").append(results.size() - 20).append(" more");
        
        JOptionPane.showMessageDialog(this, msg.toString());
        dispose();
    }
}
