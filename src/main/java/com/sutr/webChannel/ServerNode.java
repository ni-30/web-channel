package com.sutr.webChannel;

import com.sutr.webChannel.logger.Logger;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by nitish.aryan on 12/06/17.
 */

public class ServerNode {
    private static final Logger logger = Logger.getLogger(ServerNode.class.getName());

    protected final String uid;
    protected final Vertx vertx;
    protected final RedissonClient redissonClient;
    protected final HttpServer server;
    protected final ClientInstanceFactory clientInstanceFactory;

    public ServerNode(final String uid, final Vertx vertx) {
        this.uid = uid;
        this.vertx = vertx;

        Config config = new Config();
        config.useSingleServer().setAddress("localhost:6379");
        this.redissonClient = Redisson.create(config);

        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setHost("localhost")
                .setPort(8030)
                .setIdleTimeout(10000)
                .setSsl(false);

        this.server = vertx.createHttpServer(httpServerOptions);
        this.clientInstanceFactory = new ClientInstanceFactory(this.vertx, this.redissonClient);

        this.vertx.deployVerticle(new SubscriberVerticle(this.redissonClient), new DeploymentOptions()
                .setWorker(true)
                .setInstances(512)
                .setMultiThreaded(true));

        this.vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                this.vertx.eventBus().consumer("channel.notification.publish", message -> {

                    final RQueue<Message> mainQ = redissonClient.getQueue("channelQueue:" + String.valueOf(message.body()));

                    if (mainQ.isEmpty()) {
                        return;
                    }

                    final Message msg = mainQ.poll();
                    if(msg == null) return;

                    RList<String> channelSubscribersList = redissonClient.getList("channelSubscribers:" + msg.getMetadata().getChannel());
                    if(!channelSubscribersList.isExists() || channelSubscribersList.isEmpty()) {
                        return;
                    }

                    redissonClient.getBucket("message:" + msg.getMetadata().getId()).set(msg, 60, TimeUnit.DAYS);
                    final RAtomicLong rCounter = redissonClient.getAtomicLong("messagePushCounter:" + msg.getMetadata().getId());
                    rCounter.set(0);
                    rCounter.expire(60, TimeUnit.DAYS);

                    Iterator<String> iterator = channelSubscribersList.iterator();
                    while (iterator.hasNext()) {
                        final String[] subscriber = iterator.next().split(":");
                        if(subscriber.length != 2) {
                            return;
                        }
                        switch (subscriber[0]) {
                            case "client":
                                if(subscriber[1].equals(msg.getMetadata().getClientId())) {
                                    continue;
                                }
                                vertx.runOnContext(aVoid -> {
                                    rCounter.incrementAndGet();
                                    redissonClient.getQueue("clientQueue:" + subscriber[1]).add(msg.getMetadata().getId());
                                    vertx.eventBus().send("client." + subscriber[1] + ".command", "pushNext");
                                });
                                break;
                            case "processor":
                                vertx.runOnContext(aVoid -> {
                                    rCounter.incrementAndGet();
                                    redissonClient.getQueue("processorQueue:" + subscriber[1]).add(msg.getMetadata().getId());
                                    vertx.eventBus().send("processor." + subscriber[1] + ".command", "processNext");
                                });
                                break;
                            default:
                                break;
                        }
                    }
                });
            }

            @Override
            public void stop() throws Exception {

            }
        }, new DeploymentOptions()
                .setWorker(true)
                .setInstances(1024)
                .setMultiThreaded(true));

        this.server.websocketHandler(serverWebSocket -> {
            final ClientInstance bot = ServerNode.this.clientInstanceFactory.inputOutput(serverWebSocket);
            if(bot != null) {
                bot.deploy();
            }
        });
    }

    public void start() {
        logger.info("Starting ...");
        this.redissonClient.getBucket("channel");
        this.server.listen(httpServerAsyncResult -> {
            if(httpServerAsyncResult.succeeded()) {
                logger.info("ServerNode started listening");
            } else {
                logger.error("Failed to start", httpServerAsyncResult.cause());
                vertx.close();
                return;
            }
        });
    }

    public void shutdown() {
        this.vertx.close(voidAsyncResult ->  {
            if(voidAsyncResult.succeeded()) {
                logger.info("server shutdown initiated");
                redissonClient.shutdown();
                logger.info("server shutdown completed");
            } else {
                logger.log("failed to shutdown server due to - {}", voidAsyncResult.cause());
            }
        });
    }
}
