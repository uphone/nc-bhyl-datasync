
package nc.bs.ic.onhand.action;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.pub.env.ICBSContext;
import nc.itf.scmpub.reference.uap.group.SysInitGroupQuery;
import nc.pubitf.ic.onhand.IOnhandQry;
import nc.vo.ic.location.ICLocationVO;
import nc.vo.ic.material.define.InvBasVO;
import nc.vo.ic.material.define.InvMeasVO;
import nc.vo.ic.onhand.define.BalanceOnhandRes;
import nc.vo.ic.onhand.define.IOnhandReq;
import nc.vo.ic.onhand.define.OnhandBalanceResult;
import nc.vo.ic.onhand.define.OnhandRes;
import nc.vo.ic.onhand.entity.OnhandDimVO;
import nc.vo.ic.onhand.entity.OnhandSNVO;
import nc.vo.ic.onhand.entity.OnhandSNViewVO;
import nc.vo.ic.onhand.entity.OnhandVO;
import nc.vo.ic.onhand.pub.OnhandSelectDim;
import nc.vo.ic.onhand.pub.OnhandVOTools;
import nc.vo.ic.pub.calc.BusiCalculator;
import nc.vo.ic.pub.util.CollectionUtils;
import nc.vo.ic.pub.util.DimMatchUtil;
import nc.vo.ic.pub.util.NCBaseTypeUtils;
import nc.vo.ic.pub.util.StringUtil;
import nc.vo.ic.pub.util.VOEntityUtil;
import nc.vo.ic.pub.util.ValueCheckUtil;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;

public class OnhandResBalance<T extends IOnhandReq> implements ResAlloc {
    private BusiCalculator calculator;
    private List<String> ctranstypes;
    private Map<T, OnhandBalanceResult<T>> mapResults;
    private Map<String, OnhandVO> mapyethandvo;
    private T[] onhandReq;
    private T[] onhandSNReq;
    private OnhandResManager onhandres;
    private DimMatchUtil<T> reqsmatch;

    public OnhandResBalance(T[] reqs) {
        this.onhandReq = reqs;
        this.calculator = BusiCalculator.getBusiCalculatorAtBS();
    }

    public boolean assignRes(OnhandRes handres) {
        List<T> lreqs = this.reqsmatch.searchMatchedDimObj(handres, -1);
        if (ValueCheckUtil.isNullORZeroLength(lreqs)) {
            return true;
        } else {
            boolean byetoverhand = this.processResDataForHand(handres);
            OnhandBalanceResult<T> balancerow = null;
            int ireqs = lreqs.size();
            Iterator i$ = lreqs.iterator();

            while(i$.hasNext()) {
                T req = (T)i$.next();
                balancerow = (OnhandBalanceResult)this.mapResults.get(req);
                if (balancerow == null) {
                    balancerow = new OnhandBalanceResult(req);
                    this.mapResults.put(req, balancerow);
                }

                boolean isNumAssign = handres.isNumUseable();
                boolean isAstNumAssign = handres.isAstnumUseable();
                boolean isGrossNumAssign = handres.isGrossnumUseable();
                this.assignRes(balancerow, handres);
                if (balancerow.isFull(isNumAssign, isAstNumAssign, isGrossNumAssign)) {
                    this.reqsmatch.remoteMatchedDimObj(req);
                    --ireqs;
                }

                if (!handres.isUseable()) {
                    break;
                }
            }

            if (byetoverhand) {
                return true;
            } else {
                return ireqs <= 0;
            }
        }
    }

    public OnhandBalanceResult<T> getResults(T req) {
        return this.mapResults == null ? null : (OnhandBalanceResult)this.mapResults.get(req);
    }

