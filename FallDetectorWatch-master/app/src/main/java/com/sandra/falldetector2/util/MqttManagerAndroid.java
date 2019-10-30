package com.sandra.falldetector2.util;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

/*

Adicionar no buildscript do build.gradle

    repositories {
            maven {
            url "https://repo.eclipse.org/content/repositories/paho-snapshots/"
            }
            }

Adicionar as dependencias no build.gradle

            dependencies {
        compile 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
        compile 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
    }

Adicionar permissoes do Usuario no AndroidManifest.xml

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

Adicionar a tag antes do </application> no AndroidManifest.xml

    <service android:name="org.eclipse.paho.android.service.MqttService" />

Para utilizar em outra classe fazer ela implementar a interface MqttCallbackExtended

    Exemplo: Implements MqttCallbackExtended

Instanciar o Mqtt Manager

    MqttManagerAndroid mqttManagerAndroid = new MqttManagerAndroid();

Utilizar o callback

    mqttManagerAndroid.getMqttAndroidClient().setCallback(this)

Publicar Mensagem

    mqttManagerAndroid.getMqttAndroidClient().publishMessage("Testando 123","/teste/recieve");

Subscribe a um topico

    mqttManagerAndroid.getMqttAndroidClient().subscribeToTopic("/teste/message");

*/



public class MqttManagerAndroid {

    //Configuracoes iniciais do MQTT
    private MqttAndroidClient mqttAndroidClient;
    private final String brokerIPAdress = "test.mosquitto.org";
    private final String brokerPort = "1883";
    private final String clientID = "androidMQTT" + UUID.randomUUID();
    private final int connectionTimeOut = 30;
    private final int keepAliveInterval = 60;
    private final Boolean isCleanSession = true;
    private final Boolean isAutoRecconect = true;
    private final String userMqtt = "admin";
    private final String passwordMqtt = "admin";
    private final String mqttUrl = "tcp://" + brokerIPAdress + ":" + brokerPort;
    private static final String TAG = "testeMQTT";


    //Construtor do MQTT
    public MqttManagerAndroid(Context context)
    {

        //Iniciando Client do MQTT com as definicoes iniciais
        setMqttAndroidClient(new MqttAndroidClient(context,mqttUrl,clientID));
        getMqttAndroidClient().setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                Log.w(TAG, "Conexao Completada");
                //publishMessage("oi App denovo","/teste");

            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.w(TAG, "Conexao Perdida");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.w(TAG, "Mensagem Recebida: " + message.toString());
                //publishMessage("Testando 123","/teste/recieve");

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

                Log.w(TAG, "Mensagem Publicada");

            }
        });
        connect();
    }

    private void connect()
    {
        MqttConnectOptions myConnectOption = new MqttConnectOptions();
        myConnectOption.setCleanSession(isCleanSession);
        myConnectOption.setAutomaticReconnect(isAutoRecconect);
        myConnectOption.setConnectionTimeout(connectionTimeOut);
        myConnectOption.setKeepAliveInterval(keepAliveInterval);
        try
        {
            getMqttAndroidClient().connect(myConnectOption, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    getMqttAndroidClient().setBufferOpts(disconnectedBufferOptions);
                    //subscribeToTopic("/teste/message");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception)
                {
                    Log.w(TAG, "Falha ao conectar: " + mqttUrl + exception.toString());
                }
            });
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
    }

    public void subscribeToTopic(String topic)
    {
        try {
            getMqttAndroidClient().subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    Log.w(TAG,"Subscribed no topico: " + topic);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    Log.w(TAG, "Nao foi possivel subscribe!");
                }
            });
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
    }

    public void publishMessage(String message,String topic)
    {
        try
        {
            getMqttAndroidClient().publish(topic, new MqttMessage(message.getBytes()));

        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
    }

    public MqttAndroidClient getMqttAndroidClient() {
        return mqttAndroidClient;
    }

    public void setMqttAndroidClient(MqttAndroidClient mqttAndroidClient) {
        this.mqttAndroidClient = mqttAndroidClient;
    }
}

