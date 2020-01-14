package org.bonitasoft.meteor.scenario.process;

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
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;
import org.bonitasoft.meteor.scenario.process.MeteorDefInputs.MeteorInputItem;

public class MeteorRobotCreateCase extends MeteorRobot {

    private MeteorDefProcess mMeteorProcess;

    public List<MeteorDocument> mListDocuments = new ArrayList<MeteorDocument>();

    // if this value is set, then we create case until this time, and then we
    // stop. So, for example, we can say "create case for 2 hours
    public Long mTimeEndOfTheSimulationInMs = null;

    /**
     * robot keep a list of all cases created
     */
    private List<Long> mListProcessInstanceCreated = new ArrayList<Long>();

    public MeteorRobotCreateCase(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        super(meteorSimulation, apiAccessor);
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* getter/Setter */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public void setParameters(final MeteorDefProcess meteorProcess, final List<MeteorDocument> mListDocuments, final Long timeEndOfTheSimulationInMs) {
        mMeteorProcess = meteorProcess;
        this.mListDocuments = mListDocuments;

        mTimeEndOfTheSimulationInMs = timeEndOfTheSimulationInMs;

    }

    public MeteorDefProcess getMeteorProcess() {
        return mMeteorProcess;
    }

    public List<Long> getListProcessInstanceCreated() {
        return mListProcessInstanceCreated;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* Execution */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public void executeRobot() {

        mCollectPerformance.mTitle = "CREATE CASE: " + mMeteorProcess.getInformation() + " #" + getRobotId();
        setSignatureInfo("CREATE CASE: " + mMeteorProcess.getInformation());

        mCollectPerformance.mOperationTotal = mMeteorProcess.mNumberOfCases;
        final ProcessAPI processAPI = getAPIAccessor().getProcessAPI();

        // ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
        try {

            // recalulated the processID
            mMeteorProcess.mProcessDefinitionId = processAPI.getProcessDefinitionId(mMeteorProcess.mProcessName, mMeteorProcess.mProcessVersion);

            // preparation
            mMeteorProcess.prepareInputs(processAPI);
            Thread.sleep(mMeteorProcess.mDelaySleep);

            mMeteorProcess.mInputs.setRunSteps(mMeteorProcess.mNumberOfCases);
            for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < mMeteorProcess.mNumberOfCases; mCollectPerformance.mOperationIndex++) {
                logger.info("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " ------ Advancement " + mCollectPerformance.mOperationIndex + " / " + mMeteorProcess.mNumberOfCases + " Sleep[" + mMeteorProcess.mTimeSleep + "]");

                // select the input
                MeteorInputItem meteorInput = mMeteorProcess.mInputs.getInputAtStep(mCollectPerformance.mOperationIndex);

                // -------------------------------------- create
                final long timeStart = System.currentTimeMillis();

                ProcessInstance processInstance = createACase(mMeteorProcess.mProcessDefinitionId, false, meteorInput, mListDocuments, mLogExecution, processAPI);
                mListProcessInstanceCreated.add(processInstance.getId());

                final long timeEnd = System.currentTimeMillis();
                mCollectPerformance.collectOneStep(timeEnd - timeStart);
                if (mTimeEndOfTheSimulationInMs != null) {
                    if (System.currentTimeMillis() > mTimeEndOfTheSimulationInMs) {
                        return;
                    }
                }

                Thread.sleep(mMeteorProcess.mTimeSleep);
            } // end loop
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + getSignature() + " exception " + e.toString() + " at " + sw.toString());
            mLogExecution.addLog("Error during create case " + e.toString());

            setFinalStatus(FINALSTATUS.FAIL);
            mLogExecution.addEvent(new BEvent(MeteorSimulation.EventLogExecution, e, "ProcessDefinitionId=" + mMeteorProcess.mProcessDefinitionId));
        }
        logger.info("--------- SID #" + meteorSimulation.getId() + " ROBOT #" + getRobotId() + " ENDED");

    } // end robot type CREATE CASE

    /**
     * create a case basic log information are done : Case created, or Contract
     * Violation
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
    public static ProcessInstance createACase(final Long processDefinitionId, boolean isVariable, final MeteorInputItem meteorInput, final List<MeteorDocument> listDocuments, LogExecution logExecution, final ProcessAPI processAPI)
            throws InvalidExpressionException, ProcessDefinitionNotFoundException, ProcessActivationException, ProcessExecutionException {

        final List<Operation> listOperations = new ArrayList<Operation>();
        final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

        // variable ?
        if (isVariable) {
            if (meteorInput.getContent() != null)
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

        try {
            if (isVariable) {
                ProcessInstance processInstance = processAPI.startProcess(processDefinitionId, listOperations, ListExpressionsContext);
                logExecution.addLog("Case:" + String.valueOf(processInstance.getId()));
                return processInstance;
            }

            ProcessInstance processInstance = processAPI.startProcessWithInputs(processDefinitionId, meteorInput == null ? null : meteorInput.getContent());
            logExecution.addLog("Case:" + String.valueOf(processInstance.getId()));
            return processInstance;
        } catch (ContractViolationException e) {
            logExecution.addEvent(new BEvent(MeteorSimulation.EventContractViolationException, "Error[" + e.getMessage() + "] " + (meteorInput == null ? "No input" : "With input[" + meteorInput.mIndex + "]")));
            return null;
        }

    }
}
