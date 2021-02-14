package android.e.blephotoidentifier;

import java.util.ArrayList;
import java.lang.reflect.Type;
import android.content.Context;
import com.google.gson.Gson;
import android.content.Intent;
import android.app.IntentService;
import android.preference.PreferenceManager;
import android.content.res.Resources;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
//Extend IntentService//
public class ActivityIntentService extends IntentService {
    protected static final String TAG = "Activity";
    //Call the super IntentService constructor with the name for the worker thread//
    public ActivityIntentService() {
        super(TAG);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
//Define an onHandleIntent() method, which will be called whenever an activity detection update is available//

    @Override
    protected void onHandleIntent(Intent intent) {
//Check whether the Intent contains activity recognition data//
        if (ActivityRecognitionResult.hasResult(intent)) {
            Log.e("and","here");
//If data is available, then extract the ActivityRecognitionResult from the Intent//
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            String activityType = getActivityString(mostProbableActivity.getType());
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("Detected Activity",activityType).apply();

        }
    }
//Convert the code for the detected activity type, into the corresponding string//

    static String getActivityString(int detectedActivityType) {
        switch(detectedActivityType) {
            case DetectedActivity.ON_BICYCLE:
                return "ON BICYCLE";
            case DetectedActivity.ON_FOOT:
                return "ON FOOT";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.IN_VEHICLE:
                return "IN VEHICLE";
            default:
                return "UNKNOWN";
        }
    }
    static final int[] POSSIBLE_ACTIVITIES = {

            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.TILTING,
            DetectedActivity.UNKNOWN
    };

}
