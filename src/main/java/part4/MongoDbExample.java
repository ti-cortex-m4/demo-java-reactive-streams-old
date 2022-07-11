package part4;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.internal.MongoClientImpl;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Flow;

public class MongoDbExample {

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

            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:"+ port)) {
                MongoDatabase database = mongoClient.getDatabase("test");
                Document document = database.runCommand(new Document("buildInfo", 1));
                System.out.println(document.getString("version"));
            }

        } finally {
            if (mongodExecutable != null)
                mongodExecutable.stop();
        }
    }
}
