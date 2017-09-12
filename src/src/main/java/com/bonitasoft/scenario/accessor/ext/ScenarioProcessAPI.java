package com.bonitasoft.scenario.accessor.ext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bonitasoft.engine.bpm.actor.ActorCriterion;
import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.actor.ActorMember;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.document.impl.ArchivedDocumentImpl;
import org.bonitasoft.engine.bpm.document.impl.DocumentImpl;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.business.data.MultipleBusinessDataReference;
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionType;
import org.bonitasoft.engine.form.FormMapping;
import org.bonitasoft.engine.form.FormMappingSearchDescriptor;
import org.bonitasoft.engine.form.FormMappingType;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;

// import com.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;

import com.bonitasoft.process.starter.command.ProcessStarterCommand;
import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.ext.data.BusinessVariableAccessor;
import com.bonitasoft.scenario.accessor.ext.data.DataAccessorManager;
import com.bonitasoft.scenario.accessor.ext.data.DocumentVariableAccessor;
import com.bonitasoft.scenario.accessor.ext.data.GlobalVariableAccessor;
import com.bonitasoft.scenario.accessor.ext.data.LocalVariableAccessor;
import com.bonitasoft.scenario.accessor.ext.data.ReferencedVariableAccessor;
import com.bonitasoft.scenario.accessor.ext.waiter.TaskExecutionWaiter;
import com.bonitasoft.scenario.accessor.parameter.Extractor;
import com.bonitasoft.scenario.accessor.resource.ResourceType;

public class ScenarioProcessAPI {
	private final static int PROCESS_VARIABLES_MAX_NUMBER = 1000;
	
	static public boolean undeployProcesses(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "undeployProcesses";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		List<ProcessDefinition> processDefinitions = getListOfProcessDefinitions(accessor, parameters);

		// Delete all the processes
		for(ProcessDefinition processDefinition : processDefinitions) {
			accessor.log(Level.FINE, methodName + ": deleting the process " + processDefinition.getName() + "-" + processDefinition.getVersion());
			final ProcessDeploymentInfo processDeploymentInfo = accessor.getDefaultProcessAPI().getProcessDeploymentInfo(processDefinition.getId());
			if(processDeploymentInfo.getActivationState() == ActivationState.ENABLED) {
				accessor.log(Level.FINE, methodName + ": disable it");
				accessor.getDefaultProcessAPI().disableProcess(processDeploymentInfo.getProcessId());
			}
			
			// Purge the cases
			accessor.log(Level.FINE, methodName + ": purge its process instances");
			purgeProcesses(accessor, parameters);
			accessor.log(Level.FINE, methodName + ": delete it");
			accessor.getDefaultProcessAPI().deleteProcessDefinition(processDeploymentInfo.getProcessId());
		}
		
		return true;
	}

	static private boolean undeployOneProcess(Accessor accessor, Serializable processDefinition) throws Exception {
		Map<String, Serializable> parameters = new HashMap<String, Serializable>();
		Serializable[] processes = new Long[1];
		processes[0] = processDefinition;
		parameters.put(Constants.PROCESSES, processes);
		return undeployProcesses(accessor, parameters);
	}
	
