package org.jboss.datavirtualization.client;

import com.google.gwt.junit.client.GWTTestCase;

public class CompileGwtTest extends GWTTestCase {
  
  @Override
  public String getModuleName() {
    return "org.jboss.datavirtualization.DataVirt";
  }

  public void testSandbox() {
    assertTrue(true);
  }
  
}
