# nats.scp.automation

Automating deployment and configuration through the REST API of Synadia Control Plane.

This is work in progress. Examples are designed to exmeplify SCP REST API usage and do nto necessarily represent best practices.

## HResources
* Run one of the example classes below
* See \config-templates for template configuration used in the corresponding examples
* Sub folder will contain sample output (configurations, logs, credentials)

## How to run

* See \java\src\main\java\com\synadia\automation for all examples
* To run an example go to the /java folder
    * Set SCP_LOG to you log location - default is `./`
    * Set SCP_URL to your SCP API URL  e.g. `http://172.23.129.153:8080/api/core/beta/`
    * set SCP_BEARER to you SCP access token e.g. `uat_mOywaPDfEgNUrd1eRqpxXo34LmRASe8Ze5mcBHV6rkRBUzEg9Vn1gf9k5xhUEZ04`
    * run "java  com.synadia.automation.CreateSystemCluster"
* All configuration is through environment variables - Check source or run and inspect log output
* SCP_LOG sets the log directory ( default is '.')

## Current examples

### Basic
* CreateSystemCluster
    * Create a new system
    * Create config files for a 3 node cluster
    
* CreateAccountUserCreds
    * Creates account in existing system
    * Create user
    * Download user credentials
    
* CreateSystemClusterAccountPreload
    * Create a new system and configures a 3 node cluster
    * Create account 
    * Create an additional account preload entry
    * Create config files for a 3 node cluster

### Complex

* CreateOperatorLeafCluster
    * Create a new system for a leaf cluster
    * Create accounts in leaf system to link to leaf remotes
    * Create users in existing accounts a hub systems
    * Download credentials
    * Create config files for the leaf cluster with remotes, credentials, preloaded accounts ready to run

    

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

