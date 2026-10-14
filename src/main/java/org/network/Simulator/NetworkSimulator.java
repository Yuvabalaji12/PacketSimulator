package org.network.Simulator;

import org.network.packet.DataPacket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;
public class NetworkSimulator{
    private final Random random = new Random();

    // Configure simulation probabilities (e.g., 0.25 = 25% chance)
    private final double lossRate = 0.25;
    private final double corruptionRate = 0.20;

    public void transmit(DataPacket packet, ObjectOutputStream os) throws IOException {
        int seq = packet.getSequenceNumber();

        // 1. Packet Loss Simulation: Drop packet without sending
        if (random.nextDouble() < lossRate) {
            System.out.println("[SIMULATOR DROP] Dropped packet #" + seq);
            return;
        }

        DataPacket packetToSend = packet;

        // 2. Packet Corruption Simulation: Corrupt payload bytes
        if (random.nextDouble() < corruptionRate) {
            System.out.println("[SIMULATOR CORRUPT] Corrupted packet #" + seq);
            byte[] corruptedPayload = packet.getPayloadChunk().clone();
            if (corruptedPayload.length > 0) {
                corruptedPayload[0] = (byte) (corruptedPayload[0] ^ 0xFF); // Flip bits
            }
            // Use your exact constructor
            packetToSend = new DataPacket(seq,packet.getTotalPackets(), corruptedPayload, packet.getChecksum());
        }

        os.writeObject(packetToSend);
        os.flush();
    }
}