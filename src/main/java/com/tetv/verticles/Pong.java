package com.tetv.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pong extends AbstractVerticle {
    private final static Logger LOG = LoggerFactory.getLogger(Pong.class);
    private final String name = System.getProperty("vertx.id", "pong");
    private final int wait;
    private final String mode;
    private final boolean ordered;
    private final ExecutorService javaExecutor;
    private WorkerExecutor workerExecutor;
    final Map<String, Long> finishedProcessedTimeMap = new ConcurrentHashMap<>();

    public Pong() {
        this.wait = Integer.parseInt(System.getProperty("wait", "-1"));
        this.mode = System.getProperty("mode", "default");
        this.ordered = Boolean.parseBoolean(System.getProperty("ordered", "true"));
        this.javaExecutor = ordered ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(20);
    }

    public Pong(int wait, String mode, boolean ordered) {
        this.wait = wait;
        this.mode = mode;
        this.ordered = ordered;
        this.javaExecutor = ordered ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(20);
    }

    @Override
    public void start() {
        workerExecutor = getVertx().createSharedWorkerExecutor("vert.x-new-internal-blocking-thread", 20);

        LOG.info("Ordered: {}", ordered);
        LOG.info("Wait: {}", wait);

        getVertx().eventBus().consumer("table", message -> {
            switch (mode) {
                case "default":
                    process(message);
                    break;
                case "block":
                    getVertx().executeBlocking(future -> {
                        process(message);
                        future.complete();
                    }, ordered, result -> {
                    });
                    break;
                case "wexec":
                    workerExecutor.executeBlocking(future -> {
                        process(message);
                        future.complete();
                    }, ordered, result -> {
                    });
                    break;
                case "jexec":
                    javaExecutor.execute(() -> process(message));
                    break;
            }
        }).completionHandler(event -> {
            if (event.failed()) {
                LOG.error("{}> failed", name, event.cause());
            }
        });
    }

    private void process(Message<Object> message) {
        String ball = (String)message.body();
        LOG.info("{}> {} recv [{}]", name, ball, LocalTime.now());
        process(wait);
        LOG.info("{}> {} send [{}]", name, ball, LocalTime.now());
        finishedProcessedTimeMap.put(ball, System.currentTimeMillis());
        message.reply(ball);
    }

    private static void process(long millis) {
        if(millis >= 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void main(String[] args) {
        List<String> myArgs = Arrays.asList(args);
        myArgs.add(0, "run");
        myArgs.add(1, Pong.class.getName());
        Launcher.main(myArgs.toArray(new String[myArgs.size()]));
    }
}