    public void onhandBalance() {
        this.processData();
        this.processAllocSNData();
        if (!ValueCheckUtil.isNullORZeroLength(this.onhandReq) && this.reqsmatch != null) {
            if (!ValueCheckUtil.isNullORZeroLength(this.mapyethandvo)) {
                OnhandVO[] handvos = (OnhandVO[])this.mapyethandvo.values().toArray(new OnhandVO[this.mapyethandvo.size()]);
                this.getOnhandRes().matchRes(handvos, this);
                this.allocSn(handvos);
            }
        }
    }

    public void setCtranstype(List<String> ctranstypes) {
        this.ctranstypes = ctranstypes;
    }

    private void allocSn(OnhandVO[] handvos) {
        if (!ValueCheckUtil.isNullORZeroLength(handvos) && !ValueCheckUtil.isNullORZeroLength(this.mapResults)) {
            Set<String> onhandpks = VOEntityUtil.getVOsValueSet(handvos, "pk_onhanddim");
            List<String>[] ls = this.getOuttrackinOnhandRes();
            if (!ValueCheckUtil.isNullORZeroLength(ls) && !ValueCheckUtil.isNullORZeroLength(ls[0])) {
                this.getOnhandRes().loadSNRes(handvos, (String[])ls[0].toArray(new String[ls[0].size()]), (String[])ls[1].toArray(new String[ls[1].size()]));
            } else {
                this.getOnhandRes().loadSNRes(handvos, (String[])null, (String[])null);
            }

            OnhandSNVO[] snvos = null;
            Iterator i$ = this.mapResults.values().iterator();

            while(true) {
                OnhandBalanceResult rts;
                do {
                    if (!i$.hasNext()) {
                        return;
                    }

                    rts = (OnhandBalanceResult)i$.next();
                } while(rts.getResults() == null);

                Iterator i$2 = rts.getResults().iterator();

                while(i$2.hasNext()) {
                    BalanceOnhandRes res = (BalanceOnhandRes)i$2.next();
                    if (res.getNastnum() != null && onhandpks.contains(res.getOnhanddimvo().getPk_onhanddim())) {
                        snvos = this.getOnhandRes().getSNRes(res.getOnhanddimvo(), res.getCgeneralbid(), res.getNastnum().intValue());
                        res.addSnVo(snvos);
                    }
                }
            }
        }
    }

    private void assignRes(OnhandBalanceResult<T> balance, OnhandRes res) {
        if (res.isUseable()) {
            UFDouble dtemp = null;
            BalanceOnhandRes balanceres = new BalanceOnhandRes(res);
            if (res.isNumUseable()) {
                dtemp = NCBaseTypeUtils.sub(res.getNuseablenum(), new UFDouble[]{balance.getNreqnum()});
                if (NCBaseTypeUtils.isGtZero(dtemp)) {
                    balanceres.setNnum(balance.getNreqnum());
                    balance.setNreqnum(UFDouble.ZERO_DBL);
                    res.setNuseablenum(dtemp);
                } else {
                    balanceres.setNnum(res.getNuseablenum());
                    balance.setNreqnum(NCBaseTypeUtils.sub(balance.getNreqnum(), new UFDouble[]{balanceres.getNnum()}));
                    res.setNuseablenum(UFDouble.ZERO_DBL);
                }
            } else {
                balanceres.setNnum(UFDouble.ZERO_DBL);
            }

            if (res.isAstnumUseable()) {
                dtemp = NCBaseTypeUtils.sub(res.getNuseableastnum(), new UFDouble[]{balance.getNreqastnum()});
                if (NCBaseTypeUtils.isGtZero(dtemp)) {
                    balanceres.setNastnum(balance.getNreqastnum());
                    balance.setNreqastnum(UFDouble.ZERO_DBL);
                    res.setNuseableastnum(dtemp);
                } else {
                    balanceres.setNastnum(res.getNuseableastnum());
                    balance.setNreqastnum(NCBaseTypeUtils.sub(balance.getNreqastnum(), new UFDouble[]{balanceres.getNastnum()}));
                    res.setNuseableastnum(UFDouble.ZERO_DBL);
                }
            } else {
                balanceres.setNastnum(UFDouble.ZERO_DBL);
            }

            if (res.isGrossnumUseable()) {
                dtemp = NCBaseTypeUtils.sub(res.getNuseablegrossnum(), new UFDouble[]{balance.getNreqgrossnum()});
                if (NCBaseTypeUtils.isGtZero(dtemp)) {
                    balanceres.setNgrossnum(balance.getNreqgrossnum());
                    balance.setNreqgrossnum(UFDouble.ZERO_DBL);
                    res.setNuseablegrossnum(dtemp);
                } else {
                    balanceres.setNgrossnum(res.getNuseablegrossnum());
                    balance.setNreqgrossnum(NCBaseTypeUtils.sub(balance.getNreqgrossnum(), new UFDouble[]{balanceres.getNgrossnum()}));
                    res.setNuseablegrossnum(UFDouble.ZERO_DBL);
                }
            } else {
                balanceres.setNgrossnum(UFDouble.ZERO_DBL);
            }

            balance.addBalanceOnhandRes(balanceres);
        }
    }

