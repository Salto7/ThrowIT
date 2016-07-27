package com.google.android.gms.nearby.messages.samples.nearbydevices;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements PeerListListener,WifiP2pManager.ConnectionInfoListener {

    private WifiP2pManager manager;
    private Channel channel;
    private MainActivity activity;

    private WifiP2pInfo info;
    public List<WifiP2pDevice> peers;

    /**
     * @param manager  WifiP2pManager system service
     * @param channel  Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
        peers = new ArrayList<WifiP2pDevice>();
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                //TODO reset data;

            }
            Log.d(MainActivity.TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                manager.requestPeers(channel, (PeerListListener) this);
            }
            Log.d(MainActivity.TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                //  DeviceDetailFragment fragment = (DeviceDetailFragment) activity
                //         .getFragmentManager().findFragmentById(R.id.frag_detail);
                manager.requestConnectionInfo(channel, this);
            } else {
                // It's a disconnect
                //  activity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //    DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
            //            .findFragmentById(R.id.frag_list);
            //    fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
            //            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

        if(wifiP2pInfo==null)
            return;
        this.info = wifiP2pInfo;
        MainActivity.mService.Groupinfo=wifiP2pInfo;

        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
            Log.w("GROUP","this is a group owner");
            Log.w("GROUP","starting file server");
            MainActivity.mService.start_server();

        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
            Log.w("GROUP","this is a client");
        }
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        peers.clear();
        peers.addAll(wifiP2pDeviceList.getDeviceList());
        if (peers.size() == 0) {
            Log.d("WifiBroadcst","No devices found");
            return;
        }
        else{
            for(int i=0;i<peers.size();i++)
            {
                Log.d("WifiBroadcst","found:"+peers.get(i).deviceName);
            }
        }
        MainActivity.mService.peers=this.peers;
    }
}