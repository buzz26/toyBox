//
//  service .exe
//
//	 2001/11/6  by takapyu
//
#include <windows.h>
#include <windowsx.h>
#include <tchar.h>
#include <commctrl.h>

#include "resource.h"

#ifndef ICON_SMALL
#define ICON_SMALL	0
#endif
#ifndef ICON_BIG
#define ICON_BIG	1
#endif

#define ERROR_PARAMETER			1
#define ERROR_NO_INSTALL		2
#define ERROR_INSTALL			3
#define ERROR_START				4

#define END_CLOSE				0
#define END_SYSCOMMAND			1
#define END_SYS_AND_CLOSE		2		// 2002/6/18
#define END_CTRL_BREAK			3		// 2004/8/9
#define END_CTRL_C				4		// 2011/9/1 kimukou.buzz

// 2010/6/10 Vista/7 で WM_DROPFILES を受ける
typedef BOOL (WINAPI *funcChangeWindowMessageFilter)(UINT, DWORD);
#define MSGFLT_ADD				1
#define MSGFLT_REMOVE			2

// ヘッダとか
const TCHAR *HEADER = _T("sexe");
const TCHAR *ERROR_HEADER = _T("sexe error");
const TCHAR *MUTEX_STRING = _T("$$sexe$$%s$$");
const TCHAR *WINDOW_TITLE = _T("sexe");

// .ini のキー
const TCHAR *KEY_EXE = _T("exe");
const TCHAR *KEY_OPTION = _T("option");
const TCHAR *KEY_NAME = _T("name");
const TCHAR *KEY_DESCRIPTION = _T("description");
const TCHAR *KEY_END = _T("end");
// 2004/8/9
const TCHAR *KEY_AUTO = _T("auto");
const TCHAR *KEY_DESKTOP = _T("desktop");
// 2009/3/26
const TCHAR *KEY_RETRY = _T("retry");
// 2009/8/26
const TCHAR *KEY_ENCODE = _T("encode");

// 面倒なのでグローバル変数を使いまくっております。
SERVICE_STATUS_HANDLE service_handle;
BOOL service_flag;					// running serivce
BOOL service_install_flag;			// install service
BOOL service_stop_flag;				// stop service
BOOL nt_flag;						// runnning Windows NT ?

TCHAR module_name[MAX_PATH];		// sexe.exe のフルパス
TCHAR execute_path[MAX_PATH];		// sexe.exe のフォルダ
TCHAR ini_name[MAX_PATH];			// sexe.ini のフルパス
TCHAR exe_name[MAX_PATH];			// サービスとして動作させるプログラムのフルパス
TCHAR option_name[MAX_PATH];		// 起動時オプション
TCHAR service_name[MAX_PATH];		// サービス名
TCHAR description_name[MAX_PATH];	// 2009/3/26 サービス説明
int end_pattern;					// 終了方法
int auto_flag;						// 2004/8/9 自動起動
int desktop_flag;					// 2004/8/9 デスクトップとの対話を許可
int retry_flag;						// 2009/3/26 再起動
int shutdown_flag;					// 2009/3/26 Shutdown
BOOL install_flag;					// 2011/4/28 インストール
BOOL uninstall_flag;				// 2011/4/28 アンインストール
BOOL read_ini_flag;					// 2011/4/28 インストール時に ini ファイルを参照
BOOL start_flag;					// 2011/4/28 インストール時そのままサービス開始
BOOL no_error_flag;					// 2011/5/31 エラーメッセージ非表示
BOOL question_flag;					// 2011/5/31 インストール/アンインストール問い合わせ
BOOL ok_flag;					// 2011/5/31 終了メッセージ表示
HANDLE mutex;						// 二重起動防止用 Mutex
DWORD process_id;					// 起動したプログラムのプロセス ID
HINSTANCE h_instance;				// hInstance

// 関数定義
BOOL APIENTRY MainFunc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);
BOOL send_status_to_scm(int state, int exit_code, int progress);
void service_control_handler(int control);
void service_main(void);
BOOL check_already(void);
BOOL remove_service(void);
int install_service(void);
BOOL restart_service(void);
BOOL check_execute_service(void);
void start_service(void);
void extract_directory(TCHAR *path);
TCHAR *extract_ext(TCHAR *path);
void get_inifile(void);
void set_inifile(void);

//
//	スペースおよびタブを飛ばす
//	in: pt .. ポインタ
//	out: スペースおよびタブを飛ばした後のポインタ
//
TCHAR *space_skip(TCHAR *pt)
{
	while(*pt != _T(' ') && *pt != _T('\t') && *pt != _T('\0')) {
		pt++;
	}
	return pt;
}

//
//	パラメータをバッファにコピー
//	スペースもしくは終了までコピーするが、
//	"" で囲われたスペースは無視
//	in: dst .. コピー先バッファ
//	    src .. コピー元バッファ
//	    size .. バッファサイズ
//	out: コピー終了後のポインタ
//
TCHAR *copy_param(TCHAR *dst, TCHAR *src, int size)
{
	BOOL flag = FALSE;

	if(*src == _T('=')) {
		src++;
	}
	if(*src == _T('"')) {
		flag = TRUE;
		src++;
	}
	while(size > 1 && *src != _T('\0')) {
		*dst = *src;
		if(flag) {
			if(*src == _T('\\') && *(src + 1) == _T('"')) {
				src++;
				*dst = *src;
			} else if(*src == _T('"')) {
				*dst = _T('\0');
				return space_skip(src + 1);
			}
		} else if(!flag && (*src == _T(' ') || *src == _T('\t'))) {
			*dst = _T('\0');
			return space_skip(src);
		}
		src++;
		dst++;
		size--;
	}
	*dst = _T('\0');
	return (TCHAR *)src;
}

//
//	リソースから文字列を読み込んでメッセージボックスを表示
//	in: wnd .. オーナーウインドウハンドル
//	    id  .. リソース ID
//	    param .. パラメータ文字列(NULL で無)
//	    caption .. タイトル文字列
//	    type .. メッセージボックススタイル
//	out: MessageBox() の返値
//
int MessageBoxResourceText(HWND wnd, unsigned int id, const TCHAR *param, const TCHAR *caption, int type)
{
	TCHAR text[MAX_PATH * 2];

	if(param != NULL) {
		TCHAR temp[MAX_PATH * 2];
		LoadString(h_instance, id, temp, MAX_PATH * 2);
		wsprintf(text, temp, param);
	} else {
		LoadString(h_instance, id, text, MAX_PATH * 2);
	}
	return MessageBox(wnd, text, caption, type);
}

