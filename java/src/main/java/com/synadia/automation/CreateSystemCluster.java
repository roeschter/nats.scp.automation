package com.synadia.automation;


import static com.synadia.automation.impl.SCPAPI.*;

import com.synadia.automation.impl.JSON;
import com.synadia.automation.impl.SCPAPI;


public class CreateSystemCluster {

	public static void main ( String arg[] ) throws Exception
	{
		SCPAPI api = new SCPAPI();

		api.env(SCP_URL, "URL to the SCP REST API - e.g. http://172.23.129.153:8080/api/core/beta/");
		api.env(SCP_BEARER, "Create Acess token in SCP in your personal profile section.");

		api.env(SCP_TEAM_NAME, "Default");
		api.env(SCP_SYSTEM_NAME, "CLUSTER01");

		api.env(SCP_TEMPLATES, "..\\config-templates\\CreateSystemCluster\\");
		api.env(SCP_OUTPUT,  api.env(SCP_TEMPLATES) + api.env("SCP_SYSTEM_NAME") );


		String[] PORT = api.envList("SCP_PORTS", "9101, 9102, 9103");
		String[] ROUTEPORT = api.envList("SCP_ROUTEPORTS", "9111, 9112, 9113");
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
		api.logComment("Create leaf config from template for each server");

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
