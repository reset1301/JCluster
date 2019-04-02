package ru.rrr.cluster.event;

/**
 * Событие кластера
 */
public class ClusterEvent {
    private final ClusterEventType eventType;
    private final ClusterInfo clusterInfo;
    private final MemberDescription memberDescription;

    // TODO: 02.04.2019 Использовать!

    public ClusterEvent(ClusterEventType eventType, ClusterInfo clusterInfo, MemberDescription memberDescription) {
        this.eventType = eventType;
        this.clusterInfo = clusterInfo;
        this.memberDescription = memberDescription;
    }

    public enum ClusterEventType{
        /**
         * Нода обнаружила сама себя.
         * Врядли это событие интересно кому-то, кроме самой ноды, но с чего-то начинать надо :-)
         */
        SELF_DETECTED,
        /**
         * Добавление нового члена кластера
         */
        MEMBER_ADDED,
        /**
         * Удаление из кластера одного члена
         */
        MEMBER_REMOVED
    }
}
