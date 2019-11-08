package nc.impl.pubapp.pattern.rule.processer;

import java.util.Iterator;
import nc.bs.framework.mx.thread.*;
import nc.impl.pubapp.pattern.rule.IFilterRule;
import nc.impl.pubapp.pattern.rule.IRule;
import nc.impl.pubapp.pattern.rule.plugin.*;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pubapp.pattern.log.Log;

public class AroundProcesser {

	public AroundProcesser(IPluginPoint point) {
		before = new RuleCollection(point, EventType.Before);
		after = new RuleCollection(point, EventType.After);
	}

	public void addAfterFinalRule(IFilterRule rule) {
		after.addFinal(rule);
	}

	public void addAfterFinalRule(IRule rule) {
		after.addFinal(rule);
	}

	public void addAfterRule(IFilterRule rule) {
		after.add(rule);
	}

	public void addAfterRule(IRule rule) {
		after.add(rule);
	}

	public void addBeforeFinalRule(IFilterRule rule) {
		before.addFinal(rule);
	}

	public void addBeforeFinalRule(IRule rule) {
		before.addFinal(rule);
	}

	public void addBeforeRule(IFilterRule rule) {
		before.add(rule);
	}

	public void addBeforeRule(IRule rule) {
		before.add(rule);
	}

	public Object[] after(Object vos[]) {
		Iterator iterator = after.iterator();
		Object items[] = vos;
		ThreadEntry cte = ThreadTracer.getInstance().getCurThreadEntry();
		do {
			if (!iterator.hasNext())
				break;
			Object obj = iterator.next();
			long start = System.currentTimeMillis();
			int bsqlnum = 0;
			if (cte != null)
				bsqlnum = cte.getSqlHistory().getCountOfSql();
			if (obj instanceof IRule) {
				IRule rule = (IRule) obj;
				rule.process(items);
			} else if (obj instanceof IFilterRule) {
				IFilterRule rule = (IFilterRule) obj;
				items = rule.process(items);
			}
			if (obj != null) {
				long end = System.currentTimeMillis();
				int esqlnum = 0;
				if (cte != null)
					esqlnum = cte.getSqlHistory().getCountOfSql();
				String clname = obj.getClass().getName();
				String msg = (new StringBuilder()).append("[").append(clname).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("pubapp_0", "CompareAroundProcesser-0000")).append(end - start).append("]ms").toString();
				if (cte != null)
					msg = (new StringBuilder()).append(msg).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("pubapp_0", "AroundProcesser-0000")).append(esqlnum - bsqlnum).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("pubapp_0", "CompareAroundProcesser-0002")).toString();
				Log.debug(msg);
			}
		} while (true);
		return items;
	}

	public Object[] before(Object vos[]) {
		Iterator iterator = before.iterator();
		Object items[] = vos;
		ThreadEntry cte = ThreadTracer.getInstance().getCurThreadEntry();
		do {
			if (!iterator.hasNext())
				break;
			Object obj = iterator.next();
			long start = System.currentTimeMillis();
			int bsqlnum = 0;
			if (cte != null)
				bsqlnum = cte.getSqlHistory().getCountOfSql();
			if (obj instanceof IRule) {
				IRule rule = (IRule) obj;
				rule.process(items);
			} else if (obj instanceof IFilterRule) {
				IFilterRule rule = (IFilterRule) obj;
				items = rule.process(items);
			}
			if (obj != null) {
				long end = System.currentTimeMillis();
				int esqlnum = 0;
				if (cte != null)
					esqlnum = cte.getSqlHistory().getCountOfSql();
				String clname = obj.getClass().getName();
				String msg = (new StringBuilder()).append("[").append(clname).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("pubapp_0", "CompareAroundProcesser-0003")).append(end - start).append("]ms").toString();
				if (cte != null)
					msg = (new StringBuilder()).append(msg).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("pubapp_0", "AroundProcesser-0000")).append(esqlnum - bsqlnum).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("pubapp_0", "CompareAroundProcesser-0002")).toString();
				Log.debug(msg);
			}
		} while (true);
		return items;
	}

	private RuleCollection after;
	private RuleCollection before;
}
