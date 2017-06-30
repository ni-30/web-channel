package com.sutr.webChannel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by nitish.aryan on 12/06/17.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private Metadata metadata;
    private Object data;

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        return this.metadata;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getData() {
        return this.data;
    }

    public static class Metadata {
        private String id;
        private long timestamp;
        private String timezone;
        private String sessionId;
        private String channel;
        @JsonIgnore
        private String clientId;

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClientId() {
            return this.clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getTimezone() {
            return this.timezone;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return this.sessionId;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getChannel() {
            return this.channel;
        }
    }
}
