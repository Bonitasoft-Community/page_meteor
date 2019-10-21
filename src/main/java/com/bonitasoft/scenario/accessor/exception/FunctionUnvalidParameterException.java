package com.bonitasoft.scenario.accessor.exception;

public class FunctionUnvalidParameterException extends Exception {
	public FunctionUnvalidParameterException(String parameter, String stringifiedValue, String expectation) {
		super("The value of " + parameter + " is " + stringifiedValue + " but it is supposed to " + expectation);
	}

	public FunctionUnvalidParameterException(String parameter, String stringifiedValue, String expectation, Throwable throwable) {
		super("The value of the " + parameter + " is " + stringifiedValue + " but it is supposed to " + expectation, throwable);
	}
}