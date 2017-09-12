package com.bonitasoft.scenario.accessor.ext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bonitasoft.engine.bdm.Entity;

import com.bonitasoft.bdm.jpql.query.executor.command.BDMJpqlQueryExecutorCommand;
import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.parameter.Extractor;

public class ScenarioBdmAPI {
	static public List<Entity> findList(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "findList";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		String type = Extractor.getString(parameters.get(Constants.TYPE), Constants.TYPE, true);
		String query = Extractor.getString(parameters.get(Constants.QUERY), Constants.QUERY, true);
		HashMap<Serializable, Serializable> commandParametersParameters = Extractor.getMap(parameters.get(Constants.PARAMETERS), Constants.PARAMETERS, false);
		Integer offset = Extractor.getPositiveInteger(parameters.get(Constants.OFFSET), Constants.OFFSET, false);
		Integer limit = Extractor.getPositiveInteger(parameters.get(Constants.LIMIT), Constants.LIMIT, false);
		
		accessor.log(Level.FINE, methodName + ": launch the command");
		
		// Start the process calling the command
		Map<String , Serializable> commandParameters = new HashMap<String ,Serializable>();
		commandParameters.put(BDMJpqlQueryExecutorCommand.CLASS_TYPE, type);
		commandParameters.put(BDMJpqlQueryExecutorCommand.QUERY , query);
		commandParameters.put(BDMJpqlQueryExecutorCommand.PARAMETERS, commandParametersParameters);
		commandParameters.put(BDMJpqlQueryExecutorCommand.OFFSET, offset);
		commandParameters.put(BDMJpqlQueryExecutorCommand.LIMIT, limit);
		return (List<Entity>)accessor.getDefaultCommandAPI().execute(BDMJpqlQueryExecutorCommand.NAME, commandParameters);
	}
}
