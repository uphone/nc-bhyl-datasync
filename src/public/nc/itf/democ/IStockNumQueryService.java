package nc.itf.democ;

import java.util.Map;

// 查询现存量
public interface IStockNumQueryService {
	/**
	 * parameters:
	 * cmaterialvid	物料编码	varchar	50	否	药品/物料编码
Cmaterialvname	物料名称	varchar	50	否	
cwarehousecid	仓库编码	varchar	50	否	库房代码
cwarehousen	仓库名称	varchar	50	否	库房名称
nnum	需求主单位数量	Float	18,6	否	处方中药品开立数量
nastnum	需求基本单位数量	Float	18,6	是	
castunitid	单位	varchar	50	否	库存单位
Pk_org	医院编码	varchar	50	否	初始化时会提供各医院编码
login_user_code	用户账号编码	varchar	50	是	

login_user_pwd	用户账号密码	varchar	50	是	
login_sys_code	登陆系统账套编码	varchar	50	是	

		return:
		RST	执行结果	VARCHAR	1	否	T-成功  F-失败 
MSG	处理信息	VARCHAR	500	否	处理信息，当RST=F时返回错误消息
提示库存不足或接口失败
YPDM	药品代码	VARCHAR	20	否	
YPMC	药品名称	VARCHAR	100	否	
KCSL	库存数量	INT	500	否	
DW	库存单位	VARCHAR	500	是	

	 */
	public String stockNumQuery(String param);
}
