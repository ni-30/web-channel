package com.sutr.webChannel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sutr.webChannel.logger.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.buffer.Buffer;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RQueue;
import java.io.IOException;

/**
 * Created by nitish.aryan on 13/06/17.
 */

public class ClientInstance extends AbstractVerticle {
    private static final Logger LOGGER = Logger.getLogger(ClientInstanceFactory.class.getName());
    private static final ObjectMapper m = new ObjectMapper();

    private final ClientInstanceOption clientInstanceOption;

    public ClientInstance(ClientInstanceOption clientInstanceOption) {
        this.clientInstanceOption = clientInstanceOption;
    }

    protected void loadHandlers() {
        clientInstanceOption.getServerWebSocket().setWriteQueueMaxSize(8);
        clientInstanceOption.getServerWebSocket().closeHandler(aVoid -> {
            clientInstanceOption.getRedissonClient()
                    .getBucket("clientStatus:" + clientInstanceOption.getClientId())
                    .set("inactive");

            this.vertx.eventBus().send(getCommandTopic(), "kill");

            LOGGER.log("websocket is closed for clientId : {}", clientInstanceOption.getClientId());
        });
        clientInstanceOption.getServerWebSocket().drainHandler(aVoid -> {
            LOGGER.info("queue is ready to accept buffer again for clientId : {}", clientInstanceOption.getClientId());
        });
        clientInstanceOption.getServerWebSocket().endHandler(aVoid -> {
            LOGGER.info("no more data to read in read stream for clientId : {}", clientInstanceOption.getClientId());
        });
        clientInstanceOption.getServerWebSocket().exceptionHandler(throwable -> {
            LOGGER.error("exception in read stream for clientId - " + clientInstanceOption.getClientId(), throwable.getCause());
        });
        clientInstanceOption.getServerWebSocket().frameHandler(webSocketFrame -> {
            LOGGER.info("received frame for clientId : {}", clientInstanceOption.getClientId());
        });

        clientInstanceOption.getServerWebSocket().handler(buffer -> {
            Message message;
            try {
                message = m.readValue(buffer.toString(), Message.class);
                LOGGER.info("received buffer message for $clientId:" + clientInstanceOption.getClientId() + ", $message:" + buffer.toString());
                if (message.getMetadata() == null
                        || message.getMetadata().getId() == null
                        || message.getMetadata().getChannel() == null) {
                    return;
                }
            } catch (IOException e) {
                LOGGER.error("failed to convert buffer bytes to Message.class for $clientId:" + clientInstanceOption.getClientId(), e);
                return;
            }

            LOGGER.info("consumed message for $clientId:" + clientInstanceOption.getClientId() + " and $sessionId:" + message.getMetadata().getSessionId() + ", $messageId:" + message.getMetadata().getId());

            message.getMetadata().setClientId(clientInstanceOption.getClientId());
            message.getMetadata().setId(message.getMetadata().getId() + "." + Utils.getUID());

            boolean isAdded = clientInstanceOption.getRedissonClient()
                    .getQueue("channelQueue:" + message.getMetadata().getChannel())
                    .add(message);

            if(isAdded) {
                this.clientInstanceOption.getVertx()
                        .eventBus()
                        .send("channel.notification.publish", message.getMetadata().getChannel());
            }
        });
    }

    public String getCommandTopic() {
        return "client." + clientInstanceOption.getClientId() + ".command";
    }

    public void deploy() {
        this.clientInstanceOption.getVertx().deployVerticle(this, new DeploymentOptions()
                .setWorker(true)
                .setInstances(1)
                .setMultiThreaded(false));
    }

    protected int pushNext() {
        if(!"active".equals(clientInstanceOption.getRedissonClient()
                .getBucket("clientStatus:" + clientInstanceOption.getClientId())
                .get().toString())) {
            return 1;
        }

        final RQueue<String> mainQ = clientInstanceOption.getRedissonClient()
                .getQueue("clientQueue:" + this.clientInstanceOption.getClientId());

        if (mainQ.isEmpty()) {
            return 2;
        }

        final RQueue<String> backupQ = clientInstanceOption.getRedissonClient()
                .getQueue("clientQueue:backup_" + this.clientInstanceOption.getClientId());

        final String msgId = mainQ.pollLastAndOfferFirstTo("clientQueue:backup_" + this.clientInstanceOption.getClientId());
        final RBucket<Message> messageBucket = clientInstanceOption.getRedissonClient().getBucket("message:" + msgId);

        boolean removeMsg = true;
        try {
            if (!messageBucket.isExists()) {
                return 3;
            }

            Message message = messageBucket.get();
            if (message == null) {
                return 4;
            }

            String messageStr;
            try {
                messageStr = m.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                return 5;
            }

            clientInstanceOption.getServerWebSocket().write(Buffer.buffer(messageStr));
            return 0;
        } catch (Exception e) {
            removeMsg = false;
            return 6;
        } finally {
            if(removeMsg) {
                backupQ.remove(msgId);
                RAtomicLong rCounter = clientInstanceOption.getRedissonClient().getAtomicLong("messagePushCounter:" + msgId);
                if (rCounter.decrementAndGet() <= 0) {
                    messageBucket.delete();
                    rCounter.delete();
                }
            }
        }
    }

    public void start() throws Exception {
        this.vertx.eventBus().consumer(getCommandTopic(), message -> {
            switch (message.body().toString()) {
                case "pushNext":
                    pushNext();
                    break;
                case "kill":
                    if(this.deploymentID() != null) {
                        this.vertx.undeploy(this.deploymentID());
                    }
                    break;
                default:
                    break;
            }
        });

        this.vertx.runOnContext(aVoid -> {
            loadHandlers();
            clientInstanceOption.getServerWebSocket().resume();

            while (pushNext() == 0);

            RBucket<String> clientStatus = clientInstanceOption.getRedissonClient().getBucket("clientStatus:" + clientInstanceOption.getClientId());
            clientStatus.set("active");
        });
    }

    public void stop() throws Exception {
        clientInstanceOption.getServerWebSocket().close();
    }
}
