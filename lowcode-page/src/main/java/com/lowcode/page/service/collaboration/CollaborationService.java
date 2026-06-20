package com.lowcode.page.service.collaboration;

import com.alibaba.fastjson.JSON;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.PageComponent;
import com.lowcode.page.service.PageComponentService;
import com.lowcode.page.service.PageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CollaborationService {

    @Autowired
    private CRDTEngine crdtEngine;

    @Autowired
    private PageService pageService;

    @Autowired
    private PageComponentService pageComponentService;

    private final Map<Long, DocumentState> documentStates = new ConcurrentHashMap<>();

    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    private final Map<Long, Map<String, Collaborator>> collaborators = new ConcurrentHashMap<>();

    private final Map<Long, List<ConflictInfo>> conflicts = new ConcurrentHashMap<>();

    private final Map<Long, List<CRDTOperation>> operationHistory = new ConcurrentHashMap<>();

    private final Map<Long, AtomicLong> lamportClocks = new ConcurrentHashMap<>();

    private static final String[] COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    };

    @PostConstruct
    public void init() {
        log.info("CollaborationService initialized");
    }

    public Collaborator joinPage(Long pageId, String sessionId, String userId, String username, String avatar) {
        log.info("User joining page: pageId={}, userId={}, sessionId={}", pageId, userId, sessionId);

        try {
            DocumentState state = getOrCreateDocumentState(pageId);

            Map<String, Collaborator> pageCollaborators = collaborators.computeIfAbsent(pageId,
                    k -> new ConcurrentHashMap<>());

            int colorIndex = pageCollaborators.size() % COLORS.length;
            Collaborator collaborator = new Collaborator(sessionId, userId, username, avatar);
            collaborator.setColor(COLORS[colorIndex]);

            pageCollaborators.put(sessionId, collaborator);

            sessions.computeIfAbsent(pageId, k -> new ConcurrentHashMap<>());

            conflicts.computeIfAbsent(pageId, k -> Collections.synchronizedList(new ArrayList<>()));

            operationHistory.computeIfAbsent(pageId, k -> Collections.synchronizedList(new ArrayList<>()));

            lamportClocks.computeIfAbsent(pageId, k -> new AtomicLong(0));

            log.info("User joined page successfully: pageId={}, userId={}", pageId, userId);
            return collaborator;

        } catch (Exception e) {
            log.error("Failed to join page: pageId={}, userId={}", pageId, userId, e);
            throw new RuntimeException("Failed to join page", e);
        }
    }

    public void leavePage(Long pageId, String sessionId) {
        log.info("User leaving page: pageId={}, sessionId={}", pageId, sessionId);

        try {
            Map<String, WebSocketSession> pageSessions = sessions.get(pageId);
            if (pageSessions != null) {
                pageSessions.remove(sessionId);
            }

            Map<String, Collaborator> pageCollaborators = collaborators.get(pageId);
            if (pageCollaborators != null) {
                Collaborator collaborator = pageCollaborators.remove(sessionId);
                if (collaborator != null) {
                    log.info("User left page: userId={}", collaborator.getUserId());
                }
            }

            cleanupIfEmpty(pageId);

        } catch (Exception e) {
            log.error("Failed to leave page: pageId={}, sessionId={}", pageId, sessionId, e);
        }
    }

    private void cleanupIfEmpty(Long pageId) {
        Map<String, WebSocketSession> pageSessions = sessions.get(pageId);
        if (pageSessions != null && pageSessions.isEmpty()) {
            log.info("No more sessions for page {}, cleaning up", pageId);
            sessions.remove(pageId);
            collaborators.remove(pageId);
        }
    }

    public List<CRDTOperation> broadcastOperation(Long pageId, CRDTOperation operation) {
        log.debug("Broadcasting operation: pageId={}, type={}, opId={}",
                pageId, operation.getType(), operation.getOperationId());

        List<ConflictInfo> newConflicts = new ArrayList<>();

        try {
            DocumentState state = getOrCreateDocumentState(pageId);

            AtomicLong clock = lamportClocks.computeIfAbsent(pageId, k -> new AtomicLong(0));
            long newClock = Math.max(clock.incrementAndGet(), operation.getLamportClock() + 1);
            operation.setLamportClock(newClock);
            clock.set(newClock);

            if (operation.getOperationId() == null || operation.getOperationId().isEmpty()) {
                operation.setOperationId(crdtEngine.generateOperationId());
            }

            List<CRDTOperation> history = operationHistory.computeIfAbsent(pageId,
                    k -> Collections.synchronizedList(new ArrayList<>()));

            List<ConflictInfo> detectedConflicts = new ArrayList<>();
            synchronized (history) {
                for (CRDTOperation existing : history) {
                    crdtEngine.detectConflict(operation, existing).ifPresent(detectedConflicts::add);
                }
            }

            for (ConflictInfo conflict : detectedConflicts) {
                if (conflict.getStatus() == ConflictInfo.ConflictStatus.PENDING) {
                    if (conflict.getType() == ConflictInfo.ConflictType.PROPERTY_CONFLICT ||
                        conflict.getType() == ConflictInfo.ConflictType.STRUCTURE_CONFLICT) {
                        conflict.setStatus(ConflictInfo.ConflictStatus.AUTO_RESOLVED);
                        conflict.setResolution("auto_resolved_by_lamport_clock");
                        if (conflict.getConflictingOperations() != null &&
                            conflict.getConflictingOperations().size() >= 2) {
                            CRDTOperation op1 = conflict.getConflictingOperations().get(0);
                            CRDTOperation op2 = conflict.getConflictingOperations().get(1);
                            conflict.setChosenUserId(
                                op1.getLamportClock() >= op2.getLamportClock()
                                    ? op1.getUserId() : op2.getUserId()
                            );
                        }
                    }
                    newConflicts.add(conflict);
                }
            }

            List<ConflictInfo> pageConflicts = conflicts.computeIfAbsent(pageId,
                    k -> Collections.synchronizedList(new ArrayList<>()));
            pageConflicts.addAll(detectedConflicts);

            crdtEngine.applyOperation(operation, state);

            history.add(operation);

            log.debug("Operation applied successfully: pageId={}, opId={}", pageId, operation.getOperationId());

        } catch (Exception e) {
            log.error("Failed to broadcast operation: pageId={}, opId={}",
                    pageId, operation.getOperationId(), e);
        }

        return operationHistory.getOrDefault(pageId, Collections.emptyList());
    }

    public void broadcastCursor(Long pageId, String sessionId, Map<String, Object> cursorPosition) {
        log.debug("Broadcasting cursor: pageId={}, sessionId={}", pageId, sessionId);

        try {
            Map<String, Collaborator> pageCollaborators = collaborators.get(pageId);
            if (pageCollaborators != null) {
                Collaborator collab = pageCollaborators.get(sessionId);
                if (collab != null) {
                    collab.setCursorPosition(cursorPosition);
                    collab.updateActiveTime();
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast cursor: pageId={}, sessionId={}", pageId, sessionId, e);
        }
    }

    public List<Collaborator> getCollaborators(Long pageId) {
        Map<String, Collaborator> pageCollaborators = collaborators.get(pageId);
        if (pageCollaborators == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(pageCollaborators.values());
    }

    public DocumentState getDocumentState(Long pageId) {
        return documentStates.get(pageId);
    }

    public DocumentState getOrCreateDocumentState(Long pageId) {
        return documentStates.computeIfAbsent(pageId, id -> {
            DocumentState state = new DocumentState(id);
            loadPageFromDatabase(id, state);
            return state;
        });
    }

    private void loadPageFromDatabase(Long pageId, DocumentState state) {
        try {
            Page page = pageService.getById(pageId);
            if (page == null) {
                log.warn("Page not found in database: {}", pageId);
                return;
            }

            if (page.getPageConfig() != null && !page.getPageConfig().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = JSON.parseObject(page.getPageConfig(), Map.class);
                    state.getPageConfig().putAll(config);
                } catch (Exception e) {
                    log.warn("Failed to parse page config for page: {}", pageId, e);
                }
            }

            List<PageComponent> components = pageComponentService.lambdaQuery()
                    .eq(PageComponent::getPageId, pageId)
                    .orderByAsc(PageComponent::getSortOrder)
                    .list();

            if (components != null) {
                for (PageComponent comp : components) {
                    DocumentState.ComponentNode node = new DocumentState.ComponentNode();
                    node.setComponentId(comp.getComponentId());
                    node.setComponentType(comp.getComponentType());
                    node.setComponentName(comp.getComponentName());
                    node.setParentId(comp.getParentId());
                    node.setSortOrder(comp.getSortOrder() != null ? comp.getSortOrder() : 0);

                    if (comp.getPropsConfig() != null && !comp.getPropsConfig().isEmpty()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> props = JSON.parseObject(comp.getPropsConfig(), Map.class);
                            node.getProps().putAll(props);
                        } catch (Exception e) {
                            log.warn("Failed to parse props config for component: {}", comp.getComponentId(), e);
                        }
                    }

                    if (comp.getStyleConfig() != null && !comp.getStyleConfig().isEmpty()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> style = JSON.parseObject(comp.getStyleConfig(), Map.class);
                            node.getStyle().putAll(style);
                        } catch (Exception e) {
                            log.warn("Failed to parse style config for component: {}", comp.getComponentId(), e);
                        }
                    }

                    if (comp.getEventConfig() != null && !comp.getEventConfig().isEmpty()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> events = JSON.parseObject(comp.getEventConfig(), Map.class);
                            node.getEvents().putAll(events);
                        } catch (Exception e) {
                            log.warn("Failed to parse event config for component: {}", comp.getComponentId(), e);
                        }
                    }

                    state.getComponents().put(comp.getComponentId(), node);
                }

                for (PageComponent comp : components) {
                    String parentId = comp.getParentId();
                    if (parentId == null || parentId.isEmpty()) {
                        state.getRootChildren().add(comp.getComponentId());
                    } else {
                        DocumentState.ComponentNode parent = state.getComponents().get(parentId);
                        if (parent != null) {
                            parent.getChildren().add(comp.getComponentId());
                        }
                    }
                }
            }

            log.info("Loaded page from database: pageId={}, componentCount={}",
                    pageId, state.getComponents().size());

        } catch (Exception e) {
            log.error("Failed to load page from database: {}", pageId, e);
        }
    }

    public boolean resolveConflict(Long pageId, String conflictId, String resolution, String chosenUserId) {
        log.info("Resolving conflict: pageId={}, conflictId={}", pageId, conflictId);

        try {
            List<ConflictInfo> pageConflicts = conflicts.get(pageId);
            if (pageConflicts == null) {
                log.warn("No conflicts found for page: {}", pageId);
                return false;
            }

            for (ConflictInfo conflict : pageConflicts) {
                if (conflict.getConflictId().equals(conflictId)) {
                    conflict.setStatus(ConflictInfo.ConflictStatus.RESOLVED);
                    conflict.setResolution(resolution);
                    conflict.setChosenUserId(chosenUserId);
                    conflict.setResolvedAt(System.currentTimeMillis());
                    log.info("Conflict resolved: conflictId={}, resolution={}", conflictId, resolution);
                    return true;
                }
            }

            log.warn("Conflict not found: {}", conflictId);
            return false;

        } catch (Exception e) {
            log.error("Failed to resolve conflict: pageId={}, conflictId={}", pageId, conflictId, e);
            return false;
        }
    }

    public DocumentState syncDocument(Long pageId, String sessionId) {
        log.info("Syncing document: pageId={}, sessionId={}", pageId, sessionId);

        try {
            DocumentState state = getOrCreateDocumentState(pageId);

            Map<String, Collaborator> pageCollaborators = collaborators.get(pageId);
            if (pageCollaborators != null) {
                Collaborator collab = pageCollaborators.get(sessionId);
                if (collab != null) {
                    collab.updateActiveTime();
                }
            }

            return state;

        } catch (Exception e) {
            log.error("Failed to sync document: pageId={}, sessionId={}", pageId, sessionId, e);
            return null;
        }
    }

    public List<ConflictInfo> getPendingConflicts(Long pageId) {
        List<ConflictInfo> pageConflicts = conflicts.get(pageId);
        if (pageConflicts == null) {
            return Collections.emptyList();
        }

        List<ConflictInfo> pending = new ArrayList<>();
        synchronized (pageConflicts) {
            for (ConflictInfo conflict : pageConflicts) {
                if (conflict.getStatus() == ConflictInfo.ConflictStatus.PENDING) {
                    pending.add(conflict);
                }
            }
        }
        return pending;
    }

    public List<ConflictInfo> getAllConflicts(Long pageId) {
        List<ConflictInfo> pageConflicts = conflicts.get(pageId);
        if (pageConflicts == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(pageConflicts);
    }

    public List<CRDTOperation> getOperationHistory(Long pageId) {
        List<CRDTOperation> history = operationHistory.get(pageId);
        if (history == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(history);
    }

    public Map<String, WebSocketSession> getSessions(Long pageId) {
        return sessions.get(pageId);
    }

    public void registerSession(Long pageId, String sessionId, WebSocketSession session) {
        Map<String, WebSocketSession> pageSessions = sessions.computeIfAbsent(pageId,
                k -> new ConcurrentHashMap<>());
        pageSessions.put(sessionId, session);
        log.debug("Session registered: pageId={}, sessionId={}", pageId, sessionId);
    }

    public long getNextLamportClock(Long pageId) {
        AtomicLong clock = lamportClocks.computeIfAbsent(pageId, k -> new AtomicLong(0));
        return clock.incrementAndGet();
    }

    public void updateLamportClock(Long pageId, long receivedClock) {
        AtomicLong clock = lamportClocks.computeIfAbsent(pageId, k -> new AtomicLong(0));
        long current = clock.get();
        if (receivedClock > current) {
            clock.set(receivedClock);
        }
    }
}
