//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 * Command line option and argument.
 *
 * @author Christoph Hauert
 */
public class CLOption implements Comparable<CLOption> {

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

	/**
	 * Simple key for command line options. Keys are similar to {@code enum}s but
	 * more flexible because the set of keys can be modified at runtime.
	 */
	public static class SimpleKey implements Key {

		/**
		 * The key of the command line option.
		 */
		String key;

		/**
		 * The title of the command line option. Brief description of the key for the
		 * help screen.
		 */
		String title;

		/**
		 * Create a new key with the name <code>key</code> and title <code>title</code>.
		 * 
		 * @param key   the key of the command line option
		 * @param title the title of the command line option
		 */
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

	/**
	 * The interface for keys of command line options. Options may offer several
	 * distinct settings in the form of keys.
	 */
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
		 * @see #getDescription()
		 * @see org.evoludo.simulator.EvoLudo#showHelp()
		 */
		public String getTitle();

		/**
		 * Optional: long description of purpose of this key. Used in help display.
		 * Defaults to brief description.
		 * <p>
		 * <strong>Note:</strong> the description string may contain any UTF-8
		 * characters as well as HTML character entities. If necessary they will be
		 * escaped and converted to UTF-8 for display in XML documents.
		 * 
		 * @return the description of the command line option
		 * 
		 * @see #getTitle()
		 * @see org.evoludo.simulator.EvoLudo#showHelp()
		 */
		public default String getDescription() {
			return getTitle();
		}
	}

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
	final CLOCategory category;

	/**
	 * the short description of the command line option. May include newline's
	 * <code>'\n'</code> for basic formatting.
	 * <p>
	 * <strong>Note:</strong> the description string may contain any UTF-8
	 * characters as well as HTML character entities. If necessary they will be
	 * escaped and converted to UTF-8 for display in XML documents.
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
	 * {@code true} the keys will not be printed as part of the description.
	 */
	boolean inheritedKeys = false;

