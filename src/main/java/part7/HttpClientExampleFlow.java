package part7;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

public class HttpClientExampleFlow {

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

        Flow.Publisher<ByteBuffer> publisher = getFlowPublisher();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://postman-echo.com/post"))
            .headers("Content-Type", "text/plain;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.fromPublisher(publisher))
            .build();

        Flow.Subscriber<List<ByteBuffer>> subscriber = getFlowSubscriber();
        HttpResponse<Void> response = client.sendAsync(request, BodyHandlers.fromSubscriber(subscriber)).join();
        System.out.println(response.statusCode());
    }

    private static Flow.Publisher<ByteBuffer> getFlowPublisher() {
        return new Flow.Publisher<>() {
            @Override
            public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                    }
                });
                subscriber.onNext(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
                subscriber.onComplete();
            }
        };
    }

    private static Flow.Subscriber<List<ByteBuffer>> getFlowSubscriber() {
        return new EchoSubscriber();
    }

}
