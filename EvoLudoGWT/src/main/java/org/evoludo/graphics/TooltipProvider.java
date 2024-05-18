package org.evoludo.graphics;

public interface TooltipProvider {

	public interface Parametric extends TooltipProvider {

		/**
		 * Get the tooltip for the parametric plot {@code graph} at the scaled
		 * coordinates {@code (x,y)} with the origin in the lower left corner of the
		 * phase plane.
		 * 
		 * @param node the index of the node
		 * @return the tooltip for the node
		 */
		public String getTooltipAt(ParaGraph graph, double x, double y);
	}

	public interface Index extends TooltipProvider {

		/**
		 * Get the tooltip for the location with index {@code node}. The index typically
		 * refers to an individual node but may equally refer to a location on a lattice
		 * for PDE models or 2D trait distributions.
		 * 
		 * @param node the index of the node
		 * @return the tooltip for the node
		 */
		public <B> String getTooltipAt(AbstractGraph<B> graph, int node);
	}

	public interface Simplex extends TooltipProvider {

		/**
		 * Get the tooltip for the simplex {@code graph} at the scaled coordinates
		 * {@code (x,y)} with the origin in the lower left corner of the simplex.
		 * 
		 * @param node the index of the node
		 * @return the tooltip for the node
		 */
		public String getTooltipAt(S3Graph graph, double x, double y);
	}
}
