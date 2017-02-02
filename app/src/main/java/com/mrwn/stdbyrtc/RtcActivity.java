package com.mrwn.stdbyrtc;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mrwn.stdbyrtc.Adapter.ChatAdapter;
import com.mrwn.stdbyrtc.Model.ChatMessage;
import com.mrwn.webrtc.PeerConnectionParameters;
import com.mrwn.webrtc.WebRtcClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.LinkedList;
import java.util.List;

public class RtcActivity extends ListActivity implements WebRtcClient.RtcListener {
    private static final String VIDEO_CODEC_VP9 = "VP8";
    private static final String AUDIO_CODEC_OPUS = "opus";


    private static WebRtcClient client;
    private String mSocketAddress;
    private EditText mChatEditText;
    private String username;
    private ListView mChatList;
    private ImageButton mPushtoTalk;
    private ChatAdapter mChatAdapter;
    private String myId;
    private String number="";
    private String callerIdChat="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.main);
        this.mChatEditText = (EditText) findViewById(R.id.chat_input);
        this.mChatList = getListView();
        this.mChatEditText = (EditText) findViewById(R.id.chat_input);

        this.mPushtoTalk = (ImageButton) findViewById(R.id.mute);

        final AudioManager audioManager = (AudioManager)
                this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMicrophoneMute(true);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        mPushtoTalk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        audioManager.setMicrophoneMute(false);
                        showToast("Unmuted");
                        v.setBackground(getResources().getDrawable(R.drawable.microphone));

