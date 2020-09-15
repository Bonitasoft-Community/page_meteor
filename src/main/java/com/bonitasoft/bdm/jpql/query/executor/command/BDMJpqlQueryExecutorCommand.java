package com.bonitasoft.bdm.jpql.query.executor.command;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.command.system.CommandWithParameters;
import org.bonitasoft.engine.service.TenantServiceAccessor;

public class BDMJpqlQueryExecutorCommand extends CommandWithParameters {

    public static final String NAME = BDMJpqlQueryExecutorCommand.class.getSimpleName();
    public static final String SUMMARY = "Provide a complete function to run JPQL queries that return a list of elements on a specific BO";

    public static final String CLASS_TYPE = "classType";
    public static final String QUERY = "query";
    public static final String PARAMETERS = "parameters";
    public static final String OFFSET = "offset";
    public static final String LIMIT = "limit";

    public Serializable execute(final Map<String, Serializable> parameters, final TenantServiceAccessor serviceAccessor) throws SCommandParameterizationException, SCommandExecutionException {
        try {
            // Mandatory
            Class classType = Class.forName(getClassType(parameters));
            String query = getQuery(parameters);

            // Optional
            Map<String, Serializable> queryParameters = (parameters.get(PARAMETERS) != null ? new HashMap<String, Serializable>((Map<String, Serializable>) parameters.get(PARAMETERS)) : new HashMap<String, Serializable>());
            int offset = (parameters.get(OFFSET) != null ? (Integer) parameters.get(OFFSET) : 0);
            int limit = (parameters.get(LIMIT) != null ? (Integer) parameters.get(LIMIT) : Integer.MAX_VALUE);

            return new ArrayList(serviceAccessor.getBusinessDataRepository().findList(classType, query, queryParameters, offset, limit));
        } catch (final Exception e) {
            throw new SCommandExecutionException(NAME + " Error", e);
        }
    }

    private String getClassType(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getStringMandadoryParameter(parameters, CLASS_TYPE);
    }

    private String getQuery(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getStringMandadoryParameter(parameters, QUERY);
    }
}
