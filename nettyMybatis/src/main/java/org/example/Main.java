package org.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String HOST = System.getenv("HOST");
    private static final int PORT = Integer.parseInt(System.getenv("PORT"));
    private static final int MAX_RETRY = 10;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int IDLE_STATE_READER_IDLE_TIME = 60;
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
    private static final int RABBITMQ_PORT = Integer.parseInt(System.getenv("RABBITMQ_PORT"));
    private static final String RABBITMQ_USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            connect(workerGroup, 0);
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void connect(EventLoopGroup eventGroup, int retryCount) throws InterruptedException {
        if (retryCount > MAX_RETRY) {
            logger.error("Too many retry connection attempts");
            return;
        }
        Bootstrap bootstrap = createBootstrap(eventGroup, retryCount);
        ChannelFuture future = bootstrap.connect(HOST, PORT);
        addConnectionListener(future, eventGroup, retryCount);
        future.channel().closeFuture().sync();
    }

    private static Bootstrap createBootstrap(EventLoopGroup eventGroup, int retryCount) {
        return new Bootstrap()
                .group(eventGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .option(ChannelOption.SO_LINGER, 0)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast(new IdleStateHandler(IDLE_STATE_READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new ConnectionWatchdog(eventGroup, MAX_RETRY));
                        pipeline.addLast(new socketHandler(new RabbitMQSender(RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USERNAME, RABBITMQ_PASSWORD)));
                    }
                });
    }

    private static void addConnectionListener(ChannelFuture future, EventLoopGroup eventGroup, int retryCount) {
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                logger.info("Connected to server");
            } else {
                logger.warn("Failed to connect. Retrying...");
                f.channel().eventLoop().schedule(() -> {
                    try {
                        connect(eventGroup, retryCount + 1);
                    } catch (InterruptedException e) {
                        logger.error("Retry interrupted", e);
                    }
                }, 60, TimeUnit.SECONDS);
            }
        });
    }
}
