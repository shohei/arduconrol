package com.shoheiaoki.arducontrol;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;


public class MainActivity extends Activity {
    UsbManager manager;
    UsbSerialDriver usb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usb = UsbSerialProber.acquire(manager);
        if (usb != null) {
            try {
                usb.open();
                usb.setBaudRate(9600);
                start_read_thread(); // シリアル通信を読むスレッドを起動
                start_write_thread(); // シリアル通信を読むスレッドを起動
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
                        if (num > 0)
                            Log.v("arduino", new String(buf, 0, num)); // Arduinoから受信した値をlogcat出力
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
