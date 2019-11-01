
package nc.impl.ic.onhand.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nc.bs.ic.onhand.action.OnhandResBalance;
import nc.bs.ic.onhand.bp.OnhandBaseQry;
import nc.bs.ic.onhand.bp.OnhandDataFactory;
import nc.bs.ic.onhand.bp.OnhandLocationQry;
import nc.bs.ic.onhand.bp.OnhandSynQuery;
import nc.bs.ic.pub.env.ICBSContext;
import nc.itf.ic.onhand.OnhandResService;
import nc.vo.ic.general.define.ICBillBodyVO;
import nc.vo.ic.general.define.ICBillHeadVO;
import nc.vo.ic.general.define.ICBillVO;
import nc.vo.ic.material.define.InvCalBodyVO;
import nc.vo.ic.onhand.define.ICBillOnhandReq;
import nc.vo.ic.onhand.define.ICBillPickResults;
import nc.vo.ic.onhand.define.OnhandBalanceResult;
import nc.vo.ic.onhand.entity.OnhandDimVO;
import nc.vo.ic.onhand.entity.OnhandVO;
import nc.vo.ic.onhand.pub.HashVOUtils;
import nc.vo.ic.onhand.pub.OnhandQueryDim;
import nc.vo.ic.onhand.pub.OnhandSelectDim;
import nc.vo.ic.onhand.pub.OnhandVOTools;
import nc.vo.ic.pub.util.CollectionUtils;
import nc.vo.ic.pub.util.DimMatchedObj;
import nc.vo.ic.pub.util.StringUtil;
import nc.vo.ic.pub.util.VOEntityUtil;
import nc.vo.ic.pub.util.ValueCheckUtil;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.pubapp.pattern.pub.MapList;
import nc.vo.sc.m61.entity.SCOrderIssueVO;

public class OnhandResServiceImpl implements OnhandResService {
    public OnhandResServiceImpl() {
    }

    public SCOrderIssueVO[] getSCOrderIssueVOs(AggregatedValueObject[] billvos) throws BusinessException {
        if (ValueCheckUtil.isNullORZeroLength(billvos)) {
            return null;
        } else {
            try {
                return OnhandDataFactory.getOnhandDataSource(billvos[0]).getSCOrderIssueVOs(billvos);
            } catch (Exception var3) {
                ExceptionUtils.marsh(var3);
                return null;
            }
        }
    }

    public ICBillPickResults pickAuto(ICBillVO billvo) throws BusinessException {
        try {
            List<OnhandBalanceResult<ICBillOnhandReq>> retults = this.pickAutoInner(new ICBillVO[]{billvo});
            if (retults != null && retults.size() > 0) {
                ICBillPickResults ret = new ICBillPickResults(retults, billvo.getBodys());
                ret.getPickBodys();
                return ret;
            } else {
                return null;
            }
        } catch (Exception var4) {
            ExceptionUtils.marsh(var4);
            return null;
        }
    }

