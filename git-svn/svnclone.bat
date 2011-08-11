::SVN Repo Clone
set GIT_HOME=D:\Tool\Git
::set PATH=%PATH%;%GIT_HOME%/bin
set PATH=%GIT_HOME%/bin
git svn clone ^
--trunk=<<SVN-URL>> ^
--username=<<USER_NAME>> ^
<<ProjectName>>

git config --system core.autocrlf false
