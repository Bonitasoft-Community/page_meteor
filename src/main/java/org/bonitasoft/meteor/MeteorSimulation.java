package org.bonitasoft.meteor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorRobot.RobotStatus;
import org.bonitasoft.meteor.scenario.process.MeteorCalculCover;
import org.bonitasoft.meteor.scenario.process.MeteorCalculCover.CoverStatus;
import org.bonitasoft.meteor.scenario.process.MeteorDefProcess;

public class MeteorSimulation {

    Logger logger = Logger.getLogger(MeteorSimulation.class.getName());
    private static String loggerLabel = "MeteorSimulation ##";

    public final static BEvent EventStarted = new BEvent(MeteorSimulation.class.getName(), 1, Level.INFO, "Simulation Started", "The simulation started");
    public final static BEvent EventLogExecution = new BEvent(MeteorSimulation.class.getName(), 2, Level.INFO, "Error at execution", "An event is reported");
    public final static BEvent EventLogBonitaException = new BEvent(MeteorSimulation.class.getName(), 3, Level.APPLICATIONERROR, "Bonita Exception during execution", "An exception is reported by the Bonita Engine", "The robot may failed during the execution", "Check the exeption");
    public final static BEvent EventContractViolationException = new BEvent(MeteorSimulation.class.getName(), 4, Level.APPLICATIONERROR, "Contract exception", "The contract is not respected", "operation failed", "check the contract");
    public final static BEvent EventNoTaskToExecute = new BEvent(MeteorSimulation.class.getName(), 5, Level.APPLICATIONERROR, "No task to execute", "The robot don't find any task to executed in the activity name", "operation failed", "check the scenario");
    public final static BEvent EventAccessContract = new BEvent(MeteorSimulation.class.getName(), 6, Level.APPLICATIONERROR, "Access contract", "The contract can't be accessed", "Contract transformation cannot be done", "check the contract");    
    public final static BEvent EventFlowNodeExecution = new BEvent(MeteorSimulation.class.getName(), 7, Level.ERROR, "Task execution error", "Task faced an error during execution", "Execution is not correct", "check exception and scenario");
    
    public final static String CSTJSON_ID = "id";
    public final static String CSTJSON_PERCENTADVANCE = "percentAdvance";
    public final static String CSTJSON_TIMEESTIMATEDDELAY = "timeEstimatedDelay";
    public final static String CSTJSON_TIMEESTIMATEDEND = "timeEstimatedEnd";
    public final static String CSTJSON_STATUS = "status";
    public final static String CSTJSON_NBERRORS = "nbErrors";
    public final static String CSTJSONE_ROBOTS = "robots";

    public enum STATUS {
        DEFINITION, NOROBOT, STARTED, FINALISATION, DONE, NOSIMULATION
    }

    private STATUS mStatus = STATUS.DEFINITION;

    /**
     * when the Simulation start
     */
    private Date mDateBeginSimulation;
    /**
     * when the Simulation stop
     */
    private Date mDateEndSimulation;

    /**
     * collectPerformance
     */
    public static class CollectPerformance {

        public String mTitle = "";

        /**
         * advancement is based on Operation. Collect time is based on STEP. On
         * STEP can be one OPERATION but may be different Example : in Command
         * Scenario, when the scenario has 4 sentence, there are 4 operations
         * but one steps
         */
        // keep the advancement
        public long mOperationIndex;
        public long mOperationTotal = -1;

        // Keep the Steps
        public List<Long> mListTimePerStep = new ArrayList<>();
        public long mCollectTimeSteps = 0;

        public void clear() {
            mTitle = "";
            mCollectTimeSteps = 0;
            mListTimePerStep.clear();
        }

        public void collectOneStep(final long time) {
            mCollectTimeSteps += time;
            mListTimePerStep.add(Long.valueOf(time));
        }

        public int getNbSteps() {
            return mListTimePerStep.size();
        }
    }

    /**
     * logExecution
     */
    public static class LogExecution {

