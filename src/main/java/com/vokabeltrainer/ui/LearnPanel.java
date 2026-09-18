package com.vokabeltrainer.ui;

import com.vokabeltrainer.db.DatabaseHelper;
import com.vokabeltrainer.model.Category;
import com.vokabeltrainer.model.Vokabel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class LearnPanel extends JPanel {
    private DatabaseHelper dbHelper;
    private VokabelTrainerApp mainApp;

    // ========== UI-Komponenten ==========
    private JRadioButton rbDirection1;
    private JRadioButton rbDirection2;
    private ButtonGroup directionGroup;
    private JRadioButton rbSetAll;
    private JRadioButton rbSetErrors;
    private ButtonGroup setGroup;
    private JLabel lblCategory;
    private JComboBox<Category> cbCategoryChoice;
    private JCheckBox cbEffectless;
    private JLabel lblEffectless;
    private JCheckBox cbOldestFirst;
    private JLabel lblOldestFirst;
    private JButton btnStart;

    // Unterer Bereich
    private JPanel bottomPanel;
    private JLabel lblQuestion;
    private JTextField tfQuestion;
    private JButton btnLoudspeaker;
    private JLabel lblAnswer;
    private JTextField tfAnswer;
    private JLabel lblState;
    private JButton btnVerify;
    private JLabel lblCategories;
    private JList<String> lstVocCategories;

    // Lernlogik
    private List<Vokabel> lernVokabeln = new ArrayList<>();
    private List<Vokabel> aktuelleVokabeln = new ArrayList<>();
    private Vokabel aktuelleVokabel;
    private int aktuelleIndex = 0;
    private boolean zeigeDeutsch = true;
    private int richtigeAntworten = 0;
    private int falscheAntworten = 0;
    private boolean noneEffective = false;
    private boolean oldestFirst = false;

    public LearnPanel(DatabaseHelper dbHelper, VokabelTrainerApp mainApp) {
        this.dbHelper = dbHelper;
        this.mainApp = mainApp;
        initializeUI();
        loadCategories();
        loadVokabeln();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Richtungsauswahl
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JPanel directionPanel = new JPanel(new BorderLayout());
        directionPanel.setBorder(BorderFactory.createTitledBorder("Richtung"));
        directionGroup = new ButtonGroup();

        rbDirection1 = new JRadioButton(getPaddedString("Deutsch", "Englisch", 15) + " -> Englisch", true);
        rbDirection2 = new JRadioButton(getPaddedString("Englisch", "Deutsch", 15) + " -> Deutsch");
        directionGroup.add(rbDirection1);
        directionGroup.add(rbDirection2);

        directionPanel.add(rbDirection1, BorderLayout.NORTH);
        directionPanel.add(rbDirection2, BorderLayout.SOUTH);
        mainPanel.add(directionPanel, gbc);

        // Set-Auswahl
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JPanel setPanel = new JPanel(new BorderLayout());
        setPanel.setBorder(BorderFactory.createTitledBorder("Set"));
        setGroup = new ButtonGroup();

        rbSetAll = new JRadioButton("Alle", true);
        rbSetErrors = new JRadioButton("Fehlerhafte");
        setGroup.add(rbSetAll);
        setGroup.add(rbSetErrors);

        setPanel.add(rbSetAll, BorderLayout.NORTH);
        setPanel.add(rbSetErrors, BorderLayout.SOUTH);
        mainPanel.add(setPanel, gbc);

        // Kategorie
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        JPanel categoryPanel = new JPanel(new BorderLayout());
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Kategorie"));

        lblCategory = new JLabel("Kategorie:");
        cbCategoryChoice = new JComboBox<>();
        cbCategoryChoice.addItem(new Category(0, "Alle")); // Standardwert
        categoryPanel.add(lblCategory, BorderLayout.NORTH);
        categoryPanel.add(cbCategoryChoice, BorderLayout.SOUTH);
        mainPanel.add(categoryPanel, gbc);

        // Optionen
        gbc.gridx = 1; gbc.gridy = 2;
        JPanel optionsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Optionen"));

        JPanel effectlessPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cbEffectless = new JCheckBox("Dry Run");
        lblEffectless = new JLabel("Keine Fehlerzähler-Änderung");
        effectlessPanel.add(cbEffectless);
        effectlessPanel.add(lblEffectless);

        JPanel oldestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cbOldestFirst = new JCheckBox("Oldest First");
        lblOldestFirst = new JLabel("Älteste zuerst");
        oldestPanel.add(cbOldestFirst);
        oldestPanel.add(lblOldestFirst);

        optionsPanel.add(effectlessPanel);
        optionsPanel.add(oldestPanel);
        mainPanel.add(optionsPanel, gbc);

        // Start-Button
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        btnStart = new JButton("Start");
        btnStart.addActionListener(e -> toggleLearning());
        mainPanel.add(btnStart, gbc);

        add(mainPanel, BorderLayout.NORTH);
        createBottomPanel();
    }

    private String getPaddedString(String s1, String s2, int length) {
        int maxLen = Math.max(s1.length(), s2.length());
        int padding1 = maxLen - s1.length();
        int padding2 = maxLen - s2.length();
        return s1 + " ".repeat(padding1) + " -> " + s2;
    }

    private void createBottomPanel() {
        bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Lernvorgang"));
        bottomPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        lblQuestion = new JLabel("Frage:");
        bottomPanel.add(lblQuestion, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        tfQuestion = new JTextField(30);
        tfQuestion.setEditable(false);
        tfQuestion.setFont(new Font("Arial", Font.BOLD, 16));
        bottomPanel.add(tfQuestion, gbc);

        gbc.gridx = 3; gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        btnLoudspeaker = new JButton("🔊");
        btnLoudspeaker.setToolTipText("Sprachausgabe (noch nicht implementiert)");
        bottomPanel.add(btnLoudspeaker, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lblAnswer = new JLabel("Antwort:");
        bottomPanel.add(lblAnswer, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        tfAnswer = new JTextField(30);
        tfAnswer.setFont(new Font("Arial", Font.PLAIN, 16));
        tfAnswer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    verifyVoc();
                }
            }
        });
        bottomPanel.add(tfAnswer, gbc);

        gbc.gridx = 3; gbc.gridy = 3; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        lblState = new JLabel("0");
        lblState.setFont(new Font("Arial", Font.BOLD, 14));
        bottomPanel.add(lblState, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        btnVerify = new JButton("Überprüfen");
        btnVerify.addActionListener(e -> verifyVoc());
        bottomPanel.add(btnVerify, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        lblCategories = new JLabel("Kategorien:");
        bottomPanel.add(lblCategories, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 4;
        lstVocCategories = new JList<>();
        lstVocCategories.setBackground(new Color(220, 220, 220));
        lstVocCategories.setVisibleRowCount(5);
        bottomPanel.add(new JScrollPane(lstVocCategories), gbc);

        add(bottomPanel, BorderLayout.CENTER);
    }

    private void loadCategories() {
        SwingUtilities.invokeLater(() -> {
            List<Category> categories = dbHelper.getAllCategories();
            cbCategoryChoice.removeAllItems();
            cbCategoryChoice.addItem(new Category(0, "Alle"));
            for (Category cat : categories) {
                cbCategoryChoice.addItem(cat);
            }
            // Standardauswahl setzen
            if (cbCategoryChoice.getItemCount() > 0) {
                cbCategoryChoice.setSelectedIndex(0);
            }
        });
    }

    private void loadVokabeln() {
        // Variablen OUTSIDE des Lambdas deklarieren
        final String set = rbSetAll.isSelected() ? "All" : "Errors";
        final boolean lang1ToLang2 = rbDirection1.isSelected();
        Category selectedCategory = (Category) cbCategoryChoice.getSelectedItem();

        // Null-Prüfung und Standardwert
        if (selectedCategory == null) {
            selectedCategory = new Category(0, "Alle");
            cbCategoryChoice.setSelectedItem(selectedCategory);
        }

        final Category finalCategory = selectedCategory;
        final boolean finalNoneEffective = cbEffectless.isSelected();
        final boolean finalOldestFirst = cbOldestFirst.isSelected();

        SwingUtilities.invokeLater(() -> {
            List<Vokabel> filtered;

            if (set.equals("All")) {
                if (finalCategory.getId() == 0) {
                    filtered = dbHelper.getAllVokabeln();
                } else {
                    filtered = dbHelper.getVokabelnByCategoryName(finalCategory.getName());
                }
            } else { // Errors
                if (finalCategory.getId() == 0) {
                    filtered = dbHelper.getErrorVokabeln(lang1ToLang2);
                } else {
                    filtered = filterByCategory(dbHelper.getErrorVokabeln(lang1ToLang2), finalCategory.getName());
                }
            }

            if (finalOldestFirst) {
                filtered.sort(Comparator.comparingLong(Vokabel::getLastReviewed));
            } else {
                Collections.shuffle(filtered);
            }

            lernVokabeln = filtered;
            aktuelleVokabeln = new ArrayList<>(filtered);
        });
    }

    private List<Vokabel> filterByCategory(List<Vokabel> vokabeln, String categoryName) {
        List<Vokabel> filtered = new ArrayList<>();
        for (Vokabel v : vokabeln) {
            for (Category cat : v.getCategories()) {
                if (cat.getName().equals(categoryName)) {
                    filtered.add(v);
                    break;
                }
            }
        }
        return filtered;
    }

    private void toggleLearning() {
        if (btnStart.getText().equals("Start")) {
            startLearning();
        } else {
            stopLearning();
        }
    }

    private void startLearning() {
        if (aktuelleVokabeln.isEmpty()) {
            loadVokabeln();
            if (aktuelleVokabeln.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Keine Vokabeln zum Lernen verfügbar!",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Einstellungen deaktivieren
        rbDirection1.setEnabled(false);
        rbDirection2.setEnabled(false);
        rbSetAll.setEnabled(false);
        rbSetErrors.setEnabled(false);
        lblCategory.setEnabled(false);
        cbCategoryChoice.setEnabled(false);
        cbEffectless.setEnabled(false);
        lblEffectless.setEnabled(false);
        cbOldestFirst.setEnabled(false);
        lblOldestFirst.setEnabled(false);

        // Lernpanel aktivieren
        bottomPanel.setVisible(true);
        btnStart.setText("Stop");

        // Erste Vokabel laden
        aktuelleIndex = 0;
        nextVokabel();
        richtigeAntworten = 0;
        falscheAntworten = 0;

        revalidate();
        repaint();
    }

    private void stopLearning() {
        // Einstellungen aktivieren
        rbDirection1.setEnabled(true);
        rbDirection2.setEnabled(true);
        rbSetAll.setEnabled(true);
        rbSetErrors.setEnabled(true);
        lblCategory.setEnabled(true);
        cbCategoryChoice.setEnabled(true);
        cbEffectless.setEnabled(true);
        lblEffectless.setEnabled(true);
        cbOldestFirst.setEnabled(true);
        lblOldestFirst.setEnabled(true);

        // Lernpanel deaktivieren
        bottomPanel.setVisible(false);
        btnStart.setText("Start");

        revalidate();
        repaint();
    }

    private void nextVokabel() {
        if (aktuelleVokabeln.isEmpty()) {
            lblQuestion.setText("Keine Vokabeln mehr");
            tfQuestion.setText("");
            tfAnswer.setText("");
            lstVocCategories.setListData(new String[0]);
            lblState.setText("0");
            return;
        }

        aktuelleVokabel = aktuelleVokabeln.get(aktuelleIndex);
        zeigeDeutsch = rbDirection1.isSelected();

        // Frage anzeigen
        String question = zeigeDeutsch ? aktuelleVokabel.getDeutsch() : aktuelleVokabel.getEnglisch();
        tfQuestion.setText(question);

        // Kategorien anzeigen
        String[] categories = aktuelleVokabel.getCategories().stream()
            .map(Category::getName).toArray(String[]::new);
        lstVocCategories.setListData(categories);

        // State anzeigen
        int state = zeigeDeutsch ?
            aktuelleVokabel.getErrorCountLang1() :
            aktuelleVokabel.getErrorCountLang2();
        lblState.setText(String.valueOf(state));
        lblState.setForeground(state < 0 ? Color.RED : Color.BLACK);

        // Antwortfeld vorbereiten
        tfAnswer.setText("");
        tfAnswer.requestFocus();

        // Lernrichtung anzeigen
        lblQuestion.setText(zeigeDeutsch ? "Deutsch:" : "Englisch:");
        lblAnswer.setText(zeigeDeutsch ? "Englisch:" : "Deutsch:");
    }

    private void verifyVoc() {
        if (aktuelleVokabel == null) return;

        String answer = tfAnswer.getText().trim().toLowerCase();
        String correctAnswer = zeigeDeutsch ?
            aktuelleVokabel.getEnglisch().toLowerCase() :
            aktuelleVokabel.getDeutsch().toLowerCase();

        if (answer.equals(correctAnswer)) {
            if (!noneEffective) {
                aktuelleVokabel.incrementErrorCount(zeigeDeutsch);
                dbHelper.updateVokabel(aktuelleVokabel);
            }
            JOptionPane.showMessageDialog(this, "Richtig!", "Ergebnis", JOptionPane.INFORMATION_MESSAGE);
            richtigeAntworten++;

            // Wenn keine Fehler mehr, aus der Liste entfernen
            boolean shouldRemove = zeigeDeutsch ?
                !aktuelleVokabel.isErrorLang1() :
                !aktuelleVokabel.isErrorLang2();

            if (shouldRemove && !noneEffective) {
                aktuelleVokabeln.remove(aktuelleIndex);
                if (aktuelleIndex >= aktuelleVokabeln.size()) {
                    aktuelleIndex = 0;
                }
            } else {
                aktuelleIndex++;
            }
        } else {
            if (!noneEffective) {
                aktuelleVokabel.decrementErrorCount(zeigeDeutsch);
                dbHelper.updateVokabel(aktuelleVokabel);
            }
            JOptionPane.showMessageDialog(this,
                "Falsch! Richtige Antwort: " + correctAnswer,
                "Ergebnis", JOptionPane.ERROR_MESSAGE);
            falscheAntworten++;
            aktuelleIndex++;
        }

        // Nächste Vokabel oder Ende
        if (aktuelleIndex < aktuelleVokabeln.size()) {
            nextVokabel();
        } else {
            tfQuestion.setText("Fertig!");
            tfAnswer.setText("");
            lstVocCategories.setListData(new String[0]);
            lblState.setText("");
        }
    }

    public void refreshData() {
        loadCategories();
        loadVokabeln();
    }
}
