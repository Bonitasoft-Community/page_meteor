package org.bonitasoft.meteor.scenario.experience;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.tools.Process.CaseDetails;
import org.bonitasoft.tools.Process.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.tools.Process.CaseDetailsAPI;
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;

public class MeteorTimeLineBasic extends MeteorTimeLine {

    ProcessAPI processAPI;
    public MeteorTimeLineBasic( Long rootProcessInstanceId, ProcessAPI processAPI ) {
        super( rootProcessInstanceId );
        this.processAPI = processAPI;
    }
    @Override
    public List<BEvent> calcul() {
        CaseDetailsAPI caseDetailAPI = new CaseDetailsAPI();
        
        CaseHistoryParameter caseHistoryParameter = new CaseHistoryParameter();
        caseHistoryParameter.caseId = getRootProcessInstanceId();
        caseHistoryParameter.loadBdmVariable = false;
        caseHistoryParameter.showArchivedData=true;
        caseHistoryParameter.showContract= true;
        
    
        CaseDetails caseDetail = caseDetailAPI.getCaseDetails(caseHistoryParameter,  processAPI, null, null, null);
        
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
            if (detailFlowNode.getType().equals(FlowNodeType.HUMAN_TASK)) 
            {
                TimeLineStep timeLineStep = addOneStep();
                timeLineStep.activityName = detailFlowNode.getName();
                timeLineStep.sourceActivityDefinitionId = detailFlowNode.getFlownodeDefinitionId();
                timeLineStep.listContractValues = detailFlowNode.getListContractValues();
            }
        }
        return caseDetail.listEvents;
    }

    

}
