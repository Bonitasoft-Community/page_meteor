package org.bonitasoft.meteor.scenario.experience;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorToolbox;
import org.json.simple.JSONValue;

public abstract class MeteorTimeLine {
    
    
    private List<TimeLineStep> listTimeLineSteps = new ArrayList<TimeLineStep>();
    private Long rootCaseId;
    private String name;
    private String processName;
    private String processVersion;
    private long nbRobots;
    private long nbCases;
    
    List<Map<String, Serializable>> listContractValues;

    
    private static String cstJsonName ="name";
    private static String cstJsonTimeLinePolicy = "policy";
    private static String cstJsonRootCaseId ="rootcaseid";
    private static String cstJsonProcessName ="processname";
    private static String cstJsonProcessVersion ="processversion";
    private static String cstJsonTimeLines ="timelines";
    private static String cstJsonActivityName ="actname";
    private static String cstJsonActivityDefinitionId ="defid";
    private static String cstJsonTimeWaitBefore ="timeWaitms";
    private static String cstJsonListContractValues ="contract";
    private static String cstJsonnbcases ="nbcases";
    private static String cstJsonnbrobs ="nbrobs";
    private static String cstJsonTimelineMS ="timeLineMs";
    private static String cstJsonTimeFromBeginingMS ="timeLineFromBeginningMS";
    
    
    
    public MeteorTimeLine() {
        ;
    }
    
    
    /**
     * calcul the timeLine
     * @return
     */
    public abstract List<BEvent> calcul( Long rootCaseId, ProcessAPI processAPI,IdentityAPI identityAPI);
    
    public abstract String getPolicy();
    
    /** return a synthesis of calculation to show to user, after calculation */
    public Map<String,Object> getJson() 
    {
        Map<String,Object> synthesis = new HashMap<String,Object>();
        synthesis.put( cstJsonName,  name );
        synthesis.put( cstJsonRootCaseId,  rootCaseId );
        synthesis.put( cstJsonProcessName,  processName );
        synthesis.put( cstJsonProcessVersion,  processVersion );
        synthesis.put( cstJsonListContractValues,listContractValues==null ? null :  JSONValue.toJSONString(listContractValues));
        synthesis.put( cstJsonnbrobs, nbRobots);
        synthesis.put( cstJsonnbcases, nbCases);
        
        synthesis.put( cstJsonTimeLinePolicy,  getPolicy() );
        List<Map<String,Object>> listTimeLine = new ArrayList<Map<String,Object>>();
        synthesis.put( cstJsonTimeLines, listTimeLine);
        for (TimeLineStep timeLineStep : listTimeLineSteps) {
            Map<String,Object> timeLineMap = new HashMap<String,Object>();
            timeLineMap.put( cstJsonActivityName, timeLineStep.activityName);
            timeLineMap.put( cstJsonActivityDefinitionId, timeLineStep.sourceActivityDefinitionId);
            timeLineMap.put( cstJsonTimeWaitBefore, timeLineStep.timeToWaitBeforeInms);
            timeLineMap.put( cstJsonListContractValues, timeLineStep.listContractValues==null ? null : JSONValue.toJSONString(timeLineStep.listContractValues));
            timeLineMap.put( cstJsonTimelineMS, timeLineStep.timelinems);
            timeLineMap.put( cstJsonTimeFromBeginingMS, timeLineStep.timeFromBeginingms);
            listTimeLine.add( timeLineMap );
        }
        
        
        return synthesis;
    }
    @SuppressWarnings("unchecked")
    public static MeteorTimeLine getInstanceFromJson(Map<String,Object> json)
    {
        String policy = MeteorToolbox.getParameterString(json, cstJsonTimeLinePolicy, "");
        MeteorTimeLine  timeLine = MeteorTimeLine.getInstance( policy );

        timeLine.name= MeteorToolbox.getParameterString(json, cstJsonName, "");
        timeLine.rootCaseId= MeteorToolbox.getParameterLong(json, cstJsonRootCaseId, null);
        timeLine.processName  = MeteorToolbox.getParameterString(json, cstJsonProcessName, "");
        timeLine.processVersion  = MeteorToolbox.getParameterString(json, cstJsonProcessVersion,  "" );
        String jsonContract =  MeteorToolbox.getParameterString( json, cstJsonListContractValues, "");
        Object tempList = JSONValue.parse( jsonContract );           
        timeLine.listContractValues = ( List<Map<String, Serializable>>)  (List<?>) tempList;
        timeLine.nbRobots = MeteorToolbox.getParameterLong(json, cstJsonnbrobs, 0L);
        timeLine.nbCases = MeteorToolbox.getParameterLong(json, cstJsonnbcases, 0L);
    
        
        List<Object> listTimeLine =  MeteorToolbox.getParameterList(json,cstJsonTimeLines, new ArrayList<Object>());
        for (Object timeLineMap :  listTimeLine) {
            TimeLineStep timeLineStep = timeLine.addOneStep();
            timeLineStep.activityName =  MeteorToolbox.getParameterString( (Map<String,Object>)timeLineMap, cstJsonActivityName, "");
            timeLineStep.sourceActivityDefinitionId =  MeteorToolbox.getParameterLong( (Map<String,Object>)timeLineMap, cstJsonActivityDefinitionId, null);
            timeLineStep.timeToWaitBeforeInms =  MeteorToolbox.getParameterLong((Map<String,Object>) timeLineMap, cstJsonTimeWaitBefore, 0L);
            jsonContract =  MeteorToolbox.getParameterString( (Map<String,Object>)timeLineMap, cstJsonListContractValues, "");
            tempList = JSONValue.parse( jsonContract );           
            timeLineStep.listContractValues = ( List<Map<String, Serializable>>)  (List<?>) tempList;
            
        }
        return timeLine;
    }
    
