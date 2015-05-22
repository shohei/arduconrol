package com.shoheiaoki.arducontrol;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;


public class MainActivity extends Activity {
    UsbManager manager;
    UsbSerialDriver usb;
    WebSocketClient mWebSocketClient;
    final static String TAG = "WakeLock1";
    PowerManager powerManager;
    ScrollView mScrollView;
    TextView mTextView;
    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        wakeLock.acquire();
        mScrollView = (ScrollView) findViewById(R.id.mScrollView);
        mTextView = (TextView) findViewById(R.id.mTextView);
        mHandler = new Handler(this.getMainLooper());
        showFunkyMessage();

        webSocketConnection();
        initUsb();
        start_read_thread(); // シリアル通信を読むスレッドを起動

    }

    protected void showMessage(final String msg){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.append(msg);
            }
        });
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        },100);
    }

    protected void showFunkyMessage(){
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
            }
            showMessage(log.toString());
        } catch (IOException e) {
        }
    }

    protected void webSocketConnection() {
        URI uri;
        try {
            uri = new URI("ws://heroku-echo.herokuapp.com:80");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.v("hoge", "received: " + message);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }


    protected void initUsb() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usb = UsbSerialProber.acquire(manager);
        if (usb != null) {
            try {
                usb.open();
                usb.setBaudRate(9600);
//                start_write_thread(); // シリアル通信を読むスレッドを起動
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start_read_thread() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        byte buf[] = new byte[256];
                        int num = usb.read(buf, buf.length);
                        if (num > 0) {
                            String readMsg = new String(buf,0,num);
                            Log.v("arduino", readMsg); // Arduinoから受信した値をlogcat出力
                            showMessage(readMsg); // Arduinoから受信した値をlogcat出力
                            try {
                                mWebSocketClient.send(readMsg);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Thread.sleep(10);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void start_write_thread() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        usb.write("o".getBytes("UTF-8"), 1);
                        Thread.sleep(1000);
                        usb.write("x".getBytes("UTF-8"), 1);
                        Thread.sleep(1000);
                        Thread.sleep(10);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

}
