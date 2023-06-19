Teamcity MS Teams Build Notifier
====================

## Overview

Posts Build Status to [MsTeams](http://teams.microsoft.com).  This plugin is based on the [tcSlackBuildNotifier](https://github.com/PeteGoo/tcSlackBuildNotifier) plugin.

![Sample Notification](https://raw.github.com/spyder007/teamcity-msteams-notifier/master/docs/build-status_pass.png)
![Sample Notification](https://raw.github.com/spyder007/teamcity-msteams-notifier/master/docs/build-status_fail.png)

_Tested on TeamCity 2019.1 (build 65998)_

## Installation

Make sure you have local installations of: (see Development below for more information)
- JDK 8
- Maven 3

From the base of this repo, run the command:
`mvn package`

This will create a ZIP file in .\tcmsteamsbuildnotifier-webi-ui\target\
Copy this ZIP file to the data directory of your TeamCity installation, under \plugins.
You will need to restart the TeamCity service before it can load the plugin.

## Configuration

Once you have installed the plugin and restarted head on over to the Admin page and configure your MsTeams settings.

![Admin Page Configuration](https://raw.github.com/spyder007/teamcity-msteams-notifier/master/docs/AdminPageBig.png)

To configure an incoming webhook for a channel, go to the **Connectors** section for the channel and configure an **Incoming Webhook** connector.  Then, copy the resulting URL and paste it into the **Webhook URL** field.  You will need a different Webhook URL for each channel.

## Usage

From the MsTeams tab on the Project or Build Configuration page, add a new MsTeams Notification, and you're away!

![Sample Build Configuration](https://raw.github.com/spyder007/teamcity-msteams-notifier/master/docs/build-msteams-config.png)

## Development

### Required Software

1. Install JDK 8
   - Download JDK8 for Windows from https://adoptium.net/en-GB/temurin/releases/?version=8
   - Unless you are using another JDK on your machine, set the environment variable JAVA_HOME to the installation path:
       - JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.372.7-hotspot\
   - and add the bin folder to your PATH:
       - PATH=%PATH%;%JAVA_HOME%\bin

2. Install Maven
   - If you have IntelliJ, you can use the bundled Maven.
   - Otherwise, install Maven 3 from https://maven.apache.org/download.cgi
   - Add the Maven bin folder to your PATH

3. Install TeamCity
   - Download the TeamCity .exe version 2023.05 from https://www.jetbrains.com/teamcity/download
   - Install to the default location (C:\TeamCity).
   - Make sure env.TEAMCITY_JRE is set to the JAVA_HOME of your JDK 8 installation.

4. Configure IntelliJ (Optional)
   - Under File -> Project Structure, make sure to set SDK = JDK8 (add a new JDK with your installation path.)
   - Under File -> Settings -> Plugins, search for the Lombok plugin and install it.
   - Under File -> Build, Execution, Deployment -> Compiler -> Annotation Processors, enable annotation processing.

### Building

Issue 'mvn package' command from the root project to build the plugin.
The resulting package <artifactId>.zip will be placed in 'target' directory.

To test the plugin locally, run:
- `mvn tc-sdk:stop`
- `mvn tc-sdk:start`

Browse to http://localhost:8111/
- Accept the License agreement and configure the administrator account.
- Go to the Administration page: http://localhost:8111/admin/admin.html?item=plugins
- Under "External Plugins", the eidos-teamcity-plugin should be loaded.
- Go to http://localhost:8111/admin/admin.html?item=msTeamsNotifications
- Configure the webhook URL, etc.
