package com.toll.models;

import java.util.UUID;
import com.toll.models.TollTypes.LaneStatus;
import com.toll.models.TollTypes.LaneType;

public final class Lane {
    private final String id = UUID.randomUUID().toString();
    private final int number;
    private final LaneType type;
    private LaneStatus status;
    private boolean processing;

    public Lane(int number, LaneType type, LaneStatus status) {
        if (number < 1) throw new IllegalArgumentException("Lane number must be positive");
        if (type == null || status == null) throw new IllegalArgumentException("Lane type and status are required");
        this.number = number;
        this.type = type;
        this.status = status;
    }

    public String id() { return id; }
    public int number() { return number; }
    public LaneType type() { return type; }
    public LaneStatus status() { return status; }
    public Lane copy() { return new Lane(number, type, status); }

    public void setStatus(LaneStatus status) {
        if (status == null) throw new IllegalArgumentException("Lane status is required");
        if (status == LaneStatus.CLOSED && processing) {
            throw new IllegalStateException("A lane cannot be closed while processing a vehicle");
        }
        this.status = status;
    }

    public void beginProcessing() {
        if (status != LaneStatus.OPEN) throw new IllegalStateException("Lane is closed");
        if (processing) throw new IllegalStateException("Lane is already processing a vehicle");
        processing = true;
    }

    public void endProcessing() {
        processing = false;
    }
}
