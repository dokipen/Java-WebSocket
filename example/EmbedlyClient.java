import java.net.URI;
import java.net.URISyntaxException;

import com.embedly.stream.MessageListener;
import com.embedly.stream.EmbedlyMessageSource;


public class EmbedlyClient {
	public static void main(String[] args) throws URISyntaxException {
		if (args.length < 2) {
			System.err.println(
				"Please provide a socket endpoint and subscription command");
			System.err.println("java EmbedlyClient <endpoint> <command>");
			System.exit(1);
		}
		URI uri = new URI(args[0]);
		EmbedlyMessageSource source = new EmbedlyMessageSource();
		source.setUri(uri);
		source.setSubscriptionCommand(args[1]);
		source.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(String message) {
				System.out.println(message);
				System.out.println();
			}
		});
		source.start();
	}

}
