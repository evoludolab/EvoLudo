//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

package org.evoludo.simulator.geometries;

import java.util.Objects;

import org.evoludo.simulator.EvoLudo;

/**
 * Metapopulation geometry with two independently configurable components:
 * geometry within each deme and geometry among demes.
 */
public class MetapopulationGeometry extends AbstractGeometry {

	/**
	 * Geometry used within each deme.
	 */
	private GeometryType demeType = GeometryType.WELLMIXED;

	/**
	 * Geometry used to arrange demes.
	 */
	private GeometryType metaType = GeometryType.WELLMIXED;

	/**
	 * Number of demes in the metapopulation.
	 */
	private int nDemes = 2;

	/**
	 * Number of individuals per deme.
	 */
	private int demeSize = 2;

	/**
	 * Fixed number of demes from CLI ({@code d<...>}), or {@code -1} if unset.
	 */
	private int cliDemes = -1;

	/**
	 * Fixed deme size from CLI ({@code s<...>}), or {@code -1} if unset.
	 */
	private int cliDemeSize = -1;

	/**
	 * Optional CLI arguments for within-deme geometry (without key), or
	 * {@code null} if unspecified.
	 */
	private String cliDemeArgs = null;

	/**
	 * Optional CLI arguments for deme-arrangement geometry (without key), or
	 * {@code null} if unspecified.
	 */
	private String cliMetaArgs = null;

	/**
	 * Create a metapopulation geometry tied to the provided engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public MetapopulationGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.META);
	}

	@Override
	public boolean parse(String spec) {
		String working = spec == null ? "" : spec.trim();
		resetDefaults();
		if (working.isEmpty())
			return true;
		int split = working.indexOf(';');
		if (split <= 0 || split >= working.length() - 1) {
			logInvalidFormat();
			return false;
		}
		String demeToken = working.substring(0, split).trim();
		String tail = working.substring(split + 1);
		int suffixStart = tail.indexOf(';');
		String metaToken = (suffixStart < 0 ? tail : tail.substring(0, suffixStart)).trim();
		String suffix = suffixStart < 0 ? null : tail.substring(suffixStart + 1);
		if (demeToken.isEmpty() || metaToken.isEmpty()) {
			logInvalidFormat();
			return false;
		}
		GeometryType type = parseSubtype(demeToken, true);
		boolean success = true;
		if (type == null) {
			logInvalidFormat();
			success = false;
		} else {
			demeType = type;
		}
		type = parseSubtype(metaToken, false);
		if (type == null) {
			logInvalidFormat();
			success = false;
		} else {
			metaType = type;
		}
		success &= parseSuffixSection(suffix);
		return success;
	}

	/**
	 * Reset metapopulation parsing state to defaults.
	 */
	private void resetDefaults() {
		demeType = GeometryType.WELLMIXED;
		metaType = GeometryType.WELLMIXED;
		cliDemes = -1;
		cliDemeSize = -1;
		cliDemeArgs = null;
		cliMetaArgs = null;
	}

	/**
	 * Parse a subtype component such as {@code r4} or {@code n}.
	 *
	 * @param token  subtype component
	 * @param isDeme {@code true} for within-deme component, {@code false} for
	 *               arrangement component
	 * @return parsed subtype or {@code null} if invalid
	 */
	private GeometryType parseSubtype(String token, boolean isDeme) {
		if (token == null)
			return null;
		String trimmed = token.trim();
		if (trimmed.isEmpty())
			return null;
		GeometryType parsed = matchSubtypePrefix(trimmed);
		if (parsed == null)
			return null;
		String remainder = trimmed.substring(parsed.getKey().length());
		if (!remainder.isEmpty()) {
			if (isDeme)
				cliDemeArgs = remainder;
			else
				cliMetaArgs = remainder;
		}
		return parsed;
	}

