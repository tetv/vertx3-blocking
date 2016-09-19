package com.tetv.verticles;

public class AsyncModeTest extends DefaultModeTest {
    static {
        System.setProperty("vertx.hazelcast.async-api", "true");
    }
}