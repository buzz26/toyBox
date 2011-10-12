package sdloader.desktopswt

public class EnvUtil{
	public static setENV(){
		setJENKINS_HOME()
	}
	public static void setJENKINS_HOME(){
		String APPPATH=System.getProperty("java.application.path") == null ? './':System.getProperty("java.application.path")+"/"

		String key='JENKINS_HOME'
		String val="\"${APPPATH}.jenkins\"".toString()
		java.lang.ProcessEnvironment.theCaseInsensitiveEnvironment.put(key,val)

		key='HUDSON_HOME'
		val="\"${APPPATH}.jenkins\"".toString()
		java.lang.ProcessEnvironment.theCaseInsensitiveEnvironment.put(key,val)

		key='GRAILS_HOME'
		val="C:/opt/grails-1.3.7".toString()
		java.lang.ProcessEnvironment.theCaseInsensitiveEnvironment.put(key,val)

		key='Path'
		val="${System.getenv("Path")};${System.getenv("GRAILS_HOME")}/bin".toString()
		java.lang.ProcessEnvironment.theCaseInsensitiveEnvironment.put(key,val)

		//暫定対応
		System.setProperty("user.home",APPPATH)
		System.setProperty("file.encoding","UTF-8")
		System.setProperty("groovy.source.encoding","UTF-8")
		System.setProperty("sun.jnu.encoding","UTF-8")
	}
}


