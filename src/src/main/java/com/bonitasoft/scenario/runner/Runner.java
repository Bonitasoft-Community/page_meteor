package com.bonitasoft.scenario.runner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.runner.context.RunContext;
import com.bonitasoft.scenario.runner.groovy.GroovyScriptExecutor;

import groovy.lang.Binding;

public abstract class Runner {
	protected RunContext runContext = null;

	private List<RunListener> runListeners = new ArrayList<RunListener>();

	protected ScenarioResult scenarioResult = new ScenarioResult();

	public Runner(RunContext runContext, List<RunListener> runListeners) throws Exception {
		this.runContext = runContext;
		this.runListeners = runListeners;
	}

	protected void log(Level level, String message, Throwable throwable) {
		level = (level != null ? level : Level.INFO);
		message = runContext.toString() + (message != null && !message.isEmpty() ? " - " + message : "");
		if (throwable != null) {
			ScenarioConfiguration.logger.log(level, message, throwable);
		} else {
			ScenarioConfiguration.logger.log(level, message);
		}
	}

	public void run() throws Exception {
		start();
		execute();
		end();
	}

	protected List<String> processImports(String gsContent) throws Exception {
		StringBuffer lightGSContent = new StringBuffer();
		StringBuffer imports = new StringBuffer();

		if (gsContent != null) {
			List<String> result = new ArrayList<String>();

			// Remove the comments
			gsContent = gsContent.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");

			String[] lines = gsContent.split(Constants.LINE_SEPARATOR_REGEX, -1);
			boolean importDone = false;
			for (String line : lines) {
				if (!importDone && line.trim().startsWith(Constants.IMPORT_KEYWORD)) {
					imports.append(line + Constants.LINE_SEPARATOR);
				} else {
					importDone = true;
					lightGSContent.append(line + Constants.LINE_SEPARATOR);
				}
			}

			result.add(imports.toString());
			result.add(lightGSContent.toString());
			return result;
		} else {
			throw new Exception("The Scenario script cannot be null");
		}
	}

	protected ScenarioResult groovyEvaluation(String gsContent, Map<String, byte[]> dependencies) {
		try {
			Binding scriptBinding = new Binding(new HashMap<String, Serializable>());
			Accessor accessor = new Accessor(gsContent.split(Constants.LINE_SEPARATOR_REGEX, -1), scriptBinding, runContext, runListeners, scenarioResult);
			try {
				scriptBinding.setVariable(Accessor.HOOK, accessor);
				log(Level.INFO, "START", null);
				GroovyScriptExecutor.evaluate("runGS" + this.getClass().getSimpleName(), gsContent, scriptBinding, dependencies);
				accessor.setAdvancementAsComplete();
				log(Level.INFO, "END", null);
			} catch (Throwable e) {
				scenarioResult.addError(e, "GS Scenario execution");
				return scenarioResult;
			}
		} catch (Throwable e) {
			scenarioResult.addError(e, "The setup of the Scenario Accessor failed");
			return scenarioResult;
		}

		return scenarioResult;
	}

	private void start() {
		ScenarioConfiguration.logger.info("Start to run");
	}

	abstract protected void execute() throws Exception;

	private void end() {
		ScenarioConfiguration.logger.info("Finish to run");
	}

	public List<RunListener> getRunListeners() {
		return runListeners;
	}

	public ScenarioResult getScenarioResult() {
		return scenarioResult;
	}
}