        public List<BEvent> mLogExecutionEvent = new ArrayList<>();
        public StringBuilder mLogExecutionLog = new StringBuilder("");
        public int mStatusNbErrors = 0;

        public void addLog(String msg) {
            mLogExecutionLog.append(msg + ";");
        }

        public void addEvent(BEvent event) {
            if (event.isError())
                mStatusNbErrors++;

            // do not report N time the same event
            for (BEvent eventReported : mLogExecutionEvent) {
                if (eventReported.isIdentical(event))
                    return;
            }
            mLogExecutionEvent.add(event);
            addLog(event.getTitle() + ":" + event.getParameters());
        }

        public String getLogExecution() {
            return mLogExecutionLog.toString();
        }

        public int getNbErrors() {
            return mStatusNbErrors;
        }
    }

    /**
     * each simulation has a uniq ID
     */
    private final long mId;

    private List<MeteorDefProcess> mListMeteorProcess = new ArrayList<>();

    private final List<MeteorRobot> mListRobots = new ArrayList<>();
    private MeteorCalculCover mCalculCover = null;
    private Long mTimeEndOfTheSimulation = null;

    private long tenantId;
    private APIAccessor apiAccessor;

    /** generate a unique ID */
    public MeteorSimulation(long tenantId, APIAccessor apiAccessor) {
        mId = System.currentTimeMillis();
        this.apiAccessor = apiAccessor;
        this.tenantId = tenantId;
    }

    public void clear() {
        mListRobots.clear();
    }

    public int getNumberOfRobots() {
        return mListRobots.size();
    }

    public long getId() {
        return mId;
    }

    /**
     * collect everything we need for the simulation
     * 
     * @param meteorScenario
     * @return
     */
    public List<BEvent> registerScenario(MeteorScenario meteorScenario) {
        List<BEvent> listEvents = new ArrayList<>();
        mListRobots.addAll(meteorScenario.generateRobots(this, apiAccessor));
        
        List<MeteorDefProcess> listProcessFromScenario = meteorScenario.collectProcess(this, apiAccessor);
        for (MeteorDefProcess defProcess : listProcessFromScenario) {
            boolean alreadyExist=false;
            for (int i=0;i<mListMeteorProcess.size();i++) {
            if (mListMeteorProcess.get( i ).mProcessDefinitionId.equals(defProcess.mProcessDefinitionId))
                alreadyExist=true;
            }
            if (!alreadyExist)
                mListMeteorProcess.add( defProcess );
        }
        return listEvents;
    }

    /**
     * add a new process robots
     *
     * @param processId
     * @param numberOfCase
     * @param timeSleep
     * @param variablesToSet
     *        public void addProcess(final MeteorDefProcess meteorProcess, final APIAccessor apiAccessor) {
     *        if (meteorProcess.mNumberOfRobots == 0) {
     *        meteorProcess.mNumberOfRobots = 1;
     *        }
     *        for (int i = 0; i < meteorProcess.mNumberOfRobots; i++) {
     *        final MeteorRobotCreateCase robot = (MeteorRobotCreateCase) MeteorRobot.getInstance(this, MeteorRobot.RobotType.CREATECASE, apiAccessor);
     *        robot.mRobotId = i + 1;
     *        robot.setParameters(meteorProcess, meteorProcess.getListDocuments(), mTimeEndOfTheSimulation == 0 ? null : mTimeEndOfTheSimulation);
     *        mListRobots.add(robot);
     *        }
     *        mListMeteorProcess.add( meteorProcess);
     *        }
     */

    /**
     * add a Robot Activity
     *
     * @param meteorActivity
     * @param apiAccessor
     */
    /*
     * public void addActivity(final MeteorDefActivity meteorActivity, final APIAccessor apiAccessor) {
     * if (meteorActivity.mNumberOfRobots == 0) {
     * meteorActivity.mNumberOfRobots = 1;
     * }
     * for (int i = 0; i < meteorActivity.mNumberOfRobots; i++) {
     * final MeteorRobotActivity robot = (MeteorRobotActivity) MeteorRobot.getInstance(this, MeteorRobot.RobotType.PLAYACTIVITY, apiAccessor);
     * robot.mRobotId = i + 1;
     * robot.setParameters(meteorActivity);
     * mListRobots.add(robot);
     * }
     * }
     */

