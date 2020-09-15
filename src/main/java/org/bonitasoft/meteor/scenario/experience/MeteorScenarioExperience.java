package org.bonitasoft.meteor.scenario.experience;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorAPI;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorScenario;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.MeteorToolbox;
import org.bonitasoft.meteor.scenario.process.MeteorDefProcess;
import org.json.simple.JSONValue;

/**
 * Manage the MeteorExperience
 * Two different access :
 * - via Action, the experience is build. Cases are collected, scenarii are build. Scenario is returned in a MAP, and can be saved in JSON.
 * - on start, the JSON is sent to the command. The command call the MeteorRobotExperience; All information must be contains in the JSON.
 * 
 * @author Firstname Lastname
 */
public class MeteorScenarioExperience extends MeteorScenario {

    private final static BEvent BAD_CASEIDFORMAT = new BEvent(MeteorScenarioExperience.class.getName(), 1, Level.APPLICATIONERROR, "Bad Case ID Format",
            "Case ID is not a integer",
            "This case ID is not collected to the scenario",
            "Give a correct number");

    
    private final static String CSTJSON_SCENARII = "scenarii";
    private final static String CSTJSON_EXPERIENCE = "experience";
    private final static String CSTJSON_LISTCASESID = "listCasesId";
    private final static String CSTJSON_ACTION = "action";

    
    public static class MeteorExperienceParameter {

        public String listCasesId;
        public String action;
        public String policyTimeLine = MeteorTimeLineBasic.POLICY;
        List<Map<String, Object>> scenarii = new ArrayList<>();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static MeteorExperienceParameter getInstanceFromJsonSt(final String jsonSt) {
            final MeteorExperienceParameter meteorExperienceParameter = new MeteorExperienceParameter();
            if (jsonSt == null) {
                return meteorExperienceParameter;
            }

            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);

            Map<String, Object> experience = (Map) jsonHash.get( CSTJSON_EXPERIENCE );
            meteorExperienceParameter.listCasesId = (String) experience.get( CSTJSON_LISTCASESID );
            meteorExperienceParameter.action = (String) experience.get( CSTJSON_ACTION );
            meteorExperienceParameter.scenarii = (List) experience.get( CSTJSON_SCENARII );
            if (meteorExperienceParameter.scenarii==null)
                meteorExperienceParameter.scenarii= new ArrayList<>();

            return meteorExperienceParameter;
        }

        /**
         * give a MAP containing listCaseid, scenarii
         * 
         * @param experience
         * @return
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static MeteorExperienceParameter getInstanceFromMap(final Map<String, Object> experience) {
            final MeteorExperienceParameter meteorExperienceParameter = new MeteorExperienceParameter();
            if (experience == null) {
                return meteorExperienceParameter;
            }
            meteorExperienceParameter.listCasesId = (String) experience.get( CSTJSON_LISTCASESID );
            meteorExperienceParameter.action = (String) experience.get( CSTJSON_ACTION );
            meteorExperienceParameter.scenarii = (List) experience.get( CSTJSON_SCENARII );
            if (meteorExperienceParameter.scenarii==null)
                meteorExperienceParameter.scenarii= new ArrayList<>();

            return meteorExperienceParameter;
        }

    }

    /**
     * execute an action
     * 
     * @param meteorExperienceParameter
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public Map<String, Object> action(MeteorExperienceParameter meteorExperienceParameter, ProcessAPI processAPI, IdentityAPI identityAPI) {
        List<BEvent> listEvents = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> experience = new HashMap<>();
        result.put( CSTJSON_EXPERIENCE, experience);
        experience.put(CSTJSON_SCENARII, meteorExperienceParameter.scenarii);
        List<Map<String, Object>> listScenarii = meteorExperienceParameter.scenarii;
        if ("addCasesId".equals(meteorExperienceParameter.action)) {
            String[] listCases = meteorExperienceParameter.listCasesId == null ? new String[0] : meteorExperienceParameter.listCasesId.split(",");
            for (String caseIdSt : listCases) {
                try {
                    Long caseId = Long.valueOf(caseIdSt);

                    MeteorTimeLine meteorTimeLine = MeteorTimeLine.getInstance(meteorExperienceParameter.policyTimeLine);
                    meteorTimeLine.setName("Case " + caseIdSt);
                    meteorTimeLine.setNbCases(1);
                    meteorTimeLine.setNbRobots(1);
                    List<BEvent> listEventCalculs = meteorTimeLine.calcul(caseId, processAPI, identityAPI);
                    // add it only if there are no error
                    if (!MeteorToolbox.isApplicationError(listEventCalculs))
                        listScenarii.add(meteorTimeLine.getJson());
                    listEvents.addAll(listEventCalculs);
                } catch (NumberFormatException e) {
                    listEvents.add(new BEvent(BAD_CASEIDFORMAT, "Information [" + caseIdSt + "]"));
                }
            }
        }

        result.put(MeteorAPI.CST_JSON_LISTEVENTS, BEventFactory.getSyntheticHtml(listEvents));
        return result;
    }

    private List<MeteorTimeLine> listTimeLine = new ArrayList<>();

    
    /**
     * registerInSimulation
     */
    @Override
    public List<BEvent> registerInSimulation(StartParameters startParameters, MeteorSimulation meteorSimulation, APIAccessor apiAccessor) {

        List<BEvent> listEvents = new ArrayList<>();
        MeteorExperienceParameter meteorExperienceParameter = MeteorExperienceParameter.getInstanceFromMap(startParameters.mapOfExperience);
        for (Map<String, Object> scenario : meteorExperienceParameter.scenarii) {
            MeteorTimeLine timeLine = MeteorTimeLine.getInstanceFromJson(scenario);
            if (timeLine.getNbCases() * timeLine.getNbRobots() > 0) {
                listEvents.addAll(timeLine.initialize(apiAccessor));
                listTimeLine.add(timeLine);
            }
        }
        listEvents.addAll(meteorSimulation.registerScenario(this));
        return listEvents;
    }

    @Override
    public List<MeteorRobot> generateRobots(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        List<MeteorRobot> listRobots = new ArrayList<>();
        /**
         * explode the scenarii to generate all robots
         */
        for (MeteorTimeLine meteorTimeLine : listTimeLine) {
            for (int i=0;i< meteorTimeLine.getNbRobots();i++)
                listRobots.add(new MeteorRobotExperience(meteorTimeLine, meteorSimulation, apiAccessor));

        }
        return listRobots;

    }

    @Override
    public CollectResult collectProcess(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        CollectResult collectResult = new CollectResult();
        Map<Long, MeteorDefProcess> mapProcesses = new HashMap<>();
        // multiple time lines can work on the same process
        for (MeteorTimeLine meteorTimeLine : listTimeLine) {
            if (! mapProcesses.containsKey(meteorTimeLine.getProcessDefinitionId())) {
                MeteorDefProcess meteorDefProcess = new MeteorDefProcess(meteorTimeLine.getProcessName(), meteorTimeLine.getProcessVersion(),  meteorTimeLine.getAllowRecentVersion());
                collectResult.listEvents.addAll( meteorDefProcess.initialise( apiAccessor.getProcessAPI()));
                if (meteorDefProcess.mProcessDefinitionId != null)
                    mapProcesses.put( meteorDefProcess.mProcessDefinitionId, meteorDefProcess );
            }
        }
        collectResult.listDefProcess = new ArrayList<>(mapProcesses.values());
        return collectResult;
    }

}
