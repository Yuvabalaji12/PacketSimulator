package org.network.packet;

import java.io.Serializable;

public class DataPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int sequenceNumber;
    private byte[] payloadChunk;
    private final int totalPackets;
    private String checksum;

    public DataPacket(int sequenceNumber,int totalPackets, byte[] payloadChunk, String checksum) {
        this.sequenceNumber = sequenceNumber;
        this.payloadChunk = payloadChunk;
        this.totalPackets = totalPackets;
        this.checksum = checksum;
    }

    public int getSequenceNumber() { return sequenceNumber; }
    public byte[] getPayloadChunk() { return payloadChunk; }
    public String getChecksum() { return checksum; }
    public int getTotalPackets() { return totalPackets; }
}
