/*
 * Copyright (c) 1996, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

package org.evoludo.geom;

/**
 * The {@code NoninvertibleTransformException} class represents
 * an exception that is thrown if an operation is performed requiring
 * the inverse of an {@link AffineTransform} object but the
 * {@code AffineTransform} is in a non-invertible state.
 * 
 * @author Christoph Hauert
 *         Adapted from {@code java.awt.geom.NoninvertibleTransformException}.
 *         Generally made more GWT friendly for use in EvoLudo project.
 */
public class NoninvertibleTransformException extends java.lang.Exception {

	/**
	 * Use serialVersionUID for interoperability.
	 */
	private static final long serialVersionUID = 6137225240503990466L;

	/**
	 * Constructs an instance of {@code NoninvertibleTransformException} with the
	 * specified detail message.
	 * 
	 * @param s the detail message
	 * @since 1.2
	 */
	public NoninvertibleTransformException(String s) {
		super(s);
	}
}