//
//	コマンドラインパラメータ解析処理
//	in: pt .. コマンドラインパラメータ
//
void analyze_args(TCHAR *pt)
{
	TCHAR param[MAX_PATH];
	TCHAR name[MAX_PATH];
	TCHAR exe[MAX_PATH];
	TCHAR option[MAX_PATH];
	TCHAR description[MAX_PATH];
	int autof, desktop, retry, end;
	int no;
	struct {
		TCHAR *name;
		int *flag;
		TCHAR *dst;
	} list[] = {
		{ _T("install"), &install_flag, NULL },
		{ _T("uninstall"), &uninstall_flag, NULL },
		{ _T("ini"), &read_ini_flag, NULL },
		{ _T("name"), NULL, name },
		{ _T("exe"), NULL, exe },
		{ _T("option"), NULL, option },
		{ _T("description"), NULL, description },
		{ _T("desktop"), &desktop, NULL },
		{ _T("auto"), &autof, NULL },
		{ _T("start"), &start_flag, NULL },
		{ _T("no_error"), &no_error_flag, NULL },
		{ _T("ok"), &ok_flag, NULL },
		{ _T("question"), &question_flag, NULL },
		{ _T("retry"), &retry, NULL },
		{ _T("end"), &end, NULL },
		{ NULL, NULL, NULL }
	};

	name[0] = _T('\0');
	exe[0] = _T('\0');
	option[0] = _T('\0');
	description[0] = _T('\0');
	desktop = FALSE;
	autof = FALSE;
	retry = FALSE;
	end = END_SYS_AND_CLOSE;
	while(*pt != _T('\0')) {
		if(*pt== '/' || *pt == '-') {
			pt++;
			for(no = 0 ; list[no].name != NULL ; no++) {
				if(!_tcsnicmp(pt, list[no].name, _tcslen(list[no].name))) {
					pt += _tcslen(list[no].name);
					if(list[no].flag != NULL) {
						if(!_tcsnicmp(list[no].name, _T("end"), _tcslen(list[no].name))) {
							copy_param(param, pt, MAX_PATH);
							if(!_tcsicmp(param, _T("close"))) {
								*list[no].flag = END_CLOSE;
							} else if(!_tcsicmp(param, _T("syscommand"))) {
								*list[no].flag = END_SYSCOMMAND;
							} else if(!_tcsicmp(param, _T("sysandclose"))) {
								*list[no].flag = END_SYS_AND_CLOSE;
							} else if(!_tcsicmp(param, _T("ctrlbreak"))) {
								*list[no].flag = END_CTRL_BREAK;
// 2011/9/1 kimukou.buzz
							} else if(!_tcsicmp(param, _T("ctrlc"))) {
								*list[no].flag = END_CTRL_C;
// 2011/9/1 kimukou.buzz
							} else if(param[0] >= _T('0') && param[0] <= _T('3')) {
								*list[no].flag = param[0] - _T('0');
							}
						} else {
							*list[no].flag = TRUE;
						}
					}
					if(list[no].dst != NULL) {
						pt = copy_param(list[no].dst, pt, MAX_PATH);
					}
				}
			}
		} else {
			pt++;
		}
	}
	if(install_flag || uninstall_flag) {
		// インストールまたはアンインストールする場合
		if(read_ini_flag) {
			// ini ファイルから設定値を読む
			get_inifile();
		} else {
			// パラメータの値を使用する
#if _MSC_VER >= 1400
			_tcscpy_s(service_name, MAX_PATH, name);
			_tcscpy_s(exe_name, MAX_PATH, exe);
			_tcscpy_s(option_name, MAX_PATH, option);
			_tcscpy_s(description_name, MAX_PATH, description);
#else
			_tcscpy(service_name, name);
			_tcscpy(exe_name, exe);
			_tcscpy(option_name, option);
			_tcscpy(description_name, description);
#endif
			auto_flag = autof;
			desktop_flag = desktop;
			retry_flag = retry;
			end_pattern = end;
		}
	} else {
		// 通常起動の場合、ini ファイルから設定値を読む
		get_inifile();
	}
}

