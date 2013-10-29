package org.jboss.datavirtualization.client.rpc;

import java.io.Serializable;

public class TeiidServiceException extends Exception implements Serializable {

	private static final long serialVersionUID = 1L;

	public TeiidServiceException() {
	}

	public TeiidServiceException(String message) {
		super(message);
	}

}