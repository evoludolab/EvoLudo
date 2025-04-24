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

package org.evoludo.simulator.models;

import java.util.ArrayList;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.ODE.HasDE;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;

/**
 * The class to manage customised markers for graphs.
 * 
 * @author Christoph Hauert
 */
public class Markers {

	/**
	 * The model that is using these markers.
	 */
	Model model;

	/**
	 * The list of markers on graphs. For example to mark fixed points.
	 */
	ArrayList<double[]> markers = new ArrayList<>(5);

	/**
	 * Instantiate new population update for use in IBS {@code model}s.
	 * 
	 * @param model the model using this player update
	 */
	public Markers(Model model) {
		this.model = model;
	}

	/**
	 * Add marker to list of markers. Markers are provided as {@code double[]}
	 * arrays to indicate special frequencies/densities. By default markers are
	 * shown as solid dots or lines, respectively.
	 * 
	 * @param aMark the marker to add
	 * @return {@code true} if successfull
	 */
	public boolean addMarker(double[] aMark) {
		return addMarker(aMark, true);
	}

	/**
	 * Add marker to list of markers. Markers are provided as {@code double[]}
	 * arrays to indicate special frequencies/densities. If {@code filled==true},
	 * markers are shown as solid dots or lines, respectively and as open dots (or
	 * dashed lines), otherwise. This is useful, for example, to indicate the
	 * stability of equilibria.
	 * <p>
	 * In multi-species modules the markers for each species are concatenated into a
	 * single array. The frequencies/densities of the marker for the first species
	 * are stored in <code>aMark[0]</code> through <code>aMark[n1]</code> where
	 * <code>n1</code> denotes the number of traits in the first species. The
	 * current frequencies/densities of the second species are stored in
	 * <code>aMark[n1+1]</code> through <code>aMark[n1+n2]</code> where
	 * <code>n2</code> denotes the number of traits in the second species, etc.
	 * 
	 * @param aMark  the marker to add
	 * @param filled the flag to indicate whether the marker should be filled
	 * @return {@code true} if successfull
	 * 
	 * @see org.evoludo.simulator.models.ODE#yt
	 */
	public boolean addMarker(double[] aMark, boolean filled) {
		// important: data buffers for ParaGraph & co store time in first element
		return markers.add(ArrayMath.insert(aMark, filled ? 1.0 : -1.0, 0));
	}

	/**
	 * Get the list of markers. This serves to mark special values in different
	 * kinds of graphs.
	 * 
	 * @return the list of markers
	 */
	public ArrayList<double[]> getMarkers() {
		return markers;
	}

	/**
	 * Command line option to mark points on graphs (ParaGraph, S3Graph, LineGraph
	 * and HistoGraph). Very convenient to indicate fixed points
	 */
	public final CLOption clo = new CLOption("points", "-none", EvoLudo.catGUI, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse point markers for a single or multiple populations/species. These
				 * translate to markers added to different graphs as appropriate. {@code arg}
				 * can be a single value or an array of values with the separator
				 * {@value CLOParser#MATRIX_DELIMITER} for multiple markers. For multiple
				 * species, the separator is {@value CLOParser#SPECIES_DELIMITER}. The the
				 * values of each fixed point is given by an array with the separator
				 * {@value CLOParser#VECTOR_DELIMITER}.
				 * <p>
				 * <strong>Note:</strong>If one or more entries of the marker are negative
				 * an open marker is drawn otherwise the point is filled.
				 * 
				 * @param arg (array of) array of fixed point value(s)
				 */
				@Override
				public boolean parse(String arg) {
					if (!clo.isSet())
						return true;
					boolean success = true;
					String[] myMarkers = arg.split(CLOParser.MATRIX_DELIMITER);
					if (markers != null)
						markers.clear();
					// model loaded but not yet initialized; getNMean() etc not yet available
					int nSpecies = model.getNSpecies();
					double[] dmk = new double[0];
					for (String aMarker : myMarkers) {
						String[] mk = aMarker.split(CLOParser.SPECIES_DELIMITER);
						boolean mksuccess = true;
						boolean filled = true;
						for (int n = 0; n < nSpecies; n++) {
							double[] smk = CLOParser.parseVector(mk[n]);
							if (ArrayMath.min(smk) < 0.0) {
								filled = false;
								ArrayMath.abs(smk);
							}
							Module module = model.getSpecies(n);
							int nt = module.getNTraits();
							if (smk.length != nt) {
								// ok for frequency based modules or with vacant sites
								int vac = module.getVacant();
								int dep = (module instanceof HasDE ? ((HasDE) module).getDependent() : -1);
								if (!(smk.length == nt - 1 && (vac >= 0 || dep >= 0))) {
									mksuccess = false;
									break;
								}
								if (dep >= 0)
									smk = ArrayMath.insert(smk, 1.0 - ArrayMath.norm(smk), dep);
								else
									// vac >= 0 must hold
									smk = ArrayMath.insert(smk, 0.0, vac);
							}
							// now smk.length == nt holds
							dmk = ArrayMath.merge(dmk, smk);
						}
						if (!mksuccess) {
							model.getLogger().warning("failed to set marker '" + aMarker + "' - ignored.");
							success = false;
							continue;
						}
						addMarker(dmk, filled);
					}
					return success;
				}

				@Override
				public String getDescription() {
					String multi = "";
					if (model.getNSpecies() > 1)
						multi = "[" + CLOParser.SPECIES_DELIMITER + "<j0>,...]";
					String descr = "--points <p>    values of fixed points\n" + //
							"        format: <i0>,<i1>,..." + multi + "[" + CLOParser.MATRIX_DELIMITER
							+ "<k0>,<k1>...] with \n" + //
							"                <nm> values of fixed point(s)";
					return descr;
				}
			});
}
