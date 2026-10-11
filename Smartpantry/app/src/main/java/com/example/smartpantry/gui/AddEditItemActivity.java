package com.example.smartpantry.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.data.PantryDbHelper;
import com.example.smartpantry.data.PantryDbHelper.PantryItem;
import com.example.smartpantry.data.UnitConverter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

/**
 * Add / Edit Ingredient screen.
 * ADD mode:  launched with no extras.
 * EDIT mode: launched with the EXTRA_* values below (passed through an Intent).
 */
public class AddEditItemActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "item_id";
    public static final String EXTRA_NAME = "item_name";
    public static final String EXTRA_QUANTITY = "item_quantity";
    public static final String EXTRA_UNIT = "item_unit";
    public static final String EXTRA_EXPIRY = "item_expiry";

    private static final double MAX_QUANTITY = 100000;

    private TextInputLayout tilName, tilQuantity;
    private TextInputEditText etName, etQuantity, etExpiry;
    private Spinner spinnerUnit;
    private PantryDbHelper dbHelper;

    private long itemId = -1; // -1 means "adding a new item"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        dbHelper = new PantryDbHelper(this);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tilName = findViewById(R.id.tilName);
        tilQuantity = findViewById(R.id.tilQuantity);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnClearDate = findViewById(R.id.btnClearDate);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Unit dropdown
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UnitConverter.SUPPORTED_UNITS);
        spinnerUnit.setAdapter(unitAdapter);

        // EDIT mode: read the data passed in by the Intent and pre-fill the form
        itemId = getIntent().getLongExtra(EXTRA_ID, -1);
        if (itemId != -1) {
            tvTitle.setText("Edit Ingredient");
            etName.setText(getIntent().getStringExtra(EXTRA_NAME));

            double qty = getIntent().getDoubleExtra(EXTRA_QUANTITY, 0);
            etQuantity.setText(qty == Math.rint(qty)
                    ? String.valueOf((long) qty) : String.valueOf(qty));

            String unit = getIntent().getStringExtra(EXTRA_UNIT);
            for (int i = 0; i < UnitConverter.SUPPORTED_UNITS.length; i++) {
                if (UnitConverter.SUPPORTED_UNITS[i].equals(unit)) {
                    spinnerUnit.setSelection(i);
                }
            }
            etExpiry.setText(getIntent().getStringExtra(EXTRA_EXPIRY));
        }

        etExpiry.setOnClickListener(v -> showDatePicker());
        btnClearDate.setOnClickListener(v -> etExpiry.setText(""));
        btnSave.setOnClickListener(v -> saveItem());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                etExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /** Validates the form; saves and closes the screen only if everything is valid. */
    private void saveItem() {
        tilName.setError(null);
        tilQuantity.setError(null);

        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String qtyText = etQuantity.getText() == null ? "" : etQuantity.getText().toString().trim();
        String expiry = etExpiry.getText() == null ? "" : etExpiry.getText().toString().trim();

        boolean valid = true;

        // Name: required, must contain at least one letter
        if (name.isEmpty()) {
            tilName.setError("Please enter an ingredient name");
            valid = false;
        } else if (!name.matches(".*[A-Za-z].*")) {
            tilName.setError("Name must contain letters");
            valid = false;
        }

        // Quantity: required, numeric, greater than 0, within a sensible limit
        double quantity = 0;
        if (qtyText.isEmpty()) {
            tilQuantity.setError("Please enter a quantity");
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(qtyText);
                if (quantity <= 0) {
                    tilQuantity.setError("Quantity must be greater than 0");
                    valid = false;
                } else if (quantity > MAX_QUANTITY) {
                    tilQuantity.setError("Quantity is too large");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                tilQuantity.setError("Enter a valid number");
                valid = false;
            }
        }

        if (!valid) return;

        // Convert to base units (g / ml / pcs) before saving
        String selectedUnit = (String) spinnerUnit.getSelectedItem();
        double baseQty = UnitConverter.toBase(quantity, selectedUnit);
        String baseUnit = UnitConverter.baseUnit(selectedUnit);

        PantryItem item = new PantryItem(itemId, name, baseQty, baseUnit,
                expiry.isEmpty() ? null : expiry);

        if (itemId == -1) {
            dbHelper.addItem(item);
            Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updateItem(item);
            Toast.makeText(this, "Ingredient updated", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { // toolbar back arrow
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
