package part1;

public class Runner {

    public static void main(String[] args) {
        new SimplePublisher(10).subscribe(new SimpleSubscriber());
    }
}