    /**
     * add a Robot Scenario
     * public void addScenario(final ScenarioCmd meteorScenario, final APIAccessor apiAccessor) {
     * if (meteorScenario.mNumberOfRobots == 0) {
     * meteorScenario.mNumberOfRobots = 1;
     * }
     * for (int i = 0; i < meteorScenario.mNumberOfRobots; i++) {
     * MeteorRobot robot = null;
     * if (meteorScenario.mType == TYPESCENARIO.CMD) {
     * robot = MeteorRobot.getInstance(this, RobotType.CMDSCENARIO, apiAccessor);
     * ((MeteorRobotCmdScenario) robot).setScenario(meteorScenario);
     * }
     * if (meteorScenario.mType == TYPESCENARIO.GRV) {
     * robot = MeteorRobot.getInstance(this, RobotType.GRVSCENARIO, apiAccessor);
     * ((MeteorRobotGroovyScenario) robot).setScenario(meteorScenario);
     * }
     * if (robot != null) {
     * robot.mRobotId = i + 1;
     * mListRobots.add(robot);
     * }
     * }
     * }
     */

    public void setDurationOfSimulation(final int timeInMn) {
        mTimeEndOfTheSimulation = System.currentTimeMillis() + timeInMn * 60 * 1000;
    }

    /**
     * if null, no time is set
     * @return
     */
    public Long getDurationOfSimulation() {
        return mTimeEndOfTheSimulation;
    }

    public long getTenantId() {
        return tenantId;
    }

    public APIAccessor getApiAccessor() {
        return apiAccessor;
    }

    /**
     * run the simulation !
     */
    public void runTheSimulation() {
        logger.fine(loggerLabel+"[" + mId + "] ****************   RUN ********* the robots [" + mListRobots.size() + "] ! ");
        mStatus = STATUS.STARTED;
        mDateBeginSimulation = new Date();
        // tag each robot
        for (int i=0;i<mListRobots.size();i++)
            mListRobots.get( i ).setRobotId( i+1 );
        
        for (final MeteorRobot singleRobot : mListRobots) {
            singleRobot.start();
        }
    }

    public class OneTimeAnswer {

        public long sumTime = 0;
        public int nbAnswer = 0;

        public OneTimeAnswer(final long time) {
            sumTime = time;
            nbAnswer++;
        }

        public void add(final long time) {
            sumTime += time;
            nbAnswer++;
        }

        public long getValue() {
            if (nbAnswer == 0) {
                return sumTime;
            }
            return sumTime / nbAnswer;
        }
    }

    public STATUS getStatus() {
        return mStatus;
    }

    public void setStatus(STATUS status) {
        mStatus = status;
    }

