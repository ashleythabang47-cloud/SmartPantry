package com.example.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Local SQLite database for the Smart Pantry Manager.
 * Tables: pantry_items, recipes, recipe_ingredients.
 * All quantities are stored in BASE units: "g", "ml" or "pcs".
 * Convert kg/l/cup/tbsp etc. to base units BEFORE calling addItem/updateItem.
 */
public class PantryDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    public PantryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // enables ON DELETE CASCADE
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pantry_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"              // what the user typed, shown in the UI
                + "name_normalized TEXT NOT NULL,"   // used for matching
                + "quantity REAL NOT NULL,"
                + "unit TEXT NOT NULL,"              // g | ml | pcs
                + "expiry_date TEXT)");              // optional, yyyy-MM-dd

        db.execSQL("CREATE TABLE recipes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "steps TEXT NOT NULL)");           // steps separated by \n

        db.execSQL("CREATE TABLE recipe_ingredients ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "recipe_id INTEGER NOT NULL,"
                + "name_normalized TEXT NOT NULL,"
                + "quantity REAL NOT NULL,"
                + "unit TEXT NOT NULL,"
                + "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE)");

        seedRecipes(db); // runs once, when the database file is first created
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS recipe_ingredients");
        db.execSQL("DROP TABLE IF EXISTS recipes");
        db.execSQL("DROP TABLE IF EXISTS pantry_items");
        onCreate(db);
    }


    // Normalisation: "Tomatoes " -> "tomato", "Eggs" -> "egg"

    public static String normalize(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (s.length() <= 3) return s;
        if (s.endsWith("ies")) return s.substring(0, s.length() - 3) + "y";   // berries -> berry
        if (s.endsWith("oes") || s.endsWith("ches") || s.endsWith("shes")
                || s.endsWith("xes") || s.endsWith("sses")) {
            return s.substring(0, s.length() - 2);                            // tomatoes -> tomato
        }
        if (s.endsWith("s") && !s.endsWith("ss") && !s.endsWith("us")) {
            return s.substring(0, s.length() - 1);                            // eggs -> egg
        }
        return s;
    }


    // Pantry CRUD

    public long addItem(PantryItem item) {
        return getWritableDatabase().insert("pantry_items", null, toValues(item));
    }

    public List<PantryItem> getAllItems() {
        List<PantryItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("pantry_items", null, null, null,
                null, null, "name COLLATE NOCASE")) {
            while (c.moveToNext()) {
                list.add(new PantryItem(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getDouble(c.getColumnIndexOrThrow("quantity")),
                        c.getString(c.getColumnIndexOrThrow("unit")),
                        c.getString(c.getColumnIndexOrThrow("expiry_date"))));
            }
        }
        return list;
    }

    public int updateItem(PantryItem item) {
        return getWritableDatabase().update("pantry_items", toValues(item),
                "id = ?", new String[]{String.valueOf(item.id)});
    }

    public int deleteItem(long id) {
        return getWritableDatabase().delete("pantry_items", "id = ?",
                new String[]{String.valueOf(id)});
    }

    private ContentValues toValues(PantryItem item) {
        ContentValues v = new ContentValues();
        v.put("name", item.name.trim());
        v.put("name_normalized", normalize(item.name));
        v.put("quantity", item.quantity);
        v.put("unit", item.unit);
        v.put("expiry_date", item.expiryDate); // null is fine
        return v;
    }


    // STRICT MATCHING (core logic)
    // A recipe is returned only if NO ingredient is missing or short.
    // Pantry rows for the same ingredient are summed first.

    public List<Recipe> getSuggestedRecipes() {
        String sql = "SELECT r.id, r.name, r.steps FROM recipes r "
                + "WHERE NOT EXISTS ("
                + "  SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id = r.id AND "
                + "  COALESCE((SELECT SUM(p.quantity) FROM pantry_items p "
                + "            WHERE p.name_normalized = ri.name_normalized "
                + "            AND p.unit = ri.unit), 0) < ri.quantity"
                + ") ORDER BY r.name";
        List<Recipe> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                list.add(new Recipe(c.getLong(0), c.getString(1), c.getString(2)));
            }
        }
        return list;
    }

    public List<String> getIngredientLines(long recipeId) {
        List<String> lines = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("recipe_ingredients",
                new String[]{"name_normalized", "quantity", "unit"},
                "recipe_id = ?", new String[]{String.valueOf(recipeId)},
                null, null, "name_normalized")) {
            while (c.moveToNext()) {
                lines.add(String.format(Locale.US, "%s - %.0f %s",
                        c.getString(0), c.getDouble(1), c.getString(2)));
            }
        }
        return lines;
    }


    // Seed data: 20 recipes, inserted once on first run.
    // Ingredients are listed as name, quantity, unit (base units g/ml/pcs).

    private void seedRecipes(SQLiteDatabase db) {
        add(db, "Cheese Omelette",
                "Beat the eggs.\nMelt butter in a pan.\nPour in eggs and add cheese.\nFold and serve.",
                "egg", "3", "pcs", "cheese", "50", "g", "butter", "10", "g");
        add(db, "Tomato Pasta",
                "Boil pasta until tender.\nSaute chopped tomatoes and garlic in oil.\nToss pasta in the sauce.",
                "pasta", "200", "g", "tomato", "3", "pcs", "garlic", "2", "pcs", "olive oil", "30", "ml");
        add(db, "Scrambled Eggs on Toast",
                "Toast the bread.\nWhisk eggs with milk.\nScramble in melted butter.\nServe on toast.",
                "egg", "2", "pcs", "bread", "2", "pcs", "butter", "10", "g", "milk", "30", "ml");
        add(db, "Egg Fried Rice",
                "Cook and cool the rice.\nFry chopped onion in oil.\nAdd rice and eggs, stir until cooked.\nSeason with soy sauce.",
                "rice", "200", "g", "egg", "2", "pcs", "onion", "1", "pcs", "soy sauce", "20", "ml", "cooking oil", "15", "ml");
        add(db, "Pancakes",
                "Whisk flour, egg, milk and sugar into a batter.\nMelt butter in a pan.\nCook ladles of batter until golden on both sides.",
                "flour", "150", "g", "egg", "1", "pcs", "milk", "250", "ml", "sugar", "20", "g", "butter", "15", "g");
        add(db, "Cheese Toastie",
                "Butter the bread.\nPlace cheese between the slices.\nFry until golden on both sides.",
                "bread", "2", "pcs", "cheese", "40", "g", "butter", "10", "g");
        add(db, "Garlic Butter Pasta",
                "Boil pasta until tender.\nMelt butter and gently fry crushed garlic.\nToss pasta in the garlic butter.",
                "pasta", "200", "g", "garlic", "3", "pcs", "butter", "30", "g");
        add(db, "Tomato Soup",
                "Fry onion and garlic in butter.\nAdd chopped tomatoes and stock.\nSimmer for 20 minutes.\nBlend until smooth.",
                "tomato", "6", "pcs", "onion", "1", "pcs", "garlic", "2", "pcs", "vegetable stock", "500", "ml", "butter", "15", "g");
        add(db, "Mashed Potatoes",
                "Peel and boil the potatoes until soft.\nDrain and mash with butter and milk.",
                "potato", "500", "g", "butter", "30", "g", "milk", "60", "ml");
        add(db, "Roast Potatoes",
                "Heat the oven to 200C.\nToss chopped potatoes with oil and crushed garlic.\nRoast for 40 minutes, turning once.",
                "potato", "600", "g", "cooking oil", "30", "ml", "garlic", "2", "pcs");
        add(db, "Chicken Stir Fry",
                "Slice the chicken, onion and carrots.\nStir-fry the chicken in hot oil until cooked.\nAdd vegetables and soy sauce, cook for 5 minutes.",
                "chicken", "300", "g", "onion", "1", "pcs", "carrot", "2", "pcs", "soy sauce", "30", "ml", "cooking oil", "15", "ml");
        add(db, "Egg Fried Noodles",
                "Boil the noodles and drain.\nScramble eggs in hot oil.\nAdd noodles and soy sauce, toss for 2 minutes.",
                "noodle", "200", "g", "egg", "2", "pcs", "soy sauce", "20", "ml", "cooking oil", "15", "ml");
        add(db, "Bean Chilli",
                "Fry onion and garlic.\nAdd chopped tomatoes and beans.\nSimmer for 25 minutes.",
                "kidney bean", "400", "g", "tomato", "4", "pcs", "onion", "1", "pcs", "garlic", "2", "pcs");
        add(db, "Chicken and Rice",
                "Brown the chicken with chopped onion.\nAdd rice and stock.\nCover and simmer for 20 minutes until rice is cooked.",
                "chicken", "300", "g", "rice", "200", "g", "onion", "1", "pcs", "vegetable stock", "500", "ml");
        add(db, "Banana Pancakes",
                "Mash the bananas.\nMix in eggs and flour.\nFry small spoonfuls until golden.",
                "banana", "2", "pcs", "egg", "2", "pcs", "flour", "50", "g");
        add(db, "Vegetable Omelette",
                "Chop the onion and tomato.\nFry in oil for 2 minutes.\nPour in beaten eggs and cook until set.",
                "egg", "3", "pcs", "onion", "1", "pcs", "tomato", "1", "pcs", "cooking oil", "10", "ml");
        add(db, "French Toast",
                "Whisk eggs, milk and sugar.\nSoak the bread slices.\nFry in butter until golden.",
                "bread", "4", "pcs", "egg", "2", "pcs", "milk", "100", "ml", "sugar", "10", "g", "butter", "10", "g");
        add(db, "Tuna Pasta",
                "Boil pasta until tender.\nFry chopped onion in olive oil.\nStir in tuna and toss with the pasta.",
                "pasta", "200", "g", "tuna", "185", "g", "onion", "1", "pcs", "olive oil", "15", "ml");
        add(db, "Rice and Beans",
                "Fry onion and garlic.\nAdd beans and heat through.\nServe over cooked rice.",
                "rice", "200", "g", "kidney bean", "400", "g", "onion", "1", "pcs", "garlic", "2", "pcs");
    }

    /** ingredients = flat triples: name, quantity, unit, name, quantity, unit, ... */
    private void add(SQLiteDatabase db, String name, String steps, String... ingredients) {
        ContentValues r = new ContentValues();
        r.put("name", name);
        r.put("steps", steps);
        long recipeId = db.insert("recipes", null, r);
        for (int i = 0; i + 2 < ingredients.length; i += 3) {
            ContentValues v = new ContentValues();
            v.put("recipe_id", recipeId);
            v.put("name_normalized", normalize(ingredients[i]));
            v.put("quantity", Double.parseDouble(ingredients[i + 1]));
            v.put("unit", ingredients[i + 2]);
            db.insert("recipe_ingredients", null, v);
        }
    }


    // Simple models

    public static class PantryItem {
        public long id;
        public String name;
        public double quantity;
        public String unit;
        public String expiryDate;

        public PantryItem(long id, String name, double quantity, String unit, String expiryDate) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.expiryDate = expiryDate;
        }
    }

    public static class Recipe {
        public long id;
        public String name;
        public String steps;

        public Recipe(long id, String name, String steps) {
            this.id = id;
            this.name = name;
            this.steps = steps;
        }
    }
}