/*jadclipse*/// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.

package nc.bs.pub.pf.pfframe;

import java.lang.reflect.Array;
import java.util.*;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.pf.pub.PfDataCache;
import nc.bs.pub.pf.*;
import nc.bs.pub.pflock.*;
import nc.bs.pub.workflownote.WorknoteManager;
import nc.bs.pub.workflowpsn.WorkflowPersonDAO;
import nc.bs.uap.pf.workflow.WFAgentMessageHandler;
import nc.impl.uap.pf.PFConfigImpl;
import nc.itf.uap.pf.*;
import nc.jdbc.framework.exception.DbException;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pf.change.PfUtilBaseTools;
import nc.vo.pub.*;
import nc.vo.pub.pf.PfUtilActionVO;
import nc.vo.pub.pf.WfTaskInfo;
import nc.vo.pub.workflownote.WorkflownoteVO;
import nc.vo.pub.workflowpsn.*;
import nc.vo.sm.UserVO;
import nc.vo.uap.pf.*;
import nc.vo.wfengine.core.activity.Activity;
import nc.vo.wfengine.core.parser.XPDLParserException;
import nc.vo.wfengine.core.workflow.WorkflowProcess;
import nc.vo.wfengine.pub.WFTask;
import nc.vo.wfengine.pub.WfTaskType;

