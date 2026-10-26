package dev.labtrace.event;

public record RenderEvent(long tick, Phase phase) implements LabEvent {
    public enum Phase {
        START,
        END
    }
}
