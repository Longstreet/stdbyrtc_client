package com.mrwn.stdbyrtc;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.mrwn.stdbyrtc.Adapter.RoomAdapter;
import com.mrwn.stdbyrtc.Adapter.UserAdapter;
import com.mrwn.stdbyrtc.Model.Room;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Marouane on 29-11-2016.
 */

public class RoomActivity extends ListActivity {

    private static UserAdapter mUserAdapter;
    public ArrayList<Room> arrayOfRooms;
    private SharedPreferences mSharedPreferences;
    private String userName;
    private String roomId;
    private String userId;
    private ListView mRoomList;
    private RoomAdapter mRoomAdapter;
    private Button mRoomCreate;
    private TextView mUsernameTV;
    private TextView logoutButton;
    private Handler handler = new Handler();
    private Socket client;
    private String mRoomName;
    /**
     * Receive call emitter callback when others call you.
     *
     * @param args json value contain callerid, userid and caller name
     */
    private Emitter.Listener onReceiveCall = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String from = "";
            String name = "";
            JSONObject data = (JSONObject) args[0];
            try {
                from = data.getString("from");
                name = data.getString("name");
                client.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (isAppIsInBackground(getApplicationContext())) {
//                NotificationManager mManager;
//                mManager = (NotificationManager) getApplicationContext()
//                        .getSystemService(
//                                getApplicationContext().NOTIFICATION_SERVICE);
//                Intent in = new Intent(getApplicationContext(),
//                        IncomingCallActivity.class);
//                in.putExtra("CALLER_ID", from);
//                in.putExtra("USER_ID", userId);
//                in.putExtra("CALLER_NAME", "Lien Minh");
//                in.putExtra("USER_NAME",userName);
//                Notification notification = new Notification(R.drawable.notification_template_icon_bg,
//                        "Demo video  ", System.currentTimeMillis());
//                //RemoteViews notificationView = new RemoteViews(getPackageName(),
//                //        R.layout.notification_incoming_call);
//                in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
//                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
////                Intent receiveIntent = new Intent(getApplicationContext(), receiveButtonListener.class);
////                PendingIntent pendingReceiveIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
////                        receiveIntent, 0);
////                notificationView.setOnClickPendingIntent(R.id.noti_receive,
////                        pendingReceiveIntent);
//////                Intent rejectIntent = new Intent(getApplicationContext(), rejectButtonListener.class);
//////                PendingIntent pendingRejectIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
//////                        rejectIntent, 0);
//////                notificationView.setOnClickPendingIntent(R.id.noti_reject,
//////                                                                   pendingRejectIntent);
//
//                PendingIntent pendingNotificationIntent = PendingIntent.getActivity(
//                        getApplicationContext(), 0, in,
//                        PendingIntent.FLAG_UPDATE_CURRENT);
//                notification.flags |= Notification.FLAG_AUTO_CANCEL;
//                notification.setLatestEventInfo(getApplicationContext(),
//                        "Incoming phone", "You have a new phone ",
//                        pendingNotificationIntent);
//                //notification.contentView = notificationView;
//                notification.contentIntent = pendingNotificationIntent;
//                mManager.notify(0, notification);
                Intent intent = new Intent(getApplicationContext(), IncomingCallActivity.class);
                //intent.setComponent(new ComponentName(getPackageName(), IncomingCallActivity.class.getName()));
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("CALLER_ID", from);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("CALLER_NAME", name);
                intent.putExtra("USER_NAME", userName);
                getApplicationContext().startActivity(intent);


                //context.getApplicationContext().startActivity(it);
            } else {
                Intent intent = new Intent(getApplicationContext(), IncomingCallActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("CALLER_ID", from);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("CALLER_NAME", name);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        //Get userid and username from login or register activity
        this.mSharedPreferences = getSharedPreferences("SHARED_PREFS", MODE_PRIVATE);
        if (!this.mSharedPreferences.contains("USER_NAME")) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        this.userId = this.mSharedPreferences.getString("USER_ID", "");
        this.userName = this.mSharedPreferences.getString("USER_NAME", "");
        this.mRoomList = getListView();
        this.mRoomCreate = (Button) findViewById(R.id.createRoomButton);
        this.mUsernameTV = (TextView) findViewById(R.id.main_username);
        this.mUsernameTV.setText(this.userName);
        this.logoutButton = (TextView) findViewById(R.id.logOutButton);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        mRoomCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRoomNameDialog();
            }
        });

