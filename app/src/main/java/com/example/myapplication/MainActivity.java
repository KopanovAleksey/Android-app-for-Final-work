package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    private static final String TAG = "BluetoothTemp";

    private MqttClient mqttClient;
    private String brokerUrl = "tcp://91.122.44.242:1883";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private TextView temperatureTextView;
    private static final String DEVICE_ADDRESS = "C8:2E:18:C3:8D:82"; // MAC-адрес вашего устройства
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Стандартный UUID для SPP
    String temperatureData ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatureTextView = findViewById(R.id.temperatureTextView);
        Button connectButton = findViewById(R.id.connectButton);
        Button receiveDataButton = findViewById(R.id.receiveButton);

        Button mqttConnectButton = findViewById(R.id.mqttConnectButton);
        Button mqttPubButton = findViewById(R.id.mqttPubButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show();
            finish();
        }

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice();
            }
        });
        receiveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveData();
            }
        });

        mqttConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupMqtt();
            }
        });

        mqttPubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataToMqtt(temperatureData);
            }
        });
    }

    private void setupMqtt(){
       try {
           if(mqttClient == null)
               mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId(), null);
           MqttConnectOptions options = new MqttConnectOptions();
           options.setUserName("IoT");
           options.setPassword("KopanovAE".toCharArray());
           if(!mqttClient.isConnected()) {
               mqttClient.connect(options);
               Toast.makeText(this, "Успешно подключенно к MQTT - брокеру", Toast.LENGTH_SHORT).show();
           }
           else
               Toast.makeText(this, "Устройство уже подключенно к MQTT - брокеру", Toast.LENGTH_SHORT).show();
       } catch (MqttException e){
           Toast.makeText(this, "Не удалось подключиться к MQTT - брокеру", Toast.LENGTH_SHORT).show();
       }
    }

    private void sendDataToMqtt(String data) {
        if(mqttClient != null) {
            try {
                if (data!= null) {
                    MqttMessage message = new MqttMessage(data.getBytes());
                    message.setQos(1);
                    mqttClient.publish("temperature/topic", message);
                    Toast.makeText(this, "Данные успешно опубликованы", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "Нет данных для публикации", Toast.LENGTH_SHORT).show();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "MQTT - брокер не подключен", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            }else {
               if(bluetoothSocket == null)
                   bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
               bluetoothSocket.connect();
               //TODO: Сокет закрыт или занят обработка
               Toast.makeText(this, "Подключено к устройству", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Log.e(TAG, "Ошибка подключения", e);
            Toast.makeText(this, "Не удалось подключиться к устройству", Toast.LENGTH_SHORT).show();
        }
    }

    private void receiveData() {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                InputStream inputStream = bluetoothSocket.getInputStream();
                outputStream.write(1);
                byte[] buffer = new byte[256];
                int bytes;
                if ((bytes = inputStream.read(buffer)) != -1) {
                    temperatureData = new String(buffer, 0, bytes);
                    temperatureTextView.setText("Temperature: " + temperatureData);
                }

            } else {
                Toast.makeText(this, "Устройство не подключено", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при получении данных", e);
            Toast.makeText(this, "Не удалось получить данные", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на использование Bluetooth предоставлено.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Разрешение на использование Bluetooth не предоставлено.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}