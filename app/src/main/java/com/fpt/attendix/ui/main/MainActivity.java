package com.fpt.attendix.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.fpt.attendix.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, new AttendanceFragment())
                    .commit();
        }
    }
}
