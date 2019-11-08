package nc.impl.ic.general;

import nc.bs.ic.general.insert.IInsertBP;
import nc.impl.pubapp.pattern.rule.plugin.IPluginPoint;
import nc.impl.pubapp.pattern.rule.processer.AroundProcesser;
import nc.vo.ic.general.define.ICBillVO;

public class InsertActionTemplate {

	public InsertActionTemplate(IPluginPoint point, IInsertBP insertBP) {
		this.insertBP = insertBP;
		this.point = point;
	}

	public ICBillVO[] insert(ICBillVO insertvos[]) {
		ICBillVO bills[] = insertvos;
		AroundProcesser processor = new AroundProcesser(point);
		processor.before(bills);
		ICBillVO vos[] = insertBP.insert(bills);
		processor.after(vos);
		return vos;
	}

	private IInsertBP insertBP;
	private IPluginPoint point;
}