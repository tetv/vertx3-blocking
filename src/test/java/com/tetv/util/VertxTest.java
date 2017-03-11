package com.tetv.util;

import io.vertx.core.*;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("WeakerAccess")
public class VertxTest {
    private final Vertx vertx;

    private VertxTest(Vertx vertx) {
        this.vertx = vertx;
    }

    public static VertxTest createLocal() throws Exception {
        return new VertxTest(Vertx.vertx());
    }

    public static VertxTest createCluster() throws Exception {
        return new VertxTest(createClusterVertx());
    }

    public VertxTest deploy(Verticle verticle, DeploymentOptions options) throws Exception {
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<AsyncResult<String>> ref = new AtomicReference<>();
        vertx.deployVerticle(verticle, options, result -> {
            ref.set(result);
            semaphore.release();
        });
        semaphore.acquire();
        AsyncResult<String> result = ref.get();
        assertThat(result.cause()).isNull();
        assertThat(result.succeeded()).isTrue();
        //System.out.println("Successfully deployed: " + result.result());
        return this;
    }

    public void close() {
        if(vertx != null) {
            try {
                Semaphore semaphore = new Semaphore(0);
                AtomicReference<AsyncResult<Void>> ref = new AtomicReference<>();
                vertx.close(result -> {
                    ref.set(result);
                    semaphore.release();
                });
                semaphore.acquire();
                AsyncResult<Void> result = ref.get();
                assertThat(result.cause()).isNull();
                assertThat(result.succeeded()).isTrue();
            } catch (InterruptedException ignored) {
            }
        }
    }

    // Helper methods

    private static Vertx createClusterVertx() throws Exception {
        AtomicReference<AsyncResult<Vertx>> ref = new AtomicReference<>();
        Semaphore semaphore = new Semaphore(0);
        Vertx.clusteredVertx(new VertxOptions(), response -> {
            ref.set(response);
            semaphore.release();
        });
        semaphore.acquire();
        AsyncResult<Vertx> response = ref.get();
        assertThat(response.cause()).isNull();
        assertThat(response.succeeded()).isTrue();
        return ref.get().result();
    }
}
