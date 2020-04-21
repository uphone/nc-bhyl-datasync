/**
 * create table bhyl_datasync_log(
   pk_log char(20) not null primary key,
   ts char(19),
   time1 char(19),
   time2 char(19),
   itf_name varchar(50), 
   	-- 1:现存量查询， 
   	-- 2：新增材料出库单， 
   	-- 3：删除材料出库单， 
   	-- B1：同步部门数据到Beauty，
   	-- B2：同步人员数据到Beauty，
   	-- B3：同步岗位数据到Beauty,
   	-- BZ_BE20：同步部门数据到HIS，
   	-- BZ_BE21：同步人员数据到HIS，
   	-- BZ_BE22：同步物料数据到HIS
   req_data varchar(2000),
   req_data_enc varchar(2000),
   res_data varchar(2000),
   status int default 0,
   err_msg varchar(500)
);

特别注意事项： 如果是单机部署，请将{home/ufjdk/jre/lib/security/java.security}中的以下内容做更改：
（取消使用ibm的ssl校验，更改为使用默认的ssl校验）
# Default JSSE socket factories
ssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl
ssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl
# WebSphere socket factories (in cryptosf.jar)
#ssl.SocketFactory.provider=com.ibm.websphere.ssl.protocol.SSLSocketFactory
#ssl.ServerSocketFactory.provider=com.ibm.websphere.ssl.protocol.SSLServerSocketFactory
 */
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

// 部门信息同步后台任务
public class DeptSyncWorkPlugin implements IBackgroundWorkPlugin {
	private String secKey = "35fd0ec3";

	@Override
	public PreAlertObject executeTask(BgWorkingContext context) throws BusinessException {
		LinkedHashMap param = context.getEngineContext().getExecutorContext().getKeyMap();
		boolean isAll = "1".equals(param.get("isAll"));
		String code = (String) param.get("code");
		StringBuilder sql = new StringBuilder();
		sql.append("select * from (\n" +
                "select A.ts, A.Pk_Org as ID, A.code as JGDM,A.name as JGMC,A.shortname as JGJC,\n" +
                "B.code as SJJGDM,B.name as SJJGMC,\n" +
                "null as ZZJGDM,null as JGDZ,\n" +
                "null as LXR,null as LXDH,\n" +
                "null as EnterpriseID,\n" +
                "null as CompanyID,\n" +
                "null as CompanyDeptID,\n" +
                "decode(A.Enablestate,2,1,0) as Active,\n" +
                "'机构' as JGLB\n" +
                "from org_orgs A left join org_orgs B on A.Pk_Fatherorg=B.pk_org\n" +
                "where A.Isbusinessunit='Y'\n" +
                "union\n" +
                "select A.ts,A.pk_dept as ID, A.code as JGDM,A.name as JGMC,A.shortname as JGJC,\n" +
                "nvl(B.code,C.code) as SJJGDM,\n" +
                "nvl(B.name,C.name) as SJJGMC,\n" +
                "C.code as ZZJGDM,A.Address as JGDZ,\n" +
                "F.code as LXR,A.Tel as LXDH,\n" +
                "C.code as EnterpriseID,\n" +
                "E.code as CompanyID,\n" +
                "D.code as CompanyDeptID,\n" +
                "decode(A.Enablestate,2,1,0) as Active,\n" +
                "decode(A.glbdef8,'Y','科室','机构') as JGLB\n" +
                "from org_dept A left join org_dept B on A.Pk_Fatherorg=B.Pk_Dept\n" +
                "left join org_orgs C on A.Pk_Org=C.Pk_Org\n" +
                "left join org_dept D on A.glbdef9=D.pk_dept\n" +
                "left join org_orgs E on D.pk_org=E.pk_org\n" +
                "left join bd_psndoc F on A.Principal=F.Pk_Psndoc\n" +
                ") T ");
		if (!isAll) {
			if (code != null && !"".equals(code.trim())) {
				sql.append(" where JGDM='").append(code).append("'");
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
		String[] keys = new String[] {"ID", "JGDM", "JGMC", "JGJC", "SJJGDM", "SJJGMC", "ZZJGDM", "JGDZ", "LXR", "LXDH", "EnterpriseID", "CompanyID", "CompanyDeptID", "Active", "JGLB" };
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
			String businessInfoEnc = DemocWorkUtil.DESEncrypt(secKey, businessInfo);
			serviceParam.put("url", url);
			serviceParam.put("namespace", namespace);
			serviceParam.put("method", method);
			serviceParam.put("businessInfo", businessInfoEnc);
			String resData = null;
			try {
				resData = callService(serviceParam);
				/*
				 * <DocumentElement> <Result> <RST>T</RST> <MSG>操作成功！</MSG>
				 * </Result> </DocumentElement>
				 */
				String rst = resData.substring(resData.indexOf("<RST>") + 5, resData.indexOf("</RST>"));
				// HashMap retMap = new XmlMapper().readValue(resData,
				// HashMap.class);
				// String rst = (String) ((Map)
				// retMap.get("Result")).get("RST");
				if ("F".equalsIgnoreCase(rst)) {
					String errMsg = resData.substring(resData.indexOf("<MSG>") + 5, resData.indexOf("</MSG>"));
					throw new Exception(errMsg);
				}
				String pk_log = OidGenerator.getInstance().nextOid();
				String logSql = "insert into BHYL_DATASYNC_LOG(PK_LOG,TS,TIME1,TIME2,ITF_NAME,REQ_DATA,REQ_DATA_ENC,RES_DATA,STATUS) values(?,?,?,?,?,?,?,?,?)";
				String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				SQLParameter parameter = new SQLParameter();
				parameter.addParam(pk_log);
				parameter.addParam(time2);
				parameter.addParam(time1);
				parameter.addParam(time2);
				parameter.addParam("BZ_BE20");
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
				parameter.addParam("BZ_BE20");
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
		String businessCode = "BZ_BE20";
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
