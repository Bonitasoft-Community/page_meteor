package com.bonitasoft.custompage.meteor.scenario.cmd;

import java.util.List;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

import com.bonitasoft.custompage.meteor.MeteorRobot;
import com.bonitasoft.custompage.meteor.scenario.Scenario;

public class MeteorRobotCmdScenario extends MeteorRobot {




    public Scenario meteorScenario;

    public MeteorRobotCmdScenario(final APIAccessor apiAccessor) {
        super(apiAccessor);
    }

    public void setScenario(final Scenario meteorScenario)
    {
        this.meteorScenario = meteorScenario;
    }
    /**
     * Unit Test :
     * <createCase processdefinitionname="pool" processdefinition="1.0" caseid="MyCaseFrancis">
     * <contract name"firstname" value="Francis">
     * <createCase>
     * <assert caseid="MyCaseFrancis" taskname="AmountTooBig" performanceexpectedms="4000">
     * <variable name="amount.value" value="233" type="Integer">
     * <assert>
     * <executeTask caseid="MyCaseFrancis" taskname="AmountTooBig">
     * <contract name="status" value="true">
     * <contract name="comment" value="accepted">
     * </executeTask>
     * <executeTask caseid="MyCaseFrancis" taskname="FinalAcceptance">
     * <contract name="final" value="validated">
     * </executeTask>
     * <assert caseid="MyCaseFrancis" isarchived="true" performanceexpectedms="35000"/>
     * Load Test :
     * <createCase processdefinitionname="pool" processdefinition="1.0" numberofcase="1000">
     * <contract name="firstname" value="Francis">
     * <contract name="amount" value="10000">
     * <createCase>
     * <createCase processdefinitionname="pool" processdefinition="1.0" numberofcase="500">
     * <contract name"firstname" value="Francis">
     * <contract name"firstname" value="14000">
     * <createCase>
     * <executeTask taskname="AmountTooBig" numberoftask="500">
     * <contract name="status" value="true">
     * <contract name="comment" value="accepted">
     * </executeTask>
     * <assert isarchived="true" performanceexpectedms="35000"/>
     */
    /**
     * @param args
     */


    @Override
    public void executeRobot() {
        // please call setNumberTotalOperation( long nbOperation) and setOperationIndex( long indexOperation)
        final List<BEvent> listEvents = meteorScenario.decodeScenario();
        if (BEventFactory.isError(listEvents)) {
            return;
        }

        setNumberTotalOperation(meteorScenario.listSentences.size());
        for (int i = 0; i < meteorScenario.listSentences.size(); i++)
        {

            setOperationIndex(i);
            meteorScenario.listSentences.get(i).execute();
        }

    }


}