//
//	Windows メイン関数
//
int WINAPI _tWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPTSTR lpszArgs, int nWinMode)
{
    MSG msg;
	HWND hMain;
	int err;

	// 初期化
	install_flag = FALSE;
	uninstall_flag = FALSE;
	read_ini_flag = FALSE;
	start_flag = FALSE;
	no_error_flag = FALSE;
	question_flag = FALSE;
	ok_flag = FALSE;

	h_instance = hInstance;

	// 実行ディレクトリにある sexe.exe のフルパスを作成
	GetModuleFileName(NULL, module_name, sizeof(module_name));
	lstrcpy(execute_path, module_name);
	extract_directory(execute_path);
	lstrcpy(ini_name, execute_path);
	lstrcat(ini_name, _T("\\sexe.ini"));

	// コマンドライン解析
	analyze_args(lpszArgs);

	// サービスで動作しているか？
	service_flag = check_execute_service();
	// WindowsNT/2000 ?
	if(nt_flag) {
		// サービスとして動作中？
		if(service_flag) {
			// すでに動作中？
			if(!check_already()) {
				// サービスとして起動
				start_service();
			}
		} else {
			// 2011/5/31
			// コマンドラインパラメータでインストール・アンインストール
			if(install_flag) {
				TCHAR *ext;
				if(service_name[0] == _T('\0')) {
					if(!no_error_flag) {
						// サービス名を指定してください
						MessageBoxResourceText(NULL, IDS_ERROR_NO_SERVICE_NAME, NULL, ERROR_HEADER, MB_OK);
					}
					return ERROR_PARAMETER;
				}
				ext = extract_ext(exe_name);
				if(exe_name[0] == _T('\0') || (_tcsicmp(ext, _T("exe")) && _tcsicmp(ext, _T("bat")))) {
					if(!no_error_flag) {
						// プログラム名を指定してください
						MessageBoxResourceText(NULL, IDS_ERROR_NO_PROGRAM_NAME, NULL, ERROR_HEADER, MB_OK);
					}
					return ERROR_PARAMETER;
				}
				if(question_flag) {
					// サービス service_name を登録しますか？
					if(MessageBoxResourceText(NULL, IDS_QUESTION_INSTALL, service_name, ERROR_HEADER, MB_YESNO) != IDYES) {
						return ERROR_NO_INSTALL;
					}
				}
				if(!read_ini_flag) {
					// ini ファイルから読み出したのでなければ設定値を保存
					set_inifile();
				}
				// インストール
				if((err = install_service()) == ERROR_SUCCESS) {
					if(start_flag) {
						// サービス開始
						if(restart_service()) {
							if(ok_flag) {
								// サービス service_name を登録し、開始しました。
								MessageBoxResourceText(NULL, IDS_INSTALL_START_OK, service_name, HEADER, MB_OK);
							}
						} else if(!no_error_flag) {
							// サービス service_name を登録しましたが、開始に失敗しました。
							MessageBoxResourceText(NULL, IDS_ERROR_INSTALL_START, service_name, HEADER, MB_OK);
							return ERROR_START;
						}
					} else if(ok_flag) {
						// サービス service_name を登録しました。
						MessageBoxResourceText(NULL, IDS_INSTALL_OK, service_name, HEADER, MB_OK);
					}
				} else {
					if(!no_error_flag) {
						if(err == ERROR_SERVICE_EXISTS) {
							// すでに同名のサービスが登録済みです
							MessageBoxResourceText(NULL, IDS_ERROR_SAME_SERVICE, NULL, ERROR_HEADER, MB_OK);
						} else {
							// サービスに登録できませんでした。\nサービスの権限があるユーザーでログインして実行してください。
							MessageBoxResourceText(NULL, IDS_ERROR_INSTALL_SERVICE, NULL, ERROR_HEADER, MB_OK);
						}
					}
					return ERROR_INSTALL;
				}
			} else if(uninstall_flag) {
				if(service_name[0] == _T('\0')) {
					if(!no_error_flag) {
						// サービス名を指定してください
						MessageBoxResourceText(NULL, IDS_ERROR_NO_SERVICE_NAME, NULL, ERROR_HEADER, MB_OK);
					}
					return ERROR_PARAMETER;
				}
				if(question_flag) {
					// サービス service_name を削除しますか？
					if(MessageBoxResourceText(NULL, IDS_QUESTION_UNINSTALL, service_name, HEADER, MB_YESNO) != IDYES) {
						return ERROR_NO_INSTALL;
					}
				}
				if(service_install_flag) {
					// サービスから削除
					if(remove_service()) {
						if(ok_flag) {
							// サービス service_name を削除しました
							MessageBoxResourceText(NULL, IDS_UNINSTALL_OK, service_name, HEADER, MB_OK);
						}
					} else {
						if(!no_error_flag) {
							// サービスから削除できませんでした。\nサービスの権限があるユーザーでログインして実行してください。
							MessageBoxResourceText(NULL, IDS_ERROR_UNINSTALL_SERVICE, NULL, ERROR_HEADER, MB_OK);
						}
						return ERROR_INSTALL;
					}
				} else {
					if(!no_error_flag) {
						// サービス service_name は登録されていません
						MessageBoxResourceText(NULL, IDS_ERROR_NOT_INSTALL_SERVICE, service_name, ERROR_HEADER, MB_OK);
					}
					return ERROR_INSTALL;
				}
			} else {
				// 2010/6/10 Vista/7 で WM_DROPFILES を受ける
				funcChangeWindowMessageFilter ChangeWindowMessageFilter;
				if(ChangeWindowMessageFilter = (funcChangeWindowMessageFilter)GetProcAddress(LoadLibrary(_T("user32.dll")) ,"ChangeWindowMessageFilter")) {
					ChangeWindowMessageFilter(WM_DROPFILES, MSGFLT_ADD);
					ChangeWindowMessageFilter(WM_COPYDATA, MSGFLT_ADD);
					ChangeWindowMessageFilter(0x0049, MSGFLT_ADD);
				}

				// 設定ダイアログを表示
				hMain = CreateDialog(hInstance, MAKEINTRESOURCE(IDD_DIALOG_SETUP), GetDesktopWindow(), (DLGPROC)MainFunc);
				ShowWindow(hMain, SW_SHOW);
				// Drag&Drop を受け入れる準備
				DragAcceptFiles(hMain, TRUE);
				while(GetMessage(&msg, NULL, 0, 0)) {
					if(!IsDialogMessage(hMain, &msg)) {
						TranslateMessage(&msg);
						DispatchMessage(&msg);
					}
				}
			}
		}
	} else {
		// Windows NT/2000/XP/Vista/7 で起動してください。
		MessageBoxResourceText(NULL, IDS_ERROR_OS, NULL, ERROR_HEADER, MB_OK);
	}
	return 0;
}

//
//	ウインドウを列挙し、CreateProcess で起動したプロセス ID と
//	同じなら WM_CLOSE を送って終了させる。
//
BOOL CALLBACK EnumWindowsProc(HWND hWnd, LPARAM lParam)
{
	DWORD Pid, Tid;

	if(GetWindow(hWnd, GW_OWNER) == 0) {
		Tid = GetWindowThreadProcessId(hWnd, &Pid);
		if(Pid == process_id) {
			// 2002/6/18
			if(lParam == END_SYS_AND_CLOSE) {
				SendMessage(hWnd, WM_SYSCOMMAND, SC_CLOSE, 0);
				SendMessage(hWnd, WM_CLOSE, 0, 0);
			} else if(lParam == END_SYSCOMMAND) {
				SendMessage(hWnd, WM_SYSCOMMAND, SC_CLOSE, 0);
			} else {
				SendMessage(hWnd, WM_CLOSE, 0, 0);
			}
		}
	}
	return TRUE;
}

//
//	すでに起動済みかどうかを Mutex で判定
//	out: TRUE .. 起動済み
//
BOOL check_already(void)
{
	SECURITY_DESCRIPTOR sd;
	SECURITY_ATTRIBUTES sa;
	TCHAR mutex_name[MAX_PATH];

	wsprintf(mutex_name, MUTEX_STRING, service_name);
	// 排他処理用 mutex を作成
	mutex = OpenMutex(MUTEX_ALL_ACCESS, FALSE, mutex_name);
	if(mutex != NULL) {
		if(!service_flag) {
			// すでに起動されています。
			MessageBoxResourceText(NULL, IDS_ERROR_ALREADY_EXECUTE, NULL, ERROR_HEADER, MB_OK);
		}
		return TRUE;
	}
	sa.lpSecurityDescriptor = NULL;
	if(InitializeSecurityDescriptor(&sd, SECURITY_DESCRIPTOR_REVISION)) {
		if(SetSecurityDescriptorDacl(&sd, TRUE, (PACL)NULL, FALSE)) {
			sa.lpSecurityDescriptor = &sd;
		}
	}
	sa.nLength = sizeof(sa);
	sa.bInheritHandle = TRUE;
	mutex = CreateMutex(&sa, FALSE, mutex_name);
	return FALSE;
}

//
//	サービス状態情報を更新
//
BOOL send_status_to_scm(int state, int exit_code, int progress)
{
	SERVICE_STATUS ss;

	ss.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
	ss.dwCurrentState = state;
	ss.dwControlsAccepted = SERVICE_ACCEPT_STOP | SERVICE_ACCEPT_SHUTDOWN;
	ss.dwWin32ExitCode = exit_code;
	ss.dwServiceSpecificExitCode = 0;
	ss.dwCheckPoint = progress;
	ss.dwWaitHint = 3000;	// 3sec

	return SetServiceStatus(service_handle, &ss);
}

