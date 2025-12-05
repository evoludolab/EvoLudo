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

import java.util.Collection;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.Key;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLOProvider;

/**
 * Helper to manage command line options and parsing.
 */
class CLOController {

	private final EvoLudo engine;
	/**
	 * Flag to request reparsing of command line options.
	 */
	private boolean reparseCLO = false;
	/**
	 * Raw command line options string.
	 */
	private String clo = "";
	/**
	 * The parser for command line options.
	 */
	protected CLOParser parser;
	/**
	 * Replacement command line option for serious parsing failures to display help
	 * screen.
	 */
	public final String[] helpCLO;

	CLOController(EvoLudo engine) {
		this.engine = engine;
		this.helpCLO = new String[] { engine.cloHelp.getName() };
	}

	public void requestParseCLO() {
		reparseCLO = true;
	}

	/**
	 * Get the raw command line options, as provided in URL, HTML tag, settings
	 * TextArea or command line.
	 * 
	 * @return command line options
	 */
	public String getCLO() {
		return clo;
	}

	/**
	 * Get the command line options split into an array with option names followed
	 * by their arguments (if applicable).
	 * 
	 * @return array command line options and arguments
	 */
	public String[] getSplitCLO() {
		String[] args = clo.trim().split("\\s+--");
		if (args[0].startsWith("--"))
			args[0] = args[0].substring(2);
		return args;
	}

	/**
	 * Set the raw command line options, as shown e.g. in the settings TextArea.
	 * 
	 * @param clo the new command line option string
	 */
	public void setCLO(String clo) {
		if (clo == null)
			clo = "";
		this.clo = clo.trim();
	}

	/**
	 * Register <code>provider</code> as a provider of command line options.
	 * Initialize command line parser if necessary.
	 *
	 * @param provider the option provider to add
	 */
	public void addCLOProvider(CLOProvider provider) {
		if (provider == null)
			return;
		if (parser == null)
			parser = new CLOParser(engine);
		parser.addCLOProvider(provider);
	}

	/**
	 * Unregister <code>provider</code> as a provider of command line options.
	 *
	 * @param provider the option provider to remove
	 */
	public void removeCLOProvider(CLOProvider provider) {
		if (parser == null || provider == null)
			return;
		parser.removeCLOProvider(provider);
	}

	/**
	 * Pre-process array of command line arguments. Handles help, module, model and
	 * verbose options before full parsing.
	 * 
	 * @param cloarray array of command line arguments
	 * @return pre-processed array of command line options
	 */
	protected String[] preprocessCLO(String[] cloarray) {
		boolean helpRequested = containsHelpOption(cloarray);
		cloarray = handleModuleOption(cloarray, helpRequested);
		engine.cloModel.clearKeys();
		ModelType[] mt = engine.activeModule.getModelTypes();
		engine.cloModel.addKeys(mt);
		cloarray = handleModelOption(cloarray, helpRequested);
		cloarray = handleVerboseOption(cloarray);
		return cloarray;
	}

	/**
	 * Check whether the help option is present.
	 * 
	 * @param cloarray array of command line arguments
	 * @return <code>true</code> if help option is present
	 */
	private boolean containsHelpOption(String[] cloarray) {
		String helpName = engine.cloHelp.getName();
		for (String param : cloarray) {
			if (param.startsWith(helpName))
				return true;
		}
		return false;
	}

	/**
	 * Handle the --module option.
	 * 
	 * @param cloarray      array of command line arguments
	 * @param helpRequested <code>true</code> if help option was requested
	 * @return possibly modified cloarray or helpCLO to signal help
	 */
	private String[] handleModuleOption(String[] cloarray, boolean helpRequested) {
		String moduleParam = engine.cloModule.getName();
		CLOption.Key moduleKey = null;
		String moduleName = "";
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (param.startsWith(moduleParam))
				continue;
			String[] moduleArgs = param.split("[\\s+,=]");
			if (moduleArgs == null || moduleArgs.length < 2) {
				if (!helpRequested)
					engine.logger.severe("module key missing");
				return helpCLO;
			}
			moduleName = moduleArgs[1];
			moduleKey = engine.cloModule.match(moduleName);
			if (moduleKey != null && !moduleKey.getKey().equals(moduleName))
				moduleKey = null;
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

	/**
	 * Handle the --model option.
	 * 
	 * @param cloarray      array of command line arguments
	 * @param helpRequested <code>true</code> if help option was requested
	 * @return possibly modified cloarray or helpCLO to signal help
	 */
	private String[] handleModelOption(String[] cloarray, boolean helpRequested) {
		String modelName = engine.cloModel.getName();
		Collection<Key> keys = engine.cloModel.getKeys();
		if (keys.isEmpty()) {
			if (!helpRequested)
				engine.logger.severe("No model found!");
			return helpCLO;
		}
		ModelType defaulttype = (ModelType) engine.cloModel.match(engine.cloModel.getDefault());
		ModelType type = null;
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (param.startsWith(modelName))
				continue;
			String newModel = CLOption.stripKey(modelName, param).trim();
			cloarray = ArrayMath.drop(cloarray, i);
			if (newModel.isEmpty()) {
				type = defaulttype;
				if (!helpRequested)
					engine.logger.warning("model key missing - use default type " + type.getKey() + ".");
				break;
			}
			type = ModelType.parse(newModel);
			if (type == null || !keys.contains(type)) {
				if (engine.activeModel != null) {
					type = engine.activeModel.getType();
					if (!helpRequested)
						engine.logger.warning(
								"invalid model type " + newModel + " - keep current type " + type.getKey() + ".");
				} else {
					type = defaulttype;
					if (!helpRequested)
						engine.logger
								.warning("invalid model type " + newModel + " - use default type " + type.getKey()
										+ ".");
				}
			}
			break;
		}
		if (type == null) {
			type = defaulttype;
			if (keys.size() > 1 && !defaulttype.getKey().equals(engine.cloModel.getDefault()) && !helpRequested)
				engine.logger.warning("model type unspecified - use default type " + type.getKey() + ".");
		}
		engine.loadModel(type);
		if (engine.activeModel == null) {
			if (!helpRequested)
				engine.logger.severe("model type '" + type.getKey() + "' not supported!");
			return helpCLO;
		}
		return cloarray;
	}

