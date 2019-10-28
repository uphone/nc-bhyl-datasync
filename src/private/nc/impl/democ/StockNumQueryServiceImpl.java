package nc.impl.democ;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import nc.bs.logging.Logger;
import nc.itf.democ.IStockNumQueryService;

public class StockNumQueryServiceImpl implements IStockNumQueryService {

	@Override
	public String stockNumQuery(String json) {
		// IOnhandQry
		try {
			ObjectMapper mapper = new ObjectMapper();
			HashMap param = mapper.readValue(json, HashMap.class);
			if(param == null || param.size() == 0) {
				throw new Exception("参数不能为空");
			}
			// 必填：机构代码、仓库代码、物料代码
			String orgCode = (String)param.get("orgCode");
			if(orgCode == null || "".equals(orgCode.trim())) {
				throw new Exception("机构代码不能为空[orgCode]");
			}
			String whCode = (String)param.get("whCode");
			if(whCode == null || "".equals(whCode.trim())) {
				throw new Exception("仓库代码不能为空[whCode]");
			}
			String mrCode = (String)param.get("mrCode");
			if(mrCode == null || "".equals(mrCode.trim())) {
				throw new Exception("物料代码不能为空[mrCode]");
			}
			// convert code to id
			
			
			Logger.error("stockNumQuery service...");
			Logger.error("the param size: "
					+ (param == null ? "0" : param.size()));
			if (param != null && param.size() > 0) {
				Set keySet = param.keySet();
				for (Object key : keySet) {
					Logger.error(key + "=>" + param.get(key) == null ? "NULL"
							: param.get(key));
				}
			}
			Map ret = new HashMap();
			/*
			 * RST 执行结果 VARCHAR 1 否 T-成功 F-失败 MSG 处理信息 VARCHAR 500 否
			 * 处理信息，当RST=F时返回错误消息 提示库存不足或接口失败 YPDM 药品代码 VARCHAR 20 否 YPMC 药品名称
			 * VARCHAR 100 否 KCSL 库存数量 INT 500 否 DW 库存单位 VARCHAR 500 是
			 */
			ret.put("RST", "T");
			ret.put("MSG", "");
			ret.put("YPDM", "0000112");
			ret.put("KCSL", 1231);
			ret.put("DW", "箱");
			return mapper.writeValueAsString(ret);
		} catch (Exception ex) {
			String exceptionMsg = ex.getMessage();
			String errMsg = exceptionMsg == null ? "NPE" : exceptionMsg
					.replaceAll("\"", "'");
			return new StringBuilder().append(
					"{\"RST\":\"F\",\"MSG\":\"" + errMsg + "\"}").toString();
		}
	}

}
