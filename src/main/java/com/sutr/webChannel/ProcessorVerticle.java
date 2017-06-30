package com.sutr.webChannel;

import io.vertx.core.AbstractVerticle;

/**
 * Created by nitish.aryan on 30/06/17.
 */
public abstract class ProcessorVerticle extends AbstractVerticle {
    protected final String name;

    public ProcessorVerticle(String name) {
        this.name = name;
    }

    public void start() throws Exception {
        this.vertx.eventBus().consumer("processor." + this.name + ".command", message -> {
            switch (message.body().toString()) {
                case "pushNext":
                    this.vertx.runOnContext(aVoid -> {
                        processNext();
                    });
                    break;
            }
        });
    }

    public void stop() throws Exception {

    }

    protected abstract int processNext();
}