	/**
	 * Parse an optional suffix section from semicolon-separated metapop specs.
	 * The section may contain {@code d<n>} and {@code s<n>} with optional
	 * separators (comma and/or semicolon) between tags.
	 *
	 * @param suffixSection suffix section after the second semicolon
	 * @return {@code true} if suffix parsing succeeded
	 */
	private boolean parseSuffixSection(String suffixSection) {
		if (suffixSection == null || suffixSection.isEmpty())
			return true;
		String compact = suffixSection.replaceAll("[\\s,;]+", "");
		if (compact.isEmpty())
			return true;
		boolean success = true;
		String working = compact;
		while (!working.isEmpty()) {
			char tag = Character.toLowerCase(working.charAt(0));
			if (tag != 'd' && tag != 's') {
				logInvalidSuffix(working);
				return false;
			}
			int idx = 1;
			while (idx < working.length() && Character.isDigit(working.charAt(idx)))
				idx++;
			if (idx == 1) {
				logInvalidSuffix(working);
				return false;
			}
			int value = Integer.parseInt(working.substring(1, idx));
			if (value < 1) {
				logInvalidSuffix(working);
				return false;
			}
			if (tag == 'd') {
				if (cliDemes > 0) {
					logInvalidSuffix(working, "using d" + value);
					success = false;
				}
				cliDemes = normalizeFixed(value, metaType, "number of demes",
						"arrangement geometry constraints");
			} else {
				if (cliDemeSize > 0) {
					logInvalidSuffix(working, "using s" + value);
					success = false;
				}
				cliDemeSize = normalizeFixed(value, demeType, "deme size",
						"within-deme geometry constraints");
			}
			working = working.substring(idx);
		}
		return success;
	}

	/**
	 * Normalize an explicitly requested fixed quantity to satisfy subtype
	 * constraints.
	 *
	 * @param requested requested value
	 * @param subtype   geometry imposing constraints
	 * @param what      label for warnings
	 * @param reason    reason text for warnings
	 * @return normalized value
	 */
	private int normalizeFixed(int requested, GeometryType subtype, String what, String reason) {
		int normalized = normalizeSize(subtype, Math.max(2, requested));
		if (normalized != requested)
			warn(what + " adjusted to satisfy " + reason + ": " + requested + " -> " + normalized + ".");
		return normalized;
	}

	/**
	 * Match a geometry key as prefix of {@code spec}, preferring the longest match.
	 *
	 * @param spec key fragment
	 * @return matching subtype or {@code null}
	 */
	private GeometryType matchSubtypePrefix(String spec) {
		if (spec == null || spec.isEmpty())
			return null;
		GeometryType best = null;
		int bestLen = 0;
		for (GeometryType candidate : GeometryType.values()) {
			if (!isSupportedSubtype(candidate))
				continue;
			String key = candidate.getKey();
			if (spec.startsWith(key) && key.length() > bestLen) {
				best = candidate;
				bestLen = key.length();
			}
		}
		return best;
	}

	/**
	 * Check whether a geometry type can be used as metapopulation subtype.
	 *
	 * @param subtype the candidate subtype
	 * @return {@code true} if supported
	 */
	private boolean isSupportedSubtype(GeometryType subtype) {
		switch (subtype) {
			case HIERARCHY:
			case META:
			case DYNAMIC:
				return false;
			default:
				return true;
		}
	}

	/**
	 * Get the geometry used within each deme.
	 *
	 * @return within-deme geometry type
	 */
	public GeometryType getDemeType() {
		return demeType;
	}

	/**
	 * Get the geometry used to arrange demes.
	 *
	 * @return deme-arrangement geometry type
	 */
	public GeometryType getMetaType() {
		return metaType;
	}

	/**
	 * Get number of demes.
	 *
	 * @return number of demes
	 */
	public int getNDemes() {
		return nDemes;
	}

	/**
	 * Get number of individuals per deme.
	 *
	 * @return deme size
	 */
	public int getDemeSize() {
		return demeSize;
	}

