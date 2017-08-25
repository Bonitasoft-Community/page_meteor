package org.bonitasoft.meteor;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
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
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorActivity;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorDocument;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorInput;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;

public class MeteorRobotActivity extends MeteorRobot {

	public MeteorActivity mMeteorActivity;

	public List<MeteorDocument> mListDocuments = new ArrayList<MeteorDocument>();

	protected MeteorRobotActivity(MeteorSimulation meteorSimulation,final APIAccessor apiAccessor) {
		super(meteorSimulation, apiAccessor);

	}

	public void setParameters(final MeteorActivity meteorDefinitionActivity) {
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

		
		final long timeStart = System.currentTimeMillis();
		try
		{
			mMeteorActivity.mProcessDefinitionId = processAPI.getProcessDefinitionId(mMeteorActivity.mProcessName, mMeteorActivity.mProcessVersion);
			mMeteorActivity.mInputs.setInputSteps( mCollectPerformance.mOperationTotal );
			for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < mCollectPerformance.mOperationTotal; mCollectPerformance.mOperationIndex++) {
				logger.info("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " ------ Advancement " + mCollectPerformance.mOperationIndex + " / " + mMeteorActivity.mNumberOfCases + " Sleep[" + mMeteorActivity.mTimeSleep + "]");

				// -------------------------------------- create
				final List<Operation> listOperations = new ArrayList<Operation>();
				final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

				MeteorInput meteorInput = mMeteorActivity.mInputs.getInputAtStep( mCollectPerformance.mOperationIndex );

				// add documents
				for (final MeteorDocument meteorDocument : mListDocuments) {
					final DocumentValue documentValue = new DocumentValue(meteorDocument.mContent.toByteArray(), "plain/text", "myfilename");
					final Operation docRefOperation = new OperationBuilder().createSetDocument(meteorDocument.mDocumentName, new ExpressionBuilder().createInputExpression(meteorDocument.mDocumentName + "Reference", DocumentValue.class.getName()));

					listOperations.add(docRefOperation);
					ListExpressionsContext.put(meteorDocument.mDocumentName + "Reference", documentValue);
				}

				
				
				Long taskId = executeActivity(mMeteorActivity.mProcessDefinitionId, mMeteorActivity.mActivityName, meteorInput, mMeteorActivity.mTimeSleep, mRobotId,  mLogExecution, processAPI, identityAPI);
				
				
				
				Thread.sleep( mMeteorActivity.mTimeSleep );

			} // end loop
			
		
	} catch( ProcessDefinitionNotFoundException e)
	{
		final StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		logger.severe("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
		if (! alreadyLoggedException.contains(e))
		{
			alreadyLoggedException.add( e.toString() );
			mLogExecution.addLog("Process:["+mMeteorActivity.mProcessName+"] version["+mMeteorActivity.mProcessVersion+"] not found;");
		}
		
	}
	catch (Exception e) {
		final StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		logger.severe("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
		if (! alreadyLoggedException.contains(e))
		{
			alreadyLoggedException.add( e.toString() );
			mLogExecution.addLog("error:"+e.toString());
		}

	}
		final long timeEnd = System.currentTimeMillis();
		mCollectPerformance.collectOneStep(timeEnd - timeStart);
		
		
		
		logger.info("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " ENDED");

	}

	
	public static Long executeActivity( Long processDefinitionId, String activityName, MeteorInput meteorInput, long timeSleep, long robotId, LogExecution logExecution, ProcessAPI processAPI, IdentityAPI identityAPI ) throws SearchException
	{

		// search a user to run the human task
		
		User user = null;
		
		SearchResult<User> searchUser = identityAPI.searchUsers(new SearchOptionsBuilder(0, 10).done());
		user = searchUser.getCount() > 0 ? searchUser.getResult().get(0) : null;
		List<String> alreadyLoggedException = new ArrayList<String>();

		// search a task
				int countExecute = 10;
				while (countExecute > 0) {
					countExecute--;

					try {
						SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0, 10);
						searchOptions.filter(HumanTaskInstanceSearchDescriptor.NAME, activityName );
						searchOptions.filter(HumanTaskInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId);
						SearchResult<HumanTaskInstance> search = processAPI.searchHumanTaskInstances(searchOptions.done());
						if (search.getResult().size() > 0) {
							Long taskId = search.getResult().get(0).getId();
							// assign it
							processAPI.assignUserTask(taskId, user != null ? user.getId() : 0);
							// run it
							final long timeStart = System.currentTimeMillis();
							// variable
							processAPI.executeUserTask(taskId, meteorInput.getContent());
							final long timeEnd = System.currentTimeMillis();
							logExecution.addLog("Task:["+String.valueOf(taskId)+"]");
							return taskId;
							
						}
					} catch(ContractViolationException vc)
					{
						logExecution.addEvent( new BEvent( MeteorSimulation.EventContractViolationException, meteorInput==null ? "No input" : "With input["+meteorInput.index+"]"));
						return null;
				
					} catch (Exception e) {						
						final StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
												
						logger.severe("Robot #" + robotId + " exception " + e.toString() + " at " + sw.toString());
						// not yet logged ? Add in the logExecution
						logExecution.addEvent( new BEvent( MeteorSimulation.EventLogExecution, ""));

					}
					if (countExecute != 0) {
						// Ok, an error arrive or there are no task yet, we have
						// to wait a little
						try { Thread.sleep(1000); } catch(Exception e){};
					}
				}
				logExecution.addEvent( new BEvent(MeteorSimulation.EventNoTaskToExecute, "Activity["+activityName+"]"));
				return null;
				

		
	}
}
