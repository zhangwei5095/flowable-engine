package org.flowable.examples.bpmn.shell;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

public class ShellTaskTest extends PluggableFlowableTestCase {

  enum OsType {
    LINUX, WINDOWS, MAC, SOLARIS, UNKOWN
  }

  OsType osType;

  OsType getSystemOsType() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win"))
      return OsType.WINDOWS;
    else if (osName.contains("mac"))
      return OsType.MAC;
    else if ((osName.contains("nix")) || (osName.contains("nux")))
      return OsType.LINUX;
    else if (osName.contains("sunos"))
      return OsType.SOLARIS;
    else
      return OsType.UNKOWN;
  }

  protected void setUp() throws Exception {
    osType = getSystemOsType();
  }

  public void testOsDetection() throws Exception {
    assertNotSame(osType, OsType.UNKOWN);
  }

  @Deployment
  public void testEchoShellWindows() {
    if (osType == OsType.WINDOWS) {

      ProcessInstance pi = runtimeService.startProcessInstanceByKey("echoShellWindows");

      String st = (String) runtimeService.getVariable(pi.getId(), "resultVar");
      assertNotNull(st);
      assertTrue(st.startsWith("EchoTest"));
    }
  }

  @Deployment
  public void testEchoShellLinux() {
    if (osType == OsType.LINUX) {

      ProcessInstance pi = runtimeService.startProcessInstanceByKey("echoShellLinux");

      String st = (String) runtimeService.getVariable(pi.getId(), "resultVar");
      assertNotNull(st);
      assertTrue(st.startsWith("EchoTest"));
    }
  }

  @Deployment
  public void testEchoShellMac() {
    if (osType == OsType.MAC) {

      ProcessInstance pi = runtimeService.startProcessInstanceByKey("echoShellMac");

      String st = (String) runtimeService.getVariable(pi.getId(), "resultVar");
      assertNotNull(st);
      assertTrue(st.startsWith("EchoTest"));
    }
  }
}