//
//	サービス制御要求処理
//	in: control .. SERVICE_CONTROL_STOP, SERVICE_CONTROL_SHUTDOWN で終了処理
//
void service_control_handler(int control)
{
	switch(control) {
		case SERVICE_CONTROL_STOP:
		case SERVICE_CONTROL_SHUTDOWN:
			// 2009/3/26
			shutdown_flag = 1;

			send_status_to_scm(SERVICE_STOP_PENDING, 0, 1);
			if(end_pattern == END_CTRL_BREAK) {
				GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, process_id);
// 2011/9/1 kimukou.buzz
			}else if(end_pattern == END_CTRL_C) {
				GenerateConsoleCtrlEvent(CTRL_C_EVENT, process_id);
				EnumWindows(EnumWindowsProc, END_SYS_AND_CLOSE);
// 2011/9/1 kimukou.buzz
			} else {
				EnumWindows(EnumWindowsProc, end_pattern);
			}
			send_status_to_scm(SERVICE_STOPPED, 0, 0);
			break;
	}
}

//
//	プログラム起動
//	in: exe_name .. プログラムファイルパス
//	    option_name .. プログラムコマンドラインオプション
//	    pi .. プロセス情報格納構造体のポインタ
//
BOOL execute_process(TCHAR *exe_name, TCHAR *option_name, PROCESS_INFORMATION *pi)
{
	STARTUPINFO si;
	TCHAR command[MAX_PATH], path[MAX_PATH];
	DWORD create;

	ZeroMemory(pi, sizeof(PROCESS_INFORMATION));
	GetStartupInfo(&si);
	wsprintf(command, _T("%s %s"), exe_name, option_name);
	lstrcpy(path, exe_name);
	extract_directory(path);
	create = CREATE_DEFAULT_ERROR_MODE;
// 2011/9/1 kimukou.buzz
	//if(end_pattern == END_CTRL_BREAK) {
	if(end_pattern == END_CTRL_BREAK || end_pattern == END_CTRL_C) {
// 2011/9/1 kimukou.buzz
		AllocConsole();
// 2011/9/21 kimukou.buzz comment start
		//create |= CREATE_NEW_PROCESS_GROUP;
// 2011/9/21 kimukou.buzz comment end
	}
	return CreateProcess(NULL, command, NULL, NULL, FALSE, create, NULL, path, &si, pi);
}

//
//	サービスメイン処理
//
void service_main(void)
{
	PROCESS_INFORMATION pi;

	service_handle = RegisterServiceCtrlHandler(service_name,
	                               (LPHANDLER_FUNCTION)service_control_handler);
	if(service_handle != 0) {
		send_status_to_scm(SERVICE_START_PENDING, 0, 1);

		send_status_to_scm(SERVICE_RUNNING, 0, 0);
		// プログラムを起動する。
		if(execute_process(exe_name, option_name, &pi)) {
			// 2009/3/26
			shutdown_flag = 0;

			process_id = pi.dwProcessId;
			send_status_to_scm(SERVICE_RUNNING, 0, 0);

			// サービスの終了で抜ける
			while(1) {
		    	if(WaitForSingleObject(pi.hProcess, 0) != WAIT_TIMEOUT) {
					// 2001/11/9
					CloseHandle(pi.hThread);
					CloseHandle(pi.hProcess);
					// 2009/3/26
					if(!retry_flag || shutdown_flag) {
						break;
					} else {
						if(execute_process(exe_name, option_name, &pi)) {
							process_id = pi.dwProcessId;
						} else {
							break;
						}
					}
				}
				Sleep(100);
			}
		}
// 2011/9/1 kimukou.buzz
		//if(end_pattern == END_CTRL_BREAK) {
		if(end_pattern == END_CTRL_BREAK || end_pattern == END_CTRL_C) {
// 2011/9/1 kimukou.buzz
			FreeConsole();
		}
		CloseHandle(mutex);
	}
	send_status_to_scm(SERVICE_STOPPED, 0, 0);
}

//
//	サービス開始
//
void start_service(void)
{
	SERVICE_TABLE_ENTRY serviceTable[] = {
		{
			service_name, (LPSERVICE_MAIN_FUNCTION)service_main
		}, {
			NULL, NULL
		}
	};
	StartServiceCtrlDispatcher(serviceTable);
}

//
//	サービスを登録する
//	out: ERROR_SUCCESS .. 登録成功
//
int install_service(void)
{
	SC_HANDLE scm, sc;
	int ret;
	DWORD stype, start;
	SERVICE_DESCRIPTION sd;

	if((scm = OpenSCManager(0, 0, SC_MANAGER_CREATE_SERVICE))) {
		stype = SERVICE_WIN32_OWN_PROCESS;
		if(desktop_flag) {
			stype |= SERVICE_INTERACTIVE_PROCESS;
		}
		if(auto_flag) {
			start = SERVICE_AUTO_START;
		} else {
			start = SERVICE_DEMAND_START;
		}
		if((sc = CreateService(scm, service_name, service_name,
		                       SERVICE_ALL_ACCESS, stype, start,
			                   SERVICE_ERROR_NORMAL,
							   module_name,
							   NULL, NULL, NULL, NULL, NULL))) {
			sd.lpDescription = description_name;
			ChangeServiceConfig2(sc, SERVICE_CONFIG_DESCRIPTION, &sd);

			CloseServiceHandle(sc);
			ret = ERROR_SUCCESS;
		} else {
			ret = GetLastError();
		}
		CloseServiceHandle(scm);
	} else {
		ret = GetLastError();
	}
	return ret;
}

//
//	サービスから削除
//	out: TRUE .. 削除成功
//
BOOL remove_service(void)
{
	SC_HANDLE scm, sc;
	SERVICE_STATUS st;
	BOOL ret;

	ret = FALSE;
	if((scm = OpenSCManager(0, 0, SC_MANAGER_CREATE_SERVICE))) {
		if((sc = OpenService(scm, service_name, SERVICE_ALL_ACCESS | DELETE))) {
			if(QueryServiceStatus(sc, &st)) {
				if(st.dwCurrentState != SERVICE_STOPPED) {
					if(ControlService(sc, SERVICE_CONTROL_STOP, &st)) {
						Sleep(500);
					}
				}
			}
			if(DeleteService(sc)) {
				ret = TRUE;
			}
			CloseServiceHandle(sc);
		}
		CloseServiceHandle(scm);
	}
	return ret;
}

