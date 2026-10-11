package com.example.smartpantry.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.data.PantryDbHelper;

import java.util.List;

/** Recipe Detail screen: full ingredient list and numbered method. */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "recipe_id";
    public static final String EXTRA_NAME = "recipe_name";
    public static final String EXTRA_STEPS = "recipe_steps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Data passed in by the Intent
        long recipeId = getIntent().getLongExtra(EXTRA_ID, -1);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String steps = getIntent().getStringExtra(EXTRA_STEPS);

        setTitle("Recipe");
        ((TextView) findViewById(R.id.tvRecipeTitle)).setText(name);

        // Ingredients come from the database
        PantryDbHelper dbHelper = new PantryDbHelper(this);
        List<String> lines = dbHelper.getIngredientLines(recipeId);
        dbHelper.close();

        StringBuilder ingredients = new StringBuilder();
        for (String line : lines) {
            String pretty = line.substring(0, 1).toUpperCase() + line.substring(1);
            ingredients.append("\u2022 ").append(pretty).append("\n");
        }
        ((TextView) findViewById(R.id.tvIngredients)).setText(ingredients.toString().trim());

        // Steps are stored separated by newlines; number them for display
        StringBuilder method = new StringBuilder();
        if (steps != null) {
            String[] parts = steps.split("\n");
            for (int i = 0; i < parts.length; i++) {
                method.append(i + 1).append(". ").append(parts[i].trim()).append("\n");
            }
        }
        ((TextView) findViewById(R.id.tvSteps)).setText(method.toString().trim());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
