package com.synadia.automation;


import static com.synadia.automation.impl.SCPAPI.*;

import com.synadia.automation.impl.JSON;
import com.synadia.automation.impl.SCPAPI;


public class CreateSystemClusterAccountPreload {

	public static void main ( String arg[] ) throws Exception
	{
		SCPAPI api = new SCPAPI();

		api.env(SCP_URL, "URL to the SCP REST API - e.g. http://172.23.129.153:8080/api/core/beta/");
		api.env(SCP_BEARER, "Create Acess token in SCP in your personal profile section.");

		api.env(SCP_TEAM_NAME, "Default");
		api.env(SCP_SYSTEM_NAME, "CLUSTER02");
		api.env(SCP_ACCOUNT_NAME, "ACME");

		api.env(SCP_TEMPLATES, "..\\config-templates\\"+ api.getExampleName() +"\\");
		api.env(SCP_OUTPUT,  api.env(SCP_TEMPLATES) + api.env("SCP_SYSTEM_NAME") );


		String[] PORT = api.envList("SCP_PORTS", "9201, 9202, 9203");
		String[] ROUTEPORT = api.envList("SCP_ROUTEPORTS", "9211, 9212, 9213");
		String[] SERVER_NAME = api.envList( "SCP_SERVER_NAMES", "NODE1, NODE2, NODE3" );
		String[] SERVER_HOST_NAME = api.envList( "SERVER_HOST_NAMES", "172.23.128.1, 172.23.128.1, 172.23.128.1" );

		//----------------------------------
		api.logComment("DONE INITIALIZING - Set Env variables above to configure");
		//----------------------------------


		//-------------------------------------------------
		api.logComment("Find the named team");
		JSON teams = api.listTeams();
		JSON team = teams.getItemByKey( "name", api.env("SCP_TEAM_NAME"));
		api.envSet(SCP_TEAM, team.get("id") );


		//-------------------------------------------------
		api.logComment("Find or create system");
		JSON systems = api.listSystems( api.env(SCP_TEAM) );
		JSON system = systems.getItemByKey( "name", api.env("SCP_SYSTEM_NAME"));

		if ( !system.hasData) {

			api.logComment( api.env("SCP_SYSTEM_NAME") +": System not found - Creating new System" );
			api.envSet("URL", api.buildClusterURL(SERVER_HOST_NAME, PORT) );
			api.envSet("NAME", api.env("SCP_SYSTEM_NAME") );
			system = api.createSystem( api.env(SCP_TEAM), "create_system_template.json");
		} else {
			api.logComment("System already exsist, reading details");
		}
		api.envSet( SCP_SYSTEM, system.get( "id" ));
		api.envSet( "system_account_jwt",  system.get( "system_account_jwt" ) );
		api.envSet( "operator_jwt",  system.get( "operator_jwt" ) );
		api.envSet( "system_account_key", system.get( "operator_claims", "nats", "system_account" ) );


		//-------------------------------------------------
		api.logComment("Find or create account");
		JSON accounts = api.listAccounts( api.env(SCP_SYSTEM));

		JSON account = accounts.getItemByKey( "name", api.env(SCP_ACCOUNT_NAME) );

		if ( !account.hasData ) {

			api.logComment("Account "+ api.env(SCP_ACCOUNT_NAME) + " not found. Creating.");
			api.envSet( "NAME", api.env(SCP_ACCOUNT_NAME) );

			account = api.createAccount( api.env(SCP_SYSTEM), "create_account_template.json" );
		}

		api.envSet(SCP_ACCOUNT, account.get("id"));

		//-------------------------------------------------
		api.logComment("Create account preload - using acocunt public key and JWT");

		String preload = account.get("account_public_key") + ": " +  account.get("jwt") + "\n";
		api.envSet("PRELOAD", preload);


		//-------------------------------------------------
		api.logComment("Create cluster config from template for each server");

		api.envSet("ROUTES", api.buildClusterURL(SERVER_HOST_NAME, ROUTEPORT) );
		for ( int i=0; i<SERVER_NAME.length; i++)
		{
			api.envSet("SERVER_NAME", SERVER_NAME[i]);
			api.envSet("SERVER_HOST_NAME", SERVER_HOST_NAME[i]);
			api.envSet("CLUSTER", api.env( SCP_SYSTEM_NAME));
			api.envSet("PORT", PORT[i]);
			api.envSet("RPORT", ROUTEPORT[i]);

			api.outputFromTemplate( SERVER_NAME[i] + ".conf" , "node_template.conf" );
		}

		api.close();
	}
}
