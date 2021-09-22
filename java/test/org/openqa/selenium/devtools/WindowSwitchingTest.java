package org.openqa.selenium.devtools;

import org.junit.Test;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.devtools.v93.network.Network;
import org.openqa.selenium.devtools.v93.target.Target;

public class WindowSwitchingTest extends DevToolsTestBase {

  @Test
  public void shouldBeAbleToSwitchWindowsAndCloseTheOriginal() throws InterruptedException {
    driver.get("https://www.selenium.dev");

    String originalWindow = driver.getWindowHandle();
    getAllCookies();

    driver.switchTo().newWindow(WindowType.TAB);

    String newWindowHandle = driver.getWindowHandle();
    getAllCookies();

    Thread.sleep(3000);

    System.out.println("Before:" + devTools.send(devTools.getDomains().target().getTargets()));

    // this .Close() kills the dev tools session, no chance to ever retrieve a new one for the other tab
    driver.switchTo().window(originalWindow).close();
    driver.switchTo().window(newWindowHandle);
    driver.get("https://www.selenium.dev/documentation/webdriver/browser_manipulation/");

    System.out.println("After:" + devTools.send(devTools.getDomains().target().getTargets()));

    getAllCookies();
  }

  private Object getAllCookies() {
    devTools.createSessionIfThereIsNotOne();
    return devTools.send(Network.getAllCookies());
  }

}
