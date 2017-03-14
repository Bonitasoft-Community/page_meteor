package com.bonitasoft.custompage.meteor;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessExecutionException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionType;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;

import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.MeteorProcessDefinition;
import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.meteorDocument;

public class MeteorRobotCreateCase extends MeteorRobot {

    public MeteorProcessDefinition mMeteorProcessDefinition;
    public List<meteorDocument> mListDocuments = new ArrayList<meteorDocument>();

    // if this value is set, then we create case until this time, and then we stop. So, for example, we can say "create case for 2 hours
    public Long mTimeEndOfTheSimulationInMs = null;

    protected MeteorRobotCreateCase(final APIAccessor apiAccessor) {
        super(apiAccessor);
    }

    public void setParameters(final MeteorProcessDefinition meteorProcessDefinition, final List<meteorDocument> mListDocuments,
            final Long timeEndOfTheSimulationInMs)
    {
        mMeteorProcessDefinition = meteorProcessDefinition;
        this.mListDocuments = mListDocuments;

        mTimeEndOfTheSimulationInMs = timeEndOfTheSimulationInMs;

    }
    @Override
    public void executeRobot() {

        mCollectPerformance.mTitle = "CREATE CASE: " + mMeteorProcessDefinition.getInformation() + " #" + mRobotId;
        mCollectPerformance.mOperationTotal = mMeteorProcessDefinition.mNumberOfCases;
        final ProcessAPI processAPI = getAPIAccessor().getProcessAPI();

        // ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
        try
        {
            for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < mMeteorProcessDefinition.mNumberOfCases; mCollectPerformance.mOperationIndex++) {
                logger.info("--------- ROBOT #" + mRobotId + " ------ Advancement " + mCollectPerformance.mOperationIndex + " / "
                        + mMeteorProcessDefinition.mNumberOfCases
                    + " Sleep[" + mMeteorProcessDefinition.mTimeSleep + "]");

            // -------------------------------------- create
                final long timeStart = System.currentTimeMillis();

                createACase(mMeteorProcessDefinition.mProcessDefinitionId, mMeteorProcessDefinition.mVariables, mListDocuments, processAPI);

            final long timeEnd = System.currentTimeMillis();
            mCollectPerformance.collectOneTime(timeEnd - timeStart);
                if (mTimeEndOfTheSimulationInMs != null) {
                    if (System.currentTimeMillis() > mTimeEndOfTheSimulationInMs) {
                    return;
                }
            }

            Thread.sleep(mMeteorProcessDefinition.mTimeSleep);
        } // end loop
        } catch (final InvalidExpressionException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final ProcessActivationException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final ProcessDefinitionNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final ProcessExecutionException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final InterruptedException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        }
        logger.info("--------- ROBOT #" + mRobotId + " ENDED");

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
    public static void createACase(final Long processDefinitionId, final Map<String, Object> variables, final List<meteorDocument> listDocuments,
            final ProcessAPI processAPI) throws InvalidExpressionException, ProcessDefinitionNotFoundException, ProcessActivationException,
            ProcessExecutionException
    {

        final List<Operation> listOperations = new ArrayList<Operation>();
        final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

        for (String variableName : variables.keySet()) {

            if (variables.get(variableName) == null
                    || !(variables.get(variableName) instanceof Serializable)) {
                continue;
            }
            final Object value = variables.get(variableName);
            final Serializable valueSerializable = (Serializable) value;

            variableName = variableName.toLowerCase();
            final Expression expr = new ExpressionBuilder().createExpression(variableName, variableName, value.getClass().getName(),
                    ExpressionType.TYPE_INPUT);
            final Operation op = new OperationBuilder().createSetDataOperation(variableName, expr);
            listOperations.add(op);
            ListExpressionsContext.put(variableName, valueSerializable);
        }

        // add documents
        for (final meteorDocument toolHatProcessDefinitionDocument : listDocuments) {
            final DocumentValue documentValue = new DocumentValue(toolHatProcessDefinitionDocument.mContent.toByteArray(), "plain/text",
                    "myfilename");
            final Operation docRefOperation = new OperationBuilder().createSetDocument(
                    toolHatProcessDefinitionDocument.mDocumentName,
                    new ExpressionBuilder().createInputExpression(toolHatProcessDefinitionDocument.mDocumentName + "Reference",
                            DocumentValue.class.getName()));

            listOperations.add(docRefOperation);
            ListExpressionsContext.put(toolHatProcessDefinitionDocument.mDocumentName + "Reference", documentValue);
        }

        processAPI.startProcess(processDefinitionId, listOperations, ListExpressionsContext);

    }
}
