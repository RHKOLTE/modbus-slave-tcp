set JAVA_HOME=E:\Work\java\sapjvm_8_Win64
set PATH=%JAVA_HOME%\bin
set JARS=.;bin;config
for %%f in (lib\*.jar) do (
call :add_jar %%f
)

echo %JARS%
%JAVA_HOME%\bin\java -classpath %JARS% com.ksh.modbus.SlaveExample
exit /b
:add_jar
set JARS=%JARS%;%1
exit /b
