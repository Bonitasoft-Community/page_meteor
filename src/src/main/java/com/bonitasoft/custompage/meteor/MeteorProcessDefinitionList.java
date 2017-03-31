package com.bonitasoft.custompage.meteor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
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
import org.json.simple.JSONValue;

public class MeteorProcessDefinitionList {

    private final Logger logger = Logger.getLogger(MeteorProcessDefinition.class.getName());

    public static String cstHtmlNumberOfCases = "nbcases";
    public static String cstHtmlType = "type";
    public static String cstHtmlTypeProcess = "pro";
    public static String cstHtmlTypeActivity = "act";
    public static String cstHtmlTypeUser = "usr";

    public static String cstHtmlId = "processid";
    public static String cstHtmlNumberOfRobots = "nbrob";
    public static String cstHtmlTimeSleep = "timesleep";
    public static String cstHtmlVariableString = "Variables";
    public static String cstHtmlUserName = "username";
    public static String cstHtmlUserPassword = "userpassword";
    public static String cstHtmlDocumentName = "documentname";
    public static String cstHtmlDocumentValue = "documentvalue";

    public static String cstHtmlPrefixActivity = "ACT_";
    public static String cstHtmlPrefixDocument = "DOC_";
    public static int cstCurrentSimulation = 2;

    private static BEvent EventGetListProcesses = new BEvent(MeteorProcessDefinitionList.class.getName(), 1, Level.ERROR,
            "Error while accessing information on process list", "Check Exception ",
            "The processes presented may be incomplete", "Check Exception");

    private static BEvent EventCalculateListProcess = new BEvent(MeteorProcessDefinitionList.class.getName(), 2, Level.SUCCESS,
            "Collect of processes done with success", "");

    private static BEvent EventCheckRobotCaseIncoherent = new BEvent(MeteorProcessDefinitionList.class.getName(), 3, Level.APPLICATIONERROR,
            "Number of Robots and Cases not coherent", "No robots can start", "No test can be done if the robot=0 and case>0 or if robot>0 and case=0",
            "If you set a number of robot, then set a number of case(or inverse)");


    private final  boolean mShowActivity=true;

    /**
     * after the calculation, we get this information : the listprocessDefinition, the listEventCalculation and the performance
     */
    final List<BEvent> listEventsCalculation = new ArrayList<BEvent>();
    public long performanceCalcul=0;
    private final HashMap<Long, MeteorProcessDefinition> mListProcessDefinition = new HashMap<Long, MeteorProcessDefinition>();



    /* ******************************************************************************** */
    /*                                                                                  */
    /* Internal class */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */


    /**
     * describe a user
     *
     */
    public static class MeteorProcessDefinitionUser {

        public Long mProcessDefinitionId;
        public long mNumberOfThread;
        public long mTimeSleep;
        public long mNumberOfCase;
        public String mVariablesString = "";
        public String mUserName = "";
        public String mUserPassword = "";
        public int mDefinitionId = 0;
        public HashMap<String, Object> mVariables = new HashMap<String, Object>();
        public ArrayList<meteorDocument> mListDocuments = new ArrayList<meteorDocument>();

        public MeteorProcessDefinitionUser(final int definitionId, final Long processDefinitionId) {
            mDefinitionId = definitionId;
            mProcessDefinitionId = processDefinitionId;
        }

        public String getHtmlId() {
            return cstHtmlPrefixActivity + mProcessDefinitionId.toString() + mDefinitionId;
        }
    }

    public static class meteorDocument {

        public Long mProcessDefinitionId;

        public Long mActivityDefinitionId;

        public String mDocumentName = "";
        public int mIndice;
        public String mFileName = "";
        public ByteArrayOutputStream mContent;

        public meteorDocument(final Long processDefinitionId, final Long activityDefinitionId, final int indice) {
            mProcessDefinitionId = processDefinitionId;
            mActivityDefinitionId = activityDefinitionId;
            mIndice = indice;
        }