public class PlatFormEntryImpl
    implements IplatFormEntry
{

    public PlatFormEntryImpl()
    {
    }

    public Object processAction(String actionName, String billType, WorkflownoteVO worknoteVO, AggregatedValueObject billvo, Object userObj, HashMap hmPfExParams)
        throws BusinessException
    {
        Logger.debug("******\u8FDB\u5165PlatFormEntryImpl.processAction\u65B9\u6CD5******************");
        Logger.debug((new StringBuilder()).append("* actionName=").append(actionName).toString());
        Logger.debug((new StringBuilder()).append("* billType=").append(billType).toString());
        PfBusinessLock pfLock = null;
        PfBusinessLock pfAgentHistoryLock = null;
        Object obj;
        if(hmPfExParams == null)
            hmPfExParams = new HashMap();
        pfLock = new PfBusinessLock();
        Object paramNoLock = hmPfExParams != null ? hmPfExParams.get("nolockandconsist") : null;
        if(paramNoLock == null)
            pfLock.lock(new VOLockData(billvo, billType), new VOConsistenceCheck(billvo, billType));
        if(worknoteVO != null && worknoteVO.getTaskInfo() != null && worknoteVO.getTaskInfo().getTask() != null)
        {
            WFTask task = worknoteVO.getTaskInfo().getTask();
            if(task.getTaskType() == WfTaskType.Backward.getIntValue())
            {
                String backTo = task.getJumpToActivity();
                try
                {
                    WorkflowProcess wp = PfDataCache.getWorkflowProcess(task.getWfProcessDefPK());
                    Activity activity = wp.findActivityByID(backTo);
                    if(wp.findStartActivity().getId().equals(backTo))
                        task.setBackToFirstActivity(true);
                }
                catch(XPDLParserException e)
                {
                    throw new BusinessException(e);
                }
            }
        }
        Object paramReloadVO = hmPfExParams != null ? hmPfExParams.get("reload_vo") : null;
        AggregatedValueObject reloadvo = billvo;
        if(paramReloadVO != null)
        {
            String billId = billvo.getParentVO().getPrimaryKey();
            reloadvo = (new PFConfigImpl()).queryBillDataVO(billType, billId);
            if(reloadvo == null)
                throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("pfworkflow", "PlatFormEntryImpl-0000", null, new String[] {
                    billType, billId
                }));
            hmPfExParams.remove("reload_vo");
        }
        Object paramSilent = hmPfExParams != null ? hmPfExParams.get("silently") : null;
        if(worknoteVO == null && paramSilent != null && (PfUtilBaseTools.isApproveAction(actionName, billType) || PfUtilBaseTools.isSignalAction(actionName, billType)))
            worknoteVO = ((IWorkflowMachine)NCLocator.getInstance().lookup(nc.itf.uap.pf.IWorkflowMachine.class)).checkWorkFlow(actionName, billType, reloadvo, hmPfExParams);
        Object checkObj = PfUtilTools.getBizRuleImpl(billType);
        AggregatedValueObject completeVO = reloadvo;
        AggregatedValueObject cloneVO = reloadvo;
        if(checkObj instanceof IPfBeforeAction)
        {
            completeVO = ((IPfBeforeAction)checkObj).beforeAction(reloadvo, userObj, hmPfExParams);
            AggregatedValueObject tmpAry[] = ((IPfBeforeAction)checkObj).getCloneVO();
            if(tmpAry != null && tmpAry.length > 0)
                cloneVO = tmpAry[0];
        }
        Object retObjAfterAction = ((IPFBusiAction)NCLocator.getInstance().lookup(nc.itf.uap.pf.IPFBusiAction.class)).processAction(actionName, billType, worknoteVO, completeVO, userObj, hmPfExParams);
        if(checkObj instanceof IPfAfterAction)
            retObjAfterAction = ((IPfAfterAction)checkObj).afterAction(cloneVO, retObjAfterAction, hmPfExParams);
        try
        {
            if(worknoteVO != null && PfUtilBaseTools.isApproveAction(actionName, billType))
            {
                String originalCheckMan = worknoteVO.getAgencyuser();
                if(!StringUtil.isEmptyWithTrim(originalCheckMan) && !worknoteVO.getCheckman().equals(originalCheckMan))
                {
                    pfAgentHistoryLock = new PfBusinessLock();
                    IPFResource agentSrv = (IPFResource)NCLocator.getInstance().lookup(nc.itf.uap.pf.IPFResource.class);
                    WorkflowpersonVO outInfo = agentSrv.queryWFPersonOutInfo(originalCheckMan);
                    if(outInfo != null)
                    {
                        WFAgentHistoryVO agentHistory = new WFAgentHistoryVO();
                        agentHistory.setAgentor(worknoteVO.getCheckman());
                        agentHistory.setPk_outinfo(outInfo.getPk_outinfo());
                        String pk_billType = "";
                        WorkflowPersonDAO dao = new WorkflowPersonDAO();
                        List agents = dao.queryDynamicAgentVOs(originalCheckMan, worknoteVO.getPk_billtype());
                        Iterator i$ = agents.iterator();
                        do
                        {
                            if(!i$.hasNext())
                                break;
                            WorkflowagentVO agent = (WorkflowagentVO)i$.next();
                            if(!originalCheckMan.equals(agent.getPk_cuserid()))
                                continue;
                            pk_billType = agent.getBilltypes();
                            break;
                        } while(true);
                        agentHistory.setBilltype(pk_billType);
                        pfAgentHistoryLock.lock(new WFSuperVOLockData(new WFAgentHistoryVO[] {
                            agentHistory
                        }), null);
                        agentSrv.insertWFAgentHistoryInfo(agentHistory);
                    }
                }
            }
            handleAgentMsgSend(worknoteVO, actionName, billType);
            obj = retObjAfterAction;
            Logger.debug("******\u79BB\u5F00PlatFormEntryImpl.processAction\u65B9\u6CD5******************");
            return obj;
        }
        catch(DbException ex)
        {
            throw new BusinessException(ex);
        }
//        
//        
//        Exception exception;
//        exception;
//        throw exception;
    }

    private void handleAgentMsgSend(WorkflownoteVO worknoteVO, String actionName, String billtype)
    {
        if(worknoteVO == null || !PfUtilBaseTools.isApproveAction(actionName, billtype))
        {
            return;
        } else
        {
            WFAgentMessageHandler handler = new WFAgentMessageHandler();
            handler.handleApprove(worknoteVO);
            return;
        }
    }

    public Object processBatch(String actionName, String billType, WorkflownoteVO worknoteVO, AggregatedValueObject billvos[], Object userObjAry[], HashMap hmPfExParams)
        throws BusinessException
    {
        PfBusinessLock pfLock = null;
        PfProcessBatchRetObject pfprocessbatchretobject;
        pfLock = new PfBusinessLock();
        pfLock.lock(new VOsLockData(billvos, billType), new VOsConsistenceCheck(billvos, billType));
        if(worknoteVO != null && worknoteVO.getTaskInfo() != null && worknoteVO.getTaskInfo().getTask() != null)
        {
            WFTask task = worknoteVO.getTaskInfo().getTask();
            if(task.getTaskType() == WfTaskType.Backward.getIntValue())
            {
                String backTo = task.getJumpToActivity();
                try
                {
                    WorkflowProcess wp = PfDataCache.getWorkflowProcess(task.getWfProcessDefPK());
                    Activity activity = wp.findActivityByID(backTo);
                    if(wp.findStartActivity().getId().equals(backTo))
                        task.setBackToFirstActivity(true);
                }
                catch(XPDLParserException e)
                {
                    throw new BusinessException(e);
                }
            }
        }
        Object paramReloadVO = hmPfExParams != null ? hmPfExParams.get("reload_vo") : null;
        AggregatedValueObject reloadvos[] = billvos;
        if(paramReloadVO != null)
        {
            reloadvos = (AggregatedValueObject[])(AggregatedValueObject[])Array.newInstance(billvos[0].getClass(), billvos.length);
            for(int i = 0; i < billvos.length; i++)
            {
                String billId = billvos[i].getParentVO().getPrimaryKey();
                reloadvos[i] = (new PFConfigImpl()).queryBillDataVO(billType, billId);
                if(reloadvos[i] == null)
                    throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("pfworkflow", "PlatFormEntryImpl-0000", null, new String[] {
                        billType, billId
                    }));
            }

            hmPfExParams.remove("reload_vo");
        }
        Object checkObj = PfUtilTools.getBizRuleImpl(billType);
        AggregatedValueObject completeVOs[] = reloadvos;
        AggregatedValueObject cloneVOs[] = reloadvos;
        if(checkObj instanceof IPfBeforeAction)
        {
            completeVOs = ((IPfBeforeAction)checkObj).beforeBatch(billvos, userObjAry, hmPfExParams);
            cloneVOs = ((IPfBeforeAction)checkObj).getCloneVO();
        }
        Object paramSilent = hmPfExParams != null ? hmPfExParams.get("silently") : null;
        if(worknoteVO == null && paramSilent != null && PfUtilBaseTools.isSignalFlowAction(actionName, billType))
            worknoteVO = ((IWorkflowMachine)NCLocator.getInstance().lookup(nc.itf.uap.pf.IWorkflowMachine.class)).checkWorkFlow(actionName, billType, completeVOs[0], hmPfExParams);
        PFBatchExceptionInfo batchExceptionInfo = new PFBatchExceptionInfo();
        Object retObjsAfterAction[] = ((IPFBusiAction)NCLocator.getInstance().lookup(nc.itf.uap.pf.IPFBusiAction.class)).processBatch(actionName, billType, completeVOs, userObjAry, worknoteVO, hmPfExParams, batchExceptionInfo);
        if(checkObj instanceof IPfAfterAction)
            retObjsAfterAction = ((IPfAfterAction)checkObj).afterBatch(cloneVOs, retObjsAfterAction, hmPfExParams);
        try{
        	pfprocessbatchretobject = new PfProcessBatchRetObject(retObjsAfterAction, batchExceptionInfo);
        	return pfprocessbatchretobject;
        }catch(Exception ex){
        	throw new RuntimeException(ex);
        }finally {
        	if(pfLock != null)
                pfLock.unLock();
        }
