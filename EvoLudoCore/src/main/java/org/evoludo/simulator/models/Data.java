package org.evoludo.simulator.models;

/**
 * Data types that are handled by the model. Currently the following data types
 * are supported:
 * <dl>
 * <dt>Strategy
 * <dd>the data represents strategies.
 * <dt>Fitness
 * <dd>the data represents payoffs/scores/fitness.
 * <dt>Degree
 * <dd>the data represents degrees of the network structure.
 * <dt>Fixation probability
 * <dd>the data represents fixation probabilities.
 * <dt>Fixation time
 * <dd>the data represents fixation times.
 * <dt>Stationary distribution
 * <dd>the data represents the stationary strategy distribution.
 * <dt>undefined
 * <dd>the data type is not defined/unknown.
 * </dl>
 */
public enum Data {

	/**
	 * Undefined: the data type is not defined/unknown.
	 */
	UNDEFINED("undefined"), //

	/**
	 * Strategy: the data represents strategies.
	 */
	STRATEGY("Strategies - Histogram"), //

	/**
	 * Fitness: the data represents payoffs/scores/fitness.
	 */
	FITNESS("Fitness - Histogram"), //

	/**
	 * Degree: the data represents degrees of the network structure.
	 */
	DEGREE("Structure - Degree"), //

	/**
	 * Fixation probability: the data represents fixation probabilities.
	 */
	STATISTICS_FIXATION_PROBABILITY("Statistics - Fixation probability"), //

	/**
	 * Fixation time: the data represents fixation times.
	 */
	STATISTICS_FIXATION_TIME("Statistics - Fixation time"), //

	/**
	 * Stationary distribution.
	 */
	STATISTICS_STATIONARY("Statistics - Stationary distribution"); //

	/**
	 * Identifying id of the type of data.
	 */
	String id;

	/**
	 * Construct an enum for the type of data.
	 * 
	 * @param id the identifier of the data type
	 */
	Data(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}

	/**
	 * Checks if the data type is a statistics type.
	 * 
	 * @return <code>true</code> for statistics data types
	 */
	public boolean isStatistics() {
		return (this == STATISTICS_FIXATION_PROBABILITY || this == STATISTICS_FIXATION_TIME
				|| this == STATISTICS_STATIONARY);
	}
}
