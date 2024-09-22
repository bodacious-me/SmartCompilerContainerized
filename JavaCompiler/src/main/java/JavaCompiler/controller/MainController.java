package JavaCompiler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping; 

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import JavaCompiler.model.JavaModel;
import JavaCompiler.service.AllServices;
import reactor.core.publisher.Flux;

@RestController
public class MainController {

    @Autowired
    AllServices service;

    

    @PostMapping("/")
    public void JavaCompiler(@RequestBody JavaModel model) throws IOException {
        service.run(model);
        service.cleaner(model);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getEvents() throws IOException{
        AtomicInteger counter = new AtomicInteger(1);
        @SuppressWarnings("static-access")
        Stream<String> lines = service.ComError;

         return Flux.fromStream(lines)
                .map(line -> ServerSentEvent.<String> builder()
                        .id(String.valueOf(counter.getAndIncrement()))
                        .data(line)
                        .event("lineEvent")
                        .retry(Duration.ofMillis(1000))
                        .build())
                .delayElements(Duration.ofMillis(300));
    }
}
