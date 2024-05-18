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
	public String getTooltipAt(double x, double y);
}
