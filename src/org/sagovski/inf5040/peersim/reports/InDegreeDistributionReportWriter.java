package org.sagovski.inf5040.peersim.reports;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.reports.GraphObserver;

public class InDegreeDistributionReportWriter extends GraphObserver implements ReportWriter {

	private static final Logger logger = LogManager.getLogger(InDegreeDistributionReportWriter.class);

	private final String OUTPUT_REPORT_PATH = PathUtils.getAbsolutePath("inDegreeDistributionReport");

	private final static String PAR_PID = "protocol";
	private static final String PAR_START_PROTOCOL = "starttime";
	private static final String PAR_END_PROTOCOL = "endtime";

	private final int pid;
	private final long startTime;
	private final long endTime;

	public InDegreeDistributionReportWriter(String name) {
		super(name);
		this.pid = Configuration.getPid(name + "." + PAR_PID);
		this.startTime = Configuration.getLong(name + "." + PAR_START_PROTOCOL, Long.MIN_VALUE);
		this.endTime = Configuration.getLong(name + "." + PAR_END_PROTOCOL, Long.MAX_VALUE);
		try {
			new File(OUTPUT_REPORT_PATH).delete();
			new File(OUTPUT_REPORT_PATH).createNewFile();
		} catch (IOException e) {
			System.err.println(ExceptionUtils.getStrStackTrace(e));
		}
	}

	@Override
	public void writeEntry(String reportEntry) {
		try {
			Files.write(Paths.get(OUTPUT_REPORT_PATH), (reportEntry + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.println(ExceptionUtils.getStrStackTrace(e));
		}
	}

	@Override
	public boolean execute() {

		if ((CommonState.getTime() >= endTime) || (CommonState.getTime() < startTime))
			return false;

		// Map of all nodes and their in-degree count
		Map<Long, Integer> degreeCount = new HashMap<Long, Integer>();

		for (int i = 0; i < Network.size(); i++) {
			// Get all the nodes in the network
			Node n = Network.get(i);

			if (n.isUp()) {
				// Get the linkable protocol for all the running nodes
				Linkable linkable = (Linkable) n.getProtocol(pid);
				// Go through the neighbor list and update the degrees in the map
				for (int j = 0; j < linkable.degree(); j++) {
					Long nodeId = linkable.getNeighbor(j).getID();
					Integer count = degreeCount.get(nodeId);
					if (count == null)
						degreeCount.put(nodeId, 1);
					else
						degreeCount.put(nodeId, count + 1);
				}
			}
		}

		// Map of the in-degree distribution. The key is the in-degree and the
		// entry is the number of nodes having this distribution
		Map<Integer, Integer> dist = new HashMap<Integer, Integer>();

		// Fill the map with the in-degree distribution of each node
		for (int i = 0; i < Network.size(); i++) {
			Long nodeId = Network.get(i).getID();
			Integer degree = degreeCount.get(nodeId);
			int value = 1;
			if (dist.containsKey(degree))
				value = dist.get(degree) + 1;
			dist.put(degree, value);
		}

		if (dist.keySet() == null) {
			return false;
		}
		try {
			// Sort the distribution and print the result
			SortedSet<Integer> sortedKeys = new TreeSet<Integer>(dist.keySet());
			for (int i = 0; i <= sortedKeys.last(); i++) {
				if (sortedKeys.contains(i))
					this.writeEntry(i + "\t" + dist.get(i));
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStrStackTrace(e));
			return false;
		}
		return false;
	}

}
