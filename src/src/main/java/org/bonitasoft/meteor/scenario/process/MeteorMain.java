package org.bonitasoft.meteor.scenario.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityDefinition;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.HumanTaskDefinition;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.meteor.MeteorSimulation;
import org.bonitasoft.meteor.MeteorToolbox;
import org.json.simple.JSONValue;


/* ******************************************************************** */
/*                                                                      */
/* Main class for the Process execution (list of processes / activities)*/
/*                                                                      */
/* ******************************************************************** */

public class MeteorMain {

	private final Logger logger = Logger.getLogger(MeteorMain.class.getName());

	// todo : rename html => json
	public static String cstJsonNumberOfCases = "nbcases";
	public static String cstHtmlType = "type";
	public static String cstHtmlTypeProcess = "pro";
	public static String cstHtmlTypeActivity = "act";
	public static String cstHtmlTypeUser = "usr";

	public static String cstHtmlId = "id";
	public static String cstHtmlNumberOfRobots = "nbrob";
	public static String cstHtmlTimeSleep = "timesleep";
	public static String cstHtmlDelaySleep = "delaysleep";
	public static String cstHtmlInputPercent = "percent";
	public static String cstHtmlInputContent = "content";
	public static String cstHtmlInputProposeContent = "proposecontent";


	public static String cstHtmlInputs = "inputs";
	public static String cstHtmlUserName = "username";
	public static String cstHtmlUserPassword = "userpassword";
	public static String cstHtmlDocumentName = "documentname";
	public static String cstHtmlDocumentValue = "documentvalue";
	public static String cstJsonProcessName = "processname";
	public static String cstHtmlProcessVersion = "processversion";
	public static String cstHtmlActivityName = "activityname";
	public static String cstHtmlProcessDefId = "processid";
	public static String cstHtmlCoverAll = "coverall";
	public static String cstHtmlCoverPercent = "coverpercent";
	public static String cstHtmlCoverHappyPathPercent = "coverhappypathpercent";
	public static String cstJsonActivitiesNotCovered ="activitiesnotcovered";

	public static String cstJsonListEvents ="listevents";

	
	public static String cstHtmlPrefixActivity = "ACT_";
	public static String cstHtmlPrefixDocument = "DOC_";
	public static int cstCurrentSimulation = 2;

	private static BEvent EventGetListProcesses = new BEvent(MeteorMain.class.getName(), 1, Level.ERROR, "Error while accessing information on process list", "Check Exception ", "The processes presented may be incomplete", "Check Exception");

	private static BEvent EventCalculateListProcess = new BEvent(MeteorMain.class.getName(), 2, Level.SUCCESS, "Collect of processes done with success", "");

	private static BEvent EventCheckRobotCaseIncoherent = new BEvent(MeteorMain.class.getName(), 3, Level.APPLICATIONERROR, "Number of Robots and Cases not coherent", "No robots can start", "No test can be done if the robot=0 and case>0 or if robot>0 and case=0",
			"If you set a number of robot, then set a number of case(or inverse)");

	private static BEvent EventInitializeJson = new BEvent(MeteorMain.class.getName(), 4, Level.APPLICATIONERROR, "Variables can't be decoded", "The variable you gave must be JSON compatible", "The simulation will not start until this error is fixed", "Verify the JSON syntaxe");

	private final boolean mShowActivity = true;

	/**
	 * after the calculation, we get this information : the
	 * listprocessDefinition, the listEventCalculation and the performance
	 */
	final List<BEvent> listEventsCalculation = new ArrayList<BEvent>();
	public long performanceCalcul = 0;
	private final HashMap<Long, MeteorDefProcess> mListProcessDefinition = new HashMap<Long, MeteorDefProcess>();

	/* ******************************************************************** */
	/*                                                                      */
	/* operation on the content 											*/
	/*                                                                      */
	/*                                                                      */
	/* ******************************************************************** */

