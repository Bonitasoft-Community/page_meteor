package org.bonitasoft.meteor.scenario.process;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.UserTaskNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;
import org.bonitasoft.meteor.scenario.process.MeteorDefInputs.MeteorInputItem;

public class MeteorRobotActivity extends MeteorRobot {

	public MeteorDefActivity mMeteorActivity;

	public List<MeteorDocument> mListDocuments = new ArrayList<MeteorDocument>();

	public MeteorRobotActivity(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
		super(meteorSimulation, apiAccessor);

	}

	public void setParameters(final MeteorDefActivity meteorDefinitionActivity) {
		mMeteorActivity = meteorDefinitionActivity;
	}

	@Override
	public void executeRobot() {
		mCollectPerformance.mTitle = "EXECUTE ACTIVITY: " + mMeteorActivity.getInformation() + " #" + mRobotId;
		mCollectPerformance.mOperationTotal = mMeteorActivity.mNumberOfCases;
		ProcessAPI processAPI = getAPIAccessor().getProcessAPI();
		IdentityAPI identityAPI = getAPIAccessor().getIdentityAPI();

		// -------------------------------------- Activity
		List<String> alreadyLoggedException = new ArrayList<String>();

		
		long timeStart = System.currentTimeMillis();
		
		try {
			// preparation
			Thread.sleep(mMeteorActivity.mDelaySleep);

			// run now : update the timeStart
			timeStart = System.currentTimeMillis();
			mMeteorActivity.mInputs.setRunSteps(mCollectPerformance.mOperationTotal);
			for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < mCollectPerformance.mOperationTotal; mCollectPerformance.mOperationIndex++) {
				logger.info("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + mRobotId + " ------ Advancement " + mCollectPerformance.mOperationIndex + " / " + mMeteorActivity.mNumberOfCases + " Sleep[" + mMeteorActivity.mTimeSleep + "]");

				// -------------------------------------- execute
				final List<Operation> listOperations = new ArrayList<Operation>();
				final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

				MeteorInputItem meteorInputItem = mMeteorActivity.mInputs.getInputAtStep(mCollectPerformance.mOperationIndex);

				// add documents
				for (final MeteorDocument meteorDocument : mListDocuments) {
					final DocumentValue documentValue = new DocumentValue(meteorDocument.mContent.toByteArray(), "plain/text", "myfilename");
					final Operation docRefOperation = new OperationBuilder().createSetDocument(meteorDocument.mDocumentName, new ExpressionBuilder().createInputExpression(meteorDocument.mDocumentName + "Reference", DocumentValue.class.getName()));

					listOperations.add(docRefOperation);
					ListExpressionsContext.put(meteorDocument.mDocumentName + "Reference", documentValue);
				}

				Long taskId = executeActivity(mMeteorActivity.getMeteorProcess().mProcessDefinitionId, mMeteorActivity.mActivityName,  mMeteorActivity.mInputs, meteorInputItem, mMeteorActivity.mTimeSleep, mRobotId, mLogExecution, processAPI, identityAPI);

				Thread.sleep(mMeteorActivity.mTimeSleep);

			} // end loop

		} catch (Exception e) {
			final StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			logger.severe("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
			if (!alreadyLoggedException.contains(e)) {
				alreadyLoggedException.add(e.toString());
				mLogExecution.addLog("error:" + e.toString());
			}

		}
		final long timeEnd = System.currentTimeMillis();
		mCollectPerformance.collectOneStep(timeEnd - timeStart);

		logger.info("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + mRobotId + " ENDED");

	}

	public static Long executeActivity(Long processDefinitionId,
			String activityName, 
			MeteorDefInputs meteorInputs,
			MeteorInputItem meteorInput, long timeSleep, long robotId, LogExecution logExecution, ProcessAPI processAPI, IdentityAPI identityAPI) throws SearchException {

		// search a user to run the human task

		User user = null;

		SearchResult<User> searchUser = identityAPI.searchUsers(new SearchOptionsBuilder(0, 10).done());
		user = searchUser.getCount() > 0 ? searchUser.getResult().get(0) : null;
		List<String> alreadyLoggedException = new ArrayList<String>();

		// search a task
		int countExecute = 10;
		while (countExecute > 0) {
			countExecute--;
			Long taskId=null;
			try {
				SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0, 10);
				searchOptions.filter(HumanTaskInstanceSearchDescriptor.NAME, activityName);
				searchOptions.filter(HumanTaskInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId);
				SearchResult<HumanTaskInstance> search = processAPI.searchHumanTaskInstances(searchOptions.done());
				if (search.getResult().size() > 0) {
					taskId = search.getResult().get(0).getId();
					Long processInstanceId =  search.getResult().get(0).getRootContainerId();
					// assign it
					processAPI.assignUserTask(taskId, user != null ? user.getId() : 0);
					// run it
					final long timeStart = System.currentTimeMillis();
					
					
					// Now, the contract can be reach. So, if the contract is unkwonw, this is the moment
					if (meteorInputs.getContractDefinition() ==null)
					{
						try
						{
							meteorInputs.setContractDefinition( processAPI.getUserTaskContract(taskId) );
							meteorInputs.prepareInputs();
							
						}
						catch(Exception e)
						{
							logExecution.addEvent(new BEvent(MeteorSimulation.EventAccessContract, "ProcessDefinition["+processDefinitionId+"] ActivityName["+activityName+"] "+ meteorInput == null ? "No input" : "With input[" + meteorInput.mIndex + "]"));
						}
					}
					
					
					
					processAPI.executeUserTask(taskId, meteorInput==null ? null : meteorInput.getContent());
					final long timeEnd = System.currentTimeMillis();
					logExecution.addLog("#:" + String.valueOf(taskId)+" ("+processInstanceId+")" );
					return taskId;

				}
			} catch (ContractViolationException vc) {
				logExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, "ProcessDefinition["+processDefinitionId+"] ActivityName["+activityName+"] "+ (meteorInput == null ? "No input" : "With input[" + meteorInput.mIndex + "]")));
				return null;

			} catch (FlowNodeExecutionException fl)
			{
				// someone execute the same node, no worry
			 } catch (UserTaskNotFoundException ue)
      {
        // someone execute the same node, no worry
      } catch (Exception e) {
				final StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));

				logger.severe("Robot #" + robotId + " exception " + e.toString() + " at " + sw.toString());
				// not yet logged ? Add in the logExecution
				logExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "taskId:["+taskId+"]"));

			}
			if (countExecute != 0) {
				// Ok, an error arrive or there are no task yet, we have
				// to wait a little
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				;
			}
		}
		final SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy HH:MM:SS");
		logExecution.addEvent(new BEvent(MeteorSimulation.EventNoTaskToExecute, "Activity[" + activityName + "] at "+sdf.format( new Date() )));
		return null;

	}
}
