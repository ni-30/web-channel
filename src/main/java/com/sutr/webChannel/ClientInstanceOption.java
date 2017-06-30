package com.sutr.webChannel;

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import org.redisson.api.RedissonClient;

/**
 * Created by nitish.aryan on 29/06/17.
 */
public class ClientInstanceOption {
    private String clientId;
    private Vertx vertx;
    private ServerWebSocket serverWebSocket;
    private RedissonClient redissonClient;

    public ClientInstanceOption vertex(Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    public Vertx getVertx() {
        return this.vertx;
    }

    public ClientInstanceOption clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientId() {
        return this.clientId;
    }

    public ClientInstanceOption serverWebSocket(ServerWebSocket serverWebSocket) {
        this.serverWebSocket = serverWebSocket;
        return this;
    }

    public ServerWebSocket getServerWebSocket() {
        return serverWebSocket;
    }

    public ClientInstanceOption redissonClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        return this;
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
