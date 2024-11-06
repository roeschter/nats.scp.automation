package com.synadia.automation;


import static com.synadia.automation.SCPAPI.*;

import java.io.File;
import java.util.ArrayList;

public class CreateOperatorLeafCluster {

	public static void main ( String arg[] ) throws Exception
	{
		SCPAPI api = new SCPAPI();

		api.env(SCP_URL, "URL to the SCP REST API - e.g. http://172.23.129.153:8080/api/core/beta/");
		api.env(SCP_BEARER, "Create Acess token in SCP in your personal profile section.");

		api.env("SCP_LEAF_NAME", "LEAF2");
		api.env(SCP_TEAM_NAME, "Default");
		api.env(SCP_SYSTEM_NAME, "C10_CP");

		api.env(SCP_TEMPLATES, "..\\config-templates\\CreateOperatorLeafCluster\\");
		api.env(SCP_OUTPUT,  api.env(SCP_TEMPLATES) + api.env("SCP_LEAF_NAME") );

		//List of account to link - hub and leaf
		String[] hubAccounts = api.envList("SCP_HUB_ACCOUNTS", "hAccountA,hAccountB");
		String[] leafAccounts = api.envList("SCP_LEAF_ACCOUNTS", "lAccountA,lAccountB");
		String[] leafCreds = new String[ leafAccounts.length];

		String[] PORT = api.envList("SCP_PORTS", "4301,4302,4303");
		String[] ROUTEPORT = api.envList("SCP_ROUTEPORTS", "7711,7712,7713");

		api.logComment("DONE INITIALIZING - Set Env variables above to configure");


		api.logComment("Find the team");
		api.listTeams();
		JSON team = api.result.getItemByKey( "name", api.env("SCP_TEAM_NAME"));
		api.envSet(SCP_TEAM, team.get("id") );


		api.logComment("List systems and extract operator and accounts.");
		JSON systems = api.listSystems( api.env(SCP_TEAM) );
		JSON system = systems.getItemByKey( "name", api.env("SCP_LEAF_NAME"));

		if ( !system.hasData) {
			//Create System if not exists
			api.logComment( api.env("SCP_LEAF_NAME") +": System not found - Creating new System" );

			//api.log( "Exiting: Not yet implemented" );
			//System.exit(-1);
			api.envSet("URL", "nats://172.23.128.1:4301" );
			api.envSet("NAME", api.env("SCP_LEAF_NAME") );
			system = api.createSystem( api.env(SCP_TEAM), "create_system_template.json");
		}


		api.envSet( SCP_SYSTEM, system.get( "id" ));
		api.envSet( "system_account_jwt",  system.get( "system_account_jwt" ) );
		api.envSet( "operator_jwt",  system.get( "operator_jwt" ) );
		api.envSet( "system_account_key", system.get( "operator_claims", "nats", "system_account" ) );


		api.logComment("Find or create users on hub system in account to be linked to remote system");
		JSON hubSystem = systems.getItemByKey( "name", api.env(SCP_SYSTEM_NAME));
		JSON accounts = api.listAccounts( hubSystem.get("id") );
		//Iterate expected accounts
		for ( int i=0; i<hubAccounts.length; i++  )
		{
			//Find by name
			JSON account = accounts.getItemByKey( "name", hubAccounts[i]);
			//Try to find user ny name
			String accountId = account.get("id");

			JSON users = api.listNATSUsers(accountId);
			String userName = "CONNECT-" + api.env("SCP_LEAF_NAME");
			JSON user = users.getItemByKey("name", userName );

			//If not exist create
			if ( !user.hasData) {
				//Gte default account SKS
				api.listAccountSigningKeyGroups(accountId);
				JSON sks = api.result.getItemByKey( "name", "Default");

				api.envSet("SKSID", sks.get("id"));
				api.envSet("NAME", userName);

				api.logComment("Create user");
				user = api.createNATSUser( accountId, "create_nats_user_template.json");
			}
			api.logComment("Export credentials");
			api.getCreds(user.get("id"));
			leafCreds[i] = hubAccounts[i] + "-" + userName + ".creds";
			api.outputFile( leafCreds[i] , api.result.get() );


		}

		api.logComment("Find or create accounts on leaf system");
		accounts = api.listAccounts( api.env(SCP_SYSTEM));
		String preload = "";
		String remotes = "";
		for ( int i=0; i<leafAccounts.length; i++  )
		{
			String acc = leafAccounts[i];
			//Check
			JSON account = accounts.getItemByKey( "name", acc);
			String key;
			String jwt;
			if ( account.hasData ) {
				key = account.get("account_public_key");
				jwt = account.get("jwt");
			} else {

				api.envSet( "NAME", acc);
				api.createAccount( api.env(SCP_SYSTEM), "create_account_template.json" );
				key = api.result.get("account_public_key");
				jwt = api.result.get("jwt");

			}

			preload += key + ": " + jwt + "\n";

			api.envSet("ACCOUNT", key);
			api.envSet("CREDS", leafCreds[i]);
			remotes += api.readTemplate("remotes_template.conf");
		}
		api.envSet("PRELOAD", preload);
		api.envSet("REMOTES", remotes);

		//List of leaf node names
		String leaf = api.env("SCP_LEAF_NAME" );
		String[] SERVER_NAME = new String[] { leaf+"_NODE1",  leaf+"_NODE2",  leaf+"_NODE3",};

		api.logComment("Create leaf config from template for each server");
		for ( int i=0; i<SERVER_NAME.length; i++)
		{
			api.envSet("SERVER_NAME", SERVER_NAME[i]);
			api.envSet("CLUSTER", api.env( "SCP_LEAF_NAME"));
			api.envSet("PORT", PORT[i]);
			api.envSet("RPORT", ROUTEPORT[i]);

			api.outputFromTemplate( SERVER_NAME[i] + ".conf" , "leaf_template.conf" );
		}

		api.close();
	}
}
