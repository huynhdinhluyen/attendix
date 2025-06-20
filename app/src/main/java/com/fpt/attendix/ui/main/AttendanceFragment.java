package com.fpt.attendix.ui.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import com.fpt.attendix.data.model.SlotTime;
import com.fpt.attendix.data.repository.Result;
import com.fpt.attendix.util.AppConstants;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Locale;

public class AttendanceFragment extends Fragment {

    private AttendanceViewModel viewModel;

    private TextView textViewWifiStatus, textViewWifiName;
    private ImageView imageViewWifiIcon;
    private AutoCompleteTextView autoCompleteTextViewSlot;
    private Button buttonAttend, buttonLogout;
    private ProgressBar progressBar;
    private String studentId;

    private String selectedSlot;
    private boolean isWifiValid = false;

    // Launcher for permissions
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean locationGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                if (locationGranted) {
                    viewModel.checkWifiStatus();
                }

                if (cameraGranted && locationGranted) {
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

        textViewWifiStatus = view.findViewById(R.id.textViewWifiStatus);
        textViewWifiName = view.findViewById(R.id.textViewWifiName);
        imageViewWifiIcon = view.findViewById(R.id.imageViewWifiIcon);
        autoCompleteTextViewSlot = view.findViewById(R.id.autoCompleteTextViewSlot);
        buttonAttend = view.findViewById(R.id.buttonAttend);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        progressBar = view.findViewById(R.id.progressBar);

        setupSlotDropdown();
        setupObservers();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.fetchUserDetails();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.checkWifiStatus();
        } else {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    private void setupSlotDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, AppConstants.ALLOWED_SLOTS);
        autoCompleteTextViewSlot.setAdapter(adapter);
        autoCompleteTextViewSlot.setOnItemClickListener((parent, view, position, id) -> {
            selectedSlot = (String) parent.getItemAtPosition(position);
            viewModel.checkSlotAttendanceStatus(selectedSlot);
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
                imageViewWifiIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.attendance_wifi_valid));
            } else {
                textViewWifiStatus.setText(R.string.attendance_wifi_invalid);
                textViewWifiName.setText(wifiResult.ssid);
                imageViewWifiIcon.setImageResource(R.drawable.ic_wifi_off);
                imageViewWifiIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.attendance_error));
            }
        });

        viewModel.slotAttendanceStatus.observe(getViewLifecycleOwner(), status -> {
            if (status != null && status.slot != null && status.slot.equals(selectedSlot)) {
                updateAttendanceButton(status);
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading && selectedSlot != null) {
                viewModel.checkSlotAttendanceStatus(selectedSlot);
            }
        });

        viewModel.attendanceResult.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(requireContext(), R.string.attendance_success, Toast.LENGTH_LONG).show();
                autoCompleteTextViewSlot.setText("");
                selectedSlot = null;
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Void>) result).getError();
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Toast.makeText(requireContext(), getString(R.string.attendance_error, errorMessage), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateAttendanceButton(AttendanceViewModel.SlotAttendanceStatus status) {
        buttonAttend.setBackgroundResource(R.drawable.button_attendance);

        if (status.hasAttended) {
            buttonAttend.setText(R.string.attendance_already_attended);
            buttonAttend.setEnabled(false);
            buttonAttend.getBackground().setTint(ContextCompat.getColor(requireContext(), R.color.attendance_disabled));
            buttonAttend.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            buttonAttend.setText(R.string.attendance_button_text);
            boolean isLoading = Boolean.TRUE.equals(viewModel.isLoading.getValue());
            buttonAttend.setEnabled(isWifiValid && !isLoading);
            buttonAttend.getBackground().setTint(ContextCompat.getColor(requireContext(), R.color.attendance_primary));
            buttonAttend.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }
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

            AttendanceViewModel.SlotAttendanceStatus currentStatus = viewModel.slotAttendanceStatus.getValue();
            if (currentStatus != null && currentStatus.hasAttended) {
                Toast.makeText(requireContext(), "Bạn đã điểm danh slot này ngày hôm nay", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isCurrentTimeInSlot(selectedSlot)) {
                SlotTime slotTime = AppConstants.SLOT_TIMES.get(selectedSlot);
                String timeRange = slotTime != null ?
                        String.format(Locale.getDefault(), "(%02d:%02d - %02d:%02d)",
                                slotTime.startHour, slotTime.startMinute,
                                slotTime.endHour, slotTime.endMinute) : "";
                Toast.makeText(requireContext(),
                        "Bạn chỉ có thể điểm danh trong thời gian của slot học " + timeRange,
                        Toast.LENGTH_LONG).show();
                return;
            }

            checkPermissionsAndLaunchCamera();
        });
        buttonLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireActivity(), com.fpt.attendix.ui.auth.AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private boolean isCurrentTimeInSlot(String slot) {
        SlotTime slotTime = AppConstants.SLOT_TIMES.get(slot);
        if (slotTime == null) {
            return false;
        }

        Calendar currentTime = Calendar.getInstance();
        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
        int currentMinute = currentTime.get(Calendar.MINUTE);

        int currentTotalMinutes = currentHour * 60 + currentMinute;
        int startTotalMinutes = slotTime.startHour * 60 + slotTime.startMinute;
        int endTotalMinutes = slotTime.endHour * 60 + slotTime.endMinute;

        return currentTotalMinutes >= startTotalMinutes && currentTotalMinutes <= endTotalMinutes;
    }

    private void checkPermissionsAndLaunchCamera() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};
        boolean cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (cameraGranted && locationGranted) {
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
