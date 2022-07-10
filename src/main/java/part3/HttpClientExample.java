package part3;

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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpClientExample {

    public static void main(String[] args) throws Exception {
        httpPostRequest();
    }

    public static void httpPostRequest() throws URISyntaxException, IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://jsonplaceholder.typicode.com/posts"))
            .version(HttpClient.Version.HTTP_2)
            .POST(BodyPublishers.ofString("Sample Post Request"))
            .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String responseBody = response.body();
        System.out.println("httpPostRequest : " + responseBody);
    }

}
