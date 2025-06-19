package com.fpt.attendix.ui.main;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fpt.attendix.R;
import com.fpt.attendix.data.repository.Result;
import com.fpt.attendix.util.AppConstants;

public class AttendanceFragment extends Fragment {

    private AttendanceViewModel viewModel;

    private TextView textViewWifiStatus, textViewWifiName;
    private ImageView imageViewWifiIcon;
    private AutoCompleteTextView autoCompleteTextViewSlot;
    private Button buttonAttend;
    private ProgressBar progressBar;
    private String studentId;

    private String selectedSlot;
    private boolean isWifiValid = false;

    // Launcher for permissions
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA)) &&
                        (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                                Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION)))) {
                    launchCamera();
                } else {
                    Toast.makeText(requireContext(), R.string.attendance_permissions_required, Toast.LENGTH_LONG).show();
                }
            });

    // Launcher for CameraActivity
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String uriString = result.getData().getStringExtra(CameraActivity.EXTRA_IMAGE_URI);
                    if (uriString != null) {
                        Uri imageUri = Uri.parse(uriString);
                        if (studentId != null && !studentId.isEmpty()) {
                            viewModel.performAttendance(studentId, selectedSlot, imageUri);
                        } else {
                            Toast.makeText(requireContext(), R.string.attendance_student_id_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.attendance_cancelled, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        // Bind views
        textViewWifiStatus = view.findViewById(R.id.textViewWifiStatus);
        textViewWifiName = view.findViewById(R.id.textViewWifiName);
        imageViewWifiIcon = view.findViewById(R.id.imageViewWifiIcon);
        autoCompleteTextViewSlot = view.findViewById(R.id.autoCompleteTextViewSlot);
        buttonAttend = view.findViewById(R.id.buttonAttend);
        progressBar = view.findViewById(R.id.progressBar);

        setupSlotDropdown();
        setupObservers();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check WiFi status every time the fragment is resumed
        viewModel.checkWifiStatus();
    }

    private void setupSlotDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, AppConstants.ALLOWED_SLOTS);
        autoCompleteTextViewSlot.setAdapter(adapter);
        autoCompleteTextViewSlot.setOnItemClickListener((parent, view, position, id) -> {
            selectedSlot = (String) parent.getItemAtPosition(position);
        });
    }

    private void setupObservers() {
        viewModel.userDetails.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                this.studentId = user.getStudentId();
            }
        });
        viewModel.wifiStatus.observe(getViewLifecycleOwner(), wifiResult -> {
            isWifiValid = wifiResult.isValid;
            if (wifiResult.isValid) {
                textViewWifiStatus.setText(R.string.attendance_wifi_valid);
                textViewWifiName.setText(getString(R.string.attendance_wifi_ssid, wifiResult.ssid));
                imageViewWifiIcon.setImageResource(R.drawable.ic_wifi_on);
                imageViewWifiIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.design_default_color_primary));
            } else {
                textViewWifiStatus.setText(R.string.attendance_wifi_invalid);
                textViewWifiName.setText(wifiResult.ssid);
                imageViewWifiIcon.setImageResource(R.drawable.ic_wifi_off);
                imageViewWifiIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.design_default_color_error));
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonAttend.setEnabled(!isLoading);
        });

        viewModel.attendanceResult.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(requireContext(), R.string.attendance_success, Toast.LENGTH_LONG).show();
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Void>) result).getError();
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Toast.makeText(requireContext(), getString(R.string.attendance_error, errorMessage), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        buttonAttend.setOnClickListener(v -> {
            if (!isWifiValid) {
                Toast.makeText(requireContext(), R.string.attendance_connect_to_wifi, Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedSlot == null || selectedSlot.isEmpty()) {
                Toast.makeText(requireContext(), R.string.attendance_select_slot, Toast.LENGTH_SHORT).show();
                return;
            }
            checkPermissionsAndLaunchCamera();
        });
    }

    private void checkPermissionsAndLaunchCamera() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(getActivity(), CameraActivity.class);
        cameraLauncher.launch(intent);
    }
}
