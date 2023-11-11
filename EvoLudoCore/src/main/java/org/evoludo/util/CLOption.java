//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For publications in any form, you are kindly requested to attribute the
// author and project as follows:
//
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.util;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * Command line option and argument.
 *
 * @author Christoph Hauert
 */
public class CLOption implements Comparable<CLOption> {

	/**
	 * Interface to process command line arguments
	 */
	public interface CLODelegate {

		/**
		 * Parse string <code>arg</code> and set configurable parameters that correspond
		 * to this command line option.
		 * <p>
		 * <strong>Note:</strong> should only return <code>false</code> if a warning or
		 * other information was logged.
		 * </p>
		 * 
		 * @param arg for parsing by command line option
		 * @return <code>true</code> if parsing successful
		 */
		public boolean parse(String arg);

		/**
		 * Report settings of configurable parameters that correspond to this command
		 * line option (optional implementation).
		 * 
		 * @param output the outlet to send reports to
		 */
		public default void report(PrintStream output) {
		}

		/**
		 * If settings for option are not known upon initialization, an up-to-date
		 * description is requested when needed (e.g. if help is requested, typically
		 * using <code>--help</code> options).
		 * 
		 * @return description of command line option.
		 */
		public default String getDescription() {
			return null;
		};
	}

	/**
	 * Types of command line options:
	 * <dl>
	 * <dt>REQUIRED</dt>
	 * <dd>required argument. Must be separated from command line option name by
	 * whitespace or <code>'='</code>.</dd>
	 * <dt>OPTIONAL</dt>
	 * <dd>optional argument. If present, must be separated from command line option
	 * name by whitespace or <code>'='</code>.</dd>
	 * <dt>NONE</dt>
	 * <dd>no argument.</dd>
	 * </dl>
	 */
	public enum Argument {
		/**
		 * <code>REQUIRED</code>: required argument. Must be separated from command line
		 * option name by whitespace or <code>'='</code>.
		 */
		REQUIRED,

		/**
		 * <code>OPTIONAL</code>: optional argument. If present, must be separated from
		 * command line option name by whitespace or
		 * <code>'='</code>.
		 */
		OPTIONAL,

		/**
		 * <code>NONE</code>: no argument.
		 */
		NONE;
	}

	public static class SimpleKey implements Key {
		String key;
		String title;

		public SimpleKey(String key, String title) {
			this.key = key;
			this.title = title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}
	}

	public interface Key {

		/**
		 * Return key of in this collection. Used when parsing command line options.
		 * 
		 * @return the key of the command line option
		 */
		public String getKey();

		/**
		 * Brief description of purpose of this key. Used in GUI and in help display.
		 * 
		 * @return the title of the command line option
		 * 
		 * @see org.evoludo.simulator.EvoLudo#helpCLO()
		 */
		public String getTitle();

		/**
		 * Optional: long description of purpose of this key. Used in help display.
		 * Defaults to brief description.
		 * 
		 * @return the description of the command line option
		 * 
		 * @see #getTitle()
		 * @see org.evoludo.simulator.EvoLudo#helpCLO()
		 */
		public default String getDescription() {
			return getTitle();
		}
	}

	/**
	 * Handle different categories of options. This is mostly used to provide a more
	 * readable and useful help screen for options organized in categories.
	 */
	public static class Category {

		/**
		 * The brief category description. Section header in help screen.
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
		public Category(String header) {
			this(header, 0);
		}

		/**
		 * Create a new category with {@code header} and {@code priority}.
		 * 
		 * @param header   the header of the category
		 * @param priority the priority of the category
		 */
		public Category(String header, int priority) {
			this.header = header;
			this.priority = priority;
		}

		/**
		 * Get the priority.
		 * 
		 * @return the priority
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Get the header.
		 * 
		 * @return the header
		 */
		public String getHeader() {
			return header;
		}
	}

	/**
	 * Counter to assign every option a unique identifier.
	 */
	private static int uniqueID = 0;

	/**
	 * Unique identifier of command line option (currently unused).
	 */
	final int ID;

	/**
	 * The name of the command line option (required).
	 */
	final String name;

	/**
	 * The type of the command line option with no, optional, or required argument.
	 */
	final Argument type;

	/**
	 * The category of the command line option. Used to structure the help screen.
	 */
	final Category category;

	/**
	 * the short description of the command line option. May include newline's
	 * <code>'\n'</code> for basic formatting but no HTML or other formatting.
	 */
	String description = null;

