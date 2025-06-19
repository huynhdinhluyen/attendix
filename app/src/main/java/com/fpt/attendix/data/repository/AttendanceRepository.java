package com.fpt.attendix.data.repository;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.fpt.attendix.data.model.AttendanceRecord;
import com.fpt.attendix.data.model.User;
import com.fpt.attendix.util.AppConstants;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.UUID;

public class AttendanceRepository {

    private static volatile AttendanceRepository instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final CollectionReference attendanceCollection;
    private final CollectionReference usersCollection;

    private AttendanceRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        attendanceCollection = firestore.collection(AppConstants.FIRESTORE_COLLECTION_ATTENDANCE);
        usersCollection = firestore.collection(AppConstants.FIRESTORE_COLLECTION_USERS);
    }

    public static AttendanceRepository getInstance() {
        if (instance == null) {
            instance = new AttendanceRepository();
        }
        return instance;
    }

    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<Void> saveUserDetails(User user) {
        return usersCollection.document(user.getUid()).set(user);
    }

    public DocumentReference getUserDetails(String uid) {
        return usersCollection.document(uid);
    }

    public Task<AuthResult> loginUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public String getCurrentUserId() {
        if (firebaseAuth.getCurrentUser() != null) {
            return firebaseAuth.getCurrentUser().getUid();
        }
        return null;
    }

    public WifiInfo getCurrentWifiInfo(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            return wifiManager.getConnectionInfo();
        }
        return null;
    }

    public Task<QuerySnapshot> hasAttendedToday(String userId, String slot) {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        Query query = attendanceCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("slot", slot)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay.getTime())
                .whereLessThanOrEqualTo("timestamp", endOfDay.getTime());

        return query.get();
    }

    public UploadTask uploadAttendanceImage(Uri imageUri) {
        String fileName = "attendance_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);
        return storageRef.putFile(imageUri);
    }

    public Task<Void> saveAttendanceRecord(AttendanceRecord record) {
        return attendanceCollection.document().set(record);
    }
}
