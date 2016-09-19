# vertx3-blocking
Tests for vertx bug that doesn't send instantaneously messages while processing handlers!
- https://github.com/vert-x3/vertx-hazelcast/issues/43 - (clustured vertx)!
- https://github.com/eclipse/vert.x/issues/1631 (non clustered vertx)!

# Background

It seems that `vertx` in some cases doesn't send messages instantaneously.
That means while a `vertx` is very busy/active there is no messages dispatched to eventBus until the `vertx` becomes completely idle, that could be a big limitation if there is a need to send messages immediately.

To illustrate the issues, I created a simple ping pong example with junit tests.
- Ping sends several messages to pong;
- Pong emulates a 50ms processing time and reply back the message to ping.
- Ping, in some cases, doesn't receive immediately the message after pong has processed it.

The tests includes two workarounds (while the fix is not available) that solves the issue:
- using `WorkerExecutor` (available since vertx 3.3.x) - [Vertx documentation](http://vertx.io/docs/vertx-core/java/#blocking_code)
- using java `ExecutorService`

# Non-clustured vertx:

The issue happens when using `vertx.executeBlocking` (only for non ordered) or when processing default code in a `Multi-Thread Worker`.

v Test Type / Pong Verticle Type > | Standard | Worker | MT Worker
---------|----------|---------|-----------
Direct code (No executeBlocking) |  :heavy_check_mark: | :heavy_check_mark: | :heavy_multiplication_x:`≥ 40`
vertx.executeBlocking(ordered) |  :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
vertx.executeBlocking(not ordered) | :heavy_multiplication_x:`≥ 40` | :heavy_multiplication_x:`≥ 40` | :heavy_multiplication_x:`≥ 40`
workerExecutor.executeBloking(ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
workerExecutor.executeBloking(not ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
javaExecutor.process(ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
javaExecutor.process(not ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:

# Clustured vertx:

The issue happens when using `vert.executeBlocking` in a worker verticle and when processing default code in a verticle (independently of the verticle type).

This issue could be connected with this issue: https://github.com/vert-x3/issues/issues/75

v Test Type / Pong Verticle Type > | Standard | Worker | MT Worker
---------|----------|---------|-----------
Direct code (No executeBlocking) |  :heavy_multiplication_x:`≥ 2` | :heavy_multiplication_x:`≥ 2` | :heavy_multiplication_x:`≥ 40`
vertx.executeBlocking(ordered) |  :heavy_check_mark: | :heavy_multiplication_x:`≥ 2` | :heavy_multiplication_x:`≥ 2`
vertx.executeBlocking(not ordered) | :heavy_check_mark: | :heavy_multiplication_x:`≥ 40` | :heavy_multiplication_x:`≥ 40`
workerExecutor.executeBloking(ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
workerExecutor.executeBloking(not ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
javaExecutor.process(ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
javaExecutor.process(not ordered) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:

**Note:** The `-Dvertx.hazelcast.async-api=true` doesn't influence the result.

# Build
`mvn clean package`

# Tests
`mvn clean test`

# Test environment
- Ubuntu 16.04 LTS
- Java 1.8.0_101-b13
