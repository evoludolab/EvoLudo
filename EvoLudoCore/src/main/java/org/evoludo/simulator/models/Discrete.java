package org.evoludo.simulator.models;

/**
 * Common interface for all models with discrete strategy sets.
 */
public interface Discrete {

	/**
	 * Calculate and return the payoff/score of individuals in monomorphic
	 * populations with trait/strategy {@code type} but also deals with payoff
	 * accounting (averaged versus accumulated).
	 *
	 * @param id   the id of the population for multi-species models
	 * @param type trait/strategy
	 * @return payoff/score in monomorphic population with trait/strategy
	 *         {@code type}. Returns {@code NaN} if scores ill defined
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#getMonoGameScore(int)
	 */
	public default double getMonoScore(int id, int type) {
		return Double.NaN;
	}
}
