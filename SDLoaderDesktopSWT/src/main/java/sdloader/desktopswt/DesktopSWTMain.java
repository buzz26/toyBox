package sdloader.desktopswt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.BindException;
import java.util.Properties;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderAdapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import sdloader.SDLoader;
import sdloader.lifecycle.LifecycleEvent;
import sdloader.lifecycle.LifecycleListener;

public class DesktopSWTMain {

	private Shell shell;
	private Shell splash;
	private Display display;
	private CTabFolder tabFolder;

	private SDLoader server;
	private AppConfig appConfig;

	public static void main(String[] args) {
		new DesktopSWTMain().open();
	}

	@SuppressWarnings("deprecation")
	public void open() {

		initAppConfig();

		startupHook();

		display = new Display();
		openSplashWindow();

		try {
			initWindow();
			initSDLoader();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (Exception e) {
			try {
				splash.close();
			} catch (Exception ex) {
			}
			MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR);
			msg.setMessage("エラー");
			msg.setMessage(e.getMessage());
			msg.open();
		} finally {
			try {
				display.dispose();
			} catch (Exception e) {
			}
			try {
				server.stop();
			} catch (Exception e) {
			}
			shutdownHook();
			System.exit(0);
		}
	}

	protected void initWindow() {
		int arg = SWT.TITLE | SWT.MIN | SWT.CLOSE;
		if (appConfig.isResizable()) {
			arg |= SWT.RESIZE;
		}
		if (appConfig.isMaximizeButtonEnable()) {
			arg |= SWT.MAX;
		}
		shell = new Shell(display, arg);
		if (appConfig.isMaximized()) {
			shell.setMaximized(true);
		}
		Point p = appConfig.getWindowSize();
		if (p != null) {
			shell.setSize(p);
		}
		shell.setText(appConfig.getTitleName());
		shell.setImage(getWindowIconImage());
		FillLayout layout = new FillLayout(SWT.VERTICAL);
		shell.setLayout(layout);

		tabFolder = new CTabFolder(shell, SWT.BORDER | SWT.CLOSE);
		tabFolder.setMaximizeVisible(true);
		tabFolder.setMinimizeVisible(true);
		tabFolder.setTabHeight(24);
		// 選択タブの背景色にグラデーションを設定
		tabFolder.setSelectionBackground(new Color[] {
				display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND),
				display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT) },
				new int[] { 90 }, true);
		tabFolder.setSelectionForeground(display
				.getSystemColor(SWT.COLOR_WHITE));

		tabFolder.addCTabFolderListener(new CTabFolderAdapter() {
			public void itemClosed(CTabFolderEvent event) {
				event.doit = false; // タブをとじないようにする
			}
		});
	}

	private void openSplashWindow() {
		splash = new Shell(SWT.ON_TOP);
		splash.setText("loading");
		splash.setLayout(new GridLayout(1, false));
		Image img = getSplashImage();
		Label label = new Label(splash, SWT.NONE);
		label.setImage(img);

		Label loadLabel = new Label(splash, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		loadLabel.setLayoutData(gd);

		splash.pack();

		// スプラッシュウィンドウを中央に配置
		Rectangle shellRect = splash.getBounds();
		Rectangle dispRect = display.getBounds();
		int x = (dispRect.width - shellRect.width) / 2;
		int y = (dispRect.height - shellRect.height) / 2;
		// 位置の指定はpack()のあとに呼ぶ必要がある
		splash.setLocation(x, y);
		splash.open();
	}

	protected void initAppConfig() {
		appConfig = new AppConfig();
		appConfig.init();
	}

	public void initSDLoader() {
		initServer();
		startServer();
	}

	protected void initServer() {
		server = new SDLoader(true);

		for (Entry<Object, Object> entry : appConfig.getEntryList()) {
			String key = (String) entry.getKey();
			if (key.startsWith(SDLoader.CONFIG_KEY_PREFIX)) {
				String value = (String) entry.getValue();
				server.setConfig(key, value);
			}
			// TODO
			if (key.equals("port")) {
				String value = (String) entry.getValue();
				server.setConfig(SDLoader.KEY_SDLOADER_PORT, value);
			}
		}

		String webAppsDir = System.getProperty("user.dir") + "/webapps";
		webAppsDir = webAppsDir.replace("\\", "/");
		server.setWebAppsDir(webAppsDir);

		server.addEventListener(LifecycleEvent.AFTER_START,
				new LifecycleListener() {
					public void handleLifecycle(LifecycleEvent<?> arg0) {
						showUI();
					}
				});
	}

	protected void showUI() {
		createView();
		splash.close();
		shell.open();
	}

	protected void startServer() {
		try {
			server.start();
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof BindException) {
				throw new RuntimeException("2重起動は出来ません。");
			} else {
				e.printStackTrace();
				throw new RuntimeException("エラーが発生しました。\n" + e.getMessage());
			}
		}
	}

	protected Image getSplashImage() {
		File splash = new File("splash.bmp");
		if (splash.exists()) {
			return new Image(display, "splash.bmp");
		} else {
			return new Image(display, getClass().getResourceAsStream(
					"/icon/splash.bmp"));
		}
	}

	protected Image getWindowIconImage() {
		File icon = new File("windowicon.gif");
		if (icon.exists()) {
			return new Image(display, "windowicon.gif");
		} else {
			return new Image(display, getClass().getResourceAsStream(
					"/icon/windowicon.gif"));
		}
	}

	protected void createView() {
		File viewconfigDir = null;
		String durl = System.getProperty("user.dir") + "/viewconfig";
		try {
			viewconfigDir = new File(durl);
		} catch (Exception e) {
			MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR);
			msg.setMessage("エラー");
			msg.setMessage("viewconfigが見つかりません。 " + durl);
			msg.open();
			return;
		}

		File[] configfiles = viewconfigDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".properties");
			}
		});
		if (configfiles == null || configfiles.length == 0) {
			MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR);
			msg.setMessage("エラー");
			msg.setText("viewconfigがありません。 " + durl);
			msg.open();
		}
		for (int i = 0; i < configfiles.length; i++) {
			Properties p = new Properties();
			try {
				p.load(new FileInputStream(configfiles[i]));
				String url = p.getProperty("url", "about:blank");
				if (!url.startsWith("http")) {
					url = "http://localhost:" + server.getPort() + url;
				}
				boolean showButtons = Boolean.valueOf(
						p.getProperty("buttons", "false")).booleanValue();
				String title = p.getProperty("title", "    ");

				// イメージ付きのタブを作成
				final CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
				tabItem.setText(title);
				Image image = new Image(display, DesktopSWTMain.class
						.getResourceAsStream("/icon/internal_browser.gif"));
				tabItem.setImage(image);

				final BrowserView view = new BrowserView();

//2011/06/20 kimukou.buzz add start
				//Composite comp = view.createPartControl(tabFolder, url,showButtons);
				Composite comp = view.createPartControl(tabFolder, url,showButtons,appConfig.getBrowserType());
//2011/06/20 kimukou.buzz add end

				tabItem.setControl(comp);

				tabFolder.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						if (event.item == tabItem) {
							view.home();
							tabFolder.removeSelectionListener(this);
						}
					}
				});

			} catch (Exception ioe) {
				ioe.printStackTrace();
			}
		}
	}

	protected void startupHook() {
		final String startupHook = appConfig.getStartupHook();
		if (startupHook != null) {
			System.out.println("Invoke startup thread start. hook="
					+ startupHook);
			new Thread() {
				@Override
				public void run() {
					try {
						System.out.println("startup hook invoke.");
						ProcessBuilder pb = new ProcessBuilder(startupHook);
						Process process = pb.start();
						printProcess(process);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	protected void shutdownHook() {
		final String shutdownHook = appConfig.getShutdownHook();
		if (shutdownHook != null) {
			System.out.println("Invoke shutdown thread start. hook="
					+ shutdownHook);
			new Thread() {
				@Override
				public void run() {
					try {
						System.out.println("Invoke shutdown hook start.");
						ProcessBuilder pb = new ProcessBuilder(shutdownHook);
						Process process = pb.start();
						printProcess(process);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	protected void printProcess(Process process) throws Exception {
		InputStream stream = process.getInputStream();
		while (true) {
			int c = stream.read();
			if (c == -1) {
				stream.close();
				break;
			}
			System.out.print((char) c);
		}
	}
}
