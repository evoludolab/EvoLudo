package org.evoludo.simulator.models;

import java.io.PrintStream;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;

public class PopulationUpdate {

	/**
	 * The model that is using this population update.
	 * <p>
	 * Currently only IBS models use population updates.
	 */
	IBS ibs;

	/**
	 * Instantiate new population update for use in IBS {@code model}s.
	 * 
	 * @param ibs the model using this player update
	 */
	public PopulationUpdate(IBS ibs) {
		this.ibs = ibs;
	}

	/**
	 * The population update type.
	 * 
	 * @see #clo
	 */
	PopulationUpdate.Type type;

	/**
	 * Sets the population update type.
	 * 
	 * @param type the updating type for the population
	 * @return {@code true} if population update type changed
	 */
	public boolean setType(PopulationUpdate.Type type) {
		if (type == null || type == this.type)
			return false;
		this.type = type;
		return true;
	}

	/**
	 * Gets the population update type.
	 * 
	 * @return the population update type
	 */
	public PopulationUpdate.Type getType() {
		return type;
	}

	/**
	 * Determine whether population update is synchronous.
	 * 
	 * @return {@code true} if update is synchronous
	 */
	public boolean isSynchronous() {
		return (type.equals(Type.SYNC) || type.equals(Type.WRIGHT_FISHER));
	}

	/**
	 * Determine whether population update is a variant of Moran updates. Moran type
	 * updates do not require specifying a player update type separately.
	 * 
	 * @return {@code true} if update is Moran
	 * 
	 * @see Module#clo
	 */
	public boolean isMoran() {
		return (type.equals(Type.MORAN_BIRTHDEATH) || type.equals(Type.MORAN_DEATHBIRTH) || type.equals(Type.MORAN_IMITATE));
	}

	/**
	 * Command line option to set the method for updating the population(s).
	 * 
	 * @see PopulationUpdate.Type
	 */
	public final CLOption clo = new CLOption("popupdate", PopulationUpdate.Type.ASYNC.getKey(), EvoLudo.catModel,
			"--popupdate <u> [<p>]  population update type; fraction p\n" + //
					"                for synchronous updates (1=all):",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse population update types for a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the population update
				 * type set.
				 * 
				 * @param arg the (array of) update types
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] popupdates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module mod : ibs.species) {
						IBSPopulation pop = mod.getIBSPopulation();
						String updt = popupdates[n++ % popupdates.length];
						PopulationUpdate.Type put = (PopulationUpdate.Type) clo.match(updt);
						if (put == null) {
							if (success)
								ibs.logger.warning((ibs.isMultispecies ? mod.getName() + ": " : "") + //
										"population update '" + updt + "' not recognized - using '"
										+ pop.getPopulationUpdate().getType()
										+ "'");
							success = false;
							continue;
						}
						pop.getPopulationUpdate().setType(put);
						// parse p, if present
						String[] args = updt.split("\\s+|=");
						double sync = 1.0;
						if (args.length > 1)
							sync = CLOParser.parseDouble(args[1]);
						pop.setSyncFraction(sync);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module mod : ibs.species) {
						IBSPopulation pop = mod.getIBSPopulation();
						String opt = (pop instanceof IBSDPopulation && ((IBSDPopulation) pop).optimizeMoran
								? " (optimized)"
								: "");
						output.println("# populationupdate:     " + pop.getPopulationUpdate().getType() + opt
								+ (ibs.isMultispecies ? " (" + mod.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Types of species updates (only relevant for multi-species models):
	 * <dl>
	 * <dt>synchronous</dt>
	 * <dd>Synchronized population updates. The number of individuals that reassess
	 * their strategy is determined by the player update noise,
	 * {@link org.evoludo.simulator.modules.PlayerUpdate#getNoise()}. Without noise
	 * all individuals update their strategy, while with high noise levels only a
	 * few update (but at least one individual and each at most once).</dd>
	 * <dt>Wright-Fisher</dt>
	 * <dd>Wright-Fisher process (synchronous)</dd>
	 * <dt>asynchronous</dt>
	 * <dd>Asynchronous population updates (default).</dd>
	 * <dt>Bd</dt>
	 * <dd>Moran process (birth-death, asynchronous).</dd>
	 * <dt>dB</dt>
	 * <dd>Moran process (death-birth, asynchronous).</dd>
	 * <dt>imitate</dt>
	 * <dd>Moran process (imitate, asynchronous).</dd>
	 * <dt>ecology</dt>
	 * <dd>Asynchronous updates (non-constant population size).</dd>
	 * </dl>
	 * For <b>size</b> and <b>fitness</b> selection is also proportional to the
	 * update rate of each species.
	 * 
	 * @see org.evoludo.simulator.models.IBS#clo
	 *      IBS.cloPopulationUpdate
	 * @see org.evoludo.simulator.modules.PlayerUpdate#clo
	 */
	public static enum Type implements CLOption.Key {

		/**
		 * Synchronized population updates. The number of individuals that reassess
		 * their strategy is determined by the player update noise,
		 * {@link Module#cloPlayerUpdateNoise}. Without noise all individuals update
		 * their strategy, while with high noise levels only a few update (but at least
		 * one individual and each at most once).
		 */
		SYNC("synchronous", "synchronized population updates"),

		/**
		 * Wright-Fisher process (synchronous). (not yet implemented!)
		 */
		WRIGHT_FISHER("Wright-Fisher", "Wright-Fisher process (synchronous)"),

		/**
		 * Asynchronous population updates.
		 */
		ASYNC("asynchronous", "asynchronous population updates"),

		/**
		 * Every individual updates exactly once per generation. In contrast for
		 * {@code ASYNC} every individual updates once <strong>on average</strong>.
		 */
		ONCE("once", "everyone updates once (asynchronous)"),

		/**
		 * Moran process (birth-death, asynchronous).
		 */
		MORAN_BIRTHDEATH("Bd", "Moran process (birth-death, asynchronous)"),

		/**
		 * Moran process (death-birth, asynchronous).
		 */
		MORAN_DEATHBIRTH("dB", "Moran process (death-birth, asynchronous)"),

		/**
		 * Moran process (imitate, asynchronous).
		 */
		MORAN_IMITATE("imitate", "Moran process (imitate, asynchronous)"),

		/**
		 * Asynchronous updates (non-constant population size).
		 */
		ECOLOGY("ecology", "asynchronous updates (non-constant population size)");

		/**
		 * Key of population update type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#clo
		 *      IBS.cloPopulationUpdate
		 */
		String key;

		/**
		 * Brief description of population update type for GUI and help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new type of population update.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title the summary of geometry for GUI and help display
		 */
		Type(String key, String title) {
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

		@Override
		public String toString() {
			return key + ": " + title;
		}
	}
}