	/**
	 * Calculate the list of process. Update the current list
	 *
	 * @param processAPI
	 *            to communicate with the bonita engine
	 *
	 * @return nothing : access the getListEventCalculation
	 */
	public List<BEvent> calculateListProcess(final ProcessAPI processAPI) {

		listEventsCalculation.clear();
		final long timeBegin = System.currentTimeMillis();
		try {

			List<ProcessDeploymentInfo> listProcessDeploymentInfos;
			int startIndex = 0;
			do {
				listProcessDeploymentInfos = processAPI.getProcessDeploymentInfos(startIndex, 100, ProcessDeploymentInfoCriterion.NAME_ASC);

				startIndex += 100;
				for (final ProcessDeploymentInfo processDeploymentInfos : listProcessDeploymentInfos) {
					logger.info("ProcessDeployment [" + processDeploymentInfos.getName() + "] state[" + processDeploymentInfos.getActivationState() + "]");
					if (!processDeploymentInfos.getActivationState().equals(ActivationState.ENABLED)) {
						continue;
					}

					try {
						final MeteorDefProcess meteorProcess = new MeteorDefProcess(processDeploymentInfos.getProcessId());
						meteorProcess.mProcessDefinitionId = processAPI.getProcessDefinitionId(processDeploymentInfos.getName(), processDeploymentInfos.getVersion());
						meteorProcess.mProcessName = processDeploymentInfos.getName();
						meteorProcess.mProcessVersion = processDeploymentInfos.getVersion();
						meteorProcess.calculContractDefinition( processAPI );
						
						
						/*
						 * String classPath =
						 * System.getProperty("java.class.path"); String
						 * libraryPath =
						 * System.getProperty("java.library.path"); String
						 * classPath2 = System.getProperty("CLASSPATH");
						 * System.out.println("ClassPath[" + classPath +
						 * "] classpath2["+classPath2+"] library[" + libraryPath
						 * + "]"); logger.severe("ClassPath[" + classPath +
						 * "] library[" + libraryPath + "]");
						 * System.setProperty("java.class.path",
						 * "c:/atelier/Tomcat 6.0_Eclipse/bin/bootstrap.jar;c:/atelier/Tomcat 6.0_Eclipse/bin/tomcat-juli.jar"
						 * ); System.setProperty("java.library.path",
						 * "c:/atelier/Tomcat 6.0_Eclipse/bin;C:/Windows/Sun/Java/bin;C:/Windows/system32;C:/Windows;C:/Program Files/Common Files/Microsoft Shared/Windows Live;C:/Program Files (x86)/Common Files/Microsoft Shared/Windows Live;C:/Program Files (x86)/NVIDIA Corporation/PhysX/Common;C:/Windows/system32;C:/Windows;C:/Windows/System32/Wbem;C:/Windows/System32/WindowsPowerShell/v1.0/;C:/Program Files (x86)/Sony/VAIO Startup Setting Tool;C:/Program Files (x86)/Windows Live/Shared;C:/atelier/Subversion/bin;C:/Program Files (x86)/QuickTime/QTSystem/;;."
						 * );
						 */

						if (mShowActivity) {
							/*
							 * if (cstCurrentSimulation == 1) {
							 * MeteorDefinitionActivity
							 * toolHatProcessDefinitionActivity = new
							 * MeteorDefinitionActivity();
							 * toolHatProcessDefinitionActivity.mActivityName =
							 * "Simulation Step1";
							 * toolHatProcessDefinitionActivity.
							 * mActivityDefinitionId = 333L;
							 * meteorProcessDefinition.mListActivities.add(
							 * toolHatProcessDefinitionActivity);
							 * toolHatProcessDefinitionActivity = new
							 * MeteorDefinitionActivity();
							 * toolHatProcessDefinitionActivity.mActivityName =
							 * "Simulation Step3";
							 * toolHatProcessDefinitionActivity.
							 * mActivityDefinitionId = 543L;
							 * meteorProcessDefinition.mListActivities.add(
							 * toolHatProcessDefinitionActivity); } else if
							 * (cstCurrentSimulation == 2) {
							 */
							logger.info("ProcessDeployment [" + processDeploymentInfos.getName() + "] state[" + processDeploymentInfos.getActivationState() + "]");

							final DesignProcessDefinition designProcessDefinition = processAPI.getDesignProcessDefinition(meteorProcess.mProcessDefinitionId);
							final FlowElementContainerDefinition flowElement = designProcessDefinition.getFlowElementContainer();
							final List<ActivityDefinition> listActivities = flowElement.getActivities();
							logger.info("listActivities [" + listActivities.size() + "]");

							for (final ActivityDefinition activityDefinition : listActivities) {
						
								if (activityDefinition instanceof HumanTaskDefinition) {
									final MeteorDefActivity meteorActivity = new MeteorDefActivity(meteorProcess, activityDefinition.getId());
									
									meteorActivity.mActivityName = activityDefinition.getName();
									meteorActivity.mActivityDefinitionId = activityDefinition.getId();
									meteorActivity.calculContractDefinition( processAPI );
									meteorProcess.addActivity(meteorActivity);
								}
							}
							/*
							 * } else { final byte[] barOnByte =
							 * processAPI.exportBarProcessContentUnderHome(
							 * processDefinitionId); final ByteArrayInputStream
							 * barOnInput = new ByteArrayInputStream(barOnByte);
							 * // BusinessArchiveFactory businessArchiveFactory
							 * // = // new BusinessArchiveFactory(); final
							 * BusinessArchive businessArchive =
							 * BusinessArchiveFactory.readBusinessArchive(
							 * barOnInput); final DesignProcessDefinition
							 * designProcessDefinition =
							 * businessArchive.getProcessDefinition(); final
							 * FlowElementContainerDefinition flowElement =
							 * designProcessDefinition.getFlowElementContainer()
							 * ; final List<ActivityDefinition> listActivity =
							 * flowElement.getActivities(); for (final
							 * ActivityDefinition activityDefinition :
							 * listActivity) { final MeteorDefinitionActivity
							 * toolHatProcessDefinitionActivity = new
							 * MeteorDefinitionActivity();
							 * toolHatProcessDefinitionActivity.mActivityName =
							 * activityDefinition.getName();
							 * toolHatProcessDefinitionActivity.
							 * mActivityDefinitionId =
							 * activityDefinition.getId();
							 * meteorProcessDefinition.mListActivities.add(
							 * toolHatProcessDefinitionActivity); } }
							 */
						}

						// already exist ?
						mListProcessDefinition.put(meteorProcess.mProcessDefinitionId, meteorProcess);

					} catch (final ProcessDefinitionNotFoundException e) {
						listEventsCalculation.add(new BEvent(EventGetListProcesses, e, ""));
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionDetails = sw.toString();

						logger.severe("ProcessDefinitionNotFoundException " + exceptionDetails);
						/*
						 * } catch (final ProcessExportException e) {
						 * listEvents.add( new
						 * BEvent(EventGetListProcesses,e,""));
						 * logger.severe("ProcessExportException "+e.toString())
						 * ; } catch (final
						 * InvalidBusinessArchiveFormatException e) {
						 * listEvents.add( new
						 * BEvent(EventGetListProcesses,e,"")); logger.
						 * severe("InvalidBusinessArchiveFormatException "+e.
						 * toString()); } catch (final IOException e) {
						 * listEvents.add( new
						 * BEvent(EventGetListProcesses,e,""));
						 * logger.severe("IOException "+e.toString());
						 */
					} catch (final Exception e) {
						listEventsCalculation.add(new BEvent(EventGetListProcesses, e, ""));
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionDetails = sw.toString();

						logger.severe("Exception " + exceptionDetails);
					}

				} // end for
			} while (listProcessDeploymentInfos.size() > 0);
			listEventsCalculation.add(new BEvent(EventCalculateListProcess, mListProcessDefinition.size() + " processes detected"));

		} catch (final Exception e1) {
			final StringWriter sw = new StringWriter();
			e1.printStackTrace(new PrintWriter(sw));
			logger.severe("Error during get Process e=" + sw.toString());
			listEventsCalculation.add(new BEvent(EventGetListProcesses, e1, ""));
		}
		final long timeEnd = System.currentTimeMillis();
		performanceCalcul = timeEnd - timeBegin;
		return listEventsCalculation;
	}