                        //Log.d("RA-oC", "Mode: " + audioManager.getMode());
                        //Log.d("RA-oC", "Speakerphone on: " + audioManager.isSpeakerphoneOn());
                        break;
                    case MotionEvent.ACTION_UP:
                        audioManager.setMicrophoneMute(true);
                        showToast("Muted");
                        v.setBackground(getResources().getDrawable(R.drawable.muted));
                        break;
                }
                return false;
            }
        });


        //Set list chat adapter for the list activity
        List<ChatMessage> ll = new LinkedList<ChatMessage>();
        mChatAdapter = new ChatAdapter(this, ll);
        mChatList.setAdapter(mChatAdapter);

        mSocketAddress = "http://" + getResources().getString(R.string.host);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            myId = extras.getString("id");
            number = extras.getString("number");
            callerIdChat = extras.getString("callerIdChat");
            username = extras.getString("name");
        }

        init();

    }

    //Initialize WebRTC client
    //Set up the peer connection parameters and then pass this information to WebRtcClient class.

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        //First boolean is to enable or disable Videocalling
        PeerConnectionParameters params = new PeerConnectionParameters(
                false, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);
        client = new WebRtcClient(this, mSocketAddress, params, this.myId);

    }


    //Handle chat messages when a user clicks the send button
    //Gets the message from the input and then adds it to chat adapter
    //Transmit the message to other users except the one who called this function
    //The parameter view contains the button

    public void sendMessage(View view) {
        String message = mChatEditText.getText().toString();
        if (message.equals("")) return; // Return if empty
        ChatMessage chatMsg = new ChatMessage(username, message, System.currentTimeMillis());
        mChatAdapter.addMessage(chatMsg);

        //Data is being sent under JSON object
        JSONObject messageJSON = new JSONObject();
        try {

            if (number != "" && number != null){
                //Log.d("RA-sM","Comes first "+number);
                messageJSON.put("to", number);
            }else{
                //Log.d("RA-sM","Comes second "+callerIdChat);
                messageJSON.put("to", callerIdChat);
            }

            //Log.d("RA-sM","Messages sent "+ );
            messageJSON.put("user_id", chatMsg.getSender());
            messageJSON.put("msg", chatMsg.getMessage());
            messageJSON.put("time", chatMsg.getTimeStamp());
            client.transmitChat(messageJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Hide keyboard when you send a message.
        View focusView = this.getCurrentFocus();
        if (focusView != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        mChatEditText.setText("");
    }

    //Handle when people click hangup button
    //Destroy all video resources and connection

    public void hangup(View view) {
        if (client != null) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            String mNumber = "";
            try {
                if (number != "" && number != null){
                    mNumber = number;
                }else{
                    mNumber= callerIdChat;
                }
                JSONObject messageJSON = new JSONObject();
                messageJSON.put("callerId", mNumber);
                client.removeCall(messageJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try{ Thread.sleep(1000); }catch(InterruptedException e){ }
            onDestroy();
        }
    }

    /**
     * Handle onPause event which is implement by RtcListener class
     * <p/>
     * Pause the video source
     */
    @Override
    public void onPause() {
        super.onPause();

        if(isAppIsInBackground(getBaseContext())){
            NotificationManager mManager;
            mManager = (NotificationManager) getApplicationContext()
                    .getSystemService(
                            getApplicationContext().NOTIFICATION_SERVICE);
            Intent in = new Intent(getApplicationContext(),
                    RtcActivity.class);
            Notification notification = new Notification(R.drawable.notification_template_icon_bg,
                    "Video  ", System.currentTimeMillis());
            RemoteViews notificationView = new RemoteViews(getPackageName(),
                    R.layout.notification_video_calling);
            in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Intent hangIntent = new Intent(this, hangButtonListener.class);
            PendingIntent pendingHangIntent = PendingIntent.getBroadcast(this, 0,
                    hangIntent, 0);
            notificationView.setOnClickPendingIntent(R.id.hang_up_noti,
                    pendingHangIntent);
            Intent stopIntent = new Intent(this, stopButtonListener.class);
            if (number != "" && number != null){
                stopIntent.putExtra("otheruser",number);
            }else{
                stopIntent.putExtra("otheruser",callerIdChat);
            }

            PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this, 0,
                    stopIntent, 0);
            notificationView.setOnClickPendingIntent(R.id.end_call_noti,pendingStopIntent
                    );
            PendingIntent pendingNotificationIntent = PendingIntent.getActivity(
                    getApplicationContext(), 0, in,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.contentView = notificationView;
            notification.contentIntent = pendingNotificationIntent;
            mManager.notify(0, notification);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    //Handle onResume event which is implement by RtcListener class
    //Resume the video source

    //This function is being call when user have got an id from nodejs server
    //Check if caller id is not null then answer the call
    //If not then start the camera and send id to other user
    //callId is the id of the user
    @Override
    public void onCallReady(String callId) {
       // this.username = client.client_id();
        if (number != null) {
            try {
                client.startClient(number, "init", null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            call(callId);
        }
    }


    //Handle onDestroy event which is implement by RtcListener class
    //Destroy the video source

    @Override
    public void onReject() {
    }

    @Override
    public void onAcceptCall(String callId) {
        try {
            try{ Thread.sleep(1500); }catch(InterruptedException e){ }
            answer(callId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //This function is being called when a user rejects a call

    @Override
    public void receiveMessage(final String id, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatMessage chatMsg = new ChatMessage(id, msg, System.currentTimeMillis());
                mChatAdapter.addMessage(chatMsg);
                if(isAppIsInBackground(getBaseContext())){
                    NotificationManager mManager;
                    mManager = (NotificationManager) getApplicationContext()
                            .getSystemService(
                                    getApplicationContext().NOTIFICATION_SERVICE);
                    Intent in = new Intent(getApplicationContext(),
                            RtcActivity.class);
                    Notification notification = new Notification(R.drawable.notification_template_icon_bg,
                            "New message from "+id, System.currentTimeMillis());
                    notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    PendingIntent pendingNotificationIntent = PendingIntent.getActivity(
                            getApplicationContext(), 0, in,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                    notification.setLatestEventInfo(getApplicationContext(),
                            "New message", msg,
                            pendingNotificationIntent);
                    mManager.notify(0, notification);
                }
            }
        });
    }

    private boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }
        return isInBackground;
    }


    //This function is used when the chat event is triggered
    //Add the chat message to the chat adapter

    //This function is being call to answer call from other user
    //send init message to the caller and connect
    //start the camera
    //@param callerId the id of the caller
    public void answer(String callerId) throws JSONException {
        client.sendMessage(callerId, "init", null);
        startCam();
    }

    //Check if the application is in the background or in the foreground
    //context is the id of the user that sent the chat

    //This function is to send message contain id to the other user in order to start a call
    //Start intent then start the message intent contain url and user id
    public void call(String callId) {
        startCam();
    }

    //Start audio function
    public void startCam() {
        client.startAudio("android_test");
    }

    //Being called when call status changes
    //Log message when webrtc status changes
    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Being called when local stream is added
    //Update render view for the local stream in the small window
    @Override
    public void onLocalStream(MediaStream localStream) {
    }

    //Being called when remote stream is added
    //Update render view for the remote stream in the big window
    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
    }

    //Being called when remote stream is removed
    //make local renderer become the big one again
    @Override
    public void onRemoveRemoteStream(int endPoint) {
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RtcActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class hangButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            client.stopVideo();
        }
    }

    public static class stopButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Intent mainview = new Intent(context, MainActivity.class);
            mainview.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainview);

            if (extras.containsKey("otheruser")) {
                try {
                    JSONObject messageJSON = new JSONObject();
                    messageJSON.put("callerId", extras.getString("otheruser"));
                    client.removeCall(messageJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("RA-sBL", "Call ends");
            }

        }
    }
}