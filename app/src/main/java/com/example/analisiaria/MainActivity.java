package com.example.analisiaria;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "AiotMqtt";
    /* 设备三元组信息 */
    final private String PRODUCTKEY = "a11xsrWmW14";
    /* 阿里云Mqtt服务器域名 */
    final String host = "tcp://" + PRODUCTKEY + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:443";
    final private String DEVICENAME = "paho_android";
    final private String DEVICESECRET = "tLMT9QWD36U2SArglGqcHCDK9rK9nOrA";
    /* 自动Topic, 用于上报消息 */
    final private String PUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/update";
    /* 自动Topic, 用于接受消息 */
    final private String SUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/get";
    MqttAndroidClient mqttAndroidClient;
    private String clientId;
    private String userName;
    private String passWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 获取Mqtt建连信息clientId, username, password */
        AiotMqttOption aiotMqttOption = new AiotMqttOption().getMqttOption(PRODUCTKEY, DEVICENAME, DEVICESECRET);
        if (aiotMqttOption == null) {
            Log.e(TAG, "device info error");
        } else {
            clientId = aiotMqttOption.getClientId();
            userName = aiotMqttOption.getUsername();
            passWord = aiotMqttOption.getPassword();
        }

        /* 创建MqttConnectOptions对象并配置username和password */
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());


        /* 创建MqttAndroidClient对象, 并设置回调接口 */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });

        /* Mqtt建连 */
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");

                    subscribeTopic(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

        /* 通过按键发布消息 */
        Button pubButton = findViewById(R.id.publish);
        pubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage("hello IoT");
            }
        });
    }

    /**
     * 订阅特定的主题
     *
     * @param topic mqtt主题
     */
    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向默认的主题/user/update发布消息
     *
     * @param payload 消息载荷
     */
    public void publishMessage(String payload) {
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }
}