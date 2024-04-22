//
// EvoLudo Project
//
// Copyright 2022 Christoph Hauert
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

package org.evoludo.simulator.modules;

import java.awt.Color;
import java.io.PrintStream;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.ODERK;
import org.evoludo.simulator.views.HasConsole;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * The module to investigate eco-evolutionary interactions between two species,
 * inspired by bacterial hosts and their phages. The model is derived from the
 * following reaction kinetics equations:
 * \[
 * \begin{alignat}{2}
 * H_U &amp;\overset{b_U}{\longrightarrow} &amp;&amp;\ H_U +H_U \\
 * H_U &amp;\overset{d_U}{\longrightarrow} &amp;&amp;\ \emptyset \\
 * H_U + H_U &amp;\overset{\xi}{\longrightarrow} &amp;&amp;\ H_U\\
 * H_U + P &amp;\overset{\mu_1}{\longrightarrow} &amp;&amp;\ H_I \\
 * H_U + P + P &amp;\overset{\mu_2}{\longrightarrow} &amp;&amp;\ H_I\\
 * H_I &amp;\overset{d}{\longrightarrow} &amp;&amp;\ \lambda P \\
 * P &amp;\overset{d\kappa}{\longrightarrow} &amp;&amp;\ \emptyset,
 * \end{alignat}
 * \]
 * which models single and simultaneous double infections of hosts. Only
 * uninfected hosts \(H_U\) replicate at rate \(b_U\), die at rate \(d_U\),
 * compete at rate \(\xi\) and get singly infected at rate \(\mu_1\) while
 * simultaneous double infections occur at rate \(\mu_2\). Infected host \(H_I\)
 * lyse due to the phages \(P\) at rate \(d\) and release \(\lambda\) phages
 * into the environment. Finally phages perish at a rate \(d\kappa\) such that
 * for \(\kappa&gt;1\) they are shorter lived than their hosts but longer for
 * \(\kappa&lt;1\).
 * <p>
 * According to the mass-action principle this yields the following system of
 * ordinary differential equations (ODE's):
 * \[
 * \begin{align}
 * \dot{H}_U =&amp; r\,H_U - \xi\,H_U^2 - \mu_1\,H_U\,P - \mu_2\,H_U\,P^2 \\
 * \dot{H}_I =&amp; \mu_1\,H_U\,P + \mu_2\,H_U\,P^2 - d\,H_I \\
 * \dot{P} =&amp; d\,\lambda\, H_I - d\,\kappa\, P.
 * \end{align}
 * \]
 * 
 * <h3>Notes:</h3>
 * <ol>
 * <li>Currently implements only ODE model.
 * <li>In order to implement {@code HasSDE} a system size expansion of the
 * reaction kinetics/transition probabilities is necessary. Size of each species
 * matters and determines the respective demographic noise. In contrast to
 * {@code ODEEuler}, {@code SDEEuler} not yet set up for multi-species. Probably
 * easier to implement than for replicator equations because there are no
 * conserved quantities, i.e. no correlations between noise in different
 * dynamical variables.
 * </ol>
 *
 * @author Christoph Hauert
 */
public class EcoMutualism extends Discrete implements /* Discrete.Pairs, */
		/* HasIBS, */ HasODE, // HasSDE, // PDEs not (yet) an option
		HasPop2D.Strategy, HasPop3D.Strategy, HasMean.Strategy, HasPhase2D, HasPop2D.Fitness, HasPop3D.Fitness,
		/* HasMean.Fitness, HasHistogram.Fitness, HasHistogram.StatisticsStationary, */ HasHistogram.Degree, HasConsole {

	/**
	 * The trait (and index) value of hosts infected by the defector strain of the
	 * phage.
	 */
	public static final int HOST_DEFECT = 0;

	/**
	 * The trait (and index) value of hosts infected by the cooperator strain of the
	 * phage.
	 */
	public static final int HOST_COOPERATE = 1;

	/**
	 * The trait (and index) value of hosts infected by both cooperator and defector
	 * strains of the phage.
	 */
	public static final int HOST_MIXED = 2;

	/**
	 * The trait (and index) value of uninfected hosts.
	 */
	public static final int HOST_NONE = 3;

	/**
	 * The trait (and index) value of the defector strain of the phage.
	 */
	public static final int VIRUS_DEFECT = 0;

	/**
	 * The trait (and index) value of the cooperator strain of the phage.
	 */
	public static final int VIRUS_COOPERATE = 1;

	/**
	 * The reference to the partner species with {@code partner.partner == this}.
	 */
	EcoMutualism partner;

	/**
	 * The reference to the phage species. Convenience field to reduce casts. Same
	 * as {@code partner} in the host species.
	 */
	Phage phage;

	public static class Phage extends EcoMutualism {

		/**
		 * The payoff for mutual cooperation. The number of phages released by host
		 * infected by cooperator phage.
		 */
		double alpha;

		/**
		 * The payoff to a cooperator when facing a defector. The number of defector
		 * phages released by host infected by both strains.
		 */
		double beta;

		/**
		 * The payoff to a defector when facing a cooperator. The number of cooperator
		 * phages released by host infected by both strains.
		 */
		double gamma;

		/**
		 * The payoff for mutual defection. The number of phages released by host
		 * infected by defector phage.
		 */
		double delta;

		/**
		 * Create a new instance of the phage population.
		 * 
		 * @param partner the reference to the partner species
		 */
		public Phage(EcoMutualism partner) {
			super(partner);
			setName("Mutualist");
			setNTraits(3); // 2 plus empty sites
			VACANT = nTraits - 1;
			// trait names
			String[] names = new String[nTraits];
			names[VIRUS_DEFECT] = "Defector";
			names[VIRUS_COOPERATE] = "Cooperator";
			names[VACANT] = "Vacant";
			setTraitNames(names);
			// trait colors (automatically generates lighter versions for new strategists)
			Color[] colors = new Color[nTraits];
			colors[VIRUS_DEFECT] = Color.ORANGE;
			colors[VIRUS_COOPERATE] = Color.GREEN;
			colors[VACANT] = Color.LIGHT_GRAY;
			setTraitColors(colors);
		}

		@Override
		public double getMinGameScore() {
			return Math.min(alpha, Math.min(beta, Math.min(gamma, delta)));
		}

		@Override
		public double getMaxGameScore() {
			return Math.max(alpha, Math.max(beta, Math.max(gamma, delta)));
		}

		/**
		 * Set the payoffs from the {@code 2×2} matrix {@code payoffs} for phage
		 * interactions. This represents the rates at which cooperator and defector
		 * phages are released by hosts that are infected by cooperator or defector
		 * strains, or both.
		 * 
		 * @param payoffs the phage payoff matrix
		 */
		public void setPayoffs(double[][] payoffs) {
			alpha = payoffs[VIRUS_COOPERATE][VIRUS_COOPERATE];
			beta = payoffs[VIRUS_COOPERATE][VIRUS_DEFECT];
			gamma = payoffs[VIRUS_DEFECT][VIRUS_COOPERATE];
			delta = payoffs[VIRUS_DEFECT][VIRUS_DEFECT];
		}

		/**
		 * Get the payoffs as a {@code 2×2} matrix for phage interactions. This
		 * represents the rates at which cooperator and defector phages are released by
		 * hosts that are infected by cooperator or defector strains, or both.
		 * 
		 * @return the phage payoff matrix (or {@code null} for the host)
		 */
		public double[][] getPayoffs() {
			final double[][] payoffs = new double[2][2];
			payoffs[VIRUS_COOPERATE][VIRUS_COOPERATE] = alpha;
			payoffs[VIRUS_COOPERATE][VIRUS_DEFECT] = beta;
			payoffs[VIRUS_DEFECT][VIRUS_COOPERATE] = gamma;
			payoffs[VIRUS_DEFECT][VIRUS_DEFECT] = delta;
			return payoffs;
		}
	}

	/**
	 * Create a new instance of the module for eco-evolutionary host phage interactions.
	 * 
	 * <h3>Important:</h3>
	 * This instantiates only the skeleton and only of one species. The other
	 * species is created only when {@code load}ing this module.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
	 * 
	 * @see Phage
	 */
	public EcoMutualism(EvoLudo engine) {
		super(engine);

		setName("Host");
		setNTraits(5); // 4 plus empty sites
		VACANT = nTraits - 1;
	}

	/**
	 * Only used during the creation of the phage population.
	 * 
	 * @param partner the reference to the partner species
	 */
	protected EcoMutualism(EcoMutualism partner) {
		super(partner);
	}

	@Override
	public void load() {
		super.load();
		if (phage == null) {
			// loading the host population
			phage = new Phage(this);
			phage.phage = phage;
			phage.load();
			partner = phage;
			partner.partner = this;
			// NOTE: traitName and traitColor are cleared when unloading module
			// trait names
			String[] names = new String[nTraits];
			names[HOST_NONE] = "Uninfected";
			names[HOST_DEFECT] = "Defector";
			names[HOST_COOPERATE] = "Cooperator";
			names[HOST_MIXED] = "Mixed";
			names[VACANT] = "Vacant";
			setTraitNames(names);
			// trait colors (automatically generates lighter versions for new strategists)
			Color[] colors = new Color[nTraits];
			colors[HOST_NONE] = Color.BLACK;
			colors[HOST_DEFECT] = Color.RED;
			colors[HOST_COOPERATE] = Color.BLUE;
			colors[HOST_MIXED] = Color.PINK;
			colors[VACANT] = Color.LIGHT_GRAY;
			setTraitColors(colors);
			// by default show HOST_COOPERATE on horizontal and VIRUS_COOPERATE on
			// vertical axis of phase plane
			phase2DTraitX = new int[] { HOST_COOPERATE };
			phase2DTraitY = new int[] { nTraits + VIRUS_COOPERATE};
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <h3>Important:</h3>
	 * This method is only called for the first created instance (see
	 * {@link #EcoMutualism(EvoLudo)}) but not it's partner (see
	 * {@link Phage#Phage(EcoMutualism)}). In order to properly unload both species
	 * {@code partner.unload()} needs to be called. However, to avoid infinite
	 * loops, use, e.g.
	 * 
	 * <pre>
	 * if (partner != null) {
	 * 	partner.partner = null;
	 * 	partner.unload();
	 * 	partner = null;
	 * }
	 * 
	 * </pre>
	 * 
	 * @see #EcoMutualism(EvoLudo)
	 * @see Phage#Phage(EcoMutualism)
	 */
	@Override
	public void unload() {
		super.unload();
		phage = null;
		if (partner != null) {
			partner.partner = null;
			partner.unload();
			partner = null;
		}
		phase2DTraitX = null;
		phase2DTraitY = null;
	}

	@Override
	public Model.ODE createODE() {
		return new EcoMutualism.ODE(engine);
	}

	@Override
	public String getKey() {
		return "eMut";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\nHosts & Viruses.";
	}

	@Override
	public String getTitle() {
		return "Ecological Mutualisms";
	}

	@Override
	public String getVersion() {
		return "v1.0 May 2022";
	}

	int[] phase2DTraitX;
	int[] phase2DTraitY;
	Data2Phase map;

	@Override
	public void setPhase2DMap(Data2Phase map) {
		this.map = map;
		map.setMultitrait(true);
		map.setTraits(phase2DTraitX, phase2DTraitY);
	}

	@Override
	public double getMinGameScore() {
		return -Double.MAX_VALUE;
	}

	@Override
	public double getMaxGameScore() {
		return Double.MAX_VALUE;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> mono score is ill defined as the homogeneous states of
	 * the population and its opponent is required!
	 * </p>
	 */
	@Override
	public double getMonoGameScore(int mono) {
		return Double.NaN;
	}

	// /**
	//  * {@inheritDoc}
	//  * <p>
	//  * <strong>Note:</strong> <code>count</code> refers to opponent (possibly
	//  * different species)
	//  */
	// @Override
	// public void mixedScores(int[] count, double[] traitScores) {
	// 		// avoid self interactions in intra-species interactions
	// 		int selfi = getInteractionGeometry().isInterspecies() ? 0 : -1;
	// 		int x = count[COOPERATE];
	// 		int y = count[DEFECT];
	// 		double in = 1.0 / (x + y - selfi);
	// 		traitScores[COOPERATE] = ((x - selfi) * reward + y * sucker) * in;
	// 		traitScores[DEFECT] = (x * temptation + (y - selfi) * punishment) * in;
	// }

	// @Override
	// public double pairScores(int me, int[] tCount, double[] tScore) {
	// 	switch (me) {
	// 		case COOPERATE:
	// 			tScore[COOPERATE] = partner.reward;
	// 			tScore[DEFECT] = partner.temptation;
	// 			return tCount[COOPERATE] * reward + tCount[DEFECT] * sucker;

	// 		case DEFECT:
	// 			tScore[COOPERATE] = partner.sucker;
	// 			tScore[DEFECT] = partner.punishment;
	// 			return tCount[COOPERATE] * temptation + tCount[DEFECT] * punishment;

	// 		default: // should not end here
	// 			throw new Error("Unknown strategy (" + me + ")");
	// 	}
	// }

	/**
	 * The birth rate of uninfected hosts.
	 */
	double birthRate;

	/**
	 * The competition rate of uninfected hosts.
	 */
	double compRate;

	/**
	 * The death rate of phages relative to their hosts. For \(\kappa &gt;1\) phages
	 * are shorter lived but longer lived than their host for \(\kappa &gt;1\).
	 */
	double kappa;
	
	/**
	 * The rate at which phages are released into the environment when a host lyses.
	 * For example, host infected by the cooperator phage strain releases phages at
	 * a rate \(\alpha \lambda\), while a host infected by both strains releases
	 * defector phages at rate \(\gamma \lambda\) and cooperator phages at rate
	 * \(\beta \lambda\).
	 */
	double lambda;

	/**
	 * The rate of single infections of uninfected hosts by phages.
	 */
	double mu1;

	/**
	 * The birth rate of simultaneous double infections of uninfected hosts by
	 * phages.
	 */
	double mu2;

	/**
	 * Get the birth rate of uninfected hosts.
	 * 
	 * @return the birth rate
	 */
	public double getBirthRate() {
		return birthRate;
	}

	/**
	 * Set the birth rate of uninfected hosts.
	 * 
	 * @param birthRate the birth rate
	 */
	public void setBirthRate(double birthRate) {
		this.birthRate = birthRate;
	}

	/**
	 * Get the competition rate of uninfected hosts.
	 * 
	 * @return the competition rate
	 */
	public double getCompetitionRate() {
		return compRate;
	}

	/**
	 * Set the competition rate of uninfected hosts.
	 * 
	 * @param compRate the competition rate
	 */
	public void setCompetitionRate(double compRate) {
		this.compRate = compRate;
	}

	/**
	 * Get the relative rate of death of phages as compared to the host. 
	 * 
	 * @return the relative rate of death
	 * 
	 * @see #kappa
	 */
	public double getKappa() {
		return kappa;
	}

	/**
	 * Set the relative rate of death of phages as compared to the host. 
	 * 
	 * @param kappa the relative rate of death
	 * 
	 * @see #kappa
	 */
	public void setKappa(double kappa) {
		this.kappa = kappa;
	}

	/**
	 * Get the rate at which phages are released into the environment when a host lyses.
	 * 
	 * @return the rate for releasing phages
	 * 
	 * @see #lambda
	 */
	public double getLambda() {
		return lambda;
	}

	/**
	 * Set the rate at which phages are released into the environment when a host lyses.
	 * 
	 * @param lambda the rate for releasing phages
	 * 
	 * @see #lambda
	 */
	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	/**
	 * Get the rate of single infections.
	 * 
	 * @return the rate of single infections
	 */
	public double getSingleRate() {
		return mu1;
	}

	/**
	 * Set the rate of single infections.
	 * 
	 * @param mu1 the rate of single infections
	 */
	public void setSingleRate(double mu1) {
		this.mu1 = mu1;
	}

	/**
	 * Get the rate of simultaneous double infections.
	 * 
	 * @return the rate of simultaneous double infections
	 */
	public double getDoubleRate() {
		return mu2;
	}

	/**
	 * Set the rate of simultaneous double infections.
	 * 
	 * @param mu2 the rate of simultaneous double infections
	 */
	public void setDoubleRate(double mu2) {
		this.mu2 = mu2;
	}

	/**
	 * Command line option to set the game parameters for ecological population
	 * updates. The default is a donation game with cost-to-benefit ratio of
	 * <code>c/b = 0.16</code>.
	 */
	public final CLOption cloPayVirus = new CLOption("payvirus", "0.16,1.16;0,1", EvoLudo.catModule,
			"--payvirus <a,b;c,d>  payoff matrix for virus reproduction", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse the payoff matrix of the phage species interacting with the host
				 * species through {@code 2×2} games. The argument must have the form
				 * \(\delta,\gamma;\beta,\alpha\) referring to the payoffs for mutual defection,
				 * defectors against a cooperator, cooperators against a defector, and for
				 * mutual defection, respectively. Note '{@value CLOParser#VECTOR_DELIMITER}'
				 * separates entries in 1D arrays and '{@value CLOParser#MATRIX_DELIMITER}'
				 * separates rows in 2D arrays.
				 * 
				 * @param arg the payoff matrix
				 */
				@Override
				public boolean parse(String arg) {
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix == null || payMatrix.length != 2 || payMatrix[0].length != 2) {
						logger.warning("invalid paymutualist parameter (" + arg + ") - using '"
								+ cloPayVirus.getDefault() + "'");
						return false;
					}
					phage.setPayoffs(payMatrix);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# payvirus:             " + Formatter.format(phage.getPayoffs(), 4));
				}
			});

	/**
	 * Command line option to set the birth rate uninfected hosts.
	 */
	public final CLOption cloBirthRate = new CLOption("birthrate", "26", EvoLudo.catModule,
			"--birthrate <b>  rate of reproduction", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse birth rate for ecological mutualism model.
				 * 
				 * @param arg birth rate
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					double rate = CLOParser.parseDouble(arg);
					for (Module pop : species) {
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "deathrate must be non-negative - using " + ((EcoMutualism) pop).getBirthRate()
									+ "!");
							success = false;
							continue;
						}
						((EcoMutualism) pop).setBirthRate(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println("# birthrate:   " + Formatter.format(((EcoMutualism) pop).getBirthRate(), 4)
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set rate of competition of uninfected hosts.
	 */
	public final CLOption cloCompRate = new CLOption("comprate", "0.1", EvoLudo.catModule,
			"--comprate <c>  rate of competition", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse competition rate for ecological mutualism model.
				 * 
				 * @param arg competition rate
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					double rate = CLOParser.parseDouble(arg);
					for (Module pop : species) {
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "competition rate must be non-negative - using "
									+ ((EcoMutualism) pop).getCompetitionRate() + "!");
							success = false;
							continue;
						}
						((EcoMutualism) pop).setCompetitionRate(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println(
								"# competition: " + Formatter.format(((EcoMutualism) pop).getCompetitionRate(), 4)
										+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the death rate of phages relative to their host.
	 */
	public final CLOption cloKappa = new CLOption("kappa", "1.0", EvoLudo.catModule,
			"--kappa <k>     death factor of virus", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse death factor of virus for ecological mutualism model.
				 * 
				 * @param arg death factor of virus
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					double rate = CLOParser.parseDouble(arg);
					for (Module pop : species) {
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "death factor of virus must be non-negative - using "
									+ ((EcoMutualism) pop).getKappa() + "!");
							success = false;
							continue;
						}
						((EcoMutualism) pop).setKappa(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println("# deathvirus: " + Formatter.format(((EcoMutualism) pop).getKappa(), 4)
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the rate at which phages are released into the environment when a host lyses.
	 * 
	 * @see #lambda
	 */
	public final CLOption cloLambda = new CLOption("lambda", "1.0", EvoLudo.catModule,
			"--lambda <l>    birth factor of virus", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse birth factor of virus for ecological mutualism model.
				 * 
				 * @param arg birth factor of virus
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					double rate = CLOParser.parseDouble(arg);
					for (Module pop : species) {
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "birth factor of virus must be non-negative - using "
									+ ((EcoMutualism) pop).getLambda() + "!");
							success = false;
							continue;
						}
						((EcoMutualism) pop).setLambda(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println("# birthvirus: " + Formatter.format(((EcoMutualism) pop).getLambda(), 4)
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the rate of single infection of hosts by phages.
	 */
	public final CLOption cloSingle = new CLOption("single", "0.1", EvoLudo.catModule,
			"--single <s>    rates of single infections", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse single infection rates of hosts by virus for ecological mutualism
				 * model.
				 * 
				 * @param arg single infection rates of hosts
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					double rate = CLOParser.parseDouble(arg);
					for (Module pop : species) {
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "single infection rate must be non-negative - using "
									+ ((EcoMutualism) pop).getSingleRate() + "!");
							success = false;
							continue;
						}
						((EcoMutualism) pop).setSingleRate(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println("# singleinf:  " + Formatter.format(((EcoMutualism) pop).getSingleRate(), 4)
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the rate of simultanous double infection of hosts by phages.
	 */
	public final CLOption cloDouble = new CLOption("double", "0.01", EvoLudo.catModule,
			"--double <d>    rates of double infections", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse double infection rates of hosts by virus for ecological mutualism
				 * model.
				 * 
				 * @param arg double infection rates of hosts
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					double rate = CLOParser.parseDouble(arg);
					for (Module pop : species) {
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "double infection rate must be non-negative - using "
									+ ((EcoMutualism) pop).getDoubleRate() + "!");
							success = false;
							continue;
						}
						((EcoMutualism) pop).setDoubleRate(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println("# doubleinf:  " + Formatter.format(((EcoMutualism) pop).getDoubleRate(), 4)
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloPayVirus);
		parser.addCLO(cloBirthRate);
		parser.addCLO(cloCompRate);
		parser.addCLO(cloKappa);
		parser.addCLO(cloLambda);
		parser.addCLO(cloSingle);
		parser.addCLO(cloDouble);
		super.collectCLO(parser);
	}

	public class ODE extends ODERK {

		// convenience variables
		double r, d, xi;
		double alpha, beta, gamma, delta;

		protected ODE(EvoLudo engine) {
			super(engine);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Ecological dynamics is unable to deal with time reversal due to disspative
		 * terms arising from e.g. from death rates.
		 */
		@Override
		public boolean permitsTimeReversal() {
			return false;
		}

		@Override
		public boolean isDensity() {
			return true;
		}

		@Override
		public boolean check() {
			super.check();
			// some abbreviations
			d = deathRate;
			r = birthRate - d;
			xi = compRate;
			alpha = phage.alpha;
			beta = phage.beta;
			gamma = phage.gamma;
			delta = phage.delta;
			return false;
		}

		@Override
		protected void getDerivatives(double time, double[] state, double[] fit, double[] change) {
			double hu = state[EcoMutualism.HOST_NONE];
			double hc = state[EcoMutualism.HOST_COOPERATE];
			double hd = state[EcoMutualism.HOST_DEFECT];
			double hm = state[EcoMutualism.HOST_MIXED];
			double ec = state[5 + EcoMutualism.VIRUS_COOPERATE];
			double ed = state[5 + EcoMutualism.VIRUS_DEFECT];

			change[EcoMutualism.HOST_NONE] = hu * (r - (ec + ed) * (mu1 + mu2 * (ec + ed)) - xi * hu);
			change[EcoMutualism.HOST_COOPERATE] = hu * ec * (mu1 + ec * mu2) - d * hc;
			change[EcoMutualism.HOST_DEFECT] = hu * ed * (mu1 + ed * mu2) - d * hd;
			change[EcoMutualism.HOST_MIXED] = hu * mu2 * ec * ed * 2 - d * hm;
			change[5 + EcoMutualism.VIRUS_COOPERATE] = lambda * d * (alpha * hc + beta * hm) - kappa * d * ec;
			change[5 + EcoMutualism.VIRUS_DEFECT] = lambda * d * (gamma * hm + delta * hd) - kappa * d * ed;
		}
	}
}
