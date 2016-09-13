# vertx3-blocking
Tests for vertx bug #43 - vertx don't instant sends messages while processing handlers!

# Build
`mvn clean package`

# Run tests
- `./run-tests.sh`    # for help
- `./run-tests.sh xx` # run all 21 tests
- `./run-tests.sh 1x` # run all 3 Direct code tests
- `./run-tests.sh 2x` # run all 6 vertx.executeBlocking tests
- `./run-tests.sh 3x` # run all 6 workerExecutor.executeBlocking tests
- `./run-tests.sh 4x` # run all 6 javaExecutor.process tests

# Pre-requirments for running `./run-tests.sh`
- bash/sh shell
- date, grep, fgrep, sed, expr, sort, mv, rm, sleep
- vertx (command line utility installed used for stop verticles)

# Customize tests
Edit variables on run-tests.sh:
 - PING_TIMES (e.g.: 500)
 - PONG_WAIT (e.g.: 50ms)
 - ASYNC (comment or uncomment to be active)

# Tests results:
Using vertx 3.3.2 (with and without `executeAync`).
- Ping sends 500 instant messages to Pong.
- Pong emulates 50ms processing time and answers to Ping.

Each cell tells if the messages are sent instantaneously to Ping after processing.

v Test Type / Pong Verticle Type > | Standard | Worker | MT Worker
---------|----------|---------|-----------
Direct code (No executeBlocking) |  No | No | No
vertx.executeBlocking(ordered) |  **Yes** | From-24 | No`*1`
vertx.executeBlocking(not ordered) | **Yes** | No | From-115`*2`
workerExecutor.executeBloking(ordered) | **Yes** | **Yes** | **Yes**`*1`
workerExecutor.executeBloking(not ordered) | **Yes** | **Yes** | **Yes**`*2`
javaExecutor.process(ordered) | **Yes** | **Yes** | **Yes**`*1`
javaExecutor.process(not ordered) | **Yes** | **Yes** | **Yes**
**Using Async=true**|||
Direct code (No executeBlocking) |  No | No | From-337
vertx.executeBlocking(ordered) |  **Yes** | From-69 | No`*1`
vertx.executeBlocking(not ordered) | **Yes** | From-58 | From-59`*2`
workerExecutor.executeBloking(ordered) | **Yes** | **Yes** | **Yes**`*1`
workerExecutor.executeBloking(not ordered) | **Yes** | **Yes** | **Yes**`*2`
javaExecutor.process(ordered) | **Yes** | **Yes** | **Yes**`*1`
javaExecutor.process(not ordered) | **Yes** | **Yes** | **Yes**`*2`

Notes:
- `*1` Messages are not processed in ordinal order.
- `*2` First 20 messages (thread pool size) processed are not the first 20 sent messages by ping.

# Test environment
- Ubuntu 16.04 LTS
- Java 1.8.0_101-b13
