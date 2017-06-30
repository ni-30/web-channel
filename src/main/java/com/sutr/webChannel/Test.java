package com.sutr.webChannel;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.*;
import io.vertx.core.eventbus.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by nitish.aryan on 15/06/17.
 *
 *
 *
 * 1. Event loop, Worker thread pool for vertical
 * 2. Context
 *
 */
public class Test {

    public static void main(String[] args) {
        EventBusOptions eventBusOptions = new EventBusOptions()
                .setClustered(true)
                .setClusterPingInterval(1000)
                .setClusterPingReplyInterval(1000)
                .setHost("localhost");

        eventBusOptions.setReuseAddress(true);

        Vertx.clusteredVertx(new VertxOptions()
                .setWorkerPoolSize(20)
                .setEventLoopPoolSize(2)
                .setBlockedThreadCheckInterval(10000000000000L)
                .setEventBusOptions(eventBusOptions), vertxAsyncResult -> {

                final Vertx vertx = vertxAsyncResult.result();

                vertx.eventBus().consumer("hello", new Handler<io.vertx.core.eventbus.Message<String>>() {
                    @Override
                    public void handle(Message<String> message) {
                        System.out.println(System.currentTimeMillis() + " : BB (1) Periodic : " + vertx.getOrCreateContext() + " : " + Thread.currentThread() + " : " + vertx.getOrCreateContext().deploymentID()  + " : " + message.body());
                        try {
                            Thread.sleep(3000);
                        } catch (Exception e){}
                        System.out.println(System.currentTimeMillis() + " : BB (2) Periodic : " + vertx.getOrCreateContext() + " : " + Thread.currentThread() + " : " + vertx.getOrCreateContext().deploymentID()  + " : " + message.body());
                    }
                });

            vertx.registerVerticleFactory(new VerticleFactory() {
                @Override
                public String prefix() {
                    return "";
                }

                @Override
                public Verticle createVerticle(String s, ClassLoader classLoader) throws Exception {
                    return null;
                }
            });
                vertx.deployVerticle(new AbstractVerticle() {
                    @Override
                    public void start() throws Exception {

                        System.out.println(System.currentTimeMillis() + " : AA (0) Periodic : " + vertx.getOrCreateContext() + " : " + Thread.currentThread() + " : " + vertx.getOrCreateContext().deploymentID());

                        vertx.eventBus().consumer("hello", new Handler<io.vertx.core.eventbus.Message<String>>() {
                            @Override
                            public void handle(Message<String> message) {
                                System.out.println(System.currentTimeMillis() + " : AA (1) Periodic : " + vertx.getOrCreateContext() + " : " + Thread.currentThread() + " : " + vertx.getOrCreateContext().deploymentID() + " : " + message.body());
                                try {
                                    Thread.sleep(3000);
                                } catch (Exception e){}
                                System.out.println(System.currentTimeMillis() + " : AA (2) Periodic : " + vertx.getOrCreateContext() + " : " + Thread.currentThread() + " : " + vertx.getOrCreateContext().deploymentID() + " : " + message.body());
                            }
                        });
                    }
                }, new DeploymentOptions().setMultiThreaded(false).setWorker(true));

                vertx.eventBus().send("hello", "msg1");
                System.out.println("sent - msg1");
                vertx.eventBus().send("hello", "msg2");
                System.out.println("sent - msg2");
        });

    }

    public static void main_2(String[] args) {
        System.out.println(Thread.currentThread());
        Vertx vertx = Vertx.vertx();
        for (int i = 0; i < 20; i++) {
            int index = i;
            CountDownLatch latch;
            latch = new CountDownLatch(1);
            vertx.setTimer(1, id -> {
                System.out.println(index + ":" + Thread.currentThread());
                latch.countDown();
                System.out.println("A");
            });

            try {
                latch.await(2, TimeUnit.SECONDS);
                System.out.println("B");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
    public static void main_1(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Vertx vertx = Vertx.vertx();
        com.hazelcast.config.Config cfg = new com.hazelcast.config.Config("myInstance");
        HazelcastClusterManager hazelcastClusterManager = new HazelcastClusterManager(cfg);
        hazelcastClusterManager.setVertx(vertx);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        Map<Integer, String> mapCustomers = instance.getMap("instanceNode");

        while (true) {
            System.out.print("action: ");
            String action = scanner.next();
            if("in".equals(action)) {
                System.out.print("enter int key-value: ");
                mapCustomers.put(scanner.nextInt(), scanner.next());
            } else if("out".equals(action)) {
                System.out.print("enter int key: ");
                System.out.println(mapCustomers.get(scanner.nextInt()));
            } else if("rem".equals(action)){
                System.out.print("enter int key to remove: ");
                mapCustomers.remove(scanner.nextInt());
            } else if("exit".equals(action)){
                break;
            }
        }


        scanner.next();

        mapCustomers.put(2, "Ali");
        mapCustomers.put(3, "Avi");

        System.out.println("Customer with key 1: "+ mapCustomers.get(1));

        String delete = scanner.next();

        System.out.println("Customer with key 1: "+ mapCustomers.get(1));


        System.out.println("Map Size:" + mapCustomers.size());

        Queue<String> queueCustomers = instance.getQueue("customers");
        queueCustomers.offer("Tom");
        queueCustomers.offer("Mary");
        queueCustomers.offer("Jane");
        System.out.println("First customer: " + queueCustomers.poll());
        System.out.println("Second customer: "+ queueCustomers.peek());
        System.out.println("Queue size: " + queueCustomers.size());



        Config config = new Config();
        config.useSingleServer().setAddress("localhost:6379");
        RedissonClient client  = Redisson.create(config);



        RSet<String> set = client.getSet("rset");
        System.out.println("name : " + set.getName());
        set.add("nitish");
        System.out.println("move : " + set.move("v", "v"));




        RBucket<String> b = client.getBucket("bucket");

        client.getBucket("hello");

        RBlockingQueue<String> mainQ = client.getBlockingQueue("");
        try {
            System.out.println("get");
            String k = mainQ.pollLastAndOfferFirstTo("main_queue_2", 5, TimeUnit.SECONDS);
            System.out.println(k+"-----");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            int time = 0;
            b.set("entering while loop");
            System.out.println("entering while loop");
            while(time<3000) {
                b.set("before sleep");
                System.out.println("before sleep");
                Thread.sleep(100);
                b.set("after sleep");
                System.out.println("after sleep");
                time+=100;
                b.set("after increment");
                System.out.println("after increment");
            }
            b.set("after loop");
            System.out.println("after loop");
        } catch(Exception e) {
            b.set("catch");
            System.out.println("catch");
        } finally {
            b.set("finally");
            System.out.println("finally");
        }
    }
}
