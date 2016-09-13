package com.tetv.verticles;

import com.tetv.util.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.Message;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pong extends AbstractVerticle {
    private final static Log LOG = Log.getLogger(Pong.class);
    private final String name = System.getProperty("vertx.id");
    private final int wait = Integer.parseInt(System.getProperty("wait", "-1"));
    private final String mode = System.getProperty("mode", "default");
    private final boolean ordered = Boolean.parseBoolean(System.getProperty("ordered", "true"));
    private final ExecutorService exec2 = ordered ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(20);
    private WorkerExecutor exec1;

    @Override
    public void start() {
        exec1 = getVertx().createSharedWorkerExecutor("vert.x-new-internal-blocking-thread", 20);

        LOG.info("Ordered:" + ordered);
        LOG.info("Wait:" + wait);

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
                case "exec1":
                    exec1.executeBlocking(future -> {
                        process(message);
                        future.complete();
                    }, ordered, result -> {
                    });
                    break;
                case "exec2":
                    exec2.execute(() -> {
                        process(message);
                    });
                    break;
            }
        }).completionHandler(event -> {
            if (event.failed()) {
                LOG.error(String.format("%s> failed", name), event.cause());
            }
        });
    }

    private void process(Message<Object> message) {
        String ball = (String)message.body();
        LOG.info(String.format("%s> %s recv", name, ball));
        process(wait);
        LOG.info(String.format("%s> %s send", name, ball));
        message.reply(ball.replaceFirst(" .*", ""));
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