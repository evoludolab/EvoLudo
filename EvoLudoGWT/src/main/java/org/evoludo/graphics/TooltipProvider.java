package org.evoludo.graphics;

public interface TooltipProvider {

	public interface PopGraph {

		/**
		 * Get the tooltip for the node with index {@code node}.
		 * 
		 * @param node the index of the node
		 * @return the tooltip for the node
		 */
		public String getTooltipAt(AbstractGraph graph, int node);
	}

	public interface S3 {

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
