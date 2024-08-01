package org.example;

import com.rabbitmq.client.*;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String QUEUE_NAME = System.getenv("QUEUE_NAME");
    private static final String EXCHANGE_NAME = System.getenv("EXCHANGE_NAME");
    private static final String ROUTING_KEY = System.getenv("ROUTING_KEY");
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
    private static final int RABBITMQ_PORT = Integer.parseInt(System.getenv("RABBITMQ_PORT"));
    private static final String RABBITMQ_USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");

    private MqttSender mqttSender;
    public static void main(String[] args) {
        Main receiver = new Main();
        try{
            receiver.start();
        }catch(IOException | TimeoutException | MqttException e){
            System.err.println("Failed to start RabbitMQ receiver: " +e.getMessage());
            e.printStackTrace();
        }

    }
    private void configureRabbitMQ(Channel channel) throws IOException{
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
    }
    private ConnectionFactory createConnectionFactory(){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setPort(RABBITMQ_PORT);
        factory.setUsername(RABBITMQ_USERNAME);
        factory.setPassword(RABBITMQ_PASSWORD);
        return factory;
    }
    private void consumeMessages(Channel channel) throws IOException{
        System.out.println(" [*] Waiting for message. To exit press CTRL+C");
        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException{
                String msg = new String(body, StandardCharsets.UTF_8);
                System.out.println("[X] Received '"+msg+"'");
                handleMessage(msg);
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
    private void handleMessage(String message){
        System.out.println(message);
        try{
            mqttSender.sendMessage(message);
            System.out.println("Send Success to MQTT");
        }catch(MqttException e){
            System.err.println("Failed to send MQTT: "+e.getMessage());
            e.printStackTrace();
        }
    }
    private void start() throws IOException, TimeoutException, MqttException {
        mqttSender = new MqttSender();
        ConnectionFactory factory = createConnectionFactory();
        try(Connection connection = factory.newConnection(); Channel channel = connection.createChannel()){
            configureRabbitMQ(channel);
            consumeMessages(channel);
            synchronized(this){
                this.wait();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                try{
                    if(channel.isOpen()){
                        channel.close();
                    }
                    if(connection.isOpen()){
                        connection.close();
                    }
                    mqttSender.disconnect();
                }catch(IOException | TimeoutException | MqttException e){
                    e.printStackTrace();
                }
            }));
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}