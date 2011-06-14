package sdloader

public class EnvUtil{
	public static setENV(){
		setJENKINS_HOME()
	}
	public static void setJENKINS_HOME(){
		String APPPATH=System.getProperty("java.application.path") == null ? './':System.getProperty("java.application.path")+"/"
		String key='JENKINS_HOME'
		String val="${APPPATH}.jenkins".toString()
		java.lang.ProcessEnvironment.theCaseInsensitiveEnvironment.put(key,val)
		System.setProperty("user.home",APPPATH)
	}
}