    public static MeteorTimeLine getInstance(String policy) {
        if (policy.equals(MeteorTimeLineBasic.policy))
            return new MeteorTimeLineBasic();
        return null;
    }
    
    
    /* ************************************************************************ */
    /*                                                                          */
    /* getter/setter                                                         */
    /*                                                                          */
    /* ************************************************************************ */
    
    public Long getRootCaseId() {
        return rootCaseId;
    }
    public void setRootCaseId(Long rootCaseId) {
        this.rootCaseId = rootCaseId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }


    public String getProcessName() {
        return processName;
    }


    
    public void setProcessName(String processName) {
        this.processName = processName;
    }


    
    public String getProcessVersion() {
        return processVersion;
    }


    
    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }


    
    public List<Map<String, Serializable>> getListContractValues() {
        return listContractValues;
    }


    
    public void setListContractValues(List<Map<String, Serializable>> listContractValues) {
        this.listContractValues = listContractValues;
    }
    /* ************************************************************************ */
    /*                                                                          */
    /* Steps                                                         */
    /*                                                                          */
    /* ************************************************************************ */
  
    


    public TimeLineStep addOneStep()
    {
        TimeLineStep timeLineActivity = new TimeLineStep();
        listTimeLineSteps.add( timeLineActivity );
        return timeLineActivity;
    }
    public List<TimeLineStep> getListTimeLineSteps() {
        return listTimeLineSteps;
    }

    /** timeLineStep 
     * 
     * @author Firstname Lastname
     *
     */
    public static class TimeLineStep {
        String activityName;
        
        /**
         * Identifiant of this activity
         */
        Long sourceActivityDefinitionId;
        
        /** Date where the time line is executed
         * 
         */
        
        Long timelinems;
        
        Long timeFromBeginingms;
        /**
         * time to wait before searching for this steps
         */
        long timeToWaitBeforeInms =0;
        /**
         * contract to execute this activity
         */
        List<Map<String, Serializable>> listContractValues;
    }
   
    
    
    /* ************************************************************************ */
    /*                                                                          */
    /* main information                                                         */
    /*                                                                          */
    /* ************************************************************************ */

   
}
