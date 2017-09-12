package com.bonitasoft.scenario.accessor.exception;

import java.util.List;
import java.util.stream.Collectors;

public class FunctionMandatoryParameterException extends Exception {
	public FunctionMandatoryParameterException(String parameter) {
		super("Impossible to retrieve " + parameter);
	}
	
	public FunctionMandatoryParameterException(List<String> parameters) {
		super("Impossible to retrieve at least one "); // of these: " + parameters.stream().map(Object::toString).collect(Collectors.joining(", ")));
	}
}