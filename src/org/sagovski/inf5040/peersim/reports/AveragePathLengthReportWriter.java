package org.sagovski.inf5040.peersim.reports;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.reports.GraphObserver;
import peersim.util.IncrementalStats;

public class AveragePathLengthReportWriter extends GraphObserver implements ReportWriter {

	private final String AVG_PATH_LENGTH_OUTPUT_REPORT_PATH = PathUtils.getAbsolutePath("averageLengthReport");

	private static final String PAR_STEP = "step";
	private static final String PAR_NL = "nl";

	private final int step;
	private final int nl;

	public AveragePathLengthReportWriter(String name) {
		super(name);
		step = Configuration.getInt(name + "." + PAR_STEP, 0);
		nl = Configuration.getInt(name + "." + PAR_NL, 0);
		try {
			new File(AVG_PATH_LENGTH_OUTPUT_REPORT_PATH).delete();
			new File(AVG_PATH_LENGTH_OUTPUT_REPORT_PATH).createNewFile();
		} catch (IOException e) {
			System.err.println(ExceptionUtils.getStrStackTrace(e));
		}
	}

	private double getAveragePathLength() {
		IncrementalStats stats = new IncrementalStats();
		outerloop: for (int i = 0; i < nl && i < g.size(); ++i) {
			ga.dist(g, i);
			for (int j = 0; j < g.size(); ++j) {
				if (j == i)
					continue;
				if (ga.d[j] == -1) {
					stats.add(Double.POSITIVE_INFINITY);
					break outerloop;
				} else
					stats.add(ga.d[j]);
			}
		}
		return stats.getAverage();
	}

	@Override
	public boolean execute() {
		int cycleNumber = CommonState.getIntTime() / step;
		updateGraph();
		double averagePathLength = this.getAveragePathLength();
		this.writeEntry(cycleNumber + "\t" + averagePathLength);
		return false;
	}

	@Override
	public void writeEntry(String reportEntry) {
		try {
			Files.write(Paths.get(AVG_PATH_LENGTH_OUTPUT_REPORT_PATH), (reportEntry + "\n").getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.println(ExceptionUtils.getStrStackTrace(e));
		}
	}

}