//
//	サービスとして起動
//	out: TRUE .. 起動成功
//
BOOL restart_service(void)
{
	SC_HANDLE scm, sc;
	BOOL ret;

	ret = FALSE;
	if((scm = OpenSCManager(0, 0, SC_MANAGER_CREATE_SERVICE))) {
		if((sc = OpenService(scm, service_name, SERVICE_ALL_ACCESS))) {
			if(StartService(sc, 0, NULL)) {
				ret = TRUE;
			}
			CloseServiceHandle(sc);
		}
		CloseServiceHandle(scm);
	}
	return ret;
}

//
//	サービスとして起動したかチェック
//	実行ファイルのディレクトリと、カレントディレクトリを比較
//	out: TRUE .. サービスとして起動したと思われる
//
BOOL check_execute_service(void)
{
	TCHAR current_path[MAX_PATH];
	OSVERSIONINFO ovi;
	SC_HANDLE scm, sc;
	SERVICE_STATUS st;
	DWORD size;
	LPQUERY_SERVICE_CONFIG qsc;

	nt_flag = FALSE;
	service_install_flag = FALSE;
	service_stop_flag = FALSE;
	ovi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
	GetVersionEx(&ovi);
	if(ovi.dwPlatformId == VER_PLATFORM_WIN32_NT) {
		nt_flag = TRUE;
		GetCurrentDirectory(MAX_PATH, current_path);
		// 実行ファイルのパスとカレントパスが違う場合、
		// サービスとして起動されたと判定。
		// 起動ドライブのルートにこのプログラムが置かれてたり、
		// CreateProcess で違うディレクトリで起動されるとまずい
		// ような気がする。まあ大丈夫でしょう。いいかげん。
		if(_tcsicmp(execute_path, current_path)) {
			SetCurrentDirectory(execute_path);
			return TRUE;
		}
		// サービスインストールチェック
		if(scm = OpenSCManager(0, 0, SC_MANAGER_CREATE_SERVICE)) {
			if(sc = OpenService(scm, service_name, SERVICE_ALL_ACCESS)) {
				// サービスインストール済み
				service_install_flag = TRUE;
				// 2004/8/9
				QueryServiceConfig(sc, 0, 0, &size);
				qsc = (LPQUERY_SERVICE_CONFIG)LocalAlloc(LPTR, size);
				QueryServiceConfig(sc, qsc, size, &size);
				if(qsc->dwStartType == SERVICE_AUTO_START) {
					auto_flag = 1;
				} else if(qsc->dwStartType == SERVICE_DEMAND_START) {
					auto_flag = 0;
				}
				if(qsc->dwServiceType & SERVICE_INTERACTIVE_PROCESS) {
					desktop_flag = 1;
				} else {
					desktop_flag = 0;
				}
				LocalFree(qsc);
				if(QueryServiceStatus(sc, &st)) {
					if(st.dwCurrentState != SERVICE_STOPPED) {
						service_stop_flag = TRUE;
					}
					ControlService(sc, SERVICE_CONTROL_STOP, &st);
					Sleep(500);
				} else {
					// サービスを停止できません。\nサービスの権限のあるユーザーでログインしてください。
					MessageBoxResourceText(NULL, IDS_ERROR_STOP_SERVICE, NULL, ERROR_HEADER, MB_OK);
				}
				CloseServiceHandle(sc);
			}
			CloseServiceHandle(scm);
		}
	}
	return FALSE;
}

#ifdef UNICODE
//
//	ディレクトリのみ切り出す
//	in: path .. フルパス名
//
void extract_directory(TCHAR *path)
{
	TCHAR *pt;

	pt = NULL;
	while(*path != _T('\0')) {
		if(*path == _T('\\')) {
			pt = path;
		}
		path++;
	}
	if(pt != NULL) {
		*pt = _T('\0');
	}
}

//
//	ファイル名のみ取り出す
//	in: name .. ファイル名を格納するバッファ
//	    path .. フルパス名
//
void extract_name_only(TCHAR *name, TCHAR *path)
{
	TCHAR *pt;

	pt = path;
	while(*path != _T('\0')) {
		if(*path == _T('\\') || *path == _T(':')) {
			pt = path + 1;
		}
		path++;
	}
	while(*pt != _T('\0') && *pt != '.') {
		*name++ = *pt++;
	}
	*name = '\0';
}
//
//	" -> \", \ -> \\ とする
//	in: dst .. 変換後の文字列を格納するバッファ
//	    src .. 元の文字列バッファ
//
void encode_copy(TCHAR *dst, TCHAR *src)
{
	while(*src != _T('\0')) {
		if(*src == _T('\\') || *src == _T('"')) {
			*dst++ = _T('\\');
		}
		*dst++ = *src++;
	}
	*dst = _T('\0');
}

//
//	\" -> ", \\ -> \ とする
//	in: dst .. 変換後の文字列を格納するバッファ
//	    src .. 元の文字列バッファ
//
void decode_copy(TCHAR *dst, TCHAR *src)
{
	while(*src != _T('\0')) {
		if(*src == _T('\\')) {
			if(*(src + 1) == _T('\\') || *(src + 1) == _T('"')) {
				src++;
			}
		}
		*dst++ = *src++;
	}
	*dst = _T('\0');
}

#else
//
//	ShiftJIS の漢字１byte目かチェック
//	in: ch .. 文字コード
//	out: TRUE .. 漢字 1byte 目
//
int iskanji(unsigned char ch)
{
    if(((ch >= 0x81) && (ch <= 0x9f))
        || ((ch >= 0xe0) && (ch <= 0xfc))){
        return TRUE;
    }
    return FALSE;
}

//
//	ディレクトリのみ切り出す
//	in: path .. フルパス名
//
void extract_directory(TCHAR *path)
{
	int flag;
	TCHAR *pt;

	pt = NULL;
	flag = FALSE;
	while(*path != '\0') {
		if(flag) {
			flag = FALSE;
		} else {
			if(iskanji((unsigned char)*path)) {
				flag = TRUE;
			} else if(*path == '\\') {
				pt = path;
			}
		}
		path++;
	}
	if(pt != NULL) {
		*pt = '\0';
	}
}

