package part3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        Flow.Publisher<? extends ByteBuffer> publisher = new Flow.Publisher<ByteBuffer>() {
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
                subscriber.onNext(ByteBuffer.wrap("hello".getBytes(Charset.defaultCharset())));
                subscriber.onComplete();
            }
        };

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://postman-echo.com/post"))
            .headers("Content-Type", "text/plain;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.fromPublisher(publisher))
            .build();

        Flow.Subscriber<? super String> subscriber = new Flow.Subscriber<String>() {
            
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void onNext(String item) {
                System.out.println("Subscriber.onNext: {}"+ item);
                this.subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Subscriber.onError: {}"+ throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("Subscriber.onComplete");
            }
        };
        HttpResponse<Void> response = client.send(request, BodyHandlers.fromLineSubscriber(subscriber));
    }

}
