package com.sutr.webChannel;
import org.redisson.api.*;

/**
 * Created by nitish.aryan on 30/06/17.
 */
public class SubscriberVerticle extends ProcessorVerticle {

    protected final RedissonClient redissonClient;

    public SubscriberVerticle(RedissonClient redissonClient) {
        super("subscriber");
        this.redissonClient = redissonClient;

    }

    @Override
    protected int processNext() {
        final RQueue<String> mainQ = redissonClient.getQueue("processorQueue:" + name);

        if (mainQ.isEmpty()) {
            return 1;
        }

        final RQueue<String> backupQ = redissonClient.getQueue("processorQueue:backup_" + name);

        final String msgId = mainQ.pollLastAndOfferFirstTo("processorQueue:backup_" + name);
        final RBucket<Message> messageBucket = redissonClient.getBucket("message:" + msgId);

        boolean removeMsg = true;
        try {
            if (!messageBucket.isExists()) {
                return 2;
            }

            Message message = messageBucket.get();
            if (message == null || message.getData() == null) {
                return 3;
            }

            String channelName = message.getData().toString();

            RList<String> channelSubscribersList = redissonClient.getList("channelSubscribers:" + channelName);
            channelSubscribersList.add(message.getMetadata().getClientId());
            return 0;
        } catch (Exception e) {
            removeMsg = false;
            return 6;
        } finally {
            if(removeMsg) {
                backupQ.remove(msgId);
                RAtomicLong rCounter = redissonClient.getAtomicLong("messagePushCounter:" + msgId);
                if (rCounter.decrementAndGet() <= 0) {
                    messageBucket.delete();
                    rCounter.delete();
                }
            }
        }

    }
}
