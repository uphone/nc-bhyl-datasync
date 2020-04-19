package nc.work.democ;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import nc.bs.dao.BaseDAO;
import nc.bs.logging.Logger;
import nc.bs.pub.pa.PreAlertObject;
import nc.bs.pub.taskcenter.BgWorkingContext;
import nc.bs.pub.taskcenter.IBackgroundWorkPlugin;
import nc.bs.uap.oid.OidGenerator;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.MapListProcessor;
import nc.vo.pub.BusinessException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

// 人员信息同步后台任务
public class PsnSyncWorkPlugin2 implements IBackgroundWorkPlugin {

	@Override
	public PreAlertObject executeTask(BgWorkingContext context) throws BusinessException {
		LinkedHashMap param = context.getEngineContext().getExecutorContext().getKeyMap();
		boolean isAll = "1".equals(param.get("isAll"));
		String code = (String) param.get("code");
		StringBuilder sql = new StringBuilder();
		sql.append("select * from (\n" +
	            "select A.ts, A.begindate,B.code as \"JGDM\",A.endflag,\n" +
	            "       C.pk_psndoc as \"ID\",\n" +
	            "       C.code as \"RYGH\",\n" +
	            "       C.name as \"XM\",\n" +
	            "       C.sex as \"XB\",\n" +
	            "       C.Birthdate as \"CSRQ\",\n" +
	            "       D.Code as \"RYLB\",\n" +
	            "       E.postcode as \"RYZC\",F.Code as \"KSDM\",F.name as \"KSMC\",null as \"YSJJ\",C.id as \"SFZH\", \n" +
	            "       A.jobglbdef2 as \"CostDeptID\",C.secret_email as \"Mail\",C.Mobile as \"Tel\",\n" +
	            "       H.endflag as \"EF2\",A.trnsevent,\n" +
	            "       case when (H.endflag='Y' and A.trnsevent=4) then 'N' else 'Y' end as \"StatuID\",\n" +
	            "       A.Ismainjob as \"ismainjob\"\n" +
	            "from hi_psnjob A left join org_orgs B on A.pk_hrorg=B.Pk_Org\n" +
	            "     inner join bd_psndoc C on A.pk_psndoc=C.pk_psndoc\n" +
	            "     left join bd_psncl D on A.pk_psncl = D.Pk_Psncl\n" +
	            "     left join om_post E on A.pk_post=E.pk_post\n" +
	            "     left join org_dept F on A.Pk_Dept=F.Pk_Dept\n" +
	            "     left join org_dept G on F.def1=G.pk_dept\n" +
	            "     left join Hi_psnorg H on A.pk_psndoc=H.pk_Psndoc\n" +
	            ") T ");
		if (!isAll) {
			if (code != null && !"".equals(code.trim())) {
				sql.append(" where RYGH='").append(code).append("'");
			} else {
				Calendar now = Calendar.getInstance();
				now.add(Calendar.DATE, -1);
				String ts = new SimpleDateFormat("yyyy-MM-dd").format(now.getTime());
				ts += " 00:00:00";
				sql.append(" where ts >= '").append(ts).append("'");
			}
		}
		sql.append(" order by begindate,ts");
		BaseDAO dao = new BaseDAO();
		List<Map> rows = (List<Map>) dao.executeQuery(sql.toString(), new MapListProcessor());
		if (rows == null || rows.size() == 0)
			return null;
		String[] keys = new String[] {"ID", "JGDM", "RYGH", "XM", "XB", "CSRQ", "RYLB", "RYZC", "KSDM", "KSMC", "YSJJ", "SFZH", "CostDeptID", "Mail", "Tel", "StatuID", "ismainjob", "endflag" };
		String method = (String) param.get("method");
		String url = (String) param.get("url");
		String namespace = (String) param.get("namespace");
		for (Map row : rows) {
			String time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			StringBuilder xml = new StringBuilder();
			xml.append("<NETHIS><CSXX>");
			for (String key : keys) {
				String mapKey = key.toLowerCase();
				xml.append("<").append(key).append(">");
				xml.append(row.get(mapKey) == null ? "" : row.get(mapKey));
				xml.append("</").append(key).append(">");
			}
			xml.append("</CSXX></NETHIS>");
			Map serviceParam = new HashMap();
			String businessInfo = xml.toString();
			serviceParam.put("url", url);
			serviceParam.put("namespace", namespace);
			serviceParam.put("method", method);
			serviceParam.put("businessInfo", businessInfo);
			String resData = null;
			try {
				resData = callService(serviceParam);
				String rst = resData.substring(resData.indexOf("<RST>") + 5, resData.indexOf("</RST>"));
				if ("F".equalsIgnoreCase(rst)) {
					String errMsg = resData.substring(resData.indexOf("<MSG>") + 5, resData.indexOf("</MSG>"));
					throw new Exception(errMsg);
				}
				String pk_log = OidGenerator.getInstance().nextOid();
				String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?)";
				String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				SQLParameter parameter = new SQLParameter();
				parameter.addParam(pk_log);
				parameter.addParam(time2);
				parameter.addParam(time1);
				parameter.addParam(time2);
				parameter.addParam("B2");
				parameter.addParam(businessInfo);
				parameter.addParam(resData);
				parameter.addParam(1);
				dao.executeUpdate(logSql, parameter);
			} catch (Throwable ex) {
				Logger.error(ex);
				String pk_log = OidGenerator.getInstance().nextOid();
				String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,RES_DATA,STATUS,ERR_MSG) values(?,?,?,?,?,?,?,?,?)";
				String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				SQLParameter parameter = new SQLParameter();
				parameter.addParam(pk_log);
				parameter.addParam(time2);
				parameter.addParam(time1);
				parameter.addParam(time2);
				parameter.addParam("B2");
				parameter.addParam(businessInfo);
				parameter.addParam(resData);
				parameter.addParam(0);
				String errMsg = ex.getMessage();
				if (errMsg == null) {
					errMsg = "NPE";
				} else {
					if (errMsg.length() > 500) {
						errMsg = errMsg.substring(0, 500);
					}
				}
				parameter.addParam(errMsg);
				dao.executeUpdate(logSql, parameter);
			}
		}
		return null;
	}

	private String callService(Map param) throws Exception {
		// 解决代理
		// System.setProperty("http.proxyHost", "10.10.66.12");
		// System.setProperty("http.proxyPort", "8080");
		/*
		 * 接口登录账号：shbh 接口登录秘钥：35fd0ec3
		 * 接口登录密码：689222BDC8BD33045F75C5C8411F41B4049D4618
		 */
		String url = (String) param.get("url"); // "http://dv5mnm.natappfree.cc/shiyu_interface_war_exploded/cxfservice/interface?wsdl";
		String namespace = (String) param.get("namespace"); // "http://tempuri.org/";
		// String actionUrl = "http://tempuri.org/nethis_common_business"; //
		// http://tempuri.org/nethis_common_business
		String method = (String) param.get("method"); // "InsertTkentity";
		String userId = "SLGZxO8eCYInl7u0kwyWXQ==";
		String userPassword = "ShW0GnUJwmInXKXRfygRA0t697y2QbKCQyziXjyAu7I=";
		String businessCode = "B2";
		String businessInfo = (String) param.get("businessInfo");
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setTargetEndpointAddress(new java.net.URL(url));
		// call.setSOAPActionURI(actionUrl);
		// call.setUseSOAPAction(true);
		call.setOperationName(new QName(namespace, method));
		// 该方法需要4个参数
		call.addParameter(new QName(namespace, "userId"), org.apache.axis.encoding.XMLType.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
		call.addParameter(new QName(namespace, "userPassword"), org.apache.axis.encoding.XMLType.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
		call.addParameter(new QName(namespace, "businessCode"), org.apache.axis.encoding.XMLType.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
		call.addParameter(new QName(namespace, "businessInfo"), org.apache.axis.encoding.XMLType.XSD_STRING, javax.xml.rpc.ParameterMode.IN);

		// 方法的返回值类型
		call.setReturnType(org.apache.axis.encoding.XMLType.XSD_STRING);
		String ret = (String) call.invoke(new Object[] { userId, userPassword, businessCode, businessInfo });
		return ret;
	}

}
