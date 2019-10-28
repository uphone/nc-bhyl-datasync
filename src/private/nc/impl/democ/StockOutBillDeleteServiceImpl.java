package nc.impl.democ;

import nc.itf.democ.IStockOutBillDeleteService;

public class StockOutBillDeleteServiceImpl implements
		IStockOutBillDeleteService {

	@Override
	public String stockOutBillDelete(String param) {
		try {
			if (1 == 1)
				throw new Exception("‘›Œ¥ µœ÷");
			return "";
		} catch (Exception ex) {
			String exceptionMsg = ex.getMessage();
			String errMsg = exceptionMsg == null ? "NPE" : exceptionMsg
					.replaceAll("\"", "'");
			return new StringBuilder().append(
					"{\"RST\":\"F\",\"MSG\":\"" + errMsg + "\"}").toString();
		}
	}

}
