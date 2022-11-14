package part3;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.reactivestreams.FlowAdapters;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
        return new Flow.Publisher<ByteBuffer>() {
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
        return new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(List<ByteBuffer> buffers) {
                for (ByteBuffer buffer : buffers) {
                    System.out.println("onNext: " + StandardCharsets.UTF_8.decode(buffer));
                }
//                subscription.request(1);
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
    }
}
