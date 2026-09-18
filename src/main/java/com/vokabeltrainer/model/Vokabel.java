package com.vokabeltrainer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vokabel {
    private int id;
    private String deutsch;
    private String englisch;
    private int errorCountLang1 = 0;
    private int errorCountLang2 = 0;
    private long lastReviewed = 0;
    private List<Category> categories = new ArrayList<>();

    public static final int ERROR_THRESHOLD = -5;

    // Konstruktoren
    public Vokabel() {}

    public Vokabel(String deutsch, String englisch) {
        this.deutsch = deutsch;
        this.englisch = englisch;
    }

    public Vokabel(String deutsch, String englisch, List<Category> categories) {
        this.deutsch = deutsch;
        this.englisch = englisch;
        this.categories = categories;
    }

    public Vokabel(int id, String deutsch, String englisch,
                 int errorCountLang1, int errorCountLang2,
                 long lastReviewed, List<Category> categories) {
        this.id = id;
        this.deutsch = deutsch;
        this.englisch = englisch;
        this.errorCountLang1 = errorCountLang1;
        this.errorCountLang2 = errorCountLang2;
        this.lastReviewed = lastReviewed;
        this.categories = categories;
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDeutsch() { return deutsch; }
    public void setDeutsch(String deutsch) { this.deutsch = deutsch; }
    public String getEnglisch() { return englisch; }
    public void setEnglisch(String englisch) { this.englisch = englisch; }
    public int getErrorCountLang1() { return errorCountLang1; }
    public void setErrorCountLang1(int errorCountLang1) { this.errorCountLang1 = errorCountLang1; }
    public int getErrorCountLang2() { return errorCountLang2; }
    public void setErrorCountLang2(int errorCountLang2) { this.errorCountLang2 = errorCountLang2; }
    public long getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(long lastReviewed) { this.lastReviewed = lastReviewed; }
    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }
    public void addCategory(Category category) { this.categories.add(category); }

    public boolean isErrorLang1() { return errorCountLang1 <= ERROR_THRESHOLD; }
    public boolean isErrorLang2() { return errorCountLang2 <= ERROR_THRESHOLD; }

    public void incrementErrorCount(boolean lang1ToLang2) {
        if (lang1ToLang2) errorCountLang1++;
        else errorCountLang2++;
        lastReviewed = System.currentTimeMillis();
    }

    public void decrementErrorCount(boolean lang1ToLang2) {
        if (lang1ToLang2) {
            errorCountLang1--;
            if (errorCountLang1 > 0) errorCountLang1 = ERROR_THRESHOLD;
        } else {
            errorCountLang2--;
            if (errorCountLang2 > 0) errorCountLang2 = ERROR_THRESHOLD;
        }
        lastReviewed = System.currentTimeMillis();
    }
}