	static public boolean purgeProcesses(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "purgeProcesses";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));
		
		List<ProcessDefinition> processDefinitions = getListOfProcessDefinitions(accessor, parameters);

		// Delete all the cases
		for(ProcessDefinition processDefinition : processDefinitions) {
			accessor.log(Level.FINE, methodName + ": purging the process instances of the process " + processDefinition.getName() + "-" + processDefinition.getVersion());
			accessor.log(Level.FINE, methodName + ": purge the active ones");
			accessor.log(Level.FINE, methodName + ": " + accessor.getDefaultProcessAPI().deleteProcessInstances(processDefinition.getId(), 0, Integer.MAX_VALUE) + " have been purged");
			accessor.log(Level.FINE, methodName + ": purge the archived ones");
			accessor.log(Level.FINE, methodName + ": " + accessor.getDefaultProcessAPI().deleteArchivedProcessInstances(processDefinition.getId(), 0, Integer.MAX_VALUE) + " have been purged");
		}
		
		return true;
	}
	
	static private List<ProcessDefinition> getListOfProcessDefinitions(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		List<ProcessDefinition> processDefinitions = Extractor.getListOfProcessDefinitions(parameters.get(Constants.PROCESSES), Constants.PROCESSES, false, accessor);
		if(processDefinitions == null) {
			processDefinitions = new ArrayList<ProcessDefinition>();
			for(ProcessDeploymentInfo processDeploymentInfo : accessor.getDefaultProcessAPI().searchProcessDeploymentInfos(new SearchOptionsBuilder(0, Integer.MAX_VALUE).done()).getResult()) {
				processDefinitions.add(accessor.getDefaultProcessAPI().getProcessDefinition(processDeploymentInfo.getProcessId()));
			}
		}
		
		return processDefinitions;
	}
	
	static public ProcessDefinition deployProcess(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "deployProcess";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		BusinessArchive businessArchive = (BusinessArchive)Extractor.getScenarioResource(parameters.get(Constants.RESOURCE_NAME), Constants.RESOURCE_NAME, true, accessor, ResourceType.PROCESS);
		String actorsResource = (String)Extractor.getScenarioResource(parameters.get(Constants.ACTORS), Constants.ACTORS, false, accessor, ResourceType.PROCESS_ACTORS);
		ArrayList<Map<Serializable, Serializable>> connectors = Extractor.getListOfConnectorMaps(parameters.get(Constants.CONNECTORS), Constants.CONNECTORS, false, accessor);
		byte[] parametersResource = (byte[])Extractor.getScenarioResource(parameters.get(Constants.PARAMETERS), Constants.PARAMETERS, false, accessor, ResourceType.PROCESS_PARAMETERS);
		Boolean defaultFormMapping = Extractor.getBoolean(parameters.get(Constants.DEFAULT_FORM_MAPPING), Constants.DEFAULT_FORM_MAPPING, false);
		Boolean enable = Extractor.getBoolean(parameters.get(Constants.ENABLE), Constants.ENABLE, false);
		
		if(!accessor.getDefaultProcessAPI().searchProcessDeploymentInfos(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(ProcessDeploymentInfoSearchDescriptor.NAME, businessArchive.getProcessDefinition().getName()).filter(ProcessDeploymentInfoSearchDescriptor.VERSION, businessArchive.getProcessDefinition().getVersion()).done()).getResult().isEmpty()) {
			accessor.log(Level.FINE, methodName + ": undeploy the old process");
			undeployOneProcess(accessor, accessor.getDefaultProcessAPI().getProcessDefinitionId(businessArchive.getProcessDefinition().getName(), businessArchive.getProcessDefinition().getVersion()));
		}

		// Deployment
		accessor.log(Level.FINE, methodName + ": deploy the new process from the BAR");
		ProcessDefinition processDefinition = accessor.getDefaultProcessAPI().deploy(businessArchive);
		
		// Configuration load
		if(actorsResource != null) {
			accessor.log(Level.FINE, methodName + ": deploying the provided actors mapping configuration");
			accessor.log(Level.FINE, methodName + ": purge all the actors");
			final List<ActorInstance> actors = accessor.getDefaultProcessAPI().getActors(processDefinition.getId(), 0, Integer.MAX_VALUE, ActorCriterion.NAME_ASC);
			for (final ActorInstance actor : actors) {
				for (final ActorMember actorMember : accessor.getDefaultProcessAPI().getActorMembers(actor.getId(), 0, Integer.MAX_VALUE)) {
					accessor.getDefaultProcessAPI().removeActorMember(actorMember.getId());
				}
			}
			accessor.log(Level.FINE, methodName + ": import the provided actors mapping");
			accessor.getDefaultProcessAPI().importActorMapping(processDefinition.getId(), actorsResource);
		}

		// Connectors load
		if(connectors != null) {
			accessor.log(Level.FINE, methodName + ": deploying all the connector implementations");
			for(Map<Serializable, Serializable> connector : connectors) {
				accessor.log(Level.FINE, methodName + ": deploy the connector definition " + connector.get(Constants.NAME) + "-" + connector.get(Constants.VERSION));
				// only BONITASOFT SUBSCRIPTION accessor.getDefaultProcessAPI().setConnectorImplementation(processDefinition.getId(), (String)connector.get(Constants.NAME), (String)connector.get(Constants.VERSION), (byte[])connector.get(Constants.RESOURCE_NAME));
			}
		}
		
		// Parameters load
		if(parametersResource != null) {
			accessor.log(Level.FINE, methodName + ": import parameters");
			// only BONITASOFT SUBSCRIPTION accessor.getDefaultProcessAPI().importParameters(processDefinition.getId(), parametersResource);
		}

		// Add the auto generated forms by default for the process instantiation form and human tasks in case it is
		if(defaultFormMapping == null || defaultFormMapping) {
			accessor.log(Level.FINE, methodName + ": change the forms/pages mapping if not defined using auto generated ones");
			List<FormMapping> formMappings = new ArrayList<FormMapping>();
			formMappings.addAll(accessor.getDefaultProcessAPI().searchFormMappings(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(FormMappingSearchDescriptor.PROCESS_DEFINITION_ID, processDefinition.getId()).filter(FormMappingSearchDescriptor.TYPE, FormMappingType.PROCESS_OVERVIEW).done()).getResult());
			formMappings.addAll(accessor.getDefaultProcessAPI().searchFormMappings(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(FormMappingSearchDescriptor.PROCESS_DEFINITION_ID, processDefinition.getId()).filter(FormMappingSearchDescriptor.TYPE, FormMappingType.PROCESS_START).done()).getResult());
			formMappings.addAll(accessor.getDefaultProcessAPI().searchFormMappings(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(FormMappingSearchDescriptor.PROCESS_DEFINITION_ID, processDefinition.getId()).filter(FormMappingSearchDescriptor.TYPE, FormMappingType.TASK).done()).getResult());
			for(FormMapping formMapping : formMappings) {
				if(formMapping.getURL() == null) {
					List<Page> pages = null;
					if(FormMappingType.PROCESS_START == formMapping.getType()) {
						pages = accessor.getDefaultCustomPageAPI().searchPages(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(PageSearchDescriptor.NAME, "custompage_processAutogeneratedForm").done()).getResult();
					} else if(FormMappingType.TASK == formMapping.getType()) {
						pages = accessor.getDefaultCustomPageAPI().searchPages(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(PageSearchDescriptor.NAME, "custompage_taskAutogeneratedForm").done()).getResult();
					} else if(FormMappingType.PROCESS_OVERVIEW == formMapping.getType()) {
						pages = accessor.getDefaultCustomPageAPI().searchPages(new SearchOptionsBuilder(0, Integer.MAX_VALUE).filter(PageSearchDescriptor.NAME, "custompage_caseoverview").done()).getResult();
					}
					if(pages != null && !pages.isEmpty()) {
						accessor.log(Level.FINE, methodName + ": the mapping " + formMapping.getType() + " is not configured, update it");
						// only BONITASOFT SUBSCRIPTION accessor.getDefaultProcessAPI().updateFormMapping(formMapping.getId(), null, pages.get(0).getId());
					}
				}
			}
		}
		
		// Enablement
		if(enable == null || enable) {
			accessor.log(Level.FINE, methodName + ": enable it");
			accessor.getDefaultProcessAPI().enableProcess(processDefinition.getId());
		}
		
		return processDefinition;
	}
	
	static public ProcessInstance startProcess(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "startProcess";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		ProcessDefinition processDefinition = Extractor.getProcessDefinition(parameters.get(Constants.PROCESS), Constants.PROCESS, false, accessor);
		String name = Extractor.getString(parameters.get(Constants.NAME), Constants.NAME, false);
		String version = Extractor.getString(parameters.get(Constants.VERSION), Constants.VERSION, false);
		HashMap<String, Serializable> inputsParameter = Extractor.getMapOfStrings(parameters.get(Constants.INPUTS), Constants.INPUTS, false);
		ArrayList<Operation> operationsParameter = Extractor.getListOfOperations(parameters.get(Constants.OPERATIONS), Constants.OPERATIONS, false);
		HashMap<String, Serializable> contextParameter = Extractor.getMapOfStrings(parameters.get(Constants.CONTEXT), Constants.CONTEXT, false);
		HashMap<String, Serializable> variablesParameter = Extractor.getMapOfStrings(parameters.get(Constants.VARIABLES), Constants.VARIABLES, false);
		HashMap<String, Serializable> documentsParameter = Extractor.getMapOfStrings(parameters.get(Constants.DOCUMENTS), Constants.DOCUMENTS, false);
		ArrayList<String> startingPointsParameter = Extractor.getListOfStrings(parameters.get(Constants.STARTING_POINTS), Constants.STARTING_POINTS, false);
		
		List<String> mandatoryParameterNames = new ArrayList<String>();
		mandatoryParameterNames.add(Constants.PROCESS);
		mandatoryParameterNames.add(Constants.NAME);
		List<Serializable> mandatoryParameterValues = new ArrayList<Serializable>();
		mandatoryParameterValues.add(parameters.get(Constants.PROCESS));
		mandatoryParameterValues.add(parameters.get(Constants.NAME));
		Extractor.checkAtLeastOne(mandatoryParameterValues, mandatoryParameterNames);

		Long processDefinitionId = null;
		if(processDefinition != null) {
			processDefinitionId = processDefinition.getId();
		} else if(name != null) {
			if(version != null) {
				accessor.log(Level.FINE, methodName + ": retrieve from name and version");
				processDefinitionId = accessor.getDefaultProcessAPI().getProcessDefinitionId(name, version);
			} else {
				accessor.log(Level.FINE, methodName + ": retrieve last version from name");
				processDefinitionId = accessor.getDefaultProcessAPI().getLatestProcessDefinitionId(name);
			}
		}
		
		// Extract the inputs if any
		HashMap<String, Serializable> inputs = new HashMap<String, Serializable>();
		if(inputsParameter != null) {
			accessor.log(Level.FINE, methodName + ": add inputs");
			inputs.putAll(inputsParameter);
		}

		// Extract the operations if any
		ArrayList<Operation> operations = new ArrayList<Operation>();
		if(operationsParameter != null) {
			accessor.log(Level.FINE, methodName + ": add operations");
			operations.addAll(operationsParameter);
		}
		
		// Extract the context if any
		HashMap<String, Serializable> context = new HashMap<String, Serializable>();
		if(contextParameter != null) {
			accessor.log(Level.FINE, methodName + ": add context");
			context.putAll(contextParameter);
		}
		
		// Extract the initial variables if any and complement the operations and context
		HashMap<String, Serializable> initialVariables = new HashMap<String, Serializable>();
		if(variablesParameter != null) {
			accessor.log(Level.FINE, methodName + ": add variables");
			initialVariables.putAll(variablesParameter);
		}
		operations.addAll(generateSetOperations(processDefinitionId, initialVariables));
		context.putAll(initialVariables);
		
		// Extract the initial documents if any and complement the operations and context
		HashMap<String, Serializable> initialDocuments = new HashMap<String, Serializable>();
		if(documentsParameter != null) {
			accessor.log(Level.FINE, methodName + ": add documents");
			initialDocuments.putAll(documentsParameter);
		}
		operations.addAll(generateSetOperations(processDefinitionId, initialDocuments));
		context.putAll(initialDocuments);
		
		// Extract the starting points if any
		ArrayList<String> startingPoints = new ArrayList<String>();
		if(startingPointsParameter != null) {
			accessor.log(Level.FINE, methodName + ": add starting points");
			startingPoints.addAll(startingPointsParameter);
		}
		
		accessor.log(Level.FINE, methodName + ": launch the command");

		// Start the process calling the command
		Map<String , Serializable> commandParameters = new HashMap<String ,Serializable>();
		commandParameters.put(ProcessStarterCommand.STARTED_BY, 0L);
		commandParameters.put(ProcessStarterCommand.PROCESS_DEFINITION_ID , processDefinitionId);
		commandParameters.put(ProcessStarterCommand.OPERATIONS, operations);
		commandParameters.put(ProcessStarterCommand.CONTEXT, context);
		commandParameters.put(ProcessStarterCommand.PROCESS_CONTRACT_INPUTS, inputs);
		commandParameters.put(ProcessStarterCommand.ACTIVITY_NAMES, startingPoints);
		return (ProcessInstance)accessor.getDefaultCommandAPI().execute(ProcessStarterCommand.NAME, commandParameters);
	}
	
	static private List<Operation> generateSetOperations(Long processDefinitionId, Map<String, Serializable> elements) throws Exception {
        List<Operation> operations = new ArrayList<Operation>();
        ClassLoaderService classLoaderService = TenantServiceSingleton.getInstance(ServiceAccessorFactory.getInstance().createSessionAccessor().getTenantId()).getClassLoaderService();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ClassLoader localClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);
            Thread.currentThread().setContextClassLoader(localClassLoader);
            for (Entry<String, Serializable> element : elements.entrySet()) {
                String name = element.getKey();
                Serializable value = element.getValue();
                Expression expression = new ExpressionBuilder().createExpression(name, name, value.getClass().getName(), ExpressionType.TYPE_INPUT);
            	Operation operation = new OperationBuilder().createSetDataOperation(name, expression);
                if(value instanceof DocumentValue) {
                    operation = new OperationBuilder().createSetDocument(name, expression);
                } else {
                	operation = new OperationBuilder().createSetDataOperation(name, expression);
                }
                operations.add(operation);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        
        return operations;
	}
	
	static public HumanTaskInstance isHumanTaskAvailableInProcessInstance(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "isHumanTaskAvailableInProcessInstance";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));
		
		ProcessInstance processInstance = Extractor.getProcessInstance(parameters.get(Constants.PROCESS_INSTANCE), Constants.PROCESS_INSTANCE, true, accessor, true);
		String taskName = Extractor.getString(parameters.get(Constants.TASK_NAME), Constants.TASK_NAME, true);

		accessor.log(Level.FINE, methodName + ": look for the task");
		List<HumanTaskInstance> humanTaskIntances = accessor.getDefaultProcessAPI().getHumanTaskInstances(processInstance.getId(), taskName, 0, 1);
		if(!humanTaskIntances.isEmpty()) {
			accessor.log(Level.FINE, methodName + ": there was at least one, return the first occurrence");
			return humanTaskIntances.get(0);
		} else {
			accessor.log(Level.FINE, methodName + ": none found");
			return null;
		}
	}
	
	static public ArchivedProcessInstance isProcessInstanceArchived(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "isProcessInstanceArchived";
		parameters = Extractor.preProcessParameters(parameters);

		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		accessor.log(Level.FINE, methodName + ": look for the archived process instance");
		ProcessInstance processInstance = Extractor.getProcessInstance(parameters.get(Constants.PROCESS_INSTANCE), Constants.PROCESS_INSTANCE, true, accessor, false);
		if(processInstance == null) {
			ArchivedProcessInstance archivedProcessInstance = Extractor.getArchivedProcessInstance(parameters.get(Constants.PROCESS_INSTANCE), Constants.PROCESS_INSTANCE, false, accessor, true);
			accessor.log(Level.FINE, methodName + ": " + (archivedProcessInstance != null ? "found" : "not found"));
			return archivedProcessInstance;
		} else {
			Serializable instance = getProcessInstance(accessor, processInstance.getId());
			if(instance instanceof ArchivedProcessInstance) {
				accessor.log(Level.FINE, methodName + ": found");
				return (ArchivedProcessInstance)instance;
			} else {
				accessor.log(Level.FINE, methodName + ": not found");
				return null;
			}
		}
	}
	
	static public ArchivedActivityInstance isActivityInstanceArchived(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "isActivityInstanceArchived";
		parameters = Extractor.preProcessParameters(parameters);

		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		accessor.log(Level.FINE, methodName + ": look for the archived activity instance");
		ActivityInstance activityInstance = Extractor.getActivityInstance(parameters.get(Constants.TASK), Constants.TASK, true, accessor, false);
		if(activityInstance == null) {
			ArchivedActivityInstance archivedActivityInstance = Extractor.getArchivedActivityInstance(parameters.get(Constants.TASK), Constants.TASK, false, accessor, true);
			accessor.log(Level.FINE, methodName + ": " + (archivedActivityInstance != null ? "found" : "not found"));
			return archivedActivityInstance;
		} else {
			Serializable instance = getActivityInstance(accessor, activityInstance.getId());
			if(instance instanceof ArchivedProcessInstance) {
				accessor.log(Level.FINE, methodName + ": found");
				return (ArchivedActivityInstance)instance;
			} else {
				accessor.log(Level.FINE, methodName + ": not found");
				return null;
			}
		}
	}
	
	static public User executeUserTask(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "executeUserTask";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		ActivityInstance activityInstance = Extractor.getActivityInstance(parameters.get(Constants.TASK), Constants.TASK, true, accessor, true);
		Boolean autoAssign = Extractor.getBoolean(parameters.get(Constants.AUTO_ASSIGN), Constants.AUTO_ASSIGN, false);
		HashMap<String, Serializable> inputsParameter = Extractor.getMapOfStrings(parameters.get(Constants.INPUTS), Constants.INPUTS, false);

		User user = null;

		accessor.log(Level.FINE, methodName + ": retrieve the human task");
		HumanTaskInstance humanTaskInstance = accessor.getDefaultProcessAPI().getHumanTaskInstance(activityInstance.getId());
		if(humanTaskInstance.getAssigneeId() > 0L) {
			accessor.log(Level.FINE, methodName + ": get the current assignee");
			user = accessor.getDefaultIdentityAPI().getUser(humanTaskInstance.getAssigneeId());
			accessor.log(Level.FINE, methodName + ": it is " + user.getUserName());
		}

		// Auto assignment
		if((autoAssign == null || autoAssign) && user == null) {
			accessor.log(Level.FINE, methodName + ": no current assignee, auto assign");
			user = assignUserTask(accessor, parameters);
			accessor.log(Level.FINE, methodName + ": " + user.getUserName() + " is now the assignee");
		}
		
		// Execute
		if(user != null) {
			Map<String, Serializable> inputs = new HashMap<String, Serializable>();
			if(inputsParameter != null) {
				inputs.putAll(inputsParameter);
			}
			accessor.log(Level.FINE, methodName + ": execute the task given the inputs");
			accessor.getDefaultProcessAPI().executeUserTask(humanTaskInstance.getId(), inputs);
			
			accessor.log(Level.FINE, methodName + ": waiting for the execution to be done (task archive");
			return (new TaskExecutionWaiter(accessor, parameters).execute() != null ? user : null);
		} else {
			throw new Exception("Impossible to locate the assignee to use for the execution of the task " + humanTaskInstance.getId());
		}
	}
	
	static public boolean releaseUserTask(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "releaseUserTask";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		ActivityInstance activityInstance = Extractor.getActivityInstance(parameters.get(Constants.TASK), Constants.TASK, true, accessor, true);

		accessor.log(Level.FINE, methodName + ": release the task");
		accessor.getDefaultProcessAPI().releaseUserTask(accessor.getDefaultProcessAPI().getHumanTaskInstance(activityInstance.getId()).getId());
		
		return true;
	}
	
	static public User assignUserTask(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "assignUserTask";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		ActivityInstance activityInstance = Extractor.getActivityInstance(parameters.get(Constants.TASK), Constants.TASK, true, accessor, true);
		User assignee = Extractor.getUser(parameters.get(Constants.ASSIGNEE), Constants.ASSIGNEE, false, accessor);
		String assigneeName = Extractor.getString(parameters.get(Constants.ASSIGNEE_NAME), Constants.ASSIGNEE_NAME, false);

		HumanTaskInstance humanTaskInstance = accessor.getDefaultProcessAPI().getHumanTaskInstance(activityInstance.getId());
		
		if(assignee == null) {
			if(assigneeName != null) {
				assignee = accessor.getDefaultIdentityAPI().getUserByUserName(assigneeName);
			} else {
				accessor.log(Level.FINE, methodName + ": no assignee provided, look for one");
				List<User> candidates = accessor.getDefaultProcessAPI().getPossibleUsersOfPendingHumanTask(humanTaskInstance.getId(), 0, Integer.MAX_VALUE);
				if(!candidates.isEmpty()) {
					boolean existAsCandidate=false;
					for (User user : candidates)
					{
						if (user.getId() == accessor.getUser().longValue())
						{
							existAsCandidate=true;
						}
					}
					// if(candidates.stream().map(Use::getId()).collect(Collectors.toList()).contains(accessor.getUser())) {
					if (existAsCandidate)
					{
						// Assign the user if part of candidates
						accessor.log(Level.FINE, methodName + ": assign to the current user as it is in the candidates list");
						assignee = accessor.getDefaultIdentityAPI().getUser(accessor.getUser());
					} else {
						accessor.log(Level.FINE, methodName + ": assign to the first candidate found as the current user is not a candidate");
						assignee = candidates.get(0);
					}
				} else {
					throw new Exception("Impossible to assign the task " + humanTaskInstance.getId() + " because there is not candidates");
				}
			}
		}
		
		accessor.log(Level.FINE, methodName + ": assign the task to " + assignee.getUserName());
		accessor.getDefaultProcessAPI().assignUserTask(humanTaskInstance.getId(), assignee.getId());
		
		return assignee;
	}
	
	static public DataAccessorManager caseContext(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "caseContext";
		parameters = Extractor.preProcessParameters(parameters);

		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		ActivityInstance activityInstance = Extractor.getActivityInstance(parameters.get(Constants.TASK), Constants.TASK, false, accessor, false);
		ArchivedActivityInstance archivedActivityInstance = Extractor.getArchivedActivityInstance(parameters.get(Constants.TASK), Constants.TASK, false, accessor, false);
		ProcessInstance processInstance = Extractor.getProcessInstance(parameters.get(Constants.PROCESS_INSTANCE), Constants.PROCESS_INSTANCE, false, accessor, false);
		ArchivedProcessInstance archivedProcessInstance = Extractor.getArchivedProcessInstance(parameters.get(Constants.PROCESS_INSTANCE), Constants.PROCESS_INSTANCE, false, accessor, false);
		
		List<String> mandatoryParameterNames = new ArrayList<String>();
		mandatoryParameterNames.add(Constants.TASK);
		mandatoryParameterNames.add(Constants.PROCESS_INSTANCE);
		List<Serializable> mandatoryParameterValues = new ArrayList<Serializable>();
		mandatoryParameterValues.add(parameters.get(Constants.TASK));
		mandatoryParameterValues.add(parameters.get(Constants.PROCESS_INSTANCE));
		Extractor.checkAtLeastOne(mandatoryParameterValues, mandatoryParameterNames);
		
		DataAccessorManager context = new DataAccessorManager(accessor);
		
		//------- Load the context based on the information provided (provided IDs are always active but task and case may be archived or active)
		
		// Locate the task if there is an id
		Long caseId = null;
		if(activityInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve the process instance from the provided active activity");
			caseId = activityInstance.getParentProcessInstanceId();
		} else if(archivedActivityInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve the process instance from the provided archived activity");
			caseId = archivedActivityInstance.getProcessInstanceId();
		}

		// Locate the case if there is an id
		if(caseId != null) {
			Serializable theProcessInstance = getProcessInstance(accessor, caseId);
			if(theProcessInstance instanceof ProcessInstance) {
				processInstance = (ProcessInstance)theProcessInstance;
			} else if(theProcessInstance instanceof ArchivedProcessInstance) {
				archivedProcessInstance = (ArchivedProcessInstance)theProcessInstance;
			} else {
				throw new Exception("The process instance retrieved is not supported by the Scenario library: " + theProcessInstance.getClass().getName());
			}
		}
		
		//------- Retrieve and return all the variables and documents according to the gathered context
		
		//  Handle the local process variables
		if(activityInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve local process variables from the active activity instance");
			for(DataInstance dataInstance : accessor.getDefaultProcessAPI().getActivityDataInstances(activityInstance.getId(), 0, PROCESS_VARIABLES_MAX_NUMBER)) {
				context.register(dataInstance.getName(), new LocalVariableAccessor(activityInstance.getId(), dataInstance.getName()));
			}
		} else if(archivedActivityInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve local process variables from the archived activity instance");
			for(DataInstance dataInstance : accessor.getDefaultProcessAPI().getArchivedActivityDataInstances(archivedActivityInstance.getSourceObjectId(), 0, PROCESS_VARIABLES_MAX_NUMBER)) {
				context.register(dataInstance.getName(), new LocalVariableAccessor(archivedActivityInstance.getSourceObjectId(), dataInstance.getName()));
			}
		}
		
		//  Handle the global process variables
		if(processInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve global process variables from the active process instance");
			for(DataInstance dataInstance : accessor.getDefaultProcessAPI().getProcessDataInstances(processInstance.getId(), 0, PROCESS_VARIABLES_MAX_NUMBER)) {
				context.register(dataInstance.getName(), new GlobalVariableAccessor(processInstance.getId(), dataInstance.getName()));
			}
		} else if(archivedProcessInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve global process variables from the archived process instance");
			for(DataInstance dataInstance : accessor.getDefaultProcessAPI().getArchivedProcessDataInstances(archivedProcessInstance.getSourceObjectId(), 0, PROCESS_VARIABLES_MAX_NUMBER)) {
				context.register(dataInstance.getName(), new GlobalVariableAccessor(archivedProcessInstance.getSourceObjectId(), dataInstance.getName()));
			}
		}
		
		// Handle the business and document variables
		Map<String, Serializable> executionContext = new HashMap<String, Serializable>();
		if(processInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve referenced variables (business and document) from the active process instance");
			executionContext = accessor.getDefaultProcessAPI().getProcessInstanceExecutionContext(processInstance.getId());
			for (String key : executionContext.keySet()) {
				String name = ReferencedVariableAccessor.getName(key);
				Serializable referencedVariable = executionContext.get(key);
				if(referencedVariable instanceof SimpleBusinessDataReference || referencedVariable instanceof MultipleBusinessDataReference) {
					// In case of an active business variable
					context.register(name, new BusinessVariableAccessor(processInstance.getId(), name));
				} else if(referencedVariable instanceof DocumentImpl || referencedVariable instanceof ArchivedDocumentImpl) {
					// In case of an active document variable
					context.register(name, new DocumentVariableAccessor(processInstance.getId(), name));
				} else {
					throw new Exception("The execution context element type is not supported by the Scenario library: " + referencedVariable.getClass().getName());
				}
			}
		} else if(archivedProcessInstance != null) {
			accessor.log(Level.FINE, methodName + ": retrieve referenced variables (business and document) from the archived process instance");
			executionContext = accessor.getDefaultProcessAPI().getArchivedProcessInstanceExecutionContext(archivedProcessInstance.getId());
			for (String key : executionContext.keySet()) {
				String name = ReferencedVariableAccessor.getName(key);
				Serializable referencedVariable = executionContext.get(key);
				if(referencedVariable instanceof SimpleBusinessDataReference || referencedVariable instanceof MultipleBusinessDataReference) {
					// In case of an archived business variable
					context.register(name, new BusinessVariableAccessor(archivedProcessInstance.getSourceObjectId(), name));
				} else if(executionContext.get(key) instanceof DocumentImpl || executionContext.get(key) instanceof ArchivedDocumentImpl) {
					// In case of an archived document variable
					context.register(name, new DocumentVariableAccessor(archivedProcessInstance.getSourceObjectId(), name));
				} else {
					throw new Exception("The execution context element type is not supported by the Scenario library: " + referencedVariable.getClass().getName());
				}
			}
		}
		
		return context;
	}
	
	static public Serializable getActivityInstance(Accessor accessor, Long activityInstanceId) throws Exception {
		try {
			// Start looking into the active ones
			return accessor.getDefaultProcessAPI().getActivityInstance(activityInstanceId);
		} catch(ActivityInstanceNotFoundException activityInstanceNotFoundException1) {
			try {
				// Continue looking into the archived ones
				ArchivedActivityInstance archivedActivityInstance = accessor.getDefaultProcessAPI().getArchivedActivityInstance(activityInstanceId);
				if(archivedActivityInstance != null && !archivedActivityInstance.isTerminal()) {
					// Remove it if not terminated
					archivedActivityInstance = null;
				}
				
				return archivedActivityInstance;
			} catch(ActivityInstanceNotFoundException activityInstanceNotFoundException2) {
				throw new Exception("Impossible to locate the activity instance (neither active nor archived) based on the id " + activityInstanceId);
			}
		}
	}
	
	static public Serializable getProcessInstance(Accessor accessor, Long processInstanceId) throws Exception {
		try {
			// Start looking into the active ones
			return accessor.getDefaultProcessAPI().getProcessInstance(processInstanceId);
		} catch(ProcessInstanceNotFoundException processInstanceNotFoundException) {
			try {
				// Continue looking into the archived ones
				ArchivedProcessInstance archivedProcessInstance = accessor.getDefaultProcessAPI().getFinalArchivedProcessInstance(processInstanceId);
				if(archivedProcessInstance != null && archivedProcessInstance.getEndDate() == null) {
					// Remove it if not terminated
					archivedProcessInstance = null;
				}
				
				return archivedProcessInstance;
			} catch(ArchivedProcessInstanceNotFoundException archivedProcessInstanceNotFoundException) {
				throw new Exception("Impossible to locate the process instance (neither active nor archived) based on the id " + processInstanceId);
			}
		}
	}
}