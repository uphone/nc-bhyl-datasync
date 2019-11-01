package nc.impl.democ;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.uap.oid.OidGenerator;
import nc.itf.democ.IStockOutService;
import nc.itf.ic.onhand.OnhandResService;
import nc.itf.pub.web.ILoginQueryService;
import nc.itf.uap.pf.IplatFormEntry;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.jdbc.framework.processor.MapProcessor;
import nc.pubitf.ic.onhand.IOnhandQry;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutHeadVO;
import nc.vo.ic.m4d.entity.MaterialOutVO;
import nc.vo.ic.onhand.define.ICBillPickResults;
import nc.vo.ic.onhand.entity.OnhandVO;
import nc.vo.ic.onhand.pub.OnhandQryCond;
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
				throw new Exception("参数不能为空");
			}
			// 必填：帐套代码、用户名、登录密码、机构代码、仓库代码、物料代码
			String accountCode = (String) param.get("accountCode");
			if (accountCode == null || "".equals(accountCode.trim())) {
				throw new Exception("帐套代码不能为空[accountCode]");
			}
			dao = new BaseDAO(accountCode);

			String userCode = (String) param.get("userCode");
			if (userCode == null || "".equals(userCode.trim())) {
				throw new Exception("用户名不能为空[userCode]");
			}
			String userPassword = (String) param.get("userPassword");
			if (userPassword == null || "".equals(userPassword.trim())) {
				throw new Exception("用户密码不能为空[userPassword]");
			}

			// check user & password
			String pwd = new String(Base64.decodeBase64(userPassword.getBytes()));
			ILoginQueryService loginQueryService = NCLocator.getInstance().lookup(ILoginQueryService.class);
			UserVO userVO = loginQueryService.getUserVOByUserPass(userCode, pwd);
			if (userVO == null) {
				throw new Exception("用户名或密码错误");
			}

			// check businessInfo
			if (!param.containsKey("businessInfo")) {
				throw new Exception("缺失参数[businessInfo]");
			}
			Object businessInfoObj = param.get("businessInfo");
			if (!(businessInfoObj instanceof Map)) {
				throw new Exception("参数类型错误[businessInfo]");
			}
			Map businessInfo = (Map) businessInfoObj;
			if (!businessInfo.containsKey("head")) {
				throw new Exception("缺失参数[businessInfo.head]");
			}
			Object headObj = businessInfo.get("head");
			if (!(headObj instanceof Map)) {
				throw new Exception("参数类型错误[businessInfo.head]");
			}
			Map head = (Map) headObj;
			if (!businessInfo.containsKey("children")) {
				throw new Exception("缺失参数[businessInfo.children]");
			}
			Object childrenObj = businessInfo.get("children");
			if (!(childrenObj instanceof List)) {
				throw new Exception("参数类型错误[businessInfo.children]");
			}
			List children = (List) childrenObj;
			if (children == null || children.size() == 0) {
				throw new Exception("参数不能为空[businessInfo.children]");
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
			Map orgMap = (Map)dao.executeQuery(sql1, new MapProcessor());
//			String pk_org = (String) dao.executeQuery(sql1, new ColumnProcessor());
			if (orgMap == null || orgMap.size() == 0) {
				throw new Exception("机构代码不存在或未启用");
			}
			String pk_org = (String)orgMap.get("pk_org");
			
			String sql2 = "select pk_stordoc from bd_stordoc where pk_org='" + pk_org + "' and ENABLESTATE=2 and code='" + ckbm + "'";
			String pk_stordoc = (String) dao.executeQuery(sql2, new ColumnProcessor());
			if (pk_stordoc == null || "".equals(pk_stordoc)) {
				throw new Exception("仓库代码不存在或未启用");
			}
			String sql3 = "select pk_dept from org_dept where pk_org='" + pk_org + "' and ENABLESTATE=2 and code='" + kdksbm + "'";
			String pk_dept = (String) dao.executeQuery(sql3, new ColumnProcessor());
			if (pk_dept == null || "".equals(pk_dept)) {
				throw new Exception("部门代码不存在或未启用");
			}
			String sql4 = "select cuserid from sm_user where user_code='" + hisczygh + "'";
			String pk_user = (String) dao.executeQuery(sql4, new ColumnProcessor());
			if (pk_user == null || "".equals(pk_user)) {
				throw new Exception("用户代码不存在或未启用");
			}
			headVO.setCorpoid(pk_org);
			headVO.setPk_group((String)orgMap.get("pk_group"));
			headVO.setPk_org_v((String)orgMap.get("pk_vid"));
			headVO.setCwarehouseid(pk_stordoc);
			headVO.setCdptid(pk_dept);
			headVO.setBillmaker(pk_user);
			headVO.setVdef10(khxm);
			headVO.setVdef11(khdjh);
			headVO.setVdef12(ssmc);
			headVO.setVdef13(ssrq);
			headVO.setVdef14(ysygh);
			headVO.setVdef15(ysxm);
			headVO.setVdef16(hisczygh);
			headVO.setVdef17(hisheadid);
			headVO.setVdef18(pxyxmc);
			headVO.setVtrantypecode(billType);
			headVO.setDbilldate(today);

			for (int i = 0; i < children.size(); i++) {
				Map child = (Map) children.get(i);
				String wlbm = (String) child.get("WLBM");
				Object ckzslObj = child.get("CKZSL");
				Object ckfslObj = child.get("CKFSL");
				UFDouble ckzsl = (ckzslObj == null || "".equals(ckzslObj.toString())) ? UFDouble.ZERO_DBL : new UFDouble(ckzslObj.toString());
				UFDouble ckfsl = (ckfslObj == null || "".equals(ckfslObj.toString())) ? UFDouble.ZERO_DBL : new UFDouble(ckfslObj.toString());
				String dw = (String) child.get("DW");
				String pk_unit = null;
				String pch = (String) child.get("PCH");
				String pk_batchcode = null;
				String hisbodyid = (String) child.get("HISBODYID");
				String bbddh = (String) child.get("BBDDH");
				String bbddmx = (String) child.get("BBDDMX");
				String pxyxflbm = (String) child.get("PXYXFLBM");

				// code to id
				String sql10 = "select PK_MATERIAL from bd_material where enablestate=2 and code='" + wlbm + "'";
				String pk_material = (String) dao.executeQuery(sql10, new ColumnProcessor());
				if (pk_material == null || "".equals(pk_material)) {
					throw new Exception("物料代码不存在或未启用");
				}
				if (pch != null && !"".equals(pch)) {
					String sql11 = "select pk_batchcode from scm_batchcode where cmaterialoid='" + pk_material + "' and vbatchcode='" + pch + "'";
					pk_batchcode = (String) dao.executeQuery(sql11, new ColumnProcessor());
					if (pk_batchcode == null || "".equals(pk_batchcode)) {
						throw new Exception("批次号不存在");
					}
					// 根据批次、物料、仓库、组织查询出此物料的库存单位，进行单位校验？
					if (dw != null && !"".equals(dw)) {
						String sql12 = "select pk_measdoc from bd_measdoc where code='" + dw + "'";
						pk_unit = (String) dao.executeQuery(sql12, new ColumnProcessor());
						if (pk_unit == null || "".equals(pk_unit)) {
							throw new Exception("单位代码不存在");
						}
						String dims[] = { "pk_org", "cwarehouseid", "cmaterialvid" };
						OnhandQryCond cond = new OnhandQryCond();
						cond.addSelectFields(dims);
						cond.setISSum(true);
						cond.addFilterDimConditon(dims, new Object[] { pk_org, pk_stordoc, pk_material });
						OnhandVO[] vos = onhandQuery.queryOnhand(cond);
						if (vos == null || vos.length == 0) {
							throw new Exception("物料代码[" + wlbm + "]现存量查询为空");
						}
						boolean passFlag = false;
						for (int m = 0; m < vos.length; m++) {
							String pk_batchcode2 = vos[m].getPk_batchcode();
							String pk_unit2 = vos[m].getCunitid();
							if (pk_batchcode.equals(pk_batchcode2) && pk_unit.equals(pk_unit2)) {
								passFlag = true;
								break;
							}
						}
						if (!passFlag) {
							throw new Exception("物料代码[" + wlbm + "]单位不符");
						}
					}
				} else {
					// 等vo组装完成之后：自动捡货，捡货完成后，校验单位
					// onhandResService.pickAuto(icbillvo)
				}
				MaterialOutBodyVO bodyVO = new MaterialOutBodyVO();
				bodyVO.setCorpoid(pk_org);
				bodyVO.setCorpvid((String)orgMap.get("pk_vid"));
				bodyVO.setPk_group((String)orgMap.get("pk_group"));
				bodyVO.setCbodywarehouseid(pk_stordoc);
				bodyVO.setCmaterialoid(pk_material);
				bodyVO.setCmaterialvid(pk_material);
				bodyVO.setNnum(ckzsl);
				bodyVO.setNshouldnum(ckzsl);
				bodyVO.setNassistnum(ckfsl);
				bodyVO.setNshouldassistnum(ckfsl);
				bodyVO.setCunitid(pk_unit);
				bodyVO.setPk_batchcode(pk_batchcode);
				bodyVO.setVsndef10(hisbodyid);
				bodyVO.setVsndef11(bbddh);
				bodyVO.setVsndef12(bbddmx);
				bodyVO.setVsndef13(pxyxflbm);
				// "pk_group", "pk_org", "cwarehouseid", "cmaterialoid", "cmaterialvid"
				childVOs[i] = bodyVO;
			}

			List<MaterialOutBodyVO> pickChildVOs = new ArrayList<MaterialOutBodyVO>();
			for (int i = 0; i < childVOs.length; i++) {
				if (childVOs[i].getPk_batchcode() == null)
					pickChildVOs.add(childVOs[i]);
			}
			if (pickChildVOs.size() > 0) {
				MaterialOutVO pickBillVO = new MaterialOutVO();
				MaterialOutHeadVO pickHeadVO = headVO;
				pickBillVO.setParentVO(pickHeadVO);
				pickBillVO.setChildrenVO(pickChildVOs.toArray(new MaterialOutBodyVO[pickChildVOs.size()]));
				ICBillPickResults pickResults = onhandResService.pickAuto(pickBillVO);
				// TODO 自动捡货完成后是否需要手工设置批次号到表体
				System.out.print(222);
			}
			String actionName = "WHITE";
			WorkflownoteVO workflownotevo = null;
			Object userObj = null;
			HashMap hmPfExParams = null;
			IplatFormEntry platFormEntryService = NCLocator.getInstance().lookup(IplatFormEntry.class);
			Object whiteActionResult = platFormEntryService.processAction(actionName, billType, workflownotevo, aggVO, userObj, hmPfExParams);
			System.out.print(11);
			Map ret = new HashMap();
			// TODO business process
			ret.put("RST", "T");
			ret.put("MSG", "");

			String retJson = mapper.writeValueAsString(ret);
			String pk_log = OidGenerator.getInstance().nextOid();
			String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
			String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			SQLParameter parameter = new SQLParameter();
			parameter.addParam(pk_log);
			parameter.addParam(time2);
			parameter.addParam(time1);
			parameter.addParam(time2);
			parameter.addParam("2"); // 1:库存查询 2:新增材料出库单（并签字） 3:取消签字材料出库单（并删除）
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
