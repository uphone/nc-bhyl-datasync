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

// 岗位信息同步后台任务
public class PostSyncWorkPlugin2 implements IBackgroundWorkPlugin {

	@Override
	public PreAlertObject executeTask(BgWorkingContext context) throws BusinessException {
		LinkedHashMap param = context.getEngineContext().getExecutorContext().getKeyMap();
		boolean isAll = "1".equals(param.get("isAll"));
		String code = (String) param.get("code");
		StringBuilder sql = new StringBuilder();
		sql.append("select * from (");
		sql.append(" select A.ts,A.postcode as JobTypeID,");
		sql.append(" A.postname as JobTypeName,");
		sql.append(" decode(A.Enablestate,2,1,0) as Active,");
		sql.append(" B.code as ORGCODE");
		sql.append(" from om_post A");
		sql.append(" left join org_orgs B on A.Pk_Org=B.Pk_Org");
		sql.append(" ) T ");
		if (!isAll) {
			if (code != null && !"".equals(code.trim())) {
				sql.append(" where JobTypeID='").append(code).append("'");
			} else {
				Calendar now = Calendar.getInstance();
				now.add(Calendar.DATE, -1);
				String ts = new SimpleDateFormat("yyyy-MM-dd").format(now.getTime());
				ts += " 00:00:00";
				sql.append(" where ts >= '").append(ts).append("'");
			}
		}
		BaseDAO dao = new BaseDAO();
		List<Map> rows = (List<Map>) dao.executeQuery(sql.toString(), new MapListProcessor());
		if (rows == null || rows.size() == 0)
			return null;
		String[] keys = new String[] { "JobTypeID", "JobTypeName", "Active", "ORGCODE" };
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
				parameter.addParam("B3");
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
				parameter.addParam("B3");
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
		/*
		 * 
		 * String endpoint =
		 * "http://localhost:8087/shiyu_interface_war_exploded/cxfservice/interface?wsdl"
		 * ; String endpoint =
		 * "http://m59q73.natappfree.cc/shiyu_interface_war_exploded/cxfservice/interface?wsdl"
		 * ; //直接引用远程的wsdl文件 //以下都是调用方式 Service service = new Service(); Call
		 * call = (Call) service.createCall();
		 * call.setTargetEndpointAddress(endpoint); call.setUseSOAPAction(true);
		 * call.setOperationName(new
		 * QName("http://controller.sysInterface.shiyu.com/","InsertTkentity"));
		 * call.addParameter(new QName("http://tempuri.org/","tkentity"),
		 * XMLType.XSD_STRING, ParameterMode.IN);//接口的参数
		 * call.setReturnType(XMLType.XSD_STRING);//设置返回类型SOAP_STRING XSD_STRING
		 * String result = (String) call.invoke(new
		 * Object[]{sb.toString()});//给方法传递参数，并且调用方法
		 */
		String url = (String) param.get("url"); // "http://121.42.53.239:8092/shiyu_interface/cxfservice/interface?wsdl";
		String namespace = (String) param.get("namespace"); // "http://tempuri.org/";
		// String actionUrl = "http://tempuri.org/nethis_common_business"; //
		// http://tempuri.org/nethis_common_business
		String method = (String) param.get("method"); // "InsertTkentity";
		String userId = "SLGZxO8eCYInl7u0kwyWXQ==";
		String userPassword = "ShW0GnUJwmInXKXRfygRA0t697y2QbKCQyziXjyAu7I=";
		String businessCode = "B3";
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