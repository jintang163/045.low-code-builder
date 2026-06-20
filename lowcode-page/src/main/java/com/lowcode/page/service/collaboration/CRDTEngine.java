package com.lowcode.page.service.collaboration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

            switch (op.getType()) {
                case INSERT:
                    applyInsert(op, state);
                    break;
                case DELETE:
                    applyDelete(op, state);
                    break;
                case UPDATE:
                    applyUpdate(op, state);
                    break;
                case MOVE:
                    applyMove(op, state);
                    break;
                case PROP_CHANGE:
                    applyPropChange(op, state);
                    break;
                default:
                    log.warn("Unknown operation type: {}", op.getType());
            }
        } catch (Exception e) {
            log.error("Failed to apply operation: {}", op.getOperationId(), e);
        }

        return state;
    }

    private void applyInsert(CRDTOperation op, DocumentState state) {
        if (state.hasComponent(op.getComponentId())) {
            log.debug("Component already exists, skipping insert: {}", op.getComponentId());
            return;
        }

        DocumentState.ComponentNode node = new DocumentState.ComponentNode();
        node.setComponentId(op.getComponentId());
        node.setComponentType(op.getComponentType());
        node.setComponentName(op.getComponentName());

        if (op.getProps() != null) {
            node.getProps().putAll(op.getProps());
        }
        if (op.getStyle() != null) {
            node.getStyle().putAll(op.getStyle());
        }
        if (op.getEvents() != null) {
            node.getEvents().putAll(op.getEvents());
        }

        state.addComponent(node, op.getParentId(), op.getPosition());
        log.debug("Applied INSERT operation for component: {}", op.getComponentId());
    }

    private void applyDelete(CRDTOperation op, DocumentState state) {
        if (!state.hasComponent(op.getComponentId())) {
            log.debug("Component not found, skipping delete: {}", op.getComponentId());
            return;
        }
        state.removeComponent(op.getComponentId());
        log.debug("Applied DELETE operation for component: {}", op.getComponentId());
    }

    private void applyUpdate(CRDTOperation op, DocumentState state) {
        if (!state.hasComponent(op.getComponentId())) {
            log.debug("Component not found, skipping update: {}", op.getComponentId());
            return;
        }

        if (op.getProps() != null && !op.getProps().isEmpty()) {
            state.updateProps(op.getComponentId(), op.getProps());
        }
        if (op.getStyle() != null && !op.getStyle().isEmpty()) {
            state.updateStyle(op.getComponentId(), op.getStyle());
        }
        if (op.getEvents() != null && !op.getEvents().isEmpty()) {
            state.updateEvents(op.getComponentId(), op.getEvents());
        }
        log.debug("Applied UPDATE operation for component: {}", op.getComponentId());
    }

    private void applyMove(CRDTOperation op, DocumentState state) {
        if (!state.hasComponent(op.getComponentId())) {
            log.debug("Component not found, skipping move: {}", op.getComponentId());
            return;
        }
        state.moveComponent(op.getComponentId(), op.getParentId(), op.getPosition());
        log.debug("Applied MOVE operation for component: {}", op.getComponentId());
    }

    private void applyPropChange(CRDTOperation op, DocumentState state) {
        if (!state.hasComponent(op.getComponentId())) {
            log.debug("Component not found, skipping prop change: {}", op.getComponentId());
            return;
        }
        if (op.getPropName() != null) {
            state.updateProp(op.getComponentId(), op.getPropName(), op.getPropValue());
        }
        log.debug("Applied PROP_CHANGE operation for component: {}, prop: {}",
                op.getComponentId(), op.getPropName());
    }

    public CRDTOperation transform(CRDTOperation op1, CRDTOperation op2) {
        if (op1 == null || op2 == null) {
            return op1;
        }

        if (op1.getOperationId() != null && op1.getOperationId().equals(op2.getOperationId())) {
            return op1;
        }

        try {
            CRDTOperation transformed = deepCopyOperation(op1);

            switch (op1.getType()) {
                case INSERT:
                case DELETE:
                case UPDATE:
                case PROP_CHANGE:
                    break;
                case MOVE:
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

        if (otherOp.getType() == CRDTOperation.OperationType.INSERT) {
            if (moveOp.getParentId() != null && moveOp.getParentId().equals(otherOp.getParentId())) {
                Integer otherPos = otherOp.getPosition();
                Integer movePos = moveOp.getPosition();
                if (otherPos != null && movePos != null && otherPos <= movePos) {
                    transformed.setPosition(movePos + 1);
                }
            }
        } else if (otherOp.getType() == CRDTOperation.OperationType.DELETE) {
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

        if (op1.getOperationId() != null && op1.getOperationId().equals(op2.getOperationId())) {
            return Optional.empty();
        }

        if (op1.getComponentId() == null || op2.getComponentId() == null) {
            return Optional.empty();
        }

        ConflictInfo.ConflictType conflictType = null;
        String conflictComponentId = null;
        String propName = null;

        if (isDeleteUpdateConflict(op1, op2)) {
            conflictType = ConflictInfo.ConflictType.DELETE_UPDATE_CONFLICT;
            conflictComponentId = op1.getComponentId();
        } else if (isPropertyConflict(op1, op2)) {
            conflictType = ConflictInfo.ConflictType.PROPERTY_CONFLICT;
            conflictComponentId = op1.getComponentId();
            propName = findConflictingProp(op1, op2);
        } else if (isStructureConflict(op1, op2)) {
            conflictType = ConflictInfo.ConflictType.STRUCTURE_CONFLICT;
            conflictComponentId = op1.getParentId() != null ? op1.getParentId() : "root";
        }

        if (conflictType != null) {
            ConflictInfo conflict = new ConflictInfo();
            conflict.setConflictId(generateOperationId());
            conflict.setType(conflictType);
            conflict.setComponentId(conflictComponentId);
            conflict.setPropName(propName);
            conflict.addConflictingOperation(op1);
            conflict.addConflictingOperation(op2);
            conflict.setStatus(ConflictInfo.ConflictStatus.PENDING);
            return Optional.of(conflict);
        }

        return Optional.empty();
    }

    private boolean isDeleteUpdateConflict(CRDTOperation op1, CRDTOperation op2) {
        if (!op1.getComponentId().equals(op2.getComponentId())) {
            return false;
        }

        CRDTOperation.OperationType t1 = op1.getType();
        CRDTOperation.OperationType t2 = op2.getType();

        return (t1 == CRDTOperation.OperationType.DELETE &&
                (t2 == CRDTOperation.OperationType.UPDATE ||
                 t2 == CRDTOperation.OperationType.PROP_CHANGE ||
                 t2 == CRDTOperation.OperationType.MOVE))
               ||
               (t2 == CRDTOperation.OperationType.DELETE &&
                (t1 == CRDTOperation.OperationType.UPDATE ||
                 t1 == CRDTOperation.OperationType.PROP_CHANGE ||
                 t1 == CRDTOperation.OperationType.MOVE));
    }

    private boolean isPropertyConflict(CRDTOperation op1, CRDTOperation op2) {
        if (!op1.getComponentId().equals(op2.getComponentId())) {
            return false;
        }

        CRDTOperation.OperationType t1 = op1.getType();
        CRDTOperation.OperationType t2 = op2.getType();

        if (t1 == CRDTOperation.OperationType.PROP_CHANGE &&
            t2 == CRDTOperation.OperationType.PROP_CHANGE) {
            return op1.getPropName() != null && op1.getPropName().equals(op2.getPropName());
        }

        if ((t1 == CRDTOperation.OperationType.UPDATE && t2 == CRDTOperation.OperationType.PROP_CHANGE) ||
            (t2 == CRDTOperation.OperationType.UPDATE && t1 == CRDTOperation.OperationType.PROP_CHANGE)) {
            CRDTOperation updateOp = t1 == CRDTOperation.OperationType.UPDATE ? op1 : op2;
            CRDTOperation propOp = t1 == CRDTOperation.OperationType.PROP_CHANGE ? op1 : op2;
            return updateOp.getProps() != null && updateOp.getProps().containsKey(propOp.getPropName());
        }

        if (t1 == CRDTOperation.OperationType.UPDATE && t2 == CRDTOperation.OperationType.UPDATE) {
            return hasOverlappingProps(op1.getProps(), op2.getProps()) ||
                   hasOverlappingProps(op1.getStyle(), op2.getStyle()) ||
                   hasOverlappingProps(op1.getEvents(), op2.getEvents());
        }

        return false;
    }

    private boolean hasOverlappingProps(Map<String, Object> props1, Map<String, Object> props2) {
        if (props1 == null || props2 == null) {
            return false;
        }
        Set<String> keys1 = new HashSet<>(props1.keySet());
        keys1.retainAll(props2.keySet());
        return !keys1.isEmpty();
    }

    private String findConflictingProp(CRDTOperation op1, CRDTOperation op2) {
        if (op1.getPropName() != null && op2.getPropName() != null &&
            op1.getPropName().equals(op2.getPropName())) {
            return op1.getPropName();
        }

        if (op1.getProps() != null && op2.getProps() != null) {
            Set<String> keys1 = new HashSet<>(op1.getProps().keySet());
            keys1.retainAll(op2.getProps().keySet());
            if (!keys1.isEmpty()) {
                return keys1.iterator().next();
            }
        }

        return null;
    }

    private boolean isStructureConflict(CRDTOperation op1, CRDTOperation op2) {
        CRDTOperation.OperationType t1 = op1.getType();
        CRDTOperation.OperationType t2 = op2.getType();

        boolean isStructureOp1 = t1 == CRDTOperation.OperationType.INSERT ||
                                 t1 == CRDTOperation.OperationType.DELETE ||
                                 t1 == CRDTOperation.OperationType.MOVE;
        boolean isStructureOp2 = t2 == CRDTOperation.OperationType.INSERT ||
                                 t2 == CRDTOperation.OperationType.DELETE ||
                                 t2 == CRDTOperation.OperationType.MOVE;

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
                int clockCompare = Long.compare(o1.getLamportClock(), o2.getLamportClock());
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
                        ci.setStatus(ConflictInfo.ConflictStatus.AUTO_RESOLVED);
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
        if (conflict == null) {
            return false;
        }

        switch (conflict.getType()) {
            case PROPERTY_CONFLICT:
                return true;
            case STRUCTURE_CONFLICT:
                return true;
            case DELETE_UPDATE_CONFLICT:
                return false;
            default:
                return false;
        }
    }

    private void autoResolve(ConflictInfo conflict, DocumentState state) {
        if (conflict == null || conflict.getConflictingOperations() == null ||
            conflict.getConflictingOperations().size() < 2) {
            return;
        }

        List<CRDTOperation> ops = conflict.getConflictingOperations();
        CRDTOperation winner = selectWinner(ops.get(0), ops.get(1));
        conflict.setChosenUserId(winner.getUserId());
        conflict.setResolution("auto_resolved_by_lamport_clock");
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
                int clockCompare = Long.compare(o1.getLamportClock(), o2.getLamportClock());
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

    private CRDTOperation deepCopyOperation(CRDTOperation op) {
        if (op == null) {
            return null;
        }

        CRDTOperation copy = new CRDTOperation();
        copy.setOperationId(op.getOperationId());
        copy.setType(op.getType());
        copy.setComponentId(op.getComponentId());
        copy.setParentId(op.getParentId());
        copy.setPosition(op.getPosition());
        copy.setPropName(op.getPropName());
        copy.setPropValue(op.getPropValue());
        copy.setComponentType(op.getComponentType());
        copy.setComponentName(op.getComponentName());
        copy.setUserId(op.getUserId());
        copy.setUsername(op.getUsername());
        copy.setLamportClock(op.getLamportClock());
        copy.setTimestamp(op.getTimestamp());
        copy.setBaseVersion(op.getBaseVersion());

        if (op.getProps() != null) {
            copy.setProps(new java.util.HashMap<>(op.getProps()));
        }
        if (op.getStyle() != null) {
            copy.setStyle(new java.util.HashMap<>(op.getStyle()));
        }
        if (op.getEvents() != null) {
            copy.setEvents(new java.util.HashMap<>(op.getEvents()));
        }

        return copy;
    }
}
