package com.tetv.util;

public class Ball {
    public final String name;
    public final int recvPos;
    public final long pongFinnTime;
    public final long pingRecvTime;

    public Ball(String name, int recvPos, long pongFinnTime, long pingRecvTime) {
        this.name = name;
        this.recvPos = recvPos;
        this.pongFinnTime = pongFinnTime;
        this.pingRecvTime = pingRecvTime;
    }

    public long delayedTime() {
        return Math.max(-1, pingRecvTime - pongFinnTime);
    }

    public String toString() {
        return String.format("%s; pos:%d; delayed: %d (between pong finished and ping received)", name, recvPos, delayedTime());
    }
}
