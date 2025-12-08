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

package org.evoludo.simulator;

import java.awt.Color;
import java.util.Collection;
import java.util.logging.Level;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.Key;

/**
 * Encapsulates command line option setup, preprocessing, and parsing for
 * EvoLudo.
 */
public class CLOController {

	private final EvoLudo engine;
	private final CLOParser parser;

	private boolean reparseCLO = false;
	private String clo = "";

	/**
	 * Command line option to set module.
	 */
	public final CLOption cloModule = new CLOption("module", null, CLOCategory.Global,
			"--module <m>    select module from:", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// option gets special treatment
					return true;
				}
			});

	/**
	 * Command line option to set the type of model (see {@link ModelType}).
	 */
	public final CLOption cloModel = new CLOption("model", ModelType.IBS.getKey(), CLOCategory.Module,
			"--model <m>     model type", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// option gets special treatment
					return true;
				}
			});

	/**
	 * Command line option to set seed of random number generator.
	 */
	public final CLOption cloSeed = new CLOption("seed", "0", CLOption.Argument.OPTIONAL, CLOCategory.Model,
			"--seed [<s>]    set random seed (0)", new CLODelegate() {
				@Override
				public boolean parse(String arg, boolean isSet) {
					if (isSet)
						engine.getRNG().setSeed(Long.parseLong(arg));
					else
						engine.getRNG().clearSeed();
					return true;
				}
			});

	/**
	 * Command line option to request that the EvoLudo model immediately starts
	 * running after loading.
	 */
	public final CLOption cloRun = new CLOption("run", CLOCategory.GUI,
			"--run           simulations run after launch", new CLODelegate() {
				@Override
				public boolean parse(boolean isSet) {
					// by default do not interfere - i.e. leave simulations running if possible
					if (isSet)
						engine.setSuspended(true);
					return true;
				}
			});

	/**
	 * Command line option to set the delay between subsequent updates.
	 */
	public final CLOption cloDelay = new CLOption("delay", "" + EvoLudo.DELAY_INIT, CLOCategory.GUI,
			"--delay <d>     delay between updates (d: delay in msec)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					engine.setDelay(Integer.parseInt(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the color for trajectories. For example, this
	 * affects the display in {@link org.evoludo.simulator.views.S3} or
	 * {@link org.evoludo.simulator.views.Phase2D}.
	 */
	public final CLOption cloTrajectoryColor = new CLOption("trajcolor", "black", CLOCategory.GUI,
			"--trajcolor <c>  color for trajectories\n"
					+ "           <c>: color name or '(r,g,b[,a])' with r,g,b,a in [0-255]",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					Color color = CLOParser.parseColor(arg);
					if (color == null)
						return false;
					engine.getModule().setTrajectoryColor(color);
					return true;
				}
			});

	/**
	 * Command line option to set verbosity level of logging.
	 */
	public final CLOption cloVerbose = new CLOption("verbose", "info", CLOCategory.Global,
			"--verbose <l>   level of verbosity with l one of\n" //
					+ "                all, debug/finest, finer, fine, config,\n" //
					+ "                info, warning, error, or none",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					String larg = arg.toLowerCase();
					if ("all".startsWith(larg)) {
						engine.logger.setLevel(Level.ALL);
						return true;
					}
					if ("debug".startsWith(larg)) {
						engine.logger.setLevel(Level.FINEST);
						return true;
					}
					if ("finest".startsWith(larg)) {
						engine.logger.setLevel(Level.FINEST);
						return true;
					}
					if ("finer".startsWith(larg)) {
						engine.logger.setLevel(Level.FINER);
						return true;
					}
					if ("fine".startsWith(larg)) {
						engine.logger.setLevel(Level.FINE);
						return true;
					}
					if ("debug".startsWith(larg)) {
						engine.logger.setLevel(Level.CONFIG);
						return true;
					}
					if ("warning".startsWith(larg)) {
						engine.logger.setLevel(Level.WARNING);
						return true;
					}
					if ("error".startsWith(larg) || "severe".startsWith(larg)) {
						engine.logger.setLevel(Level.SEVERE);
						return true;
					}
					if ("none".startsWith(larg) || "off".startsWith(larg)) {
						engine.logger.setLevel(Level.OFF);
						return true;
					}
					if ("info".startsWith(larg)) {
						engine.logger.setLevel(Level.INFO);
						return true;
					}
					return false;
				}
			});

	/**
	 * Command line option to print help message for available command line options.
	 */
	public final CLOption cloHelp = new CLOption("help", CLOCategory.Global,
			"--help          print this help screen", new CLODelegate() {
				@Override
				public boolean parse(boolean isSet) {
					if (isSet)
						engine.showHelp();
					return true;
				}
			});

	/**
	 * Replacement command line option for serious parsing failures to display help
	 * screen.
	 */
	public final String[] helpCLO = new String[] { cloHelp.getName() };

	public CLOController(EvoLudo evo) {
		this.engine = evo;
		this.parser = new CLOParser(evo);
	}

	public CLOParser getParser() {
		return parser;
	}

	public String getParserCLO() {
		return parser.getCLO();
	}

	public boolean providesCLO(String name) {
		return parser.providesCLO(name);
	}

	public void addCLOProvider(CLOProvider provider) {
		parser.addCLOProvider(provider);
	}

	public void removeCLOProvider(CLOProvider provider) {
		parser.removeCLOProvider(provider);
	}

	public void requestParseCLO() {
		reparseCLO = true;
	}

	public String getCLO() {
		return clo;
	}

	public void setCLO(String clo) {
		if (clo == null)
			clo = "";
		this.clo = clo.trim();
	}

	public String[] getSplitCLO() {
		// strip all whitespace at start and end
		String[] args = clo.trim().split("\\s+--");
		// strip '--' from first argument
		if (args.length > 0 && args[0].startsWith("--"))
			args[0] = args[0].substring(2);
		return args;
	}

	public int parseCLO() {
		return parseCLO(getSplitCLO());
	}

	public int parseCLO(String[] cloarray) {
		return parsePreprocessedCLO(preprocessCLO(cloarray));
	}

	public int parsePreprocessedCLO(String[] cloarray) {
		parser.setLogger(engine.logger);
		parser.initCLO();
		// preprocessing removed (and possibly altered) --module and --model options
		// add current settings back to cloarray
		if (engine.getModule() != null)
			cloarray = ArrayMath.append(cloarray, cloModule.getName() + " " + engine.getModule().getKey());
		if (engine.getModel() != null)
			cloarray = ArrayMath.append(cloarray, cloModel.getName() + " " + engine.getModel().getType().getKey());
		int issues = parser.parseCLO(cloarray);
		if (reparseCLO) {
			// start again from scratch
			reparseCLO = false;
			parser.initCLO();
			return parser.parseCLO(cloarray);
		}
		return issues;
	}

	public String[] preprocessCLO(String[] cloarray) {
		// first, deal with --help option
		boolean helpRequested = containsHelpOption(cloarray);

		// handle module option (preprocessing requires module to be loaded early)
		cloarray = handleModuleOption(cloarray, helpRequested);

		// determine feasible --model options for given module
		cloModel.clearKeys();
		Module<?> module = engine.getModule();
		ModelType[] mt = module.getModelTypes();
		cloModel.addKeys(mt);

		// handle model option
		cloarray = handleModelOption(cloarray, helpRequested);

		// handle verbose option early so parsing reports use desired logging level
		cloarray = handleVerboseOption(cloarray);

		return cloarray;
	}

	private boolean containsHelpOption(String[] cloarray) {
		String helpName = cloHelp.getName();
		for (String param : cloarray) {
			if (param.startsWith(helpName))
				return true;
		}
		return false;
	}

	private String[] handleModuleOption(String[] cloarray, boolean helpRequested) {
		String moduleParam = cloModule.getName();
		CLOption.Key moduleKey = null;
		String moduleName = "";
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (!param.startsWith(moduleParam))
				continue;
			String[] moduleArgs = param.split("[\\s+,=]");
			if (moduleArgs == null || moduleArgs.length < 2) {
				if (!helpRequested)
					engine.logger.severe("module key missing");
				return helpCLO;
			}
			moduleName = moduleArgs[1];
			moduleKey = cloModule.match(moduleName);
			// exact match required
			if (moduleKey != null && !moduleKey.getKey().equals(moduleName))
				moduleKey = null;
			// remove the processed module option
			cloarray = ArrayMath.drop(cloarray, i);
			break;
		}
		if (moduleKey == null || !engine.loadModule(moduleKey.getKey())) {
			if (!helpRequested)
				engine.logger.severe("Use --module to load a module or --help for more information.");
			return helpCLO;
		}
		return cloarray;
	}

	private String[] handleModelOption(String[] cloarray, boolean helpRequested) {
		String modelName = cloModel.getName();
		Collection<Key> keys = cloModel.getKeys();
		if (keys.isEmpty()) {
			if (!helpRequested)
				engine.logger.severe("No model found!");
			return helpCLO;
		}
		ModelType defaulttype = (ModelType) cloModel.match(cloModel.getDefault());
		ModelType type = null;
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (!param.startsWith(modelName))
				continue;
			String newModel = CLOption.stripKey(modelName, param).trim();
			// remove model option
			cloarray = ArrayMath.drop(cloarray, i);
			if (newModel.isEmpty()) {
				type = defaulttype;
				if (!helpRequested)
					engine.logger.warning("model key missing - use default type " + type.getKey() + ".");
				break;
			}
			type = ModelType.parse(newModel);
			if (type == null || !keys.contains(type)) {
				Model activeModel = engine.getModel();
				if (activeModel != null) {
					type = activeModel.getType();
					if (!helpRequested)
						engine.logger.warning(
								"invalid model type " + newModel + " - keep current type " + type.getKey() + ".");
				} else {
					type = defaulttype;
					if (!helpRequested)
						engine.logger.warning(
								"invalid model type " + newModel + " - use default type " + type.getKey() + ".");
				}
			}
			break;
		}
		if (type == null) {
			type = defaulttype;
			if (keys.size() > 1 && !defaulttype.getKey().equals(cloModel.getDefault()) && !helpRequested)
				engine.logger.warning("model type unspecified - use default type " + type.getKey() + ".");
		}
		// NOTE: currently models cannot be mix'n'matched between species
		engine.loadModel(type);
		if (engine.getModel() == null) {
			if (!helpRequested)
				engine.logger.severe("model type '" + type.getKey() + "' not supported!");
			return helpCLO;
		}
		return cloarray;
	}

	private String[] handleVerboseOption(String[] cloarray) {
		String verboseName = cloVerbose.getName();
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (!param.startsWith(verboseName))
				continue;
			String verbosity = CLOption.stripKey(verboseName, param).trim();
			if (verbosity.isEmpty()) {
				engine.logger.warning("verbose level missing - ignored.");
				// remove verbose option
				return ArrayMath.drop(cloarray, i);
			}
			// parse --verbose first to set logging level already for processing of command
			// line arguments; gets processed again with all others but no harm in it
			cloVerbose.setArg(verbosity);
			cloVerbose.parse();
			break;
		}
		return cloarray;
	}

	public String getCLOHelp() {
		// list trait indices and names
		String globalMsg = "List of command line options";
		Module<?> module = engine.getModule();
		Model activeModel = engine.getModel();
		if (module != null) {
			globalMsg += " for module '" + module.getKey() + "'";
			if (activeModel != null) {
				globalMsg += " and model '" + activeModel.getType().getKey() + "'";
				CLOCategory.Model.setHeader("Options for model '" + activeModel.getType().getKey() + "'");
			} else
				globalMsg += " (select model for more options)";
			globalMsg += "\n\nGlobal options:";

			int idx = 0;
			StringBuilder sb = new StringBuilder();
			for (Module<?> mod : module.getSpecies()) {
				String name = mod.getName();
				int namelen = name.length();
				if (namelen > 0)
					sb.append("\n       Species: ")
							.append(name);
				int nt = mod.getNTraits();
				for (int n = 0; n < nt; n++) {
					sb.append("\n             ")
							.append(idx + n)
							.append(": ")
							.append(mod.getTraitName(n));
					if (mod instanceof Model.HasDE && ((Model.HasDE) mod).getDependent() == n) {
						sb.append(" (dependent)");
					}
				}
				idx += nt;
			}
			String moduleMsg = sb.toString();
			CLOCategory.Module.setHeader("Options for module '" + module.getKey() //
					+ "' with trait indices and names:" + moduleMsg);
		} else
			globalMsg += " (select module and model for more info):";
		CLOCategory.Global.setHeader(globalMsg);
		return parser.helpCLO(true);
	}

	public void collectCLO(CLOParser prsr) {
		prsr.addCLO(cloHelp);
		prsr.addCLO(cloVerbose);
		if (!EvoLudo.isGWT)
			// default verbosity if running as java application is warning
			cloVerbose.setDefault("warning");
		prsr.addCLO(cloModule);
		prsr.addCLO(cloModel);
		prsr.addCLO(cloSeed);
		prsr.addCLO(cloRun);
		prsr.addCLO(cloDelay);
		Module<?> module = engine.getModule();

		// trajectory color settings used by phase plane and simplex plots
		if (module instanceof HasS3 || module instanceof HasPhase2D)
			prsr.addCLO(cloTrajectoryColor);
	}

	/**
	 * Notify controller that available module keys changed.
	 * 
	 * @param key   module key
	 * @param title module title
	 */
	public void addModuleKey(String key, String title) {
		cloModule.addKey(key, title);
	}
}
