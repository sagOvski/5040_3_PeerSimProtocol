package example.gossip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

/**
 * @author Lucas Provensi
 * 
 *         Basic Shuffling protocol template
 * 
 *         The basic shuffling algorithm, introduced by Stavrou et al in the
 *         paper: "A Lightweight, Robust P2P System to Handle Flash Crowds", is
 *         a simple peer-to-peer communication model. It forms an overlay and
 *         keeps it connected by means of an epidemic algorithm. The protocol is
 *         extremely simple: each peer knows a small, continuously changing set
 *         of other peers, called its neighbors, and occasionally contacts a
 *         random one to exchange some of their neighbors.
 * 
 *         This class is a template with instructions of how to implement the
 *         shuffling algorithm in PeerSim. Should make use of the classes Entry
 *         and GossipMessage: Entry - Is an entry in the cache, contains a
 *         reference to a neighbor node and a reference to the last node this
 *         entry was sent to. GossipMessage - The message used by the protocol.
 *         It can be a shuffle request, reply or reject message. It contains the
 *         originating node and the shuffle list.
 *
 */
public class BasicShuffle implements Linkable, EDProtocol, CDProtocol {

	private static final String PAR_CACHE = "cacheSize";
	private static final String PAR_L = "shuffleLength";
	private static final String PAR_TRANSPORT = "transport";

	private final int tid;

	// The list of neighbors known by this node, or the cache.
	private List<Entry> cache;

	// The maximum size of the cache;
	private final int size;

	// The maximum length of the shuffle exchange;
	private final int l;

	private boolean awaitingResponseFromPreviousShuffle = false;

	private boolean isNodeRemoved = false;

	/**
	 * Constructor that initializes the relevant simulation parameters and other
	 * class variables.
	 * 
	 * @param n
	 *            simulation parameters
	 */
	public BasicShuffle(String n) {
		this.size = Configuration.getInt(n + "." + PAR_CACHE);
		this.l = Configuration.getInt(n + "." + PAR_L);
		this.tid = Configuration.getPid(n + "." + PAR_TRANSPORT);

		cache = new ArrayList<Entry>(size);
	}

	/*
	 * START YOUR IMPLEMENTATION FROM HERE
	 * 
	 * The simulator engine calls the method nextCycle once every cycle (specified
	 * in time units in the simulation script) for all the nodes.
	 * 
	 * You can assume that a node initiates a shuffling operation every cycle.
	 * 
	 * @see peersim.cdsim.CDProtocol#nextCycle(peersim.core.Node, int)
	 */
	@Override
	public void nextCycle(Node node, int protocolID) {
		// Implement the shuffling protocol using the following steps (or
		// you can design a similar algorithm):
		// Let's name this node as P

		// 1. If P is waiting for a response from a shuffling operation initiated in a
		// previous cycle, return;
		if (true == awaitingResponseFromPreviousShuffle) {
			return;
		}

		// 2. If P's cache is empty, return;
		if (cache.isEmpty()) {
			return;
		}

		// 3. Select a random neighbor (named Q) from P's cache to initiate the
		// shuffling;
		// - You should use the simulator's common random source to produce a random
		// number: CommonState.r.nextInt(cache.size())
		int randomIndex = CommonState.r.nextInt(cache.size());
		Entry qNode = cache.get(randomIndex);

		// 4. If P's cache is full, remove Q from the cache;
		if (cache.size() >= size) {
			cache.remove(qNode);
			isNodeRemoved = true;
		}

		// 5. Select a subset of other l - 1 random neighbors from P's cache;
		// - l is the length of the shuffle exchange
		// - Do not add Q to this subset
		List<Entry> randomSubset = new ArrayList<Entry>(l);

		// Creating a tempCache to make sure qNode is not included in the subset
		List<Entry> tempCache = new ArrayList<Entry>(cache);
		tempCache.remove(qNode);
		for (int iteration = 0; (iteration < l) && (false == tempCache.isEmpty()); ++iteration) {
			final Entry randomEntry = tempCache.remove(CommonState.r.nextInt(tempCache.size()));
			randomSubset.add(randomEntry);
		}

		// 6. Add P to the subset;
		Entry pNode = new Entry(node);
		randomSubset.add(pNode);

		// 7. Send a shuffle request to Q containing the subset;
		// - Keep track of the nodes sent to Q
		// - Example code for sending a message:
		GossipMessage message = new GossipMessage(node, randomSubset);
		message.setType(MessageType.SHUFFLE_REQUEST);
		Transport tr = (Transport) node.getProtocol(tid);
		tr.send(node, qNode.getNode(), message, protocolID);

		// 8. From this point on P is waiting for Q's response and will not initiate a
		// new shuffle operation;
		awaitingResponseFromPreviousShuffle = true;

		// The response from Q will be handled by the method processEvent.

	}

