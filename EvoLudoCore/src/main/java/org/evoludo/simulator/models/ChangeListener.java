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

package org.evoludo.simulator.models;

/**
 * All <code>ChangeListener</code>'s get informed about state changes of the
 * core of EvoLudo models.
 *
 * @author Christoph Hauert
 */
public interface ChangeListener {

	/**
	 * The GUI or engine can request running models to suspend execution and process
	 * a <code>pendingAction</code>. Valid requests are:
	 * <ul>
	 * <li><code>NONE</code>: No action requested, continue.</li>
	 * <li><code>APPLY</code>: Command line options may have changed and should be
	 * applied to EvoLudo model. Running models resume execution if no reset was
	 * required.</li>
	 * <li><code>INIT</code> Initialize model (re-initialize strategies, stop
	 * execution).</li>
	 * <li><code>RESET</code>: Reset model (re-initialize geometry and strategies,
	 * stop execution).</li>
	 * <li><code>UNLOAD</code>: Unload model (stop execution).</li>
	 * <li><code>STOP</code>: Stop execution.</li>
	 * <li><code>STATISTIC</code>: Statistic is ready. Make sure to resume
	 * calculations.</li>
	 * <li><code>SNAPSHOT</code>: Produce snapshot of current configuration (may not
	 * always be available, type of snapshot (graphical, statistics, or state) not
	 * defined).</li>
	 * </ul>
	 */
	public enum PendingAction {
		/**
		 * No action requested, continue.
		 */
		NONE,

		/**
		 * GWT application unloaded (stop execution, unload model).
		 */
		UNLOAD,

		/**
		 * Initialize model (re-initialize strategies, stop execution).
		 */
		INIT,

		/**
		 * Reset model (re-initialize geometry and strategies, stop execution).
		 */
		RESET,

		/**
		 * Stop execution.
		 */
		STOP,

		/**
		 * Start execution.
		 */
		START,

		/**
		 * Command line options may have changed and should be applied to EvoLudo model.
		 * Running models resume execution if no reset was required.
		 */
		APPLY,

		/**
		 * Re-parse command line options. This is necessary e.g. if nTraits has changed.
		 */
		CLO,

		/**
		 * Change execution mode of model.
		 */
		MODE,

		/**
		 * Statistic is ready. Make sure to resume calculations.
		 */
		STATISTIC,

		/**
		 * Produce snapshot of current configuration (may not always be available, type
		 * of snapshot (graphical, statistics, or state) not defined).
		 */
		SNAPSHOT;

		/**
		 * The pending execution mode of the model.
		 */
		public Mode mode;
	}

	/**
	 * Called whenever the state of the EvoLudo model changed. Process potentially
	 * pending requests.
	 * <p>
	 * <strong>Note:</strong> the model may process some pending actions directly
	 * and without notifying the listeners through
	 * <code>modelChanged(PendingAction)</code> first. In particular, this applies
	 * to pending actions that fire their own notifications, such as
	 * <code>RESET</code> and <code>INIT</code> that in turn trigger
	 * <code>modelReset()</code> and <code>modelInit()</code>, respectively.
	 *
	 * @param action pending action that needs to be processed.
	 * @see ChangeListener.PendingAction PendingAction
	 */
	public void modelChanged(PendingAction action);
}