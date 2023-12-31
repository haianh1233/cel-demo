package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptCreateException;
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

    @Test
    public void buildCheckInputSizeValidationScriptForJson() throws ScriptException {
        String inputKey = "input";
        String inputNameValue = "David";

        Input input = Input.builder()
                .name(inputNameValue)
                .build();

        ScriptHost scriptHost = ScriptHost.newBuilder()
                .registry(JacksonRegistry.newRegistry())
                .build();

        Script script = scriptHost.buildScript("size(input.name) == " + inputNameValue.length())
                .withDeclarations(
                        Decls.newVar(inputKey, Decls.newObjectType(Input.class.getName()))
                )
                .withTypes(Input.class)
                .build();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put(inputKey, input);

        boolean result = script.execute(Boolean.class, arguments);

        System.out.println("Result: " + result);
        Assertions.assertTrue(result);

    }

    @Test
    public void buildCheckInputSizeValidationScriptForRawJson() throws ScriptException, JsonProcessingException {
        String inputKey = "input";
        String inputJson = "{\"name\":\"hai\",\"address\":{\"city\":\"hochiminh\",\"street\":\"nguyenhuucanh\"}}";

        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};

        Map<String, Object> input = objectMapper.readValue(inputJson, typeReference);

        ScriptHost scriptHost = ScriptHost.newBuilder()
                .registry(JacksonRegistry.newRegistry())
                .build();

        Script script = scriptHost.buildScript(
                "input.name == 'hai' " +
                        "&& input.address.city == 'hochiminh' " +
                        "&& size(input.address.street) > 5"
                )
                .withDeclarations(
                        Decls.newVar(inputKey, Decls.Any)
                )
                .build();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put(inputKey, input);

        boolean result = script.execute(Boolean.class, arguments);

        System.out.println("Result: " + result);
        Assertions.assertTrue(result);
    }

    @Test
    public void buildCheckInputSizeValidationScriptForRawJsonWithConfigRules() throws ScriptException, JsonProcessingException {
        String inputKey = "input";
        String inputJson = "{\"name\":\"hai\",\"address\":{\"city\":\"hochiminh\",\"street\":\"nguyenhuucanh\"}}";

        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};

        Map<String, Object> input = objectMapper.readValue(inputJson, typeReference);

        ScriptHost scriptHost = ScriptHost.newBuilder()
                .registry(JacksonRegistry.newRegistry())
                .build();

        Map<String, String> validationRules = loadValidationRules();
        StringBuilder scriptExpressionBuilder = new StringBuilder();
        boolean firstItem = true;

        for (Map.Entry<String, String> entry : validationRules.entrySet()) {
            String field = entry.getKey();
            String validationRule = entry.getValue();


            if (firstItem) {
                firstItem = false;
            } else {
                scriptExpressionBuilder.append(" && ");
            }

            scriptExpressionBuilder.append(validationRule.replace(field, inputKey + "." + field));
        }

        Script script = scriptHost.buildScript(scriptExpressionBuilder.toString())
                .withDeclarations(
                        Decls.newVar(inputKey, Decls.Any)
                )
                .build();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put(inputKey, input);

        boolean result = script.execute(Boolean.class, arguments);

        System.out.println("Result: " + result);
        Assertions.assertTrue(result);
    }

    private Map<String, String> loadValidationRules() {
        Map<String, String> validationRules = new HashMap<>();
        validationRules.put("name", "name == 'hai' ");
        validationRules.put("address.city", "size(address.city) > 5 ");
        validationRules.put("address.street", "size(address.street) > 5 ");

        return validationRules;
    }

    @Getter
    @Setter
    @Builder
    private static class Input {
        private String name;
    }



}
