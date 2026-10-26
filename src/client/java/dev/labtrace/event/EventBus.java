package dev.labtrace.event;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Thread-safe, typed event bus. Listeners should not mutate Minecraft state off the client thread. */
public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    public <T> void subscribe(Class<T> eventType, Consumer<? super T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        Consumer<Object> bridge = event -> listener.accept(eventType.cast(event));
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(bridge);
    }

    public void post(Object event) {
        Objects.requireNonNull(event, "event");
        Class<?> actualType = event.getClass();
        listeners.forEach((registeredType, handlers) -> {
            if (registeredType.isAssignableFrom(actualType)) {
                for (Consumer<Object> handler : handlers) {
                    try {
                        handler.accept(event);
                    } catch (RuntimeException exception) {
                        System.err.println("[LabTrace] Event listener failed for " + actualType.getName());
                        exception.printStackTrace(System.err);
                    }
                }
            }
        });
    }
}
