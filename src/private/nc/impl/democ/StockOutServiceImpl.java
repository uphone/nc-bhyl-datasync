/**
 
 1, ������κŲ�Ϊ�գ����ѯ���κŵ���У�����κ��Ƿ���ȷ
   ������κŴ��ڣ� �����λ��Ϊ�գ���У�鵥λ�����Ƿ���ڣ�
      �����λ������ڣ�����ݻ���+�ֿ�+���ϲ�ѯ�ִ�����Ȼ����е�λƥ�䣬����������򱨴�
2��������κ�Ϊ�գ����Զ����
   �����ȡ�������κţ�����е�λУ��
   ���δ��ȡ�����κţ��򱨴�

 */
package nc.impl.democ;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.uap.oid.OidGenerator;
import nc.itf.democ.IStockOutService;
import nc.itf.ic.onhand.OnhandResService;
//import nc.itf.pub.web.ILoginQueryService;
import nc.itf.uap.pf.IplatFormEntry;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.jdbc.framework.processor.MapProcessor;
import nc.pubitf.ic.onhand.IOnhandQry;
import nc.vo.ic.general.define.ICBillBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutHeadVO;
import nc.vo.ic.m4d.entity.MaterialOutVO;
import nc.vo.ic.onhand.define.ICBillPickResults;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pub.workflownote.WorkflownoteVO;
import nc.vo.sm.UserVO;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StockOutServiceImpl implements IStockOutService {

	@Override
	public String stockOut(String json) {
		String time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		// IOnhandQry
		BaseDAO dao = null;
		String billType = "4D-01";
		UFDate today = new UFDate();
		try {

			ObjectMapper mapper = new ObjectMapper();
			HashMap param = mapper.readValue(json, HashMap.class);
			if (param == null || param.size() == 0) {
				throw new Exception("��������Ϊ��");
			}
			// ������״��롢�û�������¼���롢�������롢�ֿ���롢���ϴ���
			String accountCode = (String) param.get("accountCode");
			if (accountCode == null || "".equals(accountCode.trim())) {
				throw new Exception("���״��벻��Ϊ��[accountCode]");
			}
			dao = new BaseDAO(accountCode);

			String userCode = (String) param.get("userCode");
			if (userCode == null || "".equals(userCode.trim())) {
				throw new Exception("�û�������Ϊ��[userCode]");
			}
			String userPassword = (String) param.get("userPassword");
			if (userPassword == null || "".equals(userPassword.trim())) {
				throw new Exception("�û����벻��Ϊ��[userPassword]");
			}

			// check user & password
//			String pwd = new String(Base64.decodeBase64(userPassword.getBytes()));
//			ILoginQueryService loginQueryService = NCLocator.getInstance().lookup(ILoginQueryService.class);
//			UserVO userVO = loginQueryService.getUserVOByUserPass(userCode, pwd);
//			if (userVO == null) {
//				throw new Exception("�û������������");
//			}

			// check businessInfo
			if (!param.containsKey("businessInfo")) {
				throw new Exception("ȱʧ����[businessInfo]");
			}
			Object businessInfoObj = param.get("businessInfo");
			if (!(businessInfoObj instanceof Map)) {
				throw new Exception("�������ʹ���[businessInfo]");
			}
			Map businessInfo = (Map) businessInfoObj;
			if (!businessInfo.containsKey("head")) {
				throw new Exception("ȱʧ����[businessInfo.head]");
			}
			Object headObj = businessInfo.get("head");
			if (!(headObj instanceof Map)) {
				throw new Exception("�������ʹ���[businessInfo.head]");
			}
			Map head = (Map) headObj;
			if (!businessInfo.containsKey("children")) {
				throw new Exception("ȱʧ����[businessInfo.children]");
			}
			Object childrenObj = businessInfo.get("children");
			if (!(childrenObj instanceof List)) {
				throw new Exception("�������ʹ���[businessInfo.children]");
			}
			List children = (List) childrenObj;
			if (children == null || children.size() == 0) {
				throw new Exception("��������Ϊ��[businessInfo.children]");
			}

			IOnhandQry onhandQuery = NCLocator.getInstance().lookup(IOnhandQry.class);
			OnhandResService onhandResService = NCLocator.getInstance().lookup(OnhandResService.class);

			MaterialOutVO aggVO = new MaterialOutVO();
			MaterialOutHeadVO headVO = new MaterialOutHeadVO();
			MaterialOutBodyVO[] childVOs = new MaterialOutBodyVO[children.size()];
			String yybm = (String) head.get("YYBM");
			String ckbm = (String) head.get("CKBM");
			String kdksbm = (String) head.get("KDKSBM");
			String khxm = (String) head.get("KHXM");
			String khdjh = (String) head.get("KHDJH");
			String ssmc = (String) head.get("SSMC");
			String ssrq = (String) head.get("SSRQ");
			String ysygh = (String) head.get("YSYGH");
			String ysxm = (String) head.get("YSXM");
			String hisczygh = (String) head.get("HISCZYGH");
			String hisheadid = (String) head.get("HISHEADID");
			String pxyxmc = (String) head.get("PXYXMC");

			String sql1 = "select pk_org,pk_vid,pk_group from org_orgs where ISBUSINESSUNIT='Y' and ENABLESTATE=2 and code='" + yybm + "'";
			Map orgData = (Map) dao.executeQuery(sql1, new MapProcessor());
			// String pk_org = (String) dao.executeQuery(sql1, new
			// ColumnProcessor());
			if (orgData == null || orgData.size() == 0) {
				throw new Exception("�������벻���ڻ�δ����");
			}
			String pk_org = (String) orgData.get("pk_org");
			String pk_group = (String) orgData.get("pk_group");

			String sql2 = "select pk_stordoc from bd_stordoc where pk_org='" + pk_org + "' and ENABLESTATE=2 and code='" + ckbm + "'";
			String pk_stordoc = (String) dao.executeQuery(sql2, new ColumnProcessor());
			if (pk_stordoc == null || "".equals(pk_stordoc)) {
				throw new Exception("�ֿ���벻���ڻ�δ����");
			}
			String sql3 = "select pk_dept,pk_vid from org_dept where pk_org='" + pk_org + "' and ENABLESTATE=2 and code='" + kdksbm + "'";
			Map deptData = (Map) dao.executeQuery(sql3, new MapProcessor());
			if (deptData == null || deptData.size() == 0) {
				throw new Exception("���Ŵ��벻���ڻ�δ����");
			}
			String pk_dept = (String) deptData.get("pk_dept");

			String sql4 = "select cuserid from sm_user where user_code='" + hisczygh + "'";
			String pk_user = (String) dao.executeQuery(sql4, new ColumnProcessor());
			if (pk_user == null || "".equals(pk_user)) {
				throw new Exception("�û����벻���ڻ�δ����");
			}
			String sql5 = "select pk_billtypeid from bd_billtype where pk_group='" + pk_group + "' and pk_billtypecode='" + billType + "'";
			String pk_billtypeid = (String) dao.executeQuery(sql5, new ColumnProcessor());
			if (pk_billtypeid == null || "".equals(pk_billtypeid)) {
				throw new Exception("��������ʹ��벻��");
			}
			headVO.setCorpoid(pk_org);
			headVO.setPk_group(pk_group);
			headVO.setPk_org(pk_org);
			headVO.setPk_org_v((String) orgData.get("pk_vid"));
			headVO.setCwarehouseid(pk_stordoc);
			headVO.setCdptid(pk_dept);
			headVO.setCdptvid((String) deptData.get("pk_vid"));
			// headVO.setBillmaker(pk_user);
			// headVO.setCreator(pk_user);
			// headVO.setApprover(pk_user);
			// headVO.setTaudittime(today);
			// headVO.setModifier(pk_user);
			headVO.setVdef10(khxm);
			headVO.setVdef11(khdjh);
			headVO.setVdef12(ssmc);
			headVO.setVdef13(ssrq);
			headVO.setVdef14(ysygh);
			headVO.setVdef15(ysxm);
			headVO.setVdef16(hisczygh);
			headVO.setVdef17(hisheadid);
			// TODO
			// headVO.setVdef18(pxyxmc);
			headVO.setVtrantypecode(billType);
			headVO.setCtrantypeid(pk_billtypeid);
			headVO.setDbilldate(today);

			// UFDouble totalNum = UFDouble.ZERO_DBL;
			for (int i = 0; i < children.size(); i++) {
				Map child = (Map) children.get(i);
				String wlbm = (String) child.get("WLBM");
				Object ckzslObj = child.get("CKZSL");
				Object ckfslObj = child.get("CKFSL");
				UFDouble ckzsl = (ckzslObj == null || "".equals(ckzslObj.toString())) ? UFDouble.ZERO_DBL : new UFDouble(ckzslObj.toString());
				// totalNum.add(ckzsl);
				UFDouble ckfsl = (ckfslObj == null || "".equals(ckfslObj.toString())) ? UFDouble.ZERO_DBL : new UFDouble(ckfslObj.toString());
				String dw = (String) child.get("DW");
				String pk_unit = null;
				String pch = (String) child.get("PCH");
				String pk_batchcode = null;
				String dvalidate = null, dinbounddate = null, dproducedate = null;
				String hisbodyid = (String) child.get("HISBODYID");
				String bbddh = (String) child.get("BBDDH");
				String bbddmx = (String) child.get("BBDDMX");
				String pxyxflbm = (String) child.get("PXYXFLBM");

				// code to id
				String sql10 = "select PK_MATERIAL from bd_material where enablestate=2 and code='" + wlbm + "'";
				String pk_material = (String) dao.executeQuery(sql10, new ColumnProcessor());
				if (pk_material == null || "".equals(pk_material)) {
					throw new Exception("���ϴ��벻���ڻ�δ����");
				}
				String sql12 = "select pk_measdoc from bd_measdoc where name='" + dw + "'";
				pk_unit = (String) dao.executeQuery(sql12, new ColumnProcessor());
				if (pk_unit == null || "".equals(pk_unit)) {
					throw new Exception("������λ[" + dw + "]������");
				}

				// ���κż���
				if (pch != null && !"".equals(pch)) {
					String sql11 = "select pk_batchcode,dvalidate,dinbounddate,dproducedate from scm_batchcode where cmaterialoid='" + pk_material + "' and vbatchcode='" + pch + "'";
					Map batchData = (Map) dao.executeQuery(sql11, new MapProcessor());
					if (batchData == null || batchData.size() == 0) {
						throw new Exception("���κŲ�����");
					}
					pk_batchcode = (String) batchData.get("pk_batchcode");
					dvalidate = (String) batchData.get("dvalidate");
					dinbounddate = (String) batchData.get("dinbounddate");
					dproducedate = (String) batchData.get("dproducedate");
					// TODO ��ô��ȡʧЧ���ڣ�
					// �������Ρ����ϡ��ֿ⡢��֯��ѯ�������ϵĿ�浥λ�����е�λУ��
					// String dims[] = { "pk_org", "cwarehouseid",
					// "cmaterialvid" };
					// OnhandQryCond cond = new OnhandQryCond();
					// cond.addSelectFields(dims);
					// cond.setISSum(true);
					// cond.addFilterDimConditon(dims, new Object[] { pk_org,
					// pk_stordoc, pk_material });
					// OnhandVO[] vos = onhandQuery.queryOnhand(cond);
					// if (vos == null || vos.length == 0) {
					// throw new Exception("���ϴ���[" + wlbm + "]�ִ�����ѯΪ��");
					// }
					// boolean passFlag = false;
					// for (int m = 0; m < vos.length; m++) {
					// String pk_batchcode2 = vos[m].getPk_batchcode();
					// String pk_unit2 = vos[m].getCunitid();
					// if (pk_batchcode.equals(pk_batchcode2) &&
					// pk_unit.equals(pk_unit2)) {
					// passFlag = true;
					// break;
					// }
					// }
					// if (!passFlag) {
					// throw new Exception("���ϴ���[" + wlbm + "]��λ����");
					// }
				}
				MaterialOutBodyVO bodyVO = new MaterialOutBodyVO();
				bodyVO.setCorpoid(pk_org);
				bodyVO.setCorpvid((String) orgData.get("pk_vid"));
				bodyVO.setPk_group(pk_group);
				bodyVO.setCbodywarehouseid(pk_stordoc);
				bodyVO.setCmaterialoid(pk_material);
				bodyVO.setCmaterialvid(pk_material);
				bodyVO.setNshouldassistnum(ckzsl); // Ӧ������
				bodyVO.setNshouldnum(ckzsl); // Ӧ��������
				bodyVO.setNassistnum(ckzsl); // ʵ������
				bodyVO.setNnum(ckzsl); // ʵ��������
				bodyVO.setCunitid(pk_unit);
				bodyVO.setCastunitid(pk_unit);
				bodyVO.setVchangerate("1/1");
				bodyVO.setPk_batchcode(pk_batchcode);
				bodyVO.setVbatchcode(pch);
				if (dvalidate != null) {
					bodyVO.setDvalidate(new UFDate(dvalidate));
				}
				if (dinbounddate != null) {
					bodyVO.setDinbounddate(new UFDate(dinbounddate));
				}
				if (dproducedate != null) {
					bodyVO.setDproducedate(new UFDate(dproducedate));
				}
				bodyVO.setVsndef10(hisbodyid);
				bodyVO.setVsndef11(bbddh);
				bodyVO.setVsndef12(bbddmx);
				
//				 select * from bd_defdoclist where code='004';
//				 
//				 select * from bd_defdoc where pk_defdoclist='1001A210000000001M6I';
//				 
				// String sql12 = "select pk_defdoc";
				
				bodyVO.setVsndef13(pxyxflbm);
				bodyVO.setCrowno(String.valueOf((i + 1) * 10));
				bodyVO.setDbizdate(today);
				childVOs[i] = bodyVO;
			}
			// headVO.setNtotalnum(totalNum);

			List<MaterialOutBodyVO> pickChildVOs = new ArrayList<MaterialOutBodyVO>();
			Map<Integer, Integer> idxMap = new HashMap<Integer, Integer>();
			for (int i = 0; i < childVOs.length; i++) {
				if (childVOs[i].getPk_batchcode() == null) {
					pickChildVOs.add(childVOs[i]);
					idxMap.put(i, pickChildVOs.size() - 1);
				}
			}
			if (pickChildVOs.size() > 0) {
				MaterialOutVO pickBillVO = new MaterialOutVO();
				MaterialOutHeadVO pickHeadVO = headVO;
				pickBillVO.setParentVO(pickHeadVO);
				pickBillVO.setChildrenVO(pickChildVOs.toArray(new MaterialOutBodyVO[pickChildVOs.size()]));
				// �Զ����
				ICBillPickResults pickResults = onhandResService.pickAuto(pickBillVO);
				if (pickResults == null) {
					throw new Exception("�Զ����ʧ�ܣ���鿴�����ִ����Ƿ����");
				}
				aggVO.setParent(headVO);
				ICBillBodyVO[] pickedBodyVOs = pickResults.getPickBodys();
				for (int i = 0; i < childVOs.length; i++) {
					if (idxMap.containsKey(i)) {
						Integer pickIdx = idxMap.get(i);
						ICBillBodyVO pickedBodyVO = pickedBodyVOs[pickIdx];
						String pk_batchcode = pickedBodyVO.getPk_batchcode();
						String sql12 = "select dvalidate,dinbounddate,dproducedate from scm_batchcode where pk_batchcode='" + pk_batchcode + "'";
						Map batchData = (Map) dao.executeQuery(sql12, new MapProcessor());
						if (batchData == null || batchData.size() == 0) {
							throw new Exception("���β�����[" + pk_batchcode + "]");
						}
						String dvalidate = (String) batchData.get("dvalidate");
						String dinbounddate = (String) batchData.get("dinbounddate");
						String dproducedate = (String) batchData.get("dproducedate");
						childVOs[i].setPk_batchcode(pk_batchcode);
						childVOs[i].setVbatchcode(pickedBodyVO.getVbatchcode());
						childVOs[i].setDvalidate(new UFDate(dvalidate));
						childVOs[i].setDinbounddate(new UFDate(dinbounddate));
						childVOs[i].setDproducedate(new UFDate(dproducedate));
						// �������Ρ����ϡ��ֿ⡢��֯��ѯ�������ϵĿ�浥λ�����е�λУ��(�����ϵͳ�Լ�ȥУ��)
						// String pk_material = childVOs[i].getCmaterialoid();
						// String pk_unit = childVOs[i].getCunitid();
						// String wlbm =
						// (String)((Map)children.get(i)).get("WLBM");
						// String dims[] = { "pk_org", "cwarehouseid",
						// "cmaterialvid" };
						// OnhandQryCond cond = new OnhandQryCond();
						// cond.addSelectFields(dims);
						// cond.setISSum(true);
						// cond.addFilterDimConditon(dims, new Object[] {
						// pk_org, pk_stordoc, pk_material });
						// OnhandVO[] vos = onhandQuery.queryOnhand(cond);
						// if (vos == null || vos.length == 0) {
						// throw new Exception("���ϴ���[" + wlbm + "]�ִ�����ѯΪ��");
						// }
						// boolean passFlag = false;
						// for (int m = 0; m < vos.length; m++) {
						// String pk_batchcode2 = vos[m].getPk_batchcode();
						// String pk_unit2 = vos[m].getCunitid();
						// if (pk_batchcode.equals(pk_batchcode2) &&
						// pk_unit.equals(pk_unit2)) {
						// passFlag = true;
						// break;
						// }
						// }
						// if (!passFlag) {
						// throw new Exception("���ϴ���[" + wlbm + "]��λ����");
						// }
					}
				}
				aggVO.setChildrenVO(childVOs);
			} else {
				aggVO.setParentVO(headVO);
				aggVO.setChildrenVO(childVOs);
			}

			InvocationInfoProxy proxy = InvocationInfoProxy.getInstance();
			proxy.setUserId(pk_user);
			proxy.setUserCode(userCode);
			proxy.setBizDateTime(today.getMillis());
			proxy.setGroupId(pk_group);
			String actionName = "WRITE";
			WorkflownoteVO workflownotevo = null;
			Object userObj = null;
			HashMap hmPfExParams = null;
			IplatFormEntry platFormEntryService = NCLocator.getInstance().lookup(IplatFormEntry.class);
			Object actionResult = platFormEntryService.processAction(actionName, billType, workflownotevo, aggVO, userObj, hmPfExParams);
			MaterialOutVO[] resultBillVOs = (MaterialOutVO[]) actionResult;
			MaterialOutVO resultAggVO = resultBillVOs[0];
			Map ret = new HashMap();
			// TODO business process
			ret.put("RST", "T");
			String sql21 = "select vbillcode from ic_material_h where cgeneralhid='" + resultAggVO.getHead().getCgeneralhid() + "'";
			String ckdh = (String) dao.executeQuery(sql21, new ColumnProcessor());
			ret.put("CKDH", ckdh);

			String retJson = mapper.writeValueAsString(ret);
			String pk_log = OidGenerator.getInstance().nextOid();
			String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
			String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			SQLParameter parameter = new SQLParameter();
			parameter.addParam(pk_log);
			parameter.addParam(time2);
			parameter.addParam(time1);
			parameter.addParam(time2);
			parameter.addParam("2"); // 1:����ѯ 2:�������ϳ��ⵥ����ǩ�֣� 3:ȡ��ǩ�ֲ��ϳ��ⵥ����ɾ����
			parameter.addParam(json);
			parameter.addParam(retJson);
			parameter.addParam(1);
			dao.executeUpdate(logSql, parameter);
			return retJson;
		} catch (Exception ex) {
			Logger.error("StockOutService Error:", ex);
			String exceptionMsg = ex.getMessage();
			String errMsg = exceptionMsg == null ? "NPE" : exceptionMsg.replaceAll("\"", "'");
			String retJson = new StringBuilder().append("{\"RST\":\"F\",\"MSG\":\"" + errMsg + "\"}").toString();
			String pk_log = OidGenerator.getInstance().nextOid();
			String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
			String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			SQLParameter parameter = new SQLParameter();
			parameter.addParam(pk_log);
			parameter.addParam(time2);
			parameter.addParam(time1);
			parameter.addParam(time2);
			parameter.addParam("2");
			parameter.addParam(json);
			parameter.addParam(retJson);
			parameter.addParam(0);
			try {
				dao.executeUpdate(logSql, parameter);
			} catch (Exception ex2) {
				Logger.error("StockOutService Error2:", ex2);
			}
			return retJson;
		}
	}
}