	/**
	 * The argument provided on the command line (if any).
	 */
	String optionArg = null;

	/**
	 * The default argument for option (if applicable).
	 */
	String defaultArg = null;

	/**
	 * The list of valid keys (if applicable).
	 */
	HashMap<String, Key> keys = null;

	/**
	 * The flag to indicate if keys were inherited from another option. If
	 * <code>true</code> the keys will not be printed as part of the description.
	 */
	boolean inheritedKeys = false;

	/**
	 * <code>true</code> if option was set on command line.
	 */
	boolean isSet = false;

	/**
	 * The delegate for parsing arguments, reporting settings and retrieving
	 * customized
	 * descriptions.
	 */
	private CLODelegate delegate;

	/**
	 * Creates command line option with the name <code>name</code> and the
	 * <code>delegate</code> to process the argument and provide the description.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li><code>delegate</code> must implement {@link CLODelegate#getDescription()}
	 * to provide option description.</li>
	 * </ul>
	 * 
	 * @param name     the name of the command line option
	 * @param delegate delegate for processing command line argument
	 */
	public CLOption(String name, CLODelegate delegate) {
		this(name, null, Argument.NONE, null, null, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (no arguments)
	 * with category {@code category} and the <code>delegate</code> to process the
	 * argument and retrieve the description.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li><code>delegate</code> must implement {@link CLODelegate#getDescription()}
	 * to provide option description.</li>
	 * </ul>
	 * 
	 * @param name     the name of the command line option
	 * @param category the category of option
	 * @param delegate delegate for processing command line argument
	 */
	public CLOption(String name, Category category, CLODelegate delegate) {
		this(name, null, Argument.NONE, category, null, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (no arguments)
	 * and short description <code>description</code> as well as the delegate
	 * <code>delegate</code> to process the argument and optionally retrieve the
	 * description.
	 * <p>
	 * <strong>Note:</strong> on the command line option names need to be preceded
	 * by <code>--</code>, e.g. <code>--help</code>.
	 * 
	 * @param name        the name of the command line option
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String description, CLODelegate delegate) {
		this(name, null, Argument.NONE, null, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (no arguments)
	 * with category {@code category} and brief description <code>description</code>
	 * as well as the delegate <code>delegate</code> to process the argument and
	 * optionally retrieve the description.
	 * <p>
	 * <strong>Note:</strong> on the command line option names need to be preceded
	 * by <code>--</code>, e.g. <code>--help</code>.
	 * 
	 * @param name        the name of the command line option
	 * @param category    the category of option
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, Category category, String description, CLODelegate delegate) {
		this(name, null, Argument.NONE, category, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (with required
	 * argument), which defaults to {@code defaultArg}, and brief description
	 * <code>description</code> as well as the delegate <code>delegate</code> to
	 * process the argument and optionally retrieve the description.
	 * <p>
	 * <strong>Note:</strong> on the command line option names need to be preceded
	 * by <code>--</code>, e.g. <code>--help</code>.
	 * 
	 * @param name        the name of the command line option
	 * @param defaultArg  the default argument if option is not specified on command
	 *                    line
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, String description, CLODelegate delegate) {
		this(name, defaultArg, Argument.REQUIRED, null, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (with required
	 * argument), which defaults to {@code defaultArg}, of catgeory {@code category}
	 * and brief description <code>description</code> as well as the delegate
	 * <code>delegate</code> to process the argument and optionally retrieve the
	 * description.
	 * <p>
	 * <strong>Note:</strong> on the command line option names need to be preceded
	 * by <code>--</code>, e.g. <code>--help</code>.
	 * 
	 * @param name        the name of the command line option
	 * @param defaultArg  the default argument if option is not specified on command
	 *                    line
	 * @param category    the category of option
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, Category category, String description, CLODelegate delegate) {
		this(name, defaultArg, Argument.REQUIRED, category, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> of type
	 * {@code type}, which defaults to {@code defaultArg}, as well as the delegate
	 * <code>delegate</code> to process the argument and retrieve the description.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li><code>delegate</code> must implement {@link CLODelegate#getDescription()}
	 * to provide option description.</li>
	 * </ul>
	 * 
	 * @param name       the name of the command line option
	 * @param defaultArg the default argument if option is not specified on command
	 *                   line
	 * @param type       of command line option (whether argument required)
	 * @param delegate   delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, Argument type, CLODelegate delegate) {
		this(name, defaultArg, type, null, null, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> of type
	 * {@code type}, which defaults to {@code defaultArg}, and catgeory
	 * {@code category} as well as the delegate <code>delegate</code> to process the
	 * argument and retrieve the description.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li><code>delegate</code> must implement {@link CLODelegate#getDescription()}
	 * to provide option description.</li>
	 * </ul>
	 * 
	 * @param name       the name of the command line option
	 * @param defaultArg the default argument if option is not specified on command
	 *                   line
	 * @param type       of command line option (whether argument required)
	 * @param category   the category of option
	 * @param delegate   delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, Argument type, Category category, CLODelegate delegate) {
		this(name, defaultArg, type, category, null, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> of type
	 * {@code type}, which defaults to {@code defaultArg}, and brief description
	 * <code>description</code> as well as the delegate <code>delegate</code> to
	 * process the argument and optionally retrieve the description.
	 * <p>
	 * <strong>Note:</strong> on the command line option names need to be preceded
	 * by <code>--</code>, e.g. <code>--help</code>.
	 * 
	 * @param name        the name of the command line option
	 * @param defaultArg  default argument if option is not specified on command
	 *                    line
	 * @param type        of command line option (whether argument required)
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, Argument type, String description, CLODelegate delegate) {
		this(name, defaultArg, type, null, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> of type
	 * {@code type}, which defaults to {@code defaultArg}, in category
	 * {@code category} and brief description <code>description</code> as well as
	 * the delegate <code>delegate</code> to process the argument and optionally
	 * retrieve the description.
	 * <p>
	 * <strong>Note:</strong> on the command line options need to be preceded
	 * by <code>--</code>, e.g. <code>--help</code>.
	 *
	 * @param name        name of command line option
	 * @param defaultArg  default argument if option is not specified on command
	 *                    line
	 * @param category    the category of command line option
	 * @param type        of command line option (whether argument required)
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, Argument type, Category category, String description,
			CLODelegate delegate) {
		this.ID = uniqueID++;
		this.name = name;
		this.type = type;
		this.defaultArg = defaultArg;
		this.description = description;
		this.delegate = delegate;
		this.category = category;
	}

	/**
	 * Set the default argument. This argument is parsed by the delegate if option
	 * is not specified on command line or if the parsing of the provided argument
	 * failed. If the parsing of <code>defaultArg</code> fails, the option settings
	 * are undefined.
	 * 
	 * @param defaultArg default argument for command line option
	 */
	public void setDefault(String defaultArg) {
		this.defaultArg = defaultArg;
	}

	/**
	 * Return the default argument for command line option.
	 * 
	 * @return default argument
	 */
	public String getDefault() {
		return defaultArg;
	}

	/**
	 * Process command line option and argument. If <code>option</code> matches the
	 * name of this option then the option <code>isSet</code> and, depending on the
	 * option <code>type</code> the next entry in <code>options</code> is processed
	 * as the option argument, as appropriate. Returns <code>0</code> on successful
	 * processing of option and possible arguments. Returns <code>-1</code> if
	 * <code>option</code> matches the name and <code>1</code> if processing of
	 * argument fails for other reasons.
	 * 
	 * @param option  the name of option to parse
	 * @param options the list of options and arguments provided on command line
	 * @return <code>-1</code> if no match, <code>0</code> on success,
	 *         <code>1</code> if parsing of argument failed
	 */
	public int processOption(String option, ListIterator<String> options) {
		// if an option has been removed in some preliminary screening option==null;
		// simply skip over
		if (option == null)
			return 0;
		String opt = "--" + name;
		// check if name of option (must be perfect match because option names
		// have to be followed by either ' ' or '=' and the latter has already
		// been taken care of, see CLOParser.parseCLO)
		if (option.equals(opt))
			return processOptionArg(options) ? 0 : 1;
		return -1;
	}

	/**
	 * For options with {@link Argument#OPTIONAL} or {@link Argument#REQUIRED}
	 * checks next entry on command line and processes arguments as appropriate.
	 * <p>
	 * <strong>Note:</strong> legitimate arguments can start with <code>'-'</code>,
	 * e.g. negative numbers, vectors or matrices. If next entry starts with
	 * <code>'-'</code> but parses as a number or matrix, assume it is an argument.
	 * If, for example, <code>-1</code> is a valid short option then specifying
	 * <code>-1</code> after an option with optional argument processes
	 * <code>-1</code> as the optional argument and <em>not</em> as the next option.
	 * 
	 * @param options list of options and arguments provided on command line
	 * @return <code>true</code> on success
	 */
	private boolean processOptionArg(ListIterator<String> options) {
		optionArg = null;
		isSet = false;
		if (type == Argument.NONE) {
			isSet = true;
			return true;
		}
		if (!options.hasNext()) {
			// last option, no argument
			if (type == Argument.REQUIRED)
				return false;
			// last option had optional argument
			isSet = true;
			return true;
		}
		// read argument
		String arg = options.next();
		// argument should not start with '--', must be subsequent option
		if (arg.startsWith("--")) {
			options.previous();
			if (type == Argument.REQUIRED)
				return false;
			// optional argument
			isSet = true;
			return true;
		}
		if (type == Argument.REQUIRED) {
			// argument could still start with '-' indicating negative number [in array], be
			// greedy and assume this is the argument; check if first char of argument is
			// valid key (or options has no keys).
			if (!isValidKey(arg)) {
				options.previous();
				return false;
			}
			setOptionArg(arg);
			return true;
		}
		// argument is optional; could still be a short option (only start with '--'
		// has been ruled out)
		if (arg.charAt(0) == '-') {
			// short option or negative number - try to parse as matrix
			try {
				if (CLOParser.parseMatrix(arg).length > 0) {
					// option is number, treat it as argument
					setOptionArg(arg);
				}
			} catch (Exception e) {
				// doesn't look like a number; rewind options
				options.previous();
				if (type == Argument.REQUIRED)
					return false;
				// argument optional
				isSet = true;
			}
			return true;
		}
		// in the most complex valid case it is a matrix
		setOptionArg(arg);
		return true;
	}

	private void setOptionArg(String arg) {
		optionArg = arg.trim();
		isSet = true;
	}

	/**
	 * Parses the option and its argument, if applicable, through the delegate. If
	 * this option was not specified on command line, the default argument is passed
	 * to the delegate.
	 * 
	 * @return <code>true</code> on successful parsing of argument
	 */
	public boolean parse() {
		if (delegate == null)
			return false;
		return delegate.parse(getArg());
	}

	/**
	 * Parses the default argument for this option. Typically called if
	 * {@link #parse()} failed.
	 * 
	 * @return <code>true</code> on successful parsing of default argument
	 */
	public boolean parseDefault() {
		if (delegate == null)
			return false;
		return delegate.parse(getDefault());
	}

	public void addKeys(Key[] chain) {
		for (Key key : chain)
			addKey(key);
	}

	public Key addKey(String key, String title) {
		if (keys == null)
			keys = new HashMap<String, Key>();
		return keys.put(key, new SimpleKey(key, title));
	}

	public Key addKey(Key key) {
		if (keys == null)
			keys = new HashMap<String, Key>();
		return keys.put(key.getKey(), key);
	}

	public Key getKey(String aKey) {
		if (keys == null)
			return null;
		return keys.get(aKey);
	}

	/**
	 * Returns the key that best matches <code>name</code>. If several keys are
	 * equally good matches the first match is returned. If <code>name</code>
	 * perfectly matches one key, i.e. <code>name.startsWith(key.getName())</code>
	 * is <code>true</code>, then a better match must match at least one more
	 * character of <code>name</code>.
	 * 
	 * @param keyname of key to match
	 * @return matching <code>Key</code> or <code>null</code> if no match was found
	 */
	public Key match(String keyname) {
		if (keys == null)
			return null;
		double best = 0.0;
		Key match = null;
		for (Key key : keys.values()) {
			int diff = differAt(keyname, key.getKey());
			if (diff >= best && diff == key.getKey().length()) {
				best = diff + 0.5;
				match = key;
				continue;
			}
			if (diff > best) {
				best = diff;
				match = key;
			}
		}
		return match;
	}

	public Key removeKey(Key key) {
		return removeKey(key.getKey());
	}

	public Key removeKey(String aKey) {
		if (keys == null)
			return null;
		return keys.remove(aKey);
	}

	public void clearKeys() {
		if (keys == null)
			return;
		keys.clear();
	}

	public boolean isValidKey(Key key) {
		return isValidKey(key.getKey());
	}

	public boolean isValidKey(String aKey) {
		if (keys == null)
			return true;
		// in order to allow abbreviating keys as well as appending options, this test
		// is very lenient and passes if aKey and one of the keys start at least with
		// one identical character
		for (String key : keys.keySet()) {
			if (differAt(key, aKey) > 0)
				return true;
		}
		return false;
	}

	public static int differAt(String a, String b) {
		int max = Math.min(a.length(), b.length());
		if (max == 0)
			return 0;
		int idx = 0;
		while (a.charAt(idx) == b.charAt(idx)) {
			if (++idx == max)
				return max;
		}
		return idx;
	}

	public static String stripKey(CLOption.Key keyopt, String arg) {
		return stripKey(keyopt.getKey(), arg);
	}

	public static String stripKey(String key, String arg) {
		return arg.substring(CLOption.differAt(key, arg));
	}

	public Collection<Key> getKeys() {
		return keys.values();
	}

	public void inheritKeysFrom(CLOption option) {
		inheritedKeys = true;
		keys = option.keys;
	}

	public String getDescriptionKey(String aKey) {
		Key key = getKey(aKey);
		if (key == null)
			return null;
		return key.toString();
	}

	public String getDescriptionKey() {
		if (keys == null || inheritedKeys)
			return "";
		String keydescr = "";
		for (Key key : keys.values()) {
			String descr = key.getDescription();
			// align ':' for better readability
			String aKey = "             " + key.getKey() + ": ";
			int keylen = aKey.length();
			keydescr += aKey.substring(keylen - 16, keylen) + (descr == null ? key.getTitle() : descr) + "\n";
			// keydescr += " " + key.getKey() + ": " + (descr == null ? key.getTitle() :
			// descr) + "\n";
		}

		int len = keydescr.length();
		if (len > 0)
			return keydescr.substring(0, len - 1);
		return keydescr;
	}

	/**
	 * Print report for this option to <code>output</code> through delegate. If no
	 * delegate is available or <code>output == null</code> do nothing.
	 * 
	 * @param output the {@link PrintStream} for the output
	 */
	public void report(PrintStream output) {
		if (delegate == null || output == null)
			return;
		delegate.report(output);
	}

	/**
	 * Reset option. Clear argument, if applicable, and mark as not
	 * <code>isSet</code>.
	 */
	public void reset() {
		optionArg = null;
		isSet = false;
		// custom description has not yet been retrieved
		if (description == null)
			return;
		// check if custom description provided
		String descr = delegate.getDescription();
		if (descr != null)
			description = null;
	}

	/**
	 * @return the name of this option
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return type of option
	 */
	public Argument getType() {
		return type;
	}

	/**
	 *
	 * @return argument of option or default if argument not set.
	 */
	public String getArg() {
		if (optionArg == null)
			return defaultArg;
		return optionArg;
	}

	/**
	 * @return <code>true</code> if no argument set.
	 */
	public boolean isDefault() {
		return (optionArg == null);
	}

	/**
	 * @return <code>true</code> if option set on command line (regardless of
	 *         whether an argument was provided).
	 */
	public boolean isSet() {
		return isSet;
	}

	/**
	 * Retrieve short description of option and include the default as well as the
	 * current arguments. If no description was provided at initialization, the
	 * delegate is queried for an up-to-date description.
	 * 
	 * @return description of option and arguments.
	 */
	public String getDescription() {
		String myDescr;
		if (description == null) {
			// description is delegate's responsibility - including keys (if applicable)
			myDescr = delegate.getDescription();
		} else {
			String descr = getDescriptionKey();
			if (descr.length() > 0)
				myDescr = description + "\n" + descr;
			else
				myDescr = description;
		}
		if (type == Argument.NONE)
			return myDescr + "\n      (current: " + (isSet() ? "" : "not ") + "set)";
		String arg = getArg();
		if (!isSet() || isDefault() || arg.equals(defaultArg))
			return myDescr + (defaultArg != null ? "\n      (default: " + defaultArg + ")" : "");
		if (keys != null) {
			String[] args = arg.split(CLOParser.SPECIES_DELIMITER);
			String argkeys = "";
			for (int n = 0; n < args.length; n++) {
				String key = match(args[n]).getKey();
				String keyarg = stripKey(key, args[n]);
				argkeys += key + keyarg + (n == args.length - 1 ? "" : CLOParser.SPECIES_DELIMITER);
			}
			return myDescr + "\n      (current: " + argkeys + ", default: " + defaultArg + ")";
		}
		return myDescr + "\n      (current: " + arg + ", default: " + defaultArg + ")";
	}

	/**
	 * Set short description of option.
	 * 
	 * @param descr description of option
	 */
	public void setDescription(String descr) {
		description = descr;
	}

	@Override
	public int compareTo(CLOption opt) {
		return name.compareTo(opt.getName());
	}
}