package com.bonitasoft.scenario.accessor.exception;

public class FunctionUnvalidTypeParameterException extends Exception {

    public FunctionUnvalidTypeParameterException(String parameter, Class type, Class expectedType) {
        super("Invalid value type " + type.getName() + " for " + parameter + ", " + expectedType.getName() + " was expected instead");
    }
}
