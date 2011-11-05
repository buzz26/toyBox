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

// 2010/6/10 Vista/7 �� WM_DROPFILES ���󂯂�
typedef BOOL (WINAPI *funcChangeWindowMessageFilter)(UINT, DWORD);
#define MSGFLT_ADD				1
#define MSGFLT_REMOVE			2

// �w�b�_�Ƃ�
const TCHAR *HEADER = _T("sexe");
const TCHAR *ERROR_HEADER = _T("sexe error");
const TCHAR *MUTEX_STRING = _T("$$sexe$$%s$$");
const TCHAR *WINDOW_TITLE = _T("sexe");

// .ini �̃L�[
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

// �ʓ|�Ȃ̂ŃO���[�o���ϐ����g���܂����Ă���܂��B
SERVICE_STATUS_HANDLE service_handle;
BOOL service_flag;					// running serivce
BOOL service_install_flag;			// install service
BOOL service_stop_flag;				// stop service
BOOL nt_flag;						// runnning Windows NT ?

TCHAR module_name[MAX_PATH];		// sexe.exe �̃t���p�X
TCHAR execute_path[MAX_PATH];		// sexe.exe �̃t�H���_
TCHAR ini_name[MAX_PATH];			// sexe.ini �̃t���p�X
TCHAR exe_name[MAX_PATH];			// �T�[�r�X�Ƃ��ē��삳����v���O�����̃t���p�X
TCHAR option_name[MAX_PATH];		// �N�����I�v�V����
TCHAR service_name[MAX_PATH];		// �T�[�r�X��
TCHAR description_name[MAX_PATH];	// 2009/3/26 �T�[�r�X����
int end_pattern;					// �I�����@
int auto_flag;						// 2004/8/9 �����N��
int desktop_flag;					// 2004/8/9 �f�X�N�g�b�v�Ƃ̑Θb������
int retry_flag;						// 2009/3/26 �ċN��
int shutdown_flag;					// 2009/3/26 Shutdown
BOOL install_flag;					// 2011/4/28 �C���X�g�[��
BOOL uninstall_flag;				// 2011/4/28 �A���C���X�g�[��
BOOL read_ini_flag;					// 2011/4/28 �C���X�g�[������ ini �t�@�C�����Q��
BOOL start_flag;					// 2011/4/28 �C���X�g�[�������̂܂܃T�[�r�X�J�n
BOOL no_error_flag;					// 2011/5/31 �G���[���b�Z�[�W��\��
BOOL question_flag;					// 2011/5/31 �C���X�g�[��/�A���C���X�g�[���₢���킹
BOOL ok_flag;					// 2011/5/31 �I�����b�Z�[�W�\��
HANDLE mutex;						// ��d�N���h�~�p Mutex
DWORD process_id;					// �N�������v���O�����̃v���Z�X ID
HINSTANCE h_instance;				// hInstance

// �֐���`
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
//	�X�y�[�X����у^�u���΂�
//	in: pt .. �|�C���^
//	out: �X�y�[�X����у^�u���΂�����̃|�C���^
//
TCHAR *space_skip(TCHAR *pt)
{
	while(*pt != _T(' ') && *pt != _T('\t') && *pt != _T('\0')) {
		pt++;
	}
	return pt;
}

//
//	�p�����[�^���o�b�t�@�ɃR�s�[
//	�X�y�[�X�������͏I���܂ŃR�s�[���邪�A
//	"" �ň͂�ꂽ�X�y�[�X�͖���
//	in: dst .. �R�s�[��o�b�t�@
//	    src .. �R�s�[���o�b�t�@
//	    size .. �o�b�t�@�T�C�Y
//	out: �R�s�[�I����̃|�C���^
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
//	���\�[�X���當�����ǂݍ���Ń��b�Z�[�W�{�b�N�X��\��
//	in: wnd .. �I�[�i�[�E�C���h�E�n���h��
//	    id  .. ���\�[�X ID
//	    param .. �p�����[�^������(NULL �Ŗ�)
//	    caption .. �^�C�g��������
//	    type .. ���b�Z�[�W�{�b�N�X�X�^�C��
//	out: MessageBox() �̕Ԓl
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
//	�R�}���h���C���p�����[�^��͏���
//	in: pt .. �R�}���h���C���p�����[�^
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
		// �C���X�g�[���܂��̓A���C���X�g�[������ꍇ
		if(read_ini_flag) {
			// ini �t�@�C������ݒ�l��ǂ�
			get_inifile();
		} else {
			// �p�����[�^�̒l���g�p����
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
		// �ʏ�N���̏ꍇ�Aini �t�@�C������ݒ�l��ǂ�
		get_inifile();
	}
}

