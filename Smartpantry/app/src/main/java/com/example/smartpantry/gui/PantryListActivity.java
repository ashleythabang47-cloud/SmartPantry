package com.example.smartpantry.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapter.PantryAdapter;
import com.example.smartpantry.data.PantryDbHelper;
import com.example.smartpantry.data.PantryDbHelper.PantryItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Pantry List screen: shows every ingredient stored in SQLite (Read),
 * and launches the Add/Edit screen (Create / Update) or deletes an item (Delete).
 */
public class PantryListActivity extends AppCompatActivity
        implements PantryAdapter.OnItemActionListener {

    private PantryDbHelper dbHelper;
    private PantryAdapter adapter;
    private TextView tvEmpty;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry_list);
        setTitle("My Pantry");

        dbHelper = new PantryDbHelper(this);

        recyclerView = findViewById(R.id.recyclerPantry);
        tvEmpty = findViewById(R.id.tvEmpty);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        adapter = new PantryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Explicit Intent: open the Add/Edit screen in ADD mode (no extras)
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditItemActivity.class)));
    }

    /** Reload from the database every time the screen becomes visible again,
     *  so changes made in the Add/Edit screen appear when the user returns.
     *  Also re-reads the Settings so alert changes apply straight away. */
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        boolean alertsOn = prefs.getBoolean(SettingsActivity.KEY_ALERTS, true);
        int days = prefs.getInt(SettingsActivity.KEY_DAYS, 3);
        adapter.setExpiryAlerts(alertsOn, days);

        loadItems();
        if (alertsOn) showExpiryReminder(days);
    }

    /** Shows a short reminder if any ingredient is expired or expiring soon. */
    private void showExpiryReminder(int days) {
        int count = 0;
        for (PantryItem item : dbHelper.getAllItems()) {
            if (item.expiryDate != null && !item.expiryDate.isEmpty()
                    && PantryAdapter.daysUntil(item.expiryDate) <= days) {
                count++;
            }
        }
        if (count > 0) {
            Toast.makeText(this, count + (count == 1
                    ? " ingredient is expired or expiring soon"
                    : " ingredients are expired or expiring soon"),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadItems() {
        List<PantryItem> items = dbHelper.getAllItems();
        adapter.setItems(items);

        // Empty state instead of a blank screen
        boolean empty = items.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ---- Adapter callbacks ----

    /** Explicit Intent carrying the item's data to the Add/Edit screen (EDIT mode). */
    @Override
    public void onEdit(PantryItem item) {
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra(AddEditItemActivity.EXTRA_ID, item.id);
        intent.putExtra(AddEditItemActivity.EXTRA_NAME, item.name);
        intent.putExtra(AddEditItemActivity.EXTRA_QUANTITY, item.quantity);
        intent.putExtra(AddEditItemActivity.EXTRA_UNIT, item.unit);
        intent.putExtra(AddEditItemActivity.EXTRA_EXPIRY, item.expiryDate);
        startActivity(intent);
    }

    @Override
    public void onDelete(PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete ingredient")
                .setMessage("Remove " + item.name + " from your pantry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteItem(item.id);
                    Toast.makeText(this, item.name + " deleted", Toast.LENGTH_SHORT).show();
                    loadItems();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---- Toolbar menu (navigation) ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pantry_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_suggested) {
            startActivity(new Intent(this, SuggestedRecipesActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
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
