package org.jboss.datavirtualization.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class FileUploadedEvent extends GwtEvent<FileUploadedEventHandler> {

	public static Type<FileUploadedEventHandler> TYPE = new Type<FileUploadedEventHandler>();

	private String uploadType = "UNKNOWN";
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<FileUploadedEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(FileUploadedEventHandler handler) {
		handler.onEvent(this);
	}
	
	public void setUploadType(String typeStr) {
		this.uploadType = typeStr;
	}
	
	public String getUploadType() {
		return this.uploadType;
	}

}
