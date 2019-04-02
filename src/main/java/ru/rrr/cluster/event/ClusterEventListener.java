package ru.rrr.cluster.event;

/**
 * Слушатель событий кластера
 */
public interface ClusterEventListener {
    void onClusterEvent(ClusterEvent event);

    default void onMemberAdd(MemberDescription memberDescription) {}

    default void onMemberRemove(MemberDescription memberDescription) {}
}
