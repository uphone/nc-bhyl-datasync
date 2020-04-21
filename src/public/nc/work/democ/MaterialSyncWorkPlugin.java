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

// 物料同步后台任务
public class MaterialSyncWorkPlugin implements IBackgroundWorkPlugin {

	private String secKey = "35fd0ec3";

	@Override
	public PreAlertObject executeTask(BgWorkingContext context) throws BusinessException {
		LinkedHashMap param = context.getEngineContext().getExecutorContext().getKeyMap();
		boolean isAll = "1".equals(param.get("isAll"));
		String code = (String) param.get("code");
		StringBuilder sql = new StringBuilder();
		sql.append("select * From (\n" +
                " select A.pk_material as ID, 'BHJT' as JGDM,A.code as YPDM,A.Name as YPMC,A.Materialspec as YPGG,\n" +
                "       A.def3 as YPJX,B.Code as YPLB,\n" +
                "       null as TJFL,null as GNFL,\n" +
                "       C.Name as CJMC,null as YPLY,null as PZWH,1 as JYBZ,\n" +
                "       D.Name as YPDW,0 as YPDJ,A.def1 as YPJL,A.def2 as JLDW,1 as JLDWXS,\n" +
                "       D.name as MZDW,1 as MZDWXS,null as ZYDW,null as ZYDWXS,\n" +
                "       null as ZXDW,null ZXDJ,null as ZHXS,null as CYJL\n" +
                " from bd_material A join bd_marbasclass B on A.Pk_Marbasclass=B.Pk_Marbasclass\n" +
                "     left join bd_branddoc C on A.Pk_Brand=C.Pk_Brand\n" +
                "     join bd_measdoc D on A.Pk_Measdoc=D.Pk_Measdoc \n" +
                ") T ");
		if (!isAll) {
			if (code != null && !"".equals(code.trim())) {
				sql.append(" where YPDM='").append(code).append("'");
			} else {
				Calendar now = Calendar.getInstance();
				now.add(Calendar.DATE, -1);
				String ts = new SimpleDateFormat("yyyy-MM-dd").format(now.getTime());
				ts += " 00:00:00";
				sql.append(" where ts >= '").append(ts).append("'");
			}
		}
		sql.append(" order by ts");
		BaseDAO dao = new BaseDAO();
		List<Map> rows = (List<Map>) dao.executeQuery(sql.toString(), new MapListProcessor());
		if (rows == null || rows.size() == 0)
			return null;
		String[] keys = new String[] {"ID", "JGDM", "YPDM", "YPMC", "YPGG", "YPJX", "YPLB", "TJFL", "GNFL", "CJMC", "YPLY", "PZWH", "JYBZ", "YPDW", "YPDJ", "YPJL", "JLDW", "JLDWXS", "MZDW", "MZDWXS", "ZYDW", "ZYDWXS", "ZXDW", "ZXDJ", "ZHXS", "CYJL" };
		String method = (String) param.get("method");
		String url = (String) param.get("url");
		String namespace = (String) param.get("namespace");
		for (Map row : rows) {
			String time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			StringBuilder xml = new StringBuilder();
			xml.append("<NETHIS><CSXX>");
			for (String key : keys) {
				String mapKey = key.toLowerCase();
				String value = row.get(mapKey) == null ? "" : row.get(mapKey).toString();
				if ("YPJL".equalsIgnoreCase(mapKey) && "".equals(value))
					value = "0";
				else if ("YPDJ".equalsIgnoreCase(mapKey) && "".equals(value))
					value = "0";
				else if ("ZXDJ".equalsIgnoreCase(mapKey) && "".equals(value))
					value = "0";
				xml.append("<").append(key).append(">");
				xml.append(value);
				xml.append("</").append(key).append(">");
			}
			xml.append("</CSXX></NETHIS>");
			Map serviceParam = new HashMap();
			String businessInfo = xml.toString();
			String businessInfoEnc = DemocWorkUtil.DESEncrypt(secKey, businessInfo);
			serviceParam.put("url", url);
			serviceParam.put("namespace", namespace);
			serviceParam.put("method", method);
			serviceParam.put("businessInfo", businessInfoEnc);
			String resData = null;
			try {
				resData = callService(serviceParam);
				String rst = resData.substring(resData.indexOf("<RST>") + 5, resData.indexOf("</RST>"));
				// HashMap retMap = new XmlMapper().readValue(resData,
				// HashMap.class);
				// String rst = (String) ((Map)
				// retMap.get("Result")).get("RST");
				if ("F".equalsIgnoreCase(rst)) {
					String errMsg = resData.substring(resData.indexOf("<MSG>") + 5, resData.indexOf("</MSG>"));
					throw new Exception(errMsg);
					// throw new Exception(
					// (String) ((Map) retMap.get("Result")).get("MSG"));
				}
				String pk_log = OidGenerator.getInstance().nextOid();
				String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,REQ_DATA_ENC,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?,?)";
				String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				SQLParameter parameter = new SQLParameter();
				parameter.addParam(pk_log);
				parameter.addParam(time2);
				parameter.addParam(time1);
				parameter.addParam(time2);
				parameter.addParam("BZ_BE22");
				parameter.addParam(businessInfo);
				parameter.addParam(businessInfoEnc);
				parameter.addParam(resData);
				parameter.addParam(1);
				dao.executeUpdate(logSql, parameter);
			} catch (Throwable ex) {
				Logger.error(ex);
				String pk_log = OidGenerator.getInstance().nextOid();
				String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,REQ_DATA_ENC,RES_DATA,STATUS,ERR_MSG) values(?,?,?,?,?,?,?,?,?,?)";
				String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				SQLParameter parameter = new SQLParameter();
				parameter.addParam(pk_log);
				parameter.addParam(time2);
				parameter.addParam(time1);
				parameter.addParam(time2);
				parameter.addParam("BZ_BE22");
				parameter.addParam(businessInfo);
				parameter.addParam(businessInfoEnc);
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
		String url = (String) param.get("url"); // "https://chisapp.bestway.cn/HISWebService/NetHisWebService.asmx?wsdl";
		String namespace = (String) param.get("namespace"); // "http://tempuri.org/";
															// wsdl中definitions根节点的targetNamespace属性值
		String actionUrl = "http://tempuri.org/nethis_common_business"; // http://tempuri.org/nethis_common_business
		String method = (String) param.get("method"); // "nethis_common_business";
		String userId = DemocWorkUtil.DESEncrypt(secKey, "shbh"); // "shbh";
		String userPassword = DemocWorkUtil.DESEncrypt(secKey, "689222BDC8BD33045F75C5C8411F41B4049D4618"); // "689222BDC8BD33045F75C5C8411F41B4049D4618";
		String businessCode = "BZ_BE22";
		String businessInfo = (String) param.get("businessInfo");
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setTargetEndpointAddress(new java.net.URL(url));
		call.setSOAPActionURI(actionUrl);
		call.setUseSOAPAction(true);
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
