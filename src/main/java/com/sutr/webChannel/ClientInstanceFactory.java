package com.sutr.webChannel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sutr.webChannel.logger.Logger;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import org.redisson.api.*;

import static com.sutr.webChannel.Constants.CLIENT_ID_HEADER;

/**
 * Created by nitish.aryan on 12/06/17.
 */
public class ClientInstanceFactory {
    private static final Logger LOGGER = Logger.getLogger(ClientInstanceFactory.class.getName());
    private static final ObjectMapper m = new ObjectMapper();

    private final Vertx vertx;
    private final RedissonClient redissonClient;

    public ClientInstanceFactory(Vertx vertx, RedissonClient redissonClient) {
        this.vertx = vertx;
        this.redissonClient = redissonClient;
    }

    public ClientInstance inputOutput(final ServerWebSocket serverWebSocket) {
        final MultiMap headers = serverWebSocket.headers();

        final String clientId = headers.get(CLIENT_ID_HEADER);

        // TODO : validate connection

        serverWebSocket.pause();
        return new ClientInstance(new ClientInstanceOption()
                    .vertex(this.vertx)
                    .clientId(clientId)
                    .redissonClient(redissonClient)
                    .serverWebSocket(serverWebSocket));
    }
}
