package nc.impl.ic.general.query;

import nc.itf.ic.general.query.IICPageQuery;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pub.BusinessException;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.pubapp.pattern.model.entity.bill.IBill;
import nc.vo.scmpub.page.PageQueryVO;

// Referenced classes of package nc.impl.ic.general.query:
//            PageQueryUtil

public class ICPageQueryImpl implements IICPageQuery {

	public ICPageQueryImpl() {
	}

	public PageQueryVO pageLazyQueryByScheme(IQueryScheme scheme, String billVoClassName, String icBillType) throws BusinessException {
		try {
			PageQueryUtil util = new PageQueryUtil();
			return util.pageLazyQueryByScheme(scheme, billVoClassName, icBillType);
		} catch (Exception e) {
			ExceptionUtils.marsh(e);
		}
		return null;
	}

	public IBill[] pageLazyQueryByIDs(String ids[], String billVoClassName) throws BusinessException {
		try {
			PageQueryUtil util = new PageQueryUtil();
			return util.pageLazyQueryByIDs(ids, billVoClassName);
		} catch (Exception e) {
			ExceptionUtils.marsh(e);
		}
		return null;
	}
}