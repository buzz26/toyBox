::SVN Repo Commit
set GIT_HOME=D:\Tool\Git
set PATH=%PATH%;%GIT_HOME%/bin

pushd <<ProjectName>>
git commit -a -m "%1 "
git svn dcommit
popd

pause