	/**
	 * The delegate for parsing arguments, reporting settings and retrieving
	 * customized descriptions.
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
	public CLOption(String name, CLOCategory category, CLODelegate delegate) {
		this(name, null, Argument.NONE, category, null, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (no arguments)
	 * and short description <code>description</code> as well as the delegate
	 * <code>delegate</code> to process the argument and optionally retrieve the
	 * description.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li>the description string may contain any UTF-8 characters as well as HTML
	 * character entities. If necessary they will be escaped and converted to UTF-8
	 * for display in XML documents.
	 * </ul>
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
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li>the description string may contain any UTF-8 characters as well as HTML
	 * character entities. If necessary they will be escaped and converted to UTF-8
	 * for display in XML documents.
	 * </ul>
	 * 
	 * @param name        the name of the command line option
	 * @param category    the category of option
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, CLOCategory category, String description, CLODelegate delegate) {
		this(name, null, Argument.NONE, category, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> (with required
	 * argument), which defaults to {@code defaultArg}, and brief description
	 * <code>description</code> as well as the delegate <code>delegate</code> to
	 * process the argument and optionally retrieve the description.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li>the description string may contain any UTF-8 characters as well as HTML
	 * character entities. If necessary they will be escaped and converted to UTF-8
	 * for display in XML documents.
	 * </ul>
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
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li>the description string may contain any UTF-8 characters as well as HTML
	 * character entities. If necessary they will be escaped and converted to UTF-8
	 * for display in XML documents.
	 * </ul>
	 * 
	 * @param name        the name of the command line option
	 * @param defaultArg  the default argument if option is not specified on command
	 *                    line
	 * @param category    the category of option
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, CLOCategory category, String description, CLODelegate delegate) {
		this(name, defaultArg, Argument.REQUIRED, category, description, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> of type
	 * {@code type}, which defaults to {@code defaultArg}, as well as the delegate
	 * <code>delegate</code> to process the argument and retrieve the description.
	 * <p>
	 * <strong>Notes:</strong>
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
	public CLOption(String name, String defaultArg, Argument type, CLOCategory category, CLODelegate delegate) {
		this(name, defaultArg, type, category, null, delegate);
	}

	/**
	 * Creates command line option with the name <code>name</code> of type
	 * {@code type}, which defaults to {@code defaultArg}, and brief description
	 * <code>description</code> as well as the delegate <code>delegate</code> to
	 * process the argument and optionally retrieve the description.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li>the description string may contain any UTF-8 characters as well as HTML
	 * character entities. If necessary they will be escaped and converted to UTF-8
	 * for display in XML documents.
	 * </ul>
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
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>on the command line option names need to be preceded by <code>--</code>,
	 * e.g. <code>--help</code>.</li>
	 * <li>the description string may contain any UTF-8 characters as well as HTML
	 * character entities. If necessary they will be escaped and converted to UTF-8
	 * for display in XML documents.
	 * </ul>
	 *
	 * @param name        name of command line option
	 * @param defaultArg  default argument if option is not specified on command
	 *                    line
	 * @param category    the category of command line option
	 * @param type        of command line option (whether argument required)
	 * @param description short description of command line option
	 * @param delegate    delegate for processing command line argument
	 */
	public CLOption(String name, String defaultArg, Argument type, CLOCategory category, String description,
			CLODelegate delegate) {
		this.name = name;
		this.type = type;
		this.defaultArg = defaultArg;
		this.description = description;
		this.category = category;
		this.delegate = delegate;
		if (delegate == null)
			throw new IllegalArgumentException("CLOption: delegate must not be null");
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
	 * Set the argument for the command line option.
	 * 
	 * @param arg the argument
	 */
	public void setArg(String arg) {
		if (arg != null)
			arg = arg.trim();
		optionArg = arg;
	}

	/**
	 * Parses the option and its argument, if applicable, through the delegate. If
	 * this option was not specified on command line, the default argument is passed
	 * to the delegate.
	 * 
	 * @return {@code true} on successful parsing of argument
	 */
	public boolean parse() {
		boolean isSet = isSet();
		switch (type) {
			case NONE:
				return delegate.parse(isSet);
			case OPTIONAL:
				return parse(true);
			case REQUIRED:
				return parse(false);
			default:
				return false;
		}
	}

	/**
	 * Delegate parsing helper that handles optional vs required arguments.
	 * 
	 * @param isOptional {@code true} if the option argument is optional
	 * @return {@code true} if parsing succeeds
	 */
	private boolean parse(boolean isOptional) {
		try {
			if (isOptional)
				return delegate.parse(getArg(), isSet());
			if (delegate.parse(getArg()))
				return true;
			return delegate.parse(getDefault());
		} catch (Exception e) {
			try {
				if (isOptional)
					return delegate.parse(getDefault(), isSet());
				return delegate.parse(getDefault());
			} catch (Exception ex) {
				return false;
			}
		}
	}

	/**
	 * Add all {@link Key}s in the array {@code chain} to this option. Note, this
	 * ignores keys starting with '-', except if '-' is the key. If needed, those
	 * keys can still be added by calling {@code addKey(Key)} or
	 * {@code #addKey(String, String)}.
	 * 
	 * @param chain the array of {@link Key}s to be added
	 * 
	 * @see #addKey(Key)
	 * @see #addKey(String, String)
	 */
	public void addKeys(Key[] chain) {
		for (Key key : chain) {
			String keyname = key.getKey();
			if (keyname.length() > 1 && keyname.startsWith("-"))
				continue;
			addKey(key);
		}
	}

	/**
	 * Add a {@link Key} to option with name {@code key} and description
	 * {@code title}.
	 * 
	 * @param key   the name of the key
	 * @param title the description of the key
	 * @return the previous value associated with {@code key}, or {@code null} if
	 *         there was no mapping for {@code key}. (A {@code null} return can also
	 *         indicate that the map previously associated {@code null} with
	 *         {@code key}.)
	 */
	public Key addKey(String key, String title) {
		if (keys == null)
			keys = new HashMap<>();
		return keys.put(key, new SimpleKey(key, title));
	}

	/**
	 * Add {@link Key} {@code key} to option.
	 * 
	 * @param key the {@link Key} to be added
	 * @return the previous value associated with {@code key}, or {@code null} if
	 *         there was no mapping for {@code key}. (A {@code null} return can also
	 *         indicate that the map previously associated {@code null} with
	 *         {@code key}.)
	 */
	public Key addKey(Key key) {
		if (keys == null)
			keys = new HashMap<>();
		return keys.put(key.getKey(), key);
	}

	/**
	 * Get the key with name {@code aKey}. Returns {@code null} if the option has no
	 * keys or no key with the name {@code aKey}.
	 * 
	 * @param aKey the name of the key
	 * @return the key with name {@code aKey} or {@code null} if no such key exists
	 */
	public Key getKey(String aKey) {
		if (keys == null)
			return null;
		return keys.get(aKey);
	}

	/**
	 * Returns the key that best matches <code>name</code>. If several keys are
	 * equally good matches the first match is returned. If <code>name</code>
	 * perfectly matches one key, i.e. <code>name.startsWith(key.getName())</code>
	 * is {@code true}, then a better match must match at least one more
	 * character of <code>name</code>.
	 * 
	 * @param keyname the name of the key to match
	 * @return matching <code>Key</code> or <code>null</code> if no match was found
	 */
	public Key match(String keyname) {
		return match(keyname, 1);
	}

	/**
	 * Returns the key that best matches <code>name</code> with at least {@code min}
	 * characters matching. If several keys are equally good matches the first match
	 * is returned. If <code>name</code> perfectly matches one key, i.e.
	 * <code>name.startsWith(key.getName())</code> is {@code true}, then a
	 * better match must match at least one more character of <code>name</code>.
	 * 
	 * @param keyname the name of the key to match
	 * @param min     minimum number of characters that must match
	 * @return matching <code>Key</code> or <code>null</code> if no match was found
	 */
	public Key match(String keyname, int min) {
		if (keys == null)
			return null;
		double best = 0.0;
		Key match = null;
		for (Key key : keys.values()) {
			int diff = differAt(keyname, key.getKey());
			if (diff < min) {
				// not enough matching characters; skip this key
			} else if (diff >= best && diff == key.getKey().length()) {
				best = diff + 0.5;
				match = key;
			} else if (diff > best) {
				best = diff;
				match = key;
			}
		}
		return match;
	}

	/**
	 * Remove key from the option's key collection. Returns {@code null} if the
	 * option has no keys or the {@code key} is not part of the collection.
	 * 
	 * @param key the key to remove
	 * @return the key that was removed or <code>null</code> if no such key exists
	 */
	public Key removeKey(Key key) {
		return removeKey(key.getKey());
	}

	/**
	 * Remove key with name <code>aKey</code> from the option's key collection.
	 * 
	 * @param aKey the name of the key to remove
	 * @return the key that was removed or <code>null</code> if no such key exists
	 */
	public Key removeKey(String aKey) {
		if (keys == null)
			return null;
		return keys.remove(aKey);
	}

	/**
	 * Clear all keys from the option.
	 */
	public void clearKeys() {
		if (keys == null)
			return;
		keys.clear();
	}

	/**
	 * Check if <code>key</code> is a valid key for this option.
	 * 
	 * @param key the key to check
	 * @return {@code true} if <code>key</code> is a valid key
	 */
	public boolean isValidKey(Key key) {
		return isValidKey(key.getKey());
	}

	/**
	 * Check if the key with name <code>aKey</code> is a valid key for this option.
	 * This test is very lenient and passes if {@code aKey} and one of the keys
	 * start at least with one identical character. This allows abbreviating keys as
	 * well as appending options.
	 * 
	 * @param aKey the name of the key to check
	 * @return {@code true} if the name <code>aKey</code> is valid
	 * 
	 * @see #differAt(String, String)
	 */
	public boolean isValidKey(String aKey) {
		if (keys == null)
			return true;
		for (String key : keys.keySet()) {
			if (differAt(key, aKey) > 0)
				return true;
		}
		return false;
	}

	/**
	 * Compare two strings and return the index of the first character that differs.
	 * 
	 * @param a the first string
	 * @param b the second string
	 * @return the index of the first differing character
	 */
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

	/**
	 * Strips the name of the key from the argument. If the key is not found, the
	 * argument is returned unchanged.
	 * 
	 * @param key the key to strip
	 * @param arg the argument to strip the key from
	 * @return the argument without the key
	 */
	public static String stripKey(CLOption.Key key, String arg) {
		return stripKey(key.getKey(), arg);
	}

	/**
	 * Strips the key from the argument. If the key is not found, the argument is
	 * returned unchanged.
	 * 
	 * @param key the name of the key to strip
	 * @param arg the argument to strip the key from
	 * @return the argument without the key
	 */
	public static String stripKey(String key, String arg) {
		return arg.substring(CLOption.differAt(key, arg));
	}

	/**
	 * Gets all keys of this option.
	 * 
	 * @return the key collection
	 */
	public Collection<Key> getKeys() {
		if (keys != null)
			return keys.values();
		return new ArrayList<>();
	}

	/**
	 * Inherit keys from another option. This is useful if options share the same
	 * keys.
	 * 
	 * @param option the option to inherit keys from
	 */
	public void inheritKeysFrom(CLOption option) {
		inheritedKeys = true;
		keys = option.keys;
	}

	/**
	 * Get the description of the key with name <code>aKey</code>.
	 * 
	 * @param aKey the name of the key
	 * @return the description of the key
	 */
	public String getDescriptionKey(String aKey) {
		Key key = getKey(aKey);
		if (key == null)
			return null;
		return key.toString();
	}

	/**
	 * Get the description of all keys of this option. Minimal formatting is applied
	 * with the name of the key and a brief description of the key separated by
	 * '\n'. No HTML or other formatting can be applied.
	 * 
	 * @return the description of all keys
	 */
	public String getDescriptionKey() {
		if (keys == null || inheritedKeys)
			return "";
		StringBuilder keydescr = new StringBuilder();
		for (Key key : keys.values()) {
			String descr = key.getDescription();
			// align ':' for better readability
			String aKey = "             " + key.getKey() + ": ";
			int keylen = aKey.length();
			keydescr.append(aKey.substring(keylen - 16, keylen))
					.append(descr == null ? key.getTitle() : descr)
					.append("\n");
		}

		int len = keydescr.length();
		if (len > 0)
			return keydescr.substring(0, len - 1);
		return keydescr.toString();
	}

	/**
	 * Reset option. Clear argument, if applicable, and mark as not
	 * <code>isSet</code>.
	 */
	public void reset() {
		optionArg = null;
		// custom description has not yet been retrieved
		if (description == null)
			return;
		// check if custom description provided
		String descr = delegate.getDescription();
		if (descr != null)
			description = null;
	}

	/**
	 * Get the name of the option.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the type of the option.
	 * 
	 * @return the type
	 */
	public Argument getType() {
		return type;
	}

	/**
	 * Get the option argument. If no argument was set, the default argument is
	 * returned.
	 * 
	 * @return the argument
	 */
	public String getArg() {
		if (optionArg == null || optionArg.isEmpty())
			return defaultArg;
		return optionArg;
	}

	/**
	 * Check if option was set on command line (regardless of whether an argument
	 * was provided).
	 * 
	 * @return {@code true} if option set
	 */
	public boolean isSet() {
		return (optionArg != null);
	}

	/**
	 * Retrieve short description of option and include the default as well as the
	 * current arguments. If no description was provided at initialization, the
	 * delegate is queried for an up-to-date description.
	 * <p>
	 * <strong>Note:</strong> the description string may contain any UTF-8
	 * characters. If necessary they will be escaped for display in HTML or XML
	 * documents.
	 * 
	 * @return description of option and arguments.
	 */
	public String getDescription() {
		if (description == null && delegate != null) {
			// description is delegate's responsibility - including keys (if applicable)
			return delegate.getDescription();
		}
		StringBuilder myDescr = buildBaseDescription();
		String currentArg = optionArg;
		if (keys != null)
			currentArg = buildArgKeys();
		buildCurrent(myDescr, currentArg);
		return myDescr.toString();

	}

	/**
	 * Append the current and default argument description to the provided builder.
	 * 
	 * @param sb         builder to append to
	 * @param currentArg textual representation of the current argument
	 * @return the supplied builder for chaining
	 */
	private StringBuilder buildCurrent(StringBuilder sb, String currentArg) {
		sb.append("\n      (");
		if (type == Argument.NONE) {
			sb.append("current: ").append(isSet() ? "" : "not ").append("set)");
			return sb;
		}
		if (currentArg != null) {
			sb.append("current: ").append(currentArg);
			if (defaultArg != null)
				sb.append(" ");
		}
		if (defaultArg != null) {
			sb.append("default: ").append(defaultArg);
		}
		sb.append(")");
		return sb;
	}

	/**
	 * Build the base description string, using either the stored description or
	 * asking the delegate and appending key descriptions if applicable.
	 * 
	 * @return builder containing the base description text
	 */
	private StringBuilder buildBaseDescription() {
		StringBuilder myDescr = new StringBuilder(description);
		String descr = getDescriptionKey();
		if (!descr.isEmpty()) {
			myDescr.append("\n").append(descr);
		}
		return myDescr;
	}

	/**
	 * Construct the user-facing representation of the argument when keys are
	 * defined; mirrors previous loop logic but extracted to reduce complexity.
	 * 
	 * @return textual representation of the key-based arguments
	 */
	private String buildArgKeys() {
		// for multi-species modules, remember previous key
		Key prev = null;
		String[] args = getArg().split(CLOParser.SPECIES_DELIMITER);
		StringBuilder sb = new StringBuilder();
		for (int n = 0; n < args.length; n++) {
			String[] argsn = args[n].split("\\s+|=|,");
			int keypos = delegate.getKeyPos();

			if (keypos < 0 || keypos >= argsn.length) {
				// no valid key position available, append as-is and continue.
				sb.append(String.join(",", argsn));
				if (n != args.length - 1)
					sb.append(CLOParser.SPECIES_DELIMITER);
			} else {
				Key key = match(argsn[keypos]);
				if (key == null) {
					key = prev;
				}
				if (key == null) {
					sb.append("INVALID '").append(args[n].trim()).append("'");
					break;
				}
				prev = buildKey(sb, key, argsn, keypos);
			}
		}
		return sb.toString();
	}

	/**
	 * Append the resolved key value and remaining arguments to the builder.
	 * 
	 * @param sb   builder to append to
	 * @param key  resolved key
	 * @param args tokenized argument list
	 * @param pos  position of the key token within {@code args}
	 * @return the key that was appended (for chaining)
	 */
	private Key buildKey(StringBuilder sb, Key key, String[] args, int pos) {
		String[] lead = Arrays.copyOfRange(args, 0, pos);
		String[] tail = Arrays.copyOfRange(args, pos + 1, args.length);

		if (lead.length > 0) {
			sb.append(String.join(",", lead)).append(" ");
		}
		sb.append(key.getKey());
		if (tail.length > 0) {
			sb.append(" ").append(String.join(",", tail));
		}

		if (pos != args.length - 1)
			sb.append(CLOParser.SPECIES_DELIMITER);
		return key;
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		CLOption other = (CLOption) obj;
		return name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
