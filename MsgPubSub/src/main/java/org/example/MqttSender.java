package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttSender {
    private static final String MQTT_BROKER = System.getenv("MQTT_BROKER");
    private static final String MQTT_CLIENT = System.getenv("MQTT_CLIENT");
    private static final String MQTT_TOPIC = System.getenv("MQTT_TOPIC");
    private static final String MQTT_USERNAME = System.getenv("MQTT_USERNAME");
    private static final String MQTT_PASSWORD = System.getenv("MQTT_PASSWORD");
    
    private MqttClient mqttClient;
    public MqttSender() throws MqttException {
        mqttClient = new MqttClient(MQTT_BROKER, MQTT_CLIENT);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(MQTT_USERNAME);
        options.setPassword(MQTT_PASSWORD.toCharArray());
        mqttClient.connect();
    }
    public void sendMessage(String message) throws MqttException{
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttClient.publish(MQTT_TOPIC, mqttMessage);
    }
    public void disconnect() throws MqttException{
        if(mqttClient!=null && mqttClient.isConnected()){
            mqttClient.disconnect();
        }
    }


}
