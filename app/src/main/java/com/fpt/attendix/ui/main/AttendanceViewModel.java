package com.fpt.attendix.ui.main;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fpt.attendix.R;
import com.fpt.attendix.data.model.AttendanceRecord;
import com.fpt.attendix.data.model.SlotTime;
import com.fpt.attendix.data.model.User;
import com.fpt.attendix.data.repository.AttendanceRepository;
import com.fpt.attendix.data.repository.Result;
import com.fpt.attendix.util.AppConstants;

import java.util.Calendar;
import java.util.Locale;

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
    private final MutableLiveData<SlotAttendanceStatus> _slotAttendanceStatus = new MutableLiveData<>();
    public final LiveData<SlotAttendanceStatus> slotAttendanceStatus = _slotAttendanceStatus;

    private String currentSlot = null;

    public AttendanceViewModel(Application application) {
        super(application);
        this.attendanceRepository = AttendanceRepository.getInstance();
        fetchUserDetails();
    }

    public void checkSlotAttendanceStatus(String slot) {
        if (slot == null || slot.isEmpty()) {
            _slotAttendanceStatus.setValue(new SlotAttendanceStatus(false, slot, "Hãy chọn slot"));
            return;
        }

        currentSlot = slot;
        String userId = attendanceRepository.getCurrentUserId();
        if (userId == null) {
            _slotAttendanceStatus.setValue(new SlotAttendanceStatus(false, slot, "Người dùng chưa đăng nhập"));
            return;
        }

        _slotAttendanceStatus.setValue(new SlotAttendanceStatus(false, slot, "Đang kiểm tra..."));

        attendanceRepository.hasAttendedToday(userId, slot).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Calendar startOfDay = Calendar.getInstance();
                startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                startOfDay.set(Calendar.MINUTE, 0);
                startOfDay.set(Calendar.SECOND, 0);

                Calendar endOfDay = Calendar.getInstance();
                endOfDay.set(Calendar.HOUR_OF_DAY, 23);
                endOfDay.set(Calendar.MINUTE, 59);
                endOfDay.set(Calendar.SECOND, 59);

                boolean hasAttendedToday = task.getResult().getDocuments().stream()
                        .anyMatch(doc -> {
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                            if (timestamp == null) return false;

                            long docTime = timestamp.toDate().getTime();
                            return docTime >= startOfDay.getTimeInMillis() &&
                                    docTime <= endOfDay.getTimeInMillis();
                        });

                if (hasAttendedToday) {
                    _slotAttendanceStatus.setValue(new SlotAttendanceStatus(true, slot, "Bạn đã điểm danh hôm nay"));
                } else {
                    _slotAttendanceStatus.setValue(new SlotAttendanceStatus(false, slot, "Điểm danh"));
                }
            } else {
                _slotAttendanceStatus.setValue(new SlotAttendanceStatus(false, slot, "Lỗi kiểm tra trạng thái"));
            }
        });
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
        // Check if we have location permission first
        if (ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            _wifiStatus.postValue(new WifiCheckResult(false, "Location permission required"));
            return;
        }

        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            _wifiStatus.postValue(new WifiCheckResult(false, "Wi-Fi service not available"));
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            String currentSsid = wifiInfo.getSSID();
            String currentBssid = wifiInfo.getBSSID();

            if (currentSsid == null || currentSsid.equals("<unknown ssid>") || currentSsid.equals("\"<unknown ssid>\"")) {

                if (AppConstants.ENABLE_SMART_WIFI_VALIDATION && isWifiConnectionValid(wifiInfo)) {
                    _wifiStatus.postValue(new WifiCheckResult(true, "Valid Network"));
                } else {
                    _wifiStatus.postValue(new WifiCheckResult(false, "Cannot verify Wi-Fi network"));
                }
                return;
            }

            if (currentSsid.startsWith("\"") && currentSsid.endsWith("\"")) {
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

    private boolean isWifiConnectionValid(WifiInfo wifiInfo) {
        if (wifiInfo == null) return false;

        boolean isConnected = wifiInfo.getSupplicantState() == SupplicantState.COMPLETED;
        boolean hasGoodSignal = wifiInfo.getRssi() > -70;
        boolean hasValidBssid = wifiInfo.getBSSID() != null &&
                !wifiInfo.getBSSID().equals("02:00:00:00:00:00");

        if (!hasValidBssid && AppConstants.ANDROID_15_TESTING_MODE) {
            return isConnected && hasGoodSignal;
        }

        return isConnected && hasValidBssid;
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

    public void performAttendance(String studentId, String slot, Uri imageUri) {
        _isLoading.setValue(true);
        String userId = attendanceRepository.getCurrentUserId();
        if (userId == null) {
            _attendanceResult.setValue(new Result.Error<>(new Exception("User not logged in.")));
            _isLoading.setValue(false);
            return;
        }

        // 0. Check if current time is within slot time
        if (!isCurrentTimeInSlot(slot)) {
            SlotTime slotTime = AppConstants.SLOT_TIMES.get(slot);
            String timeRange = slotTime != null ?
                    String.format(Locale.getDefault(), "(%02d:%02d - %02d:%02d)",
                            slotTime.startHour, slotTime.startMinute,
                            slotTime.endHour, slotTime.endMinute) : "";
            _attendanceResult.setValue(new Result.Error<>(
                    new Exception("Bạn chỉ có thể điểm danh trong khung giờ của slot học " + timeRange)));
            _isLoading.setValue(false);
            return;
        }

        // 1. Check if already attended
        attendanceRepository.hasAttendedToday(userId, slot).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Calendar startOfDay = Calendar.getInstance();
                startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                startOfDay.set(Calendar.MINUTE, 0);
                startOfDay.set(Calendar.SECOND, 0);

                Calendar endOfDay = Calendar.getInstance();
                endOfDay.set(Calendar.HOUR_OF_DAY, 23);
                endOfDay.set(Calendar.MINUTE, 59);
                endOfDay.set(Calendar.SECOND, 59);

                boolean hasAttendedToday = task.getResult().getDocuments().stream()
                        .anyMatch(doc -> {
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                            if (timestamp == null) return false;

                            long docTime = timestamp.toDate().getTime();
                            return docTime >= startOfDay.getTimeInMillis() &&
                                    docTime <= endOfDay.getTimeInMillis();
                        });

                if (hasAttendedToday) {
                    _attendanceResult.setValue(new Result.Error<>(new Exception("Bạn đã điểm danh ngày hôm nay.")));
                    _isLoading.setValue(false);
                } else {
                    // 2. Not attended, proceed to upload image
                    uploadImageAndSaveRecord(userId, studentId, slot, imageUri);
                }
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
                // Update slot status after successful attendance
                if (currentSlot != null && currentSlot.equals(slot)) {
                    _slotAttendanceStatus.setValue(new SlotAttendanceStatus(true, slot, "Điểm danh thành công"));
                }
            } else {
                _attendanceResult.setValue(new Result.Error<>(saveTask.getException()));
            }
            _isLoading.setValue(false);
        });
    }

    public static class WifiCheckResult {
        public final boolean isValid;
        public final String ssid;
        public WifiCheckResult(boolean isValid, String ssid) {
            this.isValid = isValid;
            this.ssid = ssid;
        }
    }

    public static class SlotAttendanceStatus {
        public final boolean hasAttended;
        public final String slot;
        public final String message;

        public SlotAttendanceStatus(boolean hasAttended, String slot, String message) {
            this.hasAttended = hasAttended;
            this.slot = slot;
            this.message = message;
        }
    }
}