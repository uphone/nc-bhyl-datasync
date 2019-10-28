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
				throw new Exception("��������Ϊ��");
			}
			// ����������롢�ֿ���롢���ϴ���
			String orgCode = (String)param.get("orgCode");
			if(orgCode == null || "".equals(orgCode.trim())) {
				throw new Exception("�������벻��Ϊ��[orgCode]");
			}
			String whCode = (String)param.get("whCode");
			if(whCode == null || "".equals(whCode.trim())) {
				throw new Exception("�ֿ���벻��Ϊ��[whCode]");
			}
			String mrCode = (String)param.get("mrCode");
			if(mrCode == null || "".equals(mrCode.trim())) {
				throw new Exception("���ϴ��벻��Ϊ��[mrCode]");
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
			 * RST ִ�н�� VARCHAR 1 �� T-�ɹ� F-ʧ�� MSG ������Ϣ VARCHAR 500 ��
			 * ������Ϣ����RST=Fʱ���ش�����Ϣ ��ʾ��治���ӿ�ʧ�� YPDM ҩƷ���� VARCHAR 20 �� YPMC ҩƷ����
			 * VARCHAR 100 �� KCSL ������� INT 500 �� DW ��浥λ VARCHAR 500 ��
			 */
			ret.put("RST", "T");
			ret.put("MSG", "");
			ret.put("YPDM", "0000112");
			ret.put("KCSL", 1231);
			ret.put("DW", "��");
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
