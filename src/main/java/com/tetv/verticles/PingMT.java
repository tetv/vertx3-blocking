package com.tetv.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingMT extends AbstractVerticle {
    private final static Logger LOG = LoggerFactory.getLogger(PingMT.class);
    private String name = System.getProperty("vertx.id");

    @Override
    public void start() {
        getVertx().deployVerticle(Ping.class.getName(), new DeploymentOptions().setWorker(true).setMultiThreaded(true), event -> {
            if(event.failed()) {
                LOG.error("{}> Multithread deployment failed", name, event.cause());
            }
        });
    }
}
