package nc.impl.democ;

import nc.itf.democ.IStockOutService;

public class StockOutServiceImpl implements IStockOutService {

	@Override
	public String stockOut(String param) {
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
