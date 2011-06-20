package sdloader.desktopswt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class BrowserView {

	private Composite parent = null;

	private Composite top = null;

	private Browser browser = null;

	private Button backButton = null;

	private Button homeButton = null;

	private Button reloadButton = null;

	private String url;
	
	public Composite createPartControl(
			Composite parent, 
			String initUril,
//2011/06/20 kimukou.buzz mod start
			//boolean showButtons
			boolean showButtons,
			String browserType
//2011/06/20 kimukou.buzz mod end
) {
		this.parent = parent;
		this.url = initUril;

		top = new Composite(parent, SWT.NONE);

		GridLayout gridLayout = new GridLayout();
		if (showButtons) {
			gridLayout.marginHeight = 0;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 0;
			gridLayout.numColumns = 3;
			gridLayout.marginWidth = 0;
			backButton = new Button(top, SWT.NONE);
			backButton.setText("back");
			backButton.setImage(new Image(Display.getCurrent(), getClass()
					.getResourceAsStream("/icon/nav_backward.gif")));
			reloadButton = new Button(top, SWT.NONE);
			reloadButton.setText("reload");
			reloadButton.setImage(new Image(Display.getCurrent(), getClass()
					.getResourceAsStream("/icon/nav_refresh.gif")));
			homeButton = new Button(top, SWT.NONE);
			homeButton.setText("home");
			homeButton.setImage(new Image(Display.getCurrent(), getClass()
					.getResourceAsStream("/icon/nav_home.gif")));
			createButton();
		} else {
			gridLayout.marginHeight = 0;
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 0;
			gridLayout.numColumns = 1;
			gridLayout.marginWidth = 0;
		}
//2011/06/20 kimukou.buzz mod start
		//createBrowser();
		createBrowser(browserType);
//2011/06/20 kimukou.buzz mod end

		top.setLayout(gridLayout);

		return top;
	}

	private void createButton() {
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				back();
			}
		});
		homeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				home();
			}
		});
		reloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				reload();
			}
		});
	}

//2011/06/20 kimukou.buzz mod start
	//private void createBrowser() {
	private void createBrowser(String browserType) {
//2011/06/20 kimukou.buzz mod end
		GridData gridData2 = new GridData();
		gridData2.horizontalSpan = 3;
		GridData gridData1 = new GridData();
		gridData1.horizontalSpan = 3;
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 3;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;

//2011/06/20 kimukou.buzz mod start
		//browser = new Browser(top, SWT.NONE);
		if("SWT.WEBKIT".equals(browserType)){
			browser = new Browser(top, SWT.WEBKIT);
		}
		else{
			browser = new Browser(top, SWT.NONE);
		}
//2011/06/20 kimukou.buzz mod end
		browser.setLayoutData(gridData2);
		browser.setLayoutData(gridData1);
		browser.setLayoutData(gridData);
		addBrowserEvent(browser);
//		browser.setText("<a href='" + url + "'/>start</a>");
//		home();
	}

	private void addBrowserEvent(Browser browser) {
		// ウィンドウオープンのリスナ追加
		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				Shell shell = new Shell(parent.getShell(),SWT.CLOSE|SWT.MIN|SWT.MAX|SWT.RESIZE);
				shell.setSize((int)(parent.getSize().x*0.8),(int)(parent.getSize().y*0.8));
				shell.setLayout(new FillLayout(SWT.HORIZONTAL));
				final Browser browser = new Browser(shell, SWT.NONE);
				event.browser = browser;
				addBrowserEvent(event.browser);
				shell.addShellListener(new ShellAdapter(){
					@Override
					public void shellClosed(ShellEvent arg0) {
						try{
							browser.dispose();
						}catch(Exception e){}
					}
				});
				shell.open();				
			}
		});
	}

	public void back() {
		browser.back();
	}

	public void home() {
		browser.setUrl(url);
	}

	public void reload() {
		browser.refresh();
	}

}
