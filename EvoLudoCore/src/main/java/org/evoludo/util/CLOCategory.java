package org.evoludo.util;

/**
 * Handle different categories of options. This is mostly used to provide a more
 * readable and useful help screen for options organized in categories.
 */
public class CLOCategory {

	/**
	 * The brief category description. Section header in help screen.
	 * <p>
	 * <strong>Note:</strong> the description string may contain any UTF-8
	 * characters as well as HTML character entities. If necessary they will be
	 * escaped and converted to UTF-8 for display in XML documents.
	 */
	String header;

	/**
	 * The priority of this category. Higher priorities are printed first.
	 */
	int priority;

	/**
	 * Create a new category with the header {@code header}. The priority is set to
	 * {@code 0}.
	 * 
	 * @param header the header of the category
	 */
	public CLOCategory(String header) {
		this(header, 0);
	}

	/**
	 * Create a new category with {@code header} and {@code priority}.
	 * 
	 * @param header   the header of the category
	 * @param priority the priority of the category
	 */
	public CLOCategory(String header, int priority) {
		this.header = header;
		this.priority = priority;
	}

	/**
	 * Get the priority of this category. Parameters are grouped by priority in help
	 * display.
	 * 
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Set the header of category for help display.
	 * 
	 * @param header the header
	 */
	public void setHeader(String header) {
		this.header = header;
	}

	/**
	 * Get the header of category for help display.
	 * 
	 * @return the header
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * The category for global options.
	 */
	public static final CLOCategory Global = new CLOCategory("Global options:", 50);

	/**
	 * The category for user interface specific options.
	 */
	public static final CLOCategory GUI = new CLOCategory("User interface specific options:", 10);

	/**
	 * The category for simulation specific options.
	 */
	public static final CLOCategory Simulation = new CLOCategory("Simulation specific options:", 20);

	/**
	 * The category for model specific options.
	 */
	public static final CLOCategory Model = new CLOCategory("Model specific options:", 30);

	/**
	 * The category for module specific options.
	 */
	public static final CLOCategory Module = new CLOCategory("Module specific options:", 40);
}