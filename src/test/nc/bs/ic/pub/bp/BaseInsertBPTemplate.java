package nc.bs.ic.pub.bp;

import nc.bs.ic.pub.base.ICAroundProcesser;
import nc.bs.ic.pub.base.IInsertRuleProvider;
import nc.bs.ic.pub.env.ICBSContext;
import nc.impl.pubapp.pattern.data.bill.BillOperator;
import nc.impl.pubapp.pattern.rule.plugin.IPluginPoint;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.pubapp.pattern.model.entity.bill.AbstractBill;

public class BaseInsertBPTemplate {

	public BaseInsertBPTemplate(IPluginPoint point, IInsertRuleProvider rules) {
		this.point = point;
		ruleProvider = rules;
	}

	public IPluginPoint getPoint() {
		return point;
	}

	public IInsertRuleProvider getRuleProvider() {
		return ruleProvider;
	}

	public AbstractBill[] insert(AbstractBill vos[]) {
		try {
			ICBSContext context = new ICBSContext();
			context.setBillOperate(nc.vo.ic.pub.define.BillOperator.New);
			ICAroundProcesser processor = new ICAroundProcesser(getPoint());
			addBeforeRule(vos, processor);
			addAfterRule(vos, processor);
			AbstractBill beforeRstVos[] = (AbstractBill[]) processor.before(vos);
			AbstractBill resultBills[] = insertBill(beforeRstVos);
			processor.after(resultBills);
			return resultBills;
		} catch (Exception ex) {
			ExceptionUtils.wrappException(ex);
		}
		return null;
	}

	protected void addAfterRule(AbstractBill vos[], ICAroundProcesser processor) {
		ruleProvider.addAfterRule(vos, processor);
	}

	protected void addBeforeRule(AbstractBill vos[], ICAroundProcesser processor) {
		ruleProvider.addBeforeRule(vos, processor);
	}

	protected AbstractBill[] insertBill(AbstractBill vos[]) {
		BillOperator operator = new BillOperator();
		return (AbstractBill[]) operator.insert(vos);
	}

	private IPluginPoint point;
	private IInsertRuleProvider ruleProvider;
}
