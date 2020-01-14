package org.bonitasoft.meteor.scenario.cmd;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;

/**
 * sentence
 */

public abstract class Sentence {

    public APIAccessor apiAccessor;

    public String verb;
    public Map<String, Object> mapParam;

    public Sentence(final Map<String, Object> mapParam, final APIAccessor apiAccessor) {
        this.mapParam = mapParam;
        this.apiAccessor = apiAccessor;
    }

    /**
     * <createCase processdefinitionname="pool" processdefinition="1.0" caseid=
     * "MyCaseFrancis"> <contract name"firstname" value="Francis"> <createCase>
     * <assert caseid="MyCaseFrancis" taskname="AmountTooBig"
     * performanceexpectedms="4000">
     * <variable name="amount.value" value="233" type="Integer"> <assert>
     * <executeTask caseid="MyCaseFrancis" taskname="AmountTooBig">
     * <contract name="status" value="true">
     * <contract name="comment" value="accepted"> </executeTask>
     * <executeTask caseid="MyCaseFrancis" taskname="FinalAcceptance">
     * <contract name="final" value="validated"> </executeTask>
     * <assert caseid="MyCaseFrancis" isarchived="true" performanceexpectedms=
     * "35000"/> Load Test :
     * <createCase processdefinitionname="pool" processdefinition="1.0"
     * numberofcase="1000"> <contract name="firstname" value="Francis">
     * <contract name="amount" value="10000"> <createCase>
     * <createCase processdefinitionname="pool" processdefinition="1.0"
     * numberofcase="500"> <contract name"firstname" value="Francis"> <contract
     * name"firstname" value="14000"> <createCase>
     * <executeTask taskname="AmountTooBig" numberoftask="500">
     * <contract name="status" value="true">
     * <contract name="comment" value="accepted"> </executeTask>
     * <assert isarchived="true" performanceexpectedms="35000"/>
     */

    /*
     * @param verb
     * @param listParam
     */
    public static Sentence getInstance(final String verb, final Map<String, Object> mapParam, final APIAccessor apiAccessor) {
        if (SentenceCreateCase.Verb.equalsIgnoreCase(verb)) {
            return new SentenceCreateCase(mapParam, apiAccessor);

        } else if (SentenceAssert.Verb.equalsIgnoreCase(verb)) {
            return new SentenceAssert(mapParam, apiAccessor);

        } else if (SentenceExecuteTask.Verb.equalsIgnoreCase(verb)) {
            return new SentenceExecuteTask(mapParam, apiAccessor);

        } else if (SentenceSleep.Verb.equalsIgnoreCase(verb)) {
            return new SentenceSleep(mapParam, apiAccessor);
        }
        return null;
    }

    /**
     * Decode the sentence. Any error should be collected in a event in
     * 
     * @param lineNumber
     * @return
     */
    public abstract List<BEvent> decodeSentence(int lineNumber);

    public abstract List<BEvent> execute(int robotId, LogExecution logExecution);

    protected final static String cstParamProcessName = "processName";
    protected final static String cstParamProcessVersion = "processVersion";
    protected final static String cstParamInput = "input";
    protected final static String cstParamTaskName = "taskName";
    protected final static String cstParamSleepInMs = "sleepInMs";
    protected final static String cstParamNbExecution = "nbExecution";

    /**
     * get the param
     *
     * @param index
     * @return
     */
    public String getParam(final String paramName) {
        return mapParam.get(paramName) == null ? "" : mapParam.get(paramName).toString();
    }

    /**
     * @param index
     * @return
     */
    public Long getParamLong(final String paramName, Long defaultValue) {
        final String param = getParam(paramName);
        if (param == null) {
            return defaultValue;
        }
        try {
            return Long.valueOf(param.trim());
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    public Map<String, Serializable> getJsonVariables(String paramName) {
        return mapParam.get(paramName) == null ? null : (Map<String, Serializable>) mapParam.get(paramName);
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