        /*mCallNumET.setOnFocusChangeListener(new AutoCompleteTextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //Add all user for searching and add friends
                    ArrayList<User> adapter = new ArrayList<User>();
                    String json_users = "";
                    try {
                        try {
                            json_users = new MainActivity.RetrieveUserTask().execute().get();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                    try {
                        JSONArray jsonarr = new JSONArray(json_users);
                        for (int i = 0; i < jsonarr.length(); i++) {
                            JSONObject jsonobj = jsonarr.getJSONObject(i);

                            String id = jsonobj.getString("id");
                            String name = jsonobj.getString("name");
                            if (!id.equals(userId)) {
                                User x = new User(id, name);
                                adapter.add(x);
                            }
                        }
                    } catch (Exception e) {
                    }


                    mUserAdapter = new UserAdapter(v.getContext(), adapter);
                    mCallNumET.setThreshold(1);//will start working from first character
                    mCallNumET.setAdapter(mUserAdapter);//setting the adapter data into the AutoCompleteTextView

                } else {
                    Toast.makeText(getApplicationContext(), "lost the focus", Toast.LENGTH_LONG).show();
                }
            }
        });*/


        this.mRoomAdapter = new RoomAdapter(this, arrayOfRooms);
        this.mRoomList.setAdapter(this.mRoomAdapter);


        //start other thread for checking friend online status
        startHandler();