	private void updateCache(final Node node, final List<Entry> neighbors) {
		// - No neighbor appears twice in the cache
		// - Use empty cache slots to add the new entries
		// - If the cache is full, you can replace entries among the ones sent to P with
		// the new ones
		Set<Entry> combinedSet = new HashSet<Entry>(cache);
		combinedSet.addAll(neighbors);
		if (combinedSet.size() <= size) {
			this.cache = new ArrayList<Entry>(combinedSet);
		} else {
			int emptyCount = size - cache.size();
			this.cache.addAll(neighbors.subList(0, emptyCount));
		}
	}

	/*
	 * The simulator engine calls the method processEvent at the specific time unit
	 * that an event occurs in the simulation. It is not called periodically as the
	 * nextCycle method.
	 * 
	 * You should implement the handling of the messages received by this node in
	 * this method.
	 * 
	 * @see peersim.edsim.EDProtocol#processEvent(peersim.core.Node, int,
	 * java.lang.Object)
	 */
	@Override
	public void processEvent(Node node, int pid, Object event) {
		// Let's name this node as Q;
		// Q receives a message from P;
		// - Cast the event object to a message:
		GossipMessage message = (GossipMessage) event;
		Node pNode = message.getNode();

		switch (message.getType()) {

		// If the message is a shuffle request:
		case SHUFFLE_REQUEST:
			// 1. If Q is waiting for a response from a shuffling initiated in a previous
			// cycle, send back to P a message rejecting the shuffle request;

			if (true == awaitingResponseFromPreviousShuffle) {
				GossipMessage rejectMessage = new GossipMessage(node, null);
				rejectMessage.setType(MessageType.SHUFFLE_REJECTED);
				Transport tr = (Transport) node.getProtocol(tid);
				tr.send(node, pNode, rejectMessage, pid);
				return;
			}

			// 2. Q selects a random subset of size l of its own neighbors;
			List<Entry> randomSubset = new ArrayList<Entry>(l);
			List<Entry> tempCache = new ArrayList<Entry>(cache);
			tempCache.remove(new Entry(pNode));
			for (int iteration = 0; (iteration < l) && (false == tempCache.isEmpty()); ++iteration) {
				randomSubset.add(tempCache.remove(CommonState.r.nextInt(tempCache.size())));
			}

			// 3. Q reply P's shuffle request by sending back its own subset;
			GossipMessage replyMessage = new GossipMessage(node, randomSubset);
			replyMessage.setType(MessageType.SHUFFLE_REPLY);
			Transport tr = (Transport) node.getProtocol(tid);
			tr.send(node, pNode, replyMessage, pid);

			// 4. Q updates its cache to include the neighbors sent by P:
			// - No neighbor appears twice in the cache
			// - Use empty cache slots to add the new entries
			// - If the cache is full, you can replace entries among the ones sent to P with
			// the new ones
			this.updateCache(pNode, message.getShuffleList());

			break;

		// If the message is a shuffle reply:
		case SHUFFLE_REPLY:
			// 1. In this case Q initiated a shuffle with P and is receiving a response
			// containing a subset of P's neighbors
			// 2. Q updates its cache to include the neighbors sent by P:
			// - No neighbor appears twice in the cache
			// - Use empty cache slots to add new entries
			// - If the cache is full, you can replace entries among the ones originally
			// sent to P with the new ones
			this.updateCache(pNode, message.getShuffleList());

			// 3. Q is no longer waiting for a shuffle reply;
			awaitingResponseFromPreviousShuffle = false;
			isNodeRemoved = false;

			break;

		// If the message is a shuffle rejection:
		case SHUFFLE_REJECTED:
			// 1. If P was originally removed from Q's cache, add it again to the cache.
			if (isNodeRemoved) {
				cache.add(new Entry(pNode));
				isNodeRemoved = false;
			}

			// 2. Q is no longer waiting for a shuffle reply;
			awaitingResponseFromPreviousShuffle = false;

			break;

		default:
			break;
		}

	}

	/*
	 * The following methods are used only by the simulator and don't need to be
	 * changed
	 */

	@Override
	public int degree() {
		return cache.size();
	}

	@Override
	public Node getNeighbor(int i) {
		return cache.get(i).getNode();
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		if (contains(neighbour))
			return false;

		if (cache.size() >= size)
			return false;

		Entry entry = new Entry(neighbour);
		cache.add(entry);

		return true;
	}

	@Override
	public boolean contains(Node neighbor) {
		return cache.contains(new Entry(neighbor));
	}

	public Object clone() {
		BasicShuffle gossip = null;
		try {
			gossip = (BasicShuffle) super.clone();
		} catch (CloneNotSupportedException e) {

		}
		gossip.cache = new ArrayList<Entry>();

		return gossip;
	}

	@Override
	public void onKill() {
		// TODO Auto-generated method stub
	}

	@Override
	public void pack() {
		// TODO Auto-generated method stub
	}
}
