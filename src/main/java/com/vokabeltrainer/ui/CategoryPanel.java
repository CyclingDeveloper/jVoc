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

public class CategoryPanel extends JPanel {
    private DatabaseHelper dbHelper;
    private VokabelTrainerApp mainApp;

    private JTable categoryTable;
    private DefaultTableModel tableModel;
    private JTextField tfNewCategory;
    private JTextField tfRenameCategory;
    private JComboBox<Category> cbMoveToCategory;

    public CategoryPanel(DatabaseHelper dbHelper, VokabelTrainerApp mainApp) {
        this.dbHelper = dbHelper;
        this.mainApp = mainApp;
        initializeUI();
        refreshCategories();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Oberes Panel für neue Kategorie
        JPanel addPanel = createAddPanel();
        add(addPanel, BorderLayout.NORTH);

        // Mittleres Panel für Kategorien-Tabelle
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Unteres Panel für Bearbeiten/Löschen
        JPanel editPanel = createEditPanel();
        add(editPanel, BorderLayout.SOUTH);
    }

    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Neue Kategorie hinzufügen"));

        panel.add(new JLabel("Name:"));
        tfNewCategory = new JTextField(20);
        tfNewCategory.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addCategory();
                }
            }
        });
        panel.add(tfNewCategory);

        JButton btnAdd = new JButton("Hinzufügen");
        btnAdd.addActionListener(e -> addCategory());
        panel.add(btnAdd);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vorhandene Kategorien"));

        String[] columnNames = {"Kategorie", "Anzahl Vokabeln"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        categoryTable = new JTable(tableModel);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        categoryTable.getColumnModel().getColumn(1).setPreferredWidth(100);

        // Doppelklick zum Umbenennen
        categoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && categoryTable.getSelectedRow() != -1) {
                    enableRename();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(categoryTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEditPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Kategorie bearbeiten"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Umbenennen
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Umbenennen:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        tfRenameCategory = new JTextField(20);
        tfRenameCategory.setEditable(false);
        panel.add(tfRenameCategory, gbc);

        gbc.gridx = 2; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        JButton btnRename = new JButton("Umbenennen");
        btnRename.addActionListener(e -> enableRename());
        panel.add(btnRename, gbc);

        gbc.gridx = 3;
        JButton btnSaveRename = new JButton("Speichern");
        btnSaveRename.addActionListener(e -> saveRename());
        btnSaveRename.setEnabled(false);
        panel.add(btnSaveRename, gbc);

        gbc.gridx = 4;
        JButton btnCancelRename = new JButton("Abbrechen");
        btnCancelRename.addActionListener(e -> cancelRename());
        btnCancelRename.setEnabled(false);
        panel.add(btnCancelRename, gbc);

        // Löschen/Verschieben
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Löschen/Verschieben:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        cbMoveToCategory = new JComboBox<>();
        loadMoveToCategories();
        panel.add(cbMoveToCategory, gbc);

        gbc.gridx = 2;
        JButton btnDelete = new JButton("Löschen");
        btnDelete.addActionListener(e -> deleteCategory());
        panel.add(btnDelete, gbc);

        return panel;
    }

    private void loadMoveToCategories() {
        SwingUtilities.invokeLater(() -> {
            cbMoveToCategory.removeAllItems();
            cbMoveToCategory.addItem(new Category(0, "Löschen (Vokabeln löschen)"));
            cbMoveToCategory.addItem(new Category(-1, "Löschen (Vokabeln behalten)"));
            for (Category cat : dbHelper.getAllCategories()) {
                if (!cat.getName().contains("_gelöscht_") && !cat.getName().contains("_archiviert_")) {
                    cbMoveToCategory.addItem(cat);
                }
            }
        });
    }

    private void addCategory() {
        String newCategory = tfNewCategory.getText().trim();

        // 1. Prüfen ob leer
        if (newCategory.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie einen Kategorienamen ein.",
                "Fehler: Leeres Feld", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Prüfen auf ungültige Zeichen
        if (newCategory.contains(";") || newCategory.contains("'") || newCategory.contains("\"")) {
            JOptionPane.showMessageDialog(this,
                "Der Kategoriename darf keine Sonderzeichen wie ; ' \" enthalten.",
                "Fehler: Ungültige Zeichen", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Prüfen ob bereits existiert
        Category existing = dbHelper.getCategoryByName(newCategory);
        if (existing != null) {
            JOptionPane.showMessageDialog(this,
                "Die Kategorie '" + newCategory + "' existiert bereits!",
                "Fehler: Dopplung", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Kategorie hinzufügen
        Category newCat = new Category(0, newCategory);
        boolean success = dbHelper.addCategory(newCat);

        if (success) {
            tfNewCategory.setText("");
            refreshCategories();
            loadMoveToCategories();
            mainApp.refreshAllTabs();
            JOptionPane.showMessageDialog(this,
                "Kategorie '" + newCategory + "' erfolgreich hinzugefügt!",
                "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Speichern der Kategorie in der Datenbank. \n" +
                "Details: Siehe Konsole für SQL-Fehler.",
                "Fehler: Datenbank", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enableRename() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie eine Kategorie aus der Tabelle aus.",
                "Fehler: Keine Auswahl", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String categoryName = (String) tableModel.getValueAt(selectedRow, 0);
        tfRenameCategory.setText(categoryName);
        tfRenameCategory.setEditable(true);

        // Buttons aktualisieren
        for (Component c : getComponents()) {
            if (c instanceof JPanel) {
                for (Component sc : ((JPanel) c).getComponents()) {
                    if (sc instanceof JButton) {
                        JButton btn = (JButton) sc;
                        if ("Speichern".equals(btn.getText())) btn.setEnabled(true);
                        else if ("Abbrechen".equals(btn.getText())) btn.setEnabled(true);
                        else if ("Umbenennen".equals(btn.getText())) btn.setEnabled(false);
                    }
                }
            }
        }
    }

    private void saveRename() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) return;

        String oldCategoryName = (String) tableModel.getValueAt(selectedRow, 0);
        String newCategoryName = tfRenameCategory.getText().trim();

        // 1. Prüfen ob leer
        if (newCategoryName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie einen neuen Kategorienamen ein.",
                "Fehler: Leeres Feld", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Prüfen auf ungültige Zeichen
        if (newCategoryName.contains(";") || newCategoryName.contains("'") || newCategoryName.contains("\"")) {
            JOptionPane.showMessageDialog(this,
                "Der Kategoriename darf keine Sonderzeichen wie ; ' \" enthalten.",
                "Fehler: Ungültige Zeichen", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Prüfen ob neue Kategorie bereits existiert
        Category existing = dbHelper.getCategoryByName(newCategoryName);
        if (existing != null && !existing.getName().equals(oldCategoryName)) {
            JOptionPane.showMessageDialog(this,
                "Die Kategorie '" + newCategoryName + "' existiert bereits!",
                "Fehler: Dopplung", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Kategorie umbenennen
        Category oldCategory = dbHelper.getCategoryByName(oldCategoryName);
        if (oldCategory != null) {
            oldCategory.setName(newCategoryName);
            if (dbHelper.addCategory(oldCategory)) {
                // Alle Vokabeln mit alter Kategorie aktualisieren
                List<Vokabel> vokabeln = dbHelper.getVokabelnByCategoryName(oldCategoryName);
                for (Vokabel v : vokabeln) {
                    List<Category> categories = v.getCategories();
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getName().equals(oldCategoryName)) {
                            categories.set(i, new Category(oldCategory.getId(), newCategoryName));
                        }
                    }
                    v.setCategories(categories);
                    dbHelper.updateVokabel(v);
                }

                tfRenameCategory.setText("");
                tfRenameCategory.setEditable(false);
                cancelRename();
                refreshCategories();
                loadMoveToCategories();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Kategorie '" + oldCategoryName + "' erfolgreich in '" + newCategoryName + "' umbenannt!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern der Umbenennung in der Datenbank.",
                    "Fehler: Datenbank", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelRename() {
        tfRenameCategory.setText("");
        tfRenameCategory.setEditable(false);

        for (Component c : getComponents()) {
            if (c instanceof JPanel) {
                for (Component sc : ((JPanel) c).getComponents()) {
                    if (sc instanceof JButton) {
                        JButton btn = (JButton) sc;
                        if ("Speichern".equals(btn.getText())) btn.setEnabled(false);
                        else if ("Abbrechen".equals(btn.getText())) btn.setEnabled(false);
                        else if ("Umbenennen".equals(btn.getText())) btn.setEnabled(true);
                    }
                }
            }
        }
    }

    private void deleteCategory() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie eine Kategorie aus der Tabelle aus.",
                "Fehler: Keine Auswahl", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String categoryName = (String) tableModel.getValueAt(selectedRow, 0);
        int vokabelCount = (int) tableModel.getValueAt(selectedRow, 1);

        Category selectedAction = (Category) cbMoveToCategory.getSelectedItem();

        if (selectedAction.getId() == 0) {
            // Option: Vokabeln löschen
            if (vokabelCount > 0) {
                int result = JOptionPane.showConfirmDialog(this,
                    "Die Kategorie '" + categoryName + "' enthält " + vokabelCount + " Vokabel(n).\n" +
                    "Möchten Sie ALLE Vokabeln in dieser Kategorie löschen?",
                    "Kategorie löschen", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.NO_OPTION) return;
            }

            // Kategorie und alle zugehörigen Vokabeln löschen
            Category cat = dbHelper.getCategoryByName(categoryName);
            if (cat != null) {
                List<Vokabel> vokabeln = dbHelper.getVokabelnByCategoryName(categoryName);
                for (Vokabel v : vokabeln) {
                    dbHelper.deleteVokabel(v.getId());
                }
                // Workaround: Kategorie umbenennen, da SQLite keine CASCADE DELETE unterstützt
                cat.setName(cat.getName() + "_gelöscht_" + System.currentTimeMillis());
                dbHelper.addCategory(cat);
                refreshCategories();
                loadMoveToCategories();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Kategorie '" + categoryName + "' und alle Vokabeln gelöscht!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (selectedAction.getId() == -1) {
            // Option: Vokabeln behalten (Kategorie einfach entfernen)
            Category cat = dbHelper.getCategoryByName(categoryName);
            if (cat != null) {
                // Alle Vokabeln von dieser Kategorie trennen
                List<Vokabel> vokabeln = dbHelper.getVokabelnByCategoryName(categoryName);
                for (Vokabel v : vokabeln) {
                    List<Category> categories = new ArrayList<>();
                    for (Category c : v.getCategories()) {
                        if (!c.getName().equals(categoryName)) {
                            categories.add(c);
                        }
                    }
                    v.setCategories(categories);
                    dbHelper.updateVokabel(v);
                }
                // Workaround: Kategorie umbenennen
                cat.setName(cat.getName() + "_archiviert_" + System.currentTimeMillis());
                dbHelper.addCategory(cat);
                refreshCategories();
                loadMoveToCategories();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Kategorie '" + categoryName + "' entfernt, Vokabeln behalten!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // Option: In andere Kategorie verschieben
            if (selectedAction.getName().equals(categoryName)) {
                JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie eine andere Kategorie als Ziel aus.",
                    "Fehler: Gleiches Ziel", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Category sourceCat = dbHelper.getCategoryByName(categoryName);
            Category targetCat = dbHelper.getCategoryByName(selectedAction.getName());

            if (sourceCat != null && targetCat != null) {
                List<Vokabel> vokabeln = dbHelper.getVokabelnByCategoryName(categoryName);
                for (Vokabel v : vokabeln) {
                    // Füge Zielkategorie hinzu (falls nicht bereits vorhanden)
                    boolean hasTarget = false;
                    for (Category c : v.getCategories()) {
                        if (c.getName().equals(targetCat.getName())) {
                            hasTarget = true;
                            break;
                        }
                    }
                    if (!hasTarget) {
                        v.getCategories().add(targetCat);
                    }

                    // Entferne Quellkategorie
                    v.getCategories().removeIf(c -> c.getName().equals(sourceCat.getName()));
                    dbHelper.updateVokabel(v);
                }
                // Workaround: Kategorie umbenennen
                sourceCat.setName(sourceCat.getName() + "_archiviert_" + System.currentTimeMillis());
                dbHelper.addCategory(sourceCat);
                refreshCategories();
                loadMoveToCategories();
                mainApp.refreshAllTabs();
                JOptionPane.showMessageDialog(this,
                    "Vokabeln aus '" + categoryName + "' in '" + targetCat.getName() + "' verschoben!",
                    "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void refreshCategories() {
        SwingUtilities.invokeLater(() -> {
            List<Category> categories = dbHelper.getAllCategories();
            tableModel.setRowCount(0);

            for (Category cat : categories) {
                // Nur Kategorien anzeigen, die nicht archiviert/gelöscht sind
                if (!cat.getName().contains("_gelöscht_") && !cat.getName().contains("_archiviert_")) {
                    List<Vokabel> vokabeln = dbHelper.getVokabelnByCategoryName(cat.getName());
                    tableModel.addRow(new Object[]{cat.getName(), vokabeln.size()});
                }
            }
        });
    }

    public void refreshData() {
        refreshCategories();
        loadMoveToCategories();
    }
}
