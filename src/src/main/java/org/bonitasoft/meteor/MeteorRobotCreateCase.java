package org.bonitasoft.meteor;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessExecutionException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionType;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorDocument;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorInput;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorProcess;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;

public class MeteorRobotCreateCase extends MeteorRobot {

	public MeteorProcess mMeteorProcessDefinition;
	
	public List<MeteorDocument> mListDocuments = new ArrayList<MeteorDocument>();

	// if this value is set, then we create case until this time, and then we
	// stop. So, for example, we can say "create case for 2 hours
	public Long mTimeEndOfTheSimulationInMs = null;

	protected MeteorRobotCreateCase(MeteorSimulation meteorSimulation,final APIAccessor apiAccessor) {
		super(meteorSimulation, apiAccessor);
	}

	public void setParameters(final MeteorProcess meteorProcessDefinition, final List<MeteorDocument> mListDocuments, final Long timeEndOfTheSimulationInMs) {
		mMeteorProcessDefinition = meteorProcessDefinition;
		this.mListDocuments = mListDocuments;

		mTimeEndOfTheSimulationInMs = timeEndOfTheSimulationInMs;

	}

	@Override
	public void executeRobot() {

		mCollectPerformance.mTitle = "CREATE CASE: " + mMeteorProcessDefinition.getInformation() + " #" + mRobotId;
		setSignatureInfo("CREATE CASE: " + mMeteorProcessDefinition.getInformation());

		mCollectPerformance.mOperationTotal = mMeteorProcessDefinition.mNumberOfCases;
		final ProcessAPI processAPI = getAPIAccessor().getProcessAPI();

		
		// ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
		try {
			// recalulated the processID
			mMeteorProcessDefinition.mProcessDefinitionId = processAPI.getProcessDefinitionId(mMeteorProcessDefinition.mProcessName, mMeteorProcessDefinition.mProcessVersion);

			mMeteorProcessDefinition.mInputs.setInputSteps( mMeteorProcessDefinition.mNumberOfCases );
			for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < mMeteorProcessDefinition.mNumberOfCases; mCollectPerformance.mOperationIndex++) {
				logger.info("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " ------ Advancement " + mCollectPerformance.mOperationIndex + " / " + mMeteorProcessDefinition.mNumberOfCases + " Sleep[" + mMeteorProcessDefinition.mTimeSleep + "]");

				// select the input
				MeteorInput meteorInput = mMeteorProcessDefinition.mInputs.getInputAtStep( mCollectPerformance.mOperationIndex );
				
				
				// -------------------------------------- create
				final long timeStart = System.currentTimeMillis();
				
				ProcessInstance processInstance = createACase(mMeteorProcessDefinition.mProcessDefinitionId, false, meteorInput, mListDocuments, mLogExecution, processAPI);

				final long timeEnd = System.currentTimeMillis();
				mCollectPerformance.collectOneStep(timeEnd - timeStart);
				if (mTimeEndOfTheSimulationInMs != null) {
					if (System.currentTimeMillis() > mTimeEndOfTheSimulationInMs) {
						return;
					}
				}

				Thread.sleep(mMeteorProcessDefinition.mTimeSleep);
			} // end loop
		} catch (final Exception e) {
			final StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			logger.severe("Robot #" + getSignature() + " exception " + e.toString() + " at " + sw.toString());
			mLogExecution.addLog("Error during create case "+e.toString());

			setFinalStatus( FINALSTATUS.FAIL);
			mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "ProcessDefinitionId=" + mMeteorProcessDefinition.mProcessDefinitionId));
		}
		logger.info("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " ENDED");

	} // end robot type CREATE CASE

	/**
	 * create a case
	 * basic log information are done : Case created, or Contract Violation
	 * 
	 * @param processDefinitionId
	 * @param variables
	 * @param listDocuments
	 * @param processAPI
	 * @throws InvalidExpressionException
	 * @throws ProcessDefinitionNotFoundException
	 * @throws ProcessActivationException
	 * @throws ProcessExecutionException
	 */
	public static ProcessInstance createACase(final Long processDefinitionId, boolean isVariable, final MeteorInput meteorInput, final List<MeteorDocument> listDocuments,
			LogExecution logExecution,
			final ProcessAPI processAPI)
			throws InvalidExpressionException, ProcessDefinitionNotFoundException, ProcessActivationException, ProcessExecutionException {

		final List<Operation> listOperations = new ArrayList<Operation>();
		final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

		// variable ? 
		if (isVariable)
		{
			if (meteorInput.getContent()!=null)
				for (String variableName : meteorInput.getContent().keySet()) {
	
					if (meteorInput.getContent().get(variableName) == null || !(meteorInput.getContent().get(variableName) instanceof Serializable)) {
						continue;
					}
					final Object value = meteorInput.getContent().get(variableName);
					final Serializable valueSerializable = (Serializable) value;
		
					variableName = variableName.toLowerCase();
					final Expression expr = new ExpressionBuilder().createExpression(variableName, variableName, value.getClass().getName(), ExpressionType.TYPE_INPUT);
					final Operation op = new OperationBuilder().createSetDataOperation(variableName, expr);
					listOperations.add(op);
					ListExpressionsContext.put(variableName, valueSerializable);
				}
		}
		// add documents
		for (final MeteorDocument meteorDocument : listDocuments) {
			final DocumentValue documentValue = new DocumentValue(meteorDocument.mContent.toByteArray(), "plain/text", "myfilename");
			final Operation docRefOperation = new OperationBuilder().createSetDocument(meteorDocument.mDocumentName, new ExpressionBuilder().createInputExpression(meteorDocument.mDocumentName + "Reference", DocumentValue.class.getName()));

			listOperations.add(docRefOperation);
			ListExpressionsContext.put(meteorDocument.mDocumentName + "Reference", documentValue);
		}

		try
		{
			if (isVariable)
			{
				ProcessInstance processInstance = processAPI.startProcess(processDefinitionId, listOperations, ListExpressionsContext);
				logExecution.addLog("Case:"+String.valueOf(processInstance.getId()));
				return processInstance;
			}
			
			ProcessInstance processInstance = processAPI.startProcessWithInputs(processDefinitionId, meteorInput==null ? null : meteorInput.getContent());
			logExecution.addLog("Case:"+String.valueOf(processInstance.getId()));
			return processInstance;
		}
		catch( ContractViolationException e)
		{
			logExecution.addEvent( new BEvent( MeteorSimulation.EventContractViolationException, meteorInput==null ? "No input" : "With input["+meteorInput.index+"]"));
			return null;
		}
			
	}
}
