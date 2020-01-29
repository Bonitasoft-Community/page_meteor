package org.bonitasoft.meteor.scenario.experience;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.casedetails.CaseContract;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.scenario.experience.MeteorTimeLine.TimeLineStep;

public class MeteorRobotExperience extends MeteorRobot {

    private APIAccessor apiAccessor = null;
    private MeteorSimulation meteorSimulation;
    private MeteorTimeLine meteorTimeLine;

    private List<Long> mListProcessInstanceCreated = new ArrayList<Long>();

    public MeteorRobotExperience(MeteorTimeLine meteorTimeLine, MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        super(meteorSimulation, apiAccessor);
        this.meteorTimeLine = meteorTimeLine;
        this.apiAccessor = apiAccessor;
        this.meteorSimulation = meteorSimulation;

    }

    @Override
    public void executeRobot() {
        mCollectPerformance.mTitle = "EXECUTE EXPERIENCE1: " + meteorTimeLine.getName() + " #" + getRobotId();
        setSignatureInfo("Experience "+ meteorTimeLine.getName());

        // find a userId to assign task
        Long userId = findUserId();
        
        mCollectPerformance.mOperationTotal = (meteorTimeLine.getListTimeLineSteps().size() + 1) * meteorTimeLine.getNbCases();
        ProcessAPI processAPI = apiAccessor.getProcessAPI();
        Map<String, Serializable> mapContract=null;
        try {
            mapContract = CaseContract.recalculateContractValue( meteorTimeLine.getListContractValues());

            for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < meteorTimeLine.getNbCases(); mCollectPerformance.mOperationIndex++) {
                long timeStart = System.currentTimeMillis();
                ProcessInstance processInstance = processAPI.startProcessWithInputs(meteorTimeLine.getProcessDefinitionId(), mapContract);
                mLogExecution.addLog("Case:" + String.valueOf(processInstance.getId()));
                mListProcessInstanceCreated.add( processInstance.getId() );
                
                mCollectPerformance.collectOneStep(System.currentTimeMillis() - timeStart);
                // execute the steps now
                for (int i = 0; i < meteorTimeLine.getListTimeLineSteps().size(); i++) {
                    TimeLineStep timeLine = meteorTimeLine.getListTimeLineSteps().get(i);
                    try {
                        Thread.sleep(timeLine.timeWaitms);
                    } catch (InterruptedException e) {
                    }
                    timeStart = System.currentTimeMillis();
                    // now search the tasks 
                    SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 100);
                    sob.filter(ActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstance.getId());
                    sob.filter(ActivityInstanceSearchDescriptor.NAME, timeLine.activityName);
                    HumanTaskInstance foundHumanTask = null;
                    int count = 0;
                    while (count < 100 && foundHumanTask == null) {

                        count++;
                        SearchResult<HumanTaskInstance> searchHumanTask = processAPI.searchHumanTaskInstances(sob.done());
                        if (searchHumanTask.getCount() == 0) {
                            if (meteorSimulation.getDurationOfSimulation() != null && System.currentTimeMillis() > meteorSimulation.getDurationOfSimulation()) {
                                return;
                            }

                            Thread.sleep(1000);
                        } else
                            foundHumanTask = searchHumanTask.getResult().get(0);
                    } // end search humanTasks 
                    if (foundHumanTask == null) {
                        // error 
                    } else {
                        // execute it
                        Map<String, Serializable> mapContractTask = CaseContract.recalculateContractValue(timeLine.listContractValues);

                        try {
                            processAPI.assignUserTask(foundHumanTask.getId(), userId);
                            processAPI.executeUserTask( foundHumanTask.getId(), mapContractTask);
                        } catch (Exception e) {
                            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, "ProcessDefinition[" + meteorTimeLine.getProcessDefinitionId() + "] ActivityName[" + foundHumanTask.getName() + "] "));
                        }
                    }
                    mCollectPerformance.collectOneStep( System.currentTimeMillis() - timeStart);
                } // end execute a timeLine 
            } // end execute nbCases
        } catch (ContractViolationException vc) {
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, "ProcessDefinition[" + meteorTimeLine.getProcessDefinitionId() + "] "));
            return;
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + getSignature() + " exception " + e.toString() + " at " + sw.toString());
            // not yet logged ? Add in the logExecution
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "]"));

        }
    }
    public List<Long> getListProcessInstanceCreated() {
        return mListProcessInstanceCreated;
    }
    
    public MeteorTimeLine getMeteorTimeLine() {
        return meteorTimeLine;
    }
    private Long findUserId() {
        IdentityAPI identityAPI = apiAccessor.getIdentityAPI();
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0,5);
        sob.filter(UserSearchDescriptor.ENABLED, "true");
        SearchResult<User> searchUser;
        try {
            searchUser = identityAPI.searchUsers( sob.done());
            if (searchUser.getCount()>0)
                return searchUser.getResult().get(0).getId();
        } catch (SearchException e) {
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "Robot #:[" + getSignature() + "] can't find a user "+e.getMessage()));

        }
        return null;
        
    }
}
