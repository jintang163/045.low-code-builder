package com.lowcode.page.service.collaboration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class CRDTEngine {

    public DocumentState applyOperation(CRDTOperation op, DocumentState state) {
        if (op == null || state == null) {
            log.warn("Cannot apply null operation or state");
            return state;
        }

        try {
            state.updateLamportClock(op.getLamportClock());

            String type = op.getType();
            if (type == null) {
                log.warn("Operation type is null, skipping");
                return state;
            }

            switch (type.toUpperCase()) {
                case "INSERT":
                    applyInsert(op, state);
                    break;
                case "DELETE":
                    applyDelete(op, state);
                    break;
                case "UPDATE":
                    applyUpdate(op, state);
                    break;
                case "MOVE":
                    applyMove(op, state);
                    break;
                case "PROP_CHANGE":
                    applyPropChange(op, state);
                    break;
                default:
                    log.warn("Unknown operation type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to apply operation: {}", op.getId(), e);
        }

        return state;
    }

    @SuppressWarnings("unchecked")
    private void applyInsert(CRDTOperation op, DocumentState state) {
        String targetId = op.getTargetId();
        if (state.hasComponent(targetId)) {
            log.debug("Component already exists, skipping insert: {}", targetId);
            return;
        }

        DocumentState.ComponentNode node = new DocumentState.ComponentNode();
        node.setComponentId(targetId);

        Object dataObj = op.getData();
        if (dataObj instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) dataObj;
            if (data.containsKey("componentType")) {
                node.setComponentType(String.valueOf(data.get("componentType")));
            }
            if (data.containsKey("componentName")) {
                node.setComponentName(String.valueOf(data.get("componentName")));
            }
            if (data.containsKey("props") && data.get("props") instanceof Map) {
                node.getProps().putAll((Map<String, Object>) data.get("props"));
            }
            if (data.containsKey("style") && data.get("style") instanceof Map) {
                node.getStyle().putAll((Map<String, Object>) data.get("style"));
            }
            if (data.containsKey("events") && data.get("events") instanceof Map) {
                node.getEvents().putAll((Map<String, Object>) data.get("events"));
            }
        }

        state.addComponent(node, op.getParentId(), op.getPosition());
        log.debug("Applied INSERT operation for component: {}", targetId);
    }

    private void applyDelete(CRDTOperation op, DocumentState state) {
        String targetId = op.getTargetId();
        if (!state.hasComponent(targetId)) {
            log.debug("Component not found, skipping delete: {}", targetId);
            return;
        }
        state.removeComponent(targetId);
        log.debug("Applied DELETE operation for component: {}", targetId);
    }

    @SuppressWarnings("unchecked")
    private void applyUpdate(CRDTOperation op, DocumentState state) {
        String targetId = op.getTargetId();
        if (!state.hasComponent(targetId)) {
            log.debug("Component not found, skipping update: {}", targetId);
            return;
        }

        Object dataObj = op.getData();
        if (dataObj instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) dataObj;
            if (data.containsKey("props") && data.get("props") instanceof Map) {
                Map<String, Object> props = (Map<String, Object>) data.get("props");
                if (!props.isEmpty()) {
                    state.updateProps(targetId, props);
                }
            }
            if (data.containsKey("style") && data.get("style") instanceof Map) {
                Map<String, Object> style = (Map<String, Object>) data.get("style");
                if (!style.isEmpty()) {
                    state.updateStyle(targetId, style);
                }
            }
            if (data.containsKey("events") && data.get("events") instanceof Map) {
                Map<String, Object> events = (Map<String, Object>) data.get("events");
                if (!events.isEmpty()) {
                    state.updateEvents(targetId, events);
                }
            }
        }
        log.debug("Applied UPDATE operation for component: {}", targetId);
    }

    private void applyMove(CRDTOperation op, DocumentState state) {
        String targetId = op.getTargetId();
        if (!state.hasComponent(targetId)) {
            log.debug("Component not found, skipping move: {}", targetId);
            return;
        }
        state.moveComponent(targetId, op.getParentId(), op.getPosition());
        log.debug("Applied MOVE operation for component: {}", targetId);
    }

    @SuppressWarnings("unchecked")
    private void applyPropChange(CRDTOperation op, DocumentState state) {
        String targetId = op.getTargetId();
        if (!state.hasComponent(targetId)) {
            log.debug("Component not found, skipping prop change: {}", targetId);
            return;
        }

        Object dataObj = op.getData();
        String propName = null;
        Object propValue = null;

        if (dataObj instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) dataObj;
            if (data.containsKey("propName")) {
                propName = String.valueOf(data.get("propName"));
            }
            if (data.containsKey("propValue")) {
                propValue = data.get("propValue");
            }
        }

        if (propName != null) {
            state.updateProp(targetId, propName, propValue);
        }
        log.debug("Applied PROP_CHANGE operation for component: {}, prop: {}",
                targetId, propName);
    }

    public CRDTOperation transform(CRDTOperation op1, CRDTOperation op2) {
        if (op1 == null || op2 == null) {
            return op1;
        }

        if (op1.getId() != null && op1.getId().equals(op2.getId())) {
            return op1;
        }

        try {
            CRDTOperation transformed = deepCopyOperation(op1);

            String type1 = op1.getType();
            if (type1 == null) {
                return op1;
            }

            switch (type1.toUpperCase()) {
                case "INSERT":
                case "DELETE":
                case "UPDATE":
                case "PROP_CHANGE":
                    break;
                case "MOVE":
                    transformed = transformMove(op1, op2);
                    break;
                default:
                    break;
            }

            return transformed;
        } catch (Exception e) {
            log.error("Failed to transform operations", e);
            return op1;
        }
    }

    private CRDTOperation transformMove(CRDTOperation moveOp, CRDTOperation otherOp) {
        CRDTOperation transformed = deepCopyOperation(moveOp);

        String otherType = otherOp.getType();
        if (otherType == null) {
            return transformed;
        }

        if ("INSERT".equalsIgnoreCase(otherType)) {
            if (moveOp.getParentId() != null && moveOp.getParentId().equals(otherOp.getParentId())) {
                Integer otherPos = otherOp.getPosition();
                Integer movePos = moveOp.getPosition();
                if (otherPos != null && movePos != null && otherPos <= movePos) {
                    transformed.setPosition(movePos + 1);
                }
            }
        } else if ("DELETE".equalsIgnoreCase(otherType)) {
            if (moveOp.getParentId() != null && moveOp.getParentId().equals(otherOp.getParentId())) {
                Integer movePos = moveOp.getPosition();
                if (movePos != null && movePos > 0) {
                    transformed.setPosition(Math.max(0, movePos - 1));
                }
            }
        }

        return transformed;
    }

    public Optional<ConflictInfo> detectConflict(CRDTOperation op1, CRDTOperation op2) {
        if (op1 == null || op2 == null) {
            return Optional.empty();
        }

        if (op1.getId() != null && op1.getId().equals(op2.getId())) {
            return Optional.empty();
        }

        if (op1.getTargetId() == null || op2.getTargetId() == null) {
            return Optional.empty();
        }

        String conflictType = null;
        String conflictTargetId = null;
        String description = null;

        if (isDeleteUpdateConflict(op1, op2)) {
            conflictType = "DELETE_UPDATE_CONFLICT";
            conflictTargetId = op1.getTargetId();
            description = "One user deleted a component while another updated it";
        } else if (isPropertyConflict(op1, op2)) {
            conflictType = "PROPERTY_CONFLICT";
            conflictTargetId = op1.getTargetId();
            description = "Multiple users modified the same property";
        } else if (isStructureConflict(op1, op2)) {
            conflictType = "STRUCTURE_CONFLICT";
            conflictTargetId = op1.getParentId() != null ? op1.getParentId() : "root";
            description = "Structural changes conflict in the same parent";
        }

        if (conflictType != null) {
            ConflictInfo conflict = new ConflictInfo();
            conflict.setConflictId(generateOperationId());
            conflict.setConflictType(conflictType);
            conflict.setOperationA(op1);
            conflict.setOperationB(op2);
            conflict.setDescription(description);
            conflict.setStatus("PENDING");
            return Optional.of(conflict);
        }

        return Optional.empty();
    }

    private boolean isDeleteUpdateConflict(CRDTOperation op1, CRDTOperation op2) {
        if (!op1.getTargetId().equals(op2.getTargetId())) {
            return false;
        }

        String t1 = op1.getType();
        String t2 = op2.getType();
        if (t1 == null || t2 == null) {
            return false;
        }

        boolean isDelete1 = "DELETE".equalsIgnoreCase(t1);
        boolean isDelete2 = "DELETE".equalsIgnoreCase(t2);
        boolean isUpdateLike1 = "UPDATE".equalsIgnoreCase(t1) || "PROP_CHANGE".equalsIgnoreCase(t1) || "MOVE".equalsIgnoreCase(t1);
        boolean isUpdateLike2 = "UPDATE".equalsIgnoreCase(t2) || "PROP_CHANGE".equalsIgnoreCase(t2) || "MOVE".equalsIgnoreCase(t2);

        return (isDelete1 && isUpdateLike2) || (isDelete2 && isUpdateLike1);
    }

    @SuppressWarnings("unchecked")
    private boolean isPropertyConflict(CRDTOperation op1, CRDTOperation op2) {
        if (!op1.getTargetId().equals(op2.getTargetId())) {
            return false;
        }

        String t1 = op1.getType();
        String t2 = op2.getType();
        if (t1 == null || t2 == null) {
            return false;
        }

        boolean propChange1 = "PROP_CHANGE".equalsIgnoreCase(t1);
        boolean propChange2 = "PROP_CHANGE".equalsIgnoreCase(t2);
        boolean update1 = "UPDATE".equalsIgnoreCase(t1);
        boolean update2 = "UPDATE".equalsIgnoreCase(t2);

        if (propChange1 && propChange2) {
            String pn1 = getPropNameFromData(op1.getData());
            String pn2 = getPropNameFromData(op2.getData());
            return pn1 != null && pn1.equals(pn2);
        }

        if ((update1 && propChange2) || (update2 && propChange1)) {
            CRDTOperation updateOp = update1 ? op1 : op2;
            CRDTOperation propOp = propChange1 ? op1 : op2;
            String propName = getPropNameFromData(propOp.getData());
            if (propName == null) {
                return false;
            }
            Map<String, Object> updateProps = getPropsFromData(updateOp.getData());
            return updateProps != null && updateProps.containsKey(propName);
        }

        if (update1 && update2) {
            Map<String, Object> props1 = getPropsFromData(op1.getData());
            Map<String, Object> props2 = getPropsFromData(op2.getData());
            Map<String, Object> style1 = getStyleFromData(op1.getData());
            Map<String, Object> style2 = getStyleFromData(op2.getData());
            Map<String, Object> events1 = getEventsFromData(op1.getData());
            Map<String, Object> events2 = getEventsFromData(op2.getData());

            return hasOverlappingProps(props1, props2) ||
                   hasOverlappingProps(style1, style2) ||
                   hasOverlappingProps(events1, events2);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private String getPropNameFromData(Object data) {
        if (data instanceof Map) {
            Object propName = ((Map<String, Object>) data).get("propName");
            if (propName != null) {
                return String.valueOf(propName);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPropsFromData(Object data) {
        if (data instanceof Map) {
            Object props = ((Map<String, Object>) data).get("props");
            if (props instanceof Map) {
                return (Map<String, Object>) props;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getStyleFromData(Object data) {
        if (data instanceof Map) {
            Object style = ((Map<String, Object>) data).get("style");
            if (style instanceof Map) {
                return (Map<String, Object>) style;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getEventsFromData(Object data) {
        if (data instanceof Map) {
            Object events = ((Map<String, Object>) data).get("events");
            if (events instanceof Map) {
                return (Map<String, Object>) events;
            }
        }
        return null;
    }

    private boolean hasOverlappingProps(Map<String, Object> props1, Map<String, Object> props2) {
        if (props1 == null || props2 == null) {
            return false;
        }
        Set<String> keys1 = new HashSet<>(props1.keySet());
        keys1.retainAll(props2.keySet());
        return !keys1.isEmpty();
    }

    private boolean isStructureConflict(CRDTOperation op1, CRDTOperation op2) {
        String t1 = op1.getType();
        String t2 = op2.getType();
        if (t1 == null || t2 == null) {
            return false;
        }

        boolean isStructureOp1 = "INSERT".equalsIgnoreCase(t1) ||
                                 "DELETE".equalsIgnoreCase(t1) ||
                                 "MOVE".equalsIgnoreCase(t1);
        boolean isStructureOp2 = "INSERT".equalsIgnoreCase(t2) ||
                                 "DELETE".equalsIgnoreCase(t2) ||
                                 "MOVE".equalsIgnoreCase(t2);

        if (!isStructureOp1 || !isStructureOp2) {
            return false;
        }

        String parent1 = getEffectiveParentId(op1);
        String parent2 = getEffectiveParentId(op2);

        if (parent1 == null || parent2 == null) {
            return false;
        }

        return parent1.equals(parent2);
    }

    private String getEffectiveParentId(CRDTOperation op) {
        if (op.getParentId() != null) {
            return op.getParentId();
        }
        return "root";
    }

    public List<ConflictInfo> applyOperations(List<CRDTOperation> ops, DocumentState state) {
        List<ConflictInfo> conflicts = new ArrayList<>();

        if (ops == null || ops.isEmpty()) {
            return conflicts;
        }

        List<CRDTOperation> sortedOps = new ArrayList<>(ops);
        Collections.sort(sortedOps, new Comparator<CRDTOperation>() {
            @Override
            public int compare(CRDTOperation o1, CRDTOperation o2) {
                int clockCompare = Integer.compare(o1.getLamportClock(), o2.getLamportClock());
                if (clockCompare != 0) {
                    return clockCompare;
                }
                if (o1.getUserId() != null && o2.getUserId() != null) {
                    return o1.getUserId().compareTo(o2.getUserId());
                }
                return 0;
            }
        });

        List<CRDTOperation> appliedOps = new ArrayList<>();

        for (CRDTOperation op : sortedOps) {
            for (CRDTOperation applied : appliedOps) {
                Optional<ConflictInfo> conflict = detectConflict(op, applied);
                if (conflict.isPresent()) {
                    ConflictInfo ci = conflict.get();
                    if (canAutoResolve(ci)) {
                        autoResolve(ci, state);
                        ci.setStatus("AUTO_RESOLVED");
                    }
                    conflicts.add(ci);
                }
            }

            CRDTOperation transformedOp = op;
            for (CRDTOperation applied : appliedOps) {
                transformedOp = transform(transformedOp, applied);
            }

            applyOperation(transformedOp, state);
            appliedOps.add(transformedOp);
        }

        return conflicts;
    }

    private boolean canAutoResolve(ConflictInfo conflict) {
        if (conflict == null || conflict.getConflictType() == null) {
            return false;
        }

        String type = conflict.getConflictType();
        if ("PROPERTY_CONFLICT".equalsIgnoreCase(type)) {
            return true;
        }
        if ("STRUCTURE_CONFLICT".equalsIgnoreCase(type)) {
            return true;
        }
        if ("DELETE_UPDATE_CONFLICT".equalsIgnoreCase(type)) {
            return false;
        }
        return false;
    }

    private void autoResolve(ConflictInfo conflict, DocumentState state) {
        if (conflict == null || conflict.getOperationA() == null || conflict.getOperationB() == null) {
            return;
        }

        CRDTOperation op1 = conflict.getOperationA();
        CRDTOperation op2 = conflict.getOperationB();
        CRDTOperation winner = selectWinner(op1, op2);
        conflict.setResolvedBy(winner.getUserId());
        conflict.setResolution("auto_resolved_by_lamport_clock");
        conflict.setResolveTime(new java.util.Date());
    }

    private CRDTOperation selectWinner(CRDTOperation op1, CRDTOperation op2) {
        if (op1.getLamportClock() > op2.getLamportClock()) {
            return op1;
        } else if (op1.getLamportClock() < op2.getLamportClock()) {
            return op2;
        }

        if (op1.getUserId() != null && op2.getUserId() != null) {
            return op1.getUserId().compareTo(op2.getUserId()) > 0 ? op1 : op2;
        }

        return op1;
    }

    public List<CRDTOperation> mergeOperations(List<CRDTOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return new ArrayList<>();
        }

        List<CRDTOperation> sorted = new ArrayList<>(operations);
        Collections.sort(sorted, new Comparator<CRDTOperation>() {
            @Override
            public int compare(CRDTOperation o1, CRDTOperation o2) {
                int clockCompare = Integer.compare(o1.getLamportClock(), o2.getLamportClock());
                if (clockCompare != 0) {
                    return clockCompare;
                }
                if (o1.getUserId() != null && o2.getUserId() != null) {
                    return o1.getUserId().compareTo(o2.getUserId());
                }
                return 0;
            }
        });

        return sorted;
    }

    public String generateOperationId() {
        return "op_" + UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private CRDTOperation deepCopyOperation(CRDTOperation op) {
        if (op == null) {
            return null;
        }

        CRDTOperation copy = new CRDTOperation();
        copy.setId(op.getId());
        copy.setUserId(op.getUserId());
        copy.setUsername(op.getUsername());
        copy.setType(op.getType());
        copy.setTargetType(op.getTargetType());
        copy.setTargetId(op.getTargetId());
        copy.setParentId(op.getParentId());
        copy.setPosition(op.getPosition());
        copy.setTimestamp(op.getTimestamp());
        copy.setLamportClock(op.getLamportClock());
        copy.setSessionId(op.getSessionId());

        if (op.getData() instanceof Map) {
            copy.setData(new HashMap<>((Map<String, Object>) op.getData()));
        } else {
            copy.setData(op.getData());
        }

        if (op.getOldData() instanceof Map) {
            copy.setOldData(new HashMap<>((Map<String, Object>) op.getOldData()));
        } else {
            copy.setOldData(op.getOldData());
        }

        return copy;
    }
}
