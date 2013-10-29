package org.jboss.datavirtualization.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface FileUploadedEventHandler extends EventHandler {
	
	void onEvent(FileUploadedEvent event);

}
