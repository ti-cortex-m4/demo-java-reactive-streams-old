package demo.reactivestreams.part6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class HttpClientRunner {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientRunner.class);

    public static void main(String[] args) throws URISyntaxException {
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

        Flow.Publisher<ByteBuffer> publisher = new StringPublisher("The quick brown fox jumps over the lazy dog");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://postman-echo.com/post"))
            .headers("Content-Type", "text/plain;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.fromPublisher(publisher))
            .build();

        Flow.Subscriber<List<ByteBuffer>> subscriber = new StringSubscriber();

        CompletableFuture<HttpResponse<Void>> responseFuture = client.sendAsync(request, BodyHandlers.fromSubscriber(subscriber));
        logger.info("request sent");

        HttpResponse<Void> response = responseFuture.join();
        logger.info("response received, status code {}", response.statusCode());
    }
}
