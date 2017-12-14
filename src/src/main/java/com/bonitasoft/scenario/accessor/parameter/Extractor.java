package com.bonitasoft.scenario.accessor.parameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.session.SSessionException;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.exception.FunctionMandatoryParameterException;
import com.bonitasoft.scenario.accessor.exception.FunctionUnvalidParameterException;
import com.bonitasoft.scenario.accessor.exception.FunctionUnvalidTypeParameterException;
import com.bonitasoft.scenario.accessor.ext.ScenarioProcessAPI;
import com.bonitasoft.scenario.accessor.resource.Resource;
import com.bonitasoft.scenario.accessor.resource.ResourceType;

public class Extractor {
	static public Map<String, Serializable> preProcessParameters(Map<String, Serializable> parameters) {
		if (parameters == null) {
			return new HashMap<String, Serializable>();
		}

		return parameters;
	}

	static public Object getObject(Serializable parameter, String parameterName, boolean mandatory) throws FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		return parameter;
	}

	static public String getString(Serializable parameter, String parameterName, boolean mandatory) throws FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		return parameter.toString();
	}

	public static Boolean getBoolean(Serializable parameter, String parameterName, boolean mandatory) throws FunctionMandatoryParameterException, FunctionUnvalidTypeParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		try {
			return Boolean.parseBoolean(getString(parameter, parameterName, mandatory));
		} catch (Exception e) {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Boolean.class);
		}
	}

	static public Integer getInteger(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		try {
			return Integer.parseInt(getString(parameter, parameterName, mandatory));
		} catch (Exception e) {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Integer.class);
		}
	}

	static public Integer getPositiveInteger(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException, FunctionUnvalidParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		Integer value = getInteger(parameter, parameterName, mandatory);
		if (value < 0) {
			throw new FunctionUnvalidParameterException(parameterName, value.toString(), "be positive");
		}

		return value;
	}

	static public Long getLong(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		try {
			return Long.parseLong(getString(parameter, parameterName, mandatory));
		} catch (Exception e) {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Long.class);
		}
	}

	public static Operation getOperation(Serializable parameter, String parameterName, boolean mandatory) throws FunctionMandatoryParameterException, FunctionUnvalidTypeParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		try {
			return (Operation) getObject(parameter, parameterName, mandatory);
		} catch (Exception e) {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Operation.class);
		}
	}

	static public ArrayList<String> getListOfStrings(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		ArrayList<String> value = new ArrayList<String>();
		if (parameter instanceof Collection<?>) {
			Collection<Serializable> values = (Collection<Serializable>) parameter;
			for (Serializable serializable : values) {
				value.add(getString(serializable, parameterName, mandatory));
			}
		} else {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Collection.class);
		}

		return value;
	}

	static public ArrayList<Operation> getListOfOperations(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		ArrayList<Operation> value = new ArrayList<Operation>();
		if (parameter instanceof Collection<?>) {
			Collection<Serializable> values = (Collection<Serializable>) parameter;
			for (Serializable serializable : values) {
				value.add(getOperation(serializable, parameterName, mandatory));
			}
		} else {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Collection.class);
		}

		return value;
	}

	static public ArrayList<Long> getListOfLongs(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		ArrayList<Long> value = new ArrayList<Long>();
		if (parameter instanceof Collection<?>) {
			Collection<Serializable> values = (Collection<Serializable>) parameter;
			for (Serializable serializable : values) {
				value.add(getLong(serializable, parameterName, mandatory));
			}
		} else {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Collection.class);
		}

		return value;
	}

	public static List<ProcessDefinition> getListOfProcessDefinitions(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor) throws FunctionMandatoryParameterException, FunctionUnvalidParameterException, FunctionUnvalidTypeParameterException, SSessionException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		ArrayList<ProcessDefinition> value = new ArrayList<ProcessDefinition>();
		if (parameter instanceof Collection<?>) {
			Collection<Serializable> values = (Collection<Serializable>) parameter;
			for (Serializable serializable : values) {
				value.add(getProcessDefinition(serializable, parameterName, mandatory, accessor));
			}
		} else {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Collection.class);
		}

		return value;
	}

	public static HashMap<Serializable, Serializable> getMap(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		try {
			return new HashMap<Serializable, Serializable>((Map<Serializable, Serializable>) parameter);
		} catch (Exception e) {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Map.class);
		}
	}

	public static HashMap<String, Serializable> getMapOfStrings(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		try {
			return new HashMap<String, Serializable>((Map<String, Serializable>) parameter);
		} catch (Exception e) {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Map.class);
		}
	}

	static public ArrayList<Map<Serializable, Serializable>> getListOfMaps(Serializable parameter, String parameterName, boolean mandatory) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		ArrayList<Map<Serializable, Serializable>> value = new ArrayList<Map<Serializable, Serializable>>();
		if (parameter instanceof Collection<?>) {
			Collection<Serializable> values = (Collection<Serializable>) parameter;
			for (Serializable serializable : values) {
				value.add(getMap(serializable, parameterName, mandatory));
			}
		} else {
			throw new FunctionUnvalidTypeParameterException(parameterName, parameter.getClass(), Collection.class);
		}

		return value;
	}

	static public ArrayList<Map<Serializable, Serializable>> getListOfConnectorMaps(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor) throws FunctionUnvalidTypeParameterException, FunctionMandatoryParameterException, FunctionUnvalidParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		ArrayList<Map<Serializable, Serializable>> value = getListOfMaps(parameter, parameterName, mandatory);
		for (Map<Serializable, Serializable> connector : value) {
			if (!connector.containsKey(Constants.NAME) || !connector.containsKey(Constants.VERSION) || !connector.containsKey(Constants.RESOURCE_NAME)) {
				throw new FunctionUnvalidParameterException(Constants.CONNECTORS, Arrays.toString(connector.entrySet().toArray()), "provide name, version and resource name of each connector");
			}

			connector.put(Constants.NAME, getString(connector.get(Constants.NAME), Constants.CONNECTORS, true));
			connector.put(Constants.VERSION, getString(connector.get(Constants.VERSION), Constants.CONNECTORS, true));
			connector.put(Constants.RESOURCE_NAME, (byte[]) getScenarioResource(connector.get(Constants.RESOURCE_NAME), Constants.CONNECTORS, true, accessor, ResourceType.CONNECTOR_IMPLEMENTATION));
		}

		return value;
	}

	public static Object getScenarioResource(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor, ResourceType resourceType) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		String resourceName = getString(parameter, parameterName, mandatory);
		if (resourceName == null) {
			resourceName = Resource.DEFAULT_RESOURCE;
		}

		return accessor.getResource().getResource(resourceType, resourceName);
	}

	public static ProcessDefinition getProcessDefinition(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException, SSessionException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		String expectationMessage = "be a valid and existing process definition id long or Bonitasoft ProcessDefinition object";
		try {
			if (parameter instanceof Long) {
				return accessor.getDefaultProcessAPI().getProcessDefinition(getLong(parameter, parameterName, mandatory));
			} else if (parameter instanceof ProcessDefinition) {
				return (ProcessDefinition) getObject(parameter, parameterName, mandatory);
			}
		} catch (ProcessDefinitionNotFoundException e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);

		} catch (FunctionUnvalidTypeParameterException e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);

		}

		throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage);
	}

	public static ProcessInstance getProcessInstance(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor, boolean throwErrorIfArchived) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		String expectationMessage = "be an active process instance id long or Bonitasoft ProcessInstance object";
		try {
			if (parameter instanceof Long) {
				Serializable serializable = ScenarioProcessAPI.getProcessInstance(accessor, getLong(parameter, parameterName, mandatory));
				if (serializable instanceof ProcessInstance) {
					return (ProcessInstance) serializable;
				} else if (!throwErrorIfArchived) {
					return null;
				}
			} else if (parameter instanceof ProcessInstance) {
				return (ProcessInstance) getObject(parameter, parameterName, mandatory);
			}
		} catch (Exception e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);
		}

		throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage);
	}

	public static ArchivedProcessInstance getArchivedProcessInstance(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor, boolean throwErrorIfArchived) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		String expectationMessage = "be an archived process instance source object id long or Bonitasoft ArchivedProcessInstance object";
		try {
			if (parameter instanceof Long) {
				Serializable serializable = ScenarioProcessAPI.getProcessInstance(accessor, getLong(parameter, parameterName, mandatory));
				if (serializable instanceof ArchivedProcessInstance) {
					return (ArchivedProcessInstance) serializable;
				} else if (!throwErrorIfArchived) {
					return null;
				}
			} else if (parameter instanceof ArchivedProcessInstance) {
				return (ArchivedProcessInstance) getObject(parameter, parameterName, mandatory);
			}
		} catch (Exception e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);
		}

		throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage);
	}

	public static ActivityInstance getActivityInstance(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor, boolean throwErrorIfArchived) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		String expectationMessage = "be an active ativity instance id long or Bonitasoft ActivityInstance object";
		try {
			if (parameter instanceof Long) {
				Serializable serializable = ScenarioProcessAPI.getActivityInstance(accessor, getLong(parameter, parameterName, mandatory));
				if (serializable instanceof ActivityInstance) {
					return (ActivityInstance) serializable;
				} else if (!throwErrorIfArchived) {
					return null;
				}
			} else if (parameter instanceof ActivityInstance) {
				return (ActivityInstance) getObject(parameter, parameterName, mandatory);
			}
		} catch (Exception e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);
		}

		throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage);
	}

	public static ArchivedActivityInstance getArchivedActivityInstance(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor, boolean throwErrorIfArchived) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		String expectationMessage = "be an archived activity instance source object id long or Bonitasoft ArchivedActivityInstance object";
		try {
			if (parameter instanceof Long) {
				Serializable serializable = ScenarioProcessAPI.getActivityInstance(accessor, getLong(parameter, parameterName, mandatory));
				if (serializable instanceof ArchivedActivityInstance) {
					return (ArchivedActivityInstance) serializable;
				} else if (!throwErrorIfArchived) {
					return null;
				}
			} else if (parameter instanceof ArchivedActivityInstance) {
				return (ArchivedActivityInstance) getObject(parameter, parameterName, mandatory);
			}
		} catch (Exception e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);
		}

		throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage);
	}

	public static User getUser(Serializable parameter, String parameterName, boolean mandatory, Accessor accessor) throws FunctionUnvalidParameterException, FunctionMandatoryParameterException {
		checkMandatory(parameter, parameterName, mandatory);

		if (parameter == null) {
			return null;
		}

		String expectationMessage = "be a valid and existing user id long, string username string or Bonitasoft User object";
		try {
			if (parameter instanceof Long) {
				return accessor.getDefaultIdentityAPI().getUser(getLong(parameter, parameterName, mandatory));
			} else if (parameter instanceof String) {
				return accessor.getDefaultIdentityAPI().getUserByUserName(getString(parameter, parameterName, mandatory));
			} else if (parameter instanceof User) {
				return (User) getObject(parameter, parameterName, mandatory);
			}
		} catch (Exception e) {
			throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage, e);
		}

		throw new FunctionUnvalidParameterException(parameterName, parameter.toString(), expectationMessage);
	}

	public static void checkAtLeastOne(List<Serializable> parameters, List<String> parameterNames) throws FunctionMandatoryParameterException {
		for (Serializable parameter : parameters) {
			if (parameter != null) {
				return;
			}
		}

		throw new FunctionMandatoryParameterException(parameterNames);
	}

	private static void checkMandatory(Serializable parameter, String parameterName, boolean mandatory) throws FunctionMandatoryParameterException {
		if (mandatory) {
			if (parameter == null) {
				throw new FunctionMandatoryParameterException(parameterName);
			}
		}
	}
}
