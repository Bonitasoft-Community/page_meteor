package com.bonitasoft.process.starter.command;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.command.system.CommandWithParameters;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.operation.Operation;

import org.bonitasoft.engine.command.TenantCommand;
import org.bonitasoft.engine.service.TenantServiceAccessor;

public class ProcessStarterCommand extends CommandWithParameters {
	public static final String NAME = ProcessStarterCommand.class.getSimpleName();
	public static final String SUMMARY = "Provide a complete function to start a process in any way";
	
	public static final String STARTED_BY = "started_by";
	public static final String PROCESS_DEFINITION_ID = "process_definition_id";
	public static final String OPERATIONS = "operations";
	public static final String CONTEXT = "context";
	public static final String PROCESS_CONTRACT_INPUTS = "process_contract_inputs";
    public static final String ACTIVITY_NAMES = "activity_names";

    
	public Serializable execute(final Map<String, Serializable> parameters, final TenantServiceAccessor serviceAccessor) throws SCommandParameterizationException, SCommandExecutionException {
		// get parameters
		final long startedBy = getStartedBy(parameters);
		final long processDefinitionId = getProcessDefinitionId(parameters);
		final Map<String, Serializable> processContractInputs = getProcessContractInputs(parameters);
		final List<Operation> operations = getOperations(parameters);
		final Map<String, Serializable> context = getContext(parameters);
		final List<String> activityNames = getActivityNames(parameters);
		try {
			validateInputs(serviceAccessor, processDefinitionId, activityNames, processContractInputs);

			final ProcessStarter starter = new ProcessStarter(startedBy, processDefinitionId, operations, context, activityNames, processContractInputs);
			return starter.start();
		} catch (final SCommandExecutionException e) {
			throw e;
		} catch (final Exception e) {
			throw new SCommandExecutionException(e);
		}
	}

	private void validateInputs(final TenantServiceAccessor serviceAccessor, final long processDefinitionId, final List<String> activityNames, Map<String, Serializable> processContractInputs) throws SBonitaException {
		final AdvancedStartProcessValidator validator = new AdvancedStartProcessValidator(serviceAccessor.getProcessDefinitionService(), processDefinitionId, serviceAccessor.getTechnicalLoggerService(), serviceAccessor.getExpressionService());
		final List<String> problems = validator.validate(activityNames, processContractInputs);
		handleProblems(problems);
	}

	private void handleProblems(final List<String> problems) throws SCommandExecutionException {
		if (!problems.isEmpty()) {
			final StringBuilder stb = new StringBuilder();
			for (final String problem : problems) {
				stb.append(problem);
				stb.append("\n");
			}
			throw new SCommandExecutionException(stb.toString());
		}
	}

	private Long getStartedBy(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
		Long startedBy = 0L;
		if(parameters.containsKey(STARTED_BY)) {
			startedBy = Long.parseLong(parameters.get(STARTED_BY).toString());
		}
		
		return startedBy;
	}

	private Long getProcessDefinitionId(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
		return getLongMandadoryParameter(parameters, PROCESS_DEFINITION_ID);
	}

	private List<Operation> getOperations(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
		return getParameter(parameters, OPERATIONS);
	}

	private Map<String, Serializable> getContext(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
		return getParameter(parameters, CONTEXT);
	}

	private Map<String, Serializable> getProcessContractInputs(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
		return getParameter(parameters, PROCESS_CONTRACT_INPUTS);
	}
	
	private List<String> getActivityNames(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getParameter(parameters, ACTIVITY_NAMES);
    }
}
