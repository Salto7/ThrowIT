package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Messages;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * An activity that allows a user to publish device information, and receive information about
 * nearby devices.
 * <p/>
 * The UI exposes a button to subscribe to broadcasts from nearby devices, and another button to
 * publish messages that can be read nearby subscribing devices. Both buttons toggle state,
 * allowing the user to cancel a subscription or stop publishing.
 * <p/>
 * This activity demonstrates the use of the
 * {@link Messages#subscribe(GoogleApiClient, MessageListener, SubscribeOptions)},
 * {@link Messages#unsubscribe(GoogleApiClient, MessageListener)},
 * {@link Messages#publish(GoogleApiClient, Message, PublishOptions)}, and
 * {@link Messages#unpublish(GoogleApiClient, Message)} for foreground publication and subscription.
 * <p/>a
 * We check the app's permissions and present an opt-in dialog to the user, who can then grant the
 * required location permission.
 * <p/>
 * Using Nearby for in the foreground is battery intensive, and pub-sub is best done for short
 * durations. In this sample, we set the TTL for publishing and subscribing to three minutes
 * using a {@link Strategy}. When the TTL is reached, a publication or subscription expires.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        WifiP2pManager.ChannelListener,
        DeviceActionListener{

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int TTL_IN_SECONDS = 3*60; // Three minutes.3*60

    // Key used in writing to and reading from SharedPreferences.
    private static final String KEY_UUID = "key_uuid";

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    /**
     * Creates a UUID and saves it to {@link SharedPreferences}. The UUID is added to the published
     * message to avoid it being undelivered due to de-duplication. See {@link DeviceMessage} for
     * details.
     */
    private static String getUUID(SharedPreferences sharedPreferences) {
        String uuid = sharedPreferences.getString(KEY_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }

    /**
     * Nearby
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    // Views.

    /**
     * Nearby
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
    private Message mPubMessage;

    /**
     *  nearby
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;


    Button subscribe;
    Button list_devs;
    ImageView viewImage;
    public static TextView info_label;
    private float xCoOrdinate,xCoOrdinate2, yCoOrdinate;//for the image listener
    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    int mode = NONE;
    int NONE_cnt=0;
    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    boolean ischecked;
    devices Devices;

    private WifiP2pManager mManager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private boolean subscribing=false;
    private boolean client_started=false;

    private final IntentFilter mIntentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    public BroadcastReceiver mReceiver = null;

    static MyService mService;
  //  private Runnable runnable;
    Thread thread=null;
    Handler Thread_Handler;
    Runnable runnable;


    String dev_pub_name; //user defined Deice name
    public Uri selectedImage=null; //Uri for the selected image (Galler or Camera)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        subscribe=(Button)findViewById(R.id.subscribe_Button);
        list_devs=(Button)findViewById(R.id.list_devs);
        viewImage=(ImageView)findViewById(R.id.my_image);
        info_label=(TextView) findViewById((R.id.info_label));

        ischecked=false;
        Devices=new devices();

        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
            BluetoothAdapter.getDefaultAdapter().enable();

        dev_pub_name=BluetoothAdapter.getDefaultAdapter().getName();



        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        Intent intent = new Intent(this, MyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Build the message that is going to be published. This contains the device name and a
        // UUID.
        mPubMessage = DeviceMessage.newNearbyMessage(getUUID(getSharedPreferences(
                getApplicationContext().getPackageName(), Context.MODE_PRIVATE)));

        //TODO remove this later
        final List<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);
        buildGoogleApiClient();
        //Nearby
        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                mNearbyDevicesArrayAdapter.add(
                        DeviceMessage.fromNearbyMessage(message).getMessageBody());
              //  Toast.makeText(getApplicationContext(), DeviceMessage.fromNearbyMessage(message).getMessageBody().toString(), Toast.LENGTH_LONG).show();
                parse_message(DeviceMessage.fromNearbyMessage(message).getMessageBody().toString());
            }

            @Override
            public void onLost(final Message message) {
                // Called when a message is no longer detectable nearby.
                mNearbyDevicesArrayAdapter.remove(
                        DeviceMessage.fromNearbyMessage(message).getMessageBody());
            }
        };


        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Nearby: subsrcibe device info+orientation
                subscribe();
                selectImage();
            }
        });
        list_devs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                    // Configure the AlertDialog that the MyListDialog wraps
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.list);
                dialog.setTitle("Endpoint(s) Found");
                ListView listView = (ListView) dialog.findViewById(R.id.list);
                ArrayAdapter<String> ad = new ArrayAdapter<String>(MainActivity.this, R.layout.single_item , R.id.singleItem, Devices.dev_names);
                listView.setAdapter(ad);
                dialog.setCancelable(true);
                dialog.show();
                }

              //  if (mMyListDialog == null) {
                    // Configure the AlertDialog that the MyListDialog wraps
              /* //WiFidirect connect to seleted dev
               if(mService.peers.isEmpty())
                {
                    Toast.makeText(getApplicationContext(), "peers is empty", Toast.LENGTH_LONG).show();
                    return;
                }
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress =mService.peers.get(0).deviceAddress;
                config.wps.setup = WpsInfo.PBC;
               connect(config);
               */

             /*   mNearbyDevicesArrayAdapter.clear();
                for(int i=0;i<Devices.dev_List.size();i++)
                {
                    Log.d(TAG,"Listing");
                   // logAndShowSnackbar(Devices.dev_List.get(i).device_name+","+Devices.dev_List.get(i).IP+","+Devices.dev_List.get(i).azimuth+"");
                 //   mNearbyDevicesArrayAdapter.add(Devices.dev_List.get(i).device_name+","+Devices.dev_List.get(i).IP+","+Devices.dev_List.get(i).azimuth+"");
                Log.d(TAG,"Selecting "+Devices.select_best().device_name);
                }
                */

        });
        viewImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(viewImage.getDrawable()==null)
                    return false;
                Log.d("ddd",NONE_cnt+"");
                viewImage.setScaleType(ImageView.ScaleType.MATRIX);
                switch (event.getAction() & event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        xCoOrdinate = view.getX() - event.getRawX();
                        if(NONE_cnt==0)
                        {
                            xCoOrdinate2=xCoOrdinate;
                          //  NONE_cnt++;
                        }
                        yCoOrdinate = view.getY() - event.getRawY();
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        Log.d(TAG, "mode=DRAG" );
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_UP: //first finger lifted
                    case MotionEvent.ACTION_POINTER_UP: //second finger lifted
                        mode = NONE;
                        NONE_cnt++;
                        if(NONE_cnt>=2) {
                            if (Math.abs(xCoOrdinate) - Math.abs(xCoOrdinate2) < 5)
                                NONE_cnt++;
                            else
                                NONE_cnt = 0;
                        }
                        Log.w(TAG,"v1 "+xCoOrdinate+" v2 "+xCoOrdinate2);
                        if(NONE_cnt>2)
                        {

                            final CharSequence[] options = { "remove","Select another","Cancel"};
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("please pick a choice!");
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int item) {
                                    if (options[item].equals("remove"))
                                    {
                                        viewImage.setImageDrawable(null);
                                        //unsubscribe to start publising
                                        unsubscribe();
                                        MainActivity.this.disconnect();
                                    }
                                    else if (options[item].equals("Select another"))
                                    {
                                        subscribe();
                                        selectImage();
                                    }
                                    else if (options[item].equals("Cancel")) {
                                        dialog.dismiss();
                                    }
                                }
                            });
                            builder.create();
                            builder.show();
                            NONE_cnt=0;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN: //second finger down
                        oldDist = spacing(event); // calculates the distance between two points where user touched.
                        Log.d(TAG, "oldDist=" + oldDist);
                        // minimal distance between both the fingers
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                        if (mode == DRAG)
                        { //movement of first finger
                            matrix.set(savedMatrix);
                            if (view.getLeft() >= -392)
                            {
                                matrix.postTranslate(event.getX() - start.x, event.getY() -
                                        start.y);
                            }
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    //Get image from gallery
    private void selectImage() {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo"))
                {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, 1);
                }
                else if (options[item].equals("Choose from Gallery"))
                {
                    Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                }
                else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    //WiFiDirect: check if p2p is enabled
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart}, or if onStart() has already happened, it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
    //Nearby, prepare Nearby connection, called in OnCreate
    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    public void send_file()
    {
        Log.d(TAG, "Intent----------- " + selectedImage);
        Intent serviceIntent = new Intent(MainActivity.this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, selectedImage.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                mService.Groupinfo.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        Log.w(TAG, "strarting servce, connects to" +  mService.Groupinfo.groupOwnerAddress.getHostAddress()+":8988");
        MainActivity.this.startService(serviceIntent);

    }
    //ActivityResult, called after selecting an image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap=null;
        File f=null;
        String picturePath;
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) { //CAMERA
               f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                    picturePath=f.getAbsolutePath();
                    bitmap = Bitmap.createBitmap(BitmapFactory.decodeFile(picturePath,
                            bitmapOptions));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (requestCode == 2) { //Gallery
                selectedImage = data.getData();
                Log.w(TAG,data.getData().toString());
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                picturePath = c.getString(columnIndex);
                c.close();
                bitmap = (BitmapFactory.decodeFile(picturePath));
                Log.w("path of image from gallery......******************.........", picturePath + "");
            } else
                return;

            if(bitmap!=null)
                viewImage.setImageBitmap(bitmap);

            if(requestCode == 1)
            {
                if(f!=null)
                f.delete();
                selectedImage=Uri.fromFile(f);


            }
            //select best peer
            devices.device best_dev=Devices.select_best();
            if(best_dev==null) {
                Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_LONG).show();
                Log.w(TAG, "NO DEVICE FOUND");
            }

            else
            {

                //connect to selected peer
                Log.w(TAG, best_dev.device_name+"Selected "+best_dev.MAC);
                WifiP2pConfig config = new WifiP2pConfig();
                String peer_mac=mService.get_peer_MAC(best_dev.device_name);
                Log.w(TAG, "BEST MAC "+peer_mac);

                if(peer_mac==null){
                    Toast.makeText(getApplicationContext(), "device not found", Toast.LENGTH_LONG).show();
                    return;
                }
                config.deviceAddress =peer_mac; //mService.peers.get(0).deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if(mService.Groupinfo!=null)
                    send_file();
                else
                    connect(config);
            }

        }
    }
    //Nearby overriden methods
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        logAndShowSnackbar("Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onConnectionSuspended(int i) {
        logAndShowSnackbar("Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // We use the Switch buttons in the UI to track whether we were previously doing pub/sub (
        // switch buttons retain state on orientation change). Since the GoogleApiClient disconnects
        // when the activity is destroyed, foreground pubs/subs do not survive device rotation. Once
        // this activity is re-created and GoogleApiClient connects, we check the UI and pub/sub
        // again if necessary.
    }

    //end of Nearby overriden methods
    //parse messages received from Nearby subscriber
    public void parse_message(String msg)
    {
        String[] strArray = msg.split(",");  //0 dev name, 1 IP, 2 Azimuth

        Devices.updateList(strArray[0],strArray[1],strArray[2]);
      // Toast.makeText(getApplicationContext(),strArray[0]+"--"+strArray[1]+"--"+strArray[2], Toast.LENGTH_LONG).show();
    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    private void subscribe() {
        Log.i(TAG, "Subscribing");
        mNearbyDevicesArrayAdapter.clear();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Subscribed successfully.");
                        } else {
                            logAndShowSnackbar("Could not subscribe, status = " + status);
                        }
                    }
                });
        subscribing=true;
    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    private void publish() {
        Log.i(TAG, "Publishing");
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String mac = wm.getConnectionInfo().getMacAddress();
        String msgBody="("+ dev_pub_name+"),"+mac+","+Float.toString(mService.Get_orientation());
        mPubMessage = DeviceMessage.newNearbyMessage(getUUID(getSharedPreferences(
                getApplicationContext().getPackageName(), Context.MODE_PRIVATE)),msgBody);

        Nearby.Messages.publish(mGoogleApiClient, mPubMessage)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Published successfully.");
                        } else {
                            logAndShowSnackbar("Could not publish, status = " + status);
                        }
                    }
                });

    }

    /**
     * Stops subscribing to messages from nearby devices.
     */
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing.");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
        subscribing=false;
    }

    /**
     * Stops publishing message to nearby devices.
     */
    private void unpublish() {
        Log.i(TAG, "Unpublishing.");
        Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage);
    }

    /**
     * Logs a message and shows a {@link Snackbar} using {@code text};
     *
     * @param text The text used in the Log message and the SnackBar.
     * TODO: remove this later
     */
    private void logAndShowSnackbar(final String text) {
        Log.w(TAG, text);
       /* View container = findViewById(R.id.activity_main_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_SHORT).show();
        }
        */
        Toast.makeText(getApplicationContext(),text, Toast.LENGTH_LONG).show();
    }

    public void load_recieved_image()
    {
        File f = new File(Environment.getExternalStorageDirectory() + "/"
                + getPackageName() + "/PAMI_temp.jpg");

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        final Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(f.getAbsolutePath(),
                bitmapOptions), 480, 800, false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewImage.setImageBitmap(bitmap);
            }
        });


    }

    @Override
    protected void onStop() {
        super.onStop();
        //unbindService(mConnection);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG,"Destroy");
        this.disconnect();
        thread.interrupt();


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter); //stop WifiDirect
    }
    @Override
    protected void onStart() {
        super.onStart();
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() { //look for WiFiDirect peers

            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this,"Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(getApplicationContext(), "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();

            }
        });
        if(thread!=null)
            return;
        Thread_Handler=new Handler();
        runnable=new Runnable() {


            @Override
            public void run() {
                try
                {
                   while (!thread.currentThread().isInterrupted())
                    {
                        Thread.sleep(1000);//wait for 1 second
                        if(mService.Groupinfo!=null) {

                            if (mService.Groupinfo.groupFormed && !mService.Groupinfo.isGroupOwner && client_started == false) {
                                send_file();
                                client_started = true;
                                continue;
                            }
                        }
                        if(mService.received_image==true)
                            load_recieved_image();
                        if(subscribing)
                            continue;
                        publish();

                    }

                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        thread=new Thread(runnable );
            thread.start();

    }

    //binder to Myservice
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    //WiFiDirect overriden methods

    @Override
    public void onChannelDisconnected() {

    }

    @Override
    public void showDetails(WifiP2pDevice device) {

    }

    @Override
    public void cancelDisconnect() {

    }
    //from ConnectionInfoListener
    @Override
    public void connect(WifiP2pConfig config) {
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext() , "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void disconnect() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnect succeeded");
            }

        });

    }
    //end of WiFiDirect overriden methods

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds options to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        switch (item.getItemId()) {
            case R.id.about:
                //showMsg("This helps you share files");
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
                // Setting Dialog Title
                alertDialogBuilder.setTitle("About");
                // Setting Dialog Message
                alertDialogBuilder.setMessage("This is a prototype app, it share files with the Android device you are pointing to");
                // Setting Icon to Dialog
                //alertDialog.setIcon(R.drawable.tick);
                // Setting OK Button
                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog closed
                     //   Toast.makeText(getApplicationContext(), "Enjoy", Toast.LENGTH_SHORT).show();
                    }
                });
                // Showing Alert Message
                alertDialogBuilder.show();
                return true;
            case R.id.help:
                //showMsg("Help is under construction");
                android.app.AlertDialog.Builder alertDialogBuilder1 = new android.app.AlertDialog.Builder(this);
                // Setting Dialog Title
                alertDialogBuilder1.setTitle("Help");
                // Setting Dialog Message
                alertDialogBuilder1.setMessage("for help, please have a look at our tutorial video: \n http://www.yahoo.com");
                // Setting Icon to Dialog
                //alertDialog.setIcon(R.drawable.tick);
                // Setting OK Button
                alertDialogBuilder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog closed
                        Toast.makeText(getApplicationContext(), "Enjoy", Toast.LENGTH_SHORT).show();
                    }
                });
                alertDialogBuilder1.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
