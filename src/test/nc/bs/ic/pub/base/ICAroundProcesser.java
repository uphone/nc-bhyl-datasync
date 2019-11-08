package nc.bs.ic.pub.base;

import nc.impl.pubapp.pattern.rule.IFilterRule;
import nc.impl.pubapp.pattern.rule.IRule;
import nc.impl.pubapp.pattern.rule.plugin.IPluginPoint;
import nc.impl.pubapp.pattern.rule.processer.AroundProcesser;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;

// Referenced classes of package nc.bs.ic.pub.base:
//            BizRuleCollection

public class ICAroundProcesser extends AroundProcesser {

	private BizRuleCollection getBizRuleCollection() {
		return selfrules;
	}

	private boolean isAddToProcesser() {
		return isAddToProcesser;
	}

	public void addBeforeRuleAt(IRule rule, Class atruleclass) {
		if (!isAddToProcesser())
			getBizRuleCollection().addBeforeRuleAt(rule, atruleclass);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void replaceBeforeRuleAt(IRule rule, Class atruleclass) {
		if (!isAddToProcesser())
			getBizRuleCollection().replaceBeforeRuleAt(rule, atruleclass);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void setBeforeRuleAtHead(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().setBeforeRuleAtHead(rule);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void setBeforeRuleAtTail(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().setBeforeRuleAtTail(rule);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void addAfterRuleAt(IRule rule, Class atruleclass) {
		if (!isAddToProcesser())
			getBizRuleCollection().addAfterRuleAt(rule, atruleclass);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void replaceAfterRuleAt(IRule rule, Class atruleclass) {
		if (!isAddToProcesser())
			getBizRuleCollection().replaceAfterRuleAt(rule, atruleclass);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void setAfterRuleAtHead(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().setAfterRuleAtHead(rule);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public void setAfterRuleAtTail(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().setAfterRuleAtTail(rule);
		else
			ExceptionUtils.wrappBusinessException("ICAroundProcesser : at erro !");
	}

	public ICAroundProcesser(IPluginPoint point) {
		super(point);
		selfrules = new BizRuleCollection();
		isAddToProcesser = false;
	}

	public void addAfterFinalRule(IFilterRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addAfterFinalRule(rule);
		else
			super.addAfterFinalRule(rule);
	}

	public void addAfterFinalRule(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addAfterFinalRule(rule);
		else
			super.addAfterFinalRule(rule);
	}

	public void addAfterRule(IFilterRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addAfterRule(rule);
		else
			super.addAfterRule(rule);
	}

	public void addAfterRule(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addAfterRule(rule);
		else
			super.addAfterRule(rule);
	}

	public void addBeforeFinalRule(IFilterRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addBeforeFinalRule(rule);
		else
			super.addBeforeFinalRule(rule);
	}

	public void addBeforeFinalRule(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addBeforeFinalRule(rule);
		else
			super.addBeforeFinalRule(rule);
	}

	public void addBeforeRule(IFilterRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addBeforeRule(rule);
		else
			super.addBeforeRule(rule);
	}

	public void addBeforeRule(IRule rule) {
		if (!isAddToProcesser())
			getBizRuleCollection().addBeforeRule(rule);
		else
			super.addBeforeRule(rule);
	}

	public Object[] after(Object vos[]) {
		isAddToProcesser = true;
		getBizRuleCollection().addAllAfterRuleToProcesser(this);
		return super.after(vos);
	}

	public Object[] before(Object vos[]) {
		isAddToProcesser = true;
		getBizRuleCollection().addAllBeforeRuleToProcesser(this);
		return super.before(vos);
	}

	private BizRuleCollection selfrules;
	private boolean isAddToProcesser;
}