        public String getHtmlId() {
            return cstHtmlPrefixDocument + (mProcessDefinitionId == null ? "" : mProcessDefinitionId.toString()) + "_"
                    + (mActivityDefinitionId == null ? "" : mActivityDefinitionId.toString()) + "_" + mIndice;

        }
    }

    /**
     * describe a Human activity Inside a process.
     * Then, this human activity can be process by one or multiple robot.
     */

    public static class MeteorActivity {

        public Long mActivityDefinitionId;
        public String mActivityName;
        // this is the robot part : how many robot do we have to start on this activity ?
        public long mNumberOfRobots;
        public long mTimeSleep;
        public long mNumberOfCases;

        public String mVariablesString = "";
        public Map<String, Serializable> mInputs = new HashMap<String, Serializable>();
        public ArrayList<meteorDocument> mListDocuments = new ArrayList<meteorDocument>();

        public String getHtmlId() {
            return cstHtmlPrefixActivity + (mActivityDefinitionId == null ? "#" : mActivityDefinitionId.toString());
        }

        public String getInformation()
        {
            return mActivityName;
        }
    }

    /**
     * define a process to simulate it
     *
     */
    public static class MeteorProcessDefinition {

        public Long mProcessDefinitionId;
        public String mProcessName;

        public String mProcessVersion;

        public long mNumberOfRobots;
        public long mNumberOfCases;
        public long mTimeSleep;

        public String mVariablesString = "";
        public Map<String, Object> mVariables = new HashMap<String, Object>();

        public List<meteorDocument> mListDocuments = new ArrayList<meteorDocument>();
        public List<MeteorActivity> mListActivities = new ArrayList<MeteorActivity>();
        public List<MeteorProcessDefinitionUser> mListUsers = new ArrayList<MeteorProcessDefinitionUser>();


        public String getInformation() {
            return mProcessName + "(" + mProcessVersion + ")";
        }

        /** return an activity */
        public MeteorActivity getActivity(final String activityName) {
            for (final MeteorActivity toolHatProcessDefinitionActivity : mListActivities) {
                if (toolHatProcessDefinitionActivity.mActivityName.equals(activityName)) {
                    return toolHatProcessDefinitionActivity;
                }
            }
            return null;
        }

        public Map<String, Object> getMap()
        {
            final Map<String, Object> oneProcess = new HashMap<String, Object>();

            // attention : the processdefinitionId is very long it has to be set in STRING else JSON will do an error
            oneProcess.put(cstHtmlId, mProcessDefinitionId.toString());
            oneProcess.put(cstHtmlNumberOfRobots, mNumberOfRobots);
            oneProcess.put(cstHtmlNumberOfCases, mNumberOfCases);
            oneProcess.put(cstHtmlTimeSleep, mTimeSleep);
            oneProcess.put(cstHtmlVariableString, mVariablesString);
            oneProcess.put("processname", mProcessName);
            oneProcess.put("processversion", mProcessVersion);
            return oneProcess;
        }

        public void fromMap(final Map<String, String> oneProcess)
        {

            // attention : the processdefinitionId is very long it has to be set in STRING else JSON will do an error
            mProcessDefinitionId = MeteorToolbox.getParameterLong(oneProcess, cstHtmlId, -1);
            mNumberOfRobots = MeteorToolbox.getParameterLong(oneProcess, cstHtmlNumberOfRobots, 0);
            mNumberOfCases = MeteorToolbox.getParameterLong(oneProcess, cstHtmlNumberOfCases, 0);
            mTimeSleep = MeteorToolbox.getParameterLong(oneProcess, cstHtmlTimeSleep, 0);
            mVariablesString = MeteorToolbox.getParameterString(oneProcess, cstHtmlVariableString, "");
            mProcessName = MeteorToolbox.getParameterString(oneProcess, "processname", "");
            mProcessVersion = MeteorToolbox.getParameterString(oneProcess, "processversion", "");
        }

    }




