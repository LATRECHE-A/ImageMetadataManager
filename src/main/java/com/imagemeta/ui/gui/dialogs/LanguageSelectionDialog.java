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
 * {@summary Modal dialog to select the application language (locale).}
 * <p>
 * Lists the locales advertised by {@link Localization#getSupported()}, preselects the current
 * locale, and applies the user's choice through {@link Localization#setLocale(Locale)}.
 * The dialog is always on top and blocks closing unless the user confirms a selection.
 */
public class LanguageSelectionDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private Locale selected;
    
    /**
     * Creates the language-selection dialog and initializes its UI.
     *
     * @param parent owner frame for modality and centering
     */
    public LanguageSelectionDialog(Frame parent) {
        super(parent, "Select Language / Choisir la langue", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        initUI();
        setSize(400, 250);
        setLocationRelativeTo(parent);
        setAlwaysOnTop(true);
    }

    /**
     * Builds the dialog layout with a radio-button list of supported locales and a confirm button.
     */
    private void initUI() {
        setLayout(new BorderLayout(20, 20));
        ((JPanel) getContentPane()).setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Choose your preferred language:");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JPanel langPanel = new JPanel();
        langPanel.setLayout(new BoxLayout(langPanel, BoxLayout.Y_AXIS));
        
        ButtonGroup group = new ButtonGroup();
        
        for (Locale locale : Localization.getSupported()) {
            JRadioButton radio = new JRadioButton(getDisplay(locale));
            radio.setFont(new Font("Arial", Font.PLAIN, 14));
            radio.setAlignmentX(Component.CENTER_ALIGNMENT);
            radio.addActionListener(e -> selected = locale);
            
            if (locale.equals(Localization.getCurrent())) {
                radio.setSelected(true);
                selected = locale;
            }
            
            group.add(radio);
            langPanel.add(radio);
            langPanel.add(Box.createVerticalStrut(10));
        }
        
        add(langPanel, BorderLayout.CENTER);

        JButton confirm = new JButton("Confirm / Confirmer");
        confirm.setFont(new Font("Arial", Font.BOLD, 14));
        confirm.addActionListener(e -> {
            if (selected != null) {
                Localization.setLocale(selected);
                dispose();
            }
        });
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(confirm);
        add(btnPanel, BorderLayout.SOUTH);
    }

    /**
     * Returns a readable label for the provided locale.
     *
     * @param locale the locale to display
     * @return a user-friendly language label
     */
    private String getDisplay(Locale locale) {
        switch (locale.getLanguage()) {
            case "en": return "English (EN)";
            case "fr": return "Français (FR)";
            case "es": return "Español (ES)";
            default: return locale.getDisplayLanguage();
        }
    }

    /**
     * Shows the dialog if the application requires a language selection at startup.
     *
     * @param parent owner frame for modality and centering
     */
    public static void showIfNeeded(Frame parent) {
        if (Localization.needsSelection()) {
            new LanguageSelectionDialog(parent).setVisible(true);
        }
    }
}
