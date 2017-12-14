package com.bonitasoft.process.starter.command;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.contract.validation.ContractValidator;
import org.bonitasoft.engine.bpm.contract.validation.ContractValidatorFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SContractDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeType;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SContractViolationException;
import org.bonitasoft.engine.expression.ExpressionService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

public class AdvancedStartProcessValidator {

	private final ProcessDefinitionService processDefinitionService;
	private final long processDefinitionId;
	private TechnicalLoggerService technicalLoggerService;
	private ExpressionService expressionService;

	public AdvancedStartProcessValidator(ProcessDefinitionService processDefinitionService, long processDefinitionId, TechnicalLoggerService technicalLoggerService, ExpressionService expressionService) {
		this.processDefinitionService = processDefinitionService;
		this.processDefinitionId = processDefinitionId;
		this.technicalLoggerService = technicalLoggerService;
		this.expressionService = expressionService;
	}

	public List<String> validate(List<String> flowNodeNames, Map<String, Serializable> processContractInputs) throws SBonitaException {
		List<String> problems = new ArrayList<String>();
		List<String> foundFlowNodes = new ArrayList(flowNodeNames.size());
		SProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(processDefinitionId);
		problems.addAll(checkFlowNodesAreSupported(flowNodeNames, foundFlowNodes, processDefinition));
		problems.addAll(checkForNotFoundFlowNodes(flowNodeNames, foundFlowNodes, processDefinition));
		if (!problems.isEmpty()) {
			// check contract only if flow nodes are ok
			return problems;
		}
		problems.addAll(checkProcessContract(processContractInputs, processDefinition));
		return problems;
	}

	private List<String> checkProcessContract(Map<String, Serializable> processContractInputs, SProcessDefinition processDefinition) {
		return validateContract(processContractInputs, processDefinition.getContract(), processDefinition.getName());
	}

	private List<String> validateContract(Map<String, Serializable> inputs, SContractDefinition contract, String element) {
		if (contract == null) {
			return Collections.emptyList();
		}
		final ContractValidator validator = new ContractValidatorFactory().createContractValidator(technicalLoggerService, expressionService);
		try {
			validator.validate(processDefinitionId, contract, inputs);
		} catch (SContractViolationException e) {
			return e.getExplanations().isEmpty() ? Collections.singletonList(e.getSimpleMessage() + " on " + element) : appendElement(e, element);
		}
		return Collections.emptyList();
	}

	private List<String> appendElement(SContractViolationException e, String element) {
		ArrayList<String> strings = new ArrayList<String>();
		for (String explanation : e.getExplanations()) {
			strings.add(explanation + " on " + element);
		}
		return strings;
	}

	private List<String> checkFlowNodesAreSupported(List<String> flowNodeNames, List<String> foundFlowNodes, SProcessDefinition processDefinition) {
		List<String> problems = new ArrayList<String>();
		for (SFlowNodeDefinition flowNode : processDefinition.getProcessContainer().getFlowNodes()) {
			boolean invalidType = SFlowNodeType.BOUNDARY_EVENT.equals(flowNode.getType()) || SFlowNodeType.SUB_PROCESS.equals(flowNode.getType()) || SFlowNodeType.GATEWAY.equals(flowNode.getType());
			if (flowNodeNames.contains(flowNode.getName())) {
				foundFlowNodes.add(flowNode.getName());
				if (invalidType) {
					problems.add(buildInvalidTypeErrorMessage(processDefinition, flowNode));
				}
			}
		}
		return problems;
	}

	private List<String> checkForNotFoundFlowNodes(List<String> flowNodeNames, List<String> foundFlowNodes, SProcessDefinition processDefinition) {
		List<String> problems = new ArrayList<String>();
		for (String flowNodeName : flowNodeNames) {
			if (!foundFlowNodes.contains(flowNodeName)) {
				problems.add(buildFlowNodeNotFoundErroMessage(processDefinition, flowNodeName));
			}
		}
		return problems;
	}

	private String buildInvalidTypeErrorMessage(SProcessDefinition processDefinition, SFlowNodeDefinition flowNode) {
		return "'" + flowNode.getName() + "' is not a valid start point for the process " + buildProcessContext(processDefinition) + " You cannot start a process from a gateway, a boundary event or an event sub-process";
	}

	private String buildFlowNodeNotFoundErroMessage(SProcessDefinition processDefinition, String flowNodeName) {
		return "No flownode named '" + flowNodeName + "' was found in the process" + buildProcessContext(processDefinition);
	}

	private String buildProcessContext(SProcessDefinition processDefinition) {
		return "<id: " + processDefinitionId + ", name: " + processDefinition.getName() + ", version: " + processDefinition.getVersion() + ">.";
	}

}