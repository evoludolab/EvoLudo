package org.evoludo.simulator.models;

/**
 * Common interface for all models with continuous strategy sets.
 */
public interface Continuous {

	/**
	 * Gets the minimum trait values in this module.
	 * 
	 * @param id the id of the population for multi-species models
	 * @return the array with the minimum trait values
	 */
	public double[] getTraitMin(int id);

	/**
	 * Gets the maximum trait values in this module.
	 * 
	 * @param id the id of the population for multi-species models
	 * @return the array with the maximum trait values
	 */
	public double[] getTraitMax(int id);

	/**
	 * Calculates and returns minimum score in monomorphic population. This depends
	 * on the payoff accounting (averaged versus accumulated) as well as the
	 * {@link Geometry}. Since modules are agnostic of runtime details, the request
	 * is simply forwarded to the current {@link Model} together with the species ID
	 * for multi-species modules.
	 * 
	 * @param id the id of the population for multi-species models
	 * @return the minimum monomorphic score
	 */
	public double getMinMonoScore(int id);

	/**
	 * Calculates and returns maximum score in monomorphic population. This depends
	 * on the payoff accounting (averaged versus accumulated) as well as the
	 * {@link Geometry}. Since modules are agnostic of runtime details, the request
	 * is simply forwarded to the current {@link Model} together with the species ID
	 * for multi-species modules.
	 * 
	 * @param id the id of the population for multi-species models
	 * @return the maximum monomorphic score
	 */
	public double getMaxMonoScore(int id);

	/**
	 * Gets the histogram of the trait distributions and returns the data in an
	 * array <code>bins</code>, where the first index denotes the trait (in case
	 * there are multiple) and the second index refers to the bins in the histogram.
	 * <p>
	 * This is a helper method to forward the request to the appropriate
	 * {@link IBSMCPopulation} (for multiple traits) or {@link IBSCPopulation} (for
	 * single traits).
	 *
	 * @param id   the id of the population for multi-species models
	 * @param bins the 2D data array for storing the histogram
	 */
	public void getTraitHistogramData(int id, double[][] bins);

	/**
	 * Gets the histogram of the trait distribution for the traits of the
	 * {@link Module}. For modules with multiple traits a 2D histogram is generated
	 * for traits <code>trait1</code> and <code>trait2</code>. The histogram is
	 * returned in the linear array <code>bins</code> and arranged in a way that is
	 * compatible with square lattice geometries for visualization by
	 * {@link org.evoludo.simulator.views.Distribution} (GWT only). For modules with
	 * a single trait only, <code>trait1</code> and <code>trait2</code> are ignored.
	 *
	 * @param id     the id of the population for multi-species models
	 * @param bins   the data array for storing the histogram
	 * @param trait1 the index of the first trait (horizontal axis)
	 * @param trait2 the index of the second trait (vertical axis)
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometrySquare()
	 */
	public void get2DTraitHistogramData(int id, double[] bins, int trait1, int trait2);
}