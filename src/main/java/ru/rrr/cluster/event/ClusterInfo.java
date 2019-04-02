package ru.rrr.cluster.event;

import java.util.Collection;
import java.util.Map;

/**
 * Created by nsemenyuk on 02.04.2019.
 *
 * Информация о состоянии кластера.
 *
 * Список нод и т.д.
 *
 * @author nsemenyuk
 */
public class ClusterInfo {
	private final Collection<MemberDescription> members;

	public ClusterInfo(Collection<MemberDescription> members) {
		this.members = members;
	}

	public Collection<MemberDescription> getMembers() {
		return members;
	}

}
