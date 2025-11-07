package org.evoludo.util;

/**
 * Interface to process command line arguments
 */
public interface CLODelegate {

	/**
	 * Parse string <code>arg</code> and set configurable parameters that correspond
	 * to this command line option. The delegate for options with
	 * {@code Argument.NONE} must implement this method.
	 * <p>
	 * <strong>Note:</strong> returning <code>false</code> triggers a warning about
	 * which command line option failed to correctly parse. If the parser can
	 * rectify the issue on the spot this is also acceptable. In that case the
	 * method should return {@code true} and possibly log the fact that
	 * parameters have been adjusted.
	 * 
	 * @param isSet {@code true} if option was set on command line
	 * @return {@code true} if parsing successful
	 */
	public default boolean parse(boolean isSet) {
		throw new UnsupportedOperationException("parse(boolean) not implemented");
	}

	/**
	 * Parse string <code>arg</code> and set configurable parameters that correspond
	 * to this command line option. The delegate for options with
	 * {@code Argument.REQUIRED} must implement this method.
	 * <p>
	 * <strong>Note:</strong> returning <code>false</code> triggers a warning about
	 * which command line option failed to correctly parse. If the parser can
	 * rectify the issue on the spot this is also acceptable. In that case the
	 * method should return {@code true} and possibly log the fact that
	 * parameters have been adjusted.
	 * 
	 * @param arg       the argument for parsing by command line option
	 * @param isDefault {@code true} if arg is default
	 * @return {@code true} if parsing successful
	 */
	public default boolean parse(String arg) {
		throw new UnsupportedOperationException("parse(String) not implemented");
	}

	/**
	 * Parse string <code>arg</code> and set configurable parameters that correspond
	 * to this command line option. The delegate for options with
	 * {@code Argument.OPTIONAL} must implement this method.
	 * <p>
	 * <strong>Note:</strong> returning <code>false</code> triggers a warning about
	 * which command line option failed to correctly parse. If the parser can
	 * rectify the issue on the spot this is also acceptable. In that case the
	 * method should return {@code true} and possibly log the fact that
	 * parameters have been adjusted.
	 * 
	 * @param arg       the argument for parsing by command line option
	 * @param isDefault {@code true} if arg is default
	 * @return {@code true} if parsing successful
	 */
	public default boolean parse(String arg, boolean isSet) {
		throw new UnsupportedOperationException("parse(String, boolean) not implemented");
	}

	/**
	 * If settings for option are not known upon initialization, an up-to-date
	 * description is requested when needed (e.g. if help is requested, typically
	 * using <code>--help</code> option).
	 * <p>
	 * <strong>Note:</strong> the description string may contain any UTF-8
	 * characters as well as HTML character entities. If necessary they will be
	 * escaped and converted to UTF-8 for display in XML documents.
	 *
	 * @return description of command line option.
	 */
	public default String getDescription() {
		return null;
	}

	/**
	 * Optional: position of key in the list of arguments. Used in help display.
	 * 
	 * @return the position of the key
	 */
	public default int getKeyPos() {
		return 0;
	}
}