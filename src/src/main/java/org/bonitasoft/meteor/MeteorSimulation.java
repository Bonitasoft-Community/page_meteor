package org.bonitasoft.meteor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorActivity;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorProcess;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.MeteorProcessDefinitionUser;
import org.bonitasoft.meteor.MeteorRobot.RobotStatus;
import org.bonitasoft.meteor.MeteorRobot.RobotType;
import org.bonitasoft.meteor.scenario.Scenario;
import org.bonitasoft.meteor.scenario.Scenario.TYPESCENARIO;
import org.bonitasoft.meteor.scenario.cmd.MeteorRobotCmdScenario;
import org.bonitasoft.meteor.scenario.groovy.MeteorRobotGroovyScenario;

public class MeteorSimulation {

	Logger logger = Logger.getLogger(MeteorSimulation.class.getName());

	
	protected static BEvent EventStarted = new BEvent("org.bonitasoft.custompage.meteor.MeteorSimulation", 1, Level.INFO, "Simulation Started", "The simulation started");
	protected static BEvent EventLogExecution = new BEvent("org.bonitasoft.custompage.meteor.MeteorSimulation", 2, Level.INFO, "Log execution", "An event is reported");
	protected static BEvent EventLogBonitaException = new BEvent("org.bonitasoft.custompage.meteor.MeteorSimulation", 3, Level.APPLICATIONERROR, "Bonita Exception during execution", "An exception is reported by the Bonita Engine", "The robot may failed during the execution", "Check the exeption");
	protected static BEvent EventContractViolationException = new BEvent("org.bonitasoft.custompage.meteor.MeteorSimulation", 4, Level.APPLICATIONERROR, "Contract exception", "The contract is not respected", "operation failed", "check the contract");
	protected static BEvent EventNoTaskToExecute = new BEvent("org.bonitasoft.custompage.meteor.MeteorSimulation", 5, Level.APPLICATIONERROR, "No task to execute", "The robot don't find any task to executed in the activity name", "operation failed", "check the scenario");

	public final static String cstJsonPercentAdvance="percentAdvance";
	public final static String cstJsonTimeEstimatedDelay ="timeEstimatedDelay";
	public final static String cstJsonTimeEstimatedEnd ="timeEstimatedEnd";
	public final static String cstJsonStatus= "status";
	public final static String cstJsonNbErrors ="nbErrors";
			
	public enum STATUS {
		DEFINITION, NOROBOT, STARTED, DONE, NOSIMULATION
	};

	
	private STATUS mStatus = STATUS.DEFINITION;

	/**
	 * when the Simulation start
	 */
	private Date mDateBeginSimulation;
	/**
	 * when the Simulation stop
	 */
	private Date mDateEndSimulation;

	public static class CollectPerformance {

		public String mTitle = "";

		/**
		 * advancement is based on Operation. Collect time is based on STEP. On STEP can be one OPERATION but may be different
		 * Example : in Command Scenario, when the scenario has 4 sentence, there are 4 operations but one steps
		 */
		// keep the advancement
		public long mOperationIndex;
		public long mOperationTotal = -1;

		// Keep the Steps
		public List<Long> mListTimePerStep = new ArrayList<Long>();
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
		public int getNbSteps()
		{
			return mListTimePerStep.size();
		}
	}

	public static class LogExecution {
		public List<BEvent> mLogExecutionEvent = new ArrayList<BEvent>();
		public StringBuffer mLogExecutionLog = new StringBuffer("");
		public int mStatusNbErrors= 0;
		
		public void addLog(String msg) {
			mLogExecutionLog.append(msg + ";");
		}

		public void addEvent(BEvent event) {
			if (event.isError())
				mStatusNbErrors++;

			// do not report N time the same event
			for (BEvent eventReported : mLogExecutionEvent)
			{
				if (eventReported.isIdentical(event))
					return;
			}
			mLogExecutionEvent.add(event);
			addLog( event.getTitle()+":"+event.getParameters());
		}
		public String getLogExecution()
		{
			return mLogExecutionLog.toString();
		}
		public int getNbErrors()
		{
			return mStatusNbErrors;
		}
	}

	private final long mId;

	/** generate a unique ID */
	public MeteorSimulation() {
		mId = System.currentTimeMillis();
	}

