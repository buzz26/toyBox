SDLoader original
	google code http://code.google.com/p/sdloader/

SDLoaderDesktopSWT
	JENKINS_HOME set BOOT_ROOT/.jenkins version
	swt 3.7-M7.jar use SWT.WEBKIT apply version

	(dependency SDLoader.jar)

----------------------------------

update note
	SWT.WEBKIT apply
	startupHook/shutdownHook args apply
	file.encoding,roovy.source.encoding UTF-8 setting
	location running error fix Jenkins.war running


groovy-all-1.8.0.jar(modify by @nagai_masato)
	#groovy 1.8.0 with an option to ignore "final" modifier http://t.co/8YGrHSW 
	ex) groovy -Dignore=final Foo.groovy