	/**
	 * clear the list
	 */
	public void clear() {
		mListProcessDefinition.clear();
	}

	public HashMap<Long, MeteorDefProcess> getListProcessCalculation() {
		return mListProcessDefinition;
	}

	final public List<BEvent> getListEventCalculation() {
		return listEventsCalculation;
	}

	public long getPerformanceCalculation() {
		return performanceCalcul;
	}

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Get the JSON information */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */

	/**
	 * all information are return as a flat list. The type give the type of the
	 * line
	 */
	public List<Map<String, Object>> toJson(final ListProcessParameter listProcessParameters) {
		logger.info("MeteorProcessDefinitionList: " + listProcessParameters.toString());
		final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

		if (mListProcessDefinition.size() == 0) {
			return result;
		}

		for (final MeteorDefProcess meteorProcess : mListProcessDefinition.values()) {
			final Map<String, Object> oneProcess = meteorProcess.getMap();
			result.add(oneProcess);
			oneProcess.put(cstHtmlType, cstHtmlTypeProcess);
			oneProcess.put("information", meteorProcess.getInformation());
			if (listProcessParameters.mShowCreateCases) {

				if (listProcessParameters.mShowDocuments) {
					final ArrayList<HashMap<String, Object>> listDocuments = new ArrayList<HashMap<String, Object>>();
					oneProcess.put("listdocuments", listDocuments);

					for (final MeteorDocument toolHatProcessDefinitionDocument : meteorProcess.mListDocuments) {
						final HashMap<String, Object> oneDocument = new HashMap<String, Object>();
						listDocuments.add(oneDocument);
						oneDocument.put("documentname", toolHatProcessDefinitionDocument.mDocumentName);
					}
				}
			}

			// for each activity
			if (listProcessParameters.mShowActivity) {
				final List<Map<String, Object>> listActivities = new ArrayList<Map<String, Object>>();
				oneProcess.put("activities", listActivities);
				for (final MeteorDefActivity meteorActivity : meteorProcess.getListActivities() ) {
					final Map<String, Object> oneActivity = meteorActivity.getMap();
					// oneActivity.put(cstHtmlType, cstHtmlTypeActivity);
					listActivities.add(oneActivity);

					if (listProcessParameters.mShowDocuments) {
						final ArrayList<HashMap<String, Object>> listDocuments = new ArrayList<HashMap<String, Object>>();
						oneProcess.put("listdocuments", listDocuments);
						for (final MeteorDocument meteorDocument : meteorActivity.mListDocuments) {
							final HashMap<String, Object> oneDocument = new HashMap<String, Object>();
							listDocuments.add(oneDocument);
							oneDocument.put("documentname", meteorDocument.mDocumentName);
						}
					}
				}
				// for each user
				/*
				 * if (listProcessParameters.mShowUsers) { for (final
				 * ToolHatProcessDefinitionUser toolHatProcessDefinitionUser :
				 * meteorProcessDefinition.mListUsers) { final HashMap<String,
				 * Object> oneUser = new HashMap<String, Object>();
				 * oneUser.put(cstHtmlType, cstHtmlTypeUser);
				 * result.add(oneUser); oneUser.put("username",
				 * toolHatProcessDefinitionUser.mUserName);
				 * oneUser.put("numberofthread",
				 * toolHatProcessDefinitionUser.mNumberOfThread);
				 * oneUser.put("numberofcase",
				 * toolHatProcessDefinitionUser.mNumberOfCase);
				 * oneUser.put("timesleep",
				 * toolHatProcessDefinitionUser.mTimeSleep);
				 * oneUser.put("variablestring",
				 * toolHatProcessDefinitionUser.mVariablesString);
				 * oneUser.put("username",
				 * toolHatProcessDefinitionUser.mUserName);
				 * oneUser.put("userpassword",
				 * toolHatProcessDefinitionUser.mUserPassword); } }
				 */
			}
		}
		return result;
	}

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Update the information on the listprocess */
	/* user give some value, so we update the information with this one */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */

	/**
	 * @param items
	 * @param listDocuments
	 */
	private void uploadDocuments(final List<FileItem> items, final List<MeteorDocument> listDocuments) {
		if (items == null) {
			return;
		}
		// update document
		for (final MeteorDocument toolHatProcessDefinitionDocument : listDocuments) {

			final String widgetDocumentValue = cstHtmlDocumentValue + toolHatProcessDefinitionDocument.getHtmlId();
			final String widgetDocumentName = cstHtmlDocumentName + toolHatProcessDefinitionDocument.getHtmlId();

			for (final FileItem item : items) {
				if (item.isFormField() && item.getFieldName().equals(widgetDocumentName)) {
					toolHatProcessDefinitionDocument.mDocumentName = item.getString();
				}

				if (!item.isFormField() && item.getFieldName().equals(widgetDocumentValue)) {
					// Process form file field (input type="file").

					// String filename = FilenameUtils.getName(item.getName());
					InputStream filecontent;
					try {
						filecontent = item.getInputStream();

						int pos = 0;

						final byte[] bytes = new byte[1024];
						int read;
						while ((read = filecontent.read(bytes)) != -1) {
							if (pos == 0) {
								toolHatProcessDefinitionDocument.mContent = new ByteArrayOutputStream();
							}
							pos = 1;
							toolHatProcessDefinitionDocument.mContent.write(bytes, 0, read);
						}
					} catch (final IOException e) {
						e.printStackTrace();
					}

				} // end check item

			} // enf fileITem loop
		}
	}

