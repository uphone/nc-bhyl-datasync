package nc.itf.democ;

import java.util.Map;

// ��ѯ�ִ���
public interface IStockNumQueryService {
	/**
	 * parameters:
	 * cmaterialvid	���ϱ���	varchar	50	��	ҩƷ/���ϱ���
Cmaterialvname	��������	varchar	50	��	
cwarehousecid	�ֿ����	varchar	50	��	�ⷿ����
cwarehousen	�ֿ�����	varchar	50	��	�ⷿ����
nnum	��������λ����	Float	18,6	��	������ҩƷ��������
nastnum	���������λ����	Float	18,6	��	
castunitid	��λ	varchar	50	��	��浥λ
Pk_org	ҽԺ����	varchar	50	��	��ʼ��ʱ���ṩ��ҽԺ����
login_user_code	�û��˺ű���	varchar	50	��	

login_user_pwd	�û��˺�����	varchar	50	��	
login_sys_code	��½ϵͳ���ױ���	varchar	50	��	

		return:
		RST	ִ�н��	VARCHAR	1	��	T-�ɹ�  F-ʧ�� 
MSG	������Ϣ	VARCHAR	500	��	������Ϣ����RST=Fʱ���ش�����Ϣ
��ʾ��治���ӿ�ʧ��
YPDM	ҩƷ����	VARCHAR	20	��	
YPMC	ҩƷ����	VARCHAR	100	��	
KCSL	�������	INT	500	��	
DW	��浥λ	VARCHAR	500	��	

	 */
	public String stockNumQuery(String param);
}
