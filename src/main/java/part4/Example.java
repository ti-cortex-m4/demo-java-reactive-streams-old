package part4;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import reactivestreams.helpers.SubscriberHelpers.*;

public class Example {

    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = MongoClients.create("mongodb://hostOne:27017");
        MongoDatabase database = mongoClient.getDatabase("mydb");
        MongoCollection<Document> collection = database.getCollection("test");

        Document doc = new Document("name", "MongoDB")
            .append("type", "database")
            .append("count", 1)
            .append("versions", Arrays.asList("v3.2", "v3.0", "v2.6"))
            .append("info", new Document("x", 203).append("y", 102));

        collection.insertOne(doc).subscribe(new OperationSubscriber<InsertOneResult>());

        Publisher<InsertOneResult> publisher = collection.insertOne(doc);
        publisher.subscribe(new Subscriber<InsertOneResult>() {
            @Override
            public void onSubscribe(final Subscription s) {
                s.request(1);  // <--- Data requested and the insertion will now occur
            }

            @Override
            public void onNext(final InsertOneResult result) {
                System.out.println("Inserted: " + result);
            }

            @Override
            public void onError(final Throwable t) {
                System.out.println("Failed");
            }

            @Override
            public void onComplete() {
                System.out.println("Completed");
            }
        });

        List<Document> documents = new ArrayList<Document>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents).subscribe(new ObservableSubscriber<InsertManyResult>());

        collection.count()
            .subscribe(new PrintSubscriber<Long>("total # of documents after inserting "
                + " 100 small ones (should be 101): %s"));

        collection.find().first().subscribe(new PrintDocumentSubscriber());
    }
}