	@Override
	protected boolean checkSettings() {
		boolean reset = false;
		if (pRewire > 0.0 || pAddwire > 0.0) {
			warn("rewiring and additional links are not supported for metapopulations - ignored.");
			pRewire = 0.0;
			pAddwire = 0.0;
		}

		int requested = Math.max(4, size > 0 ? size : 4);
		if (cliDemes <= 0 && cliDemeSize <= 0) {
			int target = Math.max(2, (int) Math.rint(Math.sqrt(requested)));
			nDemes = Math.max(2, normalizeSize(metaType, target));
			demeSize = Math.max(2, normalizeSize(demeType, target));
		} else {
			nDemes = resolveDemes(requested);
			demeSize = resolveDemeSize(requested, nDemes);
		}
		int requiredPopulation = nDemes * demeSize;

		if (setSize(requiredPopulation)) {
			if (engine.getModule().cloNPopulation.isSet()) {
				if (cliDemes > 0 && cliDemeSize > 0) {
					warn("d and s settings override population size - using " + requiredPopulation + " (" + nDemes
							+ " demes x " + demeSize + " individuals per deme).");
				} else {
					warn("requires population size " + requiredPopulation + " (" + nDemes + " demes x " + demeSize
							+ " individuals per deme).");
				}
			}
			reset = true;
		}
		return reset;
	}

	/**
	 * Resolve number of demes from fixed settings and requested population size.
	 *
	 * @param requestedPopulation requested population size
	 * @return number of demes
	 */
	private int resolveDemes(int requestedPopulation) {
		if (cliDemes > 0)
			return cliDemes;

		if (cliDemeSize > 0) {
			int reqDemes = Math.max(2, (int) Math.rint((double) requestedPopulation / cliDemeSize));
			return normalizeSize(metaType, reqDemes);
		}
		int target = Math.max(2, (int) Math.rint(Math.sqrt(requestedPopulation)));
		return Math.max(2, normalizeSize(metaType, target));
	}

	/**
	 * Resolve deme size from fixed settings and requested population size.
	 *
	 * @param requestedPopulation requested population size
	 * @param demes               number of demes
	 * @return deme size
	 */
	private int resolveDemeSize(int requestedPopulation, int demes) {
		if (cliDemeSize > 0)
			return cliDemeSize;
		if (cliDemes <= 0) {
			int target = Math.max(2, (int) Math.rint(Math.sqrt(requestedPopulation)));
			return Math.max(2, normalizeSize(demeType, target));
		}
		int reqDemeSize = Math.max(2, (int) Math.rint((double) requestedPopulation / demes));
		return normalizeSize(demeType, reqDemeSize);
	}

	/**
	 * Adjust a size request to satisfy structural constraints of a subtype
	 * geometry.
	 *
	 * @param subtype   geometry subtype
	 * @param requested requested size
	 * @return adjusted size
	 */
	private int normalizeSize(GeometryType subtype, int requested) {
		int n = Math.max(1, requested);
		switch (subtype) {
			case FRUCHT:
			case TIETZE:
			case FRANKLIN:
			case ICOSAHEDRON:
				return 12;
			case HEAWOOD:
				return 14;
			case DODEKAHEDRON:
			case DESARGUES:
				return 20;
			case SUPER_STAR:
				return Math.max(3, n);
			case STRONG_SUPPRESSOR:
				int unit = Math.max(1, (int) Math.floor(Math.pow(n, 0.25)));
				return unit * unit * (1 + unit * (1 + unit));
			case STRONG_AMPLIFIER:
				int unit13 = Math.max(5, (int) Math.pow(n * 0.25, 1.0 / 3.0));
				int unit23 = unit13 * unit13;
				int unit3 = unit23 * unit13;
				double lnunit = 3.0 * Math.log(unit13);
				double epsilon = lnunit / unit13;
				double alpha = 3.0 * lnunit / Math.log(1.0 + epsilon);
				return (int) (unit3 + (1 + alpha) * unit23 + 0.5);
			case CUBE:
				int cside = Math.max((int) Math.floor(Math.pow(n, 1.0 / 3.0) + 0.5), 2);
				return cside * cside * cside;
			case TRIANGULAR:
			case HEXAGONAL:
			case SQUARE_NEUMANN_2ND:
				int eside = Math.max((int) Math.floor(Math.sqrt(n) + 0.5), 2);
				if (eside % 2 != 0)
					eside++;
				return eside * eside;
			case SQUARE_NEUMANN:
			case SQUARE_MOORE:
			case SQUARE:
				int side = Math.max((int) Math.floor(Math.sqrt(n) + 0.5), 2);
				return side * side;
			default:
				return Math.max(2, n);
		}
	}

