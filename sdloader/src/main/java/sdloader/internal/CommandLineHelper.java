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
package sdloader.internal;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import sdloader.log.SDLoaderLog;
import sdloader.util.CollectionsUtil;

/**
 * コマンドライン用Helper
 *
 * @author c9katayama
 *
 */
public class CommandLineHelper {

	protected String OPTION_WEBAPPS = "webApps";
	protected String OPTION_OPENBROWSER = "openBrowser";
	protected String OPTION_WAIT_FOR_STOP = "waitForStop";

	protected String[] args;
	protected boolean parsed;

	protected Properties sdloaderProperty = new Properties();
	protected String warFilePaths;
	protected boolean openBrowser = false;
	protected boolean waitForStop = true;

	protected static interface ArgumentHandler {
		boolean accept(String key);

		void handle(String key, String value);
	}

	protected List<ArgumentHandler> argumentHandlerList = CollectionsUtil
			.newArrayList();
	{
		argumentHandlerList.add(new ArgumentHandler() {
			public boolean accept(String key) {
				return OPTION_WEBAPPS.equalsIgnoreCase(key);
			}

			public void handle(String key, String value) {
				warFilePaths = value;
			}
		});
		argumentHandlerList.add(new ArgumentHandler() {
			public boolean accept(String key) {
				return OPTION_OPENBROWSER.equalsIgnoreCase(key);
			}

			public void handle(String key, String value) {
				openBrowser = Boolean.parseBoolean(value);
			}
		});
		argumentHandlerList.add(new ArgumentHandler() {
			public boolean accept(String key) {
				return OPTION_WAIT_FOR_STOP.equalsIgnoreCase(key);
			}

			public void handle(String key, String value) {
				waitForStop = Boolean.parseBoolean(value);
			}
		});
		argumentHandlerList.add(new ArgumentHandler() {
			public boolean accept(String key) {
				return SDLoader.CONFIG_KEYS.contains(toSDLoaderKey(key));
			}

			public void handle(String key, String value) {
				sdloaderProperty.setProperty(toSDLoaderKey(key), value);
			}

			protected String toSDLoaderKey(String key) {
				return SDLoader.CONFIG_KEY_PREFIX + key;
			}
		});
	}

	public CommandLineHelper(String[] args) {
		this.args = args;
	}

	public boolean hasHelpOption() {
		if (args.length >= 1 && args[0].equals("--help")) {
			return true;
		} else {
			return false;
		}
	}