//
//	ファイル名のみ取り出す
//	in: name .. ファイル名を格納するバッファ
//	    path .. フルパス名
//
void extract_name_only(TCHAR *name, TCHAR *path)
{
	int flag;
	TCHAR *pt;

	pt = path;
	flag = FALSE;
	while(*path != '\0') {
		if(flag) {
			flag = FALSE;
		} else {
			if(iskanji((unsigned char)*path)) {
				flag = TRUE;
			} else if(*path == '\\' || *path == ':') {
				pt = path + 1;
			}
		}
		path++;
	}
	while(*pt != '\0' && *pt != '.') {
		*name++ = *pt++;
	}
	*name = '\0';
}
// 2009/8/26
//
//	" -> \", \ -> \\ とする
//	in: dst .. 変換後の文字列を格納するバッファ
//	    src .. 元の文字列バッファ
//
void encode_copy(TCHAR *dst, TCHAR *src)
{
	int flag;

	flag = FALSE;
	while(*src != '\0') {
		if(flag) {
			flag = FALSE;
		} else {
			if(iskanji((unsigned char)*src)) {
				flag = TRUE;
			} else if(*src == '\\' || *src == '"') {
				*dst++ = '\\';
			}
		}
		*dst++ = *src++;
	}
	*dst = '\0';
}

//
//	\" -> ", \\ -> \ とする
//	in: dst .. 変換後の文字列を格納するバッファ
//	    src .. 元の文字列バッファ
//
void decode_copy(TCHAR *dst, TCHAR *src)
{
	int flag;

	flag = FALSE;
	while(*src != '\0') {
		if(flag) {
			flag = FALSE;
		} else {
			if(iskanji((unsigned char)*src)) {
				flag = TRUE;
			} else if(*src == '\\') {
				if(*(src + 1) == '\\' || *(src + 1) == '"') {
					src++;
				}
			}
		}
		*dst++ = *src++;
	}
	*dst = '\0';
}

#endif

//
//	拡張子の位置を取得する
//	in: path .. フルパス名
//	out: 拡張子の先頭位置のポインタ
//
TCHAR *extract_ext(TCHAR *path)
{
	int len;
	TCHAR *pt;

	len = lstrlen(path);
	pt = path + len - 1;
	while(pt != path) {
		if(*pt == '.') {
			return pt + 1;
		}
		if(*pt == '\\' || *pt == '/' || *pt == ':') {
			return path + len;
		}
		pt--;
	}
	return pt;
}

//
//	.ini ファイルから読み出し
//
void get_inifile(void)
{
	TCHAR temp[MAX_PATH];

	GetPrivateProfileString(HEADER, KEY_EXE, _T(""), exe_name, MAX_PATH, ini_name);
	GetPrivateProfileString(HEADER, KEY_OPTION, _T(""), temp, MAX_PATH, ini_name);
	// 2009/8/26
	if(GetPrivateProfileInt(HEADER, KEY_ENCODE, 0, ini_name) == 0) {
		lstrcpy(option_name, temp);
	} else {
		decode_copy(option_name, temp);
	}
	GetPrivateProfileString(HEADER, KEY_NAME, _T(""), service_name, MAX_PATH, ini_name);
	GetPrivateProfileString(HEADER, KEY_DESCRIPTION, _T(""), description_name, MAX_PATH, ini_name);
	// 2002/6/19
	end_pattern = GetPrivateProfileInt(HEADER, KEY_END, END_SYS_AND_CLOSE, ini_name);
	auto_flag = GetPrivateProfileInt(HEADER, KEY_AUTO, 1, ini_name);
	desktop_flag = GetPrivateProfileInt(HEADER, KEY_DESKTOP, 0, ini_name);
	retry_flag = GetPrivateProfileInt(HEADER, KEY_RETRY, 0, ini_name);
}

//
//	整数値を ini ファイルに書き込む
//	in: section .. セクション名
//	    name .. キー名
//	    no .. 書き込む値
//	    ini_file_path .. ini ファイルパス
//
void WritePrivateProfileInt(const TCHAR *section, const TCHAR *name, int no, TCHAR *ini_file_path)
{
	TCHAR temp[100];

	wsprintf(temp, _T("%d"), no);
	WritePrivateProfileString(section, name, temp, ini_file_path);
}

//
//	.ini ファイルに書き込み
//
void set_inifile(void)
{
	TCHAR temp[MAX_PATH];

	WritePrivateProfileString(HEADER, KEY_EXE, exe_name, ini_name);
	// 2009/8/26
	encode_copy(temp, option_name);
	WritePrivateProfileString(HEADER, KEY_OPTION, temp, ini_name);
	WritePrivateProfileString(HEADER, KEY_NAME, service_name, ini_name);
	WritePrivateProfileString(HEADER, KEY_DESCRIPTION, description_name, ini_name);
	WritePrivateProfileInt(HEADER, KEY_END, end_pattern, ini_name);
	// 2004/8/9
	WritePrivateProfileInt(HEADER, KEY_AUTO, auto_flag, ini_name);
	WritePrivateProfileInt(HEADER, KEY_DESKTOP, desktop_flag, ini_name);
	WritePrivateProfileInt(HEADER, KEY_RETRY, retry_flag, ini_name);
	// 2009/8/26
	WritePrivateProfileInt(HEADER, KEY_ENCODE, 1, ini_name);
}

//
//	テスト起動
//	in: exe_name .. プログラムファイルパス
//	    option_name .. プログラムコマンドラインオプション
//	    pattern .. 終了パターン
//
void execute_program(HWND hDlg, TCHAR *exe_name, TCHAR *option_name, int pattern)
{
	PROCESS_INFORMATION pi;
	DWORD count;

	// プログラムを起動する。
	if(execute_process(exe_name, option_name, &pi)) {
		WaitForInputIdle(pi.hProcess,INFINITE);
		SetForegroundWindow(hDlg);
		// プログラムを起動しました。\nＯＫをクリックするとプログラムを終了します。\n終了しない場合、手動でプログラムを終了させてください。\n\n※これは sexe からプログラムを終了されられるかどうかのテストで、\n　実際にサービスでの正常動作が保証されるわけではありません\n　のでご注意ください。
		MessageBoxResourceText(hDlg, IDS_TEST_EXECUTE, NULL, HEADER, MB_OK);
		process_id = pi.dwProcessId;
		if(pattern == END_CTRL_BREAK) {
			GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, process_id);
// 2011/9/19 kimukou.buzz
		}else if(pattern == END_CTRL_C) {
			GenerateConsoleCtrlEvent(CTRL_C_EVENT, process_id);
// 2011/9/19 kimukou.buzz
		} else {
			EnumWindows(EnumWindowsProc, pattern);
		}
		count = GetTickCount() + 30 * 1000;
		// プログラム終了もしくタイムアウト(30s)で抜ける
		while(count > GetTickCount()) {
	    	if(WaitForSingleObject(pi.hProcess, 0) != WAIT_TIMEOUT) {
				break;
			}
			Sleep(100);
		}
		// 2001/11/9
		CloseHandle(pi.hThread);
		CloseHandle(pi.hProcess);
	}