    private void filterReqsData() {
        if (!ValueCheckUtil.isNullORZeroLength(this.onhandReq)) {
            List<T> datas = new ArrayList();
            List<T> sndatas = new ArrayList();
            IOnhandReq[] arrtemp = this.onhandReq;
            int len$ = arrtemp.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                T req = (T)arrtemp[i$];
                if (req != null && req.getOnhandDim() != null && req.getOnhandDim().getPk_org() != null && req.getOnhandDim().getCmaterialvid() != null && req.getOnhandDim().getCmaterialoid() != null && (!NCBaseTypeUtils.isNullOrZero(req.getReqNum()) || !NCBaseTypeUtils.isNullOrZero(req.getReqAssistNum()) || !NCBaseTypeUtils.isNullOrZero(req.getReqGrossNum()))) {
                    if (this.isNeedSearchSN(req)) {
                        sndatas.add(req);
                    } else {
                        datas.add(req);
                    }
                }
            }

            if (datas.size() > 0) {
                arrtemp = (IOnhandReq[])((IOnhandReq[])Array.newInstance(((IOnhandReq)datas.get(0)).getClass(), datas.size()));
                this.onhandReq = (T[])datas.toArray(arrtemp);
            } else {
                this.onhandReq = null;
            }

            if (sndatas.size() > 0) {
                arrtemp = (IOnhandReq[])((IOnhandReq[])Array.newInstance(((IOnhandReq)sndatas.get(0)).getClass(), sndatas.size()));
                this.onhandSNReq = (T[])sndatas.toArray(arrtemp);
            }

        }
    }

    private boolean isNeedSearchSN(T req) {
        ICLocationVO[] locationvos = req.getLocationVOs();
        if (ValueCheckUtil.isNullORZeroLength(locationvos)) {
            return false;
        } else {
            String key = SysInitGroupQuery.isSNEnabled() ? "pk_serialcode" : "vserialcode";
            String[] snvalues = (String[])VOEntityUtil.getVOsValuesNotDel(locationvos, key, String.class);
            if (StringUtil.isSEmptyOrNullForAll(snvalues)) {
                return false;
            } else {
                return !NCBaseTypeUtils.isNullOrZero(req.getReqAssistNum()) && snvalues.length == req.getReqAssistNum().intValue();
            }
        }
    }

    private OnhandResManager getOnhandRes() {
        if (this.onhandres == null) {
            this.onhandres = new OnhandResManager();
            if (!ValueCheckUtil.isNullORZeroLength(this.ctranstypes)) {
                this.onhandres.setCtranstype(this.ctranstypes);
            }
        }

        return this.onhandres;
    }

    private List<String>[] getOuttrackinOnhandRes() {
        if (ValueCheckUtil.isNullORZeroLength(this.mapResults)) {
            return null;
        } else {
            List<String> listbid = new ArrayList();
            List<String> listbilltype = new ArrayList();
            String cgeneralbid = null;
            Iterator i$ = this.mapResults.values().iterator();

            while(true) {
                OnhandBalanceResult rts;
                do {
                    if (!i$.hasNext()) {
                        if (listbid.size() <= 0) {
                            return null;
                        }

                        return new List[]{listbilltype, listbid};
                    }

                    rts = (OnhandBalanceResult)i$.next();
                } while(rts.getResults() == null);

                Iterator i$2 = rts.getResults().iterator();

                while(i$2.hasNext()) {
                    BalanceOnhandRes res = (BalanceOnhandRes)i$2.next();
                    cgeneralbid = res.getCgeneralbid();
                    if (cgeneralbid != null && !listbid.contains(cgeneralbid)) {
                        listbid.add(cgeneralbid);
                        listbilltype.add(res.getCbilltype());
                    }
                }
            }
        }
    }

    private OnhandVO[] preAllocHandRes(OnhandVO[] handvos) {
        if (ValueCheckUtil.isNullORZeroLength(handvos)) {
            return handvos;
        } else {
            this.getOnhandRes().loadInv(handvos);
            this.processNrsastnum(handvos);
            List<OnhandVO> lrethandvos = new ArrayList();
            List<OnhandVO> lprehandvos = new ArrayList();
            OnhandVO[] arr$ = handvos;
            int len$ = handvos.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                OnhandVO handvo = arr$[i$];
                if (!NCBaseTypeUtils.isLEZero(OnhandVOTools.calcHandNum(handvo)) || !NCBaseTypeUtils.isLEZero(OnhandVOTools.calcHandAstNum(handvo))) {
                    if (this.getOnhandRes().isNeedSearchFlow(handvo.getPk_org(), handvo.getCmaterialvid())) {
                        lrethandvos.add(handvo);
                    } else {
                        OnhandRes handres = new OnhandRes(handvo);
                        List<T> lreqs = this.reqsmatch.searchMatchedDimObj(handres, -1);
                        if (!ValueCheckUtil.isNullORZeroLength(lreqs)) {
                            OnhandBalanceResult<T> balancerow = null;
                            lprehandvos.add(handvo);
                            Iterator i$2 = lreqs.iterator();

                            while(i$2.hasNext()) {
                                T req = (T)i$2.next();
                                balancerow = (OnhandBalanceResult)this.mapResults.get(req);
                                if (balancerow == null) {
                                    balancerow = new OnhandBalanceResult(req);
                                    this.mapResults.put(req, balancerow);
                                }

                                boolean isNumAssign = handres.isNumUseable();
                                boolean isAstNumAssign = handres.isAstnumUseable();
                                boolean isGrossNumAssign = handres.isGrossnumUseable();
                                this.assignRes(balancerow, handres);
                                if (balancerow.isFull(isNumAssign, isAstNumAssign, isGrossNumAssign)) {
                                    this.reqsmatch.remoteMatchedDimObj(req);
                                }

                                if (!handres.isUseable()) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (lprehandvos.size() > 0) {
                this.allocSn((OnhandVO[])CollectionUtils.listToArray(lprehandvos));
            }

            return (OnhandVO[])CollectionUtils.listToArray(lrethandvos);
        }
    }

    private void processNrsastnum(OnhandVO[] handvos) {
        Map<String, InvMeasVO> mapmeasInv = this.loadInvMeas(handvos);
        if (!ValueCheckUtil.isNullORZeroLength(mapmeasInv)) {
            List<OnhandVO> reserveVOs = new ArrayList();
            OnhandVO[] arr$ = handvos;
            int len$ = handvos.length;

            String cunitid;
            for(int i$ = 0; i$ < len$; ++i$) {
                OnhandVO onhandVO = arr$[i$];
                cunitid = onhandVO.getCmaterialvid() + onhandVO.getCastunitid();
                InvMeasVO invmeasVO = (InvMeasVO)mapmeasInv.get(cunitid);
                if (invmeasVO == null) {
                    reserveVOs.add(onhandVO);
                } else if (ValueCheckUtil.isTrue(invmeasVO.getIsstorebalance()) && !NCBaseTypeUtils.isNullOrZero(onhandVO.getNrsnum())) {
                    reserveVOs.add(onhandVO);
                }
            }

            if (!ValueCheckUtil.isNullORZeroLength(reserveVOs)) {
                Iterator i$ = reserveVOs.iterator();

                while(i$.hasNext()) {
                    OnhandVO onhandVO = (OnhandVO)i$.next();
                    InvMeasVO invmeasvo = (InvMeasVO)mapmeasInv.get(onhandVO.getCmaterialvid() + onhandVO.getCastunitid());
                    if (invmeasvo == null) {
                        onhandVO.setNrsastnum(onhandVO.getNrsnum());
                    } else {
                        String convertRate = invmeasvo.getMeasrate();
                        cunitid = invmeasvo.getPk_measdoc();
                        UFDouble nrsnum = onhandVO.getNrsnum();
                        onhandVO.setNrsastnum(this.calculator.calculateAstNum(nrsnum, convertRate, cunitid));
                    }
                }

            }
        }
    }

    private Map<String, InvMeasVO> loadInvMeas(OnhandVO[] handvos) {
        Map<String, InvMeasVO> mapInv = new HashMap();
        if (handvos != null && handvos.length > 0) {
            ICBSContext con = new ICBSContext();
            InvMeasVO[] invvos = con.getInvInfo().getInvMeasVO((String[])VOEntityUtil.getVOsValues(handvos, "cmaterialvid", String.class), (String[])VOEntityUtil.getVOsValues(handvos, "castunitid", String.class));
            if (invvos == null) {
                return null;
            } else {
                InvMeasVO[] arr$ = invvos;
                int len$ = invvos.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    InvMeasVO invvo = arr$[i$];
                    if (invvo != null) {
                        mapInv.put(invvo.getPk_material() + invvo.getPk_measdoc(), invvo);
                    }
                }

                return mapInv;
            }
        } else {
            return null;
        }
    }

    private Map<String, InvBasVO> loadInvBas(OnhandVO[] handvos) {
        Map<String, InvBasVO> mapInv = new HashMap();
        if (handvos != null && handvos.length > 0) {
            ICBSContext con = new ICBSContext();
            InvBasVO[] invvos = con.getInvInfo().getInvBasVO((String[])VOEntityUtil.getVOsValues(handvos, "cmaterialvid", String.class));
            if (invvos == null) {
                return null;
            } else {
                InvBasVO[] arr$ = invvos;
                int len$ = invvos.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    InvBasVO invvo = arr$[i$];
                    if (invvo != null) {
                        mapInv.put(invvo.getPk_material(), invvo);
                    }
                }

                return mapInv;
            }
        } else {
            return null;
        }
    }

    private void processAllocSNData() {
        if (!ValueCheckUtil.isNullORZeroLength(this.onhandSNReq)) {
            Set<String> llist = this.getAllSNValues();
            OnhandSNViewVO[] snvos = this.queryOnhandSNs(llist);
            if (!ValueCheckUtil.isNullORZeroLength(snvos)) {
                if (ValueCheckUtil.isNullORZeroLength(this.mapResults)) {
                    this.mapResults = new HashMap();
                }

                String key = SysInitGroupQuery.isSNEnabled() ? "pk_serialcode" : "vserialcode";
                Map<String, OnhandSNViewVO> onhandsnmap = CollectionUtils.hashVOArray(key, snvos);
                IOnhandReq[] arr$ = this.onhandSNReq;
                int len$ = arr$.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    T req = (T)arr$[i$];
                    this.processAllocSNReq(key, onhandsnmap, req);
                }

            }
        }
    }

    private void processAllocSNReq(String key, Map<String, OnhandSNViewVO> onhandsnmap, T req) {
        OnhandBalanceResult<T> balancerow = (OnhandBalanceResult)this.mapResults.get(req);
        if (balancerow == null) {
            balancerow = new OnhandBalanceResult(req);
            this.mapResults.put(req, balancerow);
        }

        List<OnhandSNViewVO> snList = new ArrayList();
        ICLocationVO[] lovs = req.getLocationVOs();
        ICLocationVO[] arr$ = lovs;
        int len$ = lovs.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            ICLocationVO lov = arr$[i$];
            String keyvalue = (String)lov.getAttributeValue(key);
            snList.add(onhandsnmap.get(keyvalue));
        }

        Map<String, List<CircularlyAccessibleValueObject>> snmap = VOEntityUtil.groupVOByKeys(new String[]{"pk_onhanddim"}, (CircularlyAccessibleValueObject[])CollectionUtils.listToArray(snList));
        Iterator i$ = snmap.entrySet().iterator();

        while(i$.hasNext()) {
            Entry<String, List<OnhandSNViewVO>> entry = (Entry)i$.next();
            BalanceOnhandRes res = new BalanceOnhandRes();
            res.setOnhanddimvo(((OnhandSNViewVO)((List)entry.getValue()).get(0)).getOnhandDimVO());
            List<OnhandSNVO> listsnvo = new ArrayList();
            Iterator i$2 = ((List)entry.getValue()).iterator();

            while(i$2.hasNext()) {
                OnhandSNViewVO viewvo = (OnhandSNViewVO)i$2.next();
                listsnvo.add(viewvo.getOnhandSNVO());
            }

            res.setListsnvo(listsnvo);
            UFDouble[] dsums = VOEntityUtil.sumVOsFieldValuesNotDel((CircularlyAccessibleValueObject[])CollectionUtils.listToArray((List)entry.getValue()), new String[]{"nonhandnum", "nonhandastnum"});
            res.setNnum(dsums[0]);
            res.setNastnum(dsums[1]);
            balancerow.addBalanceOnhandRes(res);
        }

    }

    private OnhandSNViewVO[] queryOnhandSNs(Set<String> llist) {
        OnhandSelectDim select = new OnhandSelectDim();
        select.addSelectFields(OnhandDimVO.getContentFields());
        OnhandSNViewVO[] snvos = null;

        try {
            if (SysInitGroupQuery.isSNEnabled()) {
                snvos = ((IOnhandQry)NCLocator.getInstance().lookup(IOnhandQry.class)).queryOnhandSNBySNCode(select, (String[])CollectionUtils.setToArray(llist));
            } else {
                snvos = ((IOnhandQry)NCLocator.getInstance().lookup(IOnhandQry.class)).queryOnhandSNBySNCodes(select, (String[])CollectionUtils.setToArray(llist));
            }
        } catch (BusinessException var5) {
            ExceptionUtils.wrappException(var5);
        }

        return snvos;
    }

    private Set<String> getAllSNValues() {
        Set<String> llist = new HashSet();
        String key = SysInitGroupQuery.isSNEnabled() ? "pk_serialcode" : "vserialcode";
        IOnhandReq[] arr$ = this.onhandSNReq;
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            T onhandSN = (T)arr$[i$];
            String[] value = (String[])VOEntityUtil.getVOsValuesNotDel(onhandSN.getLocationVOs(), key, String.class);
            CollectionUtils.addArrayToSet(llist, value);
        }

        return llist;
    }

    private void processData() {
        this.filterReqsData();
        if (!ValueCheckUtil.isNullORZeroLength(this.onhandReq)) {
            OnhandDimVO[] dimvos = new OnhandDimVO[this.onhandReq.length];

            for(int i = 0; i < this.onhandReq.length; ++i) {
                dimvos[i] = this.onhandReq[i].getOnhandDim();
            }

            dimvos = OnhandVOTools.getRealOnhandDim((new ICBSContext()).getInvInfo(), dimvos);
            OnhandVO[] handvos = this.getOnhandRes().loadOnhandRes(dimvos);
            if (!ValueCheckUtil.isNullORZeroLength(handvos)) {
                this.reqsmatch = new DimMatchUtil(this.onhandReq, OnhandDimVO.getDimContentFields(), new String[]{"pk_group", "pk_org", "cwarehouseid", "cmaterialoid", "cmaterialvid"});
                this.mapResults = new HashMap();
                handvos = this.preAllocHandRes(handvos);
                if (ValueCheckUtil.isNullORZeroLength(handvos)) {
                    this.reqsmatch = null;
                } else {
                    this.processOnhandData(handvos);
                }
            }
        }
    }

    private void processOnhandData(OnhandVO[] handvos) {
        String key = null;
        this.mapyethandvo = new HashMap();
        OnhandVO[] arr$ = handvos;
        int len$ = handvos.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            OnhandVO handvo = arr$[i$];
            key = handvo.getAttributeValue("vsubhashcode") + handvo.getClocationid();
            this.mapyethandvo.put(key, handvo);
        }

    }

    private boolean processResDataForHand(OnhandRes handres) {
        String onhandkey = handres.getOnhanddimvo().getVsubhashcode() + handres.getOnhanddimvo().getClocationid();
        OnhandVO handvo = (OnhandVO)this.mapyethandvo.get(onhandkey);
        if (handvo == null) {
            handres.setNuseablenum(UFDouble.ZERO_DBL);
            handres.setNuseableastnum(UFDouble.ZERO_DBL);
            handres.setNuseablegrossnum(UFDouble.ZERO_DBL);
            return true;
        } else {
            handres.getOnhanddimvo().setPk_onhanddim(handvo.getPk_onhanddim());
            boolean byetoverhand = false;
            UFDouble onhandnum = OnhandVOTools.calcHandNum(handvo);
            UFDouble dtemp = NCBaseTypeUtils.sub(onhandnum, new UFDouble[]{handres.getNuseablenum()});
            if (NCBaseTypeUtils.isLEZero(dtemp)) {
                handres.setNuseablenum(onhandnum);
                handvo.setNonhandnum(UFDouble.ZERO_DBL);
            }

            UFDouble onhandgrossnum;
            UFDouble dtemp2;
            if (handvo.getCastunitid() != null) {
                onhandgrossnum = OnhandVOTools.calcRealAstHandNum(handvo.getOnhandNumVO());
                dtemp2 = NCBaseTypeUtils.sub(onhandgrossnum, new UFDouble[]{handres.getNuseableastnum()});
                if (NCBaseTypeUtils.isLEZero(dtemp2)) {
                    handres.setNuseableastnum(onhandgrossnum);
                    handvo.setNonhandastnum(UFDouble.ZERO_DBL);
                }
            } else {
                handres.setNuseableastnum(this.calculator.calculateAstNum(handres.getNuseablenum(), handres.getOnhanddimvo().getVchangerate(), handres.getOnhanddimvo().getCastunitid()));
                handvo.setNonhandastnum(UFDouble.ZERO_DBL);
            }

            if (NCBaseTypeUtils.isGtZero(handvo.getNgrossnum())) {
                onhandgrossnum = OnhandVOTools.calcRealGrossHandNum(handvo.getOnhandNumVO());
                dtemp2 = NCBaseTypeUtils.sub(onhandgrossnum, new UFDouble[]{handres.getNuseablegrossnum()});
                if (NCBaseTypeUtils.isLEZero(dtemp2)) {
                    handres.setNuseablegrossnum(onhandgrossnum);
                    handvo.setNgrossnum(UFDouble.ZERO_DBL);
                }
            }

            if (NCBaseTypeUtils.isLEZero(OnhandVOTools.calcHandNum(handvo)) && NCBaseTypeUtils.isLEZero(OnhandVOTools.calcRealAstHandNum(handvo.getOnhandNumVO()))) {
                byetoverhand = true;
                this.mapyethandvo.remove(onhandkey);
            }

            return byetoverhand;
        }
    }
}