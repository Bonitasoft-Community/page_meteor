package com.bonitasoft.scenario.runner;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.runner.groovy.GroovyScriptExecutor;

public class ScenarioResult extends HashMap<String, Serializable>{
	final static public String RESULT_WARN = "WARN";
	final static public String RESULT_ERROR = "ERROR";

	private Accessor accessor = null;
	
	public ScenarioResult() {
		super();
		
		this.put(RESULT_WARN, new ArrayList<Throwable>());
		this.put(RESULT_ERROR, new ArrayList<Throwable>());
	}
	
	public void addError(Throwable throwable, String message) {
		accessor.log(Level.SEVERE, message, throwable);
		getErrors().add(extendedExceptionMessage(throwable, message));
	}

	public void addWarn(Throwable throwable, String message) {
		accessor.log(Level.WARNING, message, throwable);
		getWarns().add(extendedExceptionMessage(throwable, message));
	}
	
	public ArrayList<Throwable> getErrors() {
		return (ArrayList<Throwable>)this.get(RESULT_ERROR);
	}

	public ArrayList<Throwable> getWarns() {
		return (ArrayList<Throwable>)this.get(RESULT_WARN);
	}

	public Serializable generateVisualResult() {
		HashMap<String, Serializable> serializableResult = new HashMap<String, Serializable>();
		
		for(String key : this.keySet()) {
			if(key.equals(RESULT_WARN)) {
				// Warns
				ArrayList<String> warns = new ArrayList<String>();
				for(Throwable warn : getWarns()) {
					warns.add(extractThrowableStackTrace(warn));
				}
				serializableResult.put(RESULT_WARN, warns);
			} else if(key.equals(RESULT_ERROR)) {
				// Warns
				ArrayList<String> errors = new ArrayList<String>();
				for(Throwable error : getErrors()) {
					errors.add(extractThrowableStackTrace(error));
				}
				serializableResult.put(RESULT_ERROR, errors);
			} else {
				serializableResult.put(key, this.get(key));
			}
		}
		
		return serializableResult;
	}
	
	static public String extractThrowableStackTrace(Throwable throwable) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		printWriter.close();
		try {
			stringWriter.close();
		} catch(Exception e) {}
		
		return stringWriter.toString();
	}
	
	public Throwable extendedExceptionMessage(Throwable throwable, String additionalMessage) {
		StringBuffer messageStringBuffer = new StringBuffer(additionalMessage + ":" + Constants.LINE_SEPARATOR + Constants.LINE_SEPARATOR);
		messageStringBuffer.append(throwable.getMessage());
		
		boolean decorated = false;
		for(StackTraceElement stackTraceElement : throwable.getStackTrace()) {
			if(stackTraceElement.getFileName() != null && stackTraceElement.getFileName().startsWith(GroovyScriptExecutor.SCENARIO_GS) && stackTraceElement.getFileName().endsWith(Constants.EXTENSION_GROOVY) && stackTraceElement.getLineNumber() > 0 && stackTraceElement.getLineNumber() < accessor.getGsContent().length+1) {
				if(!decorated) {
					messageStringBuffer.append(Constants.LINE_SEPARATOR + Constants.LINE_SEPARATOR + "Scenario stack trace:" + Constants.LINE_SEPARATOR);
				}
				messageStringBuffer.append(Constants.TAB_SEPARATOR + "at " + stackTraceElement.toString() + " : " + accessor.getGsContent()[stackTraceElement.getLineNumber()-1].trim() + Constants.LINE_SEPARATOR);
				decorated = true;
			}
		}
		if(decorated) {
			messageStringBuffer.append(Constants.LINE_SEPARATOR + "Technical stack trace:");
		}
		
		Exception exception = new Exception(messageStringBuffer.toString());
		exception.setStackTrace(throwable.getStackTrace());
		return exception;
	}
	
	public void setAccessor(Accessor accessor) {
		this.accessor = accessor;
	}
}
