package com.bonitasoft.custompage.meteor.scenario.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;

/**
 * sentence
 */

public abstract class Sentence {

    public APIAccessor apiAccessor;

    public String verb;
    public List<String> listParams = new ArrayList<String>();

    public Sentence(final List<String> listParams, final APIAccessor apiAccessor)
    {
        this.listParams = listParams;
        this.apiAccessor = apiAccessor;
    }
    /**
     * <createCase processdefinitionname="pool" processdefinition="1.0" caseid="MyCaseFrancis">
     * <contract name"firstname" value="Francis">
     * <createCase>
     * <assert caseid="MyCaseFrancis" taskname="AmountTooBig" performanceexpectedms="4000">
     * <variable name="amount.value" value="233" type="Integer">
     * <assert>
     * <executeTask caseid="MyCaseFrancis" taskname="AmountTooBig">
     * <contract name="status" value="true">
     * <contract name="comment" value="accepted">
     * </executeTask>
     * <executeTask caseid="MyCaseFrancis" taskname="FinalAcceptance">
     * <contract name="final" value="validated">
     * </executeTask>
     * <assert caseid="MyCaseFrancis" isarchived="true" performanceexpectedms="35000"/>
     * Load Test :
     * <createCase processdefinitionname="pool" processdefinition="1.0" numberofcase="1000">
     * <contract name="firstname" value="Francis">
     * <contract name="amount" value="10000">
     * <createCase>
     * <createCase processdefinitionname="pool" processdefinition="1.0" numberofcase="500">
     * <contract name"firstname" value="Francis">
     * <contract name"firstname" value="14000">
     * <createCase>
     * <executeTask taskname="AmountTooBig" numberoftask="500">
     * <contract name="status" value="true">
     * <contract name="comment" value="accepted">
     * </executeTask>
     * <assert isarchived="true" performanceexpectedms="35000"/>
     */

    /*
     * @param verb
     * @param listParam
     */
    public static Sentence getInstance(final String verb, final List<String> listParam, final APIAccessor apiAccessor) {
        if (SentenceCreateCase.Verb.equalsIgnoreCase(verb)) {
            return new SentenceCreateCase(listParam, apiAccessor);

        } else if (SentenceAssert.Verb.equalsIgnoreCase(verb)) {
            return new SentenceAssert(listParam, apiAccessor);

        } else if (SentenceExecuteTask.Verb.equalsIgnoreCase(verb)) {
            return new SentenceExecuteTask(listParam, apiAccessor);
        }
        return null;
    }

    public abstract List<BEvent> decodeSentence();

    public abstract List<BEvent> execute();

    /**
     * get the param
     *
     * @param index
     * @return
     */
    public String getParam(final int index)
    {
        if (index < listParams.size()) {
            return listParams.get(index);
        }
        return null;
    }

    /**
     * @param index
     * @return
     */
    public Long getParamLong( final int index)
    {
        final String param = getParam(index);
        if (param==null) {
            return null;
        }
        try
        {
            return Long.valueOf(param);
        }
        catch(final Exception e)
        {
            return null;
        }
    }


    /**
     * get the Map
     *
     * @param index
     * @return
     */
    public Map<String, Object> getMapVariables(final int index)
    {
        final Map<String, Object> variables = new HashMap<String, Object>();

        if (listParams.size() > index)
        {
            for (int i = index; i < listParams.size(); i++)
            {
                final String param = listParams.get(i);
                final int indParam = param.indexOf("=");
                if (indParam != -1)
                {
                    variables.put(param.substring(0, indParam), param.substring(indParam + 1));
                }
            }
        }
        return variables;
    }
    /**
     * by a string like ['meteor me'] return [meteor me]
     *
     * @param param
     * @return
     */
    public String removeQuote(String param) {
        if (param == null) {
            return null;
        }
        param = param.trim();
        if (param.startsWith("'")) {
            param = param.substring(1);
        }
        if (param.endsWith("'")) {
            param = param.substring(0, param.length() - 1);
        }
        return param;

    }

}
