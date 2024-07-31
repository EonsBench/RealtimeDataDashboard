package org.example;

import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;
public class ConnectionWatchdog extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionWatchdog.class);
    private final EventLoopGroup eventGroup;
    private final int maxRetries;
    private int currentRetry;

    public ConnectionWatchdog(EventLoopGroup eventGroup, int maxRetries) {
        this.eventGroup = eventGroup;
        this.maxRetries = maxRetries;
        this.currentRetry = 0;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                logger.warn("No message received for a while. Reconnecting...");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("Channel inactive. Attempting to reconnect...");
        if (currentRetry < maxRetries) {
            currentRetry++;
            logger.info("Reconnecting attempt {}/{}", currentRetry, maxRetries);
            reconnect();
        } else {
            logger.error("Max retries reached. Giving up on reconnecting.");
        }
    }

    private void reconnect() {
        eventGroup.schedule(() -> {
            try {
                Main.connect(eventGroup, currentRetry);
            } catch (Exception e) {
                logger.error("Reconnect attempt failed: ", e);
            }
        }, calculateRetryDelay(), TimeUnit.SECONDS);
    }

    private int calculateRetryDelay() {
        return Math.min(300, 10 << currentRetry);
    }
}
