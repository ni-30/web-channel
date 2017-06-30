package com.sutr.webChannel;

import com.hazelcast.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * Created by nitish.aryan on 12/06/17.
 */
public class App {

    public static void main(String[] args) throws Exception {
        final Config hazelcastConfig = new Config(System.getProperty("webChannel.node.name"));
        hazelcastConfig.setProperty("hazelcast.clientengine.thread.count", "2");

        HazelcastClusterManager clusterManager = new HazelcastClusterManager(hazelcastConfig);

        VertxOptions vertxOptions = new VertxOptions()
                .setWorkerPoolSize(1024)
                .setEventLoopPoolSize(8)
                .setClusterHost("localhost")
                .setClusterPort(5001)
                .setClusterPublicHost("localhost")
                .setClusterPublicPort(5001)
                .setClusterManager(clusterManager)
                .setClusterPingInterval(20000)
                .setClusterPingReplyInterval(20000)
                .setBlockedThreadCheckInterval(2000)
                .setWarningExceptionTime(1000)
                .setHAEnabled(true)
                .setHAGroup("ha-web-channel")
                .setClustered(true)
                .setBlockedThreadCheckInterval(10000)
                .setAddressResolverOptions(new AddressResolverOptions());

        Vertx.clusteredVertx(vertxOptions, vertxAsyncResult -> {
           ServerNode serverNode = new ServerNode(Utils.getUID(), vertxAsyncResult.result());
           serverNode.start();
        });
    }

}
