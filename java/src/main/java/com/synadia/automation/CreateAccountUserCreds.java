package com.synadia.automation;


import static com.synadia.automation.impl.SCPAPI.*;

import com.synadia.automation.impl.JSON;
import com.synadia.automation.impl.SCPAPI;


public class CreateAccountUserCreds {

	public static void main ( String arg[] ) throws Exception
	{
		SCPAPI api = new SCPAPI();

		api.env(SCP_URL, "URL to the SCP REST API - e.g. http://172.23.129.153:8080/api/core/beta/");
		api.env(SCP_BEARER, "Create Acess token in SCP in your personal profile section.");

		api.env(SCP_TEAM_NAME, "Default");
		api.env(SCP_SYSTEM_NAME, "CLUSTER01");
		api.env(SCP_ACCOUNT_NAME, "ACME");
		api.env(SCP_USER_NAME, "ACME-USER");

		api.env(SCP_TEMPLATES, "..\\config-templates\\"+ api.getExampleName() +"\\");
		api.env(SCP_OUTPUT,  api.env(SCP_TEMPLATES) + api.env("SCP_SYSTEM_NAME") );

		//----------------------------------
		api.logComment("DONE INITIALIZING - Set Env variables above to configure");
		//----------------------------------


		//-------------------------------------------------
		api.logComment("Find the named team");
		JSON teams = api.listTeams();
		JSON team = teams.getItemByKey( "name", api.env("SCP_TEAM_NAME"));
		api.envSet(SCP_TEAM, team.get("id") );


		//-------------------------------------------------
		api.logComment("Find the named system");
		JSON systems = api.listSystems( api.env(SCP_TEAM) );
		JSON system = systems.getItemByKey( "name", api.env("SCP_SYSTEM_NAME"));

		if ( !system.hasData) {

			api.logComment( api.env("SCP_SYSTEM_NAME") +": System not found." );
			System.exit(-1);
		}
		api.envSet( SCP_SYSTEM, system.get( "id" ));


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
		api.logComment("Find or create users  in account.");

		JSON users = api.listNATSUsers( api.env(  SCP_ACCOUNT ));

		JSON user = users.getItemByKey("name", api.env(SCP_USER_NAME));

		if ( !user.hasData) {

			api.logComment("User: " + api.env(SCP_USER_NAME) + " not found. Creating." );
			api.logComment("Get Default Signing Key Group (SKS)");
			JSON sksList = api.listAccountSigningKeyGroups( api.env( SCP_ACCOUNT ));
			JSON sks = sksList.getItemByKey( "name", "Default");

			api.envSet("SKSID", sks.get("id"));
			api.envSet("NAME",  api.env(SCP_USER_NAME) );

			api.logComment("Create user");
			user = api.createNATSUser(  api.env( SCP_ACCOUNT ), "create_nats_user_template.json");
		}
		api.envSet( SCP_USER, user.get("id"));


		//-------------------------------------------------
		api.logComment("Export user credentials");
		JSON creds = api.getCreds( api.env(SCP_USER) );
		String credsName = api.env(SCP_SYSTEM_NAME) + "-" + api.env(SCP_ACCOUNT_NAME)+ "-" + api.env(SCP_USER_NAME) + ".creds";
		api.outputFile( credsName , creds.get() );

		api.close();
	}
}
