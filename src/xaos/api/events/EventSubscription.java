package xaos.api.events;

public interface EventSubscription {

    void unsubscribe();

    boolean isActive();
}