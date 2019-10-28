### 开发环境: 
  UAPStudio651
### NC版本： 
  6.5
### 开发环境在UAPServer运行参数添加：
  -Dorg.owasp.esapi.resources=${FIELD_NC_HOME}/ierp/bin/esapi
### SSL调用WebService:
  ```
  需要将${jdk}/jre/lib//security/java.security 文件中的对应参数作修改（大约在162行）：
  # Default JSSE socket factories -- 这是修改后的设置
  ssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl
  ssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl
  # WebSphere socket factories (in cryptosf.jar) -- 下面是修改前的设置
  #ssl.SocketFactory.provider=com.ibm.websphere.ssl.protocol.SSLSocketFactory
  #ssl.ServerSocketFactory.provider=com.ibm.websphere.ssl.protocol.SSLServerSocketFactory
  ```
### Java调用.Net的WebService: 
  在设置参数的时候需要加上namespace
### NC开发WebService：
  编写接口，编写实现类，在开发工具上，右键点击接口，生成wsdl文件,再编写upm文件
  浏览器中输入：http://localhost/uapws/service即可查看webservice是否部署成功
