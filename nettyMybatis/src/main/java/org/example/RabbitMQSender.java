package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQSender implements MessageSender{
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQSender.class);
    private static final String EXCHANGE_NAME = System.getenv("EXCHANGE_NAME");
    private static final String ROUTING_KEY = System.getenv("ROUTING_KEY");
    private static final String QUEUE_NAME = System.getenv("QUEUE_NAME");
    private final Channel channel;
    private final Connection connection;
    public RabbitMQSender(String host, int port, String username, String password ) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    }
    @Override
    public void SendMessage(String message){
        try{
            channel.basicPublish(EXCHANGE_NAME,ROUTING_KEY,null,message.getBytes());
            logger.info("Sent message Success");
        }catch(Exception e){
            logger.error("Failed to send message", e);
        }
    }
    @Override
    public void Close() throws Exception{
        if(channel != null){
            channel.close();
        }
        if(connection!= null){
            connection.close();
        }
    }
}
