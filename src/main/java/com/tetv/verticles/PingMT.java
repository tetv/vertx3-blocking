package com.tetv.verticles;

import com.tetv.util.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

public class PingMT extends AbstractVerticle {
    private final static Log LOG = Log.getLogger(PingMT.class);
    private String name = System.getProperty("vertx.id");

    @Override
    public void start() {
        getVertx().deployVerticle(Ping.class.getName(), new DeploymentOptions().setWorker(true).setMultiThreaded(true), event -> {
            if(event.failed()) {
                LOG.error(String.format("%s> Multithread deployment failed", name), event.cause());
            }
        });
    }
}
