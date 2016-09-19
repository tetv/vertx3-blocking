package com.tetv.verticles;

import com.tetv.util.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Launcher;
import io.vertx.core.eventbus.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class Ping extends AbstractVerticle {
    private final static Log LOG = Log.getLogger(Ping.class);
    private final String name = System.getProperty("vertx.id", "ping");
    private final Collection<String> receivedBalls = new ConcurrentSkipListSet<>();
    private final int times;
    private long finishSendingTime = -1;

    public Ping() {
        this.times = Integer.parseInt(System.getProperty("times", "1"));
    }

    public Ping(int times) {
        this.times = times;
    }

    Collection<String> receivedBalls() {
        return receivedBalls;
    }

    long getFinishedSentTime() {
        return finishSendingTime;
    }

    @Override
    public void start() {
        LOG.info("Times:" + times);

        for (int i = 1; i <= times; i++) {
            String ball = String.format("ball%03d", i);
            LOG.info(String.format("%s> %s send", name, ball));
            getVertx().eventBus().send("table", ball, (AsyncResult<Message<String>> response) -> {
                if (response.succeeded()) {
                    String receivedBall = response.result().body();
                    LOG.info(String.format("%s> %s recv", name, receivedBall));
                    receivedBalls.add(receivedBall);
                } else {
                    LOG.error(String.format("%s> failed", name), response.cause());
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