// 2011/9/19 kimukou.buzz
	//if(pattern == END_CTRL_BREAK) {
	if(pattern == END_CTRL_BREAK || pattern == END_CTRL_C) {
		FreeConsole();
	}
// 2011/9/19 kimukou.buzz
}

//
//	ダイアログメッセージ処理
//
BOOL APIENTRY MainFunc(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
	HWND h;
	HDROP hDrop;
	OPENFILENAME ofn;
	HICON hicon;
	TCHAR temp[MAX_PATH];
	TCHAR *ext;

	switch(message) {
	case WM_INITDIALOG:
		// アイコンをセット
		if(hicon = LoadIcon(h_instance, MAKEINTRESOURCE(IDI_ICON_SEXE))) {
			SendMessage(hDlg, WM_SETICON, ICON_SMALL, (LPARAM)hicon);
			SendMessage(hDlg, WM_SETICON, ICON_BIG, (LPARAM)hicon);
		}
		// 設定値をコントロールにセット
		h = GetDlgItem(hDlg, IDC_EDIT_EXE);
		SendMessage(h, EM_LIMITTEXT, MAX_PATH, 0);
		SetWindowText(h, exe_name);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_EDIT_OPTION);
		SendMessage(h, EM_LIMITTEXT, MAX_PATH, 0);
		SetWindowText(h, option_name);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_EDIT_NAME);
		SendMessage(h, EM_LIMITTEXT, MAX_PATH, 0);
		SetWindowText(h, service_name);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_EDIT_DESCRIPTION);
		SendMessage(h, EM_LIMITTEXT, MAX_PATH, 0);
		SetWindowText(h, description_name);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_COMBO_END);
		LoadString(h_instance, IDS_ITEM_CLOSE, temp, MAX_PATH);
		SendMessage(h, CB_ADDSTRING, 0, (DWORD_PTR)temp);
		
		// 2002/6/18
		LoadString(h_instance, IDS_ITEM_SYSCOMMAND, temp, MAX_PATH);
		SendMessage(h, CB_ADDSTRING, 0, (DWORD_PTR)temp);
		LoadString(h_instance, IDS_ITEM_SYS_CLOSE, temp, MAX_PATH);
		SendMessage(h, CB_ADDSTRING, 0, (DWORD_PTR)temp);

		// 2004/8/9
		LoadString(h_instance, IDS_ITEM_CTRL_BREAK, temp, MAX_PATH);
		SendMessage(h, CB_ADDSTRING, 0, (DWORD_PTR)temp);

// 2011/9/1 kimukou.buzz
		LoadString(h_instance, IDS_ITEM_CTRL_C, temp, MAX_PATH);
		SendMessage(h, CB_ADDSTRING, 0, (DWORD_PTR)temp);
