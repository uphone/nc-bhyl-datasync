package nc.impl.ic.m4d.action;

import nc.bs.ic.m4d.insert.InsertBP;
import nc.impl.ic.general.InsertActionTemplate;
import nc.impl.ic.m4d.base.ActionPlugInPoint;
import nc.vo.ic.m4d.entity.MaterialOutVO;

public class InsertAction {

	public InsertAction() {
	}

	public MaterialOutVO[] insert(MaterialOutVO bills[]) {
		InsertActionTemplate insertAction = new InsertActionTemplate(ActionPlugInPoint.InsertAction, new InsertBP());
		return (MaterialOutVO[]) insertAction.insert(bills);
	}
}