package org.evoludo.simulator.views;

public interface BasicTooltipProvider {

	/**
	 * Get the tooltip at the scaled coordinates {@code (x,y)} with the origin in
	 * the lower left corner of the graph.
	 * 
	 * @param x the {@code x} coordinate
	 * @param y the {@code y} coordinate
	 * @return the tooltip
	 */
	public default String getTooltipAt(double x, double y) {
		return null;
	}

	/**
	 * Get the tooltip for the location with index {@code index}. The index typically
	 * refers to an individual node but may equally refer to a location on a lattice
	 * for PDE models or trait distributions.
	 * 
	 * @param index the {@code index} coordinate
	 * @param y     the {@code y} coordinate
	 * @return the tooltip
	 */
	public default String getTooltipAt(int index) {
		return null;
	}
}