	private final ArrayList<MeteorRobot> mListRobots = new ArrayList<MeteorRobot>();
	private long mTimeEndOfTheSimulation = 0;

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
	 * add a new process robots
	 *
	 * @param processId
	 * @param numberOfCase
	 * @param timeSleep
	 * @param variablesToSet
	 */
	public void addProcess(final MeteorProcess meteorProcessDefinition, final APIAccessor apiAccessor) {
		if (meteorProcessDefinition.mNumberOfRobots == 0) {
			meteorProcessDefinition.mNumberOfRobots = 1;
		}
		for (int i = 0; i < meteorProcessDefinition.mNumberOfRobots; i++) {
			final MeteorRobotCreateCase robot = (MeteorRobotCreateCase) MeteorRobot.getInstance(this, MeteorRobot.RobotType.CREATECASE, apiAccessor);
			robot.mRobotId = i + 1;
			robot.setParameters(meteorProcessDefinition, meteorProcessDefinition.mListDocuments, mTimeEndOfTheSimulation == 0 ? null : mTimeEndOfTheSimulation);
			mListRobots.add(robot);
		}
	}

	/**
	 * add a Robot Activity
	 *
	 * @param meteorActivity
	 * @param apiAccessor
	 */
	public void addActivity(final MeteorActivity meteorActivity, final APIAccessor apiAccessor) {
		if (meteorActivity.mNumberOfRobots == 0) {
			meteorActivity.mNumberOfRobots = 1;
		}
		for (int i = 0; i < meteorActivity.mNumberOfRobots; i++) {
			final MeteorRobotActivity robot = (MeteorRobotActivity) MeteorRobot.getInstance(this, MeteorRobot.RobotType.PLAYACTIVITY, apiAccessor);
			robot.mRobotId = i + 1;
			robot.setParameters(meteorActivity);
			mListRobots.add(robot);
		}
	}

	/**
	 * add a Robot Scenario
	 */
	public void addScenario(final Scenario meteorScenario, final APIAccessor apiAccessor) {
		if (meteorScenario.mNumberOfRobots == 0) {
			meteorScenario.mNumberOfRobots = 1;
		}
		for (int i = 0; i < meteorScenario.mNumberOfRobots; i++) {
			MeteorRobot robot = null;
			if (meteorScenario.mType == TYPESCENARIO.CMD) {
				robot = MeteorRobot.getInstance(this, RobotType.CMDSCENARIO, apiAccessor);
				((MeteorRobotCmdScenario) robot).setScenario(meteorScenario);
			}
			if (meteorScenario.mType == TYPESCENARIO.GRV) {
				robot = MeteorRobot.getInstance(this, RobotType.GRVSCENARIO, apiAccessor);
				((MeteorRobotGroovyScenario) robot).setScenario(meteorScenario);
			}
			if (robot != null) {
				robot.mRobotId = i + 1;
				mListRobots.add(robot);
			}
		}
	}

	/**
	 * add a simulation for the User activity
	 *
	 * @param applicationConnection
	 * @param processDefinition
	 */
	public void addUserActivity(final MeteorProcessDefinitionUser meteorUser, final APIAccessor apiAccessor) {

		for (int i = 0; i < meteorUser.mNumberOfThread; i++) {
			final MeteorRobotUserActivity robot = (MeteorRobotUserActivity) MeteorRobot.getInstance(this, RobotType.USERACTIVITY, apiAccessor);

			robot.mRobotId = i + 1;
			robot.setParameters(meteorUser);

			mListRobots.add(robot);
		}
	}

	public void setDurationOfSimulation(final int timeInMn) {
		mTimeEndOfTheSimulation = System.currentTimeMillis() + timeInMn * 60 * 1000;
	}

	/**
	 * run the simulation !
	 */
	public void runTheSimulation() {
		logger.info("&~~~~~~~& MeteorSimulation [" + mId + "] ****************   RUN ********* the robots [" + mListRobots.size() + "] ! ");
		mStatus = STATUS.STARTED;
		mDateBeginSimulation = new Date();

		for (final MeteorRobot singleRobot : mListRobots) {
			singleRobot.start();
		}
	}

	private class OneTimeAnswer {

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

