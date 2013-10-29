package org.jboss.datavirtualization.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface SourcesChangedEventHandler extends EventHandler {
	
	void onEvent(SourcesChangedEvent event);

}