	/**
	 * build the list as HTML
	 *
	 * @return
	 */

	public static class ListProcessParameter {

		public boolean mShowCreateCases = true;
		public boolean mShowActivity = true;
		public boolean mShowUsers = true;
		public boolean mShowVariables = true;
		public boolean mShowDocuments = true;

		public static ListProcessParameter getInstanceFromJsonSt(final String jsonSt) {
			final ListProcessParameter listProcessParameter = new ListProcessParameter();
			if (jsonSt == null) {
				return listProcessParameter;
			}
			final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);

			listProcessParameter.mShowCreateCases = jsonHash.get("showcreatecases") == null ? false : (Boolean) jsonHash.get("showcreatecases");
			listProcessParameter.mShowActivity = jsonHash.get("showactivities") == null ? false : (Boolean) jsonHash.get("showactivities");
			listProcessParameter.mShowUsers = jsonHash.get("showusers") == null ? false : (Boolean) jsonHash.get("showusers");
			listProcessParameter.mShowVariables = jsonHash.get("showvariables") == null ? false : (Boolean) jsonHash.get("showvariables");
			listProcessParameter.mShowDocuments = jsonHash.get("showdocuments") == null ? false : (Boolean) jsonHash.get("showdocuments");
			return listProcessParameter;
		}

