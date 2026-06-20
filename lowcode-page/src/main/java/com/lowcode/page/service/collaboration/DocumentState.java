package com.lowcode.page.service.collaboration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Data
public class DocumentState implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pageId;

    private Map<String, ComponentNode> components;

    private List<String> rootChildren;

    private int version;

    private long lastLamportClock;

    private Map<String, Object> pageConfig;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Data
    public static class ComponentNode implements Serializable {

        private static final long serialVersionUID = 1L;

        private String componentId;

        private String componentType;

        private String componentName;

        private String parentId;

        private int sortOrder;

        private Map<String, Object> props;

        private Map<String, Object> style;

        private Map<String, Object> events;

        private List<String> children;

        public ComponentNode() {
            this.props = new HashMap<>();
            this.style = new HashMap<>();
            this.events = new HashMap<>();
            this.children = new ArrayList<>();
        }

        public ComponentNode(String componentId, String componentType, String parentId) {
            this();
            this.componentId = componentId;
            this.componentType = componentType;
            this.parentId = parentId;
        }

        @SuppressWarnings("unchecked")
        public void updateProps(Map<String, Object> newProps) {
            if (newProps != null && !newProps.isEmpty()) {
                this.props.putAll(newProps);
            }
        }

        @SuppressWarnings("unchecked")
        public void updateStyle(Map<String, Object> newStyle) {
            if (newStyle != null && !newStyle.isEmpty()) {
                this.style.putAll(newStyle);
            }
        }

        @SuppressWarnings("unchecked")
        public void updateEvents(Map<String, Object> newEvents) {
            if (newEvents != null && !newEvents.isEmpty()) {
                this.events.putAll(newEvents);
            }
        }
    }

    public DocumentState() {
        this.components = new LinkedHashMap<>();
        this.rootChildren = new ArrayList<>();
        this.pageConfig = new HashMap<>();
        this.version = 0;
        this.lastLamportClock = 0;
    }

    public DocumentState(Long pageId) {
        this();
        this.pageId = pageId;
    }

    public ComponentNode getComponent(String id) {
        lock.readLock().lock();
        try {
            return components.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasComponent(String id) {
        lock.readLock().lock();
        try {
            return components.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addComponent(ComponentNode node, String parentId, Integer position) {
        lock.writeLock().lock();
        try {
            if (node == null || node.getComponentId() == null) {
                log.warn("Cannot add null component or component with null id");
                return;
            }

            components.put(node.getComponentId(), node);
            node.setParentId(parentId);

            List<String> targetList;
            if (parentId == null || parentId.isEmpty()) {
                targetList = rootChildren;
            } else {
                ComponentNode parent = components.get(parentId);
                if (parent == null) {
                    log.warn("Parent component not found: {}, adding to root", parentId);
                    targetList = rootChildren;
                    node.setParentId(null);
                } else {
                    targetList = parent.getChildren();
                }
            }

            if (position != null && position >= 0 && position < targetList.size()) {
                targetList.add(position, node.getComponentId());
            } else {
                targetList.add(node.getComponentId());
            }

            updateSortOrders(targetList);
            version++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeComponent(String id) {
        lock.writeLock().lock();
        try {
            ComponentNode node = components.get(id);
            if (node == null) {
                log.warn("Component not found for removal: {}", id);
                return false;
            }

            removeComponentRecursively(id);
            version++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeComponentRecursively(String id) {
        ComponentNode node = components.get(id);
        if (node == null) {
            return;
        }

        List<String> children = new ArrayList<>(node.getChildren());
        for (String childId : children) {
            removeComponentRecursively(childId);
        }

        String parentId = node.getParentId();
        if (parentId == null || parentId.isEmpty()) {
            rootChildren.remove(id);
        } else {
            ComponentNode parent = components.get(parentId);
            if (parent != null) {
                parent.getChildren().remove(id);
                updateSortOrders(parent.getChildren());
            }
        }

        components.remove(id);
    }

    public boolean moveComponent(String id, String newParentId, Integer position) {
        lock.writeLock().lock();
        try {
            ComponentNode node = components.get(id);
            if (node == null) {
                log.warn("Component not found for move: {}", id);
                return false;
            }

            if (isDescendant(newParentId, id)) {
                log.warn("Cannot move component into its own descendant: {} -> {}", id, newParentId);
                return false;
            }

            String oldParentId = node.getParentId();
            List<String> oldList;
            if (oldParentId == null || oldParentId.isEmpty()) {
                oldList = rootChildren;
            } else {
                ComponentNode oldParent = components.get(oldParentId);
                if (oldParent == null) {
                    oldList = rootChildren;
                } else {
                    oldList = oldParent.getChildren();
                }
            }
            oldList.remove(id);
            updateSortOrders(oldList);

            List<String> newList;
            if (newParentId == null || newParentId.isEmpty()) {
                newList = rootChildren;
                node.setParentId(null);
            } else {
                ComponentNode newParent = components.get(newParentId);
                if (newParent == null) {
                    log.warn("New parent not found: {}, moving to root", newParentId);
                    newList = rootChildren;
                    node.setParentId(null);
                } else {
                    newList = newParent.getChildren();
                    node.setParentId(newParentId);
                }
            }

            if (position != null && position >= 0 && position < newList.size()) {
                newList.add(position, id);
            } else {
                newList.add(id);
            }
            updateSortOrders(newList);

            version++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean isDescendant(String targetId, String ancestorId) {
        if (targetId == null || targetId.isEmpty()) {
            return false;
        }
        if (targetId.equals(ancestorId)) {
            return true;
        }
        ComponentNode node = components.get(targetId);
        if (node == null) {
            return false;
        }
        return isDescendant(node.getParentId(), ancestorId);
    }

    public boolean updateProps(String id, Map<String, Object> props) {
        lock.writeLock().lock();
        try {
            ComponentNode node = components.get(id);
            if (node == null) {
                log.warn("Component not found for props update: {}", id);
                return false;
            }
            node.updateProps(props);
            version++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean updateStyle(String id, Map<String, Object> style) {
        lock.writeLock().lock();
        try {
            ComponentNode node = components.get(id);
            if (node == null) {
                log.warn("Component not found for style update: {}", id);
                return false;
            }
            node.updateStyle(style);
            version++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean updateEvents(String id, Map<String, Object> events) {
        lock.writeLock().lock();
        try {
            ComponentNode node = components.get(id);
            if (node == null) {
                log.warn("Component not found for events update: {}", id);
                return false;
            }
            node.updateEvents(events);
            version++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean updateProp(String id, String propName, Object propValue) {
        lock.writeLock().lock();
        try {
            ComponentNode node = components.get(id);
            if (node == null) {
                log.warn("Component not found for prop update: {}", id);
                return false;
            }
            node.getProps().put(propName, propValue);
            version++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getRootChildren() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rootChildren);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getChildren(String parentId) {
        lock.readLock().lock();
        try {
            if (parentId == null || parentId.isEmpty()) {
                return new ArrayList<>(rootChildren);
            }
            ComponentNode parent = components.get(parentId);
            if (parent == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(parent.getChildren());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void updateSortOrders(List<String> children) {
        for (int i = 0; i < children.size(); i++) {
            String childId = children.get(i);
            ComponentNode child = components.get(childId);
            if (child != null) {
                child.setSortOrder(i);
            }
        }
    }

    public void updateLamportClock(long clock) {
        lock.writeLock().lock();
        try {
            if (clock > lastLamportClock) {
                lastLamportClock = clock;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getComponentCount() {
        lock.readLock().lock();
        try {
            return components.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