//
//	Windows ���C���֐�
//
int WINAPI _tWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPTSTR lpszArgs, int nWinMode)
{
    MSG msg;
	HWND hMain;
	int err;

	// ������
	install_flag = FALSE;
	uninstall_flag = FALSE;
	read_ini_flag = FALSE;
	start_flag = FALSE;
	no_error_flag = FALSE;
	question_flag = FALSE;
	ok_flag = FALSE;

	h_instance = hInstance;

	// ���s�f�B���N�g���ɂ��� sexe.exe �̃t���p�X���쐬
	GetModuleFileName(NULL, module_name, sizeof(module_name));
	lstrcpy(execute_path, module_name);
	extract_directory(execute_path);
	lstrcpy(ini_name, execute_path);
	lstrcat(ini_name, _T("\\sexe.ini"));

	// �R�}���h���C�����
	analyze_args(lpszArgs);

	// �T�[�r�X�œ��삵�Ă��邩�H
	service_flag = check_execute_service();
	// WindowsNT/2000 ?
	if(nt_flag) {
		// �T�[�r�X�Ƃ��ē��쒆�H
		if(service_flag) {
			// ���łɓ��쒆�H
			if(!check_already()) {
				// �T�[�r�X�Ƃ��ċN��
				start_service();
			}
		} else {
			// 2011/5/31
			// �R�}���h���C���p�����[�^�ŃC���X�g�[���E�A���C���X�g�[��
			if(install_flag) {
				TCHAR *ext;
				if(service_name[0] == _T('\0')) {
					if(!no_error_flag) {
						// �T�[�r�X�����w�肵�Ă�������
						MessageBoxResourceText(NULL, IDS_ERROR_NO_SERVICE_NAME, NULL, ERROR_HEADER, MB_OK);
					}
					return ERROR_PARAMETER;
				}
				ext = extract_ext(exe_name);
				if(exe_name[0] == _T('\0') || (_tcsicmp(ext, _T("exe")) && _tcsicmp(ext, _T("bat")))) {
					if(!no_error_flag) {
						// �v���O���������w�肵�Ă�������
						MessageBoxResourceText(NULL, IDS_ERROR_NO_PROGRAM_NAME, NULL, ERROR_HEADER, MB_OK);
					}
					return ERROR_PARAMETER;
				}
				if(question_flag) {
					// �T�[�r�X service_name ��o�^���܂����H
					if(MessageBoxResourceText(NULL, IDS_QUESTION_INSTALL, service_name, ERROR_HEADER, MB_YESNO) != IDYES) {
						return ERROR_NO_INSTALL;
					}
				}
				if(!read_ini_flag) {
					// ini �t�@�C������ǂݏo�����̂łȂ���ΐݒ�l��ۑ�
					set_inifile();
				}
				// �C���X�g�[��
				if((err = install_service()) == ERROR_SUCCESS) {
					if(start_flag) {
						// �T�[�r�X�J�n
						if(restart_service()) {
							if(ok_flag) {
								// �T�[�r�X service_name ��o�^���A�J�n���܂����B
								MessageBoxResourceText(NULL, IDS_INSTALL_START_OK, service_name, HEADER, MB_OK);
							}
						} else if(!no_error_flag) {
							// �T�[�r�X service_name ��o�^���܂������A�J�n�Ɏ��s���܂����B
							MessageBoxResourceText(NULL, IDS_ERROR_INSTALL_START, service_name, HEADER, MB_OK);
							return ERROR_START;
						}
					} else if(ok_flag) {
						// �T�[�r�X service_name ��o�^���܂����B
						MessageBoxResourceText(NULL, IDS_INSTALL_OK, service_name, HEADER, MB_OK);
					}
				} else {
					if(!no_error_flag) {
						if(err == ERROR_SERVICE_EXISTS) {
							// ���łɓ����̃T�[�r�X���o�^�ς݂ł�
							MessageBoxResourceText(NULL, IDS_ERROR_SAME_SERVICE, NULL, ERROR_HEADER, MB_OK);
						} else {
							// �T�[�r�X�ɓo�^�ł��܂���ł����B\n�T�[�r�X�̌��������郆�[�U�[�Ń��O�C�����Ď��s���Ă��������B
							MessageBoxResourceText(NULL, IDS_ERROR_INSTALL_SERVICE, NULL, ERROR_HEADER, MB_OK);
						}
					}
					return ERROR_INSTALL;
				}
			} else if(uninstall_flag) {
				if(service_name[0] == _T('\0')) {
					if(!no_error_flag) {
						// �T�[�r�X�����w�肵�Ă�������
						MessageBoxResourceText(NULL, IDS_ERROR_NO_SERVICE_NAME, NULL, ERROR_HEADER, MB_OK);
					}
					return ERROR_PARAMETER;
				}
				if(question_flag) {
					// �T�[�r�X service_name ���폜���܂����H
					if(MessageBoxResourceText(NULL, IDS_QUESTION_UNINSTALL, service_name, HEADER, MB_YESNO) != IDYES) {
						return ERROR_NO_INSTALL;
					}
				}
				if(service_install_flag) {
					// �T�[�r�X����폜
					if(remove_service()) {
						if(ok_flag) {
							// �T�[�r�X service_name ���폜���܂���
							MessageBoxResourceText(NULL, IDS_UNINSTALL_OK, service_name, HEADER, MB_OK);
						}
					} else {
						if(!no_error_flag) {
							// �T�[�r�X����폜�ł��܂���ł����B\n�T�[�r�X�̌��������郆�[�U�[�Ń��O�C�����Ď��s���Ă��������B
							MessageBoxResourceText(NULL, IDS_ERROR_UNINSTALL_SERVICE, NULL, ERROR_HEADER, MB_OK);
						}
						return ERROR_INSTALL;
					}
				} else {
					if(!no_error_flag) {
						// �T�[�r�X service_name �͓o�^����Ă��܂���
						MessageBoxResourceText(NULL, IDS_ERROR_NOT_INSTALL_SERVICE, service_name, ERROR_HEADER, MB_OK);
					}
					return ERROR_INSTALL;
				}
			} else {
				// 2010/6/10 Vista/7 �� WM_DROPFILES ���󂯂�
				funcChangeWindowMessageFilter ChangeWindowMessageFilter;
				if(ChangeWindowMessageFilter = (funcChangeWindowMessageFilter)GetProcAddress(LoadLibrary(_T("user32.dll")) ,"ChangeWindowMessageFilter")) {
					ChangeWindowMessageFilter(WM_DROPFILES, MSGFLT_ADD);
					ChangeWindowMessageFilter(WM_COPYDATA, MSGFLT_ADD);
					ChangeWindowMessageFilter(0x0049, MSGFLT_ADD);
				}

				// �ݒ�_�C�A���O��\��
				hMain = CreateDialog(hInstance, MAKEINTRESOURCE(IDD_DIALOG_SETUP), GetDesktopWindow(), (DLGPROC)MainFunc);
				ShowWindow(hMain, SW_SHOW);
				// Drag&Drop ���󂯓���鏀��
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
		// Windows NT/2000/XP/Vista/7 �ŋN�����Ă��������B
		MessageBoxResourceText(NULL, IDS_ERROR_OS, NULL, ERROR_HEADER, MB_OK);
	}
	return 0;
}

//
//	�E�C���h�E��񋓂��ACreateProcess �ŋN�������v���Z�X ID ��
//	�����Ȃ� WM_CLOSE �𑗂��ďI��������B
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
//	���łɋN���ς݂��ǂ����� Mutex �Ŕ���
//	out: TRUE .. �N���ς�
//
BOOL check_already(void)
{
	SECURITY_DESCRIPTOR sd;
	SECURITY_ATTRIBUTES sa;
	TCHAR mutex_name[MAX_PATH];

	wsprintf(mutex_name, MUTEX_STRING, service_name);
	// �r�������p mutex ���쐬
	mutex = OpenMutex(MUTEX_ALL_ACCESS, FALSE, mutex_name);
	if(mutex != NULL) {
		if(!service_flag) {
			// ���łɋN������Ă��܂��B
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
//	�T�[�r�X��ԏ����X�V
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
//	�T�[�r�X����v������
//	in: control .. SERVICE_CONTROL_STOP, SERVICE_CONTROL_SHUTDOWN �ŏI������
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
//	�v���O�����N��
//	in: exe_name .. �v���O�����t�@�C���p�X
//	    option_name .. �v���O�����R�}���h���C���I�v�V����
//	    pi .. �v���Z�X���i�[�\���̂̃|�C���^
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
//	�T�[�r�X���C������
//
void service_main(void)
{
	PROCESS_INFORMATION pi;

	service_handle = RegisterServiceCtrlHandler(service_name,
	                               (LPHANDLER_FUNCTION)service_control_handler);
	if(service_handle != 0) {
		send_status_to_scm(SERVICE_START_PENDING, 0, 1);

		send_status_to_scm(SERVICE_RUNNING, 0, 0);
		// �v���O�������N������B
		if(execute_process(exe_name, option_name, &pi)) {
			// 2009/3/26
			shutdown_flag = 0;

			process_id = pi.dwProcessId;
			send_status_to_scm(SERVICE_RUNNING, 0, 0);

			// �T�[�r�X�̏I���Ŕ�����
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
//	�T�[�r�X�J�n
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
//	�T�[�r�X��o�^����
//	out: ERROR_SUCCESS .. �o�^����
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
//	�T�[�r�X����폜
//	out: TRUE .. �폜����
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
//	�T�[�r�X�Ƃ��ċN��
//	out: TRUE .. �N������
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
//	�T�[�r�X�Ƃ��ċN���������`�F�b�N
//	���s�t�@�C���̃f�B���N�g���ƁA�J�����g�f�B���N�g�����r
//	out: TRUE .. �T�[�r�X�Ƃ��ċN�������Ǝv����
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
		// ���s�t�@�C���̃p�X�ƃJ�����g�p�X���Ⴄ�ꍇ�A
		// �T�[�r�X�Ƃ��ċN�����ꂽ�Ɣ���B
		// �N���h���C�u�̃��[�g�ɂ��̃v���O�������u����Ă���A
		// CreateProcess �ňႤ�f�B���N�g���ŋN�������Ƃ܂���
		// �悤�ȋC������B�܂����v�ł��傤�B����������B
		if(_tcsicmp(execute_path, current_path)) {
			SetCurrentDirectory(execute_path);
			return TRUE;
		}
		// �T�[�r�X�C���X�g�[���`�F�b�N
		if(scm = OpenSCManager(0, 0, SC_MANAGER_CREATE_SERVICE)) {
			if(sc = OpenService(scm, service_name, SERVICE_ALL_ACCESS)) {
				// �T�[�r�X�C���X�g�[���ς�
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
					// �T�[�r�X���~�ł��܂���B\n�T�[�r�X�̌����̂��郆�[�U�[�Ń��O�C�����Ă��������B
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
//	�f�B���N�g���̂ݐ؂�o��
//	in: path .. �t���p�X��
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
//	�t�@�C�����̂ݎ��o��
//	in: name .. �t�@�C�������i�[����o�b�t�@
//	    path .. �t���p�X��
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
//	" -> \", \ -> \\ �Ƃ���
//	in: dst .. �ϊ���̕�������i�[����o�b�t�@
//	    src .. ���̕�����o�b�t�@
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
//	\" -> ", \\ -> \ �Ƃ���
//	in: dst .. �ϊ���̕�������i�[����o�b�t�@
//	    src .. ���̕�����o�b�t�@
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
//	ShiftJIS �̊����Pbyte�ڂ��`�F�b�N
//	in: ch .. �����R�[�h
//	out: TRUE .. ���� 1byte ��
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
//	�f�B���N�g���̂ݐ؂�o��
//	in: path .. �t���p�X��
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
//	�t�@�C�����̂ݎ��o��
//	in: name .. �t�@�C�������i�[����o�b�t�@
//	    path .. �t���p�X��
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
//	" -> \", \ -> \\ �Ƃ���
//	in: dst .. �ϊ���̕�������i�[����o�b�t�@
//	    src .. ���̕�����o�b�t�@
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
//	\" -> ", \\ -> \ �Ƃ���
//	in: dst .. �ϊ���̕�������i�[����o�b�t�@
//	    src .. ���̕�����o�b�t�@
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
//	�g���q�̈ʒu���擾����
//	in: path .. �t���p�X��
//	out: �g���q�̐擪�ʒu�̃|�C���^
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
//	.ini �t�@�C������ǂݏo��
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
//	�����l�� ini �t�@�C���ɏ�������
//	in: section .. �Z�N�V������
//	    name .. �L�[��
//	    no .. �������ޒl
//	    ini_file_path .. ini �t�@�C���p�X
//
void WritePrivateProfileInt(const TCHAR *section, const TCHAR *name, int no, TCHAR *ini_file_path)
{
	TCHAR temp[100];

	wsprintf(temp, _T("%d"), no);
	WritePrivateProfileString(section, name, temp, ini_file_path);
}

//
//	.ini �t�@�C���ɏ�������
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
//	�e�X�g�N��
//	in: exe_name .. �v���O�����t�@�C���p�X
//	    option_name .. �v���O�����R�}���h���C���I�v�V����
//	    pattern .. �I���p�^�[��
//
void execute_program(HWND hDlg, TCHAR *exe_name, TCHAR *option_name, int pattern)
{
	PROCESS_INFORMATION pi;
	DWORD count;

	// �v���O�������N������B
	if(execute_process(exe_name, option_name, &pi)) {
		WaitForInputIdle(pi.hProcess,INFINITE);
		SetForegroundWindow(hDlg);
		// �v���O�������N�����܂����B\n�n�j���N���b�N����ƃv���O�������I�����܂��B\n�I�����Ȃ��ꍇ�A�蓮�Ńv���O�������I�������Ă��������B\n\n������� sexe ����v���O�������I��������邩�ǂ����̃e�X�g�ŁA\n�@���ۂɃT�[�r�X�ł̐��퓮�삪�ۏ؂����킯�ł͂���܂���\n�@�̂ł����ӂ��������B
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
		// �v���O�����I���������^�C���A�E�g(30s)�Ŕ�����
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
//	�_�C�A���O���b�Z�[�W����
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
		// �A�C�R�����Z�b�g
		if(hicon = LoadIcon(h_instance, MAKEINTRESOURCE(IDI_ICON_SEXE))) {
			SendMessage(hDlg, WM_SETICON, ICON_SMALL, (LPARAM)hicon);
			SendMessage(hDlg, WM_SETICON, ICON_BIG, (LPARAM)hicon);
		}
		// �ݒ�l���R���g���[���ɃZ�b�g
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
			// �ݒ�l����
			if(!service_install_flag) {
				h = GetDlgItem(hDlg, IDC_EDIT_NAME);
				GetWindowText(h, service_name, MAX_PATH);
				if(service_name[0] == _T('\0')) {
					// �T�[�r�X�����w�肵�Ă�������
					MessageBoxResourceText(hDlg, IDS_ERROR_NO_SERVICE_NAME, NULL, ERROR_HEADER, MB_OK);
					break;
				}
				h = GetDlgItem(hDlg, IDC_EDIT_EXE);
				GetWindowText(h, exe_name, MAX_PATH);
				// 2007/12/14 test
				ext = extract_ext(exe_name);
				if(exe_name[0] == _T('\0') || (_tcsicmp(ext, _T("exe")) && _tcsicmp(ext, _T("bat")))) {
					// �v���O���������w�肵�Ă�������
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

				// �ݒ�l��ۑ�
				set_inifile();

				// �T�[�r�X %s ��o�^���܂����H
				if(MessageBoxResourceText(hDlg, IDS_QUESTION_INSTALL, service_name, HEADER, MB_YESNO) == IDYES) {
					int err;
					if((err = install_service()) == ERROR_SUCCESS) {
						// 2001/11/9
						// �T�[�r�X %s ��o�^���܂����B�T�[�r�X�Ƃ��ċN�����܂����H
						if(MessageBoxResourceText(hDlg, IDS_QUESTION_INSTALL_START, service_name, HEADER, MB_YESNO) == IDYES) {
							restart_service();
						}
					} else {
						if(err == ERROR_SERVICE_EXISTS) {
							// ���łɓ����̃T�[�r�X���o�^�ς݂ł�
							MessageBoxResourceText(NULL, IDS_ERROR_SAME_SERVICE, NULL, ERROR_HEADER, MB_OK);
						} else {
							// �T�[�r�X�ɓo�^�ł��܂���ł����B\n�T�[�r�X�̌��������郆�[�U�[�Ń��O�C�����Ď��s���Ă��������B
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
				// �Q�ƃ{�^���������ꂽ
				temp[0] = '\0';
				FillMemory(&ofn, sizeof(OPENFILENAME), 0);
				ofn.lStructSize = sizeof(OPENFILENAME);
				ofn.hwndOwner = hDlg;
				ofn.nMaxFile = MAX_PATH;
				ofn.lpstrFile = temp;
				// ���s�t�@�C��(*.exe)\0*.exe\0"
				LoadString(h_instance, IDS_FILE_EXE, filter, MAX_PATH);
				len = lstrlen(filter);
				LoadString(h_instance, IDS_FILE_WILD_EXE, filter + len + 1, MAX_PATH - len - 2);
				len += lstrlen(filter + len + 1);
				*(filter + len + 2) = '\0';
// 2011/9/1 kimukou.buzz
				// ���s�t�@�C��(*.bat)\0*.bat\0"
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
				// ���s�t�@�C���̑I��
				LoadString(h_instance, IDS_FILE_TITLE, title, MAX_PATH);
				ofn.lpstrTitle = title;
				ofn.Flags = OFN_HIDEREADONLY | OFN_FILEMUSTEXIST | OFN_NOCHANGEDIR;
				if(GetOpenFileName(&ofn)) {
					ext = extract_ext(temp);
					if(!_tcsicmp(ext, _T("exe")) || !_tcsicmp(ext, _T("bat"))) {
						TCHAR name[MAX_PATH];
						// .exe �t�@�C�����Z�b�g
						h = GetDlgItem(hDlg, IDC_EDIT_EXE);
						SetWindowText(h, (LPCTSTR)temp);
						// �T�[�r�X���Ƃ��� .exe �t�@�C���̖��O���Z�b�g
						extract_name_only(name, temp);
						h = GetDlgItem(hDlg, IDC_EDIT_NAME);
						SetWindowText(h, (LPCTSTR)name);
					} else {
						// �v���O�����t�@�C���̂ݓo�^�\�ł��B
						MessageBoxResourceText(hDlg, IDS_ERROR_NOT_PROGRAM, NULL, ERROR_HEADER, MB_OK);
					}
				}
			}
			break;

		case IDC_BUTTON_TEST:
			// �T�[�r�X�Ƃ��ăC���X�g�[���ς݁H
			if(service_install_flag) {
				// �T�[�r�X service_name ���폜���܂����H
				if(MessageBoxResourceText(hDlg, IDS_QUESTION_UNINSTALL, service_name, HEADER, MB_YESNO) == IDYES) {
					// �T�[�r�X����폜
					if(remove_service()) {
						// �T�[�r�X service_name ���폜���܂���
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
						// �e�X�g�N��(&T)
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
						// �T�[�r�X����폜�ł��܂���ł����B\n�T�[�r�X�̌��������郆�[�U�[�Ń��O�C�����Ď��s���Ă��������B
						MessageBoxResourceText(NULL, IDS_ERROR_UNINSTALL_SERVICE, NULL, ERROR_HEADER, MB_OK);
					}
				}
			} else {
				TCHAR param[MAX_PATH];

				// �e�X�g�N������
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
		// Drag&Drop �Ŏ󂯂��t�@�C���������o��
		hDrop = (HDROP)wParam;
		DragQueryFile(hDrop, 0, temp, MAX_PATH);
		DragFinish(hDrop);
		if(!service_install_flag) {
// 2011/9/1 kimukou.buzz
			//if(!_tcsicmp(extract_ext(temp), _T("exe"))) {
			if(!_tcsicmp(extract_ext(temp), _T("exe")) || !_tcsicmp(extract_ext(temp), _T("bat")) ) {
// 2011/9/1 kimukou.buzz
				TCHAR name[MAX_PATH];
				// .exe �t�@�C�����Z�b�g
				h = GetDlgItem(hDlg, IDC_EDIT_EXE);
				SetWindowText(h, temp);
				// �T�[�r�X���Ƃ��� .exe �t�@�C���̖��O���Z�b�g
				extract_name_only(name, temp);
				h = GetDlgItem(hDlg, IDC_EDIT_NAME);
				SetWindowText(h, (LPCTSTR)name);
			} else {
				// �v���O�����t�@�C���̂ݓo�^�\�ł��B
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

