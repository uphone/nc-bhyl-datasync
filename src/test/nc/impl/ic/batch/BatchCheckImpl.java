
package nc.impl.ic.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nc.bs.ic.pub.env.ICBSContext;
import nc.impl.pubapp.env.BSContext;
import nc.itf.ic.batch.IBatchCheck;
import nc.vo.ic.general.define.ICBillBodyVO;
import nc.vo.ic.general.define.ICBillVO;
import nc.vo.ic.material.define.InvCalBodyVO;
import nc.vo.ic.material.define.InvCalbodyKey;
import nc.vo.ic.pub.lang.BatchRes;
import nc.vo.ic.pub.lang.ResBase;
import nc.vo.ic.pub.util.NCBaseTypeUtils;
import nc.vo.ic.pub.util.StringUtil;
import nc.vo.ic.pub.util.ValueCheckUtil;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pubapp.pattern.data.ValueUtils;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.pubapp.pattern.pub.AssertUtils;
import org.apache.commons.lang.ArrayUtils;

public class BatchCheckImpl implements IBatchCheck {
    public BatchCheckImpl() {
    }

    public void checkBatchAttribute(ICBillVO[] bills) throws BusinessException {
        try {
            if (ArrayUtils.isEmpty(bills)) {
                return;
            }

            Set<String> keySet = new HashSet();
            List<InvCalbodyKey> keys = new ArrayList();
            ICBillVO[] arr$ = bills;
            int len$ = bills.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                ICBillVO bill = arr$[i$];
                ICBillBodyVO[] arr$2 = bill.getBodys();
                int len$2 = arr$2.length;

                for(int i$2 = 0; i$2 < len$2; ++i$2) {
                    ICBillBodyVO body = arr$2[i$2];
                    if (!keySet.contains(bill.getHead().getPk_org() + body.getCmaterialvid()) && body.getStatus() != 3 && body.getNnum() != null) {
                        keys.add(new InvCalbodyKey(bill.getHead().getPk_org(), body.getCmaterialvid()));
                        keySet.add(bill.getHead().getPk_org() + body.getCmaterialvid());
                    }
                }
            }

            if (ValueCheckUtil.isNullORZeroLength(keys)) {
                return;
            }

            Map<InvCalbodyKey, InvCalBodyVO> invInfoMap = (new ICBSContext()).getInvInfo().getInvCalBodyVOs((InvCalbodyKey[])keys.toArray(new InvCalbodyKey[keys.size()]));
            Map<String, InvCalBodyVO> cInvInfoMap = this.changeRet(invInfoMap);
            ICBillVO[] arr$3 = bills;
            int len$3 = bills.length;

            for(int i$ = 0; i$ < len$3; ++i$) {
                ICBillVO bill = arr$3[i$];
                StringBuilder errStr = new StringBuilder();
                List<String> errRowNos = new ArrayList();
                this.checkBatchcode(bill, cInvInfoMap, errStr, errRowNos, true);
                if (!StringUtil.isSEmptyOrNull(errStr.toString())) {
                    List<String> cpickmbids = this.getCPickMBids(bill, errRowNos);
                    BSContext.getInstance().setSession("ICSessionContainSrcBid", cpickmbids);
                    throw new BusinessException(errStr.toString());
                }
            }
        } catch (Exception var13) {
            ExceptionUtils.marsh(var13);
        }

    }

    private Map<String, InvCalBodyVO> changeRet(Map<InvCalbodyKey, InvCalBodyVO> invInfoMap) {
        Map<String, InvCalBodyVO> cInvInfoMap = new HashMap();
        Iterator i$ = invInfoMap.values().iterator();

        while(i$.hasNext()) {
            InvCalBodyVO vo = (InvCalBodyVO)i$.next();
            cInvInfoMap.put(vo.getPk_org() + vo.getPk_material(), vo);
        }

        return cInvInfoMap;
    }

    public void checkBatchAttribute(ICBillVO bill) throws BusinessException {
        try {
            if (bill == null || bill.getBodys() == null) {
                return;
            }

            String pk_org = bill.getHead().getPk_org();
            List<String> cmaterialVIDs = new ArrayList();
            ICBillBodyVO[] arr$ = bill.getBodys();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                ICBillBodyVO body = arr$[i$];
                if (body.getStatus() != 3 && !cmaterialVIDs.contains(body.getCmaterialvid()) && body.getNnum() != null) {
                    cmaterialVIDs.add(body.getCmaterialvid());
                }
            }

            Map<String, InvCalBodyVO> materialVOs = this.fetchMaterialInfo(pk_org, cmaterialVIDs);
            StringBuilder errStr = new StringBuilder();
            List<String> errRowNos = new ArrayList();
            this.checkBatchcode(bill, materialVOs, errStr, errRowNos, true);
            if (!StringUtil.isSEmptyOrNull(errStr.toString())) {
                List<String> cpickmbids = this.getCPickMBids(bill, errRowNos);
                BSContext.getInstance().setSession("ICSessionContainSrcBid", cpickmbids);
                throw new BusinessException(errStr.toString());
            }
        } catch (BusinessException var8) {
            ExceptionUtils.marsh(var8);
        }

    }

    public void checkBatchAttributeFor4555(ICBillVO bill) throws BusinessException {
        try {
            if (bill == null || bill.getBodys() == null) {
                return;
            }

            String pk_org = bill.getHead().getPk_org();
            List<String> cmaterialVIDs = new ArrayList();
            ICBillBodyVO[] arr$ = bill.getBodys();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                ICBillBodyVO body = arr$[i$];
                if (body.getStatus() != 3 && !cmaterialVIDs.contains(body.getCmaterialvid()) && !NCBaseTypeUtils.isNullOrZero(body.getNnum())) {
                    cmaterialVIDs.add(body.getCmaterialvid());
                }
            }

            Map<String, InvCalBodyVO> materialVOs = this.fetchMaterialInfo(pk_org, cmaterialVIDs);
            StringBuilder errStr = new StringBuilder();
            List<String> errRowNos = new ArrayList();
            this.checkBatchcode(bill, materialVOs, errStr, errRowNos, false);
            if (!StringUtil.isSEmptyOrNull(errStr.toString())) {
                List<String> cpickmbids = this.getCPickMBids(bill, errRowNos);
                BSContext.getInstance().setSession("ICSessionContainSrcBid", cpickmbids);
                throw new BusinessException(errStr.toString());
            }
        } catch (BusinessException var8) {
            ExceptionUtils.marsh(var8);
        }

    }

    private void checkBatchcode(ICBillVO bill, Map<String, InvCalBodyVO> materialVOs, StringBuilder errStr, List<String> errRowNos, boolean checkQualFlag) {
        ICBillBodyVO[] arr$ = bill.getBodys();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            ICBillBodyVO body = arr$[i$];
            if (body.getStatus() != 3 && body.getNnum() != null && !StringUtil.isSEmptyOrNull(body.getCmaterialvid())) {
                InvCalBodyVO materialVO = (InvCalBodyVO)materialVOs.get(bill.getHead().getPk_org() + body.getCmaterialvid());
                AssertUtils.assertValue(materialVO != null, BatchRes.getMaterialStockErr());
                boolean isError = this.checkWholeMngInv(materialVO, body, errStr, errRowNos, checkQualFlag);
                if (!isError) {
                    this.checkNonWholeMngInv(materialVO, body, errStr, errRowNos);
                }
            }
        }

    }

    private void checkNonWholeMngInv(InvCalBodyVO materialVO, ICBillBodyVO body, StringBuilder errStr, List<String> errRowNos) {
        if (materialVO == null || materialVO.getWholemanaflag() == null || !materialVO.getWholemanaflag().equals(UFBoolean.TRUE)) {
            if (!StringUtil.isSEmptyOrNull(body.getPk_batchcode()) || !StringUtil.isSEmptyOrNull(body.getVbatchcode())) {
                this.showErr(body, errStr, errRowNos, BatchRes.getNonWholeMngErr());
            }

        }
    }

    private boolean checkValidate(ICBillBodyVO body, int qualityUnit, int qualityNum, StringBuilder errStr, List<String> errRowNos) {
        if (body.getDproducedate() != null && body.getDvalidate() != null) {
            UFDate dproduceDate = body.getDproducedate();
            UFDate dvalidateDate = body.getDvalidate();
            if (dproduceDate != null && dvalidateDate != null && dvalidateDate.before(dproduceDate)) {
            }

            return false;
        } else {
            this.showErr(body, errStr, errRowNos, BatchRes.getBatchEmptyErr());
            return true;
        }
    }

    private boolean checkWholeMngInv(InvCalBodyVO materialVO, ICBillBodyVO body, StringBuilder errStr, List<String> errRowNos, boolean checkQualFlag) {
        if (materialVO != null && materialVO.getWholemanaflag() != null && materialVO.getWholemanaflag().equals(UFBoolean.TRUE)) {
            if (this.isBatchEmpty(body)) {
                this.showErr(body, errStr, errRowNos, BatchRes.getBatchEmptyErr());
                return true;
            }

            if (checkQualFlag && ValueUtils.getUFBoolean(materialVO.getQualitymanflag()).equals(UFBoolean.TRUE)) {
                return this.checkValidate(body, materialVO.getQualityunit(), materialVO.getQualitynum(), errStr, errRowNos);
            }
        }

        return false;
    }

    private Map<String, InvCalBodyVO> fetchMaterialInfo(String pk_org, List<String> cmaterialVIDs) {
        Map<String, InvCalBodyVO> retMap = new HashMap();
        ICBSContext context = new ICBSContext();
        InvCalBodyVO[] stockVOs = context.getInvInfo().getInvCalBodyVO(pk_org, (String[])cmaterialVIDs.toArray(new String[cmaterialVIDs.size()]));
        if (stockVOs != null && stockVOs.length != 0) {
            InvCalBodyVO[] arr$ = stockVOs;
            int len$ = stockVOs.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                InvCalBodyVO vo = arr$[i$];
                retMap.put(vo.getPk_org() + vo.getPk_material(), vo);
            }

            return retMap;
        } else {
            return retMap;
        }
    }

    private List<String> getCPickMBids(ICBillVO bill, List<String> errRowNos) {
        if (errRowNos != null && errRowNos.size() != 0) {
            List<String> cpickmbids = new ArrayList();
            Iterator i$1 = errRowNos.iterator();

            while(i$1.hasNext()) {
                String rowNo = (String)i$1.next();
                ICBillBodyVO[] arr$ = bill.getChildrenVO();
                int len$ = arr$.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    ICBillBodyVO body = arr$[i$];
                    if (rowNo.equals(body.getCrowno())) {
                        String cpickmbid = (String)body.getAttributeValue("cpickmbid");
                        cpickmbids.add(cpickmbid);
                    }
                }
            }

            return cpickmbids;
        } else {
            return null;
        }
    }

    private boolean isBatchEmpty(ICBillBodyVO body) {
        boolean isBatchEmpty = StringUtil.isSEmptyOrNull(body.getVbatchcode());
        boolean isNumNotEmpty = body.getNnum() != null && !NCBaseTypeUtils.isNullOrZero(body.getNnum());
        return isBatchEmpty && isNumNotEmpty;
    }

    private void showErr(ICBillBodyVO body, StringBuilder errStrSum, List<String> errRowNos, String str) {
        errStrSum.append(str).append("\n").append(ResBase.getCrowno()).append(":").append(body.getCrowno()).append("\n");
        errRowNos.add(body.getCrowno());
    }
}
