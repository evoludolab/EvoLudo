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

/**
 * {@code RunListener}s are informed about the state of model execution. The
 * implementation of all methods is optional.
 *
 * @author Christoph Hauert
 */
public interface RunListener {

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
	 * Called when the settings of the EvoLudo model changed but no reset was
	 * necessary.
	 */
	public default void modelSettings() {
	}

	/**
	 * Called after the EvoLudo model got re-initialized.
	 */
	public default void modelDidInit() {
	}

	/**
	 * Called after the EvoLudo model was reset.
	 */
	public default void modelDidReset() {
	}
}
