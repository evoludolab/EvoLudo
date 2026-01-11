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

package org.evoludo.simulator.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.LinkedHashMap;

import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.Console;
import org.evoludo.simulator.views.Distribution;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.simulator.views.Histogram;
import org.evoludo.simulator.views.Mean;
import org.evoludo.simulator.views.Phase2D;
import org.evoludo.simulator.views.Pop2D;
import org.evoludo.simulator.views.Pop3D;
import org.evoludo.simulator.views.S3;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.NativeJS;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Controller that manages the set of {@link AbstractView} instances displayed
 * in the EvoLudo web client. Responsibilities include creating and recycling
 * views, keeping the deck and selector widgets in sync, processing the
 * <code>--view</code> CLO, and switching between views while ensuring proper
 * activation/deactivation.
 */
@SuppressWarnings("java:S1452") // views have unknown generic parameters
public class ViewController {

	/**
	 * EvoLudo engine powering the views.
	 */
	private final EvoLudoGWT engine;

	/**
	 * Deck widget hosting the views.
	 */
	private final DeckLayoutPanel deck;

	/**
	 * List box controlling the active view selection.
	 */
	private final ListBox selector;

	/**
	 * Console view that is always present.
	 */
	private final Console console;

	/**
	 * Callback invoked whenever the view changes.
	 */
	private final Runnable onViewChanged;

	/**
	 * Registry of active views keyed by name.
	 */
	private Map<String, AbstractView<?>> activeViews = new LinkedHashMap<>();

	/**
	 * Currently active view.
	 */
	private AbstractView<?> activeView;

	/**
	 * Track the module last used for view reuse decisions.
	 */
	private Module<?> lastModule;

	/**
	 * Initial view specification (index or name with optional args).
	 */
	private String initialView = "1";

	/**
	 * CLO option controlling the initial view selection.
	 */
	private final CLOption cloView = new CLOption("view", "1", CLOCategory.GUI, null, new CLODelegate() {
		@Override
		public boolean parse(String arg) {
			initialView = arg;
			return true;
		}

		@Override
		public String getDescription() {
			StringBuilder descr = new StringBuilder("--view <v>      select view (v: index or title)");
			int idx = 1;
			for (AbstractView<?> view : activeViews.values()) {
				String keycode = "              " + (idx++) + ": ";
				int len = keycode.length();
				descr.append("\n").append(keycode.substring(len - 16, len)).append(view.getName());
			}
			return descr.toString();
		}
	});

	/**
	 * Create a controller instance.
	 * 
	 * @param engine        EvoLudo engine powering the views
	 * @param deck          deck widget hosting the views
	 * @param selector      list box changing the active view
	 * @param console       console view (always included)
	 * @param onViewChanged callback invoked whenever a view change completes
	 */
	public ViewController(EvoLudoGWT engine, DeckLayoutPanel deck, ListBox selector, Console console,
			Runnable onViewChanged) {
		this.engine = engine;
		this.deck = deck;
		this.selector = selector;
		this.console = console;
		this.onViewChanged = onViewChanged;
		this.activeView = console;
	}

	/**
	 * Returns the CLO option controlling the initial view displayed in the GUI.
	 *
	 * @return command-line option that selects the view
	 */
	public CLOption getCloView() {
		return cloView;
	}

	/**
	 * Determines whether the {@code --view} option has been provided.
	 *
	 * @return {@code true} if the option is set, {@code false} otherwise
	 */
	public boolean isInitialViewSet() {
		return cloView.isSet();
	}

	/**
	 * Retrieves the currently active view.
	 *
	 * @return active view instance, or {@code null} if none selected
	 */
	public AbstractView<?> getActiveView() {
		return activeView;
	}

	/**
	 * Provides access to the console view.
	 *
	 * @return console view instance
	 */
	public AbstractView<?> getConsoleView() {
		return console;
	}

	/**
	 * Reports the index of the active view inside the deck.
	 *
	 * @return zero-based deck index or {@code -1} if no view is active
	 */
	public int getActiveViewIndex() {
		if (activeView == null)
			return -1;
		return deck.getWidgetIndex(activeView);
	}

	/**
	 * Provides a snapshot of all active views, including the console.
	 *
	 * @return list of active views
	 */
	public List<AbstractView<?>> getActiveViews() {
		return new ArrayList<>(activeViews.values());
	}

	/**
	 * Checks whether the supplied view is managed by this controller.
	 *
	 * @param view view instance to verify
	 * @return {@code true} if the view is present, {@code false} otherwise
	 */
	public boolean containsView(AbstractView<?> view) {
		return activeViews.containsValue(view);
	}

