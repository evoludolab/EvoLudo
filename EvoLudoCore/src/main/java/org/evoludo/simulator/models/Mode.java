package org.evoludo.simulator.models;

/**
 * Modes of the model. Currently thefollowing modes are supported:
 * <dl>
 * <dt>DYNAMICS
 * <dd>follow the time series of the model. This is the default.
 * <dt>STATISTICS_SAMPLE
 * <dd>generate samples to create statistics of the model. Run model until it
 * stops and advertise that a new data point is available. Start next sample,
 * once the data is retrieved and processed.
 * <dt>STATISTICS_UPDATE
 * <dd>generate samples from single run to create statistics of the
 * model reflecting the different states of the population.
 * </dl>
 */
public enum Mode {
	/**
	 * Dynamics: follow the time series of the model.
	 */
	DYNAMICS("dynamics"), //

	/**
	 * Statistics: generate samples to create statistics of the model. Run model
	 * until it stops and advertise that a new data point is available. Start
	 * next sample, once the data is retrieved and processed.
	 */
	STATISTICS_SAMPLE("statistics_sample"), //

	/**
	 * Statistics: generate samples from single run to create statistics of the
	 * model reflecting the different states of the population.
	 */
	STATISTICS_UPDATE("statistics_update"); //

	/**
	 * Identifying id of the type of mode.
	 */
	String id;

	/**
	 * Construct an enum for the type of mode.
	 * 
	 * @param id the identifier of the mode
	 */
	Mode(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}
}
