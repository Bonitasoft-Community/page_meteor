package org.bonitasoft.meteor.scenario.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.meteor.MeteorRobot;
import org.bonitasoft.meteor.scenario.experience.MeteorRobotExperience;

/**
 * This class calcul the cover of the execution
 */
/**
 * thanks to Bonita, this calcul must be done in a Separate thread ! It's not
 * possible to do it in the command, because in a command, some ProcessAPI
 * operation is not allowed like the Search (neested transaction ! ) So, the
 * only way is to create a Thread to do it*.
 * 
 * @author Pierre-Yves
 */
public class MeteorCalculCover implements Runnable {

    public static Logger logger = Logger.getLogger(MeteorRobot.class.getName());

    public enum CoverStatus {
        NONE, INPROGRESS, DONE
    };

    public CoverStatus mStatus;

    private List<MeteorRobot> mListRobots;
    private List<MeteorDefProcess> mListMeteorProcess;
    private final APIAccessor apiAccessor;

    /* ******************************************************************** */
    /*                                                                      */
    /* getter/Setter */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public MeteorCalculCover(List<MeteorDefProcess> listMeteorProcess, List<MeteorRobot> listRobots, final APIAccessor apiAccessor) {
        this.apiAccessor = apiAccessor;
        this.mListMeteorProcess = listMeteorProcess;
        this.mListRobots = listRobots;
        mStatus = CoverStatus.NONE;
    }

    public CoverStatus getStatus() {
        return mStatus;
    }

    public List<Map<String, Object>> toJson() {
        List<Map<String, Object>> listResultCover = new ArrayList<>();
        if (mStatus == CoverStatus.DONE) {
            for (MeteorDefProcess meteorProcess : mListMeteorProcess) {
                listResultCover.add(meteorProcess.getCoverResult().getMap());
            }
        }
        return listResultCover;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* Run the calcul in a Thread */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public void start() {
        final Thread T = new Thread(this);
        T.start();
    }

    public void run() {

        logger.info("----------- Start CalculCover");
        mStatus = CoverStatus.INPROGRESS;

        for (MeteorDefProcess meteorProcess : mListMeteorProcess) {
            List<Long> listProcessInstances = new ArrayList<Long>();
            // Ask all rabots to get the list of cases for this process (and only for this process)
            for (MeteorRobot robot : mListRobots) {
                if (robot instanceof MeteorRobotCreateCase) {
                    MeteorRobotCreateCase robotCreateCase = (MeteorRobotCreateCase) robot;
                    if (robotCreateCase.getMeteorProcess().mProcessDefinitionId == meteorProcess.mProcessDefinitionId) {
                        listProcessInstances.addAll(robotCreateCase.getListProcessInstanceCreated());
                    }

                }
                if (robot instanceof MeteorRobotExperience)
                {
                    MeteorRobotExperience robotExperience = (MeteorRobotExperience)robot;
                    if (robotExperience.getMeteorTimeLine().getProcessDefinitionId().equals(meteorProcess.mProcessDefinitionId)) {
                        listProcessInstances.addAll(robotExperience.getListProcessInstanceCreated());
                    }
                    
                }
            }
            meteorProcess.calculCover(listProcessInstances, apiAccessor.getProcessAPI());
        }

        mStatus = CoverStatus.DONE;

    }
}
