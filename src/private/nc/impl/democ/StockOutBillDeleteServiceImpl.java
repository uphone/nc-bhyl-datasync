package nc.impl.democ;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.uap.oid.OidGenerator;
import nc.itf.democ.IStockOutBillDeleteService;
import nc.itf.pub.web.ILoginQueryService;
import nc.jdbc.framework.SQLParameter;
import nc.vo.sm.UserVO;

public class StockOutBillDeleteServiceImpl implements
		IStockOutBillDeleteService {

	@Override
	public String stockOutBillDelete(String json) {
		String time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(Calendar.getInstance().getTime());
		// IOnhandQry
		BaseDAO dao = null;
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
			String pwd = new String(
					Base64.decodeBase64(userPassword.getBytes()));
			ILoginQueryService loginQueryService = NCLocator.getInstance()
					.lookup(ILoginQueryService.class);
			UserVO userVO = loginQueryService
					.getUserVOByUserPass(userCode, pwd);
			if (userVO == null) {
				throw new Exception("用户名或密码错误");
			}

			Map ret = new HashMap();
			// TODO business process
			ret.put("RST", "T");
			ret.put("MSG", "");

			String retJson = mapper.writeValueAsString(ret);
			String pk_log = OidGenerator.getInstance().nextOid();
			String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
			String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(Calendar.getInstance().getTime());
			SQLParameter parameter = new SQLParameter();
			parameter.addParam(pk_log);
			parameter.addParam(time2);
			parameter.addParam(time1);
			parameter.addParam(time2);
			parameter.addParam("3"); // 1:库存查询 2:新增材料出库单（并签字）
										// 3:取消签字材料出库单（并删除）
			parameter.addParam(json);
			parameter.addParam(retJson);
			parameter.addParam(1);
			dao.executeUpdate(logSql, parameter);
			return retJson;
		} catch (Exception ex) {
			Logger.error("StockOutBillDeleteService Error:", ex);
			String exceptionMsg = ex.getMessage();
			String errMsg = exceptionMsg == null ? "NPE" : exceptionMsg
					.replaceAll("\"", "'");
			String retJson = new StringBuilder().append(
					"{\"RST\":\"F\",\"MSG\":\"" + errMsg + "\"}").toString();
			String pk_log = OidGenerator.getInstance().nextOid();
			String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
			String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(Calendar.getInstance().getTime());
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
