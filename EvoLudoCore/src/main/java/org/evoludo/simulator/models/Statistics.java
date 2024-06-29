package org.evoludo.simulator.models;

import org.evoludo.util.Formatter;

public interface Statistics {

// XXX NONE OF THE METHODS SHOULD PROBABLY HAVE DEFAULT

	/**
	 * Check if the current model settings permit sample statistics. Fixation
	 * probabilities and times are examples of statistics based on samples.
	 * 
	 * @return <code>true</code> if sample statistics are permitted
	 */
	public default boolean permitsSampleStatistics() {
		return false;
	}

	/**
	 * Check if the current model settings permit update statistics. Sojourn times
	 * are an example of statistics based on updates.
	 * 
	 * @return <code>true</code> if update statistics are permitted
	 */
	public default boolean permitsUpdateStatistics() {
		return false;
	}

	/**
	 * Gets the statistics sample.
	 * 
	 * @return the statistics sample
	 */
	public default FixationData getFixationData() {
		return null;
	}

	/**
	 * Reset statistics and get ready to start new collection.
	 */
	public default void resetStatisticsSample() {
	}

	/**
	 * Clear statistics sample and get ready to collect next sample.
	 */
	public default void initStatisticsSample() {
	}

	/**
	 * Signal that statistics sample is ready to process.
	 */
	public default void readStatisticsSample() {
	}

	/**
	 * Gets the number of statistics samples collected so far.
	 * 
	 * @return the number of samples
	 */
	public default int getNStatisticsSamples() {
		return 0;
	}

	/**
	 * EXPERIMENTAL: should mature into data structure useful for statistics
	 */
	public static class FixationData {

		/**
		 * Creates a new fixation data structure.
		 */
		public FixationData() {
		}

		/**
		 * The index of the node (location) where the initial mutant arose.
		 */
		public int mutantNode = -1;

		/**
		 * The strategy type of the initial mutant.
		 */
		public int mutantTrait = -1;

		/**
		 * The strategy type of the resident.
		 */
		public int residentTrait = -1;

		/**
		 * The strategy type that reached fixation.
		 */
		public int typeFixed = -1;

		/**
		 * The number of updates until fixation was reached.
		 */
		public double updatesFixed = -1.0;

		/**
		 * The time until fixation in realtime units.
		 */
		public double timeFixed = -1.0;

		/**
		 * The flag indicating whether the fixation data (probabilities) has been read.
		 */
		public boolean probRead = true;

		/**
		 * The flag indicating whether the fixation times have been read.
		 */
		public boolean timeRead = true;

		/**
		 * Reset the fixation data to get ready for the next sample.
		 */
		public void reset() {
			mutantNode = -1;
			mutantTrait = -1;
			residentTrait = -1;
			probRead = true;
			timeRead = true;
		}

		@Override
		public String toString() {
			return "{ mutantNode -> " + mutantNode + //
					", mutantTrait -> " + mutantTrait + //
					", residentTrait -> " + residentTrait + //
					", typeFixed -> " + typeFixed + //
					", updatesFixed -> " + Formatter.format(updatesFixed, 6) + //
					", timeFixed -> " + Formatter.format(timeFixed, 6) + //
					", probRead -> " + probRead + //
					", timeRead -> " + timeRead + " }";
		}
	}
}