	@Override
	public void init() {
		if (nDemes < 2 || demeSize < 2)
			throw new IllegalStateException("metapopulations require at least two demes with at least two individuals");
		if (nDemes != size / demeSize || size % demeSize != 0)
			throw new IllegalStateException("metapopulation sizes inconsistent with total population");

		isRewired = false;
		isRegular = false;
		isValid = false;

		AbstractGeometry arrangement = createSubtypeGeometry(metaType, cliMetaArgs, nDemes);
		arrangement.init();

		int[] undirectedCursor = new int[nDemes];
		int[] outCursor = new int[nDemes];
		int[] inCursor = new int[nDemes];
		int[] incident = new int[nDemes];

		for (int deme = 0; deme < nDemes; deme++) {
			int[] neighbors = arrangement.out[deme];
			int nNeighbors = arrangement.kout[deme];
			for (int i = 0; i < nNeighbors; i++) {
				int otherDeme = neighbors[i];
				if (deme == otherDeme)
					continue;
				if (arrangement.isUndirected() && otherDeme <= deme)
					continue;
				if (arrangement.isUndirected()) {
					int from = pickConnector(deme, undirectedCursor);
					int to = pickConnector(otherDeme, undirectedCursor);
					addEdgeAt(from, to);
					incident[deme]++;
					incident[otherDeme]++;
				} else {
					int from = pickConnector(deme, outCursor);
					int to = pickConnector(otherDeme, inCursor);
					addLinkAt(from, to);
				}
			}
		}

		boolean demeUndirected = true;
		for (int deme = 0; deme < nDemes; deme++) {
			AbstractGeometry withinDeme = createSubtypeGeometry(demeType, cliDemeArgs, demeSize);
			withinDeme.init();
			demeUndirected &= withinDeme.isUndirected();
			int offset = deme * demeSize;
			for (int node = 0; node < demeSize; node++) {
				int[] neighbors = withinDeme.out[node];
				int nNeighbors = withinDeme.kout[node];
				int from = offset + node;
				for (int i = 0; i < nNeighbors; i++) {
					int to = offset + neighbors[i];
					addLinkAt(from, to);
				}
			}
		}

		if (demeType == GeometryType.RANDOM_REGULAR_GRAPH && arrangement.isUndirected()) {
			for (int deme = 0; deme < nDemes; deme++) {
				if (incident[deme] % demeSize != 0) {
					warn("inter-deme links cannot be distributed evenly across all members of each random-regular deme;"
							+ " full regularity cannot be preserved.");
					break;
				}
			}
		}

		isUndirected = arrangement.isUndirected() && demeUndirected;
		isRegular = hasUniformDegrees();
		connectivity = getFeatures().avgOut;
		isValid = true;
	}

	/**
	 * Pick a connector node in the specified deme in round-robin fashion.
	 *
	 * @param deme   deme index
	 * @param cursor per-deme cursor array
	 * @return global node index
	 */
	private int pickConnector(int deme, int[] cursor) {
		int local = cursor[deme]++ % demeSize;
		return deme * demeSize + local;
	}

