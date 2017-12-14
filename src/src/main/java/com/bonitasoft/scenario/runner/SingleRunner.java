package com.bonitasoft.scenario.runner;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;

import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.runner.context.SingleRunContext;

public class SingleRunner extends Runner {
	protected SingleRunContext singleRunContext = null;

	public SingleRunner(SingleRunContext singleRunContext, List<RunListener> runListeners) throws Exception {
		super(singleRunContext, runListeners);

		this.singleRunContext = (SingleRunContext) runContext;
	}

	@Override
	public void execute() throws Exception {
		// Generate the Scenario GS content
		String gsContent = singleRunContext.getGSContent();
		if (gsContent == null || gsContent.isEmpty()) {
			scenarioResult.addError(null, "The Scenario content is empty, nothing run");
		} else {
			// Extract the imports/script coming from the scenario resource
			List<String> processedGSContent = processImports(gsContent);
			String gsContentImports = processedGSContent.get(0);
			gsContent = processedGSContent.get(1);

			// Extract and combine the imports/scripts coming from GS
			// dependencies
			StringBuffer gsDependencyContentImportsBuffer = new StringBuffer();
			StringBuffer gsDependencyContentBuffer = new StringBuffer();
			for (String gsDependency : singleRunContext.getGsDependencies().keySet()) {
				List<String> processedGsDependencyContent = processImports(new String(singleRunContext.getGsDependencies().get(gsDependency)));
				gsDependencyContentImportsBuffer.append(processedGsDependencyContent.get(0) + Constants.LINE_SEPARATOR);
				gsDependencyContentBuffer.append(processedGsDependencyContent.get(1) + Constants.LINE_SEPARATOR);
			}

			// Build the header (add the snippet imports in the functions part
			// and then the functions in the header part)
			URL resource = SingleRunner.class.getResource("functions.groovy");
			String functions = resource == null ? "" : IOUtils.toString(resource);
			functions = functions.replaceAll("IMPORTS", gsContentImports + Constants.LINE_SEPARATOR + gsDependencyContentImportsBuffer.toString());
			resource = SingleRunner.class.getResource("singleHeader.groovy");
			String singleHeader = resource == null ? "" : IOUtils.toString(resource);
			singleHeader = singleHeader.replaceAll("FUNCTIONS", functions + Constants.LINE_SEPARATOR + gsDependencyContentBuffer.toString());

			// Get the footer
			resource = SingleRunner.class.getResource("footer.groovy");
			String footer = resource == null ? "" : IOUtils.toString(resource);

			// Append the generated header, scenario without imports and footer
			String bigGsContent = singleHeader + Constants.LINE_SEPARATOR + gsContent + Constants.LINE_SEPARATOR + footer;
			log(Level.FINE, "Generated Scenario script to be run:" + Constants.LINE_SEPARATOR + gsContent, null);

			// Launch the GS with a suitable context for the Test Suite to run
			// over BOS API
			scenarioResult = groovyEvaluation(bigGsContent, singleRunContext.getJarDependencies());

			// Log the result
			log(Level.FINE, "RESULT:" + Constants.LINE_SEPARATOR + scenarioResult.generateVisualResult(), null);
		}
	}
}
