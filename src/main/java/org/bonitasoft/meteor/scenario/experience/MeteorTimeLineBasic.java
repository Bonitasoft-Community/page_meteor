package org.bonitasoft.meteor.scenario.experience;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bonitasoft.casedetails.CaseDetails;
import org.bonitasoft.casedetails.CaseDetailsAPI;
import org.bonitasoft.casedetails.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.log.event.BEvent;

public class MeteorTimeLineBasic extends MeteorTimeLine {

    public static String policy = "BASIC";

    public MeteorTimeLineBasic() {
        super();

    }

    @Override
    public List<BEvent> calcul(Long rootProcessInstanceId, ProcessAPI processAPI, IdentityAPI identityAPI) {
        CaseDetailsAPI caseDetailAPI = new CaseDetailsAPI();

        CaseHistoryParameter caseHistoryParameter = new CaseHistoryParameter();
        caseHistoryParameter.caseId = rootProcessInstanceId;

        caseHistoryParameter.loadSubProcess = true;
        caseHistoryParameter.loadContract = true;
        caseHistoryParameter.loadArchivedData = true;;
        caseHistoryParameter.loadBdmVariables = false;
        caseHistoryParameter.loadActivities = true;
        caseHistoryParameter.loadEvents = false;
        caseHistoryParameter.loadTimers = false;
        caseHistoryParameter.loadProcessVariables = false;
        caseHistoryParameter.loadDocuments = true;
        caseHistoryParameter.contractInJsonFormat = true;

        CaseDetails caseDetail = caseDetailAPI.getCaseDetails(caseHistoryParameter, processAPI, identityAPI, null, null);

        // Search the starter
        long dateCreateCase = 0;
        for (ProcessInstanceDescription processDescription : caseDetail.listProcessInstances) {
            if (processDescription.processInstanceId == rootProcessInstanceId) {
                setProcessName(processDescription.processDefinition.getName());
                setProcessVersion(processDescription.processDefinition.getVersion());
                setListContractValues(processDescription.contractInstanciation);
                dateCreateCase = processDescription.startDate == null ? 0 : processDescription.startDate.getTime();
            }
        }
        // sort the acitivity by time
        List<CaseDetailFlowNode> listCaseDetails = caseDetail.listCaseDetailFlowNodes;
        Collections.sort(listCaseDetails, new Comparator<CaseDetailFlowNode>() {

            public int compare(final CaseDetailFlowNode s1, final CaseDetailFlowNode s2) {
                if (s1.getDate() == null && s2.getDate() == null)
                    return 0;
                if (s1.getDate() == null)
                    return 1;
                if (s2.getDate() == null)
                    return -1;

                return s1.getDate().compareTo(s2.getDate());
            };
        });

        for (CaseDetailFlowNode detailFlowNode : listCaseDetails) {
            if (detailFlowNode.getType().equals(FlowNodeType.USER_TASK) || detailFlowNode.getType().equals(FlowNodeType.HUMAN_TASK)) {
                // keep only the executed state - READY for the Bonita Engine and executed tasks
                if (ActivityStates.READY_STATE.equals(detailFlowNode.getState()) && detailFlowNode.isArchived()) {
                    TimeLineStep timeLineStep = addOneStep();
                    timeLineStep.activityName = detailFlowNode.getName();
                    timeLineStep.sourceActivityDefinitionId = detailFlowNode.getFlownodeDefinitionId();
                    timeLineStep.listContractValues = detailFlowNode.getListContractValues();
                    timeLineStep.timelinems = detailFlowNode.getDate().getTime();
                    timeLineStep.timeFromBeginingms = timeLineStep.timelinems - dateCreateCase;

                    // basic time line: do not wait
                    timeLineStep.timeWaitms = 0;
                }
            }
        }
        return caseDetail.listEvents;
    }

    @Override
    public String getPolicy() {
        return policy;
    }

}
