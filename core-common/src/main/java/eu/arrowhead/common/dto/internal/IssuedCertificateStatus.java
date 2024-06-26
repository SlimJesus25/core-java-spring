/********************************************************************************
 * Copyright (c) 2020 Evopro
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Evopro - implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.common.dto.internal;

public enum IssuedCertificateStatus {
    UNKNOWN("unknown"),
    GOOD("good"),
    REVOKED("revoked"),
    EXPIRED("expired");
	
	//=================================================================================================
	// methods

    //-------------------------------------------------------------------------------------------------
	IssuedCertificateStatus(final String name) {
        this.name = name;
    }

    //-------------------------------------------------------------------------------------------------
	public String toString() { return name; }
	
	//=================================================================================================
	// members

    private final String name;
}