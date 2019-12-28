package org.bonitasoft.meteor.scenario.experience;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.tools.Process.CaseDetails;
import org.bonitasoft.tools.Process.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.tools.Process.CaseDetailsAPI;
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;

public class MeteorTimeLineBasic extends MeteorTimeLine {

    public static String policy="BASIC";
    
    public MeteorTimeLineBasic(  ) {
        super( );
        
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
        caseHistoryParameter.loadActivities=true;
        caseHistoryParameter.loadEvents=false;
        caseHistoryParameter.loadTimers=false;
        caseHistoryParameter.loadProcessVariables = false;
        caseHistoryParameter.loadDocuments = true;
        caseHistoryParameter.contractInJsonFormat = true;
        
    
        CaseDetails caseDetail = caseDetailAPI.getCaseDetails(caseHistoryParameter,  processAPI, identityAPI, null, null);
        
        // Search the starter
        long dateCreateCase=0;
        for (ProcessInstanceDescription processDescription: caseDetail.listProcessInstances) {
            if (processDescription.processInstanceId == rootProcessInstanceId)
            {
                setProcessName( processDescription.processDefinition.getName());
                setProcessVersion( processDescription.processDefinition.getVersion());
                setListContractValues( processDescription.contractInstanciation );
                dateCreateCase = processDescription.startDate == null ? 0 : processDescription.startDate.getTime();
            }
        }
        // sort the acitivity by time
        List<CaseDetailFlowNode> listCaseDetails = caseDetail.listCaseDetailFlowNodes;
        Collections.sort(listCaseDetails, new Comparator<CaseDetailFlowNode>() {

            public int compare(final CaseDetailFlowNode s1, final CaseDetailFlowNode s2) {
                if (s1.getDate() ==null && s2.getDate() == null)
                    return 0;
                if (s1.getDate() ==null)
                    return 1;
                if (s2.getDate() ==null)
                    return -1;
                
                return s1.getDate().compareTo(s2.getDate());
            };
        });

        for (CaseDetailFlowNode detailFlowNode : listCaseDetails)
        {
            if (detailFlowNode.getType().equals(FlowNodeType.USER_TASK) || detailFlowNode.getType().equals(FlowNodeType.HUMAN_TASK)) 
            {
                TimeLineStep timeLineStep = addOneStep();
                timeLineStep.activityName = detailFlowNode.getName();
                timeLineStep.sourceActivityDefinitionId = detailFlowNode.getFlownodeDefinitionId();
                timeLineStep.listContractValues = detailFlowNode.getListContractValues();
                timeLineStep.timelinems = detailFlowNode.getDate().getTime();
                timeLineStep.timeFromBeginingms = timeLineStep.timelinems - dateCreateCase; 
            }
        }
        return caseDetail.listEvents;
    }
    @Override
    public String getPolicy() {
        return policy;
    }

    

}
