package com.tetv.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("WeakerAccess")
public class Log {
    private final Logger log;

    private Log(Class klass) {
        log = LoggerFactory.getLogger(klass);
    }

    public static Log getLogger(Class klass) {
        return new Log(klass);
    }

    public void info(String message) {
        log.info(addDateAndThreadName(message));
    }

    public void error(String message, Throwable throwable) {
        log.error(addDateAndThreadName(message), throwable);
    }

    private String addDateAndThreadName(String message) {
        String threadName = Thread.currentThread().getName();
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
        return String.format("[%s] [%s] %s", date, threadName, message);
    }
}
