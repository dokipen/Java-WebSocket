package com.embedly.stream;

import java.io.IOException;

public interface MessageSource {
    public void start();
    public void stop() throws IOException;
    public boolean isRunning();
    public void setMessageListener(MessageListener m);
}
