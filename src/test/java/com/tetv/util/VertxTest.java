package com.tetv.util;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.powermock.reflect.Whitebox;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@SuppressWarnings("WeakerAccess")
public class VertxTest {
    private final Vertx vertx;
    private final static boolean USE_CLUSTER_MANAGER = false; // Slower but it's independent from other potencial running verticles
    private final static Config CONFIG = new Config().setNetworkConfig(new NetworkConfig().setPublicAddress("127.0.0.1").setPort(15701));

    private VertxTest(Vertx vertx) {
        this.vertx = vertx;
        if(vertx != null) {
            Whitebox.setInternalState(vertx, spy(vertx.eventBus()));
        }
    }

    public static VertxTest createNull() {
        return new VertxTest(null);
    }

    public static VertxTest createLocal(Verticle... verticles) throws Exception {
        return new VertxTest(Vertx.vertx()).deploy(verticles);
    }

    public static VertxTest createCluster() throws Exception {
        return new VertxTest(createClusterVertx()).deploy(new AbstractVerticle() {});
    }

    public static VertxTest createCluster(Verticle... verticles) throws Exception {
        return new VertxTest(createClusterVertx()).deploy(verticles);
    }

    public EventBus eventBus() {
        return vertx.eventBus();
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
        System.out.println("Successfully deployed: " + result.result());
        return this;
    }

    public VertxTest deploy(Verticle... verticles) throws Exception {
        for(Verticle verticle : verticles) {
            Semaphore semaphore = new Semaphore(0);
            AtomicReference<AsyncResult<String>> ref = new AtomicReference<>();
            vertx.deployVerticle(verticle, result -> {
                ref.set(result);
                semaphore.release();
            });
            semaphore.acquire();
            AsyncResult<String> result = ref.get();
            assertThat(result.cause()).isNull();
            assertThat(result.succeeded()).isTrue();
            System.out.println("Successfully deployed: " + result.result());
        }
        return this;
    }

    public <REQ,RES> RES send(String address, REQ request) throws Exception {
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<AsyncResult<Message<RES>>> ref = new AtomicReference<>();
        vertx.eventBus().send(address, request, (AsyncResult<Message<RES>> response) -> {
            ref.set(response);
            semaphore.release();
        });
        semaphore.acquire();
        AsyncResult<Message<RES>> response = ref.get();
        assertThat(response.cause()).isNull();
        assertThat(response.succeeded()).isTrue();
        return response.result().body();
    }

    // Helper methods

    @SuppressWarnings("ConstantConditions")
    private static VertxOptions getVertxOptions() {
        return USE_CLUSTER_MANAGER ? new VertxOptions().setClusterManager(new HazelcastClusterManager(CONFIG)) : new VertxOptions();
    }

    private static Vertx createClusterVertx() throws Exception {
        AtomicReference<AsyncResult<Vertx>> ref = new AtomicReference<>();
        Semaphore semaphore = new Semaphore(0);
        Vertx.clusteredVertx(getVertxOptions(), response -> {
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
