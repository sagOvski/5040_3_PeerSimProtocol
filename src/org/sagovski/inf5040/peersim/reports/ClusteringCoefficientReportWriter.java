package org.sagovski.inf5040.peersim.reports;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.graph.GraphAlgorithms;
import peersim.reports.GraphObserver;
import peersim.util.IncrementalStats;

public class ClusteringCoefficientReportWriter extends GraphObserver implements ReportWriter {

	private final String OUTPUT_REPORT_PATH = PathUtils.getAbsolutePath("clusteringCoefficientReport");

	// private static final String PAR_CACHESIZE = "cacheSize";
	private static final String PAR_STEP = "step";
	// private static final String PAR_TYPE = "type";
	private static final String PAR_NC = "nc";

	// private final int cacheSize;
	private final int step;
	// private final String type;
	private final int nc;

	public ClusteringCoefficientReportWriter(String name) {
		super(name);
		// String prefix = name + ".";
		// cacheSize = Configuration.getInt(prefix + PAR_CACHESIZE);
		// type = Configuration.getString(prefix + PAR_TYPE);
		step = Configuration.getInt(name + "." + PAR_STEP, 0);
		nc = Configuration.getInt(name + "." + PAR_NC, 0);
		try {
			new File(OUTPUT_REPORT_PATH).delete();
			new File(OUTPUT_REPORT_PATH).createNewFile();
		} catch (IOException e) {
			System.err.println(ExceptionUtils.getStrStackTrace(e));
		}
	}

	private double getAverageClusteringCoefficient() {
		IncrementalStats stats = new IncrementalStats();
		final int n = nc < 0 ? g.size() : nc;
		for (int i = 0; i < n && i < g.size(); ++i) {
			stats.add(GraphAlgorithms.clustering(g, i));
		}
		return stats.getAverage();
	}

	@Override
	public boolean execute() {
		int cycle = CommonState.getIntTime() / step;
		updateGraph();
		if (nc != 0) {
			double avg = getAverageClusteringCoefficient();
			this.writeEntry(cycle + "\t" + avg);
		}
		return false;
	}

	@Override
	public void writeEntry(final String reportEntry) {
		try {
			Files.write(Paths.get(OUTPUT_REPORT_PATH), (reportEntry + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.println(ExceptionUtils.getStrStackTrace(e));
		}
	}

}
