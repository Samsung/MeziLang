# Building project

## Installing JDK & Eclipse
1. Installing JDK over version 1.8 (Remove older JDKs)
2. Installing Eclipse over Luna


## Installing Java CC plugin in Eclipse

1. Eclipse > Help > Eclipse Marketplace > Findindg "JavaCC"
2. Installing JavaCC Eclipse Plug-in 1.5.33
3. Selecting license agreement and selecting Finish
4. JavaCC becomes installed and eclipse restarts

## Maven setting under proxy network environment

If you are working under proxy network environment.

1. Create a maven setting (ex. maven_setting.xml )

```xml
    <settings>
     <proxies>
      <proxy>
       <id>http</id>
       <active>true</active>
       <protocol>http</protocol>
       <host>{PROXY IP ADDRESS}</host>
       <port>8080</port>
       <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
      </proxy>
      <proxy>
       <id>https</id>
       <active>true</active>
       <protocol>https</protocol>
       <host>{PROXY IP ADDRESS}</host>
       <port>8080</port>
       <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
      </proxy>
     </proxies>
     <activeProfiles>
      <!--make the profile active all the time -->
      <activeProfile>securecentral</activeProfile>
     </activeProfiles>
     <profiles>
      <profile>
        <id>securecentral</id>
        <!--Override the repository (and pluginRepository) "central" from the
        Maven Super POM -->
        <repositories>
         <repository>
          <id>central</id>
          <url>http://repo1.maven.org/maven2</url>
          <releases>
           <enabled>true</enabled>
          </releases>
         </repository>
        </repositories>
        <pluginRepositories>
         <pluginRepository>
          <id>central</id>
          <name>Maven Plugin Repository</name>
          <url>http://repo1.maven.org/maven2</url>
          <layout>default</layout>
          <snapshots>
           <enabled>false</enabled>
          </snapshots>
          <releases>
           <updatePolicy>never</updatePolicy>
          </releases>
         </pluginRepository>
        </pluginRepositories>
       </profile>
      </profiles>
     </settings>
```

2. Eclipse > Window > Preference > Maven > User Settings

   * Changing 'User Settings' path to the path of maven setting file of step 1
   * 'Update Settings' > Apply > Ok


## Importing github project

In Eclipse,
1. File > Import > Select 'Git' > Select 'Projects from Git', and do 'Next'
2. Select 'Clone URI', and do 'Next'
2. Input 'https://github.com/Samsung/MeziLang.git' in URI field
3. Input your Github User & Password and do 'Next'
4. Select 'master' branch, and do 'Next'
5. Configure you local destination path, and do 'Next'
6. Select 'Import existing Eclipse projects', and do 'Next', and do 'Finish'.

Then mezi-compiler project will be created in Eclipse.


## Configuring Java CC option

1. Selecting Package Explorer > mezi-compiler > Right Click > Properties > JavaCC options 
2. Select Global options tab
 * Configure 'Set the javaCC jar file' to {your eclipse}\plugins\sf.eclipse.javacc_1.5.33\jars\javacc-5.0.jar
 * Configure 'Set the javaCC jar file' to {your eclipse}\plugins\sf.eclipse.javacc_1.5.33\jars\jtb-1.4.11.jar
3. Do 'Apply' and 'Ok'


## Parser code generation

1. Open mezi-compiler/src/main/java/mezic/parser/Parser.jjt in the Package Explorer.
2. Right click > Selecting 'Compile with javacc | jjtree | jtb'
   - Then mezic parser code are generated in the src/main/java/mezic/parser package by the JavaCC

If you meet following error, then remove older JDK versions.
>Error: Registry key 'Software\JavaSoft\Java Runtime Environment'\CurrentVersion'
>has value '1.8', but '1.7' is required.
>Error: could not find java.dll
>Error: Could not find Java SE Runtime Environment.

## Building Maven Project

1. Select 'pom.xml' > Right Click > Run As > Maven clean

```
[INFO] ------------------------------------------------------------------------
[INFO] Building mezi-compiler 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ mezi-compiler ---
[INFO] Deleting {Project Path}\MeziLang\target
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 0.539 s
[INFO] Finished at: 2017-04-29T11:26:25+09:00
[INFO] Final Memory: 9M/184M
[INFO] ------------------------------------------------------------------------
```

2. Select 'pom.xml' > Right Click > Run As > Maven build
If it is your first build it will show 'Edit configuration and lanuch' dialog.
In 'Edit configuration and lanuch' dialog, input 'install' in 'Goals' field > 'Apply' button > 'Run' button

```
[INFO] ------------------------------------------------------------------------
[INFO] Building mezi-compiler 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
      ....
[INFO] --- maven-install-plugin:2.4:install (default-install) @ mezi-compiler ---
[INFO] Installing 
      ....
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 7.821 s
[INFO] Finished at: 2017-04-29T11:30:07+09:00
[INFO] Final Memory: 20M/369M
[INFO] ------------------------------------------------------------------------

```

If it failed to build with the following message, configure maven build JRE with the JRE inside of JDK.

```
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[INFO] -------------------------------------------------------------
[ERROR] No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?
[INFO] 1 error
[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 1.254 s
[INFO] Finished at: 2017-04-29T10:23:25+09:00
[INFO] Final Memory: 9M/232M
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.1:compile (default-compile) on project mezi-compiler: Compilation failure
[ERROR] No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?
```

Configuring 'Runtime JRE' as 'JDK-JRE' instead of default JRE.

1. Run > Run Configuration > Select 'Maven Build' > Select 'MeziLang' > Select 'JRE' tab.

2. Select 'Alternate JRE' and select JRE inside of JDK and do 'Apply', and do 'Run'

3. If you cannot select 'JRE inside of JDK', then you should add. Select 'Intalled JREs' and 'Add' > 'Standard VM' and add 'JRE inside of JDK' (For example 'C:\Program Files\Java\jre1.8.0_121')


## Helloworld compilation, and execution   

1. Open code_base/example/e000_helloworld.mz

```java
    class Hello
    {
    	static fn main( args: String[] )
    	{
    		System.out.println("Hello World!!")
    	}
    }
```

2. Open src/main/java/mezic/test/CompileHelloworld.java in eclipse editor
  - Configure proper java runtime jar in compiler class paths configuration

```java
        Compiler runner = new Compiler.Builder()
        		.setPrintout(System.out)
        		.setSourcebase("code_base/")
        		.setTargetbase("path/")
                .setClasspathes(new String[] {
                		"path/",
                		"{JRE Home Path}/lib/rt.jar",
                		"target/classes/",
                		})
                .setDfltImportpkgs(new String[] {
                		"java/lang",
                		}).build();
```

for example,

```java
                .setClasspathes(new String[] {
                		"path/",
                		"C:/Program Files/Java/jdk1.8.0_121/jre/lib/rt.jar",
                		"target/classes/",
                		})
```

  - Run > Run As > Java Application		

```
    deleting path/example (18 files)
    Parsing(example.e000_helloworld) is completed
    Compilation is completed
    Hello World!!
```

or

  - Select Package Explorer > path > example > Hello.class
    (If you cannot see the class file, press F5 for refresh)

  - Right click > Run As > Java Application

```
    Hello World!!
```
