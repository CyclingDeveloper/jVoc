package com.vokabeltrainer.ui;

import com.vokabeltrainer.db.DatabaseHelper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VokabelTrainerApp extends JFrame {
    private DatabaseHelper dbHelper;
    private LearnPanel learnPanel;
    private EditPanel editPanel;
    private InputPanel inputPanel;
    private CategoryPanel categoryPanel;  // NEU
    private JTabbedPane tabbedPane;

    public VokabelTrainerApp() {
        dbHelper = new DatabaseHelper();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Vokabeltrainer - Lernprogramm für Englisch/Deutsch");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        learnPanel = new LearnPanel(dbHelper, this);
        editPanel = new EditPanel(dbHelper, this);
        inputPanel = new InputPanel(dbHelper, this);
        categoryPanel = new CategoryPanel(dbHelper, this);  // NEU

        tabbedPane.addTab("📚 Lernen", learnPanel);
        tabbedPane.addTab("✏️ Bearbeiten", editPanel);
        tabbedPane.addTab("➕ Eingabe", inputPanel);
        // Kategorien als Dialog, nicht als Tab
        // tabbedPane.addTab("📂 Kategorien", categoryPanel);

        add(tabbedPane, BorderLayout.CENTER);

        createMenuBar();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (dbHelper != null) dbHelper.close();
                System.exit(0);
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Datei-Menü
        JMenu fileMenu = new JMenu("Datei");
        JMenuItem exitItem = new JMenuItem("Beenden");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Bearbeiten-Menü
        JMenu editMenu = new JMenu("Bearbeiten");
        JMenuItem refreshItem = new JMenuItem("Alle Daten aktualisieren");
        refreshItem.addActionListener(e -> refreshAllTabs());
        editMenu.add(refreshItem);
        menuBar.add(editMenu);

        // Lernen-Menü
        JMenu learnMenu = new JMenu("Lernen");
        JMenuItem resetAllItem = new JMenuItem("Alle Lernstatus zurücksetzen");
        resetAllItem.addActionListener(e -> resetAllLearningStatus());
        learnMenu.add(resetAllItem);
        menuBar.add(learnMenu);

        // Kategorien-Menü (NEU)
        JMenu categoryMenu = new JMenu("Kategorien");
        JMenuItem manageCategoriesItem = new JMenuItem("Kategorien verwalten...");
        manageCategoriesItem.addActionListener(e -> showCategoryManager());
        categoryMenu.add(manageCategoriesItem);
        menuBar.add(categoryMenu);

        // Hilfe-Menü
        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem infoItem = new JMenuItem("Info");
        infoItem.addActionListener(e -> showInfo());
        helpMenu.add(infoItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void showCategoryManager() {
        JDialog dialog = new JDialog(this, "Kategorien verwalten", false);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setContentPane(categoryPanel);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private void showInfo() {
        JOptionPane.showMessageDialog(this,
            "Vokabeltrainer v1.0\n\n" +
            "Ein Lernprogramm für Englisch/Deutsch Vokabeln\n\n" +
            "Funktionen:\n" +
            "- Lernen in beiden Richtungen\n" +
            "- Vokabeln müssen 8x richtig beantwortet werden\n" +
            "- Lernen nach Kategorie oder allen Vokabeln\n" +
            "- Vokabelverwaltung mit Bearbeiten und Löschen\n" +
            "- Massenimport von Vokabeln\n" +
            "- Kategorienverwaltung",
            "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshAllTabs() {
        learnPanel.refreshData();
        editPanel.refreshData();
        inputPanel.refreshData();
        if (categoryPanel != null) {
            categoryPanel.refreshData();
        }
    }

    private void resetAllLearningStatus() {
        if (JOptionPane.showConfirmDialog(this,
            "Lernstatus ALLER Vokabeln zurücksetzen?",
            "Zurücksetzen", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (dbHelper.resetAllLearningStatus()) {
                refreshAllTabs();
                JOptionPane.showMessageDialog(this, "Lernstatus zurückgesetzt!",
                                            "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            new VokabelTrainerApp().setVisible(true);
        });
    }
}
