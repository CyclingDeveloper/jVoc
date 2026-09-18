package com.vokabeltrainer.ui;

import com.vokabeltrainer.db.DatabaseHelper;
import com.vokabeltrainer.model.Category;
import com.vokabeltrainer.model.Vokabel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class EditPanel extends JPanel {
    private DatabaseHelper dbHelper;
    private VokabelTrainerApp mainApp;

    // UI-Komponenten
    private JTable vokabelTable;
    private DefaultTableModel tableModel;
    private JTextField tfSearch;
    private JComboBox<Category> cbKategorieFilter;

    // Bearbeitungsfelder
    private JTextField tfDeutsch;
    private JTextField tfEnglisch;
    private JList<Category> categoryList;
    private DefaultListModel<Category> categoryListModel;

    public EditPanel(DatabaseHelper dbHelper, VokabelTrainerApp mainApp) {
        this.dbHelper = dbHelper;
        this.mainApp = mainApp;
        initializeUI();
        loadKategorien();
        loadVokabeln();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Filter-Panel
        add(createFilterPanel(), BorderLayout.NORTH);

        // Tabellen-Panel
        add(createTablePanel(), BorderLayout.CENTER);

        // Bearbeitungs-Panel
        add(createEditPanel(), BorderLayout.SOUTH);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Suche und Filter"));

        panel.add(new JLabel("Suche:"));
        tfSearch = new JTextField(20);
        tfSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchVokabeln();
            }
        });
        panel.add(tfSearch);

        panel.add(new JLabel("Kategorie:"));
        cbKategorieFilter = new JComboBox<>();
        cbKategorieFilter.addItem(new Category(0, "Alle")); // Standardwert
        cbKategorieFilter.setSelectedIndex(0);
        cbKategorieFilter.addActionListener(e -> loadVokabeln());
        panel.add(cbKategorieFilter);

        JButton btnRefresh = new JButton("Aktualisieren");
        btnRefresh.addActionListener(e -> loadVokabeln());
        panel.add(btnRefresh);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vokabelliste"));

        // Tabellenmodell
        String[] columnNames = {"ID", "Deutsch", "Englisch", "Kategorien", "Fehler (D→E)", "Fehler (E→D)"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        vokabelTable = new JTable(tableModel);
        vokabelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Spaltenbreiten
        vokabelTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        vokabelTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        vokabelTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        vokabelTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        vokabelTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        vokabelTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        // Doppelklick zum Bearbeiten
        vokabelTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadVokabelToEdit();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(vokabelTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEditPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vokabel bearbeiten"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Deutsch
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Deutsch:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfDeutsch = new JTextField(20);
        tfDeutsch.setEditable(false);
        panel.add(tfDeutsch, gbc);

        // Englisch
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Englisch:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfEnglisch = new JTextField(20);
        tfEnglisch.setEditable(false);
        panel.add(tfEnglisch, gbc);

        // Kategorien (Mehrfachauswahl)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Kategorien:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        categoryListModel = new DefaultListModel<>();
        categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane categoryScroll = new JScrollPane(categoryList);
        categoryScroll.setPreferredSize(new Dimension(200, 80));
        panel.add(categoryScroll, gbc);

        // Buttons
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 3;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        JButton btnEdit = new JButton("Bearbeiten");
        btnEdit.addActionListener(e -> enableEditing());
        buttonPanel.add(btnEdit);
        JButton btnSave = new JButton("Speichern");
        btnSave.addActionListener(e -> saveVokabel());
        btnSave.setEnabled(false);
        buttonPanel.add(btnSave);
        JButton btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(e -> cancelEditing());
        btnCancel.setEnabled(false);
        buttonPanel.add(btnCancel);
        JButton btnDelete = new JButton("Löschen");
        btnDelete.addActionListener(e -> deleteVokabel());
        buttonPanel.add(btnDelete);
        panel.add(buttonPanel, gbc);

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
            cbKategorieFilter.removeAllItems();
            cbKategorieFilter.addItem(new Category(0, "Alle"));
            for (Category cat : categories) {
                cbKategorieFilter.addItem(cat);
            }
        });
    }

    private void loadVokabeln() {
    // Null-Prüfung OUTSIDE des Lambdas durchführen
    Category selectedCategory = (Category) cbKategorieFilter.getSelectedItem();
    if (selectedCategory == null) {
        selectedCategory = new Category(0, "Alle");
        cbKategorieFilter.setSelectedItem(selectedCategory);
    }

    // Finale Variable für das Lambda
    final Category finalCategory = selectedCategory;

    SwingUtilities.invokeLater(() -> {
        List<Vokabel> list;
        if (finalCategory.getId() == 0) { // "Alle"
            list = dbHelper.getAllVokabeln();
        } else {
            list = dbHelper.getVokabelnByCategoryName(finalCategory.getName());
        }
        updateTable(list);
        clearFields();
    });
   }


