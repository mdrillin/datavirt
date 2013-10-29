package org.jboss.datavirtualization.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface VDBRedeployEventHandler extends EventHandler {
	
	void onEvent(VDBRedeployEvent event);

}
