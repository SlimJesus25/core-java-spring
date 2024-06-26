/********************************************************************************
 * Copyright (c) 2019 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   AITIA - implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.common.dto.internal;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceInterfaceRequestDTO implements Serializable {

	//=================================================================================================
	// members

	private static final long serialVersionUID = -5448099699710703982L;

	private String interfaceName;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceRequestDTO() {}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceRequestDTO(final String interfaceName) {
		this.interfaceName = interfaceName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInterfaceName() { return interfaceName; }

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceName(final String interfaceName) { this.interfaceName = interfaceName; }
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (final JsonProcessingException ex) {
			return "toString failure";
		}
	}
}