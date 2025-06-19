package com.fpt.attendix.util;

import java.util.Arrays;
import java.util.List;

public class AppConstants {

    public static final List<String> ALLOWED_WIFI_SSIDS = Arrays.asList(
            "FPTU_Students_NVH",
            "@huynhdinhluyen",
            "Wi-MESH 2.4G"
    );

    public static final List<String> ALLOWED_SLOTS = Arrays.asList(
            "Slot 1 (7:00 - 9:15)",
            "Slot 2 (9:30 - 11:45)",
            "Slot 3 (12:30 - 14:45)",
            "Slot 4 (15:00 - 17:15)"
    );

    public static final String FIRESTORE_COLLECTION_ATTENDANCE = "attendance_records";
    public static final String FIRESTORE_COLLECTION_USERS = "users";
}