// 2011/9/1 kimukou.buzz

		SendMessage(h, CB_SETCURSEL, end_pattern, 0);
		EnableWindow(h, !service_install_flag);

		// 2001/11/9
		h = GetDlgItem(hDlg, IDC_BUTTON_EXE);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_BUTTON_TEST);
		if(service_install_flag) {
			LoadString(h_instance, IDS_BUTTON_DELETE, temp, MAX_PATH);
			SetWindowText(h, temp);
		}

		// 2004/8/9
		h = GetDlgItem(hDlg, IDC_CHECK_AUTO);
		SendMessage(h, BM_SETCHECK, auto_flag, 0);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_CHECK_DESKTOP);
		SendMessage(h, BM_SETCHECK, desktop_flag, 0);
		EnableWindow(h, !service_install_flag);

		h = GetDlgItem(hDlg, IDC_CHECK_RETRY);
		SendMessage(h, BM_SETCHECK, retry_flag, 0);
		EnableWindow(h, !service_install_flag);
		break;

	case WM_COMMAND:
		switch(LOWORD (wParam)) {
		case IDOK:
			// 設定値得る
			if(!service_install_flag) {
				h = GetDlgItem(hDlg, IDC_EDIT_NAME);
				GetWindowText(h, service_name, MAX_PATH);
				if(service_name[0] == _T('\0')) {
					// サービス名を指定してください
					MessageBoxResourceText(hDlg, IDS_ERROR_NO_SERVICE_NAME, NULL, ERROR_HEADER, MB_OK);
					break;
				}
				h = GetDlgItem(hDlg, IDC_EDIT_EXE);
				GetWindowText(h, exe_name, MAX_PATH);
				// 2007/12/14 test
				ext = extract_ext(exe_name);
				if(exe_name[0] == _T('\0') || (_tcsicmp(ext, _T("exe")) && _tcsicmp(ext, _T("bat")))) {
					// プログラム名を指定してください
					MessageBoxResourceText(hDlg, IDS_ERROR_NO_PROGRAM_NAME, NULL, ERROR_HEADER, MB_OK);
					break;
				}

				h = GetDlgItem(hDlg, IDC_EDIT_OPTION);
				GetWindowText(h, option_name, MAX_PATH);

				h = GetDlgItem(hDlg, IDC_EDIT_DESCRIPTION);
				GetWindowText(h, description_name, MAX_PATH);

				h = GetDlgItem(hDlg, IDC_COMBO_END);
				end_pattern = (int)SendMessage(h, CB_GETCURSEL, 0, 0);

				// 2004/8/9
				h = GetDlgItem(hDlg, IDC_CHECK_AUTO);
				auto_flag = (int)SendMessage(h, BM_GETCHECK, 0, 0);

				h = GetDlgItem(hDlg, IDC_CHECK_DESKTOP);
				desktop_flag = (int)SendMessage(h, BM_GETCHECK, 0, 0);

				h = GetDlgItem(hDlg, IDC_CHECK_RETRY);
				retry_flag = (int)SendMessage(h, BM_GETCHECK, 0, 0);

				// 設定値を保存
				set_inifile();

				// サービス %s を登録しますか？
				if(MessageBoxResourceText(hDlg, IDS_QUESTION_INSTALL, service_name, HEADER, MB_YESNO) == IDYES) {
					int err;
					if((err = install_service()) == ERROR_SUCCESS) {
						// 2001/11/9
						// サービス %s を登録しました。サービスとして起動しますか？
						if(MessageBoxResourceText(hDlg, IDS_QUESTION_INSTALL_START, service_name, HEADER, MB_YESNO) == IDYES) {
							restart_service();
						}
					} else {
						if(err == ERROR_SERVICE_EXISTS) {
							// すでに同名のサービスが登録済みです
							MessageBoxResourceText(NULL, IDS_ERROR_SAME_SERVICE, NULL, ERROR_HEADER, MB_OK);
						} else {
							// サービスに登録できませんでした。\nサービスの権限があるユーザーでログインして実行してください。
							MessageBoxResourceText(hDlg, IDS_ERROR_INSTALL_SERVICE, NULL, HEADER, MB_OK);
						}
					}
				}
			}
			DestroyWindow(hDlg);
			break;

		case IDCANCEL:
			DestroyWindow(hDlg);
			break;

		case IDC_BUTTON_EXE:
			{
				TCHAR filter[MAX_PATH];
				TCHAR title[MAX_PATH];
				int len;
				// 参照ボタンが押された
				temp[0] = '\0';
				FillMemory(&ofn, sizeof(OPENFILENAME), 0);
				ofn.lStructSize = sizeof(OPENFILENAME);
				ofn.hwndOwner = hDlg;
				ofn.nMaxFile = MAX_PATH;
				ofn.lpstrFile = temp;
				// 実行ファイル(*.exe)\0*.exe\0"
				LoadString(h_instance, IDS_FILE_EXE, filter, MAX_PATH);
				len = lstrlen(filter);
				LoadString(h_instance, IDS_FILE_WILD_EXE, filter + len + 1, MAX_PATH - len - 2);
				len += lstrlen(filter + len + 1);
				*(filter + len + 2) = '\0';
// 2011/9/1 kimukou.buzz
				// 実行ファイル(*.bat)\0*.bat\0"
/*
				char* pos = *(filter + len + 3);
				LoadString(h_instance, IDS_FILE_BAT, pos, MAX_PATH);
				len = lstrlen(filter);
				LoadString(h_instance, IDS_FILE_WILD_BAT, pos + len + 1, MAX_PATH - len - 2);
				len += lstrlen(pos + len + 1);
				*(filter + len + 2) = '\0';
*/
// 2011/9/1 kimukou.buzz
				
				ofn.lpstrFilter = filter;
				// 実行ファイルの選択
				LoadString(h_instance, IDS_FILE_TITLE, title, MAX_PATH);
				ofn.lpstrTitle = title;
				ofn.Flags = OFN_HIDEREADONLY | OFN_FILEMUSTEXIST | OFN_NOCHANGEDIR;
				if(GetOpenFileName(&ofn)) {
					ext = extract_ext(temp);
					if(!_tcsicmp(ext, _T("exe")) || !_tcsicmp(ext, _T("bat"))) {
						TCHAR name[MAX_PATH];
						// .exe ファイルをセット
						h = GetDlgItem(hDlg, IDC_EDIT_EXE);
						SetWindowText(h, (LPCTSTR)temp);
						// サービス名として .exe ファイルの名前をセット
						extract_name_only(name, temp);
						h = GetDlgItem(hDlg, IDC_EDIT_NAME);
						SetWindowText(h, (LPCTSTR)name);
					} else {
						// プログラムファイルのみ登録可能です。
						MessageBoxResourceText(hDlg, IDS_ERROR_NOT_PROGRAM, NULL, ERROR_HEADER, MB_OK);
					}
				}
			}
			break;

		case IDC_BUTTON_TEST:
			// サービスとしてインストール済み？
			if(service_install_flag) {
				// サービス service_name を削除しますか？
				if(MessageBoxResourceText(hDlg, IDS_QUESTION_UNINSTALL, service_name, HEADER, MB_YESNO) == IDYES) {
					// サービスから削除
					if(remove_service()) {
						// サービス service_name を削除しました
						MessageBoxResourceText(hDlg, IDS_UNINSTALL_OK, service_name, HEADER, MB_OK);

						service_install_flag = FALSE;
						h = GetDlgItem(hDlg, IDC_EDIT_EXE);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_EDIT_OPTION);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_EDIT_NAME);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_EDIT_DESCRIPTION);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_COMBO_END);
						EnableWindow(h, TRUE);

						// 2001/11/9
						h = GetDlgItem(hDlg, IDC_BUTTON_EXE);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_BUTTON_TEST);
						// テスト起動(&T)
						LoadString(h_instance, IDS_BUTTON_TEST, temp, MAX_PATH);
						SetWindowText(h, temp);

						// 2004/8/9
						h = GetDlgItem(hDlg, IDC_CHECK_AUTO);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_CHECK_DESKTOP);
						EnableWindow(h, TRUE);

						h = GetDlgItem(hDlg, IDC_CHECK_RETRY);
						EnableWindow(h, TRUE);
					} else {
						// サービスから削除できませんでした。\nサービスの権限があるユーザーでログインして実行してください。
						MessageBoxResourceText(NULL, IDS_ERROR_UNINSTALL_SERVICE, NULL, ERROR_HEADER, MB_OK);
					}
				}
			} else {
				TCHAR param[MAX_PATH];

				// テスト起動処理
				h = GetDlgItem(hDlg, IDC_EDIT_EXE);
				GetWindowText(h, temp, MAX_PATH);

				h = GetDlgItem(hDlg, IDC_EDIT_OPTION);
				GetWindowText(h, param, MAX_PATH);

				h = GetDlgItem(hDlg, IDC_COMBO_END);
				execute_program(hDlg, temp, param, (int)SendMessage(h, CB_GETCURSEL, 0, 0));
			}
			break;
		}
		break;

	case WM_DROPFILES:
		// Drag&Drop で受けたファイル名を取り出す
		hDrop = (HDROP)wParam;
		DragQueryFile(hDrop, 0, temp, MAX_PATH);
		DragFinish(hDrop);
		if(!service_install_flag) {
// 2011/9/1 kimukou.buzz
			//if(!_tcsicmp(extract_ext(temp), _T("exe"))) {
			if(!_tcsicmp(extract_ext(temp), _T("exe")) || !_tcsicmp(extract_ext(temp), _T("bat")) ) {
// 2011/9/1 kimukou.buzz
				TCHAR name[MAX_PATH];
				// .exe ファイルをセット
				h = GetDlgItem(hDlg, IDC_EDIT_EXE);
				SetWindowText(h, temp);
				// サービス名として .exe ファイルの名前をセット
				extract_name_only(name, temp);
				h = GetDlgItem(hDlg, IDC_EDIT_NAME);
				SetWindowText(h, (LPCTSTR)name);
			} else {
				// プログラムファイルのみ登録可能です。
				MessageBoxResourceText(hDlg, IDS_ERROR_NOT_PROGRAM, NULL, ERROR_HEADER, MB_OK);
			}
		}
		SetForegroundWindow(hDlg);
		break;

	case WM_HELP:
		ShellExecute(hDlg, _T("open"), _T("sexe.chm"), NULL, NULL, SW_SHOW);
		break;

	case WM_DESTROY:
		// 2001/11/9
		if(service_stop_flag) {
			restart_service();
		}
		PostQuitMessage(0);
		break;

	default:
		return FALSE;
	}
	return FALSE;
}