    /* ******************************************************************************** */
    /*                                                                                  */
    /* operation on the content */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * Calculate the list of process. Update the current list
     *
     * @param processAPI to communicate with the bonita engine
     *
     * @return nothing : access the getListEventCalculation
     */
    public List<BEvent> calculateListProcess(final ProcessAPI processAPI) {

        listEventsCalculation.clear();
        final long timeBegin = System.currentTimeMillis();
        try {

            List<ProcessDeploymentInfo> listProcessDeploymentInfos = null;
            int startIndex = 0;
            do {
                listProcessDeploymentInfos = processAPI.getProcessDeploymentInfos(startIndex, 100, ProcessDeploymentInfoCriterion.NAME_ASC);
                startIndex += 100;
                for (final ProcessDeploymentInfo processDeploymentInfos : listProcessDeploymentInfos) {
                    logger.info("ProcessDeployment ["+processDeploymentInfos.getName()+"] state["+processDeploymentInfos.getActivationState()+"]");
                    if (!processDeploymentInfos.getActivationState().equals(ActivationState.ENABLED)) {
                        continue;
                    }

                    final MeteorProcessDefinition meteorProcessDefinition = new MeteorProcessDefinition();
                    Long processDefinitionId;
                    try {
                        processDefinitionId = processAPI.getProcessDefinitionId(processDeploymentInfos.getName(), processDeploymentInfos.getVersion());
                        meteorProcessDefinition.mProcessDefinitionId = processDefinitionId;
                        meteorProcessDefinition.mProcessName = processDeploymentInfos.getName();
                        meteorProcessDefinition.mProcessVersion = processDeploymentInfos.getVersion();

                        // search all human activity

                        // ProcessDefinition processDefinition =
                        // processAPI.getProcessDefinition(processDefinitionId)
                        // ;
                        // bon, le processDefinition ne sert a rien
                        /*
                         * FileOutputStream file = new
                         * FileOutputStream("c:/tmp/ee.rar");
                         * file.write(barOnByte); file.close();
                         */
                        final MeteorProcessDefinitionUser toolHatProcessDefinitionUser = new MeteorProcessDefinitionUser(
                                meteorProcessDefinition.mListUsers.size(), processDefinitionId);
                        meteorProcessDefinition.mListUsers.add(toolHatProcessDefinitionUser);

                        // get documents

                        /*
                         * String classPath =
                         * System.getProperty("java.class.path"); String
                         * libraryPath =
                         * System.getProperty("java.library.path");
                         * String classPath2 = System.getProperty("CLASSPATH");
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
                             * MeteorDefinitionActivity toolHatProcessDefinitionActivity = new MeteorDefinitionActivity();
                             * toolHatProcessDefinitionActivity.mActivityName = "Simulation Step1";
                             * toolHatProcessDefinitionActivity.mActivityDefinitionId = 333L;
                             * meteorProcessDefinition.mListActivities.add(toolHatProcessDefinitionActivity);
                             * toolHatProcessDefinitionActivity = new MeteorDefinitionActivity();
                             * toolHatProcessDefinitionActivity.mActivityName = "Simulation Step3";
                             * toolHatProcessDefinitionActivity.mActivityDefinitionId = 543L;
                             * meteorProcessDefinition.mListActivities.add(toolHatProcessDefinitionActivity);
                             * } else if (cstCurrentSimulation == 2) {
                             */
                                logger.info("ProcessDeployment ["+processDeploymentInfos.getName()+"] state["+processDeploymentInfos.getActivationState()+"]");

                                final DesignProcessDefinition designProcessDefinition = processAPI.getDesignProcessDefinition(processDefinitionId);
                                final FlowElementContainerDefinition flowElement = designProcessDefinition.getFlowElementContainer();
                                final List<ActivityDefinition> listActivity = flowElement.getActivities();
                            logger.info("listActivities [" + listActivity.size() + "]");

                                for (final ActivityDefinition activityDefinition : listActivity) {
                                if (activityDefinition instanceof HumanTaskDefinition)
                                {
                                    final MeteorActivity meteorProcessDefinitionActivity = new MeteorActivity();
                                    meteorProcessDefinitionActivity.mActivityName = activityDefinition.getName();
                                    meteorProcessDefinitionActivity.mActivityDefinitionId = activityDefinition.getId();
                                    meteorProcessDefinition.mListActivities.add(meteorProcessDefinitionActivity);
                                }
                                }
                            /*
                             * }
                             * else
                             * {
                             * final byte[] barOnByte = processAPI.exportBarProcessContentUnderHome(processDefinitionId);
                             * final ByteArrayInputStream barOnInput = new ByteArrayInputStream(barOnByte);
                             * // BusinessArchiveFactory businessArchiveFactory
                             * // =
                             * // new BusinessArchiveFactory();
                             * final BusinessArchive businessArchive = BusinessArchiveFactory.readBusinessArchive(barOnInput);
                             * final DesignProcessDefinition designProcessDefinition = businessArchive.getProcessDefinition();
                             * final FlowElementContainerDefinition flowElement = designProcessDefinition.getFlowElementContainer();
                             * final List<ActivityDefinition> listActivity = flowElement.getActivities();
                             * for (final ActivityDefinition activityDefinition : listActivity) {
                             * final MeteorDefinitionActivity toolHatProcessDefinitionActivity = new MeteorDefinitionActivity();
                             * toolHatProcessDefinitionActivity.mActivityName = activityDefinition.getName();
                             * toolHatProcessDefinitionActivity.mActivityDefinitionId = activityDefinition.getId();
                             * meteorProcessDefinition.mListActivities.add(toolHatProcessDefinitionActivity);
                             * }
                             * }
                             */
                        }

                        // already exist ?
                        mListProcessDefinition.put(meteorProcessDefinition.mProcessDefinitionId, meteorProcessDefinition);


                    } catch (final ProcessDefinitionNotFoundException e) {
                        listEventsCalculation.add(new BEvent(EventGetListProcesses, e, ""));
                        logger.severe("ProcessDefinitionNotFoundException "+e.toString());
                        /*
                         * } catch (final ProcessExportException e) {
                         * listEvents.add( new BEvent(EventGetListProcesses,e,""));
                         * logger.severe("ProcessExportException "+e.toString());
                         * } catch (final InvalidBusinessArchiveFormatException e) {
                         * listEvents.add( new BEvent(EventGetListProcesses,e,""));
                         * logger.severe("InvalidBusinessArchiveFormatException "+e.toString());
                         * } catch (final IOException e) {
                         * listEvents.add( new BEvent(EventGetListProcesses,e,""));
                         * logger.severe("IOException "+e.toString());
                         */
                    } catch (final Exception e) {
                        listEventsCalculation.add(new BEvent(EventGetListProcesses, e, ""));
                        logger.severe("Exception "+e.toString());
                    }

                } // end for
            } while (listProcessDeploymentInfos != null && listProcessDeploymentInfos.size() > 0);
            listEventsCalculation.add(new BEvent(EventCalculateListProcess, mListProcessDefinition.size() + " processes detected"));

        } catch (final Exception e1) {
            final StringWriter sw = new StringWriter();
            e1.printStackTrace(new PrintWriter(sw));
            logger.severe("Error during get Process e=" + sw.toString());
            listEventsCalculation.add(new BEvent(EventGetListProcesses, e1, ""));
        }
        final long timeEnd = System.currentTimeMillis();
        performanceCalcul =  timeEnd - timeBegin;
        return listEventsCalculation;
    }

    /**
     * clear the list
     */
    public void clear() {
        mListProcessDefinition.clear();
    }

    public HashMap<Long, MeteorProcessDefinition> getListProcessCalculation() {
        return mListProcessDefinition;
    }

    final List<BEvent> getListEventCalculation() {
        return listEventsCalculation;
    }

    public long getPerformanceCalculation()
    {
        return performanceCalcul;
    }


    /* ******************************************************************************** */
    /*                                                                                  */
    /* Get the JSON information                                                         */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */


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

        for (final MeteorProcessDefinition meteorProcessDefinition : mListProcessDefinition.values()) {
            final Map<String, Object> oneProcess = meteorProcessDefinition.getMap();
            result.add(oneProcess);
            oneProcess.put(cstHtmlType, cstHtmlTypeProcess);
            oneProcess.put("information", meteorProcessDefinition.getInformation());
            if (listProcessParameters.mShowCreateCases) {

                if (listProcessParameters.mShowDocuments) {
                    final ArrayList<HashMap<String, Object>> listDocuments = new ArrayList<HashMap<String, Object>>();
                    oneProcess.put("listdocuments", listDocuments);

                    for (final meteorDocument toolHatProcessDefinitionDocument : meteorProcessDefinition.mListDocuments) {
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
                for (final MeteorActivity toolHatProcessDefinitionActivity : meteorProcessDefinition.mListActivities) {
                    final HashMap<String, Object> oneActivity = new HashMap<String, Object>();
                    // oneActivity.put(cstHtmlType, cstHtmlTypeActivity);
                    listActivities.add(oneActivity);
                    oneActivity.put("activityname", toolHatProcessDefinitionActivity.mActivityName);
                    // attention, the activityId is very long it has to be transform in STRING else JSON will mess it
                    oneActivity.put(cstHtmlId, toolHatProcessDefinitionActivity.mActivityDefinitionId.toString());
                    oneActivity.put(cstHtmlNumberOfRobots, toolHatProcessDefinitionActivity.mNumberOfRobots);
                    oneActivity.put(cstHtmlNumberOfCases, toolHatProcessDefinitionActivity.mNumberOfCases);
                    oneActivity.put(cstHtmlTimeSleep, toolHatProcessDefinitionActivity.mTimeSleep);
                    oneActivity.put(cstHtmlVariableString, toolHatProcessDefinitionActivity.mVariablesString);

                    if (listProcessParameters.mShowDocuments) {
                        final ArrayList<HashMap<String, Object>> listDocuments = new ArrayList<HashMap<String, Object>>();
                        oneProcess.put("listdocuments", listDocuments);
                        for (final meteorDocument toolHatProcessDefinitionDocument : toolHatProcessDefinitionActivity.mListDocuments) {
                            final HashMap<String, Object> oneDocument = new HashMap<String, Object>();
                            listDocuments.add(oneDocument);
                            oneDocument.put("documentname", toolHatProcessDefinitionDocument.mDocumentName);
                        }
                    }
                }
                // for each user
                /*
                 * if (listProcessParameters.mShowUsers) {
                 * for (final ToolHatProcessDefinitionUser toolHatProcessDefinitionUser : meteorProcessDefinition.mListUsers) {
                 * final HashMap<String, Object> oneUser = new HashMap<String, Object>();
                 * oneUser.put(cstHtmlType, cstHtmlTypeUser);
                 * result.add(oneUser);
                 * oneUser.put("username", toolHatProcessDefinitionUser.mUserName);
                 * oneUser.put("numberofthread", toolHatProcessDefinitionUser.mNumberOfThread);
                 * oneUser.put("numberofcase", toolHatProcessDefinitionUser.mNumberOfCase);
                 * oneUser.put("timesleep", toolHatProcessDefinitionUser.mTimeSleep);
                 * oneUser.put("variablestring", toolHatProcessDefinitionUser.mVariablesString);
                 * oneUser.put("username", toolHatProcessDefinitionUser.mUserName);
                 * oneUser.put("userpassword", toolHatProcessDefinitionUser.mUserPassword);
                 * }
                 * }
                 */
            }
        }
        return result;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Update the information on the listprocess */
    /* user give some value, so we update the information with this one */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */



    /**
     * @param items
     * @param listDocuments
     */
    private void uploadDocuments(final List<FileItem> items, final List<meteorDocument> listDocuments) {
        if (items == null) {
            return;
        }
        // update document
        for (final meteorDocument toolHatProcessDefinitionDocument : listDocuments) {

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
        public String toString()
        {
            return "ShowCreateCase=" + mShowCreateCases + ",ShowActivity=" + mShowActivity + ",ShowUsers=" + mShowUsers + ",ShowVariables=" + mShowVariables
                    + ",ShowDocument=" + mShowDocuments;
        }

    }


    /**
     * update the list to get what the user ask to simulate
     * @param listOfFlatInformation
     * @param listRequestMultipart
     */
    public List<BEvent> fromList(final List<Map<String, String>> listOfFlatInformation, final List<FileItem> listRequestMultipart) {
        final List<BEvent> listEvents = new ArrayList<BEvent>();
        if (listOfFlatInformation == null) {
            logger.severe("no Update information (listOfProcess is null");
            // add a event here
            return listEvents;
        }


        for (final Map<String, String> oneProcess : listOfFlatInformation) {
            final String type = oneProcess.get(cstHtmlType);
            if (cstHtmlTypeProcess.equals(type)) {

                final MeteorProcessDefinition meteorProcessDefinition  = new MeteorProcessDefinition();
                meteorProcessDefinition.fromMap(oneProcess);
                mListProcessDefinition.put(meteorProcessDefinition.mProcessDefinitionId, meteorProcessDefinition);

                // update document
                if (listRequestMultipart != null) {
                    uploadDocuments(listRequestMultipart, meteorProcessDefinition.mListDocuments);
                }
                logger.info("Update processId[" + meteorProcessDefinition.mProcessDefinitionId
                        + "] ProcessName[" + meteorProcessDefinition.mProcessName + "-" + meteorProcessDefinition.mProcessVersion
                        + "] nbThread[" + meteorProcessDefinition.mNumberOfRobots + "] nbCase[" + meteorProcessDefinition.mNumberOfCases + "]");

                // upload each activity
                /*
                 * for (ToolHatProcessDefinitionActivity
                 * toolHatProcessDefinitionActivity :
                 * toolHatProcessDefinition.mListActivities) {
                 * toolHatProcessDefinitionActivity.mNumberOfThread =
                 * getParameterLong(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlNumberOfThread +
                 * toolHatProcessDefinitionActivity.getHtmlId(), 0);
                 * toolHatProcessDefinitionActivity.mNumberOfCase =
                 * getParameterLong(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlNumberOfCase +
                 * toolHatProcessDefinitionActivity.getHtmlId(), 1);
                 * toolHatProcessDefinitionActivity.mTimeSleep =
                 * getParameterLong(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlTimeSleep +
                 * toolHatProcessDefinitionActivity.getHtmlId(), 0);
                 * toolHatProcessDefinitionActivity.mVariablesString =
                 * getParameterString(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlVariableString +
                 * toolHatProcessDefinitionActivity.getHtmlId(),"");
                 * toolHatProcessDefinitionActivity.mVariables =
                 * getParameterHashMap(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlVariableString +
                 * toolHatProcessDefinitionActivity.getHtmlId(), new
                 * HashMap<String, Object>());
                 * uploadDocuments(listRequestMultipart,
                 * toolHatProcessDefinitionActivity.mListDocuments);
                 * } for (ToolHatProcessDefinitionUser
                 * toolHatProcessDefinitionUser :
                 * toolHatProcessDefinition.mListUsers) {
                 * toolHatProcessDefinitionUser.mNumberOfThread =
                 * getParameterLong(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlNumberOfThread +
                 * toolHatProcessDefinitionUser.getHtmlId(), 0);
                 * toolHatProcessDefinitionUser.mNumberOfCase =
                 * getParameterLong(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlNumberOfCase +
                 * toolHatProcessDefinitionUser.getHtmlId(), 1);
                 * toolHatProcessDefinitionUser.mTimeSleep =
                 * getParameterLong(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlTimeSleep +
                 * toolHatProcessDefinitionUser.getHtmlId(), 0);
                 * toolHatProcessDefinitionUser.mVariablesString =
                 * getParameterString(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlVariableString +
                 * toolHatProcessDefinitionUser.getHtmlId(),"");
                 * toolHatProcessDefinitionUser.mVariables =
                 * getParameterHashMap(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlVariableString +
                 * toolHatProcessDefinitionUser.getHtmlId(), new HashMap<String,
                 * Object>()); toolHatProcessDefinitionUser.mUserName =
                 * getParameterString(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlUserName +
                 * toolHatProcessDefinitionUser.getHtmlId(), "");
                 * toolHatProcessDefinitionUser.mUserPassword =
                 * getParameterString(mapRequestMultipart,
                 * MeteorProcessDefinitionList.cstHtmlUserPassword +
                 * toolHatProcessDefinitionUser.getHtmlId(), "");
                 * }
                 */
            }
        }
        return listEvents;
    }

    /*
     *
     */
    public List<BEvent> checkParameter() {
        final List<BEvent> listEvents = new ArrayList<BEvent>();

        boolean somethingToStart = false;
        for (final MeteorProcessDefinition meteorProcessDefinition : mListProcessDefinition.values())
        {
            if ( meteorProcessDefinition.mNumberOfRobots==0 && meteorProcessDefinition.mNumberOfCases>0
                   || meteorProcessDefinition.mNumberOfRobots>0 && meteorProcessDefinition.mNumberOfCases==0)
            {
                listEvents.add( new BEvent( EventCheckRobotCaseIncoherent,
                        "Process Name[" + meteorProcessDefinition.mProcessName + "] Version[" + meteorProcessDefinition.mProcessVersion
                                + "] NumberOfRobots[" + meteorProcessDefinition.mNumberOfRobots + "] Nb case[" + meteorProcessDefinition.mNumberOfCases + "]"));
            }
            if (meteorProcessDefinition.mNumberOfRobots > 0 && meteorProcessDefinition.mNumberOfCases > 0) {
                somethingToStart = true;
            }
            for (final MeteorActivity meteorDefinitionActivity : meteorProcessDefinition.mListActivities)
            {
                if (meteorDefinitionActivity.mNumberOfRobots == 0 && meteorDefinitionActivity.mNumberOfCases > 0
                        || meteorDefinitionActivity.mNumberOfRobots > 0 && meteorDefinitionActivity.mNumberOfCases == 0)
                {
                    listEvents.add(new BEvent(EventCheckRobotCaseIncoherent,
                            "Process Name[" + meteorProcessDefinition.mProcessName + "] Version[" + meteorProcessDefinition.mProcessVersion + "] "
                                    + "] Activity[" + meteorDefinitionActivity.mActivityName
                                    + "] NumberOfRobots[" + meteorProcessDefinition.mNumberOfRobots + "] Nb case[" + meteorProcessDefinition.mNumberOfCases
                                    + "]"));
                }
                if (meteorDefinitionActivity.mNumberOfRobots > 0 && meteorDefinitionActivity.mNumberOfCases > 0) {
                    somethingToStart=true;
                }
            }
        }
        if (!somethingToStart)
        {
            // it's possible if we have a scenario
            //  listEvents.add(new BEvent(EventCheckNothingToStart, "Nothing to start"));
        }

        logger.info("checkParameter : result nbEvents=" + listEvents.size());
        for (final BEvent event : listEvents)
        {
            logger.info("checkParameter :" + event.toString());
        }
        return listEvents;
    }


    /* ******************************************************************************** */
    /*                                                                                  */
    /* Register in the MeteorSimulation                                                 */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public List<BEvent> registerInSimulation(final MeteorSimulation meteorSimulation, final APIAccessor apiAccessor)
    {
        final List<BEvent>  listEvents = new ArrayList<BEvent>();
        for (final MeteorProcessDefinition meteorProcess : mListProcessDefinition.values())
        {
            if (meteorProcess.mNumberOfRobots>0)
            {
                logger.info("Add process[" + meteorProcess.mProcessName + "] in the startCase");
                meteorSimulation.addProcess(meteorProcess, apiAccessor);
            }
            // check activity
            for (final MeteorActivity meteorActivity : meteorProcess.mListActivities)
            {
                if (meteorActivity.mNumberOfRobots>0) {
                    meteorSimulation.addActivity(meteorActivity, apiAccessor);
                }
            }
        }
        return listEvents;

    }

}
