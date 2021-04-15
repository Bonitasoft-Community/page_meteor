package org.bonitasoft.meteor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONValue;

/**
	 *
	 *
	 */
public class MeteorStartParameters {

    static Logger logger = Logger.getLogger(MeteorStartParameters.class.getName());
    static String logHeader = "MeteorStartParameters ~~ ";

    private static final String CST_BLANCKLINE = "                                                                                                      ";

    private MeteorConst.EXECUTIONMODE executionMode;
    private String scenarioName;
    private long timeMaxInMs;

    private long tenantId;

    // collect all information, from the JSON. The interpretation will be
    // done in MeteorOperation.start()
    private List<Map<String, Object>> listOfProcesses;
    private List<Map<String, Object>> listOfScenarii;
    /**
     * MapOfExperience contains
     * {
     * "listCasesId": "1003",
     * "scenarii": [
     * {
     * "processname": "experience",
     * "processversion": "1.0",
     * "nbcases": 1,
     * "nbrobs": 1,
     * "timelines": [ ...
     */
    private Map<String, Object> mapOfExperience;

    public static MeteorStartParameters getInstanceFromJsonSt(final String jsonSt) {
        final MeteorStartParameters startParameters = new MeteorStartParameters();
        startParameters.decodeFromJsonSt(jsonSt);
        return startParameters;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public void decodeFromJsonSt(String jsonSt) {
        logger.fine(logHeader + "decodeFromJsonSt : JsonSt[" + jsonSt + "]");
        listOfProcesses = new ArrayList<>();
        listOfScenarii = new ArrayList<>();
        mapOfExperience = new HashMap<>();

        if (jsonSt == null) {
            return;
        }

        // we can get 2 type of JSON :
        // { 'processes' : [ {..}, {...} ], 'scenarii':[{...}, {...},
        // 'process' : {..}, 'scenario': {} ] }
        // or a list of order
        // [ { 'process': {}, 'process': {}, 'scenario': {..}];

        final Object jsonObject = JSONValue.parse(jsonSt);
        logger.fine(logHeader + "MeteorAPI.decodeFromJsonSt : line object [" + (jsonObject == null ? "null" : jsonObject.getClass().getName()) + "] Map ? " + (jsonObject instanceof HashMap) + " line=[" + jsonSt + "] ");

        if (jsonObject instanceof HashMap) {
            logger.fine(logHeader + "MeteorAPI.decodeFromJsonSt : object [" + jsonObject.getClass().getName() + "] is a HASHMAP");

            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) jsonObject;
            String modeSt = (String) jsonHash.get(MeteorAPI.CSTJSON_MODE);
            if (modeSt != null)
                executionMode = MeteorConst.EXECUTIONMODE.valueOf(modeSt.toUpperCase());
            scenarioName = (String) jsonHash.get(MeteorAPI.CSTJSON_SCENARIONAME);
            timeMaxInMs = MeteorToolbox.getParameterLong(jsonHash, MeteorAPI.CSTJSON_TIMEMAXINMS, 0L);

            if (jsonHash.get(MeteorAPI.CSTJSON_PROCESSES) instanceof Map) {
                final Map<String, Object> jsonHashProcess = (Map<String, Object>) jsonHash.get(MeteorAPI.CSTJSON_PROCESSES);
                if (jsonHashProcess.get("scenarii") != null)
                    listOfProcesses.addAll((List<Map<String, Object>>) jsonHashProcess.get("scenarii"));
            }

            if (jsonHash.get("scenarii") instanceof Map) {
                final Map<String, Object> jsonHashScenarii = (Map<String, Object>) jsonHash.get("scenarii");
                if (jsonHashScenarii.get("actions") != null)
                    listOfScenarii.addAll((List<Map<String, Object>>) jsonHashScenarii.get("actions"));
            }

            if (jsonHash.get("experience") instanceof Map) {
                mapOfExperience = (Map<String, Object>) jsonHash.get("experience");
            }
        } else if (jsonObject instanceof List) {
            logger.fine(logHeader + "MeteorAPI.decodeFromJsonSt : object [" + jsonObject.getClass().getName() + "] is a LIST");
            final List<Map<String, Map<String, Object>>> jsonList = (List<Map<String, Map<String, Object>>>) jsonObject;
            for (final Map<String, Map<String, Object>> oneRecord : jsonList) {
                logger.fine(logHeader + "MeteorAPI.decodeFromJsonSt : process [" + oneRecord.get("process") + "] scenario [" + oneRecord.get(MeteorConst.CSTJSON_SCENARIO) + "]");

                if (oneRecord.containsKey("process")) {
                    listOfProcesses.add(oneRecord.get("process"));
                }
                if (oneRecord.containsKey(MeteorConst.CSTJSON_SCENARIO)) {
                    listOfScenarii.add(oneRecord.get(MeteorConst.CSTJSON_SCENARIO));
                }
            }
        }

        logger.fine(logHeader + "MeteorAPI.decodeFromJsonSt :  decodeFromJsonSt nbProcess=" + listOfProcesses.size() + " nbScenarii=" + listOfScenarii.size());
    }

    public String toJson() {
        Map<String, Object> jsonHash = new HashMap<>();
        
        jsonHash.put(MeteorAPI.CSTJSON_MODE, executionMode.toString());
        jsonHash.put(MeteorAPI.CSTJSON_SCENARIONAME, scenarioName);
        jsonHash.put( MeteorAPI.CSTJSON_TIMEMAXINMS, timeMaxInMs);

        Map<String, Object> jsonHashProcess = new HashMap<>();
        jsonHash.put(MeteorAPI.CSTJSON_PROCESSES,jsonHashProcess);
        
        jsonHashProcess.put("scenarii",listOfProcesses);
        
        
        Map<String, Object> jsonHashScenarii = new HashMap<>();
        jsonHash.put("scenarii",jsonHashScenarii);
        jsonHashScenarii.put("actions", listOfScenarii);

        jsonHash.put("experience",mapOfExperience);
        

        return JSONValue.toJSONString(jsonHash);
    }

    @Override
    public String toString() {
        String json = toJson();
        return "startParameters "+ (json.length()>20 ? json.substring(0,20) : json);
    }

    /**
     * All getter / setter
     * 
     * @return
     */
    public MeteorConst.EXECUTIONMODE getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(MeteorConst.EXECUTIONMODE executionMode) {
        this.executionMode = executionMode;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public long getTimeMaxInMs() {
        return timeMaxInMs;
    }

    public void setTimeMaxInMs(long timeMaxInMs) {
        this.timeMaxInMs = timeMaxInMs;
    }

    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    public List<Map<String, Object>> getListOfProcesses() {
        return listOfProcesses;
    }

    public void setListOfProcesses(List<Map<String, Object>> listOfProcesses) {
        this.listOfProcesses = listOfProcesses;
    }

    public List<Map<String, Object>> getListOfScenarii() {
        return listOfScenarii;
    }

    public void setListOfScenarii(List<Map<String, Object>> listOfScenarii) {
        this.listOfScenarii = listOfScenarii;
    }

    public Map<String, Object> getMapOfExperience() {
        return mapOfExperience;
    }

    public void setMapOfExperience(Map<String, Object> mapOfExperience) {
        this.mapOfExperience = mapOfExperience;
    }
}
