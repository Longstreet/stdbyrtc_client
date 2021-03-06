package com.mrwn.stdbyrtc;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.mrwn.stdbyrtc.Adapter.HistoryAdapter;
import com.mrwn.stdbyrtc.Adapter.UserAdapter;
import com.mrwn.stdbyrtc.Model.HistoryItem;
import com.mrwn.stdbyrtc.Model.User;

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

public class MainActivity extends ListActivity {
    private static UserAdapter mUserAdapter;
    public ArrayList<HistoryItem> arrayOfUsers;
    private SharedPreferences mSharedPreferences;
    private String userName;
    private String userId;
    private ListView mHistoryList;
    private HistoryAdapter mHistoryAdapter;
    private AutoCompleteTextView mCallNumET;
    private TextView mUsernameTV;
    private Handler handler = new Handler();
    private Socket client;
    /**
     * Receive call emitter callback when others call you.
     *
     * @param args json values contain callerid, userid and caller name
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
                Intent intent = new Intent(getApplicationContext(), IncomingCallActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("CALLER_ID", from);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("CALLER_NAME", name);
                intent.putExtra("USER_NAME", userName);
                getApplicationContext().startActivity(intent);
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
        setContentView(R.layout.activity_main);

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
        this.mHistoryList = getListView();
        this.mCallNumET = (AutoCompleteTextView) findViewById(R.id.call_num);
        this.mUsernameTV = (TextView) findViewById(R.id.main_username);
        this.mUsernameTV.setText(this.userName);
        TextView logoutButton = (TextView) findViewById(R.id.logOutButton);

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

        mCallNumET.setOnFocusChangeListener(new AutoCompleteTextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //Add all user for searching and add friends
                    ArrayList<User> adapter = new ArrayList<User>();
                    String json_users = "";
                    try {
                        try {
                            json_users = new RetrieveUserTask().execute().get();
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
        });



        //add friends to friend list
        arrayOfUsers = new ArrayList<HistoryItem>();
        String json_friend = "";
        try {
            try {
                json_friend = new ListFriendsTask().execute(userId).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            JSONArray jsonarr = new JSONArray(json_friend);
            for (int i = 0; i < jsonarr.length(); i++) {
                JSONObject jsonobj = jsonarr.getJSONObject(i);
                String id = jsonobj.getString("friend_id");
                String name = "";
                String status = "";
                try {
                    try {
                        status = new RetrieveStatusTask().execute(id).get();
                        name = new RetrieveName().execute(id).get();

                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                HistoryItem x = new HistoryItem(id, name, status);
                arrayOfUsers.add(x);
            }
        } catch (Exception e) {
        }

        this.mHistoryAdapter = new HistoryAdapter(this, arrayOfUsers);
        this.mHistoryList.setAdapter(this.mHistoryAdapter);

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

    /**
     * Check application is in the backgroud or in foreground
     *
     * @param context  the id of the user sent the chat
     */
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

