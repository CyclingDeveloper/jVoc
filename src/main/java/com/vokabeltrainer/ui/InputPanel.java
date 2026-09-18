package com.vokabeltrainer.ui;

import com.vokabeltrainer.db.DatabaseHelper;
import com.vokabeltrainer.model.Category;
import com.vokabeltrainer.model.Vokabel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class InputPanel extends JPanel {
    private DatabaseHelper dbHelper;
    private VokabelTrainerApp mainApp;

    // Eingabefelder
    private JTextField tfDeutsch;
    private JTextField tfEnglisch;
    private JList<Category> categoryList;
    private DefaultListModel<Category> categoryListModel;

    // Tabelle für neu hinzugefügte Vokabeln
    private JTable vokabelTable;
    private DefaultTableModel tableModel;
    private JComboBox<Category> cbFilterKategorie;

    public InputPanel(DatabaseHelper dbHelper, VokabelTrainerApp mainApp) {
        this.dbHelper = dbHelper;
        this.mainApp = mainApp;
        initializeUI();
        loadKategorien();
        loadNeueVokabeln();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Oberes Panel für Eingabe
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // Unteres Panel für Tabelle
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Neue Vokabel eingeben"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Deutsch
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Deutsch:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfDeutsch = new JTextField(20);
        tfDeutsch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tfEnglisch.requestFocus();
                }
            }
        });
        panel.add(tfDeutsch, gbc);

        // Englisch
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Englisch:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfEnglisch = new JTextField(20);
        tfEnglisch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addVokabel();
                }
            }
        });
        panel.add(tfEnglisch, gbc);

        // Kategorien (Mehrfachauswahl mit JList)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Kategorien:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        categoryListModel = new DefaultListModel<>();
        categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane categoryScroll = new JScrollPane(categoryList);
        categoryScroll.setPreferredSize(new Dimension(200, 100));
        panel.add(categoryScroll, gbc);

        // Buttons
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 3;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton btnAdd = new JButton("Hinzufügen");
        btnAdd.addActionListener(e -> addVokabel());
        buttonPanel.add(btnAdd);
        JButton btnClear = new JButton("Felder leeren");
        btnClear.addActionListener(e -> clearFields());
        buttonPanel.add(btnClear);
        JButton btnMultiple = new JButton("Mehrere hinzufügen");
        btnMultiple.addActionListener(e -> showAddMultipleDialog());
        buttonPanel.add(btnMultiple);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Neu hinzugefügte Vokabeln"));

        // Filter für Kategorie
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Kategorie:"));
        cbFilterKategorie = new JComboBox<>();
        cbFilterKategorie.addItem(new Category(0, "Alle"));
        cbFilterKategorie.addActionListener(e -> loadNeueVokabeln());
        filterPanel.add(cbFilterKategorie);
        panel.add(filterPanel, BorderLayout.NORTH);

        // Tabelle
        String[] columns = {"ID", "Deutsch", "Englisch", "Kategorien"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        vokabelTable = new JTable(tableModel);
        vokabelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(vokabelTable), BorderLayout.CENTER);

        // Löschen-Button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnDelete = new JButton("Aus Liste entfernen");
        btnDelete.addActionListener(e -> deleteFromTable());
        btnPanel.add(btnDelete);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadKategorien() {
        SwingUtilities.invokeLater(() -> {
            List<Category> categories = dbHelper.getAllCategories();
            categoryListModel.clear();
            for (Category cat : categories) {
                categoryListModel.addElement(cat);
            }

            // Filter-Combobox aktualisieren
            cbFilterKategorie.removeAllItems();
            cbFilterKategorie.addItem(new Category(0, "Alle"));
            for (Category cat : categories) {
                cbFilterKategorie.addItem(cat);
            }
        });
    }

    
    private void loadNeueVokabeln() {
    SwingUtilities.invokeLater(() -> {
        Category selected = (Category) cbFilterKategorie.getSelectedItem();
        List<Vokabel> list;
        if (selected.getId() == 0) { // "Alle"
            list = dbHelper.getAllVokabeln();
        } else {
            list = dbHelper.getVokabelnByCategoryName(selected.getName());
        }
        // Nur die neuesten 50 anzeigen
        int startIndex = Math.max(0, list.size() - 50);
        updateTable(list.subList(startIndex, list.size()));
        clearFields();  // <-- HIER: clearFields() statt clearSelection()
    });
   }


    private void updateTable(List<Vokabel> vokabeln) {
        tableModel.setRowCount(0);
        for (Vokabel v : vokabeln) {
            String categories = String.join(", ", v.getCategories().stream()
                .map(Category::getName).toArray(String[]::new));
            tableModel.addRow(new Object[]{
                v.getId(),
                v.getDeutsch(),
                v.getEnglisch(),
                categories
            });
        }
    }

    private void addVokabel() {
        String de = tfDeutsch.getText().trim();
        String en = tfEnglisch.getText().trim();
        List<Category> selectedCategories = categoryList.getSelectedValuesList();

        // 1. Prüfen ob Deutsch und Englisch ausgefüllt
        if (de.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie die deutsche Übersetzung ein.",
                "Fehler: Leeres Feld", JOptionPane.ERROR_MESSAGE);
            tfDeutsch.requestFocus();
            return;
        }

        if (en.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie die englische Übersetzung ein.",
                "Fehler: Leeres Feld", JOptionPane.ERROR_MESSAGE);
            tfEnglisch.requestFocus();
            return;
        }

        // 2. Prüfen ob mindestens eine Kategorie ausgewählt
        if (selectedCategories.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie mindestens eine Kategorie aus.",
                "Fehler: Keine Kategorie", JOptionPane.ERROR_MESSAGE);
            categoryList.requestFocus();
            return;
        }

        // 3. Prüfen ob Vokabel bereits existiert
        if (vokabelExists(de, en, selectedCategories)) {
            JOptionPane.showMessageDialog(this,
                "Diese Vokabel existiert bereits mit diesen Kategorien!",
                "Fehler: Dopplung", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Vokabel hinzufügen
        Vokabel vokabel = new Vokabel(de, en, selectedCategories);
        if (dbHelper.addVokabel(vokabel)) {
            loadNeueVokabeln();
            clearFields();
            mainApp.refreshAllTabs();
            JOptionPane.showMessageDialog(this,
                "Vokabel '" + de + "' -> '" + en + "' erfolgreich hinzugefügt!",
                "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Speichern der Vokabel in der Datenbank. \n" +
                "Details: Siehe Konsole für SQL-Fehler.",
                "Fehler: Datenbank", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean vokabelExists(String de, String en, List<Category> categories) {
        List<Vokabel> alleVokabeln = dbHelper.getAllVokabeln();
        for (Vokabel v : alleVokabeln) {
            if (v.getDeutsch().equalsIgnoreCase(de) &&
                v.getEnglisch().equalsIgnoreCase(en) &&
                v.getCategories().size() == categories.size()) {

                // Prüfen ob die Kategorien übereinstimmen
                boolean sameCategories = true;
                for (Category cat : categories) {
                    boolean found = false;
                    for (Category existingCat : v.getCategories()) {
                        if (existingCat.getName().equals(cat.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        sameCategories = false;
                        break;
                    }
                }
                if (sameCategories) return true;
            }
        }
        return false;
    }

    private void clearFields() {
        tfDeutsch.setText("");
        tfEnglisch.setText("");
        categoryList.clearSelection();
        tfDeutsch.requestFocus();
    }

    private void showAddMultipleDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                    "Mehrere Vokabeln hinzufügen", true);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JTextArea ta = new JTextArea(
            "# Format: Deutsch=Englisch [Kategorie1,Kategorie2,...]\n" +
            "# Beispiel:\n" +
            "Haus=house [Nomen]\n" +
            "laufen=run [Verben,Sport]\n" +
            "schnell=fast [Adjektive]"
        );
        dialog.add(new JScrollPane(ta), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel catPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        catPanel.add(new JLabel("Standard-Kategorie:"));
        JComboBox<Category> cbDefKat = new JComboBox<>();
        for (Category cat : dbHelper.getAllCategories()) {
            cbDefKat.addItem(cat);
        }
        catPanel.add(cbDefKat);
        bottom.add(catPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnImport = new JButton("Importieren");
        btnImport.addActionListener(e -> {
            int count = importMultipleVokabeln(ta.getText(), (Category) cbDefKat.getSelectedItem());
            if (count > 0) {
                loadNeueVokabeln();
                mainApp.refreshAllTabs();
                dialog.dispose();
            }
        });
        JButton btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnImport);
        btnPanel.add(btnCancel);
        bottom.add(btnPanel, BorderLayout.SOUTH);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private int importMultipleVokabeln(String text, Category defaultCategory) {
        String[] lines = text.split("\n");
        int count = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Format: Deutsch=Englisch [Kat1,Kat2,...]
            String[] parts = line.split("=", 2);
            if (parts.length != 2) continue;

            String de = parts[0].trim();
            String rest = parts[1].trim();

            // Kategorien extrahieren
            List<Category> categories = new ArrayList<>();
            if (rest.contains("[") && rest.contains("]")) {
                int start = rest.lastIndexOf("[");
                int end = rest.lastIndexOf("]");
                if (start < end) {
                    String cats = rest.substring(start + 1, end).trim();
                    String[] catNames = cats.split(",");
                    for (String catName : catNames) {
                        catName = catName.trim();
                        Category cat = dbHelper.getCategoryByName(catName);
                        if (cat != null) {
                            categories.add(cat);
                        } else {
                            // Falls Kategorie nicht existiert, Standard-Kategorie verwenden
                            categories.add(defaultCategory);
                        }
                    }
                    de = rest.substring(0, start).trim();
                }
            } else {
                categories.add(defaultCategory);
            }

            if (!de.isEmpty() && !rest.isEmpty() && !vokabelExists(de, rest, categories)) {
                Vokabel vokabel = new Vokabel(de, rest, categories);
                if (dbHelper.addVokabel(vokabel)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void deleteFromTable() {
        int row = vokabelTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie eine Vokabel aus.",
                "Fehler: Keine Auswahl", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String de = (String) tableModel.getValueAt(row, 1);
        String en = (String) tableModel.getValueAt(row, 2);

        int result = JOptionPane.showConfirmDialog(this,
            "Möchten Sie die Vokabel '" + de + "' -> '" + en + "' wirklich löschen?",
            "Vokabel löschen", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            if (dbHelper.deleteVokabel(id)) {
                loadNeueVokabeln();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Vokabel erfolgreich gelöscht!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Löschen der Vokabel.",
                    "Fehler: Datenbank", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshData() {
        loadKategorien();
        loadNeueVokabeln();
    }
}
