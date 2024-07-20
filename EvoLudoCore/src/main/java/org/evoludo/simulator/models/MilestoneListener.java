package org.evoludo.simulator.models;

/**
 * All <code>MilestoneListener</code>'s get informed about milestones and state
 * changes of the core of EvoLudo modules. The implementation of all methods is
 * optional.
 *
 * @author Christoph Hauert
 */
public interface MilestoneListener {

	/**
	 * Called when EvoLudo module finished loading.
	 */
	public default void moduleLoaded() {
	}

	/**
	 * Called when EvoLudo module is unloading.
	 */
	public default void moduleUnloaded() {
	}

	/**
	 * Called when the state of the EvoLudo module has been restored.
	 */
	public default void moduleRestored() {
	}

	/**
	 * Called when EvoLudo model finished loading.
	 */
	public default void modelLoaded() {
	}

	/**
	 * Called when EvoLudo model is unloading.
	 */
	public default void modelUnloaded() {
	}

	/**
	 * Called when the EvoLudo model starts running.
	 */
	public default void modelRunning() {
	}

	/**
	 * Called after the EvoLudo model has relaxed.
	 */
	public default void modelRelaxed() {
	}

	/**
	 * Called after a running EvoLudo model stopped because the model converged (or
	 * reached an absorbing state).
	 */
	public default void modelStopped() {
	}

	/**
	 * Called after the EvoLudo model got re-initialized.
	 */
	public default void modelDidReinit() {
	}

	/**
	 * Called after the EvoLudo model was reset.
	 */
	public default void modelDidReset() {
	}
}