	/**
	 * Build a subtype geometry with defaults and the requested size.
	 *
	 * @param subtype geometry subtype
	 * @param cliArgs optional CLI arguments for subtype (without key), or
	 *                {@code null} if unspecified
	 * @param subSize size to enforce
	 * @return configured geometry
	 */
	private AbstractGeometry createSubtypeGeometry(GeometryType subtype, String cliArgs, int subSize) {
		AbstractGeometry sub = AbstractGeometry.create(engine, subtype);
		sub.setInterspecies(isInterspecies());
		String subtypeArgs = cliArgs == null ? defaultSubtypeSpec(subtype) : cliArgs;
		sub.parse(subtypeArgs);
		sub.setSize(subSize);
		sub.check();
		if (sub.getSize() != subSize)
			throw new IllegalStateException("failed to stabilize subtype " + subtype + " at size " + subSize
					+ " (got " + sub.getSize() + ")");
		return sub;
	}

	/**
	 * Default subtype arguments used in metapopulation mode when no
	 * subtype-specific parameters are supplied.
	 *
	 * @param subtype geometry subtype
	 * @return default argument string
	 */
	private String defaultSubtypeSpec(GeometryType subtype) {
		switch (subtype) {
			case RANDOM_REGULAR_GRAPH:
			case RANDOM_GRAPH:
			case RANDOM_GRAPH_DIRECTED:
			case SCALEFREE_BA:
				return "2";
			case SCALEFREE:
				return "2,-2";
			case SCALEFREE_KLEMM:
				return "2,0";
			case HEXAGONAL:
			case CUBE:
				return "6";
			default:
				return "";
		}
	}

	/**
	 * Check if every node has the same number of incoming and outgoing links.
	 *
	 * @return {@code true} if degrees are uniform
	 */
	private boolean hasUniformDegrees() {
		if (size <= 1)
			return true;
		int out0 = kout[0];
		int in0 = kin[0];
		for (int i = 1; i < size; i++)
			if (kout[i] != out0 || kin[i] != in0)
				return false;
		return true;
	}

	@Override
	public boolean isUnique() {
		if (isRewired)
			return true;
		return demeType.isUnique() || metaType.isUnique();
	}

	/**
	 * Log a warning about invalid metapopulation format and fallback to well-mixed
	 * geometry.
	 */
	private void logInvalidFormat() {
		warn("invalid metapopulation format - using well-mixed.");
	}

	/**
	 * Log a warning about an invalid metapopulation suffix.
	 */
	private void logInvalidSuffix(String suffix) {
		logInvalidSuffix(suffix, "ignored");
	}

	/**
	 * Log a warning about an invalid metapopulation suffix and applied handling.
	 */
	private void logInvalidSuffix(String suffix, String using) {
		warn("invalid metapopulation suffix '" + suffix + "' - " + using + ".");
	}

	@Override
	public MetapopulationGeometry clone() {
		MetapopulationGeometry clone = (MetapopulationGeometry) super.clone();
		clone.demeType = demeType;
		clone.metaType = metaType;
		clone.nDemes = nDemes;
		clone.demeSize = demeSize;
		clone.cliDemes = cliDemes;
		clone.cliDemeSize = cliDemeSize;
		clone.cliDemeArgs = cliDemeArgs;
		clone.cliMetaArgs = cliMetaArgs;
		return clone;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result
				+ Objects.hash(demeType, metaType, nDemes, demeSize, cliDemes, cliDemeSize, cliDemeArgs, cliMetaArgs);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		MetapopulationGeometry other = (MetapopulationGeometry) obj;
		return demeType == other.demeType && metaType == other.metaType && nDemes == other.nDemes
				&& demeSize == other.demeSize && cliDemes == other.cliDemes
				&& cliDemeSize == other.cliDemeSize
				&& Objects.equals(cliDemeArgs, other.cliDemeArgs)
				&& Objects.equals(cliMetaArgs, other.cliMetaArgs);
	}
}
