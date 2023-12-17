//
// EvoLudo Project
//
// Copyright 2020 Christoph Hauert
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

package org.evoludo.simulator.views;

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasS3} interface include a graphical view that depicts the
 * mean state of the population in the simplex \(S_3\) as a
 * function of time in their GUI: {@link org.evoludo.simulator.MVS3}
 * for GWT and {@link org.evoludo.simulator.MVDS3} for JRE.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */

public interface HasS3 {
}
