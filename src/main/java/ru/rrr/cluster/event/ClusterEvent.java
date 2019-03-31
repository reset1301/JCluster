package ru.rrr.cluster.event;

/**
 * Событие кластера
 */
public class ClusterEvent {
    final ClusterEventType eventType;
    final MemberDescription memberDescription;

    public ClusterEvent(ClusterEventType eventType, MemberDescription memberDescription) {
        this.eventType = eventType;
        this.memberDescription = memberDescription;
    }

    public enum ClusterEventType{
        /**
         * Нода обнаружила сама себя.
         * Врядли это событие интересно кому-то, кроме самой ноды, но с чего-то начинать надо :-)
         */
        SELF_DETECTED
    }
}
