package org.bonitasoft.meteor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.meteor.MeteorSimulation.CollectPerformance;
import org.bonitasoft.meteor.MeteorSimulation.LogExecution;
import org.bonitasoft.meteor.scenario.cmd.MeteorRobotCmdScenario;
import org.bonitasoft.meteor.scenario.groovy.MeteorRobotGroovyScenario;
import org.bonitasoft.meteor.scenario.process.MeteorRobotActivity;
import org.bonitasoft.meteor.scenario.process.MeteorRobotCreateCase;


public abstract class MeteorRobot implements Runnable {

	public static Logger logger = Logger.getLogger(MeteorRobot.class.getName());

	protected enum RobotType {
		CREATECASE, PLAYACTIVITY, CMDSCENARIO, GRVSCENARIO
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

	/**
	 * when the robot start
	 */
	private Date mDateBegin;
	/**
	 * when the robot stop
	 */
	private Date mDateEnd;
	public int mRobotId;
	// public MeteorDefinitionActivity mToolHatProcessDefinitionActivity;
	// public MeteorProcessDefinitionUser mToolHatProcessDefinitionUser;
	// public ArrayList<ToolHatProcessDefinitionDocument> mListDocuments;

	private final APIAccessor apiAccessor;
	protected MeteorSimulation meteorSimulation;

	public CollectPerformance mCollectPerformance = new CollectPerformance();

	/**
	 * robot can log here the execution detail, error it face; etc...
	 */
	public LogExecution mLogExecution = new LogExecution();

	protected MeteorRobot(MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
		this.apiAccessor = apiAccessor;
		this.meteorSimulation = meteorSimulation;
		mStatus = RobotStatus.INACTIF;
	}

	public static MeteorRobot getInstance(MeteorSimulation meteorSimulation, final RobotType robotType, final APIAccessor apiAccessor) {
		if (robotType == RobotType.CREATECASE) {
			return new MeteorRobotCreateCase(meteorSimulation, apiAccessor);
		} else if (robotType == RobotType.PLAYACTIVITY) {
			return new MeteorRobotActivity(meteorSimulation, apiAccessor);
		} else if (robotType == RobotType.CMDSCENARIO) {
			return new MeteorRobotCmdScenario(meteorSimulation, apiAccessor);
		} else if (robotType == RobotType.GRVSCENARIO) {
			return new MeteorRobotGroovyScenario(meteorSimulation, apiAccessor);
		}
		return null;

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

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Status */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */

	public FINALSTATUS mFinalStatus;

	public void setFinalStatus(final FINALSTATUS finalStatus) {
		mFinalStatus = finalStatus;
	}

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
		final Thread T = new Thread(this);
		T.start();
	}

	public void run() {

		logger.info("----------- Start robot #" + mRobotId + " type[" + this.getClass().getName() + "]");
		mStatus = RobotStatus.STARTED;

		mCollectPerformance.clear();
		try {
			// log in to the tenant to create a session
			mDateBegin = new Date();
			executeRobot();
		} catch (Exception e) {
			final StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			setFinalStatus(FINALSTATUS.FAIL);
			logger.severe("Robot " + getSignature() + " exception " + e.toString() + " at " + sw.toString());
			mLogExecution.addLog("Exception " + e.toString());

		} catch (Error er) {
			final StringWriter sw = new StringWriter();
			er.printStackTrace(new PrintWriter(sw));
			setFinalStatus(FINALSTATUS.FAIL);

			logger.severe("Robot " + getSignature() + " exception " + er.toString() + " at " + sw.toString());
			mLogExecution.addLog("Exception " + er.toString());

		}

		mStatus = RobotStatus.DONE;
		mCollectPerformance.mOperationIndex = mCollectPerformance.mOperationTotal; // set
																					// to
																					// 100%

		mDateEnd = new Date();

		logger.info("----------- End robot #" + mRobotId + " type[" + getClass().getName() + "]");

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

	public String getSignature() {
		return "#" + mRobotId + " " + mSignatureInfo;
	}

	public void setSignatureInfo(String info) {
		mSignatureInfo = info;
	}

	/**
	 * get the information
	 *
	 * @return
	 */
	public RobotStatus getStatus() {
		return mStatus;
	}

	/**
	 * @return
	 */
	public Map<String, Object> getDetailStatus() {
		final Map<String, Object> resultRobot = new HashMap<String, Object>();

		resultRobot.put("title", mCollectPerformance.mTitle); // mProcessDefinition.getInformation()+"
																// #"+mRobotId+"
																// ";
		resultRobot.put("id", mRobotId); // mProcessDefinition.getInformation()+"
											// #"+mRobotId+" ";
		int percent = 0;
		resultRobot.put(MeteorSimulation.cstJsonStatus, mStatus.toString());
		resultRobot.put("finalstatus", mFinalStatus == null ? "" : mFinalStatus.toString());
		resultRobot.put("log", mLogExecution.getLogExecution());
		resultRobot.put(MeteorSimulation.cstJsonNbErrors, mLogExecution.getNbErrors());

		if (mCollectPerformance.mOperationTotal == -1) {
			if (mStatus == RobotStatus.DONE) {
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

		resultRobot.put(MeteorSimulation.cstJsonPercentAdvance, percent);
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
