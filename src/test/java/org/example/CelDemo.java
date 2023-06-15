package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptException;
import org.projectnessie.cel.tools.ScriptHost;

import java.util.HashMap;
import java.util.Map;

public class CelDemo {
    @Test
    public void buildSimpleScript() throws ScriptException {
        String xKey = "x";
        String yKey = "y";
        String xValue = "Value of x";
        String yValue = "Value of y";
        String expectedResult = xValue + " " + yValue;

        ScriptHost scriptHost = ScriptHost.newBuilder().build();

        Script script = scriptHost.buildScript("x + ' ' + y")
                .withDeclarations(
                        Decls.newVar("x", Decls.String),
                        Decls.newVar("y", Decls.String)
                )
                .build();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x", "Value of x");
        arguments.put("y", "Value of y");

        String result = script.execute(String.class, arguments);

        System.out.println("Result: " + result);
        Assertions.assertEquals(expectedResult, result);
    }
}