	protected void initWebApps(SDLoader loader) {
		if (warFilePaths == null) {
			return;
		}
		String[] paths = warFilePaths.split(";");
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			File warFile = new File(path);
			if (warFile.exists() == false) {
				throw new IllegalArgumentException(
						"WebApp file not found. path=" + path);
			}
			String contextPath = "/" + warFile.getName();
			WebAppContext app = new WebAppContext(contextPath, warFile);
			loader.addWebAppContext(app);
		}
	}

	protected void parse() throws IllegalArgumentException {
		if (args == null) {
			return;
		}
		if (parsed == true) {
			return;
		}
		loop: for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("--")) {
				String command = arg.substring(2);
				String[] keyvalue = command.split("=");
				if (keyvalue.length != 2) {
					throw new IllegalArgumentException("invalid argument ["
							+ arg + "]");
				}
				String key = keyvalue[0];
				String value = keyvalue[1].trim();
				for (ArgumentHandler handler : argumentHandlerList) {
					if (handler.accept(key) == true) {
						handler.handle(key, value);
						continue loop;
					}
				}

			}
			throw new IllegalArgumentException("invalid argument [" + arg + "]");
		}
		parsed = true;
	}

	public void applySDLoaderProperties(SDLoader sdloader) {
		parse();
		sdloader.getSDLoaderConfig().addAll(sdloaderProperty);
		initWebApps(sdloader);
	}

	public void setOpenBrowser(boolean openBrowser) {
		this.openBrowser = openBrowser;
	}

	public boolean isOpenBrowser() {
		return openBrowser;
	}

	public void setWaitForStop(boolean waitForStop) {
		this.waitForStop = waitForStop;
	}

	public boolean isWaitForStop() {
		return waitForStop;
	}

	public void printHelpOption(SDLoaderLog log) {
		log.info(getHelpOptionText());
	}

	public void printHelpOption() {
		System.out.println(getHelpOptionText());
	}

	public void printUsage(SDLoaderLog log) {
		log.info(getUsageText());
	}

	public void printUsage() {
		System.out.print(getUsageText());
	}

	public String getHelpOptionText() {
		return "オプションは --help で参照できます。";
	}

	public String getUsageText() {
		List<Option> optionList = CollectionsUtil.newArrayList();

		optionList.add(new Option("Usage:",
				"すべてのオプションは、--option=value の形で記述します。"));
		optionList.add(new Option("--port", "Listenするポート番号",
				"指定しない場合30000を使用します。", "例）--port=8080"));
		optionList.add(new Option("--" + OPTION_WEBAPPS,
				"デプロイするアプリケーションのwarファイル、もしくはディレクトリを指定します。",
				"「;」で区切ると、複数のアプリケーションをデプロイできます。",
				"コンテキストルートには、warファイル名がもしくはディレクトリ名が使用されます。",
				"例）--webApps=/path/to/app.war"));
		optionList
				.add(new Option(
						"--home",
						"SDLoaderホームディレクトリを指定します。",
						"このディレクトリ下の「webapps」もしくは--webAppsDirオプションで指定したディレクトリにアプリケーションを配置します。",
						"指定しない場合、Java実行ディレクトリを使用します。",
						"例）--home=/path/to/sdloader"));
		optionList.add(new Option("--webAppsDir", "アプリケーションの入ったディレクトリを指定します。",
				"相対パスの場合、ホームディレクトリからの相対パスになります。", "指定しない場合、「webapps」が使用されます。",
				"例）--webAppsDir=weblibs"));
		optionList.add(new Option("--autoPortDetect",
				"指定ポートが使用中の場合、空きポートを探すかどうか。", "指定しない場合、falseが使用されます。",
				"例）--autoPortDetect=true"));
		optionList
				.add(new Option("--useOutSidePort",
						"外部からのアクセス可能なポートを使用するかどうか。",
						"指定しない場合、localhostのみのリクエストを受け付けます。",
						"例）--useOutSidePort=true"));
		optionList.add(new Option("--sslEnable", "SSLを使用するかどうか。",
				"指定しない場合、SSLは使用しません。", "例）--sslEnable=true"));
		optionList.add(new Option("--lineSpeed", "擬似的な回線速度を設定します。",
				"指定しない場合、速度制限はありません。", "例）--lineSpeed=64000 (64Kbpsの場合)"));
		optionList.add(new Option("--workDir",
				"JSPおよびwarファイル展開用のworkディレクトリを指定します。",
				"指定しない場合、${java.io.tmp}/.sdloaderを使用します。",
				"例）--workDir=/path/to/dir"));

		optionList.add(new Option("--" + openBrowser,
				"SDLoader起動後にブラウザを開くかどうかを指定します。", "例）--" + openBrowser
						+ "=true"));
		optionList.add(new Option("--" + OPTION_WAIT_FOR_STOP,
				"SDLoaderがシャットダウンするまで、mainメソッドでwaitするかどうか。", "例）--"
						+ OPTION_WAIT_FOR_STOP + "=true"));

		int maxCommandSize = 0;
		for (Option option : optionList) {
			maxCommandSize = Math.max(option.command.length(), maxCommandSize);
		}
		StringWriter w = new StringWriter();
		PrintWriter bw = new PrintWriter(w);
		int commandSize = maxCommandSize + 2;
		for (Option option : optionList) {
			String[] detail = option.detail;
			for (int i = 0; i < detail.length; i++) {
				String command = (i == 0) ? option.command : "";
				for (int c = command.length(); c <= commandSize; c++) {
					command += " ";
				}
				bw.println(command + detail[i]);
			}
		}
		bw.close();
		return w.getBuffer().toString();
	}

	private static class Option {
		public Option(String command, String... detail) {
			this.command = command;
			this.detail = detail;
		}

		private String command;
		private String[] detail;
	}
}