//        if(pfLock != null)
//            pfLock.unLock();
//        return pfprocessbatchretobject;
//        Exception exception;
//        exception;
//        if(pfLock != null)
//            pfLock.unLock();
//        throw exception;
    }

    public UserVO[] queryValidCheckers(String billId, String billType)
        throws BusinessException
    {
        WorknoteManager noteMgr = new WorknoteManager();
        try
        {
            return noteMgr.queryValidCheckers(billId, billType);
        }
        catch(DbException e)
        {
            Logger.error(e.getMessage(), e);
            throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("pfworkflow", "PlatFormEntryImpl-0001", null, new String[] {
                e.getMessage()
            }));
        }
    }

    public PfUtilActionVO[] getActionDriveVOs(String billType, String busiType, String pkCorp, String actionName)
        throws BusinessException
    {
        PfUtilActionVO driveActions[] = null;
        try
        {
            PfUtilDMO dmo = new PfUtilDMO();
            driveActions = dmo.queryDriveAction(billType, busiType, pkCorp, actionName, null);
        }
        catch(DbException e)
        {
            Logger.error(e.getMessage(), e);
            throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("pfworkflow", "PlatFormEntryImpl-0002", null, new String[] {
                e.getMessage()
            }));
        }
        return driveActions;
    }

    public Object processBatch(String actionName, WorkflownoteVO worknoteVO, String billTypes[], String billIds[])
        throws BusinessException
    {
        List retList = new ArrayList();
        IPFConfig pfConf = (IPFConfig)NCLocator.getInstance().lookup(nc.itf.uap.pf.IPFConfig.class);
        Map billVOMap = new HashMap();
        for(int i = 0; i < billTypes.length; i++)
        {
            String billType = billTypes[i];
            String billId = billIds[i];
            AggregatedValueObject billvo = pfConf.queryBillDataVO(billType, billId);
            List list;
            if(!billVOMap.containsKey(billType))
            {
                list = new ArrayList();
                billVOMap.put(billType, list);
            }
            list = (List)billVOMap.get(billType);
            list.add(billvo);
        }

        HashMap param = new HashMap();
        param.put("batch", "batch");
        Object ret;
        for(Iterator it = billVOMap.keySet().iterator(); it.hasNext(); retList.add(ret))
        {
            String billtype = (String)it.next();
            WorkflownoteVO currNote = (WorkflownoteVO)worknoteVO.clone();
            HashMap currParam = (HashMap)param.clone();
            currParam.put("worknote", currNote);
            List list = (List)billVOMap.get(billtype);
            ret = processBatch(actionName, billtype, currNote, (AggregatedValueObject[])list.toArray(new AggregatedValueObject[0]), null, currParam);
        }

        return retList;
    }
}


/*
	DECOMPILATION REPORT

	Decompiled from: C:\ab\NC\NC65\bhyl\nc65home\modules\riart\META-INF\lib\riart_riartplatformLevel-1.jar
	Total time: 109 ms
	Jad reported messages/errors:
Overlapped try statements detected. Not all exception handlers will be resolved in the method processAction
Couldn't fully decompile method processAction
Couldn't resolve all exception handlers in method processAction
Overlapped try statements detected. Not all exception handlers will be resolved in the method processBatch
Couldn't fully decompile method processBatch
Couldn't resolve all exception handlers in method processBatch
	Exit status: 0
	Caught exceptions:
*/