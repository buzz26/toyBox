/*
 * Copyright 2005-2010 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import sdloader.internal.CommandLineHelper;
import sdloader.lifecycle.LifecycleEvent;
import sdloader.lifecycle.LifecycleListener;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.Browser;

/**
 * SDLoaderをオープンします.
 *
 * <pre>
 * システムトレーにアイコンを表示します。
 * </pre>
 *
 * @author c9katayama
 */
public class SystemTrayOpen {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(SystemTrayOpen.class);

	private Display display = new Display();
	private Shell shell = new Shell(display);

	private SDLoader sdloader;

	public static void main(String[] args) {
		new SystemTrayOpen().start(args);
	}

	public void start(String[] args) {

		CommandLineHelper helper = new CommandLineHelper(args);
		helper.setOpenBrowser(true);
		if (helper.hasHelpOption()) {
			helper.printUsage(log);
			showInfoDialog(helper.getUsageText());
			dispose();
			System.exit(0);
		}
		sdloader = new SDLoader();
		sdloader.setAutoPortDetect(true);
		sdloader.addEventListener(LifecycleEvent.AFTER_STOP,
				new LifecycleListener() {
					public void handleLifecycle(LifecycleEvent<?> event) {
						display.asyncExec(new Runnable() {
							public void run() {
								display.dispose();
							}
						});
					}
				});

		try {
			helper.applySDLoaderProperties(sdloader);
		} catch (Exception e) {
			log.error(e.getMessage());
			helper.printHelpOption(log);
			showErroDialog(e.getMessage());
			showInfoDialog(helper.getUsageText());
			dispose();
			System.exit(0);
		}

		try {
			sdloader.start();

			if (helper.isOpenBrowser()) {
				openBrowser();
			}

			createSystemTray(sdloader);

		} catch (Throwable e) {
			log.error("SDLoader catch error.", e);
			showErroDialog("エラーが発生しました。詳細はログを確認してください");
			dispose();
		}
		System.exit(0);
	}

	protected void showInfoDialog(String message) {
		MessageBox f = new MessageBox(shell, SWT.ICON_INFORMATION);
		f.setMessage(message);
		f.open();
	}

	protected void showErroDialog(String message) {
		MessageBox f = new MessageBox(shell, SWT.ICON_ERROR);
		f.setMessage(message);
		f.open();
	}

	/**
	 * アイコンのInputStreamを返します。 はじめにtrayicon.gifの名前でFileを検索します。
	 * ない場合、クラスパスから/sdloader/resource/trayicon.gifを探します。
	 *
	 * @return
	 */
	protected InputStream getIconInputStream() {
		try {
			InputStream iconStream = null;
			File icon = new File("trayicon.gif");
			if (icon.exists()) {
				iconStream = new FileInputStream(icon);
			} else {
				iconStream = SystemTrayOpen.class
						.getResourceAsStream("/sdloader/resource/trayicon.gif");
			}
			return iconStream;
		} catch (IOException ioe) {
			return null;
		}
	}

	/**
	 * デフォルトＵＲＬをブラウザをオープンします。
	 */
	protected void openBrowser() {
		try {
			int port = sdloader.getPort();
			String protocol = sdloader.isSSLEnable() ? "https" : "http";
			String url = protocol + "://localhost:" + port;
			Browser.open(url);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * システムトレーを作成します。
	 *
	 * @param sdLoader
	 */
	protected void createSystemTray(final SDLoader sdLoader) {
		Image image = new Image(display, getIconInputStream());
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			log.error("System tray not found.");
		} else {

			final TrayItem item = new TrayItem(tray, SWT.NONE);
			item.setImage(image);
			item.setToolTipText("SDLoader port:" + sdLoader.getPort());

			final Menu menu = new Menu(shell, SWT.POP_UP);
			item.addListener(SWT.MenuDetect, new Listener() {
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			});

			final MenuItem shutdownItem = new MenuItem(menu, SWT.PUSH);
			shutdownItem.setText("shutdown");
			shutdownItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					sdLoader.stop();
				}
			});

			final MenuItem browserItem = new MenuItem(menu, SWT.PUSH);
			browserItem.setText("open browser");
			browserItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					openBrowser();
				}
			});
			menu.setDefaultItem(browserItem);

			final ToolTip tip = new ToolTip(shell, SWT.BALLOON
					| SWT.ICON_INFORMATION);
			tip.setMessage("アプリケーションを開始しました");
			item.setToolTip(tip);
			tip.setVisible(true);
			tip.setAutoHide(true);

			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent arg0) {
					tip.dispose();
					item.dispose();
					menu.dispose();
					shutdownItem.dispose();
					browserItem.dispose();
				}
			});
		}
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	protected void dispose() {
		shell.dispose();
		display.dispose();
	}
}
