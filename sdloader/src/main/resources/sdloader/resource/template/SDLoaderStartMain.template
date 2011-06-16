import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import sdloader.util.Browser;
/**
 * SDLoader server start. 
 *
 */
public class SDLoaderStartMain {

	public static void main(String[] args) {

		SDLoader loader = new SDLoader(8080);
		loader.setAutoPortDetect(true);

		String contextPath = "/app";
		String webContentPath = "WebContent";
		WebAppContext context = new WebAppContext(contextPath, webContentPath);

		loader.addWebAppContext(context);

		loader.start();
		
		String startUrl = "http://localhost:"+loader.getPort()+contextPath+"/index.html";
		Browser.open(startUrl);
	}

}
