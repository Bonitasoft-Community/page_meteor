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
				ProcessInstance processInstance = createACase(mMeteorProcessDefinition.mProcessDefinitionId, false, meteorInput, mListDocuments, processAPI);
				mLogExecution.addLog(String.valueOf(processInstance.getId()));

				final long timeEnd = System.currentTimeMillis();
				mCollectPerformance.collectOneTime(timeEnd - timeStart);
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
			setFinalStatus( FINALSTATUS.FAIL);
			mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "ProcessDefinitionId=" + mMeteorProcessDefinition.mProcessDefinitionId));
		}
		logger.info("--------- SID #"+meteorSimulation.getId()+" ROBOT #" + mRobotId + " ENDED");

	} // end robot type CREATE CASE

	/**
	 * create a case
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
	public static ProcessInstance createACase(final Long processDefinitionId, boolean isVariable, final MeteorInput input, final List<MeteorDocument> listDocuments, final ProcessAPI processAPI)
			throws InvalidExpressionException, ProcessDefinitionNotFoundException, ProcessActivationException, ProcessExecutionException, ContractViolationException {

		final List<Operation> listOperations = new ArrayList<Operation>();
		final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

		// variable ? 
		if (isVariable)
		{
			if (input.getContent()!=null)
				for (String variableName : input.getContent().keySet()) {
	
					if (input.getContent().get(variableName) == null || !(input.getContent().get(variableName) instanceof Serializable)) {
						continue;
					}
					final Object value = input.getContent().get(variableName);
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

		if (isVariable)
		{
			ProcessInstance processInstance = processAPI.startProcess(processDefinitionId, listOperations, ListExpressionsContext);
			return processInstance;
		}
		
		ProcessInstance processInstance = processAPI.startProcessWithInputs(processDefinitionId, input==null ? null : input.getContent());
		return processInstance;
			
	}
}
