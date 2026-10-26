package dev.labtrace.event;

/** Packet metadata only: payloads and packet contents are deliberately not retained. */
public record PacketSendEvent(long tick, String packetClass, String threadName) implements LabEvent {
}
