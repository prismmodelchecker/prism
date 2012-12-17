@echo off

xcopy ..\src\java\lpsolve\*.class lpsolve\ /y
rem jar -c -f ..\lib\lpsolve55j.jar *.class lpsolve
jar cvf ..\lib\lpsolve55j.jar *.class lpsolve

javac -classpath ..\lib\lpsolve55j.jar Demo.java
rem jar -c -f Demo.jar Demo.class
jar cvf Demo.jar Demo.class
rem following shows contents of jar file.
rem rem jar -t -f Demo.jar
rem jar tf Demo.jar

javac -classpath ..\lib\lpsolve55j.jar;..\lib\junit.jar LpSolveTest.java
rem jar -c -f unittests.jar LpSolveTest.class LpSolveTest$1MyListener.class LpSolveTest$2MyListener.class LpSolveTest$3MyListener.class
jar cvf unittests.jar LpSolveTest.class LpSolveTest$1MyListener.class LpSolveTest$2MyListener.class LpSolveTest$3MyListener.class

