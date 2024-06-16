package com.example.agorasample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {
    private AgoraManager agoraManager;
    private SurfaceView localVideoView;
    private Button startStreamButton;
    private Button stopStreamButton;
    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localVideoView = findViewById(R.id.local_video_view);
        startStreamButton = findViewById(R.id.start_stream_button);
        stopStreamButton = findViewById(R.id.stop_stream_button);

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(REQUESTED_PERMISSIONS[2]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        } else {
            initAgoraEngineAndJoinChannel();
        }

        startStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rtmpUrl = "rtmp://a.rtmp.youtube.com/live2/1uba-2x0x-74g2-7w0r-17kp"; // Replace with your RTMP URL
                agoraManager.startRtmpStream(rtmpUrl);
            }
        });

        stopStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rtmpUrl = "rtmp://a.rtmp.youtube.com/live2/1uba-2x0x-74g2-7w0r-17kp"; // Replace with your RTMP URL
                agoraManager.stopRtmpStream(rtmpUrl);
            }
        });
    }

    private void initAgoraEngineAndJoinChannel() {
        agoraManager = new AgoraManager(this);
        String token = "007eJxTYJDN6zpz/ulh9T/7loUm+sc9F5/8+H+A1rKG3tknpWOYmOQUGFLMzBONki2TLA1NDUwSzYyTkpLMzSyN01JNUtOMjFKTy5zy0xoCGRk2hdxgYWSAQBCfhaEktbiEgQEAij8gHw=="; // Replace with your Agora Token
        String channelName = "test"; // Replace with your Channel Name

        setupLocalVideo();
        agoraManager.joinChannel(token, channelName);
    }

    private void setupLocalVideo() {
        RtcEngine rtcEngine = agoraManager.getRtcEngine();
        rtcEngine.setupLocalVideo(new VideoCanvas(localVideoView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        rtcEngine.startPreview();
        rtcEngine.enableAudio();
        rtcEngine.enableVideo();
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
        ));
        localVideoView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        agoraManager.leaveChannel();
        agoraManager.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, show error message
            } else {
                initAgoraEngineAndJoinChannel();
            }
        }
    }
}
