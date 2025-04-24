package com.simplifyqa.codeeditor;

import com.simplifyqa.abstraction.driver.IQAWebDriver;
import com.simplifyqa.pluginbase.codeeditor.annotations.AutoInjectCurrentObject;
import com.simplifyqa.pluginbase.codeeditor.annotations.AutoInjectWebDriver;
import com.simplifyqa.pluginbase.codeeditor.annotations.SyncAction;
import com.simplifyqa.pluginbase.common.enums.TechnologyType;
import com.simplifyqa.pluginbase.common.models.SqaObject;
import com.simplifyqa.pluginbase.plugin.annotations.ObjectTemplate;
import com.simplifyqa.web.base.search.FindBy;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Hello there!!, please keep the following things in mind while creating custom
 * class
 * Your class should have a public default constructor.
 * 
 * @SyncAction methods should be public and return a boolean value not void or
 *             anything else
 *             @AutoInjectWebDriver/ @AutoInjectAndroidDriver /
 *             @AutoInjectIOSDriver/ @AutoInjectApiDriver indicates the driver
 *             you want to use.
 *             uniqueId field in @SyncAction annotation should be unique
 *             throughout the project
 */

public class SampleClass {
    @AutoInjectWebDriver
    private IQAWebDriver driver;

    @AutoInjectCurrentObject
    private SqaObject currObject;

    public SampleClass() {
    }

    @SyncAction(uniqueId = "custom-addition", groupName = "Generic", objectTemplate = @ObjectTemplate(name = TechnologyType.GENERIC, description = "This action belongs to GENERIC"), objectRequired = false)
    public boolean customAddition() {
        System.out.println("2+3=" + 2 + 3);
        return true;
    }

    @SyncAction(uniqueId = "generate-random-number", groupName = "Generic", objectTemplate = @ObjectTemplate(name = TechnologyType.GENERIC, description = "This action generates random numbers"), objectRequired = false)
    public boolean generateRandomNumber() {
        Random random = new Random();
        int randomNum = random.nextInt(40);
        System.out.println("Generated Random Number: " + randomNum);
        return true;
    }

}