		@Override
		public String toString() {
			return "ShowCreateCase=" + mShowCreateCases + ",ShowActivity=" + mShowActivity + ",ShowUsers=" + mShowUsers + ",ShowVariables=" + mShowVariables + ",ShowDocument=" + mShowDocuments;
		}

	}

	/**
	 * update the list to get what the user ask to simulate.
	 * ATTENTION : no processAPI can be done here, we have to wait the robot execution
	 * 
	 * @param listOfFlatInformation
	 * @param listRequestMultipart
	 */
	public List<BEvent> fromList(final List<Map<String, Object>> listOfFlatInformation, final List<FileItem> listRequestMultipart, ProcessAPI processAPI) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		if (listOfFlatInformation == null) {
			logger.severe("no Update information (listOfProcess is null");
			// add a event here
			return listEvents;
		}

		for (final Map<String, Object> oneProcess : listOfFlatInformation) {
			final String type = MeteorToolbox.getParameterString(oneProcess, cstHtmlType, null);
			if (cstHtmlTypeProcess.equals(type)) {

				final MeteorDefProcess meteorProcess = MeteorDefProcess.getInstanceFromMap(oneProcess, processAPI);

				mListProcessDefinition.put(meteorProcess.mProcessDefinitionId, meteorProcess);

				// update document
				if (listRequestMultipart != null) {
					uploadDocuments(listRequestMultipart, meteorProcess.mListDocuments);
				}
				logger.info("Update processId[" + meteorProcess.mProcessDefinitionId + "] ProcessName[" + meteorProcess.mProcessName + "-" + meteorProcess.mProcessVersion + "] nbThread[" + meteorProcess.mNumberOfRobots + "] nbCase[" + meteorProcess.mNumberOfCases + "]");

				// upload each activity
				List<Map<String, Object>> listActivities = (List<Map<String, Object>>) oneProcess.get("activities");
				if (listActivities == null)
					continue;
				for (Map<String, Object> oneActivity : listActivities) {
					final MeteorDefActivity meteorActivity = MeteorDefActivity.getInstanceFromMap(meteorProcess, oneActivity, processAPI);
					meteorProcess.addActivity( meteorActivity);
				}

			}
		}
		return listEvents;
	}

	/**
	 * check and resove everything to be ready to start
	 * 
	 * @param apiAccessor
	 * @param tenantId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<BEvent> initialize(final long tenantId) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		String analysis = "";
		boolean somethingToStart = false;

		for (final MeteorDefProcess meteorProcessDefinition : mListProcessDefinition.values()) {
			try {
				analysis += "Process[" + meteorProcessDefinition.mProcessName + "]-[" + meteorProcessDefinition.mProcessVersion + "] ID[" + meteorProcessDefinition.mProcessDefinitionId + "] :";
				// the processDefition is just a container, do the job now
				if (meteorProcessDefinition.mNumberOfRobots == 0 && meteorProcessDefinition.mNumberOfCases > 0 || meteorProcessDefinition.mNumberOfRobots > 0 && meteorProcessDefinition.mNumberOfCases == 0) {
					listEvents.add(new BEvent(EventCheckRobotCaseIncoherent,
							"Process Name[" + meteorProcessDefinition.mProcessName + "] Version[" + meteorProcessDefinition.mProcessVersion + "] NumberOfRobots[" + meteorProcessDefinition.mNumberOfRobots + "] Nb case[" + meteorProcessDefinition.mNumberOfCases + "]"));
				}
				if (meteorProcessDefinition.mNumberOfRobots > 0 && meteorProcessDefinition.mNumberOfCases > 0) {
					analysis += "Start_CASE[" + meteorProcessDefinition.mNumberOfCases + "] by nbRob[" + meteorProcessDefinition.mNumberOfRobots + "]";
					somethingToStart = true;
				}

				// Activity Part
				for (final MeteorDefActivity meteorDefinitionActivity : meteorProcessDefinition.getListActivities()) {
					analysis = "On Process [" + meteorProcessDefinition.mProcessName + "] [" + meteorProcessDefinition.mProcessVersion + "], Activity[" + meteorDefinitionActivity.mActivityName + "]";

					if (meteorDefinitionActivity.mNumberOfRobots == 0 && meteorDefinitionActivity.mNumberOfCases > 0 || meteorDefinitionActivity.mNumberOfRobots > 0 && meteorDefinitionActivity.mNumberOfCases == 0) {
						listEvents.add(new BEvent(EventCheckRobotCaseIncoherent, "Process Name[" + meteorProcessDefinition.mProcessName + "] Version[" + meteorProcessDefinition.mProcessVersion + "] " + "] Activity[" + meteorDefinitionActivity.mActivityName + "] NumberOfRobots["
								+ meteorProcessDefinition.mNumberOfRobots + "] Nb case[" + meteorProcessDefinition.mNumberOfCases + "]"));
					}
					if (meteorDefinitionActivity.mNumberOfRobots > 0 && meteorDefinitionActivity.mNumberOfCases > 0) {
						analysis += "Execute_Act[" + meteorDefinitionActivity.mActivityName + "] in Process[" + meteorProcessDefinition.mProcessName + "]-[" + meteorProcessDefinition.mProcessVersion + "] ID[" + meteorProcessDefinition.mProcessDefinitionId + "] start["
								+ meteorDefinitionActivity.mNumberOfCases + "] by nbRob[" + meteorDefinitionActivity.mNumberOfRobots + "];";

						somethingToStart = true;
					}
				}
				analysis += ";";
			} catch (final Exception e) {
				listEvents.add(new BEvent(EventInitializeJson, e, analysis));
				logger.severe("initialize " + e.toString());
			}
			if (!somethingToStart) {
				// it's possible if we have a scenario
				// listEvents.add(new BEvent(EventCheckNothingToStart, "Nothing
				// to start"));
			}
			logger.info("MeteorInitialize: " + analysis);
			for (final BEvent event : listEvents) {
				logger.info("checkParameter :" + event.toString());
			}

		}
		return listEvents;
	}

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Register in the MeteorSimulation */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */
	public List<BEvent> registerInSimulation(final MeteorSimulation meteorSimulation, final APIAccessor apiAccessor) {
		final List<BEvent> listEvents = new ArrayList<BEvent>();
		for (final MeteorDefProcess meteorProcess : mListProcessDefinition.values()) {
			if (meteorProcess.mNumberOfRobots > 0) {
				logger.info("Add process[" + meteorProcess.mProcessName + "] in the startCase");
				meteorSimulation.addProcess(meteorProcess, apiAccessor);
			}
			// check activity
			for (final MeteorDefActivity meteorActivity : meteorProcess.getListActivities()) {
				if (meteorActivity.mNumberOfRobots > 0) {
					meteorSimulation.addActivity(meteorActivity, apiAccessor);
				}
			}
		}
		return listEvents;

	}

}
