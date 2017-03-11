package com.tetv.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Launcher;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Ping extends AbstractVerticle {
    private final static Logger LOG = LoggerFactory.getLogger(Ping.class);
    private final String name = System.getProperty("vertx.id", "ping");
    private final AtomicInteger lastReceivedPos = new AtomicInteger();
    private final int times;
    final Map<String, Long> receivedTimeMap = new ConcurrentHashMap<>();
    final Map<String, Integer> receivedPosMap = new ConcurrentHashMap<>();
    long finishSendingTime = -1;

    public Ping() {
        this.times = Integer.parseInt(System.getProperty("times", "1"));
    }

    public Ping(int times) {
        this.times = times;
    }

    @Override
    public void start() {
        LOG.info("Times: {}", times);

        for (int i = 1; i <= times; i++) {
            String ball = String.format("ball%03d", i);
            LOG.info("{}> {} send [{}]", name, ball, LocalTime.now());
            getVertx().eventBus().send("table", ball, (AsyncResult<Message<String>> response) -> {
                if (response.succeeded()) {
                    String receivedBall = response.result().body();
                    receivedPosMap.put(receivedBall, lastReceivedPos.incrementAndGet());
                    receivedTimeMap.put(receivedBall, System.currentTimeMillis());
                    LOG.info("{}> {} recv [{}]", name, receivedBall, LocalTime.now());
                } else {
                    LOG.error("{}> failed", name, response.cause());
                }
            });
        }
        finishSendingTime = System.currentTimeMillis();
    }

    public static void main(String... args) {
        List<String> myArgs = Arrays.asList(args);
        myArgs.add(0, "run");
        myArgs.add(1, Ping.class.getName());
        Launcher.main(myArgs.toArray(new String[myArgs.size()]));
    }
}
