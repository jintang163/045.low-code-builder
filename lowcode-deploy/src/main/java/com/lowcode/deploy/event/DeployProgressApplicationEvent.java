package com.lowcode.deploy.event;

import com.lowcode.deploy.entity.DeployProgressEvent;
import org.springframework.context.ApplicationEvent;

public class DeployProgressApplicationEvent extends ApplicationEvent {

    private final DeployProgressEvent progressEvent;

    public DeployProgressApplicationEvent(Object source, DeployProgressEvent progressEvent) {
        super(source);
        this.progressEvent = progressEvent;
    }

    public DeployProgressEvent getProgressEvent() {
        return progressEvent;
    }
}
