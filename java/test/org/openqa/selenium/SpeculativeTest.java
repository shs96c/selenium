package org.openqa.selenium;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.testing.JUnit4TestBase;

public class SpeculativeTest extends JUnit4TestBase {

  @Test
  public void typeSomething() {
    driver.get(pages.javascriptPage);

    WebElement element = driver.findElement(By.id("keyReporter"));

    new Actions(driver).keyDown(Keys.SHIFT).perform();
    element.sendKeys("ab", Keys.NULL, "cd");
    new Actions(driver).sendKeys("e").perform();

    String value = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value", element);
    fail(value);
  }

}
