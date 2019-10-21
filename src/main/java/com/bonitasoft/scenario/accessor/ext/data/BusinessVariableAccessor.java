package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.business.data.MultipleBusinessDataReference;
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bonitasoft.scenario.accessor.Accessor;

public class BusinessVariableAccessor extends ReferencedVariableAccessor {
	private final static String BUSINESS_DATA_BY_IDS = "getBusinessDataByIds";
	private final static String BUSINESS_DATA_URI_PATTERN_MULTIPLE_IDS = "/API/bdm/businessData/{className}/findByIds";

	private final static String ENTITY_CLASS_NAME = "entityClassName";
	private final static String BUSINESS_DATA_IDS = "businessDataIds";
	private final static String BUSINESS_DATA_URI_PATTERN = "businessDataURIPattern";

	public BusinessVariableAccessor(Long instanceId, String name) {
		super(instanceId, name);
	}

	@Override
	public Serializable getValue(Accessor accessor) throws Exception {
		// Retrieve the execution context
		Map<String, Serializable> executionContext = getExecutionContext(accessor);

		// Retrieve the right business variable
		Serializable businessVariable = executionContext.get(name + ReferencedVariableAccessor.REF_SUFFIX);

		if (businessVariable != null) {
			// Retrieve its associated persistence id(s)
			ArrayList<Long> persistenceIds = new ArrayList<Long>();
			String boClassName = null;
			if (businessVariable instanceof SimpleBusinessDataReference) {
				SimpleBusinessDataReference simpleBusinessDataReference = (SimpleBusinessDataReference) businessVariable;
				boClassName = simpleBusinessDataReference.getType();
				name = simpleBusinessDataReference.getName();
				persistenceIds.add(simpleBusinessDataReference.getStorageId());
			} else if (businessVariable instanceof MultipleBusinessDataReference) {
				MultipleBusinessDataReference multipleBusinessDataReference = (MultipleBusinessDataReference) businessVariable;
				boClassName = multipleBusinessDataReference.getType();
				name = multipleBusinessDataReference.getName();
				persistenceIds.addAll(multipleBusinessDataReference.getStorageIds());
			} else {
				throw new Exception("The business value type is not supported by the Scenario library: " + businessVariable.getClass().getName());
			}

			// Get the business object instance(s) from the persistences id(s)
			if (!persistenceIds.isEmpty()) {
				List<JSONObject> businessVariableEntities = new ArrayList<JSONObject>();

				// Use a system command to get the valu(es) of the persistence
				// ID(s)
				final Map<String, Serializable> commandParameters = new HashMap<String, Serializable>();
				commandParameters.put(ENTITY_CLASS_NAME, boClassName);
				commandParameters.put(BUSINESS_DATA_IDS, persistenceIds);
				commandParameters.put(BUSINESS_DATA_URI_PATTERN, BUSINESS_DATA_URI_PATTERN_MULTIPLE_IDS);
				businessVariableEntities.addAll((JSONArray) new JSONParser().parse((String) accessor.getDefaultCommandAPI().execute(BUSINESS_DATA_BY_IDS, commandParameters)));

				if (businessVariableEntities.size() == 1) {
					return businessVariableEntities.get(0);
				} else if (businessVariableEntities.size() > 1) {
					return new ArrayList<JSONObject>(businessVariableEntities);
				}
			}
		}

		return null;
	}
}
