package com.fpt.attendix.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.fpt.attendix.R;

public class ImagePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "EXTRA_IMAGE_URI";
    public static final String EXTRA_CONFIRMED_IMAGE_URI = "EXTRA_CONFIRMED_IMAGE_URI";

    private ImageView imageViewPreview;
    private Button buttonRetake, buttonConfirm;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setContentView(R.layout.activity_image_preview);

        // Initialize views
        imageViewPreview = findViewById(R.id.imageViewPreview);
        buttonRetake = findViewById(R.id.buttonRetake);
        buttonConfirm = findViewById(R.id.buttonConfirm);

        // Get image URI from intent
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            displayImage();
        } else {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupClickListeners();
    }

    private void displayImage() {
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(imageViewPreview);
    }

    private void setupClickListeners() {
        buttonRetake.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        buttonConfirm.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_CONFIRMED_IMAGE_URI, imageUri.toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
