package part5;

import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.time.Duration;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

public class IntegrationApp {

    public static void main(String[] args) {
        Publisher<Long> reactorPublisher = reactorPublisher();

//        Processor<Long, Long> akkaStreamsProcessor = akkaStreamsProcessor();
//        reactorPublisher.subscribe(akkaStreamsProcessor);
//
//        Flowable
//            .fromPublisher(FlowAdapters.toProcessor(akkaStreamsProcessor))
//            .subscribe(System.out::println);
    }

    private static Publisher<Long> reactorPublisher() {
        Flux<Long> numberFlux = Flux.interval(Duration.ofSeconds(1));
        return JdkFlowAdapter.publisherToFlowPublisher(numberFlux);
    }
/*
    private static Processor<Long, Long> akkaStreamsProcessor() {
        Flow<Long, Long, NotUsed> negatingFlow = Flow.of(Long.class).map(i -> -i);
        return JavaFlowSupport.Flow.toProcessor(negatingFlow).run(materializer);
    }

    private static ActorSystem actorSystem = ActorSystem.create();
    private static ActorMaterializer materializer = ActorMaterializer.create(actorSystem);
 */
}