private void searchVokabeln() {
    // Variablen OUTSIDE des Lambdas deklarieren
    final String term = tfSearch.getText().trim();
    Category selectedCategory = (Category) cbKategorieFilter.getSelectedItem();

    if (selectedCategory == null) {
        selectedCategory = new Category(0, "Alle");
        cbKategorieFilter.setSelectedItem(selectedCategory);
    }

    // Finale Variable für das Lambda
    final Category finalCategory = selectedCategory;

    SwingUtilities.invokeLater(() -> {
        List<Vokabel> list;
        if (term.isEmpty()) {
            if (finalCategory.getId() == 0) {
                list = dbHelper.getAllVokabeln();
            } else {
                list = dbHelper.getVokabelnByCategoryName(finalCategory.getName());
            }
        } else {
            list = dbHelper.searchVokabeln(term);
        }
        updateTable(list);
        clearFields();
    });
}


    private void updateTable(List<Vokabel> vokabeln) {
        tableModel.setRowCount(0);
        for (Vokabel v : vokabeln) {
            String categories = String.join(", ", v.getCategories().stream()
                .map(Category::getName).toArray(String[]::new));
            Object[] rowData = {
                v.getId(),
                v.getDeutsch(),
                v.getEnglisch(),
                categories,
                v.getErrorCountLang1(),
                v.getErrorCountLang2()
            };
            tableModel.addRow(rowData);
        }
    }

    private void loadVokabelToEdit() {
        int row = vokabelTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) tableModel.getValueAt(row, 0);
        Vokabel v = dbHelper.getVokabelById(id);
        if (v != null) {
            tfDeutsch.setText(v.getDeutsch());
            tfEnglisch.setText(v.getEnglisch());

            // Kategorien in der Liste auswählen
            categoryList.clearSelection();
            for (int i = 0; i < categoryListModel.size(); i++) {
                Category cat = categoryListModel.getElementAt(i);
                for (Category vokabelCat : v.getCategories()) {
                    if (vokabelCat.getName().equals(cat.getName())) {
                        categoryList.addSelectionInterval(i, i);
                    }
                }
            }
        }
    }

    private void enableEditing() {
        int row = vokabelTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie eine Vokabel aus.",
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tfDeutsch.setEditable(true);
        tfEnglisch.setEditable(true);
        categoryList.setEnabled(true);

        // Buttons aktualisieren
        Component[] components = getComponents();
        for (Component c : components) {
            if (c instanceof JPanel) {
                for (Component sc : ((JPanel) c).getComponents()) {
                    if (sc instanceof JButton) {
                        JButton btn = (JButton) sc;
                        if ("Speichern".equals(btn.getText())) btn.setEnabled(true);
                        else if ("Abbrechen".equals(btn.getText())) btn.setEnabled(true);
                        else if ("Bearbeiten".equals(btn.getText())) btn.setEnabled(false);
                    }
                }
            }
        }

        loadVokabelToEdit();
    }

    private void saveVokabel() {
        int row = vokabelTable.getSelectedRow();
        if (row == -1) return;

        String de = tfDeutsch.getText().trim();
        String en = tfEnglisch.getText().trim();
        List<Category> selectedCategories = categoryList.getSelectedValuesList();

        if (de.isEmpty() || en.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie beide Übersetzungen ein.",
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedCategories.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie mindestens eine Kategorie aus.",
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        Vokabel existing = dbHelper.getVokabelById(id);
        if (existing != null) {
            Vokabel updated = new Vokabel(
                existing.getId(),
                de,
                en,
                existing.getErrorCountLang1(),
                existing.getErrorCountLang2(),
                existing.getLastReviewed(),
                selectedCategories
            );

            if (dbHelper.updateVokabel(updated)) {
                loadVokabeln();
                cancelEditing();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Vokabel erfolgreich aktualisiert!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Aktualisieren der Vokabel.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelEditing() {
        tfDeutsch.setEditable(false);
        tfEnglisch.setEditable(false);
        categoryList.setEnabled(false);

        // Buttons zurücksetzen
        Component[] components = getComponents();
        for (Component c : components) {
            if (c instanceof JPanel) {
                for (Component sc : ((JPanel) c).getComponents()) {
                    if (sc instanceof JButton) {
                        JButton btn = (JButton) sc;
                        if ("Speichern".equals(btn.getText())) btn.setEnabled(false);
                        else if ("Abbrechen".equals(btn.getText())) btn.setEnabled(false);
                        else if ("Bearbeiten".equals(btn.getText())) btn.setEnabled(true);
                    }
                }
            }
        }

        clearFields();
    }

    private void deleteVokabel() {
        int row = vokabelTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie eine Vokabel aus.",
                "Fehler", JOptionPane.ERROR_MESSAGE);
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
                loadVokabeln();
                clearFields();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Vokabel erfolgreich gelöscht!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Löschen der Vokabel.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        vokabelTable.clearSelection();
        tfDeutsch.setText("");
        tfEnglisch.setText("");
        categoryList.clearSelection();
        tfSearch.setText("");
    }

    public void refreshData() {
        loadKategorien();
        loadVokabeln();
    }
}