    public ICBillVO[] pickAuto(ICBillVO[] bills) throws BusinessException {
        List<OnhandBalanceResult<ICBillOnhandReq>> retults = this.pickAutoInner(bills);
        if (retults != null && retults.size() > 0) {
            try {
                List<ICBillBodyVO> lbodys = new ArrayList();
                ICBillVO[] arr$ = bills;
                int len$ = bills.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    ICBillVO bill = arr$[i$];
                    ICBillBodyVO[] bodys = bill.getBodys();
                    if (!ValueCheckUtil.isNullORZeroLength(bodys)) {
                        ICBillBodyVO[] arr$2 = bodys;
                        int len$2 = bodys.length;

                        for(int i$1 = 0; i$1 < len$2; ++i$1) {
                            ICBillBodyVO body = arr$2[i$1];
                            lbodys.add(body);
                        }
                    }
                }

                ICBillPickResults ret = new ICBillPickResults(retults, (ICBillBodyVO[])lbodys.toArray(new ICBillBodyVO[lbodys.size()]));
                ICBillBodyVO[] pickbodys = ret.getPickBodys();
                MapList<Integer, ICBillBodyVO> mapPickBodyVOs = new MapList();
                ICBillBodyVO[] arr$4 = pickbodys;
                int len$4 = pickbodys.length;

                int i$;
                for(i$ = 0; i$ < len$4; ++i$) {
                    ICBillBodyVO pickbody = arr$4[i$];
                    mapPickBodyVOs.put(pickbody.getPseudoColumn(), pickbody);
                }

                ICBillVO[] arr$3 = bills;
                len$ = bills.length;

                for(i$ = 0; i$ < len$; ++i$) {
                    ICBillVO bill = arr$3[i$];
                    List<ICBillBodyVO> lbodys2 = mapPickBodyVOs.get(bill.getHead().getPseudoColumn());
                    if (!ValueCheckUtil.isNullORZeroLength(lbodys2)) {
                        bill.setChildrenVO((CircularlyAccessibleValueObject[])CollectionUtils.listToArray(lbodys2));
                    }
                }

                return bills;
            } catch (Exception var13) {
                ExceptionUtils.marsh(var13);
                return null;
            }
        } else {
            return null;
        }
    }

    public OnhandVO[] queryOnhandVOByBills(ICBillVO[] bills) throws BusinessException {
        try {
            List<OnhandDimVO> ldimvos = OnhandVOTools.getOnhandDimVOs(bills, (new ICBSContext()).getInvInfo());
            if (ValueCheckUtil.isNullORZeroLength(ldimvos)) {
                return null;
            } else {
                ICBSContext context = new ICBSContext();
                OnhandDimVO[] dimvos = (OnhandDimVO[])ldimvos.toArray(new OnhandDimVO[ldimvos.size()]);
                OnhandVOTools.getRealOnhandDim(context.getInvInfo(), dimvos);
                InvCalBodyVO[] invCalVOs = context.getInvInfo().getInvCalBodyVO((String[])VOEntityUtil.getVOsValues(dimvos, "pk_org", String.class), (String[])VOEntityUtil.getVOsValues(dimvos, "cmaterialvid", String.class));
                List<OnhandDimVO> lbalancevos = new ArrayList();
                Set<String> keysset = new HashSet();
                String key = null;

                for(int i = 0; i < dimvos.length; ++i) {
                    if (ValueCheckUtil.isTrue(invCalVOs[i].getAutobalancemeas())) {
                        key = HashVOUtils.getContentKey(dimvos[i]);
                        if (!keysset.contains(key)) {
                            lbalancevos.add(dimvos[i]);
                        }
                    }
                }

                if (ValueCheckUtil.isNullORZeroLength(lbalancevos)) {
                    return null;
                } else {
                    return this.queryOnhandVOByDims((OnhandDimVO[])lbalancevos.toArray(new OnhandDimVO[lbalancevos.size()]));
                }
            }
        } catch (Exception var10) {
            ExceptionUtils.marsh(var10);
            return null;
        }
    }

    public OnhandVO[] queryOnhandVOByDims(OnhandDimVO[] dimvos) throws BusinessException {
        try {
            OnhandSelectDim select = new OnhandSelectDim();
            select.setSum(false);
            select.addSelectFields((String[])CollectionUtils.combineArrs(new String[][]{OnhandDimVO.getDimContentFields(), {"pk_onhanddim", "vhashcode", "vsubhashcode"}}));
            return this.queryOnhandVOByDims(select, dimvos, false, (String)null);
        } catch (Exception var3) {
            ExceptionUtils.marsh(var3);
            return null;
        }
    }

    public OnhandVO[] queryOnhandVOByDims(OnhandSelectDim select, OnhandDimVO[] dimvos, boolean bqueryuseablestate, String bytranstype) throws BusinessException {
        try {
            List<String> ltranstypes = null;
            if (bytranstype != null) {
                ltranstypes = new ArrayList();
                ltranstypes.add(bytranstype);
            }

            return (new OnhandBaseQry()).queryOnhandVOByDims(select, dimvos, bqueryuseablestate, ltranstypes);
        } catch (Exception var6) {
            ExceptionUtils.marsh(var6);
            return null;
        }
    }

    public OnhandVO[] queryUseableOnhand(OnhandDimVO[] dimvos) throws BusinessException {
        if (ValueCheckUtil.isNullORZeroLength(dimvos)) {
            return null;
        } else {
            try {
                OnhandSelectDim select = new OnhandSelectDim();
                select.setSum(false);
                select.addSelectFields((String[])CollectionUtils.combineArrs(new String[][]{OnhandDimVO.getDimContentFields(), {"pk_onhanddim", "vhashcode", "vsubhashcode"}}));
                return this.queryOnhandVOByDims(select, dimvos, true, (String)null);
            } catch (Exception var3) {
                ExceptionUtils.marsh(var3);
                return null;
            }
        }
    }

    public OnhandVO[] queryUseableOnhandForAtp(OnhandDimVO[] dimvos) throws BusinessException {
        try {
            return (new OnhandBaseQry()).queryOnhandVOByDims((OnhandSelectDim)null, dimvos, true, (List)null);
        } catch (Exception var3) {
            ExceptionUtils.marsh(var3);
            return null;
        }
    }

    private List<OnhandBalanceResult<ICBillOnhandReq>> pickAutoInner(ICBillVO[] billvos) {
        if (ValueCheckUtil.isNullORZeroLength(billvos)) {
            return null;
        } else {
            List<String> ltranstypes = new ArrayList();
            List<ICBillOnhandReq> lreqs = new ArrayList();
            int count = 0;
            int totalrownum = 0;
            Integer billno = null;
            ICBillVO[] arr$ = billvos;
            int len$ = billvos.length;

            ICBillVO result;
            for(int i$ = 0; i$ < len$; ++i$) {
                result = arr$[i$];
                ICBillBodyVO[] bodys = result.getBodys();
                ICBillHeadVO head = result.getHead();
                ICBillOnhandReq req = null;
                billno = count;
                head.setPseudoColumn(billno);

                for(int i = 0; i < bodys.length; ++i) {
                    bodys[i].setPk_group(head.getPk_group());
                    bodys[i].setPk_org(head.getPk_org());
                    bodys[i].setPk_org_v(head.getPk_org_v());
                    bodys[i].setCbodywarehouseid(head.getCwarehouseid());
                    bodys[i].setPseudoColumn(billno);
                    req = new ICBillOnhandReq(bodys[i]);
                    req.setRowno(String.valueOf(totalrownum));
                    ++totalrownum;
                    lreqs.add(req);
                    if (!StringUtil.isSEmptyOrNull(head.getVtrantypecode())) {
                        ltranstypes.add(head.getVtrantypecode());
                    }
                }

                ++count;
            }

            if (ValueCheckUtil.isNullORZeroLength(lreqs)) {
                return null;
            } else {
                ICBillOnhandReq[] reqs = new ICBillOnhandReq[lreqs.size()];
                reqs = (ICBillOnhandReq[])lreqs.toArray(reqs);
                OnhandResBalance<ICBillOnhandReq> resb = new OnhandResBalance(reqs);
                resb.setCtranstype(ltranstypes);
                resb.onhandBalance();
                List<OnhandBalanceResult<ICBillOnhandReq>> results = new ArrayList();
                result = null;
                ICBillOnhandReq[] arr$2 = reqs;
                int len$2 = reqs.length;

                for(int i$ = 0; i$ < len$2; ++i$) {
                    ICBillOnhandReq reqq = arr$2[i$];
                    OnhandBalanceResult<ICBillOnhandReq> result2 = resb.getResults(reqq);
                    if (result2 != null) {
                        results.add(result2);
                    }
                }

                return results;
            }
        }
    }

    public Map<String, String> queryLastInLocation(String pk_calbody, String cwarehouseid, String[] cmaterialvids) throws BusinessException {
        try {
            return (new OnhandLocationQry()).queryLastInLocation(pk_calbody, cwarehouseid, cmaterialvids);
        } catch (Exception var5) {
            ExceptionUtils.marsh(var5);
            return new HashMap();
        }
    }

    public Map<String, String> queryOnhandLocation(String pk_calbody, String cwarehouseid, String[] cmaterialvids) throws BusinessException {
        try {
            return (new OnhandLocationQry()).queryOnhandLocation(pk_calbody, cwarehouseid, cmaterialvids);
        } catch (Exception var5) {
            ExceptionUtils.marsh(var5);
            return new HashMap();
        }
    }

    public OnhandVO[] queryAtpOnhand(OnhandDimVO[] dimvos, boolean bextendWarehouse) throws BusinessException {
        try {
            return (new OnhandSynQuery()).queryOnhandForAtp((OnhandSelectDim)null, this.getQueryDim(dimvos), bextendWarehouse);
        } catch (Exception var4) {
            ExceptionUtils.marsh(var4);
            return new OnhandVO[0];
        }
    }

    private OnhandQueryDim[] getQueryDim(OnhandDimVO[] dimvos) {
        if (ValueCheckUtil.isNullORZeroLength(dimvos)) {
            return null;
        } else {
            DimMatchedObj<OnhandDimVO> handkey = null;
            OnhandQueryDim[] dimconds = new OnhandQueryDim[dimvos.length];

            for(int i = 0; i < dimvos.length; ++i) {
                handkey = new DimMatchedObj(dimvos[i], OnhandDimVO.getDimContentFields());
                dimconds[i] = new OnhandQueryDim();
                dimconds[i].addDimCondition(handkey.getDimFields(), VOEntityUtil.getVOValues(dimvos[i], handkey.getDimFields()));
            }

            return dimconds;
        }
    }

    public OnhandVO[] queryAtpOnhandUP(OnhandDimVO[] dimvos, UFDateTime tupdatetime, UFDateTime endtime, boolean bextendWarehouse) throws BusinessException {
        return (new OnhandSynQuery()).queryOnhandForAtpUP((OnhandSelectDim)null, this.getQueryDim(dimvos), tupdatetime, endtime, bextendWarehouse);
    }
}