package demo.reactivestreams.part5;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

public class HttpClientRunner {

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

        HttpResponse<Void> response = client.sendAsync(request, BodyHandlers.fromSubscriber(subscriber)).join();
        System.out.println(response.statusCode());
    }
}
