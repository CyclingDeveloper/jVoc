package com.vokabeltrainer.db;

import com.vokabeltrainer.model.Category;
import com.vokabeltrainer.model.Vokabel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:vokabeln.db";
    private Connection connection;

    public DatabaseHelper() {
        connect();
        createTables();
    }

    // ============ VERBINDUNG & TABELLEN ============
    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DB_URL);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Datenbankfehler: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Kategorien-Tabelle
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL UNIQUE)");

            // Vokabeln-Tabelle
            stmt.execute("CREATE TABLE IF NOT EXISTS vokabeln (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "deutsch TEXT NOT NULL, " +
                        "englisch TEXT NOT NULL, " +
                        "error_count_lang1 INTEGER DEFAULT 0, " +
                        "error_count_lang2 INTEGER DEFAULT 0, " +
                        "last_reviewed INTEGER DEFAULT 0)");

            // n:m-Beziehung: Vokabeln ↔ Kategorien
            stmt.execute("CREATE TABLE IF NOT EXISTS vokabeln_categories (" +
                        "vokabel_id INTEGER NOT NULL, " +
                        "category_id INTEGER NOT NULL, " +
                        "PRIMARY KEY (vokabel_id, category_id), " +
                        "FOREIGN KEY (vokabel_id) REFERENCES vokabeln(id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE)");

            // Standard-Kategorien einfügen
            insertDefaultCategories();

        } catch (SQLException e) {
            System.err.println("Fehler beim Erstellen der Tabellen: " + e.getMessage());
        }
    }

    private void insertDefaultCategories() {
        List<String> defaultCategories = List.of("Allgemein", "Verben", "Nomen", "Adjektive", "Adverbien", "Präpositionen");
        for (String cat : defaultCategories) {
            if (!categoryExists(cat)) {
                addCategory(new Category(0, cat));
            }
        }
    }

    // ============ KATEGORIEN-METHODEN ============

    public boolean addCategory(Category category) {
        // ID nicht einfügen, da AUTO_INCREMENT!
        String sql = "INSERT OR IGNORE INTO categories (name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            int affectedRows = pstmt.executeUpdate();

            // Falls erfolgreich und ID == 0, die generierte ID abrufen
            if (affectedRows > 0 && category.getId() == 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        category.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return affectedRows > 0;
            } catch (SQLException e) {
            System.err.println("SQL-Fehler in addCategory: " + e.getMessage());
            return false;
        }
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Kategorien: " + e.getMessage());
        }
        return categories;
    }

    public Category getCategoryByName(String name) {
        String sql = "SELECT id, name FROM categories WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Category(rs.getInt("id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Kategorie: " + e.getMessage());
        }
        return null;
    }

    public boolean categoryExists(String name) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Fehler beim Prüfen der Kategorie: " + e.getMessage());
            return false;
        }
    }

    // ============ VOKABELN-METHODEN ============
    public boolean addVokabel(Vokabel vokabel) {
        try {
            // 1. Vokabel einfügen
            String sql = "INSERT INTO vokabeln (deutsch, englisch, error_count_lang1, error_count_lang2, last_reviewed) " +
                         "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, vokabel.getDeutsch());
                pstmt.setString(2, vokabel.getEnglisch());
                pstmt.setInt(3, vokabel.getErrorCountLang1());
                pstmt.setInt(4, vokabel.getErrorCountLang2());
                pstmt.setLong(5, vokabel.getLastReviewed());
                pstmt.executeUpdate();

                // ID abrufen
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    vokabel.setId(keys.getInt(1));
                }
            }

            // 2. Kategorien verknüpfen
            for (Category cat : vokabel.getCategories()) {
                Category existingCat = getCategoryByName(cat.getName());
                if (existingCat != null) {
                    addVokabelCategory(vokabel.getId(), existingCat.getId());
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Fehler beim Einfügen der Vokabel: " + e.getMessage());
            return false;
        }
    }

    public boolean updateVokabel(Vokabel vokabel) {
        try {
            // 1. Vokabel aktualisieren
            String sql = "UPDATE vokabeln SET deutsch=?, englisch=?, error_count_lang1=?, error_count_lang2=?, last_reviewed=? WHERE id=?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, vokabel.getDeutsch());
                pstmt.setString(2, vokabel.getEnglisch());
                pstmt.setInt(3, vokabel.getErrorCountLang1());
                pstmt.setInt(4, vokabel.getErrorCountLang2());
                pstmt.setLong(5, vokabel.getLastReviewed());
                pstmt.setInt(6, vokabel.getId());
                pstmt.executeUpdate();
            }

            // 2. Kategorien aktualisieren (alle alten löschen, neue hinzufügen)
            deleteVokabelCategories(vokabel.getId());
            for (Category cat : vokabel.getCategories()) {
                Category existingCat = getCategoryByName(cat.getName());
                if (existingCat != null) {
                    addVokabelCategory(vokabel.getId(), existingCat.getId());
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren der Vokabel: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteVokabel(int id) {
        String sql = "DELETE FROM vokabeln WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen der Vokabel: " + e.getMessage());
            return false;
        }
    }

    public Vokabel getVokabelById(int id) {
        List<Vokabel> result = executeVokabelQuery(
            "SELECT * FROM vokabeln WHERE id = ?", String.valueOf(id)
        );
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Vokabel> getAllVokabeln() {
        return executeVokabelQuery("SELECT * FROM vokabeln ORDER BY deutsch");
    }

    public List<Vokabel> getVokabelnByCategoryName(String categoryName) {
        Category cat = getCategoryByName(categoryName);
        if (cat == null) return new ArrayList<>();
        return executeVokabelQuery(
            "SELECT DISTINCT v.* FROM vokabeln v JOIN vokabeln_categories vc ON v.id = vc.vokabel_id WHERE vc.category_id = ? ORDER BY v.deutsch",
            String.valueOf(cat.getId())
        );
    }

    public List<Vokabel> searchVokabeln(String term) {
        return executeVokabelQuery(
            "SELECT * FROM vokabeln WHERE deutsch LIKE ? OR englisch LIKE ? ORDER BY deutsch",
            "%" + term + "%", "%" + term + "%"
        );
    }

    public List<Vokabel> getErrorVokabeln(boolean lang1ToLang2) {
        String column = lang1ToLang2 ? "error_count_lang1" : "error_count_lang2";
        return executeVokabelQuery(
            "SELECT * FROM vokabeln WHERE " + column + " <= ? ORDER BY " + column + " ASC, last_reviewed ASC",
            String.valueOf(Vokabel.ERROR_THRESHOLD)
        );
    }

    // ============ n:m BEZIEHUNGEN ============
    private boolean addVokabelCategory(int vokabelId, int categoryId) {
        String sql = "INSERT OR IGNORE INTO vokabeln_categories (vokabel_id, category_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vokabelId);
            pstmt.setInt(2, categoryId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Fehler beim Verknüpfen: " + e.getMessage());
            return false;
        }
    }

    private boolean deleteVokabelCategories(int vokabelId) {
        String sql = "DELETE FROM vokabeln_categories WHERE vokabel_id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vokabelId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen der Verknüpfungen: " + e.getMessage());
            return false;
        }
    }

    public List<Category> getCategoriesByVokabelId(int vokabelId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.id, c.name FROM categories c " +
                    "JOIN vokabeln_categories vc ON c.id = vc.category_id " +
                    "WHERE vc.vokabel_id = ? ORDER BY c.name";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vokabelId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categories.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Kategorien: " + e.getMessage());
        }
        return categories;
    }

    // ============ HILFSMETHODEN ============
    private List<Vokabel> executeVokabelQuery(String sql, String... params) {
        List<Vokabel> vokabeln = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setString(i + 1, params[i]);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Vokabel v = new Vokabel(
                    rs.getInt("id"),
                    rs.getString("deutsch"),
                    rs.getString("englisch"),
                    rs.getInt("error_count_lang1"),
                    rs.getInt("error_count_lang2"),
                    rs.getLong("last_reviewed"),
                    new ArrayList<>()
                );
                // Kategorien laden
                v.setCategories(getCategoriesByVokabelId(v.getId()));
                vokabeln.add(v);
            }
        } catch (SQLException e) {
            System.err.println("Abfragefehler: " + e.getMessage());
        }
        return vokabeln;
    }

    public int getVokabelCount() {
        return getCount("SELECT COUNT(*) FROM vokabeln");
    }

    public int getVokabelCountByCategory(String categoryName) {
        Category cat = getCategoryByName(categoryName);
        if (cat == null) return 0;
        return getCount("SELECT COUNT(*) FROM vokabeln_categories WHERE category_id = ?",
                       String.valueOf(cat.getId()));
    }

    public int getErrorCount(boolean lang1ToLang2) {
        String column = lang1ToLang2 ? "error_count_lang1" : "error_count_lang2";
        return getCount("SELECT COUNT(*) FROM vokabeln WHERE " + column + " <= ?",
                       String.valueOf(Vokabel.ERROR_THRESHOLD));
    }

    private int getCount(String sql, String... params) {
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setString(i + 1, params[i]);
            }
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Zählfehler: " + e.getMessage());
            return 0;
        }
    }

    public boolean resetAllLearningStatus() {
        String sql = "UPDATE vokabeln SET error_count_lang1=0, error_count_lang2=0";
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql) > 0;
        } catch (SQLException e) {
            System.err.println("Fehler beim Zurücksetzen: " + e.getMessage());
            return false;
        }
    }

    // ============ SCHLIESSEN ============
    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Schließfehler: " + e.getMessage());
        }
    }
}
