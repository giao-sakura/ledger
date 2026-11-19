package com.example.ledger;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ledger.databinding.ActivitySettingsBinding;
import com.example.ledger.update.UpdateFlow;

public class SettingsActivity extends AppCompatActivity {
    private UpdateFlow updateFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.backButton.setOnClickListener(v -> finish());
        updateFlow = new UpdateFlow(this, binding.versionStatus);
        updateFlow.attach(binding.updateRow);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updateFlow != null) {
            updateFlow.onResume();
        }
    }
}