	/**
	 * Returns the first available view in the registry.
	 *
	 * @return first view or {@code null} if none exist
	 */
	public AbstractView<?> getFirstView() {
		if (activeViews.isEmpty())
			return null;
		return activeViews.values().iterator().next();
	}

	/**
	 * Finds a view by name.
	 *
	 * @param name view name
	 * @return matching view or {@code null} if absent
	 */
	@SuppressWarnings("java:S1452") // views have unknown generic parameters
	public AbstractView<?> getViewByName(String name) {
		return activeViews.get(name);
	}

	/**
	 * Resets the selection state when modules/models unload.
	 */
	public void resetSelection() {
		activeView = null;
		selector.setSelectedIndex(-1);
	}

	/**
	 * Ensure the console is present while applying CLO changes and temporarily show
	 * it while parsing options.
	 */
	public void prepareForCLO() {
		if (activeView == null) {
			console.load();
			if (deck.getWidgetCount() == 0)
				deck.add(console);
			activeView = console;
		} else if (activeView == console) {
			deck.remove(console);
			deck.add(console);
		}
		deck.showWidget(console);
	}

	/**
	 * Updates the list of available views based on the current module and model.
	 */
	public void refreshViews() {
		HashMap<String, AbstractView<?>> oldViews = new HashMap<>(activeViews);
		activeViews = new LinkedHashMap<>();

		Module<?> module = engine.getModule();
		if (module == null) {
			addView(console, oldViews);
			updateSelector();
			lastModule = null;
			return;
		}

		Model model = engine.getModel();
		if (module != lastModule)
			unloadViews(oldViews);
		ModelType mt = model.getType();
		boolean isODESDE = mt.isODE() || mt.isSDE();

		addStrategyViews(module, isODESDE, oldViews);
		addFitnessViews(module, isODESDE, oldViews);
		addStructureViews(module, isODESDE, oldViews);
		addStatisticsViews(module, model, oldViews);

		addView(console, oldViews);

		if (!oldViews.isEmpty())
			unloadViews(oldViews);
		updateSelector();
		lastModule = module;
	}

	/**
	 * Unload and remove all views except the console.
	 */
	private void unloadViews(HashMap<String, AbstractView<?>> oldViews) {
		for (AbstractView<?> view : oldViews.values()) {
			if (view == console)
				continue;
			view.unload();
			deck.remove(view);
		}
		oldViews.clear();
	}

	/**
	 * Change the active view by name.
	 *
	 * @param name the view name
	 * @return {@code true} if the view actually changed
	 */
	public boolean changeViewByName(String name) {
		return changeViewTo(activeViews.get(name), false);
	}

	/**
	 * Change the currently active view.
	 *
	 * @param newView the new view
	 * @return {@code true} when a change occurred
	 */
	public boolean changeViewTo(AbstractView<?> newView) {
		return changeViewTo(newView, false);
	}

	/**
	 * Change the currently active view.
	 *
	 * @param newView the new view
	 * @param force   force activation even when unchanged
	 * @return {@code true} when a change occurred
	 */
	public boolean changeViewTo(AbstractView<?> newView, boolean force) {
		if (newView == null)
			return false;
		if (!force && newView == activeView)
			return false;

		if (activeView != null)
			activeView.deactivate();
		activeView = newView;
		deck.showWidget(activeView);
		int activeIdx = deck.getWidgetIndex(activeView);
		selector.setSelectedIndex(activeIdx);
		Scheduler.get().scheduleDeferred(() -> activeView.activate());
		if (onViewChanged != null)
			onViewChanged.run();
		return true;
	}

	/**
	 * Parse the initial view specification (if provided) and resolve the matching
	 * view.
	 *
	 * @param logger logger for diagnostics (optional)
	 * @return the initial view or {@code null} if the {@code --view} option was not
	 *         provided
	 * 
	 * @see #cloView
	 */
	public AbstractView<?> resolveInitialView(Logger logger) {
		if (!cloView.isSet())
			return null;

		List<AbstractView<?>> availableViews = new ArrayList<>(activeViews.values());
		if (availableViews.isEmpty())
			return null;
		String[] iv = initialView.split(" ", 2);
		String name = iv[0].replace('_', ' ').trim();
		AbstractView<?> newView = null;
		for (AbstractView<?> view : availableViews) {
			if (view.getName().equals(name)) {
				newView = view;
				break;
			}
		}
		if (newView == null) {
			int idx = 0;
			try {
				idx = CLOParser.parseInteger(iv[0]);
			} catch (NumberFormatException e) {
				if (logger != null)
					logger.warning("failed to set view '" + iv[0] + "' - using default.");
			}
			idx = Math.min(Math.max(idx, 1), availableViews.size());
			newView = availableViews.get(idx - 1);
		}
		newView.setOptions(iv.length > 1 ? iv[1].trim() : null);
		return newView;
	}

