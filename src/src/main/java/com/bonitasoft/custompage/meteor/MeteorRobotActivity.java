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
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.bpm.flownode.UserTaskNotFoundException;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;

import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.MeteorActivity;
import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.meteorDocument;



public class MeteorRobotActivity extends MeteorRobot {

    public MeteorActivity mMeteorActivity;

    public List<meteorDocument> mListDocuments = new ArrayList<meteorDocument>();


    protected MeteorRobotActivity(final APIAccessor apiAccessor) {
        super(apiAccessor);

    }

    public void setParameters(final MeteorActivity meteorDefinitionActivity)
    {
        mMeteorActivity = meteorDefinitionActivity;
    }

    @Override
    public void executeRobot() {
        mCollectPerformance.mTitle = "EXECUTE ACTIVITY: " + mMeteorActivity.getInformation() + " #" + mRobotId;
        mCollectPerformance.mOperationTotal = mMeteorActivity.mNumberOfCases;
        final ProcessAPI processAPI = getAPIAccessor().getProcessAPI();

        // ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
        try
        {
            for (mCollectPerformance.mOperationIndex = 0; mCollectPerformance.mOperationIndex < mCollectPerformance.mOperationTotal; mCollectPerformance.mOperationIndex++) {
                logger.info("--------- ROBOT #" + mRobotId + " ------ Advancement " + mCollectPerformance.mOperationIndex + " / "
                        + mMeteorActivity.mNumberOfCases
                    + " Sleep[" + mMeteorActivity.mTimeSleep + "]");

            // -------------------------------------- create
            final List<Operation> listOperations = new ArrayList<Operation>();
            final Map<String, Serializable> ListExpressionsContext = new HashMap<String, Serializable>();

                /*
                 * for Variable, not for Input
                 * for (String variableName : mMeteorActivity.mVariables.keySet()) {
                 * if (mMeteorActivity.mInputs.get(variableName) == null
                 * || !(mMeteorActivity.mVariables.get(variableName) instanceof Serializable)) {
                 * continue;
                 * }
                 * final Object value = mMeteorActivity.mVariables.get(variableName);
                 * final Serializable valueSerializable = (Serializable) value;
                 * variableName = variableName.toLowerCase();
                 * final Expression expr = new ExpressionBuilder().createExpression(variableName, variableName, value.getClass().getName(),
                 * ExpressionType.TYPE_INPUT);
                 * final Operation op = new OperationBuilder().createSetDataOperation(variableName, expr);
                 * listOperations.add(op);
                 * ListExpressionsContext.put(variableName, valueSerializable);
                 * }
                 */

            // add documents
            for (final meteorDocument toolHatProcessDefinitionDocument : mListDocuments) {
                final DocumentValue documentValue = new DocumentValue(toolHatProcessDefinitionDocument.mContent.toByteArray(), "plain/text",
                        "myfilename");
                final Operation docRefOperation = new OperationBuilder().createSetDocument(
                        toolHatProcessDefinitionDocument.mDocumentName,
                        new ExpressionBuilder().createInputExpression(toolHatProcessDefinitionDocument.mDocumentName + "Reference",
                                DocumentValue.class.getName()));

                listOperations.add(docRefOperation);
                ListExpressionsContext.put(toolHatProcessDefinitionDocument.mDocumentName + "Reference", documentValue);
            }

            final long timeStart = System.currentTimeMillis();
                processAPI.executeUserTask(mMeteorActivity.mActivityDefinitionId, mMeteorActivity.mInputs);
            final long timeEnd = System.currentTimeMillis();
            mCollectPerformance.collectOneTime(timeEnd - timeStart);
                Thread.sleep(mMeteorActivity.mTimeSleep);

            } // end loop

        } catch (final InvalidExpressionException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final FlowNodeExecutionException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final ContractViolationException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final UserTaskNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        } catch (final InterruptedException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("Robot #" + mRobotId + " exception " + e.toString() + " at " + sw.toString());
        }
        logger.info("--------- ROBOT #" + mRobotId + " ENDED");


    }

}
