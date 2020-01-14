package org.bonitasoft.meteor.scenario.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.SessionNotFoundException;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;

/**
 * this class is not used at this moment
 * Idea is to connect as a USER and see all different task pending for this user.
 * 
 * @author Firstname Lastname
 */
public class MeteorRobotUserActivity extends MeteorRobot {

    // MeteorProcessDefinitionUser mMeteorUser;

    Long mTimeEndOfTheSimulationInMs = null;

    protected MeteorRobotUserActivity(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        super(meteorSimulation, apiAccessor);
    }

    /*
     * public void setParameters(final MeteorProcessDefinitionUser meteorUser) {
     * mMeteorUser = meteorUser;
     * }
     */

    @Override
    public void executeRobot() {
        LoginAPI loginAPI = null;
        APISession userActivityApiSession = null;
        try {
            //mCollectPerformance.mTitle = "USER:" + mMeteorUser.mUserName + " #" + mRobotId;
            //mCollectPerformance.mOperationTotal = mMeteorUser.mNumberOfCase;
            loginAPI = TenantAPIAccessor.getLoginAPI();
            userActivityApiSession = loginAPI.login("walter.bates", "bpm" /* mMeteorUser.mUserName, mMeteorUser.mUserPassword */ );
            // else
            // userActivityApiSession = apiSession;
            final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(userActivityApiSession);

            // ProcessAPI processAPI =
            // TenantAPIAccessor.getProcessAPI(apiSession);
            final long userId = userActivityApiSession.getUserId();
            mCollectPerformance.mOperationIndex = 0;
            while (mCollectPerformance.mOperationIndex < 10 /* mMeteorUser.mNumberOfCase */) {
                // -------------------------------------- getPendingTask

                // List<HumanTaskInstance> listResult =
                // processAPI.getPendingHumanTaskInstances(userId, 0,
                // 10, ActivityInstanceCriterion.EXPECTED_END_DATE_ASC);

                final SearchOptionsBuilder searchOptionBuilder = new SearchOptionsBuilder(0, 20);
                // searchOptionBuilder.filter(HumanTaskInstanceSearchDescriptor.NAME,
                // activityName);
                searchOptionBuilder.filter(HumanTaskInstanceSearchDescriptor.PROCESS_DEFINITION_ID, 100 /* mMeteorUser.mProcessDefinitionId */ );

                long timeStart = System.currentTimeMillis();
                final SearchResult<HumanTaskInstance> searchHumanTaskInstances = processAPI.searchPendingTasksForUser(userId, searchOptionBuilder.done());
                final List<HumanTaskInstance> pendingTasks = searchHumanTaskInstances.getResult();
                long timeEnd = System.currentTimeMillis();
                mCollectPerformance.collectOneStep(timeEnd - timeStart);

                HumanTaskInstance pendingTask = pendingTasks.size() > 0 ? pendingTasks.get(0) : null;
                if (pendingTask != null && pendingTask.getProcessDefinitionId() != 100 /* mMeteorUser.mProcessDefinitionId */) {
                    pendingTask = null;
                }
                if (pendingTask != null) { // assign the task to the
                                           // user
                    timeStart = System.currentTimeMillis();
                    processAPI.assignUserTask(pendingTask.getId(), userId);
                    timeEnd = System.currentTimeMillis();
                    mCollectPerformance.collectOneStep(timeEnd - timeStart);

                    // update variable
                    /*
                     * for (final String variableName : mMeteorUser.mVariables.keySet()) {
                     * processAPI.updateActivityDataInstance(variableName, pendingTask.getId(), mMeteorUser.mVariables.get(variableName) == null ? null :
                     * mMeteorUser.mVariables.get(variableName).toString());
                     * }
                     * // add documents
                     * for (final MeteorDocument meteorDocument : mMeteorUser.mListDocuments) {
                     * processAPI.attachDocument(pendingTask.getId(), meteorDocument.mDocumentName, "TheFileName", "application/pdf",
                     * meteorDocument.mContent.toByteArray());
                     * }
                     */
                    // execute the task
                    timeStart = System.currentTimeMillis();
                    processAPI.executeFlowNode(pendingTask.getId());
                    timeEnd = System.currentTimeMillis();
                    mCollectPerformance.collectOneStep(timeEnd - timeStart);

                    mCollectPerformance.mOperationIndex++;
                }
                // Thread.sleep(mMeteorUser.mTimeSleep);
                if (mTimeEndOfTheSimulationInMs != null && mTimeEndOfTheSimulationInMs != 0) {
                    if (System.currentTimeMillis() > mTimeEndOfTheSimulationInMs) {
                        return;
                    }
                }

            } // end loop
        } catch (final LoginException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
        } catch (final UpdateException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
        } catch (final FlowNodeExecutionException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
        } catch (final SearchException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
            /*
             * } catch (final ProcessInstanceNotFoundException e) {
             * final StringWriter sw = new StringWriter();
             * e.printStackTrace(new PrintWriter(sw));
             * logger.severe("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
             * } catch (final DocumentAttachmentException e) {
             * final StringWriter sw = new StringWriter();
             * e.printStackTrace(new PrintWriter(sw));
             * logger.severe("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
             */
        } catch (final BonitaHomeNotSetException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
        } catch (final ServerAPIException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
        } catch (final UnknownAPITypeException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " exception " + e.toString() + " at " + sw.toString());
        } finally {
            if (loginAPI != null) {
                try {
                    loginAPI.logout(userActivityApiSession);
                } catch (final SessionNotFoundException e) {

                } catch (final LogoutException e) {

                }
            }
        }

    }

}
