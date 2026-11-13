package com.vokabeltrainer.db;

import com.vokabeltrainer.model.Category;
import com.vokabeltrainer.model.Vokabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseHelperTest {

    private DatabaseHelper dbHelper;

    @BeforeEach
    void setUp() {
        dbHelper = new DatabaseHelper("jdbc:sqlite::memory:");
    }

    @AfterEach
    void tearDown() {
        dbHelper.close();
    }

    @Test
    void standardKategorienWerdenAngelegt() {
        List<Category> kategorien = dbHelper.getAllCategories();

        assertTrue(kategorien.stream().anyMatch(c -> c.getName().equals("Allgemein")));
        assertTrue(kategorien.stream().anyMatch(c -> c.getName().equals("Verben")));
    }

    @Test
    void vokabelMitZweiKategorienWirdMitBeidenGeladen() {
        Category verben = dbHelper.getCategoryByName("Verben");
        Category nomen = dbHelper.getCategoryByName("Nomen");

        Vokabel vokabel = new Vokabel("gehen", "to go", List.of(verben, nomen));
        assertTrue(dbHelper.addVokabel(vokabel));

        Vokabel geladen = dbHelper.getVokabelById(vokabel.getId());

        assertNotNull(geladen);
        assertEquals("gehen", geladen.getDeutsch());
        assertEquals("to go", geladen.getEnglisch());
        assertEquals(2, geladen.getCategories().size());
        assertTrue(geladen.getCategories().stream().anyMatch(c -> c.getName().equals("Verben")));
        assertTrue(geladen.getCategories().stream().anyMatch(c -> c.getName().equals("Nomen")));
    }

    @Test
    void vokabelOhneKategorieWirdMitLeererListeGeladen() {
        Vokabel vokabel = new Vokabel("Haus", "house", new ArrayList<>());
        assertTrue(dbHelper.addVokabel(vokabel));

        Vokabel geladen = dbHelper.getVokabelById(vokabel.getId());

        assertNotNull(geladen);
        assertTrue(geladen.getCategories().isEmpty());
    }

    @Test
    void updateVokabelErsetztKategorien() {
        Category verben = dbHelper.getCategoryByName("Verben");
        Category adjektive = dbHelper.getCategoryByName("Adjektive");

        Vokabel vokabel = new Vokabel("schnell", "fast", List.of(verben));
        dbHelper.addVokabel(vokabel);

        vokabel.setCategories(List.of(adjektive));
        assertTrue(dbHelper.updateVokabel(vokabel));

        Vokabel geladen = dbHelper.getVokabelById(vokabel.getId());

        assertEquals(1, geladen.getCategories().size());
        assertEquals("Adjektive", geladen.getCategories().get(0).getName());
    }

    @Test
    void searchVokabelnFindetInBeidenSprachen() {
        dbHelper.addVokabel(new Vokabel("Haus", "house", new ArrayList<>()));

        assertEquals(1, dbHelper.searchVokabeln("Haus").size());
        assertEquals(1, dbHelper.searchVokabeln("hous").size());
        assertEquals(0, dbHelper.searchVokabeln("baum").size());
    }

    @Test
    void deleteVokabelEntferntVokabel() {
        Vokabel vokabel = new Vokabel("Haus", "house", new ArrayList<>());
        dbHelper.addVokabel(vokabel);

        assertTrue(dbHelper.deleteVokabel(vokabel.getId()));
        assertNull(dbHelper.getVokabelById(vokabel.getId()));
        assertEquals(0, dbHelper.getVokabelCount());
    }
}
