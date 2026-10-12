# Smart Pantry Manager

A Java Android application that suggests recipes based strictly on the ingredients the user already has at home, to help cut food waste.

**Module:** Mobile App Development 700 - Practical Assignment


## Description

The user keeps a digital pantry of the ingredients they have. The app then suggests only the recipes they can cook right now, with no shopping trip needed. A recipe is suggested only if **every** ingredient it needs is in the pantry, in at least the required quantity. Recipes that are missing even one ingredient are never shown.

## Features

- Add, edit and delete pantry items (name, quantity, unit and optional expiry date)
- Pantry list screen using a RecyclerView with a custom adapter
- 19 recipes pre-loaded into the database on first run
- Suggested Recipes screen using strict matching, with a clear message when nothing matches
- Recipe Detail screen with the full ingredient list and numbered method
- Settings screen: expiring-soon alerts on/off and a warning window of 1, 3 or 7 days
- Expiring items highlighted in orange, expired items in red
- Input validation on the Add/Edit form
- Toolbar menu navigation between screens

## Strict-matching rule

Matching is done in a single SQL query in `PantryDbHelper.getSuggestedRecipes()`. A recipe is returned only if no ingredient is missing or short:

- Ingredient names are normalised, so singular and plural forms match (for example "Tomatoes" and "tomato").
- Quantities are converted to base units (g, ml, pcs) before they are saved, so "1 kg" matches a recipe asking for 200 g.
- If the pantry holds the same ingredient more than once, the quantities are added together before the check.

## Database choice: SQLite

The app uses **SQLite** through `SQLiteOpenHelper`. I chose it because:

- The data is small and belongs to one user, so a local on-device database is enough.
- It needs no server, account or internet connection, so the app works offline and is simple to set up and run.
- It supports the full CRUD cycle and keeps data after the app is closed and reopened.
- A single SQL query can express the strict-matching rule clearly.

Tables: `pantry_items`, `recipes`, `recipe_ingredients` (linked to `recipes` by a foreign key with cascade delete).

## Tech

- Java (no Kotlin code in the app)
- Android Studio, Gradle
- SQLite, RecyclerView, SharedPreferences, Intents

## Project structure

```
Smartpantry/app/src/main/java/com/example/smartpantry/
  data/      PantryDbHelper, UnitConverter
  ui/        PantryListActivity, AddEditItemActivity, SuggestedRecipesActivity,
             RecipeDetailActivity, SettingsActivity
  adapter/   PantryAdapter, RecipeAdapter
```

## Setup and run

1. Install [Android Studio](https://developer.android.com/studio).
2. Clone this repository.
3. In Android Studio choose **Open** and select the inner **`Smartpantry`** folder (the one that contains `settings.gradle.kts`), not the outer repository folder.
4. Wait for the Gradle sync to finish. Android Studio may offer to install missing SDK components; accept them.
5. Create an emulator in **Device Manager**, or connect an Android phone with USB debugging enabled.
6. Click **Run**. The app opens on the pantry screen.
7. The recipe database is created and filled automatically the first time the app runs.

## Not included (by design)

No maps, location or GPS features, as required by the assignment brief.
