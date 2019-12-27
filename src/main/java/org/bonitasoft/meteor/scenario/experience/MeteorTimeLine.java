package org.bonitasoft.meteor.scenario.experience;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;

public abstract class MeteorTimeLine {
    
    private Long rootProcessInstanceId;
    public MeteorTimeLine( Long rootProcessInstanceId ) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }
    
    
    /**
     * calcul the timeLine
     * @return
     */
    public abstract List<BEvent> calcul();
    
    
    /** return a synthesis of calculation to show to user, after calcul */
    public List<Map<String,Object>> getSynthesis() 
    {
        List<Map<String,Object>> listSynthesis = new ArrayList<Map<String,Object>>();
        
        return listSynthesis;
    }
       
    
    /* ************************************************************************ */
    /*                                                                          */
    /* Steps                                                         */
    /*                                                                          */
    /* ************************************************************************ */
    
    private List<TimeLineStep> listTimeLineSteps;
    
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

    public Long getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

}
