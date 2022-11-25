package _mongodb;

import com.mongodb.reactivestreams.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ReactiveMongoDbExample {

    public static void main(String[] args) throws Exception {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        int port = Network.getFreeServerPort();
        MongodConfig mongodConfig = MongodConfig.builder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build();

        MongodExecutable mongodExecutable = null;
        try {
            mongodExecutable = starter.prepare(mongodConfig);
            MongodProcess mongod = mongodExecutable.start();

            ConnectionString connString = new ConnectionString("mongodb://localhost:"+ port);
            ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .serverApi(serverApi)
                .build();
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase("test");
            Publisher<Document> document = database.runCommand(new Document("buildInfo", 1));
            document.subscribe(new Subscriber<Document>() {

                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);
                }

                @Override
                public void onNext(Document document) {
                    System.out.println(document.getString("version"));
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
            });

            Thread.sleep(1000);
        } finally {
            if (mongodExecutable != null)
                mongodExecutable.stop();
        }
    }
}
