
package com.efe.tracker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.util.Log;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private Location lastLocation = null;
    private long lastUpdateTime = 0;
    private long lastMovedTime = 0;
    private boolean vehicleStopped = false;
    private String botToken = "7739075002:AAEpEvduB6kSgdjtb9LogBdHIVBVFRDherw";
    private String chatId = "1772624267";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textView = new TextView(this);
        textView.setText("GPS durumu: Bekleniyor...");
        setContentView(textView);

        sendTelegram("Connected\n— Efe’nin takip sisteminden gönderildi.");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                long currentTime = System.currentTimeMillis();
                textView.setText("GPS data active");

                if (lastLocation == null || location.distanceTo(lastLocation) > 5) {
                    lastLocation = location;
                    lastUpdateTime = currentTime;
                    lastMovedTime = currentTime;
                    vehicleStopped = false;
                    String message = "📍 Konum:\nLat: " + location.getLatitude() +
                                     "\nLon: " + location.getLongitude() +
                                     "\n— Efe’nin takip sisteminden gönderildi.";
                    sendTelegram(message);
                } else {
                    if (!vehicleStopped && (currentTime - lastMovedTime) >= 5 * 60 * 1000) {
                        vehicleStopped = true;
                        sendTelegram("🚨 Araç 5 dakikadır hareketsiz.\n— Efe’nin takip sisteminden gönderildi.");
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {
                textView.setText("No GPS data");
            }
        });
    }

    private void sendTelegram(String message) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendMessage");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"chat_id\": \"" + chatId + "\", \"text\": \"" + message + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                conn.getResponseCode(); // Tetiklemek için
                conn.disconnect();
            } catch (Exception e) {
                Log.e("Telegram", "Mesaj gönderme hatası", e);
            }
        }).start();
    }
}
