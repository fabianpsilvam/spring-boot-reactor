package com.course.first.reactor.app;

import com.course.first.reactor.app.model.Comment;
import com.course.first.reactor.app.model.User;
import com.course.first.reactor.app.model.UserComment;
import java.sql.Time;
import java.time.Duration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @java.lang.Override
    public void run(java.lang.String... args) throws Exception {

        backPressure2();
    }

    public void backPressure2() {

        Flux.range(1, 10)
                .log()
                .limitRate(3)
                .subscribe();
    }

    public void backPressure() {

        Flux.range(1, 10)
                .log()
                .subscribe(new Subscriber<Integer>() {

                    private Subscription subscription;
                    private Integer limit = 2;
                    private Integer consumed = 0;


                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription = s;
                        subscription.request(limit);
                    }

                    @Override
                    public void onNext(Integer i) {
                        log.info(i.toString());
                        consumed++;
                        if (consumed.equals(limit)){
                            consumed = 0;
                            subscription.request(limit);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void intervalInfiniteFromCreate() {
        Flux.create(emitter -> {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        private int count;

                        @Override
                        public void run() {
                            emitter.next(++count);
                            if (count == 3) {
                                timer.cancel();
                                emitter.complete();
                            }
                        }
                    }, 1000, 1000);
                })
                .doOnNext(next -> log.info(next.toString()))
                .doOnComplete(() -> log.info("Hemos terminado"))
                .subscribe();
    }

    public void delayElementsInfinite() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Flux.interval(Duration.ofSeconds(1))
                .doOnTerminate(countDownLatch::countDown)
                .flatMap(i -> {
                    if (i > 3) {
                        return Flux.error(new InterruptedException("Solo hasta 5"));
                    }
                    return Flux.just(i);
                })
                .map(i -> "Hola " + i)
                .retry(2)
                .subscribe(System.out::println, e -> log.error(e.getMessage()));

        countDownLatch.await();
    }

    public void interval() {
        Flux<Integer> range = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(i -> log.info(i.toString()));
        range.blockLast();
    }

    public void zipWithRange() {
        Flux.just(1, 2, 3, 4)
                .map(i -> i * 2)
                .zipWith(Flux.range(0, 4),
                        (uno, dos) -> String.format("Uno %d, dos %d", uno, dos))
                .subscribe(log::info);
    }

    public void zipWithUserWithCommentsV2() {
        Mono<User> monoUser = Mono.fromCallable(() -> new User("Jhob", "Doe"));
        Mono<Comment> monoComment = Mono.fromCallable(() -> {
            var comments = new Comment();
            comments.addComment("My first comment");
            comments.addComment("My second comment");
            return comments;
        });

        var commentsUser = monoUser.zipWith(monoComment)
                .map((tuple -> new UserComment(tuple.getT1(), tuple.getT2())));
        commentsUser.subscribe(uc -> log.info(uc.toString()));
    }

    public void zipWithUserWithComments() {
        Mono<User> monoUser = Mono.fromCallable(() -> new User("Jhob", "Doe"));
        Mono<Comment> monoComment = Mono.fromCallable(() -> {
            var comments = new Comment();
            comments.addComment("My first comment");
            comments.addComment("My second comment");
            return comments;
        });

        monoUser.zipWith(monoComment, UserComment::new)
                .subscribe(uc -> log.info(uc.toString()));
    }

    public void userWithComments() {
        Mono<User> monoUser = Mono.fromCallable(() -> new User("Jhob", "Doe"));
        Mono<Comment> monoComment = Mono.fromCallable(() -> {
            var comments = new Comment();
            comments.addComment("My first comment");
            comments.addComment("My second comment");
            return comments;
        });

        monoUser.flatMap(u -> monoComment.map(c -> new UserComment(u, c)))
                .subscribe(uc -> log.info(uc.toString()));
    }

    public void collectListExample() {
        List<User> users = List.of(new User("Fabian", "Munoz"), new User("Patricio", "Silva"),
                new User("Samuel", "Silva"), new User("Sergio", "Silva"));

        Flux.fromIterable(users).collectList().subscribe(list -> log.info(list.toString()));
    }

    public void toStringExample() {
        List<User> users = List.of(new User("Fabian", "Munoz"), new User("Patricio", "Silva"),
                new User("Samuel", "Silva"), new User("Sergio", "Silva"));

        Flux.fromIterable(users).map(user -> user.getName().concat(user.getLastName()))
                .flatMap(completeName -> {
                    if (completeName.contains("Silva")) {
                        return Mono.just(completeName);
                    } else {
                        return Mono.empty();
                    }
                })
                .subscribe(log::info);
    }

    public void flatMapExample() {
        List<String> users = List.of("Fabian", "Patricio", "Samuel", "Sergio");

        Flux.fromIterable(users).map(name -> new User(name.toUpperCase(), name.toUpperCase())
                ).map(user -> {
                    user.setName(user.getName().toLowerCase());
                    return user;
                })
                .flatMap(user -> {
                    if (user.getName().equalsIgnoreCase("fabian")) {
                        return Mono.just(user);
                    } else {
                        return Mono.empty();
                    }
                })
                .subscribe(user -> log.info(user.getName()));
    }

    public void iterableExample() throws Exception {

        List<String> users = List.of("Fabian", "Patricio", "Samuel", "Sergio");

        Flux<String> namesString = Flux.fromIterable(users);

        Flux<User> names = namesString.map(name -> new User(name.toUpperCase(), name.toUpperCase())
                )
                .doOnNext(user -> {
                    if (user == null) {
                        throw new IllegalArgumentException("Empty name");
                    } else {
                        System.out.println(user.getName());
                    }
                }).map(user -> {
                    user.setName(user.getName().toLowerCase());
                    return user;
                });

        names.subscribe(user -> log.info(user.toString()), error -> log.error(error.getMessage()),
                new Runnable() {
                    @Override
                    public void run() {
                        log.info("Se finalizo la ejecucion del FLUX");
                    }
                });
    }
}
