package android.e.blephotoidentifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.tensorflow.lite.examples.posenet.lib.Posenet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Capture extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int SEPARATOR_CHAR = 42;
    private static final ParcelUuid SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final String SERVER_IP = "192.168.1.218";
    private static final String SERVER_PORT = "5000";

    private BluetoothManager manager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private ArrayList<String> names = new ArrayList<String>();
    private HashMap<String,String> currentNames = new HashMap<String,String>();
    private HashMap<String,String> currentShirts = new HashMap<String,String>();
    private HashMap<String,String> currentPants = new HashMap<String,String>();
    private HashMap<String,String> currentMoves = new HashMap<String,String>();
    String currentPhotoPath;
    private boolean mUserRequestedInstall = true;
    private Session mSession;

    private TextView people_list;
    private Button collect;
    private Button take_photo;
    private Button facial_recognition;
    private Button tensorflow;
    private Button ar;
    private Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        initializeBt();
        createUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }
        /*try {
            mSession = new Session(this, EnumSet.of(Session.Feature.FRONT_CAMERA));
            Config config = new Config(mSession);
            config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
            mSession.configure(config);
            mSession.setCameraTextureName(500);
        } catch (Exception e) {
            Log.e("ERROR", "Couldn't configure AR session");
            return;  // mSession remains null, since session creation has failed.
        }*/

        // Ensure that Google Play Services for AR and ARCore device profile data are
        // installed and up to date.
        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success: Safe to create the AR session.
                        mSession = new Session(this);

                        // Create a session config.
                        Config config = new Config(mSession);

                        // Do feature-specific operations here, such as enabling depth or turning on
                        // support for Augmented Faces.

                        // Create a camera config filter for the session.
                        CameraConfigFilter filter = new CameraConfigFilter(mSession);
                        // Return only camera configs that target 30 fps camera capture frame rate.
                        filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
                        List<CameraConfig> cameraConfigList = mSession.getSupportedCameraConfigs(filter);

                        // Configure the session.
                        mSession.setCameraConfig(cameraConfigList.get(0));
                        mSession.configure(config);
                        break;
                    case INSTALL_REQUESTED:
                        // When this method returns `INSTALL_REQUESTED`:
                        // 1. ARCore pauses this activity.
                        // 2. ARCore prompts the user to install or update Google Play
                        //    Services for AR (market://details?id=com.google.ar.core).
                        // 3. ARCore downloads the latest device profile data.
                        // 4. ARCore resumes this activity. The next invocation of
                        //    requestInstall() will either return `INSTALLED` or throw an
                        //    exception if the installation or update did not succeed.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            Log.e("NO", "AR CORE");
            return;
        } catch (Exception e) {
            Log.e("NO", "AR CORE");
            return;  // mSession remains null, since session creation has failed.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    private void initializeBt(){
        manager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();
        if (btAdapter == null) {
            showFinishingAlertDialog("Bluetooth Error", "Bluetooth not detected on device");
        } else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else if (!btAdapter.isMultipleAdvertisementSupported()) {
            showFinishingAlertDialog("Not supported", "BLE advertising not supported on this device");
        }
        bluetoothLeScanner = btAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setLegacy(false)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .build();
        filters = new ArrayList<ScanFilter>();
        byte[] test = new byte[24];
        byte[] mask = new byte [24];
        for (int i = 0; i < 24; i++){
            test[i] = (byte)1;
            mask[i] = (byte)0;
        }
        //filters.add(new ScanFilter.Builder().setServiceData(SERVICE_UUID,test,mask).build());
        filters.add(new ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build());
    }

    private void createUI(){
        people_list = findViewById(R.id.people_list);
        collect = findViewById(R.id.collect);
        back = findViewById(R.id.back);
        take_photo = findViewById((R.id.take_photo));
        facial_recognition = findViewById(R.id.facial_recognition);
        tensorflow = findViewById(R.id.tensorflow);
        ar = findViewById(R.id.ar);
        collect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if (collect.getText().toString().equalsIgnoreCase("Collect Broadcast")){
                    collect.setText("STOP");
                    setEnabledViews(false,back,take_photo,facial_recognition);
                    scanLeDevice(true);
                } else {
                    scanLeDevice(false);
                    Assembler.clear();
                    setEnabledViews(true,back,take_photo,facial_recognition);
                    collect.setText("COLLECT BROADCAST");
                }
            }
        });
        take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                dispatchTakePictureIntent();
            }
        });
        facial_recognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                sendNames(v);
                connectServer(v);
            }
        });
        tensorflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                dispatchTensorFlowIntent();
            }
        });
        ar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent sceneViewerIntent = new Intent(Intent.ACTION_VIEW);
                Uri intentUri =
                        Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                                .appendQueryParameter("file", "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/ToyCar/glTF/ToyCar.gltf")
                                .appendQueryParameter("mode", "ar_only")
                                .build();
                sceneViewerIntent.setData(intentUri);
                sceneViewerIntent.setPackage("com.google.ar.core");
                startActivity(sceneViewerIntent);
            }
        });
        maybeEnableArButton();
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            bluetoothLeScanner.startScan(filters,settings,leScanCallback);
        } else {
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            String address = result.getDevice().getName();
            byte[] pData = Assembler.gather(address, result.getScanRecord().getServiceData(SERVICE_UUID));
            if (pData != null) {
                update(pData);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("BLE Photo Identifier","File couldn't be created.");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "android.e.blephotoidentifier.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void dispatchTensorFlowIntent() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent,2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 2){
            String result = data.getStringExtra("result");
            String name = names.get(0);
            if (result.equalsIgnoreCase(currentShirts.get(name))){
                try{
                    wait(2000);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            Toast.makeText(getApplicationContext(),name,Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    void connectServer(View v){
        String postUrl= "http://"+SERVER_IP+":"+SERVER_PORT+"/";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // Read BitMap by file path
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, options);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "recognitionPic.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        postRequest(postUrl, postBodyImage);
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(60, TimeUnit.SECONDS);
        builder.readTimeout(60, TimeUnit.SECONDS);
        builder.writeTimeout(60, TimeUnit.SECONDS);
        client = builder.build();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();
                e.printStackTrace();

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Failed to Connect to Server",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Toast.makeText(getApplicationContext(),response.body().string(),Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    void sendNames(View v){

        String postUrl= "http://"+SERVER_IP+":"+SERVER_PORT+"/names";

        String postBodyText="";
        for (String name : names){
            postBodyText += name + "*";
        }
        MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
        RequestBody postBody = RequestBody.create(mediaType, postBodyText);

        postNames(postUrl, postBody);
    }

    void postNames(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Couldn't send names!",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        });
    }

    void maybeEnableArButton() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    maybeEnableArButton();
                }
            }, 200);
        }
        if (availability.isSupported()) {
            ar.setVisibility(View.VISIBLE);
            ar.setEnabled(true);
        } else { // The device is unsupported or unknown.
            ar.setVisibility(View.INVISIBLE);
            ar.setEnabled(false);
            Log.e("AR ERROR", "AR not supported");
        }
    }

    private void setEnabledViews(boolean enabled, View... views) {
        for (View v : views) {
            v.setEnabled(enabled);
        }
    }

    private void update(byte[] data){
        int index = 0;
        while (data[index] != (byte)SEPARATOR_CHAR){
            index++;
        }
        byte[] name = Arrays.copyOfRange(data,0,index);
        index++;
        int shirtStart = index;
        while (data[index] != (byte)SEPARATOR_CHAR){
            index++;
        }
        byte[] shirtData = Arrays.copyOfRange(data,shirtStart,index);
        index++;
        byte[] pantsData = Arrays.copyOfRange(data,index,data.length);
        String nameStr = new String(name);
        String shirtStr = new String(shirtData);
        String pantsStr = new String(pantsData);
        if (!names.contains(nameStr)){
            currentShirts.put(nameStr,shirtStr);
            currentPants.put(nameStr,pantsStr);
            names.add(nameStr);
        }
        updateView();
    }

    private void updateView(){
        String nameList = "";
        for (String name : names){
            nameList = nameList + "\n" + name + "\t" + currentMoves.get(name);
        }
        people_list.setText(nameList);
    }

    // Pops an AlertDialog that quits the app on OK.
    private void showFinishingAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }

                }).show();
    }
}