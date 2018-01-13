package com.bytasaur.adbnocableroot;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private Runtime runtime;
    private TextView status;
    private TextView ip_text;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        runtime = Runtime.getRuntime();
        status = findViewById(R.id.state);
        ip_text = findViewById(R.id.ip);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStatus(null);
        setIp(null);
    }

    @SuppressLint("SetTextI18n")
    public void setStatus(View v) {
        try {
            Process getprop = runtime.exec("getprop service.adb.tcp.port");
            InputStream inputStream = getprop.getInputStream();
            byte buff[] = new byte[8];
            int s = inputStream.read(buff);
            if(new String(buff, 0, s-1).equals("5555")) {
                status.setText("Enabled");
            }
            else {
                status.setText("Disabled");
            }
            getprop.waitFor();
        }
        catch(Exception ignored) {}
    }

    public void setIp(View v) {
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        if(ip==0) {
            ip_text.setText("N/A");
        }
        else {
            ip_text.setText(Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress()));
        }
    }

    public void enable(View v) {
        setProperty("setprop service.adb.tcp.port 5555\n");
        // Can send integer port instead but sending complete string for performance
        setIp(null);
    }

    public void disable(View v) {
        setProperty("setprop service.adb.tcp.port -1\n");
    }

    void setProperty(String cmd) {  // int??
        Process su = null;
        DataOutputStream outputStream = null;
        try {
            su = runtime.exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(cmd);
            outputStream.flush();

            outputStream.writeBytes("stop adbd\n");
            outputStream.flush();

            outputStream.writeBytes("start adbd\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            su.waitFor();
            outputStream.close();

            setStatus(null);
        }
        catch(Exception ex) {
            if(su == null && ex.getClass().equals(IOException.class)) {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
            else if(su!=null) {
                su.destroy();
            }
            if(outputStream!=null) {
                try {
                    outputStream.close();
                }
                catch (IOException ignored) {}
            }
        }
    }
}
