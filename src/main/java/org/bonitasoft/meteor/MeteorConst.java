package org.bonitasoft.meteor;


public class MeteorConst {

    public enum EXECUTIONMODE { CLASSIC, UNITTEST }

    public enum SIMULATIONSTATUS {
        DEFINITION, NOROBOT, STARTING, STARTED, FINALISATION, DONE,NOSCENARIO, NOSIMULATION, FAILEDUNITTEST,SUCCESSUNITTEST, COMPLETEEXECUTION, INCOMPLETEEXECUTION
    }

    /**
     * READYTOSTART : ready to go, not started yet
     * @author Firstname Lastname
     *
     */
    public enum ROBOTSTATUS {
        READYTOSTART, STARTING, FAILSTART, STARTED, DONE, INCOMPLETEEXECUTION, FAIL, KILLED
    }

    public static final String CSTJSON_PERCENTUNITTEST =  "percentunittest";
    /**
     * contains value of MeteorSimulation.STATUS
     */
    
    public static final String CSTJSON_STATUS = "status";
    public static final String CSTJSON_ID = "id";
    public static final String CSTJSON_SIMULATIONID = "simulationid";
    public static final String CSTJSON_PERCENTADVANCE = "percentAdvance";
    public static final String CSTJSON_TIMEESTIMATEDDELAY = "timeEstimatedDelay";
    public static final String CSTJSON_TIMEESTIMATEDEND = "timeEstimatedEnd";
    public static final String CSTJSON_GLOBALSTATUS = "status";
    public static final String CSTJSON_NBERRORS = "nbErrors";
    public static final String CSTJSON_ROBOTS = "robots";
    public static final String CST_JSON_TOTAL = "total";
    public static final String CSTJSON_TIME_ENDED = "timeEnded";
    public static final String CSTJSON_COVER = "cover";
    public static final String CSTJSON_TIME_STARTED = "timeStarted";
    public static final String CSTJSON_MEM_PERCENT = "memPercent";
    public static final String CSTJSON_MEM_TOTAL_MB = "memTotalMb";
    public static final String CSTJSON_MEM_USED_MB = "memUsedMb";
    public static final String CSTJSON_ARMTIMER = "armtimer";

    public static final String CSTJSON_ROBOTTITLE = "title";
    public static final String CSTJSON_ROBOTSTATUS = "status";

    public static final String CSTJSON_ROBOTNAME = "name";
    public static final String CSTJSON_SCENARIO = "scenario";

    
}
