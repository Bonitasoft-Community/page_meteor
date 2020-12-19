package org.bonitasoft.meteor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorSimulation.CollectPerformance;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;

public abstract class MeteorRobot implements Runnable {

    public static Logger logger = Logger.getLogger(MeteorRobot.class.getName());
    private static String loggerLabel = "MeteorRobot ##";

    private static BEvent eventThreadCantStart = new BEvent(MeteorRobot.class.getName(), 1, Level.ERROR, "Thread does not start", "Robot is executed in a separate thread. Thread does not start", "The execution is failed", "Check simulationId");

    /*
     * public enum RobotType {
     * CREATECASE, PLAYACTIVITY, CMDSCENARIO, GRVSCENARIO
     * };
     */
    // public RobotType mRobotType;

    private MeteorConst.ROBOTSTATUS mStatus = MeteorConst.ROBOTSTATUS.READYTOSTART;

    public String mTitle = "";

    /**
     * when the robot start
     */
    private Date mDateBegin;
    /**
     * when the robot stop
     */
    private Date mDateEnd;
    private int mRobotId;
    // public MeteorDefinitionActivity mToolHatProcessDefinitionActivity;
    // public MeteorProcessDefinitionUser mToolHatProcessDefinitionUser;
    // public ArrayList<ToolHatProcessDefinitionDocument> mListDocuments;

    private final APIAccessor apiAccessor;
    protected MeteorSimulation meteorSimulation;

    private String mRobotName;
    private String mExplanationError = "";
    public CollectPerformance mCollectPerformance = new CollectPerformance();

    /**
     * robot can log here the execution detail, error it face; etc...
     */
    public LogExecution mLogExecution = new LogExecution();

    protected MeteorRobot(String robotName, MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
        this.mRobotName = robotName;
        this.apiAccessor = apiAccessor;
        this.meteorSimulation = meteorSimulation;
        mStatus = MeteorConst.ROBOTSTATUS.READYTOSTART;
    }

    /*
     * ********************************************************************
     */
    /*                                                                      */
    /* Manage advancement */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * each robot should call this method to give the number of operation, in
     * order to calculated the progress bar NB : this method should be call only
     * one time
     *
     * @param nbOperation
     * @see setOperationIndex
     */
    public void setOperationTotal(final long nbOperation) {
        mCollectPerformance.mOperationTotal = nbOperation;
    }