	/**
	 * Adds strategy-related views according to module capabilities.
	 *
	 * @param module   active module
	 * @param isODESDE {@code true} if current model is an ODE/SDE
	 * @param oldViews map holding recycled views
	 */
	private void addStrategyViews(Module<?> module, boolean isODESDE, Map<String, AbstractView<?>> oldViews) {
		if (module instanceof HasPop2D.Traits && !isODESDE)
			addView(new Pop2D(engine, Data.TRAIT), oldViews);
		if (NativeJS.isWebGLSupported() && module instanceof HasPop3D.Traits && !isODESDE)
			addView(new Pop3D(engine, Data.TRAIT), oldViews);
		if (module instanceof HasPhase2D)
			addView(new Phase2D(engine), oldViews);
		if (module instanceof HasMean.Traits)
			addView(new Mean(engine, Data.TRAIT), oldViews);
		if (module instanceof HasS3)
			addView(new S3(engine), oldViews);
		if (module instanceof HasHistogram.Strategy)
			addView(new Histogram(engine, Data.TRAIT), oldViews);
		if (module instanceof HasDistribution.Strategy)
			addView(new Distribution(engine, Data.TRAIT), oldViews);
	}

	/**
	 * Adds fitness-related views based on module capabilities.
	 *
	 * @param module   active module
	 * @param isODESDE {@code true} if current model is an ODE/SDE
	 * @param oldViews map holding recycled views
	 */
	private void addFitnessViews(Module<?> module, boolean isODESDE, Map<String, AbstractView<?>> oldViews) {
		if (module instanceof HasPop2D.Fitness && !isODESDE)
			addView(new Pop2D(engine, Data.FITNESS), oldViews);
		if (NativeJS.isWebGLSupported() && module instanceof HasPop3D.Fitness && !isODESDE)
			addView(new Pop3D(engine, Data.FITNESS), oldViews);
		if (module instanceof HasMean.Fitness)
			addView(new Mean(engine, Data.FITNESS), oldViews);
		if (module instanceof HasHistogram.Fitness)
			addView(new Histogram(engine, Data.FITNESS), oldViews);
	}

	/**
	 * Adds structure-related views when available.
	 *
	 * @param module   active module
	 * @param isODESDE {@code true} if current model is an ODE/SDE
	 * @param oldViews map holding recycled views
	 */
	private void addStructureViews(Module<?> module, boolean isODESDE, Map<String, AbstractView<?>> oldViews) {
		if (module instanceof HasHistogram.Degree && !isODESDE)
			addView(new Histogram(engine, Data.DEGREE), oldViews);
	}

	/**
	 * Adds statistics-related views based on model permissions.
	 *
	 * @param module   active module
	 * @param model    active model
	 * @param oldViews map holding recycled views
	 */
	private void addStatisticsViews(Module<?> module, Model model, Map<String, AbstractView<?>> oldViews) {
		if (model != null && model.permitsMode(Mode.STATISTICS_SAMPLE)) {
			if (module instanceof HasHistogram.StatisticsProbability)
				addView(new Histogram(engine, Data.STATISTICS_FIXATION_PROBABILITY), oldViews);
			if (module instanceof HasHistogram.StatisticsTime)
				addView(new Histogram(engine, Data.STATISTICS_FIXATION_TIME), oldViews);
		}
		if (model != null && model.permitsMode(Mode.STATISTICS_UPDATE)
				&& module instanceof HasHistogram.StatisticsStationary) {
			addView(new Histogram(engine, Data.STATISTICS_STATIONARY), oldViews);
		}
	}

	/**
	 * Registers a view and inserts it into the deck, reusing an existing instance
	 * if available.
	 *
	 * @param view     view to add
	 * @param oldViews map that may contain reusable instances
	 */
	private void addView(AbstractView<?> view, Map<String, AbstractView<?>> oldViews) {
		String name = view.getName();
		if (oldViews.containsKey(name))
			view = oldViews.remove(name);
		activeViews.put(view.getName(), view);
		for (Widget widget : deck)
			if (widget == view)
				return;
		int consoleIdx = deck.getWidgetIndex(console);
		if (consoleIdx < 0)
			deck.add(view);
		else
			deck.insert(view, consoleIdx);
	}

	/**
	 * Rebuilds the selector entries so they match the deck layout.
	 */
	private void updateSelector() {
		selector.clear();
		for (Widget view : deck)
			selector.addItem(((AbstractView<?>) view).getName());
	}
}
