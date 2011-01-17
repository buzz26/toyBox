::SVN Repo Commit
set GIT_HOME=D:\Tool\Git
set PATH=%PATH%;%GIT_HOME%/bin

pushd <<ProjectName>>
git svn dcommit
popd

pause