# nats.scp.automation

Automating deployment and configuration through the REST API of Synadia Control Plane.

This is work in progress. Examples are designed to exmeplify SCP REST API usage and do nto necessarily represent best practices.

## How use
* See \config-templates for template configuration used in the corresponding examples
* Sub folder will contain sample output (configurations, credentials)

## How to run
* See \java\src\main\java\com\synadia\automation for all examples
* All configuration is through environment variables - Check source or run and inspect log output
* SCP_LOG sets the log directory ( default is '.')

## Current examples
* CreateOperatorLeafCluster
    * Creates a new system for a leaf cluster
    * Creates accounts in leaf system to link to leaf remotes
    * Creates users in existing accounts a hub systems
    * Download credentials
    * Creates config files for the leaf cluster with remotes, credentials, preloaded accounts ready to run

    

## Dependencies
* JNATS
* org.json

````
<dependency>
    <groupId>io.nats</groupId>
    <artifactId>jnats</artifactId>
    <version>2.20.4</version>
</dependency>
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20240303</version>
</dependency>
````

