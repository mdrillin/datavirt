package org.jboss.datavirtualization.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface PropertyChangedEventHandler extends EventHandler {
	
	void onEvent(PropertyChangedEvent event);

}
