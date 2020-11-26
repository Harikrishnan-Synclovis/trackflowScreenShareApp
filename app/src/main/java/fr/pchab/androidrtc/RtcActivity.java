
package fr.pchab.androidrtc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;

import static android.content.ContentValues.TAG;

public class RtcActivity extends Activity implements WebRtcClient.RtcListener {
    private WebRtcClient mWebRtcClient;
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
    //    private EglBase rootEglBase;
    private static Intent mMediaProjectionPermissionResultData;
    private static int mMediaProjectionPermissionResultCode;

    public static String STREAM_NAME_PREFIX = "android_device_stream";
    // List of mandatory application permissions.／
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    //    private SurfaceViewRenderer pipRenderer;
//    private SurfaceViewRenderer fullscreenRenderer;
    public static int sDeviceWidth;
    public static int sDeviceHeight;
    public static final int SCREEN_RESOLUTION_SCALE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.checkPermission();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_rtc);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        sDeviceWidth = metrics.widthPixels;
        sDeviceHeight = metrics.heightPixels;

//        pipRenderer = (SurfaceViewRenderer) findViewById(R.id.pip_video_view);
//        fullscreenRenderer = (SurfaceViewRenderer) findViewById(R.id.fullscreen_video_view);

//        EglBase rootEglBase = EglBase.create();
//        pipRenderer.init(rootEglBase.getEglBaseContext(), null);
//        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//        fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
//        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

//        pipRenderer.setZOrderMediaOverlay(true);
//        pipRenderer.setEnableHardwareScaler(true /* enabled */);
//        fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);
        // Check for mandatory permissions.

    }

    public void checkPermission(){
        ActivityCompat.requestPermissions(RtcActivity.this,
                new String[]{Manifest.permission.CAPTURE_VIDEO_OUTPUT, Manifest.permission.CAPTURE_AUDIO_OUTPUT,Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE ,Manifest.permission.CAPTURE_SECURE_VIDEO_OUTPUT, Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.MODIFY_AUDIO_SETTINGS},
                1);
    }

    public void initiate(){
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startScreenCapture();
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case  1:{
                Log.e("permission","1"+grantResults.length);
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("permission","initiate");
                    initiate();
                //}
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @TargetApi(21)
    private VideoCapturer createScreenCapturer() {
        if (mMediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            report("User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(
                mMediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                report("User revoked permission to capture the screen.");
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mMediaProjectionPermissionResultCode = resultCode;
        mMediaProjectionPermissionResultData = data;
        init();
    }

    private void init() {
        PeerConnectionClient.PeerConnectionParameters peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(true, false,
                        true, sDeviceWidth / SCREEN_RESOLUTION_SCALE, sDeviceHeight / SCREEN_RESOLUTION_SCALE, 0,
                        0, "VP8",
                        false,
                        true,
                        0,
                        "OPUS", false, false, false, false, false, false, false, false, null);
//        mWebRtcClient = new WebRtcClient(getApplicationContext(), this, pipRenderer, fullscreenRenderer, createScreenCapturer(), peerConnectionParameters);
        mWebRtcClient = new WebRtcClient(getApplicationContext(), this, createScreenCapturer(), peerConnectionParameters);
    }

    public void report(String info) {
        Log.e(TAG, info);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (mWebRtcClient != null) {
//            mWebRtcClient.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onReady(String callId) {
        mWebRtcClient.start(STREAM_NAME_PREFIX);
    }

    @Override
    public void onCall(final String applicant) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Override
    public void onHandup() {

    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
