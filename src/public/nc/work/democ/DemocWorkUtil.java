package nc.work.democ;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class DemocWorkUtil {
	public static String DESEncrypt(String key, String message) {
		String encrypt = "";
		try {
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
			IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			byte[] b = cipher.doFinal(message.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < b.length; i++) {
				String plainText = Integer.toHexString(0xff & b[i]);
				if (plainText.length() < 2)
					plainText = "0" + plainText;
				hexString.append(plainText);
			}
			encrypt = hexString.toString();
			encrypt = encrypt.toUpperCase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encrypt;
	}

}