        if (callNum.isEmpty() || callNum.equals(this.userId)) {
            showToast("Enter a valid user ID to call.");
            return;
        }
        if (status.equals("Offline")) {
            showToast("Your friend is offline. Please call again later!");
        } else {
            //remove callback check status every 10
            handler.removeCallbacksAndMessages(null);
            dispatchCall(callNum);
        }
    }

    /**
     * TODO: Debate who calls who. Should one be on standby? Or use State API for busy/available
     * Check that user is online. If they are, dispatch the call by publishing to their standby
     * channel. If the publish was successful, then change activities over to the video chat.
     * The called user will then have the option to accept of decline the call. If they accept,
     * they will be brought to the video chat activity as well, to connect video/audio. If
     * they decline, a hangup will be issued, and the VideoChat adapter's onHangup callback will
     * be invoked.
     *
     * @param callNum Number to publish a call to.
     */
    public void dispatchCall(final String callNum) {
        Log.d("MA-dC", callNum);
        Intent intent = new Intent(MainActivity.this, RtcActivity.class);

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
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Add friend to your directory when you click add friend.
     *
     * @param id,name id and name of your friend
     */
    public void addfriend(String id, String name) {
        this.mCallNumET.dismissDropDown();
        int result = 0;
        try {
            result = new AddFriendTask().execute(id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if (result != 0) {
            HistoryItem x = new HistoryItem(id, name, "online");
            arrayOfUsers.add(x);
            mHistoryAdapter.notifyDataSetChanged();
        } else {
            showToast("error occured");
        }
    }

    /**
     * Check status of your friend whether online or offline.
     *
     * @param none
     */
    private void checkFriendStatus() {
        int checkChange = 0;
        for (HistoryItem user : arrayOfUsers) {
            String status;
            try {
                try {
                    status = new RetrieveStatusTask().execute(user.getUserId()).get();
                    Log.d("MA-cFS", "old handler status: " + user.getStatus() + "new status: " + status);
                    if (status != null && !status.isEmpty() && !status.equals(user.getStatus())) {
                        user.setStatus(status);
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
            mHistoryAdapter.notifyDataSetChanged();
        }
    }

    public void startHandler() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                checkFriendStatus();
                handler.postDelayed(this, 3000);
            }
        }, 3000);
    }

    /**
     * Task to get list of users.
     *
     * @param Void
     */
    class RetrieveUserTask extends AsyncTask<Void, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(Void... urls) {
            String name = "";
            try {
                HttpClient httpclient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpGet request = new HttpGet(host + "users/" + userId);
                HttpResponse response = httpclient.execute(request);
                name = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                //Log.e("log_tag", "Error in http connection " + e.toString());
            }
            return name;
        }

        protected void onPostExecute(String feed) {
        }
    }

    /**
     * Task to get status of a user.
     *
     * @param String id of user you want to get status
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
     * Task to add friend to your account.
     *
     * @param YOUR ID AND USER ID THAT YOU WANT TO ADD AS FRIEND
     */
    class AddFriendTask extends AsyncTask<String, Void, Integer> {

        private Exception exception;

        @Override
        protected Integer doInBackground(String... urls) {
            String id = urls[0];
            try {
                HttpClient httpClient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpPost httpPost = new HttpPost(host + "addFriend");

                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
                nameValuePair.add(new BasicNameValuePair("username", userId));
                nameValuePair.add(new BasicNameValuePair("friend_id", id));


                //Encoding POST data
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                } catch (UnsupportedEncodingException e) {
                    // log exception
                    //Log.d("MA-dIB", e.getMessage());
                    e.printStackTrace();
                }
                //making POST request.
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    String json_string = EntityUtils.toString(response.getEntity());
                    JSONObject json_data = new JSONObject(json_string);
                    int status = json_data.getInt("status");
                    //Log.d("MA-dIB", Integer.toString(status));
                    if (status == 1) {
                        return 1;
                    }

                } catch (ClientProtocolException e) {
                } catch (IOException e) {
                    // Log exception
                    e.printStackTrace();
                    //Log.d("MA-dIB", e.getMessage());

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        protected void onPostExecute(String feed) {
        }
    }

    /**
     * Task to list all of your friends.
     *
     * @param String id to get list of friends
     */
    class ListFriendsTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String json_string = "";
            String user = urls[0];
            try {
                HttpClient httpClient = new DefaultHttpClient();
                String host = "http://" + getResources().getString(R.string.host);
                host += (":" + getResources().getString(R.string.port) + "/");
                HttpPost httpPost = new HttpPost(host + "friends");


                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
                nameValuePair.add(new BasicNameValuePair("username", user));


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
     * Task to get name of a users.
     *
     * @param String id of the user you want to get name
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
                HttpPost httpPost = new HttpPost(host + "friend_name");


                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(1);
                nameValuePair.add(new BasicNameValuePair("username", user));


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
                    json_string = x.getString("name");
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
}