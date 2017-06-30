package com.sutr.webChannel.logger;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by nitish.aryan on 17/06/17.
 */

public class Logger implements IPipeline {
    private static final String VERSION;
    static {
        VERSION = "v0.1";
    }

    public static Logger getLogger(String name) {
        String systemUid = System.getProperty("uid");
        return new Logger((systemUid == null ? "null." :UUID.fromString(systemUid).toString()) + "." + name);
    }

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();
    private final java.util.logging.Logger logger;

    private final String name;
    protected Logger(String name) {
        this.name = name;
        this.logger = java.util.logging.Logger.getLogger(name);
    }

    public Logger type(LogType type, final String pipeline, final String channel) {
        this.threadLocal.set(new StringBuilder()
                .append(VERSION).append("|")
                .append(name).append("|")
                .append(type).append("|")
                .append(pipeline).append("|")
                .append(channel).append("|")
                .toString());

        return this;
    }

    public Logger type(LogType type) {
        this.threadLocal.set(new StringBuilder()
                .append(VERSION).append("|")
                .append(name).append("|")
                .append(type).append("|")
                .append("null|")
                .append("null|")
                .toString());

        return this;
    }

    public void log(String key, Object... value) {
        String local;
        try {
            local = threadLocal.get();
        } finally {
            threadLocal.remove();
        }

        if(local == null) {
            logger.log(Level.WARNING, "CONTEXT_NOT_FOUND|" + name + "");
            local = new StringBuilder()
                    .append(VERSION).append("|")
                    .append(name).append("|")
                    .append("null|null|null|").toString();
        }

//        if(key == null || value == null) {
//            final Queue<String> queue = new LinkedList<>();
//            for (int i = 0; i < value.length; i++) {
//                if (key.contains("{}")) {
//                    queue.add("{" + Utils.getUID() + "}");
//                    key.replace("{}", queue.peek());
//                }
//            }
//
//            for (Object v : value) {
//                key.replace(queue.poll(), v == null ? "null" : v.toString());
//            }
//        }

        logger.log(Level.INFO, local+key, value);
    }

    public void info(String key) {
        logger.log(Level.INFO, key);
    }

    public void info(String key, Object... value) {
        this.log(key, value);
    }

    public void debug(String key) {
        logger.log(Level.OFF, key);
    }

    public void debug(String key, Object... value) {
        logger.log(Level.OFF, key, value);
    }

    public void warn(String key) {
        logger.log(Level.WARNING, key);
    }

    public void warn(String key, Object... value) {
        logger.log(Level.WARNING, key, value);
    }

    public void error(String key) {
        logger.log(Level.SEVERE, key);
    }

    public void error(String key, Throwable throwable) {
        logger.log(Level.SEVERE, key, throwable);
    }

    @Override
    public String getUid() {
        return this.name;
    }
}
