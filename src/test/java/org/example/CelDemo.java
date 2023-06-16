package org.example;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptException;
import org.projectnessie.cel.tools.ScriptHost;
import org.projectnessie.cel.types.jackson.JacksonRegistry;

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
                        Decls.newVar(xKey, Decls.String),
                        Decls.newVar(yKey, Decls.String)
                )
                .build();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x", "Value of x");
        arguments.put("y", "Value of y");

        String result = script.execute(String.class, arguments);

        System.out.println("Result: " + result);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void buildSimpleValidationScriptForPojo() throws ScriptException {
        String xKey = "x";
        String yKey = "y";
        int xValue = 1;
        int yValue = 2;

        ScriptHost scriptHost = ScriptHost.newBuilder().build();
        Script script = scriptHost.buildScript(
                "x == y"
        ).withDeclarations(
                Decls.newVar(xKey, Decls.Int),
                Decls.newVar(yKey, Decls.Int)
        )
        .build();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put(xKey, xValue);
        arguments.put(yKey, yValue);

        Boolean result = script.execute(Boolean.class, arguments);
        System.out.println("Result: " + result);

        Assertions.assertFalse(result);
    }

    @Test
    public void buildSimpleValidationScriptForJson() throws ScriptException {
        String inputKey = "input";
        String checkNameKey = "checkName";

        String inputNameValue = "David";
        String checkNameValue = "David";

        Input input = Input.builder()
                .name(inputNameValue)
                .build();

        ScriptHost scriptHost = ScriptHost.newBuilder()
                .registry(JacksonRegistry.newRegistry())
                .build();

        Script script = scriptHost.buildScript("input.name == checkName")
                .withDeclarations(
                        Decls.newVar(inputKey, Decls.newObjectType(Input.class.getName())),
                        Decls.newVar(checkNameKey, Decls.String))
                .withTypes(Input.class)
                .build();


        Map<String, Object> arguments = new HashMap<>();
        arguments.put(inputKey, input);
        arguments.put(checkNameKey, checkNameValue);

        boolean result = script.execute(Boolean.class, arguments);

        System.out.println("Result: " + result);
        Assertions.assertTrue(result);
    }

    @Getter
    @Setter
    @Builder
    private static class Input {
        private String name;
    }



}
