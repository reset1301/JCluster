package ru.rrr.cluster;

import lombok.extern.slf4j.Slf4j;
import ru.rrr.cfg.Const;
import ru.rrr.cfg.NodeConfig;
import ru.rrr.cfg.NodeConfigException;
import ru.rrr.cfg.NodeUri;
import ru.rrr.cluster.event.ClusterEvent;
import ru.rrr.cluster.event.ClusterEventListener;
import ru.rrr.cluster.event.ClusterInfo;
import ru.rrr.cluster.event.MemberDescription;
import ru.rrr.model.Message;
import ru.rrr.model.MessageType;
import ru.rrr.tcp.NetworkExchangeException;
import ru.rrr.tcp.TcpClient;
import ru.rrr.tcp.TcpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class Node {
	private final String uuid;
	private final ScheduledExecutorService discoverExecutor = Executors.newScheduledThreadPool(1);
	private final int sendMessageTimeout;
	private TcpServer server;
	private Map<String, TcpClient> clients = new HashMap<>();
	private List<ClusterEventListener> clusterEventListeners = new ArrayList<>();
	private String address;
	private int port;
	private String clusterName;

	public Node(NodeConfig config) throws IOException, NodeConfigException {
		this(config, UUID.randomUUID().toString());
	}

	public Node(NodeConfig config, String uuid) throws NodeConfigException, IOException {
		this.uuid = uuid;
		this.port = config.getPort();
		this.clusterName = config.getClusterName();

		initListeners();

		this.sendMessageTimeout = config.getSendMessageTimeout();

		log.info("Node [{}]. Start in cluster '{}'", this.uuid, clusterName);

		this.server = new TcpServer(port, this.uuid, clusterName);
		this.port = server.getPort();

		final Collection<NodeUri> members = config.getMembers();
		if (members.size() < 1) {
			throw new NodeConfigException("No Cluster Member URI found in configuration");
		}

		// TODO: 03.03.2019 добавить отдельный поток с переконнектами к другим нодам
		for (NodeUri member : members) {
			discoverExecutor.scheduleAtFixedRate(
					() -> discover(member.getHost(), config.getPort(), config.getPortsRangeEnd(),
							config.getConnectionTimeoutSeconds(), config.getConnectionTimeoutSeconds()), 0,
					Const.NODE_DISCOVER_PERIOD, TimeUnit.SECONDS);
		}
	}

	/**
	 * Настройка слушателей событий кластера
	 */
	private void initListeners() {
		this.clusterEventListeners.add(event -> printMembersList());
	}

	/**
	 * Добавляет слушателя событий кластера
	 *
	 * @param listener слушатель событий кластера
	 */
	public void addClusterEventListener(ClusterEventListener listener) {
		this.clusterEventListeners.add(listener);
	}

	/**
	 * Ищет ноды своего кластера на машине host в определенном диапазоне портов.
	 * Метод должен выполняться с определенной периодичностью в отдельном потоке.
	 *
	 * @param host              адрес машины, на которой ищем ноды
	 * @param startPort         порт, с которого начинаем сканировать (включительно)
	 * @param endPort           порт, до которого сканируем (включительно)
	 * @param connectionTimeout таймаут соединения
	 * @param reconnectTimeout  периодичность попыток соединения при потери подключения
	 */
	private void discover(String host, int startPort, int endPort, int connectionTimeout, int reconnectTimeout) {
		log.info("Node [{}]. Running a scan on the host: {}, starting at port {}", uuid, host, startPort);
		for (int currentPort = startPort; currentPort < endPort; currentPort++) {
			TcpClient client = new TcpClient(host, currentPort, uuid, connectionTimeout, reconnectTimeout);
			if (!client.isConnected()) {
				//                client.close();
				// TODO: 31.03.2019 это надо как-то обрабатывать
				continue;
			}
			try {
				// Фильтр
				final Message messageGetUUID = client
						.sendMessage(new Message(MessageType.GET_UUID), sendMessageTimeout);
				final String currentUUID = messageGetUUID.getData();
				if (clients.containsKey(currentUUID)) {
					// Ранее обнаруженные ноды игнорируем
					continue;
				}
				if (this.uuid.equals(currentUUID)) {
					this.address = client.getHost();
					closeSelfConnection(client);
					addNewClient(currentUUID, client);
					continue;
				} else {
					final Message messageClusterName = client
							.sendMessage(new Message(MessageType.GET_CLUSTER_NAME), Const.SEND_MESSAGE_TIMEOUT);
					final String currentClusterName = messageClusterName.getData();
					if (!this.clusterName.equals(currentClusterName)) {
						closeOtherClusterConnection(client, currentClusterName);
						continue;
					}
				}

				log.debug("New node detected: {}:{}", host, currentPort);
				// Если это другая нода из нашего кластера, то добавляем ее в коллекцию нод
				addNewClient(currentUUID, client);
			} catch (NetworkExchangeException e) {
				client.close();
				log.error("Failed to get a reply to the message", e);
			}

		}
	}

	/**
	 * Добавляет новую ноду в кластер
	 *
	 * @param uuid   идентификатор ноды
	 * @param client клиент
	 */
	private void addNewClient(String uuid, TcpClient client) {
		if (!Objects.equals(this.uuid, uuid)) {
			this.clients.put(uuid, client);
		}
		ClusterInfo clusterInfo = new ClusterInfo(this.clients.values().stream().map(MemberDescription::new)
				.collect(Collectors.toList()));
		for (ClusterEventListener listener : clusterEventListeners) {
			MemberDescription memberDescription = new MemberDescription(uuid, client.getHost(), client.getPort());
			listener.onMemberAdd(memberDescription);
			listener.onClusterEvent(
					new ClusterEvent(ClusterEvent.ClusterEventType.MEMBER_ADDED, clusterInfo, memberDescription));
		}
		//        printMembersList();
	}

	/**
	 * Закрывает соединение ноды к самой себе
	 *
	 * @param client клиент
	 * @throws NetworkExchangeException ошибка при отправке/получении сообщения
	 */
	private void closeSelfConnection(TcpClient client) throws NetworkExchangeException {
		// Закрываем соединение с самим собой
		log.debug("Node [{}] found itself. This connection will be closed.", uuid);
		client.sendMessage(new Message(MessageType.CLOSE_CONNECTION), Const.SEND_MESSAGE_TIMEOUT);
		if (client.isConnected()) {
			client.close();
		}
	}

	/**
	 * Закрывает соединение ноды к серверу из чужого кластера
	 *
	 * @param client         клиент
	 * @param anotherCluster какой-то другой кластер
	 * @throws NetworkExchangeException ошибка при отправке/получении сообщения
	 */
	private void closeOtherClusterConnection(TcpClient client, String anotherCluster) throws NetworkExchangeException {
		log.info("Discovered a node from another cluster '{}'. The connection will be closed.", anotherCluster);
		client.sendMessage(new Message(MessageType.CLOSE_CONNECTION), Const.SEND_MESSAGE_TIMEOUT);
		if (client.isConnected()) {
			client.close();
		}
	}

	/**
	 * Возвращает локальный hostname машины
	 */
	private String getLocalHostName() {
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("Could not find the hostname of the local machine", e);
		}
		return hostName;
	}

	/**
	 * Возвращает локальный IP-адрес машины
	 */
	private String getLocalHostAddress() {
		String address = null;
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.error("Could not find the IP-address of the local machine", e);
		}
        return Optional.ofNullable(address).orElse("localhost");
	}

	/**
	 * Выводит в лог текущий состав кластера: список нод
	 */
	private void printMembersList() {
		StringBuilder message = new StringBuilder("Node [" + uuid + "]. members: [\n");
		List<String> membersList = new ArrayList<>();
		// TODO: 02.04.2019 this.address == null, пока нода сама себя не обнаружит. Надо решить эту проблему.
        membersList.add(String.format("uuid: %s\t[%s:%d]\t-\tthis", uuid,
                Optional.ofNullable(this.address).orElse(getLocalHostAddress()), this.port));
		clients.forEach((uuid, client) -> membersList
				.add(String.format("uuid: %s\t[%s:%d]", uuid, client.getHost(), client.getPort())));

		final String join = String.join("\n", membersList);

		message.append(join).append("\n]");

		log.info(message.toString());
	}

	/**
	 * Останавливает ноду
	 *
	 * @throws IOException
	 */
	public void stop() throws IOException {
		log.info("Node [{}]. Attempting to stop node [{}:{}]", this.uuid, this.address, this.port);
		this.server.close();
		this.clients.values().forEach(TcpClient::close);
	}

	/**
	 * Возвращает признак остановленной ноды
	 *
	 * @return boolean true - если нода остановлена, false - в обратном случае
	 */
	public boolean isStopped() {
		return this.server.isStopped() && this.clients.values().stream().noneMatch(TcpClient::isConnected);
	}
}
