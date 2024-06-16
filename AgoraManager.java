package com.example.agorasample;

import static android.provider.Settings.System.getString;

import static java.security.AccessController.getContext;
import static io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
import android.os.Handler;
import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DirectCdnStreamingMediaOptions;
import io.agora.rtc2.DirectCdnStreamingStats;
import io.agora.rtc2.IDirectCdnStreamingEventHandler;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.live.LiveTranscoding;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.rtc2.DirectCdnStreamingState;
import io.agora.rtc2.DirectCdnStreamingReason;

public class AgoraManager {
    protected Handler handler;
    private RtcEngine rtcEngine;
    private String appId = "d67a2c9b91504a63bbb7693fe4ef22ec"; // Replace with your Agora App ID
    private static final int MAX_RETRIES = 3;
    private volatile boolean cdnStreaming = false;
    private volatile boolean rtcStreaming = false;
    private Switch rtcSwitcher;


    public AgoraManager(Context context) {
        try {
            rtcEngine = RtcEngine.create(context, appId, new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.d("Agora", "Join channel success");
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    Log.d("Agora", "User joined: " + uid);
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    Log.d("Agora", "User offline: " + uid);
                }

                @Override
                public void onRtmpStreamingStateChanged(String url, int state, int errCode) {
                    handleRtmpStreamingStateChange(url, state, errCode);
                }
            });
        } catch (Exception e) {
            Log.e("Agora", "Error initializing Agora: " + e.getMessage());
        }
    }

    public RtcEngine getRtcEngine() {
        return rtcEngine;
    }

    public void joinChannel(String token, String channelName) {
        rtcEngine.joinChannel(token, channelName, "", 0);
        setClientRole(CLIENT_ROLE_BROADCASTER);
    }

    public void leaveChannel() {
        rtcEngine.leaveChannel();
    }

    public void startRtmpStream(final String url) {
//        rtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, 0));
//        rtcEngine.startPreview();
        rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
//        rtcEngine.enableVideo();
        final LiveTranscoding transcoding = new LiveTranscoding();
        transcoding.height = 720;
        transcoding.width = 1280;
        VideoEncoderConfiguration videoEncoderConfiguration = new VideoEncoderConfiguration();
//        rtcEngine.setVideoEncoderConfiguration( new VideoEncoderConfiguration());
//        rtcEngine.setDirectCdnStreamingVideoConfiguration( new VideoEncoderConfiguration());
//        DirectCdnStreamingMediaOptions directCdnStreamingMediaOptions = new DirectCdnStreamingMediaOptions();
//        directCdnStreamingMediaOptions.publishCameraTrack = true;
//        directCdnStreamingMediaOptions.publishMicrophoneTrack = true;
//        rtcEngine.startDirectCdnStreaming(IDirectCdnStreamingEventHandler, url, directCdnStreamingMediaOptions);
        rtcEngine.setVideoEncoderConfiguration(videoEncoderConfiguration);
        rtcEngine.setDirectCdnStreamingVideoConfiguration(videoEncoderConfiguration);
        int ret = startCdnStreaming(url);
        if (ret == 0) {
            Log.d("Agora", String.format("streaming success," + ret));
        } else {
            Log.d("Agora", String.format("streaming failure," + ret));
        }
        attemptRtmpPush(url, transcoding, 0);
    }

    private int startCdnStreaming(final String url) {
        DirectCdnStreamingMediaOptions directCdnStreamingMediaOptions = new DirectCdnStreamingMediaOptions();
        directCdnStreamingMediaOptions.publishCameraTrack = true;
        directCdnStreamingMediaOptions.publishMicrophoneTrack = true;
        return rtcEngine.startDirectCdnStreaming(iDirectCdnStreamingEventHandler, url, directCdnStreamingMediaOptions);
    }

    private void attemptRtmpPush(final String url, final LiveTranscoding transcoding, final int attempt) {

        int result = rtcEngine.startRtmpStreamWithoutTranscoding(url);
        if (result != 0 && attempt < MAX_RETRIES) {
            Log.d("Agora", "RTMP push failed, retrying... Attempt: " + (attempt + 1));
            attemptRtmpPush(url, transcoding, attempt + 1);
        } else if (result == 0) {
            Log.d("Agora", "RTMP push initiated on attempt: " + (attempt + 1));
        } else {
            Log.d("Agora", "RTMP push failed after " + MAX_RETRIES + " attempts");
        }
    }

    private void handleRtmpStreamingStateChange(String url, int state, int errCode) {
        if (state == Constants.RTMP_STREAM_PUBLISH_STATE_FAILURE) {
            Log.d("Agora", "RTMP streaming state changed to failure, errCode: " + errCode);
        } else if (state == Constants.RTMP_STREAM_PUBLISH_STATE_RUNNING) {
            Log.d("Agora", "RTMP streaming state changed to running");
        } else {
            Log.d("Agora", "RTMP streaming state changed, state: " + state);
        }
    }

    public void setClientRole(int role) {
        rtcEngine.setClientRole(role);
    }

    public void stopRtmpStream(String url) {
        rtcEngine.stopRtmpStream(url);
    }

    public void destroy() {
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    private final IDirectCdnStreamingEventHandler iDirectCdnStreamingEventHandler = new IDirectCdnStreamingEventHandler() {


        @Override
        public void onDirectCdnStreamingStateChanged(DirectCdnStreamingState state, DirectCdnStreamingReason reason, String message) {
            Log.d("Agora", String.format("onDirectCdnStreamingStateChanged state:%s, error:%s", state, reason));

            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    switch (state) {
                        case RUNNING:
                            Log.d("Agora", String.format("onDirectCdnStreamingStateChanged, state: %s error: %s message: %s", state.name(), reason.name(), message));
                            break;
                        case STOPPED:
                            if (rtcStreaming) {
                                ChannelMediaOptions channelMediaOptions = new ChannelMediaOptions();
                                channelMediaOptions.publishMicrophoneTrack = true;
                                channelMediaOptions.publishCameraTrack = true;
                                channelMediaOptions.clientRoleType = CLIENT_ROLE_BROADCASTER;
                                ;
                                String channelName = "test";
                                int localUid = 123123;
                                int ret = rtcEngine.joinChannel(null, channelName, localUid, channelMediaOptions);
                                if (ret != 0) {
                                    Log.d("Agora", String.format("Join Channel call failed! reason:%d", ret));
                                }
                            } else {
                                cdnStreaming = false;
                            }
                            break;
                        case FAILED:
                            Log.d("Agora", String.format("Start Streaming failed, please go back to previous page and check the settings."));
                        default:
                            Log.d("Agora", String.format("onDirectCdnStreamingStateChanged, state: %s error: %s message: %s", state.name(), reason.name(), message));
                    }
                    rtcSwitcher.setEnabled(true);
                }
            });
        }
        @Override
        public void onDirectCdnStreamingStats(DirectCdnStreamingStats directCdnStreamingStats) {

        }
    };
    /**
     * Run on ui thread.
     *
     * @param runnable the runnable
     */
    protected final void runOnUIThread(Runnable runnable) {
        this.runOnUIThread(runnable, 0);
    }

    /**
     * Run on ui thread.
     *
     * @param runnable the runnable
     * @param delay    the delay
     */
    protected final void runOnUIThread(Runnable runnable, long delay) {
        if (handler != null && runnable != null && getContext() != null) {
            if (delay <= 0 && handler.getLooper().getThread() == Thread.currentThread()) {
                runnable.run();
            } else {
                handler.postDelayed(() -> {
                    if (getContext() != null) {
                        runnable.run();
                    }
                }, delay);
            }
        }
    }

    };
