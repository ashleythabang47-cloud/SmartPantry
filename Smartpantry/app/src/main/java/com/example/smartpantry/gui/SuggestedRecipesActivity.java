package com.example.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapter.RecipeAdapter;
import com.example.smartpantry.data.PantryDbHelper;
import com.example.smartpantry.data.PantryDbHelper.Recipe;

import java.util.List;

/**
 * Suggested Recipes screen. Runs the strict-matching query against the
 * current pantry and lists ONLY recipes whose every ingredient is available
 * in at least the required quantity.
 */
public class SuggestedRecipesActivity extends AppCompatActivity
        implements RecipeAdapter.OnRecipeClickListener {

    private PantryDbHelper dbHelper;
    private RecipeAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvSummary, tvNoMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);
        setTitle("Suggested Recipes");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new PantryDbHelper(this);
        recyclerView = findViewById(R.id.recyclerRecipes);
        tvSummary = findViewById(R.id.tvSummary);
        tvNoMatches = findViewById(R.id.tvNoMatches);

        adapter = new RecipeAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /** Re-run the matching every time the screen is shown, so the list always
     *  reflects the current pantry. */
    @Override
    protected void onResume() {
        super.onResume();
        loadSuggestions();
    }

    private void loadSuggestions() {
        List<Recipe> suggested = dbHelper.getSuggestedRecipes();
        adapter.setRecipes(suggested);

        if (suggested.isEmpty()) {
            tvSummary.setText("");
            tvNoMatches.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvSummary.setText(suggested.size() == 1
                    ? "1 recipe you can cook right now"
                    : suggested.size() + " recipes you can cook right now");
            tvNoMatches.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /** Open the detail screen, passing the recipe's data through the Intent. */
    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_ID, recipe.id);
        intent.putExtra(RecipeDetailActivity.EXTRA_NAME, recipe.name);
        intent.putExtra(RecipeDetailActivity.EXTRA_STEPS, recipe.steps);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
