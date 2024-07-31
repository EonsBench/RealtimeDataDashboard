package org.example;

public interface MessageSender {
    void SendMessage(String message) throws Exception;
    void Close() throws Exception;
}
