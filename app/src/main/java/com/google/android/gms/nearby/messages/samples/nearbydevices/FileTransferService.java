package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 50000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(MainActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(MainActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(MainActivity.TAG, e.toString());
                }
                FileServerAsyncTask.copyFile(is, stream);
                Log.d(MainActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(MainActivity.TAG, "Error:"+e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}
