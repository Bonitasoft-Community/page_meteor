package org.bonitasoft.meteor.scenario.experience;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorAPI;
import org.bonitasoft.tools.Process.CaseDetailsAPI;
import org.json.simple.JSONValue;

/**
 * Manage the MeteorExperience
 * Two different access :
 * - via Action, the experience is build. Cases are collected, scenarii are build. Scenario is returned in a MAP, and can be saved in JSON.
 * - on start, the JSON is sent to the command. The command call the MeteorRobotExperience; All information must be contains in the JSON.
 * 
 * @author Firstname Lastname
 */
public class MeteorExperience {

    private final static BEvent BAD_CASEIDFORMAT = new BEvent(MeteorExperience.class.getName(), 1, Level.APPLICATIONERROR, "Bad Case ID Format", 
            "Case ID is not a integer",
            "This case ID is not collected to the scenario",
            "Give a correct number");

    public static class MeteorExperienceParameter {

        public String listCasesId;
        public String action;
        public String policyTimeLine = MeteorTimeLineBasic.policy;
        List<Map<String, Object>> scenarii;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static MeteorExperienceParameter getInstanceFromJsonSt(final String jsonSt) {
            final MeteorExperienceParameter meteorExperienceParameter = new MeteorExperienceParameter();
            if (jsonSt == null) {
                return meteorExperienceParameter;
            }
            
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
            
            Map<String,Object> experience = (Map) jsonHash.get("experience");
            meteorExperienceParameter.listCasesId = (String) experience.get("listCasesId");
            meteorExperienceParameter.action = (String) experience.get("action");
            meteorExperienceParameter.scenarii = (List) experience.get("scenarii");

            return meteorExperienceParameter;
        }

    }

    public Map<String, Object> action(MeteorExperienceParameter meteorExperienceParameter, ProcessAPI processAPI,IdentityAPI identityAPI) {
        List<BEvent> listEvents = new ArrayList<BEvent>();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String,Object> experience =new HashMap<String, Object>();
        result.put("experience", experience);
        experience.put("scenarii", meteorExperienceParameter.scenarii);
        List<Map<String, Object>> listScenarii = meteorExperienceParameter.scenarii;
        if ("addCasesId".equals(meteorExperienceParameter.action)) {
            String[] listCases = meteorExperienceParameter.listCasesId == null ? new String[0] : meteorExperienceParameter.listCasesId.split(",");
            for (String caseIdSt : listCases) {
                try
                {
                    Long caseId = Long.valueOf(caseIdSt);
                
                    MeteorTimeLine meteorTimeLine = MeteorTimeLine.getInstance(meteorExperienceParameter.policyTimeLine);
                    meteorTimeLine.setName("Case "+caseIdSt);
                    List<BEvent> listEventCalculs = meteorTimeLine.calcul(Long.valueOf(caseId), processAPI, identityAPI);
                    // add it only if there are no error
                    if (! isApplicationError( listEventCalculs ) )
                        listScenarii.add(meteorTimeLine.getJson());
                    listEvents.addAll(listEventCalculs);
                }
                catch(NumberFormatException e) {
                    listEvents.add( new BEvent(BAD_CASEIDFORMAT, "Information ["+caseIdSt+"]")); 
                }
            }
        }

        result.put(MeteorAPI.cstJsonListEvents, BEventFactory.getSyntheticHtml( listEvents )) ;
        return result;
    }
    
    public boolean isApplicationError(final List<BEvent> listEvents) {
        for (final BEvent event : listEvents) {
            if (event.isError() || event.getLevel().equals( Level.APPLICATIONERROR)) {
                return true;
            }
        }
        return false;
    }

}
