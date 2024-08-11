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
 * All <code>MilestoneListener</code>'s get informed about milestones and state
 * changes of the core of EvoLudo modules. The implementation of all methods is
 * optional.
 *
 * @author Christoph Hauert
 */
public interface MilestoneListener {

	/**
	 * Called when EvoLudo module finished loading.
	 */
	public default void moduleLoaded() {
	}

	/**
	 * Called when EvoLudo module is unloading.
	 */
	public default void moduleUnloaded() {
	}

	/**
	 * Called when the state of the EvoLudo module has been restored.
	 */
	public default void moduleRestored() {
	}

	/**
	 * Called when EvoLudo model finished loading.
	 */
	public default void modelLoaded() {
	}

	/**
	 * Called when EvoLudo model is unloading.
	 */
	public default void modelUnloaded() {
	}

	/**
	 * Called when the EvoLudo model starts running.
	 */
	public default void modelRunning() {
	}

	/**
	 * Called after the EvoLudo model has relaxed.
	 */
	public default void modelRelaxed() {
	}

	/**
	 * Called after a running EvoLudo model stopped because the model converged (or
	 * reached an absorbing state).
	 */
	public default void modelStopped() {
	}

	/**
	 * Called after the EvoLudo model got re-initialized.
	 */
	public default void modelDidReinit() {
	}

	/**
	 * Called after the EvoLudo model was reset.
	 */
	public default void modelDidReset() {
	}
}