    /**
     * calculate information on the running system
     * If all the robots finish, then the method finalise the result
     *
     * @return
     */
    public Map<String, Object> refreshDetailStatus(final APIAccessor apiAccessor) {
        logger.fine(loggerLabel+" [" + mId + "] Start currenStatusexecution");
        final HashMap<String, Object> result = new HashMap<>();
        final List<Map<String, Object>> listResultRobots = new ArrayList<>();

        Date dateEndRobots = null;
        int nbOperationRealized = 0;
        int totalSteps = 0;
        long totalStepsTime = 0;
        Long currentTime = System.currentTimeMillis();
        final List<OneTimeAnswer> listTimeAnswers = new ArrayList<>();

        Estimation estimation = getEstimatedAdvance();

        int nbErrors = 0;

        for (final MeteorRobot robot : mListRobots) {
            if (robot.getStatus() != RobotStatus.DONE) {
                logger.fine(loggerLabel+" [" + mId + "] getCurrentStatusExection robot [" + robot.getRobotId() + "] status[" + robot.getStatus() + "] StillAlive=" + estimation.robotsStillAlive);
            } else {
                if (dateEndRobots == null || dateEndRobots.before(robot.getEndDate()))
                    dateEndRobots = robot.getEndDate();
            }
            nbErrors += robot.getNbErrors();
            // calcul the minimum advancement
            nbOperationRealized += robot.mCollectPerformance.mOperationIndex;
            totalSteps += robot.mCollectPerformance.getNbSteps();
            totalStepsTime += robot.mCollectPerformance.mCollectTimeSteps;

            // get information on the robot
            listResultRobots.add(robot.getDetailStatus());

            // add the corresponding time according the type
            final List<Long> timeAnswerRobot = robot.getListTimePerStep();
            for (int i = 0; i < timeAnswerRobot.size(); i++) {
                if (i >= listTimeAnswers.size()) {
                    listTimeAnswers.add(new OneTimeAnswer(timeAnswerRobot.get(i)));
                } else {
                    final OneTimeAnswer oneTimeAnswer = listTimeAnswers.get(i);
                    oneTimeAnswer.add(timeAnswerRobot.get(i));
                    listTimeAnswers.set(i, oneTimeAnswer);
                }
            }

        }

        result.put( CSTJSONE_ROBOTS, listResultRobots);
        result.put( CSTJSON_NBERRORS, nbErrors);

        final long divisor = 1024L * 1024L;
        final long totalMemoryInMb = Runtime.getRuntime().totalMemory() / divisor;
        final long freeMemoryInMb = Runtime.getRuntime().freeMemory() / divisor;
        String total = "TOTAL : " + totalSteps + " step in " + MeteorToolbox.getHumanDelay(totalStepsTime) + " ms Op realized:"+nbOperationRealized+" Mem: " + (totalMemoryInMb - freeMemoryInMb) + "/" + totalMemoryInMb + " : " + (int) (100.0 * (totalMemoryInMb - freeMemoryInMb) / totalMemoryInMb) + " % ";
        if (totalSteps > 0) {
            total += " average " + totalStepsTime / totalSteps + " ms";
        }
        result.put("total", total);

        result.put("memUsedMb", totalMemoryInMb - freeMemoryInMb);
        result.put("memTotalMb", totalMemoryInMb);
        result.put("memPercent", (int) (100.0 * (totalMemoryInMb - freeMemoryInMb) / totalMemoryInMb));
        result.put("timeStarted", MeteorToolbox.getHumanDate(mDateBeginSimulation));
        logger.fine(loggerLabel+" [" + mId + "] currenStatusexecution : Robot Still Alive " + estimation.robotsStillAlive);

        if (!estimation.robotsStillAlive) {
            result.put(CSTJSON_PERCENTADVANCE, Integer.valueOf(100));

            // 3 possibility : 
            // DONE : all is finish
            // FINALYSE : finish, calcul cover in progress
            // else : we just arrive, robot just finish, so start the cover now
            if ((mStatus != STATUS.DONE && mStatus != STATUS.FINALISATION) || (mCalculCover == null)) {
                // start the calcul cover now
                mCalculCover = new MeteorCalculCover(mListMeteorProcess, mListRobots, apiAccessor);

                mCalculCover.start();
                mStatus = STATUS.FINALISATION;
            }
            result.put("cover", mCalculCover.toJson());

            // check the calcul cover
            if (mCalculCover != null && mCalculCover.getStatus() == CoverStatus.DONE)
                mStatus = STATUS.DONE;

            if (mDateEndSimulation == null) {
                mDateEndSimulation = dateEndRobots;
            }
            if (mDateEndSimulation != null)
                result.put("timeEnded", MeteorToolbox.getHumanDate(mDateEndSimulation));
            // display the cover

        } else {

            // calculate an estimation based on the slowest robot
            result.put(CSTJSON_PERCENTADVANCE, estimation.percentAdvance);
            if (estimation.percentAdvance == 0) {
                // we can't do any calculation
            }
            if (estimation.percentAdvance > 0) {
                result.put(CSTJSON_TIMEESTIMATEDDELAY, MeteorToolbox.getHumanDelay(estimation.timeNeedInMs));
                result.put(CSTJSON_TIMEESTIMATEDEND, MeteorToolbox.getHumanDate(new Date(currentTime + estimation.timeNeedInMs)));
            }
        }

        result.put(CSTJSON_STATUS, mStatus.toString());
        // chart
        /*
         * status.
         * append("<canvas id=\"myChart\" width=\"600\" height=\"200\"></canvas>"
         * ); status.append("<script src=\"Chart.js\"></script>");
         * status.append("<script>"); //Get the context of the canvas element we
         * want to select status.
         * append("var ctx = document.getElementById(\"myChart\").getContext(\"2d\");\n"
         * );
         *
        
        
        String valuesSerie1 = "";
        String labels = "";
        for (final OneTimeAnswer oneTimeAnswer : listTimeAnswers) {
            valuesSerie1 += oneTimeAnswer.getValue() + ",";
            labels += "\"\",";
        }
        valuesSerie1 += "0";
        labels += "\"\"";

        /*
         * status.append("var data= { labels : ["
         * +labels+"], datasets : [ {	fillColor : \"rgba(255,0,0,0.5)\",strokeColor : \"rgba(220,220,220,1)\", \n data : ["
         * +valuesSerie1+"]"); status.append("} ]};\n");
         */
        /*
         * var data = { labels :
         * ["January","February","March","April","May","June","July"], datasets
         * : [ { fillColor : "rgba(220,220,220,0.5)", strokeColor :
         * "rgba(220,220,220,1)", pointColor : "rgba(220,220,220,1)",
         * pointStrokeColor : "#fff", data : [65,59,90,81,56,55,40] },
         */
        // status.append("var chart = new Chart(ctx).Line(data);");
        // status.append("</script>");
        logger.info(loggerLabel+"["+mId+"] : END " + result);

        return result;
    }

