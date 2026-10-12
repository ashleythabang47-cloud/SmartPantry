package com.example.smartpantry.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

/** Settings screen. Changes are saved immediately in SharedPreferences. */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "pantry_prefs";
    public static final String KEY_ALERTS = "expiry_alerts_enabled";
    public static final String KEY_DAYS = "expiry_alert_days";

    private static final String[] DAY_LABELS = {"1 day", "3 days", "7 days"};
    private static final int[] DAY_VALUES = {1, 3, 7};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SwitchMaterial switchAlerts = findViewById(R.id.switchAlerts);
        Spinner spinnerDays = findViewById(R.id.spinnerDays);

        spinnerDays.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DAY_LABELS));

        // Load the saved values (defaults: alerts on, 3 days)
        boolean alertsOn = prefs.getBoolean(KEY_ALERTS, true);
        int savedDays = prefs.getInt(KEY_DAYS, 3);
        switchAlerts.setChecked(alertsOn);
        for (int i = 0; i < DAY_VALUES.length; i++) {
            if (DAY_VALUES[i] == savedDays) spinnerDays.setSelection(i);
        }
        spinnerDays.setEnabled(alertsOn);

        // Save whenever the user changes something
        switchAlerts.setOnCheckedChangeListener((button, isChecked) -> {
            prefs.edit().putBoolean(KEY_ALERTS, isChecked).apply();
            spinnerDays.setEnabled(isChecked);
        });

        spinnerDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                prefs.edit().putInt(KEY_DAYS, DAY_VALUES[pos]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
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
