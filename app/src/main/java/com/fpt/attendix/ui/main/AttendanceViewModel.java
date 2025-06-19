package com.fpt.attendix.ui.main;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fpt.attendix.R;
import com.fpt.attendix.data.model.AttendanceRecord;
import com.fpt.attendix.data.model.User;
import com.fpt.attendix.data.repository.AttendanceRepository;
import com.fpt.attendix.data.repository.Result;
import com.fpt.attendix.util.AppConstants;

public class AttendanceViewModel extends AndroidViewModel {

    private final AttendanceRepository attendanceRepository;

    private final MutableLiveData<WifiCheckResult> _wifiStatus = new MutableLiveData<>();
    public final LiveData<WifiCheckResult> wifiStatus = _wifiStatus;
    private final MutableLiveData<User> _userDetails = new MutableLiveData<>();
    public final LiveData<User> userDetails = _userDetails;

    private final MutableLiveData<Result<Void>> _attendanceResult = new MutableLiveData<>();
    public final LiveData<Result<Void>> attendanceResult = _attendanceResult;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    public AttendanceViewModel(Application application) {
        super(application);
        this.attendanceRepository = AttendanceRepository.getInstance();
        fetchUserDetails();
    }

    public void fetchUserDetails() {
        String uid = attendanceRepository.getCurrentUserId();
        if (uid != null) {
            attendanceRepository.getUserDetails(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    _userDetails.setValue(user);
                }
            }).addOnFailureListener(e -> {
            });
        }
    }

    public void checkWifiStatus() {
        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            _wifiStatus.postValue(new WifiCheckResult(false, "Wi-Fi service not available"));
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            String currentSsid = wifiInfo.getSSID();

            if (currentSsid != null && currentSsid.startsWith("\"") && currentSsid.endsWith("\"")) {
                currentSsid = currentSsid.substring(1, currentSsid.length() - 1);
            }

            boolean isSsidValid = false;
            if (currentSsid != null) {
                for (String allowedSsid : AppConstants.ALLOWED_WIFI_SSIDS) {
                    if (allowedSsid.equals(currentSsid)) {
                        isSsidValid = true;
                        break;
                    }
                }
            }

            if (isSsidValid) {
                _wifiStatus.postValue(new WifiCheckResult(true, currentSsid));
            } else {
                _wifiStatus.postValue(new WifiCheckResult(false, getApplication().getString(R.string.attendance_wifi_invalid)));
            }
        } else {
            _wifiStatus.postValue(new WifiCheckResult(false, getApplication().getString(R.string.attendance_connect_to_wifi)));
        }
    }

    public void performAttendance(String studentId, String slot, Uri imageUri) {
        _isLoading.setValue(true);
        String userId = attendanceRepository.getCurrentUserId();
        if (userId == null) {
            _attendanceResult.setValue(new Result.Error<>(new Exception("User not logged in.")));
            _isLoading.setValue(false);
            return;
        }

        // 1. Check if already attended
        attendanceRepository.hasAttendedToday(userId, slot).addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                _attendanceResult.setValue(new Result.Error<>(new Exception("You have already attended this slot today.")));
                _isLoading.setValue(false);
            } else if (task.isSuccessful()) {
                // 2. Not attended, proceed to upload image
                uploadImageAndSaveRecord(userId, studentId, slot, imageUri);
            } else {
                _attendanceResult.setValue(new Result.Error<>(task.getException()));
                _isLoading.setValue(false);
            }
        });
    }

    private void uploadImageAndSaveRecord(String userId, String studentId, String slot, Uri imageUri) {
        attendanceRepository.uploadAttendanceImage(imageUri).addOnCompleteListener(uploadTask -> {
            if (uploadTask.isSuccessful()) {
                // 3. Get image URL
                uploadTask.getResult().getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveRecordToFirestore(userId, studentId, slot, imageUrl);
                }).addOnFailureListener(e -> {
                    _attendanceResult.setValue(new Result.Error<>(e));
                    _isLoading.setValue(false);
                });
            } else {
                _attendanceResult.setValue(new Result.Error<>(uploadTask.getException()));
                _isLoading.setValue(false);
            }
        });
    }

    private void saveRecordToFirestore(String userId, String studentId, String slot, String imageUrl) {
        WifiInfo wifiInfo = attendanceRepository.getCurrentWifiInfo(getApplication());
        String ssid = wifiInfo != null ? wifiInfo.getSSID().replace("\"", "") : "N/A";
        String bssid = wifiInfo != null ? wifiInfo.getBSSID() : "N/A";

        AttendanceRecord record = new AttendanceRecord(userId, studentId, slot, ssid, bssid, imageUrl);

        // 4. Save the final record
        attendanceRepository.saveAttendanceRecord(record).addOnCompleteListener(saveTask -> {
            if (saveTask.isSuccessful()) {
                _attendanceResult.setValue(new Result.Success<>(null));
            } else {
                _attendanceResult.setValue(new Result.Error<>(saveTask.getException()));
            }
            _isLoading.setValue(false);
        });
    }

    // Helper class for Wifi Status LiveData
    public static class WifiCheckResult {
        public final boolean isValid;
        public final String ssid;
        public WifiCheckResult(boolean isValid, String ssid) {
            this.isValid = isValid;
            this.ssid = ssid;
        }
    }
}