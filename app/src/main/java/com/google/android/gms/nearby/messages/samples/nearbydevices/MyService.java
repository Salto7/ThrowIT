package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MyService extends Service implements
        SensorEventListener{

    private SensorManager mSensorManager;
    private Sensor sensorAccelerometer,sensorMagnetic;

    float[] magnetic;
    float[] gravity;
    public float[] orientation;

    public HashMap<String, String> devs;
    public HashMap<String, String> orient;
    private static final String TAG = "service";
    IBinder mBinder = new LocalBinder();

    public List<WifiP2pDevice> peers;//list that holds wifiDirect peers
    public WifiP2pInfo Groupinfo=null;

    public boolean received_image=false;

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        // mSensor= mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        Toast.makeText(getApplicationContext(), "Service created", Toast.LENGTH_SHORT).show();
        Log.d("s","service created");
    }

    public float Get_orientation()
    {
        //azimuth
        BigDecimal bigDecimal = new BigDecimal((float) Math.toDegrees(orientation[0]));
        bigDecimal = bigDecimal.setScale(3,BigDecimal.ROUND_HALF_UP);
        return bigDecimal.floatValue();
    }
    public static float convertToFloat(byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        return buffer.getFloat();

    }

    public String get_peer_MAC(String dev_name)
    {
        String ret=null;
        for(int i=0;i<peers.size();i++)
        {
            Log.w(TAG,"comparing "+dev_name+" to "+peers.get(i).deviceName);
            if(dev_name.contains(peers.get(i).deviceName))
            {
                Log.w(TAG,"Found "+peers.get(i).deviceName+" in "+dev_name);
                ret= peers.get(i).deviceAddress;
            }
        }
        return ret;
    }
    public void start_server()
    {
        new FileServerAsyncTask(getApplicationContext(), MainActivity.info_label)
                .execute();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Toast.makeText(getApplicationContext(), "Service Started", Toast.LENGTH_SHORT).show();
        Log.d("s","service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Service Destroyed", Toast.LENGTH_SHORT).show();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magnetic = sensorEvent.values;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = sensorEvent.values;
        if ((gravity == null) || (magnetic == null))
            return;
        float[] fR = new float[9];
        float[] fI = new float[9];
        if (!SensorManager.getRotationMatrix(fR, fI, gravity, magnetic))
            return;
        if (orientation == null)
            orientation = new float[3];
        SensorManager.getOrientation(fR, orientation);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class LocalBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG,"binding");
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mBinder;
    }
}
