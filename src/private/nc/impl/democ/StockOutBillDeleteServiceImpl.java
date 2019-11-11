package nc.impl.democ;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.uap.oid.OidGenerator;
import nc.itf.democ.IStockOutBillDeleteService;
import nc.itf.ic.general.query.IICPageQuery;
import nc.itf.ic.onhand.OnhandResService;
import nc.itf.pub.web.ILoginQueryService;
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
import nc.vo.pubapp.pattern.model.entity.bill.IBill;
import nc.vo.sm.UserVO;

public class StockOutBillDeleteServiceImpl implements IStockOutBillDeleteService {

	@Override
	public String stockOutBillDelete(String json) {
		String time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
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
			String hisczygh = (String) businessInfo.get("HISCZYGH"); // HIS操作员工号
			if (hisczygh == null || "".equals(hisczygh)) {
				throw new Exception("HIS操作员工号不能为空[businessInfo.HISCZYGH]");
			}
			String sql4 = "select cuserid from sm_user where user_code='" + hisczygh + "'";
			String pk_user = (String) dao.executeQuery(sql4, new ColumnProcessor());
			if (pk_user == null || "".equals(pk_user)) {
				throw new Exception("用户代码不存在或未启用");
			}
			// String hisheadid = (String) businessInfo.get("HISHEADID"); //
			// HIS表头唯一标识号
			String yybm = (String) businessInfo.get("YYBM"); // 医院编码
			// String ckbm = (String) businessInfo.get("CKBM"); // 仓库编码
			String hisheadid = (String) businessInfo.get("HISHEADID"); // HIS表头唯一标识
			if (yybm == null || "".equals(yybm)) {
				throw new Exception("医院编码不能为空[businessInfo.YYBM]");
			}
			if (hisheadid == null || "".equals(hisheadid)) {
				throw new Exception("HIS表头唯一标识不能为空[businessInfo.HISHEADID]");
			}

			String sql1 = "select pk_org,pk_vid,pk_group from org_orgs where ISBUSINESSUNIT='Y' and ENABLESTATE=2 and code='" + yybm + "'";
			Map orgData = (Map) dao.executeQuery(sql1, new MapProcessor());
			if (orgData == null || orgData.size() == 0) {
				throw new Exception("机构代码不存在或未启用");
			}
			String pk_org = (String) orgData.get("pk_org");
			String pk_group = (String) orgData.get("pk_group");

			String sql2 = "select cgeneralhid,fbillflag from ic_material_h where pk_org='" + pk_org + "' and vdef17='" + hisheadid + "' and dr=0";
			Map billData = (Map) dao.executeQuery(sql2, new MapProcessor());
			if (billData == null || billData.size() == 0) {
				throw new Exception("HIS表头唯一标识不存在");
			}
			String billId = (String) billData.get("cgeneralhid");
			Integer billStatus = (Integer) billData.get("fbillflag");
			if (billStatus != 3) {
				throw new Exception("出库单未签字，不允许取消签字");
			}

			InvocationInfoProxy proxy = InvocationInfoProxy.getInstance();
			proxy.setUserId(pk_user);
			proxy.setUserCode(userCode);
			proxy.setBizDateTime(today.getMillis());
			proxy.setGroupId(pk_group);

			// 取消签字
			IICPageQuery queryService = NCLocator.getInstance().lookup(IICPageQuery.class);
			IBill[] billVOs = queryService.pageLazyQueryByIDs(new String[] { billId }, MaterialOutVO.class.getName());
			MaterialOutVO aggVO = (MaterialOutVO) billVOs[0];
			String actionName = "CANCELSIGN";
			WorkflownoteVO workflownotevo = null;
			Object userObj = null;
			HashMap hmPfExParams = null;
			IplatFormEntry platFormEntryService = NCLocator.getInstance().lookup(IplatFormEntry.class);
			Object actionResult = platFormEntryService.processAction(actionName, billType, workflownotevo, aggVO, userObj, hmPfExParams);
			
			// 删除
			IBill[] billVOs2 = queryService.pageLazyQueryByIDs(new String[] { billId }, MaterialOutVO.class.getName());
			MaterialOutVO aggVO2 = (MaterialOutVO) billVOs[0];
			String actionName2 = "DELETE";
			WorkflownoteVO workflownotevo2 = null;
			Object userObj2 = null;
			HashMap hmPfExParams2 = null;
			Object actionResult2 = platFormEntryService.processAction(actionName2, billType, workflownotevo2, aggVO2, userObj2, hmPfExParams2);
			
			Map ret = new HashMap();
			ret.put("RST", "T");

			String retJson = mapper.writeValueAsString(ret);
			String pk_log = OidGenerator.getInstance().nextOid();
			String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
			String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			SQLParameter parameter = new SQLParameter();
			parameter.addParam(pk_log);
			parameter.addParam(time2);
			parameter.addParam(time1);
			parameter.addParam(time2);
			parameter.addParam("3"); // 1:库存查询 2:新增材料出库单（并签字） 3:取消签字材料出库单（并删除）
			parameter.addParam(json);
			parameter.addParam(retJson);
			parameter.addParam(1);
			dao.executeUpdate(logSql, parameter);
			return retJson;
		} catch (Exception ex) {
			Logger.error("StockOutBillDeleteService Error:", ex);
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
			parameter.addParam("3");
			parameter.addParam(json);
			parameter.addParam(retJson);
			parameter.addParam(0);
			try {
				dao.executeUpdate(logSql, parameter);
			} catch (Exception ex2) {
				Logger.error("StockOutBillDeleteService Error2:", ex2);
			}
			return retJson;
		}
	}

}
