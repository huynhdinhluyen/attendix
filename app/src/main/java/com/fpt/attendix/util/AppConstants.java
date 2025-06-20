package com.fpt.attendix.util;

import com.fpt.attendix.data.model.SlotTime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppConstants {

    public static final List<String> ALLOWED_WIFI_SSIDS = Arrays.asList(
            "FPTU_Students_NVH",
            "@huynhdinhluyen",
            "Wi-MESH 2.4G",
            "AMERICANO COFFEE"
    );

    public static final List<String> ALLOWED_SLOTS = Arrays.asList(
            "Slot 1 (7:00 - 9:15)",
            "Slot 2 (9:30 - 11:45)",
            "Slot 3 (12:30 - 14:45)",
            "Slot 4 (15:00 - 17:15)"
    );

    public static final Map<String, SlotTime> SLOT_TIMES = new HashMap<>();
    static {
        SLOT_TIMES.put("Slot 1 (7:00 - 9:15)", new SlotTime(7, 0, 9, 15));
        SLOT_TIMES.put("Slot 2 (9:30 - 11:45)", new SlotTime(9, 30, 11, 45));
        SLOT_TIMES.put("Slot 3 (12:30 - 14:45)", new SlotTime(12, 30, 14, 45));
        SLOT_TIMES.put("Slot 4 (15:00 - 17:15)", new SlotTime(15, 0, 17, 15));
    }

    public static final String FIRESTORE_COLLECTION_ATTENDANCE = "attendance_records";
    public static final String FIRESTORE_COLLECTION_USERS = "users";

    public static final boolean ENABLE_SMART_WIFI_VALIDATION = true;

    public static final boolean ANDROID_15_TESTING_MODE = true;

    public static final List<String> ALLOWED_BSSIDS = Arrays.asList(

    );
}
