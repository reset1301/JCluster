package ru.rrr.cluster.event;

/**
 * Слушатель событий кластера
 */
public interface ClusterEventListener {
    void onClusterEvent(ClusterEvent event);

    void onMemberAdd(MemberDescription memberDescription);

    void onMemberRemove(MemberDescription memberDescription);
}