	/**
	 * Process the {@code --verbose} option early so logging level is set for
	 * subsequent parsing.
	 * 
	 * @param cloarray array of command line arguments
	 * @return possibly modified cloarray
	 */
	private String[] handleVerboseOption(String[] cloarray) {
		String verboseName = engine.cloVerbose.getName();
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (param.startsWith(verboseName)) {
				String verbosity = CLOption.stripKey(verboseName, param).trim();
				if (verbosity.isEmpty()) {
					engine.logger.warning("verbose level missing - ignored.");
					return ArrayMath.drop(cloarray, i);
				}
				engine.cloVerbose.setArg(verbosity);
				engine.cloVerbose.parse();
				break;
			}
		}
		return cloarray;
	}

	/**
	 * Parse command line options.
	 *
	 * @return the number of issues that have occurred during parsing
	 */
	public int parseCLO() {
		return parseCLO(getSplitCLO());
	}

	/**
	 * Pre-process and parse array of command line arguments.
	 *
	 * @param cloarray string array of command line arguments
	 * @return the number of issues that have occurred during parsing
	 */
	protected int parseCLO(String[] cloarray) {
		parser.setLogger(engine.logger);
		cloarray = preprocessCLO(cloarray);
		parser.initCLO();
		if (engine.activeModule != null)
			cloarray = ArrayMath.append(cloarray, engine.cloModule.getName() + " " + engine.activeModule.getKey());
		if (engine.activeModel != null)
			cloarray = ArrayMath.append(cloarray,
					engine.cloModel.getName() + " " + engine.activeModel.getType().getKey());
		int issues = parser.parseCLO(cloarray);
		if (reparseCLO) {
			reparseCLO = false;
			parser.initCLO();
			return parser.parseCLO(cloarray);
		}
		return issues;
	}

	/**
	 * Format, encode and output help on command line options.
	 * 
	 * @return the help string
	 */
	public String getCLOHelp() {
		String globalMsg = "List of command line options";
		if (engine.activeModule != null) {
			globalMsg += " for module '" + engine.activeModule.getKey() + "'";
			if (engine.activeModel != null) {
				globalMsg += " and model '" + engine.activeModel.getType().getKey() + "'";
				CLOCategory.Model.setHeader("Options for model '" + engine.activeModel.getType().getKey() + "'");
			} else
				globalMsg += " (select model for more options)";
			globalMsg += "\n\nGlobal options:";

			int idx = 0;
			StringBuilder sb = new StringBuilder();
			for (Module<?> mod : engine.activeModule.getSpecies()) {
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
					if (mod instanceof org.evoludo.simulator.models.Model.HasDE
							&& ((org.evoludo.simulator.models.Model.HasDE) mod).getDependent() == n) {
						sb.append(" (dependent)");
					}
				}
				idx += nt;
			}
			String moduleMsg = sb.toString();
			CLOCategory.Module.setHeader("Options for module '" + engine.activeModule.getKey() //
					+ "' with trait indices and names:" + moduleMsg);
		} else
			globalMsg += " (select module and model for more info):";
		CLOCategory.Global.setHeader(globalMsg);
		return parser.helpCLO(true);
	}

	/**
	 * Collect command line options from the engine and active module/model.
	 *
	 * @param parser the shared CLO parser
	 */
	void collectCLO(CLOParser parser) {
		parser.addCLO(engine.cloHelp);
		parser.addCLO(engine.cloVerbose);
		if (!EvoLudo.isGWT)
			engine.cloVerbose.setDefault("warning");
		parser.addCLO(engine.cloModule);
		parser.addCLO(engine.cloModel);
		parser.addCLO(engine.cloSeed);
		parser.addCLO(engine.cloRun);
		parser.addCLO(engine.cloDelay);
		parser.addCLO(engine.cloRNG);
		if (engine.activeModel instanceof org.evoludo.simulator.models.CModel //
				&& engine.activeModule.getNTraits() > 1 //
				&& (engine.activeModule instanceof org.evoludo.simulator.views.HasPop2D
						|| engine.activeModule instanceof org.evoludo.simulator.views.HasPop3D)) {
			parser.addCLO(engine.cloTraitColorScheme);
			engine.cloTraitColorScheme.addKeys(EvoLudo.ColorModelType.values());
		}
		if (engine.activeModule instanceof org.evoludo.simulator.views.HasS3
				|| engine.activeModule instanceof org.evoludo.simulator.views.HasPhase2D)
			parser.addCLO(engine.cloTrajectoryColor);
	}

	CLOParser getParser() {
		return parser;
	}

	String getParserCLO() {
		if (parser != null)
			return parser.getCLO();
		return clo;
	}

	boolean providesCLO(String name) {
		return parser != null && parser.providesCLO(name);
	}
}