	/**
	 * calculate a HTML information on the running system
	 *
	 * @return
	 */
	public Map<String, Object> getDetailStatus() {
		logger.info("&~~~~~~~& MeteorSimulation [" + mId + "] Start currenStatusexecution");
		final HashMap<String, Object> result = new HashMap<String, Object>();
		final List<Map<String, Object>> listResultRobots = new ArrayList<Map<String, Object>>();

		Date dateEndRobots=null;
		int nbOperationRealized = 0;
		int totalSteps=0;
		long totalStepsTime = 0;
		Long currentTime = System.currentTimeMillis();
		final List<OneTimeAnswer> listTimeAnswers = new ArrayList<OneTimeAnswer>();
		
		Estimation estimation = getEstimatedAdvance();
		
		int nbErrors=0;
		for (final MeteorRobot robot : mListRobots) {
			if (robot.getStatus() != RobotStatus.DONE) {
				logger.info("&~~~~~~~& MeteorSimulation [" + mId + "] getCurrentStatusExection robot [" + robot.mRobotId + "] status[" + robot.getStatus() + "] StillAlive=" + estimation.robotsStillAlive);
			}
			else
			{
				if (dateEndRobots==null || dateEndRobots.before(robot.getEndDate()))
					dateEndRobots = robot.getEndDate();
			}
			nbErrors+= robot.getNbErrors();
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

		result.put("robots", listResultRobots);
		result.put("nbErrors", nbErrors);
		
		final long divisor = 1024 * 1024;
		final long totalMemoryInMb = Runtime.getRuntime().totalMemory() / divisor;
		final long freeMemoryInMb = Runtime.getRuntime().freeMemory() / divisor;
		String total = "TOTAL : " + totalSteps + " step in " + MeteorToolbox.getHumanDelay( totalStepsTime) + " ms Mem: " + (totalMemoryInMb - freeMemoryInMb) + "/" + totalMemoryInMb + " : " + (int) (100.0 * (totalMemoryInMb - freeMemoryInMb) / totalMemoryInMb) + " % ";
		if (totalSteps > 0) {
			total += " average " + totalStepsTime / totalSteps + " ms";
		}
		result.put("total",total);

		result.put("memUsedMb", totalMemoryInMb - freeMemoryInMb);
		result.put("memTotalMb", totalMemoryInMb );
		result.put("memPercent", (int) (100.0 * (totalMemoryInMb - freeMemoryInMb) / totalMemoryInMb));
		result.put("timeStarted", MeteorToolbox.getHumanDate(mDateBeginSimulation));
		logger.info("&~~~~~~~& MeteorSimulation [" + mId + "] currenStatusexecution : Robot Still Alive " + estimation.robotsStillAlive);

		if (!estimation.robotsStillAlive) {
			mStatus = STATUS.DONE;
			if (mDateEndSimulation == null) {
				mDateEndSimulation = dateEndRobots;
			}
			if (mDateEndSimulation !=null)
				result.put("timeEnded", MeteorToolbox.getHumanDate(mDateEndSimulation));
		} else {

			// calculate an estimation based on the slowest robot
			result.put( cstJsonPercentAdvance, estimation.percentAdvance);
			if (estimation.percentAdvance == 0) {
				// we can't do any calculation
			}
			if (estimation.percentAdvance > 0) {
				result.put( cstJsonTimeEstimatedDelay, MeteorToolbox.getHumanDelay( estimation.timeNeedInMs));
				result.put( cstJsonTimeEstimatedEnd, MeteorToolbox.getHumanDate( new Date(currentTime + estimation.timeNeedInMs)));
			}
		}

		result.put(cstJsonStatus, mStatus.toString());
		// chart
		/*
		 * status.
		 * append("<canvas id=\"myChart\" width=\"600\" height=\"200\"></canvas>"
		 * ); status.append("<script src=\"Chart.js\"></script>");
		 * status.append("<script>"); //Get the context of the canvas element we
		 * want to select status.
		 * append("var ctx = document.getElementById(\"myChart\").getContext(\"2d\");\n"
		 * );
		 */
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
		logger.info("MeteorSimulation.currenStatusexecution : END " + result);

		return result;
	}

	public Date getDateBeginSimulation()
	{
		return mDateBeginSimulation;
	}
	public Date getDateEndSimulation()
	{
		return mDateEndSimulation;
	}
	public class Estimation{
		public long timeNeedInMs;
		public Date dateEnd;
		public int percentAdvance;
		public boolean robotsStillAlive;
	}
	
	
	public Estimation getEstimatedAdvance()
	{
		Estimation estimation=new Estimation();
		
		estimation.percentAdvance = 100;
		
		estimation.robotsStillAlive = false;
		
		for (final MeteorRobot robot : mListRobots) {
			if (robot.getStatus() != RobotStatus.DONE) {
				estimation.robotsStillAlive=true;
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
		if (! estimation.robotsStillAlive)
			estimation.percentAdvance=100;
		
		
		
		
		if (estimation.percentAdvance==0)
			return estimation;
		
		final long currentTime = System.currentTimeMillis();
		final long executionTime = currentTime - mDateBeginSimulation.getTime();
		estimation.timeNeedInMs = (long) ( (100.0 - estimation.percentAdvance) * executionTime / estimation.percentAdvance);
		estimation.dateEnd= new Date(currentTime + estimation.timeNeedInMs);
		return estimation;
	}
	
	
	public boolean isRunning() {
		return mStatus == STATUS.STARTED;
	};
}
