package com.example.kolokvijum2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    EditText etPrvo, etDrugo;
    ImageView imageView;
    CheckBox checkbox;
    Switch switchZvuk;

    MediaPlayer mediaPlayer;

    LocationManager locationManager;
    LocationListener locationListener;

    SensorManager sensorManager;
    Sensor akcelerometar;

    int checkboxBrojac = 0;
    boolean slusaLokaciju = false;
    boolean slusaAkcelerometar = false;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_LOCATION = 2;
    static final int REQUEST_CAMERA = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPrvo = findViewById(R.id.etPrvo);
        etDrugo = findViewById(R.id.etDrugo);
        imageView = findViewById(R.id.imageView);
        checkbox = findViewById(R.id.checkbox);
        switchZvuk = findViewById(R.id.switchZvuk);

        setupSenzori();
        setupLokacija();
        setupTextWatcher();
        setupCheckbox();
    }

    // ===================== LOKACIJA =====================

    void setupLokacija() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (slusaLokaciju) {
                    etDrugo.setText("Lat: " + location.getLatitude()
                            + " | Lon: " + location.getLongitude());
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    // ===================== SENZORI =====================

    void setupSenzori() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        akcelerometar = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (slusaAkcelerometar && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            etDrugo.setText("X: " + x + " | Y: " + y + " | Z: " + z);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ===================== TEXT WATCHER =====================

    void setupTextWatcher() {
        etPrvo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String tekst = s.toString().trim();

                if (tekst.equalsIgnoreCase("kamera")) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                    } else {
                        otvoriKameru();
                    }
                    return;
                }

                try {
                    int id = Integer.parseInt(tekst);
                    fetchKomentar(id);
                } catch (NumberFormatException e) {
                    // nije broj
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    void otvoriKameru() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
        }
    }

    // ===================== CHECKBOX =====================

    void setupCheckbox() {
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxBrojac++;
                pustiZvuk();

                if (checkboxBrojac == 2) {
                    slusaAkcelerometar = false;
                    slusaLokaciju = true;
                    sensorManager.unregisterListener(this);
                } else if (checkboxBrojac >= 3) {
                    slusaLokaciju = false;
                    slusaAkcelerometar = true;
                    sensorManager.registerListener(this, akcelerometar,
                            SensorManager.SENSOR_DELAY_NORMAL);
                }
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        });
    }

    void pustiZvuk() {
        mediaPlayer = MediaPlayer.create(this, R.raw.zvuk);
        if (switchZvuk.isChecked()) {
            mediaPlayer.setLooping(true);
        } else {
            mediaPlayer.setLooping(false);
        }
        mediaPlayer.start();
    }

    // ===================== RETROFIT =====================

    void fetchKomentar(int id) {
        RetrofitClient.getApiService().getComment(id).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    etDrugo.setText(response.body().getBody());
                }
            }

            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Greška: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===================== PERMISSIONS =====================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_LOCATION) setupLokacija();
            if (requestCode == REQUEST_CAMERA) otvoriKameru();
        }
    }

    // ===================== LIFECYCLE =====================

    @Override
    protected void onResume() {
        super.onResume();
        if (slusaAkcelerometar) {
            sensorManager.registerListener(this, akcelerometar,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}