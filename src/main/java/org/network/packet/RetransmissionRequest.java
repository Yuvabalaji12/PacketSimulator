package org.network.packet;

import java.io.Serializable;
import java.util.List;

public class RetransmissionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Integer> missingSequences;

    public RetransmissionRequest(List<Integer> missingSequences) {
        this.missingSequences = missingSequences;
    }

    public List<Integer> getMissingSequences() {
        return missingSequences;
    }

    public boolean isComplete() {
        return missingSequences == null || missingSequences.isEmpty();
    }
}