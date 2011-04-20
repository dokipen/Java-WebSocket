package com.embedly.stream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tootallnate.websocket.WebSocketClient;

public class EmbedlyMessageSource implements MessageSource {
	
	public static final Charset CHARSET = Charset.forName("UTF-8");
	public static final String MSG_FRAME = "~m~";
	public static final String HB_FRAME = "~h~";
	public static final String JSON_FRAME = "~j~";
	public static final Pattern FRAME_PATTERN = 
		Pattern.compile("^"+MSG_FRAME+"(\\d+)"+MSG_FRAME+"((.+|\n)+)");

    private MessageListener listener;
    private boolean running;
    private WebSocketClient ws;
    private URI uri;
    private String filterCommand;
	private String subscriptionCommand = "listen simple 1";
    
    public EmbedlyMessageSource() {
    	try {
    		this.uri = new URI("http://localhost:8000/socket.io/websocket");
    	} catch(URISyntaxException e) {
    		// should never happen
    		throw new RuntimeException("Unexpected error", e);
    	}
    }
    
    public EmbedlyMessageSource(URI uri) {
    	this.uri = uri;
    }
    
    public EmbedlyMessageSource(URI uri, final String subscriptionCommand) {
    	this.uri = uri;
    	this.subscriptionCommand = subscriptionCommand;
    }

    public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}
	
    public String getSubscriptionCommand() {
		return subscriptionCommand;
	}

	public void setSubscriptionCommand(String subscriptionCommand) {
		this.subscriptionCommand = subscriptionCommand;
	}

	public String getFilterCommand() {
		return filterCommand;
	}

	public void setFilterCommand(String filterCommand) {
		this.filterCommand = filterCommand;
	}

	public WebSocketClient getWebSocketClient() {
    	if (ws == null) {
	    	ws = new WebSocketClient(uri) {
				
				@Override
				public void onOpen() {
					try {
						running = true;
						if (filterCommand != null) {
							ws.send(filterCommand);
						}
						ws.send(encode(subscriptionCommand));
					} catch(IOException e) {
						throw new RuntimeException("Failed handshake", e);
					}
				}
				
				@Override
				public void onMessage(String message) {
					if (message == null) return;
					String decoded = decode(message);
					if (decoded.startsWith(HB_FRAME)) {
						try {
							ws.send(message);
						} catch(IOException e) {
							throw new RuntimeException(
									"Failed to send heartbeat", e);
						}
					}
					else if (null != listener) {
						// Strip JSON frame
						if (decoded.startsWith(JSON_FRAME)) {
							listener.onMessage(decoded.substring(3));
						} else {
							listener.onMessage(decoded);
						}
					}
				}
				
				@Override
				public void onClose() {
					running = false;
				}
			};
	    }
    	return ws;
    }

    public void start() {
    	getWebSocketClient().connect();
    }

    public void stop() throws IOException {
    	getWebSocketClient().close();
    }

    public boolean isRunning() {
        return running;
    }

    public void setMessageListener(MessageListener m) {
        listener = m;
    }
    
    /**
     * Appends ~f~<size>~f~ to message.
     * 
     * @param message
     * @return
     */
    private String encode(String message) {
    	StringBuffer encoded = new StringBuffer();
    	encoded.append(MSG_FRAME);
    	encoded.append(message.getBytes(CHARSET).length);
    	encoded.append(MSG_FRAME);
    	encoded.append(message);
    	return encoded.toString();
    }
    
    /**
     * Strips frame.  Can't handle null.
     * 
     * @param message
     * @return
     */
    private String decode(String message) {
    	// TODO: check frame size for transport errors
    	Matcher matcher = FRAME_PATTERN.matcher(message);
    	if (matcher.matches()) {
    		return matcher.group(2);
    	} else {
    		return message;
    	}
    }
}
