package sdloader.desktopswt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Point;

import sdloader.desktopswt.util.BooleanUtil;
import sdloader.util.ResourceUtil;

public class AppConfig {

	public static final String CONFIG_TITLE = "title";
	public static final String CONFIG_WINDOW_WIDTH = "window.width";
	public static final String CONFIG_WINDOW_HEIGHT = "window.height";
	public static final String CONFIG_RESIZABLE = "window.resizable";
	public static final String CONFIG_MAXIMIZE_BUTTON = "window.maximizebutton";
	public static final String CONFIG_MAXIMIZED = "window.maximized";

	public static final String CONFIG_STARTUP_HOOK = "desktopswt.startupHook";
	public static final String CONFIG_SHUTDOWN_HOOK = "desktopswt.shutdownHook";

	private Properties appProperties;

	public void init() {
		appProperties = new Properties();
		InputStream app = ResourceUtil.getResourceAsStream(
				"application.properties", DesktopSWTMain.class);
		if (app == null) {
			return;
		}
		try {
			appProperties.load(app);
		} catch (IOException ioe) {
			throw new RuntimeException("application.propertiesのロードに失敗しました。");
		}
	}

	public String getTitleName() {
		return appProperties.getProperty(CONFIG_TITLE, "SDLoaderDesktopSWT");
	}

	public String getConfig(String key) {
		return appProperties.getProperty(key);
	}

	public String getConfig(String key, String defaultValue) {
		String value = appProperties.getProperty(key);
		return value == null ? defaultValue : value;
	}

	public Set<Entry<Object, Object>> getEntryList() {
		return appProperties.entrySet();
	}

	public boolean isResizable() {
		String value = getConfig(CONFIG_RESIZABLE, "true");
		return BooleanUtil.toBoolean(value);
	}

	public boolean isMaximizeButtonEnable() {
		String value = getConfig(CONFIG_MAXIMIZE_BUTTON, "true");
		return BooleanUtil.toBoolean(value);
	}

	public boolean isMaximized() {
		String value = getConfig(CONFIG_MAXIMIZED, "false");
		return BooleanUtil.toBoolean(value);
	}

	public String getStartupHook() {
		return getConfig(CONFIG_STARTUP_HOOK);
	}

	public String getShutdownHook() {
		return getConfig(CONFIG_SHUTDOWN_HOOK);
	}

//2011/06/20 kimukou.buzz add start
	public static final String CONFIG_BROWSER_TYPE = "desktopswt.browsertype";
	public String getBrowserType() {
		return getConfig(CONFIG_BROWSER_TYPE);
	}
//2011/06/20 kimukou.buzz add end
	public Point getWindowSize() {
		String w = appProperties.getProperty(CONFIG_WINDOW_WIDTH);
		String h = appProperties.getProperty(CONFIG_WINDOW_HEIGHT);
		if (w != null && w != null) {
			return new Point(Integer.valueOf(w), Integer.valueOf(h));
		} else {
			return null;
		}
	}
}
