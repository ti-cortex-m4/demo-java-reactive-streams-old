package part3;

import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Flow;

public class HttpClientExample {

    public static void main(String[] args) throws Exception {
        httpPostRequest2();
    }

    public static void httpPostRequest1() throws URISyntaxException, IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://postman-echo.com/post"))
            .headers("Content-Type", "text/plain;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString("Sample request body"))
            .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String responseBody = response.body();
        System.out.println("httpPostRequest : " + responseBody);
    }

    public static void httpPostRequest2() throws URISyntaxException, IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

        Flux<ByteBuffer> flux = Flux.just(ByteBuffer.wrap("hello\n".getBytes(Charset.defaultCharset())));
        Flow.Publisher<ByteBuffer> publisher = JdkFlowAdapter.publisherToFlowPublisher(flux);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://postman-echo.com/post"))
            .headers("Content-Type", "text/plain;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.fromPublisher(publisher))
            .build();

        Flow.Subscriber<List<ByteBuffer>> subscriber = new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(List<ByteBuffer> buffers) {
                for (ByteBuffer buffer : buffers) {
                    System.out.println("onNext: " + Charset.defaultCharset().decode(buffer));
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError: " + throwable);
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }
        };
        HttpResponse<Void> response = client.sendAsync(request, BodyHandlers.fromSubscriber(subscriber)).join();
        System.out.println(response.statusCode());
    }
}
