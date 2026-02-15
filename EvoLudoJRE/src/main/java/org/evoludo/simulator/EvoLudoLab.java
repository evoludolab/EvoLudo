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

package org.evoludo.simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.ToggleAntiAliasingAction;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.LifecycleListener;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.RunListener;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.simulator.views.MVC2Distr;
import org.evoludo.simulator.views.MVCDistr;
import org.evoludo.simulator.views.MVCMean;
import org.evoludo.simulator.views.MVCTraitHistogram;
import org.evoludo.simulator.views.MVConsole;
import org.evoludo.simulator.views.MVDMean;
import org.evoludo.simulator.views.MVDPhase2D;
import org.evoludo.simulator.views.MVDS3;
import org.evoludo.simulator.views.MVDegree;
import org.evoludo.simulator.views.MVFitHistogram;
import org.evoludo.simulator.views.MVFitness;
import org.evoludo.simulator.views.MVPop2D;
import org.evoludo.simulator.views.MultiView;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.Formatter;

public class EvoLudoLab extends JFrame
		implements LifecycleListener, RunListener, ChangeListener, CLOProvider {

	private static final long serialVersionUID = 20110423L;

	boolean paramsVisible = false;
	ParamPanel params;
	JSlider evoludoSlider;
	JButton runStop;
	JButton resetButton;
	private final EvoLudoPanel activeViews;
	JLabel status;
	JLabel counter;
	private final Action resetAction;
	private final transient Action runStopAction;
	private final Action stepAction;

	protected EvoLudoJRE engine;
	protected Font tickFont;
	protected Font labelFont;
	protected double lineStroke = GraphStyle.LINE_STROKE;
	protected double frameStroke = GraphStyle.FRAME_STROKE;
	protected Color bgcolorGUI = new Color(0.8f, 0.8f, 0.8f);

	protected static final String CONTROL_PARAM = "param";

	public EvoLudoLab(EvoLudoJRE engine) {
		// instantiate engine
		this.engine = engine;
		logger = engine.getLogger();
		// allocate console early to catch all log messages
		console = new MVConsole(this);
		logger.addHandler(new Handler() {
			@Override
			public void publish(LogRecord record) {
				console.log(record.getLevel(), record.getMessage());
			}

			@Override
			public void flush() {
				// ignore - nothing to flush
			}

			@Override
			public void close() throws SecurityException {
				// ignore - nothing to close
			}
		});
		engine.addCLOProvider(this);
		engine.addLifecycleListener(this);
		engine.addRunListener(this);
		engine.addChangeListener(this);

		BorderLayout layout = new BorderLayout();
		setLayout(layout);

		counter = new JLabel("-", SwingConstants.LEFT);
		counter.setFont(new Font("Default", Font.BOLD, 11));
		counter.setToolTipText("Number of generations elapsed");
		counter.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		status = new JLabel("-", SwingConstants.CENTER);
		status.setFont(new Font("Default", Font.PLAIN, 10));
		status.setToolTipText("Summary of current status");
		status.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

		activeViews = new EvoLudoPanel();
		JPanel panelA = new JPanel(new GridLayout(1, 2));
		panelA.setOpaque(false);
		panelA.add(counter);
		panelA.add(activeViews.getChoice());
		add(panelA, BorderLayout.NORTH);

		add(activeViews, BorderLayout.CENTER);
		panelA = new JPanel(new GridLayout(3, 1, 2, 2));
		panelA.setOpaque(false);
		evoludoSlider = new JSlider(SwingConstants.HORIZONTAL, (int) EvoLudo.DELAY_MIN, (int) EvoLudo.DELAY_MAX,
				(int) (EvoLudo.DELAY_MAX - EvoLudo.DELAY_MIN) / 2);
		evoludoSlider.setInverted(true);
		evoludoSlider.setToolTipText("Sets delay between updates (slow - left, fast - right)");
		panelA.add(evoludoSlider);
		JPanel panelB = new JPanel();
		panelB.setOpaque(false);
		panelB.setLayout(new GridLayout(1, 4));
		JButton button = new JButton("Settings");
		button.addActionListener(e -> {
			if (params == null)
				params = new ParamPanel(EvoLudoLab.this);
			params.setVisible(true);
		});
		button.setToolTipText("Change simulation parameters");
		panelB.add(button);
		resetAction = new ResetAction();
		resetButton = new JButton(resetAction);
		panelB.add(resetButton);
		runStopAction = new RunStopAction();
		runStop = new JButton(runStopAction);
		panelB.add(runStop);
		stepAction = new StepAction();
		button = new JButton(stepAction);
		panelB.add(button);
		panelA.add(panelB);
		panelA.add(status);
		add(panelA, BorderLayout.SOUTH);

		// add keyboard shortcuts - adjust control key labels if alt-modifier is pressed
		JRootPane root = getRootPane();
		addKeyControls(root);
		new ContainerHandler().addKeyListenerRecursively(root);
	}

	public EvoLudoJRE getEngine() {
		return engine;
	}

	/**
	 * simulate global key listener:
	 * http://www.javaworld.com/javaworld/javatips/jw-javatip69.html
	 * implement ContainerListener
	 */
	public class ContainerHandler implements ContainerListener {
		KeyHandler keyHandler;

		public ContainerHandler() {
			keyHandler = new KeyHandler();
		}

		@Override
		public void componentAdded(ContainerEvent e) {
			addKeyListenerRecursively(e.getChild());
		}

		@Override
		public void componentRemoved(ContainerEvent e) {
			removeKeyListenerRecursively(e.getChild());
		}

		public void addKeyListenerRecursively(Component c) {
			c.addKeyListener(keyHandler);
			if (c instanceof Container) {
				Container cont = (Container) c;
				cont.addContainerListener(this);
				Component[] children = cont.getComponents();
				for (Component child : children) {
					addKeyListenerRecursively(child);
				}
			}
		}

		public void removeKeyListenerRecursively(Component c) {
			c.removeKeyListener(keyHandler);
			if (c instanceof Container) {
				Container cont = (Container) c;
				cont.addContainerListener(this);
				Component[] children = cont.getComponents();
				for (Component child : children) {
					removeKeyListenerRecursively(child);
				}
			}
		}
	}

	/**
	 * implement KeyListener
	 */
	public class KeyHandler implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ALT) {
				((ResetAction) resetAction).update(true);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ALT) {
				((ResetAction) resetAction).update(false);
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

	public class ResetAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public ResetAction() {
			putValue(Action.NAME, "Init");
			putValue(Action.SHORT_DESCRIPTION, "Initialize population (press Alt to re-initialize structure)");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_BACK_SPACE);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if ((e.getModifiers() & ActionEvent.ALT_MASK) == 0)
				engine.requestAction(PendingAction.INIT);
			else
				engine.requestAction(PendingAction.RESET);
		}

		public void update(boolean altPressed) {
			putValue(Action.NAME, altPressed ? "Reset" : "Init");
		}
	}

	public class RunStopAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public RunStopAction() {
			super("Start");
			putValue(Action.SHORT_DESCRIPTION, "Start/stop simulations");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_SPACE);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (engine.isRunning()) {
				engine.stop();
				putValue(Action.NAME, "Stop");
			} else {
				engine.setSuspended(true);
				engine.run();
				putValue(Action.NAME, "Start");
			}
		}
	}

	public class StepAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public StepAction() {
			super("Step");
			putValue(Action.SHORT_DESCRIPTION, "Advance single simulation step");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_RIGHT);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			engine.next();
		}
	}

	public class ChangeDelayAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		private final boolean isIncrease;

		public ChangeDelayAction(double factor) {
			isIncrease = factor > 1.0;
			putValue(Action.NAME, isIncrease ? "Slower" : "Faster");
			putValue(Action.ACTION_COMMAND_KEY, isIncrease ? "Slower" : "Faster");
			putValue(Action.SHORT_DESCRIPTION, isIncrease ? "Reduce simulation speed" : "Increase simulation speed");
			putValue(Action.MNEMONIC_KEY,
					isIncrease ? Integer.valueOf(KeyEvent.VK_MINUS) : Integer.valueOf(KeyEvent.VK_PLUS));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (isIncrease)
				engine.increaseDelay();
			else
				engine.decreaseDelay();
			evoludoSlider.setValue(log2lin(engine.getDelay()));
		}
	}

	/**
	 * Helper function to convert from linear to logarithmic scales.
	 * 
	 * @param x linear value
	 * @return logarithmic value
	 */
	private int lin2log(double x) {
		double logRange = (Math.log(EvoLudo.DELAY_MAX) - Math.log(EvoLudo.DELAY_MIN));
		return (int) (Math.exp((x - EvoLudo.DELAY_MIN) / (EvoLudo.DELAY_MAX - EvoLudo.DELAY_MIN) * logRange) + 0.5);
	}

	/**
	 * Helper function to convert from logarithmic to linear scales.
	 * 
	 * @param x logarithmic value
	 * @return linear value
	 */
	private int log2lin(double x) {
		double logRange = (Math.log(EvoLudo.DELAY_MAX) - Math.log(EvoLudo.DELAY_MIN));
		return (int) (Math.log(x) / logRange * (EvoLudo.DELAY_MAX - EvoLudo.DELAY_MIN) + EvoLudo.DELAY_MIN + 0.5);
	}

	public class ChangeViewAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		int increment;

		public ChangeViewAction(int increment) {
			this.increment = increment;
			putValue(Action.NAME, "Change View");
			putValue(Action.ACTION_COMMAND_KEY, "Change View");
			putValue(Action.SHORT_DESCRIPTION, "Cycles through data views");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			activeViews.changeView(increment);
		}
	}

	/**
	 * Keyboard shortcuts
	 *
	 * Esc: exit full screen mode
	 * f: toggle full screen mode
	 * Space, Return: start/stop simulations
	 * right arrow: single simulation step
	 * +, -: increase/decrease speed of simulation
	 * backspace: init
	 * alt backspace: reset
	 * p: parameter panel
	 * tab/alt tab: next/previous data view
	 * 
	 * @param pane the root pane to add the key controls to
	 */
	private void addKeyControls(JRootPane pane) {
		final Integer DISPLAY_TOGGLE_ANTIALIASING = 9;
		final Integer CONTROL_RESET = 4;
		final Integer CONTROL_DELAY_INCREASE = 5;
		final Integer CONTROL_DELAY_DECREASE = 6;
		final Integer CONTROL_RUNSTOP = 2;
		final Integer CONTROL_STEP = 3;
		final Integer CONTROL_NEXT_VIEW = 7;
		final Integer CONTROL_PREV_VIEW = 8;

		InputMap windowInput = pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		// InputMap windowInput =
		// pane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap windowAction = pane.getActionMap();

		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.ALT_DOWN_MASK), CONTROL_RESET);
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), CONTROL_RESET);
		windowAction.put(CONTROL_RESET, resetAction);
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), CONTROL_RUNSTOP);
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), CONTROL_RUNSTOP);
		windowAction.put(CONTROL_RUNSTOP, runStopAction);
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), CONTROL_STEP);
		windowAction.put(CONTROL_STEP, stepAction);
		// 107 is the keycode for '+' on keypad
		// VK_EQUALS also applies to '=' on keypad...
		windowInput.put(KeyStroke.getKeyStroke(107, 0), CONTROL_DELAY_DECREASE);
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), CONTROL_DELAY_DECREASE);
		windowAction.put(CONTROL_DELAY_DECREASE, new ChangeDelayAction(1.0 / 1.2));
		// 109 is the keycode for '-' on keypad
		windowInput.put(KeyStroke.getKeyStroke(109, 0), CONTROL_DELAY_INCREASE);
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), CONTROL_DELAY_INCREASE);
		windowAction.put(CONTROL_DELAY_INCREASE, new ChangeDelayAction(1.2));
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), CONTROL_NEXT_VIEW);
		windowAction.put(CONTROL_NEXT_VIEW, new ChangeViewAction(1));
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), CONTROL_PREV_VIEW);
		windowAction.put(CONTROL_PREV_VIEW, new ChangeViewAction(-1));
		// anti-aliasing
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), DISPLAY_TOGGLE_ANTIALIASING);
		windowAction.put(DISPLAY_TOGGLE_ANTIALIASING, ToggleAntiAliasingAction.sharedInstance());
	}

	/**
	 * application: this init() is called from exec() passing the command line
	 * options
	 */
	protected synchronized void init() {
		evoludoSlider.addChangeListener(e -> engine.setDelay(lin2log(evoludoSlider.getValue())));
	}

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;
	MVConsole console;

	/**
	 * Process and apply the command line arguments <code>clo</code>. Loads new
	 * model (and unloads old one), if necessary, and loads/adjusts the data views
	 * as appropriate.
	 */
	public void applyCLO() {
		if (engine.isRunning()) {
			logger.warning("Cannot apply parameters while engine is running.");
			return;
		}
		int idx = activeViews.getActiveIndex();
		if (idx < 0 || cloView.isSet())
			idx = Integer.parseInt(appView);
		// parse command line options
		boolean resume = engine.isSuspended();
		engine.setSuspended(false);
		Model oldModel = engine.getModel();
		Module<?> oldModule = engine.getModule();
		int parsingIssues = engine.parseCLO();
		Module<?> module = engine.getModule();
		setTitle(module.getTitle());
		// reset is required if module and/or model changed
		if (module != oldModule || engine.getModel() != oldModel) {
			engine.modelReset();
		} else {
			if (engine.paramsDidChange()) {
				// show version information in status line (set level to replace info messages
				// but not warnings and errors)
				displayStatus(engine.getVersion(), Level.INFO.intValue() + 1);
			} else {
				// resume running if no reset was necessary
				engine.setSuspended(resume);
				// even without reset necessary data views should be adjusted to:
				// - reflect changes in report frequency (time line graphs, distributions and
				// linear geometries)
				// - changes in payoffs require rescaling of color maps
				updateViews();
			}
		}
		activeViews.setView(idx);
		if (parsingIssues > 0)
			displayStatus("Multiple parsing problems (" + parsingIssues + ") - check log for details.",
					Level.WARNING.intValue() + 1);
	}

	public void addMultiView(MultiView view) {
		activeViews.addView(view);
	}

	public List<MultiView> getMultiViews() {
		return activeViews.getViews();
	}

	protected void updateViews() {
		String myView = activeViews.getView();
		activeViews.clear();
		// model is loaded, now get GUI ready.
		// strategies related views
		Module<?> module = engine.getModule();
		Model model = engine.getModel();
		if (module instanceof HasPop2D.Traits)
			if (model.isContinuous())
				addMultiView(new MVPop2D(this, MVPop2D.Data.CSTRAT));
			else
				addMultiView(new MVPop2D(this, MVPop2D.Data.DSTRAT));
		if (module instanceof HasMean.Traits) {
			if (model.isContinuous())
				addMultiView(new MVCMean(this));
			else
				addMultiView(new MVDMean(this));
		}
		if (module instanceof HasPhase2D)
			addMultiView(new MVDPhase2D(this));
		if (module instanceof HasS3)
			addMultiView(new MVDS3(this));
		if (module instanceof HasHistogram.Strategy)
			addMultiView(new MVCTraitHistogram(this));
		if (module instanceof HasDistribution.Strategy) {
			addMultiView(new MVCDistr(this));
			if (module.getNTraits() == 2)
				addMultiView(new MVC2Distr(this));
		}
		// fitness related views
		if (module instanceof HasPop2D.Fitness)
			addMultiView(new MVPop2D(this, MVPop2D.Data.FITNESS));
		if (module instanceof HasMean.Fitness)
			addMultiView(new MVFitness(this));
		if (module instanceof HasHistogram.Fitness)
			addMultiView(new MVFitHistogram(this));
		// structure related views
		if (module instanceof HasHistogram.Degree)
			addMultiView(new MVDegree(this));
		// statistics related views
		// if( module instanceof HasHistogram.StatisticsProbability )
		// addMultiView(viewStatFixP);
		// if( module instanceof HasHistogram.StatisticsTime )
		// addMultiView(viewStatFixT);
		// miscellaneous views
		addMultiView(console);
		for (MultiView mvp : activeViews.getViews()) {
			mvp.setModule(module);
			mvp.reset(true);
		}
		if (myView != null)
			activeViews.setView(myView);
		getContentPane().setBackground(bgcolorGUI);
		runStopAction.putValue(Action.NAME, engine.isRunning() ? "Stop" : "Start");
		evoludoSlider.setValue(log2lin(engine.getDelay()));
	}

	/**
	 * applet: called by the browser or applet viewer to inform this applet to
	 * start its execution
	 */
	public void start() {
		runStop.requestFocus(); // focus on run/stop button
		if (params != null)
			params.setVisible(paramsVisible);
		if (engine.isSuspended()) {
			engine.run();
			runStopAction.putValue(Action.NAME, engine.isRunning() ? "Stop" : "Start");
		}
	}

	/**
	 * applet: called by the browser or applet viewer to inform this applet
	 * to stop its execution
	 */
	public void stop() {
		// should we rather unload the engine?
		engine.stop();
		if (params != null) {
			paramsVisible = params.isVisible();
			params.setVisible(false);
		}
	}

	private void updateLabels() {
		Model model = engine.getModel();
		counter.setText(model.getCounter());
		counter.repaint();
		displayStatus(model.getStatus());
	}

	public void displayStatus(String msg) {
		displayStatus(msg, Level.INFO.intValue());
	}

	public void displayStatus(String msg, int level) {
		status.setHorizontalAlignment(SwingConstants.LEFT);
		status.setText(msg);
		status.repaint();
	}

	/**
	 * In order to conserve computational resources the minimum time between
	 * subsequent GUI updates has to be at least
	 * <code>MIN_MSEC_BETWEEN_UPDATES</code> milliseconds. If update request are
	 * made more frequently some are request are not honoured and simply dropped.
	 */
	protected static final int MIN_MSEC_BETWEEN_UPDATES = 50; // max 20 updates per second

	/**
	 * time of last GUI update
	 */
	protected long updatetime = -1L;

	/**
	 * Update GUI. If time since last update is less than
	 * {@link #MIN_MSEC_BETWEEN_UPDATES} then views may choose to skip an update.
	 */
	public void update() {
		// limit the number of GUI updates per second to conserve resources or put them
		// to better use
		long now = System.currentTimeMillis();
		boolean updateGUI = !engine.isRunning() || (now - updatetime > MIN_MSEC_BETWEEN_UPDATES);
		update(updateGUI);
		if (updateGUI)
			updatetime = now;
	}

	/**
	 * Helper method to update GUI. If <code>updateGUI</code> is true GUI gets
	 * updated, if false, views may ignore the request. However, views that record
	 * the history need to process (but not necessarily visualize) the data
	 * regardless.
	 *
	 * @param updateGUI <code>true</code> to force GUI update
	 */
	private void update(boolean updateGUI) {
		for (MultiView mvp : activeViews.getViews())
			mvp.update(updateGUI);
		if (updateGUI) {
			updateLabels();
		}
	}

	@Override
	public void moduleUnloaded() {
		activeViews.clear();
	}

	@Override
	public void moduleRestored() {
		updateLabels();
	}

	@Override
	public synchronized void modelChanged(PendingAction action) { // Fix the method signature
		update();
	}

	@Override
	public synchronized void modelStopped() {
		for (MultiView mvp : activeViews.getViews())
			mvp.end();
		update(true);
		runStopAction.putValue(Action.NAME, "Start");
	}

	@Override
	public synchronized void modelDidInit() {
		for (MultiView mvp : activeViews.getViews())
			mvp.init();
		updateLabels();
	}

	@Override
	public synchronized void modelDidReset() {
		updateViews();
		updateLabels();
	}

	public boolean isRunning() {
		return engine.isRunning();
	}

	/*
	 * handle parameters
	 */
	protected int appWidth = 420;
	protected int appHeight = 570;
	protected String appView = "0"; // index or title
	protected boolean doAnimateLayout = false;

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloSize = new CLOption("size", "420x570", CLOCategory.GUI,
			"--size <w>x<h>  size of application window (w: width, h: height)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					int mark = arg.indexOf('x');
					if (mark < 0)
						return false;
					appWidth = Integer.parseInt(arg.substring(0, mark));
					appHeight = Integer.parseInt(arg.substring(mark + 1));
					return true;
				}
			});

	public final CLOption cloView = new CLOption("view", "0", CLOCategory.GUI,
			"--view <v>      active view upon launch (v: index or title)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					appView = arg;
					return true;
				}
			});

	public final CLOption cloAA = new CLOption("noAA", CLOCategory.GUI,
			"--noAA          disable anti-aliasing",
			new CLODelegate() {
				@Override
				public boolean parse(boolean isSet) {
					// by default do not interfere - i.e. leave AA as is
					if (!isSet)
						return true;
					ToggleAntiAliasingAction.sharedInstance().setAntiAliasing(true);
					return true;
				}
			});

	public final CLOption cloAnimate = new CLOption("animate", CLOCategory.GUI,
			"--animate       animate progress of laying out networks",
			new CLODelegate() {
				@Override
				public boolean parse(boolean isSet) {
					// by default do not interfere - i.e. leave animation setting as is
					if (!isSet)
						return true;
					doAnimateLayout = true;
					return true;
				}
			});

	public final CLOption cloTickFont = new CLOption("tickfont", "10", CLOCategory.GUI, // GraphStyle.FONT_TICK_SIZE
			"--tickfont [[<n>:]<t>:]<s>  font for tick labels (n: name, t: italic/bold/plain, s: size)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					tickFont = parseFont(arg, GraphStyle.defaultTickFont);
					return true;
				}
			});

	public final CLOption cloLabelFont = new CLOption("labelfont", "11", CLOCategory.GUI, // GraphStyle.FONT_LABEL_SIZE
			"--labelfont [[<n>:]<t>:]<s>  font for axis labels (n: name, t: italic/bold/plain, s: size)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					labelFont = parseFont(arg, GraphStyle.defaultLabelFont);
					return true;
				}
			});

	public final CLOption cloLineStroke = new CLOption("linestroke", "0.008", CLOCategory.GUI, // GraphStyle.LINE_STROKE
			"--linestroke <s>  stroke width of lines",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					lineStroke = Double.parseDouble(arg);
					return true;
				}
			});

	public final CLOption cloFrameStroke = new CLOption("framestroke", "1", CLOCategory.GUI, // GraphStyle.FRAME_STROKE
			"--framestroke <s>  stroke width of frames and ticks",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					frameStroke = Double.parseDouble(arg);
					return true;
				}
			});

	public final CLOption cloBGColorGUI = new CLOption("bgcolorGUI", "(200)", CLOCategory.GUI,
			"--bgcolorGUI <c>  set background color of GUI",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setBackgroundGUI(CLOParser.parseColor(arg));
					return true;
				}
			});

	public final CLOption cloGUI = new CLOption("gui", CLOCategory.GUI,
			"--gui           launch legacy java GUI",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// nothing to do - already handled in main()
					return true;
				}
			});

	/**
	 * do not use short options for GUI settings - they are reserved for engine
	 *
	 * @param parser command line option parser
	 */
	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloGUI);
		parser.addCLO(cloSize);
		parser.addCLO(cloView);
		parser.addCLO(cloAA);
		parser.addCLO(cloAnimate);
		parser.addCLO(cloTickFont);
		parser.addCLO(cloLabelFont);
		parser.addCLO(cloLineStroke);
		parser.addCLO(cloFrameStroke);
		parser.addCLO(cloBGColorGUI);
	}

	public Font parseFont(String arg, Font fallback) {
		String[] args = arg.split(":");
		String name = fallback.getFamily();
		int type = fallback.getStyle();
		int size;
		switch (args.length) {
			case 3:
				name = args[0];
				type = parseFontType(args[1]);
				size = Integer.parseInt(args[2]);
				break;
			case 2:
				type = parseFontType(args[0]);
				size = Integer.parseInt(args[1]);
				break;
			case 1:
				size = Integer.parseInt(args[0]);
				break;
			default:
				size = fallback.getSize();
		}
		return new Font(name, type, size);
	}

	private int parseFontType(String fonttype) {
		String type = fonttype.toLowerCase();
		if (type.contains("italic")) {
			if (type.contains("bold"))
				return Font.ITALIC | Font.BOLD;
			return Font.ITALIC;
		}
		if (type.contains("bold"))
			return Font.BOLD;
		return Font.PLAIN;
	}

	public boolean doAnimateLayout() {
		return doAnimateLayout;
	}

	public Font getLabelFont() {
		return labelFont;
	}

	public Font getTickFont() {
		return tickFont;
	}

	public double getLineStroke() {
		return lineStroke;
	}

	public double getFrameStroke() {
		return frameStroke;
	}

	public void setBackgroundGUI(Color bgcolorGUI) {
		this.bgcolorGUI = bgcolorGUI;
	}

	public Color getBackgroundGUI() {
		return bgcolorGUI;
	}

	public String getCLO() {
		return engine.getCLO();
	}

	/**
	 * Launch the java GUI. Note, exec() must be called on the EDT to take care of
	 * the initialization.
	 */
	public void exec() {
		int posX = 50;
		int posY = 10;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/*
		 * the java focus system is buggy but i guess this is apple's call...
		 * just hide the context menu when applet/application becomes deactivated
		 * although this is non-standard behavior it is otherwise just annoying...
		 */
		addWindowFocusListener(new WindowAdapter() {
			// note: maybe only dismiss menu on focus lost? - but then we loose control and
			// may end up with an orphaned menu...
			@Override
			public void windowLostFocus(WindowEvent e) {
				activeViews.setContextMenuEnabled(false);
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				activeViews.setContextMenuEnabled(true);
			}
		});
		setIconImage(new ImageIcon(EvoLudoLab.class.getResource("/images/DocIcon.png")).getImage());

		// set default values for all parameters and parse command line options
		init();
		applyCLO();
		pack();
		setBounds(posX, posY, appWidth, appHeight);
		setVisible(true);
		// start GUI manager in another thread to avoid blocking the EDT
		new Thread(this::start, "GUI Manager").start();
	}

	/**
	 * Generic entry point for java:
	 * <ol>
	 * <li>option {@code --gui} launches the legacy java GUI.
	 * <li>options {@code --data} or {@code --export} launch a generic simulation
	 * for the module specified by {@code --module}.
	 * <li>exits if none of {@code --gui}, {@code --data}, or {@code --export} are
	 * provided.
	 * </ol>
	 * In all cases the command line options are used to configure and initialize
	 * the desired simulation.
	 *
	 * @param args array of command line arguments
	 * @see EvoLudoJRE#simulation()
	 */
	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE();
		engine.setCLO(Formatter.format(args, " "));
		args = engine.getSplitCLO();
		boolean simOk = false;
		// first check if legacy java GUI requested
		for (String arg : args) {
			if (arg.startsWith("gui")) {
				engine.setHeadless(false);
				javax.swing.SwingUtilities.invokeLater(() -> new EvoLudoLab(engine).exec());
				return;
			}
			if (arg.startsWith("data") || arg.startsWith("export"))
				simOk = true;
		}
		if (!simOk)
			engine.fatal(
					"One of the command line options --gui, --data <types> or --export <file> is required.\n"
							+ "Try --help for more information.");
		engine.simulation();
		// should not get here...
		engine.fatal(
				"All warnings in the option string must first be resolved.\n"
						+ "Try --help for more information.");
	}
}
