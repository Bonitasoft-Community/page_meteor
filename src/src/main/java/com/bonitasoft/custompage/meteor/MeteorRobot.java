package com.bonitasoft.custompage.meteor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;

import com.bonitasoft.custompage.meteor.MeteorSimulation.CollectPerformance;
import com.bonitasoft.custompage.meteor.scenario.cmd.MeteorRobotCmdScenario;
import com.bonitasoft.custompage.meteor.scenario.groovy.MeteorRobotGroovyScenario;

public abstract class MeteorRobot implements Runnable {

    Logger logger = Logger.getLogger(MeteorRobot.class.getName());

    protected enum RobotType {
        CREATECASE, PLAYACTIVITY, USERACTIVITY, CMDSCENARIO, GRVSCENARIO
    };

    // public RobotType mRobotType;

    protected enum RobotStatus {
        INACTIF, STARTED, DONE
    };

    /**
     * for the test unit, the robot should give a final status
     */
    public enum FINALSTATUS {
        SUCCESS, FAIL
    };

    public RobotStatus mStatus;

    public int mRobotId;
    //public MeteorDefinitionActivity mToolHatProcessDefinitionActivity;
    //public MeteorProcessDefinitionUser mToolHatProcessDefinitionUser;
    //public ArrayList<ToolHatProcessDefinitionDocument> mListDocuments;

    private final APIAccessor apiAccessor;

    public CollectPerformance mCollectPerformance = new CollectPerformance();

    protected MeteorRobot(final APIAccessor apiAccessor)
    {
        this.apiAccessor = apiAccessor;
        mStatus = RobotStatus.INACTIF;
    }

    public static MeteorRobot getInstance(final RobotType robotType, final APIAccessor apiAccessor)
    {
        if (robotType == RobotType.CREATECASE) {
            return new MeteorRobotCreateCase(apiAccessor);
        } else if (robotType == RobotType.USERACTIVITY) {
            return new MeteorRobotUserActivity(apiAccessor);
        } else if (robotType == RobotType.CMDSCENARIO) {
            return new MeteorRobotCmdScenario(apiAccessor);
        } else if (robotType == RobotType.GRVSCENARIO) {
            return new MeteorRobotGroovyScenario(apiAccessor);
        }
        return null;

    }

    /**
     * each robot should call this method to give the number of operation, in order to calculated the progress bar
     * NB : this method should be call only one time
     *
     * @param nbOperation
     * @see setOperationIndex
     */
    public void setNumberTotalOperation(final long nbOperation)
    {
        mCollectPerformance.mOperationTotal = nbOperation;
    }

    /**
     * update the number of operation done at this moment
     *
     * @param indexOperation
     * @see setNumberTotalOperation
     */
    public void setOperationIndex(final long indexOperation)
    {
        mCollectPerformance.mOperationIndex = indexOperation;
    }


    public FINALSTATUS mFinalStatus;
    public void setFinalStatus( final FINALSTATUS finalStatus )
    {
        mFinalStatus= finalStatus;
    }
    /**
     * get the accessor
     *
     * @return
     */
    public APIAccessor getAPIAccessor()
    {
        return apiAccessor;
    }

    public void start() {
        final Thread T = new Thread(this);
        T.start();
    }


    @Override
    public void run() {

        logger.info("----------- Start robot #" + mRobotId + " type[" + this.getClass().getName() + "]");
        mStatus = RobotStatus.STARTED;

        mCollectPerformance.clear();

            // log in to the tenant to create a session
            executeRobot();
        mStatus = RobotStatus.DONE;

        logger.info("----------- End robot #" + mRobotId + " type[" + getClass().getName() + "]");

    }

    /**
     * each robot should implement this
     */
    public abstract void executeRobot();

    /**
     * get the information
     *
     * @return
     */
    public RobotStatus getStatus()
    {
        return mStatus;
    }

    /**
     * @return
     */
    public Map<String, Object> getJsonInformation() {
        final Map<String, Object> resultRobot = new HashMap<String, Object>();

        resultRobot.put("title", mCollectPerformance.mTitle); // mProcessDefinition.getInformation()+" #"+mRobotId+" ";
        resultRobot.put("id", mRobotId); // mProcessDefinition.getInformation()+" #"+mRobotId+" ";
        int percent = 0;
        resultRobot.put("status", mStatus.toString());

        if (mCollectPerformance.mOperationTotal == -1)
        {
            resultRobot.put("adv", "0/0");
            percent = 0;
        }
        else if (mCollectPerformance.mOperationIndex < mCollectPerformance.mOperationTotal)
            {
            resultRobot.put("adv", mCollectPerformance.mOperationIndex + " / " + mCollectPerformance.mOperationTotal);
            percent = (int) (100 * mCollectPerformance.mOperationIndex / mCollectPerformance.mOperationTotal);
            }
            else
            {
            resultRobot.put("adv", mCollectPerformance.mOperationIndex + " / " + mCollectPerformance.mOperationTotal);
                percent = 100;
            }

        resultRobot.put("percent", percent);
        // status.append("<td><progress max=\"100\" value=\""+percent+"\"></progress>("+percent+" %)</td>");
        resultRobot.put("time", mCollectPerformance.mCollectTime + " ms for " + mCollectPerformance.mOperationIndex + " ope.");
        if (mCollectPerformance.mOperationIndex > 0) {
            resultRobot.put("timeavg", mCollectPerformance.mCollectTime / mCollectPerformance.mOperationIndex + " ms/ope.");
        }

        logger.info("STATUS Robot " + resultRobot);

        return resultRobot;
    }

    /**
     * return the time per step
     *
     * @return
     */
    public List<Long> getListTimePerStep()
    {
        return mCollectPerformance.mListTimePerStep;
    }

}
