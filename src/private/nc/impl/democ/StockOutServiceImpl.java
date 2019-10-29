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
import nc.itf.democ.IStockOutService;
import nc.itf.pub.web.ILoginQueryService;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.pubitf.ic.onhand.IOnhandQry;
import nc.vo.ic.onhand.entity.OnhandVO;
import nc.vo.ic.onhand.pub.OnhandQryCond;
import nc.vo.sm.UserVO;

public class StockOutServiceImpl implements IStockOutService {

	@Override
	public String stockOut(String json) {
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

			String orgCode = (String) param.get("orgCode");
			if (orgCode == null || "".equals(orgCode.trim())) {
				throw new Exception("机构代码不能为空[orgCode]");
			}
			String whCode = (String) param.get("whCode");
			if (whCode == null || "".equals(whCode.trim())) {
				throw new Exception("仓库代码不能为空[whCode]");
			}
			String mrCode = (String) param.get("mrCode");
			if (mrCode == null || "".equals(mrCode.trim())) {
				throw new Exception("物料代码不能为空[mrCode]");
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

			String sql1 = "select pk_org from org_orgs where ISBUSINESSUNIT='Y' and ENABLESTATE=2 and code='"
					+ orgCode + "'";
			String pk_org = (String) dao.executeQuery(sql1,
					new ColumnProcessor());
			if (pk_org == null || "".equals(pk_org)) {
				throw new Exception("机构代码不存在或未启用");
			}
			String sql2 = "select pk_stordoc from bd_stordoc where pk_org='"
					+ pk_org + "' and ENABLESTATE=2 and code='" + whCode + "'";
			String pk_stordoc = (String) dao.executeQuery(sql2,
					new ColumnProcessor());
			if (pk_stordoc == null || "".equals(pk_stordoc)) {
				throw new Exception("仓库代码不存在或未启用");
			}
			String sql3 = "select PK_MATERIAL from bd_material where enablestate=2 and code='"
					+ mrCode + "'";
			String pk_material = (String) dao.executeQuery(sql3,
					new ColumnProcessor());
			if (pk_material == null || "".equals(pk_material)) {
				throw new Exception("物料代码不存在或未启用");
			}

			IOnhandQry onhandQuery = NCLocator.getInstance().lookup(
					IOnhandQry.class);
			String dims[] = { "pk_org", "cwarehouseid", "cmaterialvid" };
			OnhandQryCond cond = new OnhandQryCond();
			cond.addSelectFields(dims);
			cond.setISSum(true);
			cond.addFilterDimConditon(dims, new Object[] { pk_org, pk_stordoc,
					pk_material });
			OnhandVO[] vos = onhandQuery.queryOnhand(cond);
			if (vos == null || vos.length == 0) {
				throw new Exception("未查询到现存量信息");
			} else if (vos.length > 1) {
				throw new Exception("现存量查询异常[size>1]");
			} else {
				Map ret = new HashMap();
				OnhandVO vo = vos[0];
				String pk_measdoc = vo.getCunitid();
				String sql4 = "select name from bd_measdoc where pk_measdoc='"
						+ pk_measdoc + "'";
				String dw = (String) dao.executeQuery(sql4,
						new ColumnProcessor());
				ret.put("RST", "T");
				ret.put("MSG", "");
				ret.put("YPDM", mrCode);
				ret.put("KCSL", vo.getNonhandnum() == null ? 0 : vo
						.getNonhandnum().toDouble());
				ret.put("DW", dw);

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
				parameter.addParam("1"); // 1:库存查询 2:新增材料出库单（并签字）
											// 3:取消签字材料出库单（并删除）
				parameter.addParam(json);
				parameter.addParam(retJson);
				parameter.addParam(1);
				dao.executeUpdate(logSql, parameter);
				return retJson;
			}
			/*
			 * RST 执行结果 VARCHAR 1 否 T-成功 F-失败 MSG 处理信息 VARCHAR 500 否
			 * 处理信息，当RST=F时返回错误消息 提示库存不足或接口失败 YPDM 药品代码 VARCHAR 20 否 YPMC 药品名称
			 * VARCHAR 100 否 KCSL 库存数量 INT 500 否 DW 库存单位 VARCHAR 500 是
			 */
		} catch (Exception ex) {
			Logger.error("StockNumQueryService Error:", ex);
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
			parameter.addParam("1");
			parameter.addParam(json);
			parameter.addParam(retJson);
			parameter.addParam(0);
			try {
				dao.executeUpdate(logSql, parameter);
			} catch (Exception ex2) {
				Logger.error("StockNumQueryService Error2:", ex2);
			}
			return retJson;
		}
	}

}
