package com.tetv.verticles;

import com.tetv.util.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;

import java.util.Arrays;
import java.util.List;

public class Ping extends AbstractVerticle {
    private final static Log LOG = Log.getLogger(Ping.class);
    private final String name = System.getProperty("vertx.id");
    private final int times = Integer.parseInt(System.getProperty("times", "1"));

    @Override
    public void start() {
        LOG.info("Times:" + times);

        for (int i = 1; i <= times; i++) {
            String ball = String.format("ball%03d", i);
            LOG.info(String.format("%s> %s send", name, ball));
            getVertx().eventBus().send("table", ball, response -> {
                    if (response.succeeded()) {
                    LOG.info(String.format("%s> %s recv", name, response.result().body()));
                } else {
                    LOG.error(String.format("%s> failed", name), response.cause());
                }
            });
        }
    }

    public static void main(String... args) {
        List<String> myArgs = Arrays.asList(args);
        myArgs.add(0, "run");
        myArgs.add(1, Ping.class.getName());
        Launcher.main(myArgs.toArray(new String[myArgs.size()]));
    }
}