        //Receive call callback from when other people call you
        String host = "http://" + getResources().getString(R.string.host);
        host += (":" + getResources().getString(R.string.port) + "/");
        try {
            client = IO.socket(host);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        client.on("receiveCall", onReceiveCall);

        client.connect();
        try {
            JSONObject message = new JSONObject();
            message.put("myId", userId);
            client.emit("resetId", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setRoomNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Fill in your room name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRoomName = input.getText().toString();
                //add friends to friend list
                arrayOfRooms = new ArrayList<Room>();
                String json_room = "";
                try {
                    try {
                        json_room = new RoomActivity.ListRoomsTask().execute(mRoomName).get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    JSONArray jsonarr = new JSONArray(json_room);
                    for (int i = 0; i < jsonarr.length(); i++) {
                        JSONObject jsonobj = jsonarr.getJSONObject(i);
                        String id = jsonobj.getString("room_id");
                        String name = "";
                        String status = "";
                        try {
                            try {
                                status = new RoomActivity.RetrieveStatusTask().execute(id).get();
                                name = new RoomActivity.RetrieveName().execute(id).get();

                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        Room x = new Room(id, name, status);
                        arrayOfRooms.add(x);
                    }
                } catch (Exception e) {
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

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

    /**
     * Take the user to a video screen. USER_NAME is a required field.
     *
     * @param id button that is clicked to trigger toVideo
     */
    public void makeCall(String id, String status) {
        String callNum = id;

        if (callNum.isEmpty() || callNum.equals(this.roomId)) {
            showToast("Enter a valid roomID to call.");
            return;
        }
        if (status.equals("Offline")) {
            showToast("This room is offline. Please try again later!");
        } else {
            //remove callback check status every 10
            handler.removeCallbacksAndMessages(null);
            dispatchCall(callNum);
        }
    }

    /**
     * TODO: Debate who calls who. Should one be on standby? Or use State API for busy/available
     * Check that user is online. If they are, dispatch the call by publishing to their standby
     * channel. If the publish was successful, then change activities over to the call screen.
     * The called user will then have the option to accept of decline the call. If they accept,
     * they will be brought to the call screen as well. If
     * they decline, a hangup will be issued, and the chat adapter's onHangup callback will
     * be invoked.
     *
     * @param callNum Number to publish a call to.
     */
    public void dispatchCall(final String callNum) {
        Log.d("MA-dC", callNum);
        Intent intent = new Intent(RoomActivity.this, RtcActivity.class);
        //boolean activityExists = intent.resolveActivityInfo(getPackageManager(), 0) != null;
        //Log.d("RA-dC",Boolean.toString(activityExists) );
//        if (activityExists){
//            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            intent.putExtra("id", this.userId);
//            intent.putExtra("name", this.userName);
//            intent.putExtra("number", callNum);
//        }else{
//            Log.d("minhfinal", "come here re");
//            intent.putExtra("id", this.userId);
//            intent.putExtra("name", this.userName);
//            intent.putExtra("number", callNum);
//
//        }
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Log.d("MA-dC", "come here re");
        intent.putExtra("id", this.userId);
        intent.putExtra("name", this.userName);
        intent.putExtra("number", callNum);
        startActivity(intent);


//        Intent intent = new Intent(MainActivity.this, CallingActivity.class);
//        intent.putExtra("CALLER_ID", callNum);
//        intent.putExtra("USER_ID", userId);
//        intent.putExtra("CALLER_NAME", "Marouane Boutaib");
//        intent.putExtra("USER_NAME", userName);
//        startActivity(intent);

    }

    /**
     * Ensures that toast is run on the UI thread.
     *
     * @param message
     */
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RoomActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Check status of rooms, whether online or offline.
     */
    private void checkRoomStatus() {
        int checkChange = 0;
        for (Room room : arrayOfRooms) {
            String status;
            try {
                try {
                    status = new RoomActivity.RetrieveStatusTask().execute(room.getRoomId()).get();
                    Log.d("MA-cFS", "old handler status: " + room.getRoomStatus() + "new status: " + status);
                    if (status != null && !status.isEmpty() && !status.equals(room.getRoomStatus())) {
                        room.setRoomStatus(status);
                        checkChange = 1;
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        if (checkChange != 0) {
            mRoomAdapter.notifyDataSetChanged();
        }
    }

    public void startHandler() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                checkRoomStatus();
                handler.postDelayed(this, 3000);
            }
        }, 3000);
    }

    /**
     * Task to get status of a room.
     * <p>
     * String id is of room you want to get status
     */

    class RetrieveStatusTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String name = "";
            String id = urls[0];
            try {
                HttpClient httpclient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpGet request = new HttpGet(host + "status/" + id);
                HttpResponse response = httpclient.execute(request);
                String json_string = EntityUtils.toString(response.getEntity());
                JSONObject x = new JSONObject(json_string);
                int status = x.getInt("status");
                if (status == 1) {
                    name = "Online";
                } else {
                    name = "Offline";
                }

            } catch (Exception e) {
                //Log.e("MA-dIB", "Error in http connection " + e.toString());
            }
            return name;
        }

        protected void onPostExecute(String feed) {
        }
    }

    /**
     * Task to get name of rooms.
     */
    class RetrieveName extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String json_string = "";
            String user = urls[0];

            try {
                HttpClient httpClient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpPost httpPost = new HttpPost(host + "rooms/");


                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
                nameValuePair.add(new BasicNameValuePair("room_name", mRoomName));


                //Encoding POST data
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                } catch (UnsupportedEncodingException e) {
                    // log exception
                    e.printStackTrace();
                }
                //making POST request.
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    json_string = EntityUtils.toString(response.getEntity());
                    JSONObject x = new JSONObject(json_string);
                    json_string = x.getString("room_name");
                    //Log.d("MA-dIB", json_string);

                } catch (ClientProtocolException e) {
                    // Log exception

                    //Log.d("MA-dIB", "error");
                } catch (IOException e) {
                    // Log exception
                    e.printStackTrace();
                    //Log.d("MA-dIB", e.getMessage());

                }
            } catch (Exception e) {
            }
            return json_string;
        }

        protected void onPostExecute(String feed) {
        }
    }

    class ListRoomsTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String json_string = "";
            String user = urls[0];
            try {
                HttpClient httpClient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpPost httpPost = new HttpPost(host + "rooms/");


                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
                nameValuePair.add(new BasicNameValuePair("room_name", mRoomName));


                //Encoding POST data
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                } catch (UnsupportedEncodingException e) {
                    // log exception
                    e.printStackTrace();
                }
                //making POST request.
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    json_string = EntityUtils.toString(response.getEntity());


                } catch (ClientProtocolException e) {
                    // Log exception

                    //Log.d("MA-dIB, "error");
                } catch (IOException e) {
                    // Log exception
                    e.printStackTrace();
                    //Log.d("MA-dIB", e.getMessage());

                }
            } catch (Exception e) {
            }
            return json_string;
        }

        protected void onPostExecute(String feed) {
        }
    }

    /**
     * Task to get list of rooms.
     */

    class RetrieveRoomsTask extends AsyncTask<Void, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(Void... urls) {
            String room = "";
            try {
                HttpClient httpclient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpGet request = new HttpGet(host + "rooms/" + mRoomName);
                HttpResponse response = httpclient.execute(request);
                room = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                //Log.e("log_tag", "Error in http connection " + e.toString());
            }
            return room;
        }

        protected void onPostExecute(String feed) {
        }
    }

}
