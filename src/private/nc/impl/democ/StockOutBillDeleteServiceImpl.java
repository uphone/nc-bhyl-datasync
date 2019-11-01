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
			String pwd = new String(
					Base64.decodeBase64(userPassword.getBytes()));
			ILoginQueryService loginQueryService = NCLocator.getInstance()
					.lookup(ILoginQueryService.class);
			UserVO userVO = loginQueryService
					.getUserVOByUserPass(userCode, pwd);
			if (userVO == null) {
				throw new Exception("�û������������");
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
			parameter.addParam("3"); // 1:����ѯ 2:�������ϳ��ⵥ����ǩ�֣�
										// 3:ȡ��ǩ�ֲ��ϳ��ⵥ����ɾ����
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