    /**
     * update the number of operation done at this moment
     *
     * @param indexOperation
     * @see setNumberTotalOperation
     */
    public void setOperationIndex(final long indexOperation) {
        mCollectPerformance.mOperationIndex = indexOperation;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Attibutes */
    /*                                                                          */
    /*                                                                          */
    /* ************************************************************************ */

    /**
     * get the accessor
     *
     * @return
     */
    public APIAccessor getAPIAccessor() {
        return apiAccessor;
    }

    public Date getDateBegin() {
        return mDateBegin;
    }

    public Date getEndDate() {
        return mDateEnd;
    }

    public void addError(String explanationError) {
        if (explanationError.length() > 0)
            explanationError += "<br>";
        mExplanationError += explanationError;
        mLogExecution.addLog("ERROR:"+explanationError);
    }
    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* Execution */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */

    public void start() {
        mStatus = MeteorConst.ROBOTSTATUS.STARTING;
        try {
            final Thread T = new Thread(this);
            T.start();
        } catch (Exception e) {
            mStatus = MeteorConst.ROBOTSTATUS.FAILSTART;
            logger.severe(loggerLabel + "Exception starting robots " + e.getMessage());
            mLogExecution.addEvent(new BEvent(eventThreadCantStart, e, "Exception " + e.getMessage()));
        } catch (Error er) {
            mStatus = MeteorConst.ROBOTSTATUS.FAILSTART;
            logger.severe(loggerLabel + "Error starting robots " + er.getMessage());
            mLogExecution.addEvent(new BEvent(eventThreadCantStart, "Error " + er.getMessage()));
        }

    }

    public void run() {

        logger.info("----------- Start robot #" + mRobotId + " type[" + this.getClass().getName() + "]");
        mStatus = MeteorConst.ROBOTSTATUS.STARTED;

        mCollectPerformance.clear();
        try {
            // log in to the tenant to create a session
            mDateBegin = new Date();
            executeRobot();
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            mStatus = MeteorConst.ROBOTSTATUS.FAIL;
            logger.severe(loggerLabel + getSignature() + " exception " + e.toString() + " at " + sw.toString());
            mLogExecution.addLog("Exception " + e.toString());

        } catch (Error er) {
            final StringWriter sw = new StringWriter();
            er.printStackTrace(new PrintWriter(sw));
            mStatus = MeteorConst.ROBOTSTATUS.FAIL;

            logger.severe(loggerLabel + getSignature() + " exception " + er.toString() + " at " + sw.toString());
            mLogExecution.addLog("Exception " + er.toString());

        }

        mCollectPerformance.mOperationIndex = mCollectPerformance.mOperationTotal; // set
                                                                                   // to
                                                                                   // 100%

        mDateEnd = new Date();

        logger.info("----------- End robot #" + mRobotId + " type[" + getClass().getName() + "]");

    }

    public boolean isRunning() {
        if (mStatus == MeteorConst.ROBOTSTATUS.READYTOSTART || mStatus == MeteorConst.ROBOTSTATUS.STARTED)
            return true;
        return false;
    }

    /**
     * The robot is asking to stop
     */
    public void kill() {
        mLogExecution.addLog("KILL robots");
        mStatus = MeteorConst.ROBOTSTATUS.KILLED;
   
    }
    /**
     * each robot should implement this
     */
    public abstract void executeRobot();

    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* getInformation */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */

    /**
     * return a way to identify the robot
     * 
     * @return
     */
    private String mSignatureInfo = "";

    public void setRobotId(int id) {
        this.mRobotId = id;
    }

    public int getRobotId() {
        return mRobotId;
    }

    public String getSignature() {
        return "#" + mRobotId + " " + mRobotName + " " + mSignatureInfo;
    }

    public void setSignatureInfo(String info) {
        mSignatureInfo = info;
    }

    /**
     * get the information
     *
     * @return
     */
    public MeteorConst.ROBOTSTATUS getStatus() {
        return mStatus;
    }

    /**
     * set the status. if the robot is already killed, do nothing
     * @param status
     */
    public void setStatus(MeteorConst.ROBOTSTATUS status) {
        if (! MeteorConst.ROBOTSTATUS.KILLED.equals(mStatus)) 
            mStatus = status;
    }
    /**
     * Robot can check if the stop is required
     * @return
     */
    public boolean pleaseStop() {
        return MeteorConst.ROBOTSTATUS.KILLED.equals(mStatus);
    }
    
    /**
     * @return
     */
    public Map<String, Object> getDetailStatus() {
        final Map<String, Object> resultRobot = new HashMap<>();

        resultRobot.put(MeteorConst.CSTJSON_ROBOTTITLE, mTitle); // mProcessDefinition.getInformation()+"
        // #"+mRobotId+"
        // ";
        resultRobot.put("id", mRobotId); // mProcessDefinition.getInformation()+"
                                         // #"+mRobotId+" ";
        int percent = 0;
        resultRobot.put(MeteorConst.CSTJSON_ROBOTSTATUS, mStatus.toString());
        resultRobot.put(MeteorConst.CSTJSON_ROBOTNAME, mRobotName);
        resultRobot.put("explanationerror", mExplanationError);
        resultRobot.put("log", mLogExecution.getLogExecution());
        resultRobot.put(MeteorConst.CSTJSON_NBERRORS, mLogExecution.getNbErrors());

        if (mCollectPerformance.mOperationTotal == -1) {
            if (mStatus != MeteorConst.ROBOTSTATUS.DONE) {
                resultRobot.put("adv", "0/0");
                percent = 0;
            } else {
                resultRobot.put("adv", "100/100");
                percent = 100;
            }
        } else if (mCollectPerformance.mOperationIndex < mCollectPerformance.mOperationTotal) {
            resultRobot.put("adv", mCollectPerformance.mOperationIndex + " / " + mCollectPerformance.mOperationTotal);
            percent = (int) (100 * mCollectPerformance.mOperationIndex / mCollectPerformance.mOperationTotal);
        } else {
            resultRobot.put("adv", mCollectPerformance.mOperationIndex + " / " + mCollectPerformance.mOperationTotal);
            percent = 100;
        }

        resultRobot.put(MeteorConst.CSTJSON_PERCENTADVANCE, percent);
        // status.append("<td><progress max=\"100\"
        // value=\""+percent+"\"></progress>("+percent+" %)</td>");
        resultRobot.put("time", MeteorToolbox.getHumanDelay(mCollectPerformance.mCollectTimeSteps) + " for " + mCollectPerformance.getNbSteps() + " step");
        if (mCollectPerformance.getNbSteps() > 0) {
            resultRobot.put("timeavg", mCollectPerformance.mCollectTimeSteps / mCollectPerformance.getNbSteps() + " ms/step");
        }

        logger.info("STATUS Robot " + resultRobot);

        return resultRobot;
    }

    public int getNbErrors() {
        return mLogExecution.getNbErrors();
    }

    /**
     * return the time per step
     *
     * @return
     */
    public List<Long> getListTimePerStep() {
        return mCollectPerformance.mListTimePerStep;
    }

}