    public Date getDateBeginSimulation() {
        return mDateBeginSimulation;
    }

    public Date getDateEndSimulation() {
        return mDateEndSimulation;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* Estimation */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public class Estimation {

        public long timeNeedInMs;
        public Date dateEnd;
        public int percentAdvance;
        public boolean robotsStillAlive;
        /**
         * when all the robots are done, then play the finalisation
         */
        public boolean finish = false;
    }

    /* calculate the estimation */
    public Estimation getEstimatedAdvance() {
        Estimation estimation = new Estimation();

        estimation.percentAdvance = 100;

        estimation.robotsStillAlive = false;

        for (final MeteorRobot robot : mListRobots) {
            if (robot.getStatus() != RobotStatus.DONE) {
                estimation.robotsStillAlive = true;
            }
            // calcul the minimum advancement
            int percentAdvance = 0;
            if (robot.mCollectPerformance.mOperationTotal == 0) {
                percentAdvance = 100;
            } else {
                final double percent = 100.0 * robot.mCollectPerformance.mOperationIndex / robot.mCollectPerformance.mOperationTotal;
                percentAdvance = (int) percent;
            }
            if (percentAdvance < estimation.percentAdvance) {
                estimation.percentAdvance = percentAdvance;
            }
        }
        if (!estimation.robotsStillAlive)
            estimation.percentAdvance = 100;

        if (estimation.percentAdvance == 0)
            return estimation;

        if (mDateBeginSimulation != null) {
            final long currentTime = System.currentTimeMillis();
            final long executionTime = currentTime - mDateBeginSimulation.getTime();
            estimation.timeNeedInMs = (long) ((100.0 - estimation.percentAdvance) * executionTime / estimation.percentAdvance);
            estimation.dateEnd = new Date(currentTime + estimation.timeNeedInMs);
        }
        return estimation;
    }

    public boolean isRunning() {
        return mStatus == STATUS.STARTED;
    }
}
