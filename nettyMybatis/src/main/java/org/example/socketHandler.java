package org.example;

import io.netty.channel.*;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.DataMapper;
import org.example.model.Wind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class socketHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(socketHandler.class);
    private final SqlSessionFactory sqlSessionFactory;
    private MessageSender messageSender;

    public socketHandler(MessageSender messageSender) throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        this.messageSender = messageSender;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.info("Received: {}", msg);
        if (msg.startsWith("$")) {
            msg = msg.substring(1);
        }
        if(messageSender!=null){
            messageSender.SendMessage(msg);
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DataMapper mapper = session.getMapper(DataMapper.class);
            Wind newWind = new Wind();
            newWind.setData(msg);
            mapper.insertData(newWind);
            session.commit();
        } catch (Exception e) {
            logger.warn("Error while saving data to db: {}", e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught", cause);
        ctx.close();
    }
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception{
        super.handlerRemoved(ctx);
        if(messageSender != null){
            messageSender.Close();
        }
    }
}
