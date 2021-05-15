package org.turbo.beaconmqtt.broadcaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.SettingsActivity;
import org.turbo.beaconmqtt.beacon.BaseBeacon;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.turbo.beaconmqtt.SettingsActivity.MQTT_BEACON_STATE_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_BEACON_STATE_TOPIC_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_ENTER_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_EXIT_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_TOPIC_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_PASS_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_PORT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_SERVER_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_TRACK_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_TRACK_TOPIC_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_USER_KEY;

public class MqttBroadcaster extends BaseBroadcaster {
    private static final String TAG = MqttBroadcaster.class.getName();
    @SuppressWarnings("unused")
    private static final int QOS_LEVEL = 2;
    private final Context context;
    private final Broadcaster broadcaster;
    private final SharedPreferences defaultSharedPreferences;
    private MqttAndroidClient client;
    @SuppressWarnings("FieldCanBeLocal")
    private MqttCallbackExtended callback;
    private int mqttMessageId = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    public MqttBroadcaster(final Broadcaster broadcaster, final Context context) {
        this.broadcaster = broadcaster;
        this.context = context;
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        registerSettingsChangeListener();

        mName = defaultSharedPreferences.getString(MQTT_SERVER_KEY, null);
        String mqttPort = defaultSharedPreferences.getString(MQTT_PORT_KEY, null);
        String mqttUser = defaultSharedPreferences.getString(MQTT_USER_KEY, null);
        String mqttPassword = defaultSharedPreferences.getString(MQTT_PASS_KEY, null);

        connectToMqttServer(mName, mqttPort, mqttUser, mqttPassword);
    }

    private void registerSettingsChangeListener() {
        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (MQTT_SERVER_KEY.equals(key) || MQTT_PORT_KEY.equals(key) ||
                        MQTT_USER_KEY.equals(key) || MQTT_PASS_KEY.equals(key)) {
                    mName = defaultSharedPreferences.getString(MQTT_SERVER_KEY, null);
                    String mqttPort = defaultSharedPreferences.getString(MQTT_PORT_KEY, null);
                    String mqttUser = defaultSharedPreferences.getString(MQTT_USER_KEY, null);
                    String mqttPassword = defaultSharedPreferences.getString(MQTT_PASS_KEY, null);
                    connectToMqttServer(mName, mqttPort, mqttUser, mqttPassword);
                    broadcaster.notifyListeners();
                }
            }
        };
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void connectToMqttServer(final String mqttServer, String mqttPort, String mqttUser, String mqttPassword) {
        if (mqttServer != null && mqttPort != null) {
            final String serverUri = "tcp://" + mqttServer + ":" + mqttPort;
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(context, serverUri,
                    clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            //noinspection StatementWithEmptyBody
            if (TextUtils.isEmpty(mqttUser) || TextUtils.isEmpty(mqttPassword)) {
            } else {
                options.setUserName(mqttUser);
                options.setPassword(mqttPassword.toCharArray());
            }
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);

            try {
                IMqttToken token = client.connect(options);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        //debugLog("MQTT connected!");
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        client.setBufferOpts(disconnectedBufferOptions);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        //debugLog("MQTT connection failed!");

                        if (SettingsActivity.isActive()) {
                            Toast.makeText(context, "MQTT connection failed!", Toast.LENGTH_LONG).show();
                        }
                        mState = false;
                        broadcaster.notifyListeners();
                    }

                });
            } catch (MqttException e) {
                e.printStackTrace();
            }

            callback = new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable t) {
                    //debugLog("MQTT connection lost!");
                    mState = false;
                    broadcaster.notifyListeners();
                }

                //Notice to application that connection is up
                @Override
                public void connectComplete(boolean reconnect, String url) {
                    //noinspection StatementWithEmptyBody
                    if (reconnect) {
                        //debugLog("MQTT reconnected to " + url);
                    } else {
                        //debugLog("MQTT connected to " + url);
                        if (SettingsActivity.isActive()) {
                            Toast.makeText(context, "MQTT connected to " + url, Toast.LENGTH_LONG).show();
                        }
                    }
                    mState = true;
                    broadcaster.notifyListeners();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    //debugLog("MQTT messageArrived() called");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    BeaconApplication application = ((BeaconApplication) context.getApplicationContext());
                    int messageId = -1;

                    try {
                        messageId = token.getMessage().getId();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    application.debugLog("MQTT message ID:" + messageId + " delivered");
                }
            };

            client.setCallback(callback);
        }
    }


    private void mqttPublish(String topic, String payload, String method) {
        BeaconApplication application = ((BeaconApplication) context.getApplicationContext());
        byte[] encodedPayload;

        try {
            encodedPayload = payload.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setId(mqttMessageId++);
            message.setRetained(true);

            if (client != null) {
                client.publish(topic, message);
                application.debugLog("Send " + method + " MQTT message ID:" + message.getId());
            }
        } catch (NullPointerException | MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void state(BaseBeacon beacon) {
        String topic = defaultSharedPreferences.getString(MQTT_BEACON_STATE_TOPIC_KEY, "");

        if (!Objects.requireNonNull(topic).isEmpty()) {
            topic = parseStaticTokens(topic, beacon);

            String payload = defaultSharedPreferences.getString(MQTT_BEACON_STATE_MESSAGE_KEY, "");
            payload = parseStaticTokens(payload, beacon);

            mqttPublish(topic, payload, "state");
        }
    }

    @Override
    public void enterMaster(BaseBeacon beacon) {
        Log.i(TAG, "enterMaster");
        String topic = defaultSharedPreferences.getString(MQTT_MASTER_TOPIC_KEY, "");


        if (!Objects.requireNonNull(topic).isEmpty()) {
            topic = parseStaticTokens(topic, beacon);

            String payload = defaultSharedPreferences.getString(MQTT_MASTER_ENTER_MESSAGE_KEY, "");
            payload = parseStaticTokens(payload, beacon);

            Log.i(TAG, "topic: " + topic + ", payload: " + payload);

            mqttPublish(topic, payload, "onEnterMaster");
        }

    }

    @Override
    public void exitMaster(BaseBeacon beacon) {
        String topic = defaultSharedPreferences.getString(MQTT_MASTER_TOPIC_KEY, "");

        if (!Objects.requireNonNull(topic).isEmpty()) {
            topic = parseStaticTokens(topic, beacon);

            String payload = defaultSharedPreferences.getString(MQTT_MASTER_EXIT_MESSAGE_KEY, "");
            payload = parseStaticTokens(payload, beacon);

            mqttPublish(topic, payload, "onExitMaster");
        }

    }

    @Override
    public void track(BaseBeacon beacon) {
        String topic = defaultSharedPreferences.getString(MQTT_TRACK_TOPIC_KEY, "");

        if (!Objects.requireNonNull(topic).isEmpty()) {
            topic = parseStaticTokens(topic, beacon);

            String payload = defaultSharedPreferences.getString(MQTT_TRACK_MESSAGE_KEY, "");
            payload = parseStaticTokens(payload, beacon);
            payload = parseDynamicTokens(payload, beacon);

            mqttPublish(topic, payload, "track");
        }
    }
}
