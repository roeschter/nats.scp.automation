package com.synadia.automation.impl;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/*
 *
 *
 */

public class SCPAPI implements Logger {

	public static String SCP_URL = "SCP_URL";
	public static String SCP_BEARER = "SCP_BEARER";

	public static String SCP_TEMPLATES = "SCP_TEMPLATES";
	public static String SCP_OUTPUT = "SCP_OUTPUT";
	public static String SCP_LOG = "SCP_LOG";

	public static String SCP_SYSTEM = "SCP_SYSTEM";
	public static String SCP_SYSTEM_NAME = "SCP_SYSTEM_NAME";
	public static String SCP_TEAM = "SCP_TEAM";
	public static String SCP_TEAM_NAME = "SCP_TEAM_NAME";
	public static String SCP_ACCOUNT = "SCP_ACCOUNT";
	public static String SCP_USER = "SCP_USER";

	static String ENC = "UTF-8";
	static String QUOTE = "\'";

	static boolean debug = false;

	int requestCounter = 0;
	String sessionPrefix;
	File scriptLog;

	String url;
	String bearer;
	HttpClient http;

	//last result
	JSON result;

	//Environment
	HashMap<String, String> env = new HashMap<String, String>();

	FileOutputStream logFile;
	boolean logReady = false;

	public SCPAPI()
	{
		env(SCP_LOG, ".");
		http = HttpClient.newHttpClient();

		File dir = new File(env( "SCP_LOG" ));
		dir.mkdirs();

		//<date>-nnn
		LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = currentDate.format(formatter);

        int count = 0;
        do {
        	count++;
        	sessionPrefix = formattedDate + "-" + String.format("%03d", count);
        	scriptLog = new File(env( "SCP_LOG" ) + "/" + sessionPrefix + "_log_script.txt");
        } while ( scriptLog.exists() );


		try {
			startLog();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//Repeat so we have it in the logs
		env(SCP_LOG, ".");

	}

	@Override
	public void log( String s ) {
		System.out.println( requestCounter + ": " + s );
	}

	public void log(String[] ss ) {
		String log ="";
		for( String s: ss )
			log += s + " ";
		log( log );
	}

	public  void log(ArrayList<String>  ss ) {
		String log ="";
		for( String s: ss )
			log += s + " ";
		log( log );
	}


	public String logJSON( String label, JSON json ) {
		String fName = sessionPrefix + "-R-"+requestCounter + "-" + label +".json";
		fName = fName.replace( "\"", "-");
		fName = fName.replace( "/", "-");
		File f = new File( env( "SCP_LOG" ) + "/" +fName );
		writeFile( f, json.get());
		json.setFileName(fName);
		json.setFilePath(f.getAbsolutePath());
		return f.getAbsolutePath();
	}

	public void writeFile( File file, String text ) throws RuntimeException
	{
		try  {
			file.getParentFile().mkdirs();
			FileOutputStream fo = new FileOutputStream(file);
			fo.write(text.getBytes(ENC));
			fo.close();
		} catch ( Exception e) {
			throw new RuntimeException(e);
		}
	}

	public  String readFile( File file) throws RuntimeException
	{
		String text = null;
		try {
			byte[] data; //=new byte[size];
			Path path = file.toPath();
			data = Files.readAllBytes( path );
			text = new String( data,ENC);
		} catch ( Exception e) {
			throw new RuntimeException(e);
		}
		return text;
	}

	public String readTemplate( String file )
	{
		String fName = env( "SCP_TEMPLATES" ) + file;
		logComment("Using template: " + fName );
		File f = new File(fName);
		String text = readFile(f);
		//Replace variables
		for ( Entry<String, String> entry: env.entrySet()) {
			String key = "$"+entry.getKey();
			if (debug) log( key + "=" + entry.getValue() );
			text = text.replace( key , entry.getValue());
		}

		return text;
	}

	public void outputFromTemplate( String outName, String template ) {
		String text = readTemplate(template);
		File f = new File( env( "SCP_OUTPUT" ) + "/"+outName );
		writeFile( f, text);
	}

	public void outputFile( String outName, String text ) {
		File f = new File( env( "SCP_OUTPUT" ) + "/"+outName );
		writeFile( f, text);
	}

	void startLog() throws FileNotFoundException
	{
		logFile = new FileOutputStream( scriptLog );
		logReady = true;
	}

	void closeLog() throws IOException
	{
		logFile.close();
	}

	@Override
	public void logScript( String s ) {
		if (!logReady)
			return;

		s += "\n";
		try {
			logFile.write( s.getBytes(ENC) );
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	@Override
	public void logComment( String s ) {
		logScript( "# " + s );
	}

	public void close() throws IOException {
		closeLog();
	}



	public String envSet( String var, String val )
	{
		env.put(var, val);
		log( var + "=" + val);
		logScript( "export " + var + "=" + val );
		return val;
	}

	public String env( String var )
	{
		String val = env.get(var);
		if ( val == null) {
			val = System.getenv(var);
			env.put(var, val);
			log( var + "=" + val);
		}

		return val;
	}

	public String[] envList( String var, String _default )
	{
		String raw = env( var, _default);
		String[] list = raw.split(",");
		for (int i=0; i<list.length; i++)
			list[i] = list[i].trim();
		return list;
	}

	public String env( String var, String _default )
	{
		String ret = env.get(var);
		if( ret == null ) {
			ret = System.getenv(var);
			if( ret == null ) {
				System.out.println( "Env: " + var + " not set. Using default" );
				ret = _default;
			}
			env.put(var, ret);
		}
		log( var + "=" + ret);
		logScript( "export " + var + "=" + ret );

		return ret;
	}



	public int envInt( String var, String default_ )
	{
		return Integer.parseInt(env(var, default_));
	}

	// COMMAND wrappers ---------------------------------------------------------------

	static String GETSYSTEM = "systems/$1";

	public JSON getSystem( String system )
	{
		return call( GETSYSTEM, system);
	}

	static String LISTACCOUNTS = "systems/$1/accounts";
	public JSON listAccounts(String system )
	{
		return call( LISTACCOUNTS, system);
	}

	static String CREATEACCOUNT = "systems/$1/accounts";
	public JSON createAccount( String system, String template )
	{
		JSON payload = new JSON();
		payload.setJson(readTemplate(template));
		return call( CREATEACCOUNT, system, null, payload );
	}

	static String LISTSKS = "accounts/$1/account-sk-groups";
	public JSON listAccountSigningKeyGroups(String account )
	{
		return call( LISTSKS, account);
	}


	static String LISTSYSTEMS = "teams/$1/systems";
	public JSON listSystems( String team )
	{
		return call( LISTSYSTEMS, team);
	}

	static String CREATESYSTEM = "teams/$1/systems";
	public JSON createSystem( String team, String template )
	{
		JSON payload = new JSON();
		payload.setJson(readTemplate(template));
		return call( CREATESYSTEM, team, null, payload );
	}

	static String LISTTEAMS = "teams";
	public JSON listTeams()
	{
		return call( LISTTEAMS, null);
	}

	static String LISTNATSUSERS = "accounts/$1/nats-users";
	public JSON listNATSUsers( String account)
	{
		return call( LISTNATSUSERS, account);
	}

	static String CREATENATSUSER = "accounts/$1/nats-users";
	public JSON createNATSUser( String account, String template )
	{
		JSON payload = new JSON();
		payload.setJson(readTemplate(template));
		return call( CREATENATSUSER, account, null, payload );
	}

	static String GETCREDS = "nats-users/$1/creds";
	public JSON getCreds( String user)
	{
		JSON payload = new JSON();
		payload.setData("");
		return call( GETCREDS, user, null, payload, "text/plain" );
	}


	// END COMMAND wrappers ---------------------------------------------------------------

	//CHeck if the key is en env variable
	public String map(String key)
	{
		if ( key == null )
			return null;
		String val = env.get(key);
		return (val==null)?key:val;
	}

	public JSON call( String cmd, String key) {
		return call( cmd, key, null, null);
	}

	public void logCurl(  String url, String bearer, JSON payload, String accept  ) {

		String out = "curl -X";
		out = out + ((payload != null && payload.hasData)?" POST":" GET");
		out = out + " " + QUOTE + url + QUOTE;
		logScript(out);
	}


	/*
	 *curl -X GET "http://172.23.129.153:8080/api/core/beta/accounts/2kFcQYuJCbsGSGrSrevoz7lhdUM/nats-users"
	 *  -H "Authorization:Bearer uat_sIGicijynZh87wIuK768UCTcUu9lyCgZpvAFM3Y3dK7sgytiHs6wRdLSf4Nbmx0u"

	 * -H "accept: application/json"\
 	   -H "content-type: application/json" \

 	   For POST
 	   -H "Content-Type: application/json" -d @data.json https://example.com/api/endpoint


	 */
	public JSON call( String cmd, String _key, HashMap<String, String> param, JSON payload) {
		return call( cmd, _key, param, payload, "application/json");
	}

	public JSON call( String cmd, String _key, HashMap<String, String> param, JSON payload, String accept ) {

		bearer = env.get(SCP_BEARER);
		if ( !bearer.startsWith("uat_") )
			throw new RuntimeException( "Bearer token invalid: Must start with 'uat_'" + bearer );

		url = env.get(SCP_URL);
		if ( !url.endsWith("/"))
			url += "/";

		String params = "";
		if ( param != null) {
			for ( Entry<String, String> entry: param.entrySet()) {
				if ( params.length() == 0)
					params = "?";
				params += entry.getKey() + "=" + entry.getValue();
			}
		}

		//Subtitute key
		if ( _key != null)
			cmd = cmd.replace("$1", _key);

		requestCounter++;
		String uri = url + cmd + params;


	    HttpRequest.Builder builder = HttpRequest.newBuilder()
	                .uri(URI.create(uri))
	                .header("Authorization", "Bearer " + bearer)
	                .header("accept", accept);


	    logComment("Request: " + requestCounter );
	    log( uri );
	    logCurl(uri, bearer,  payload , accept);

	    String payloadFIleName = null;
	    if ( payload != null && payload.hasData )
	    {
	    	builder.header("Content-Type", "application/json");
	    	builder.POST(BodyPublishers.ofString( payload.get() ) );

	    	log( payload.get() );
	    	payloadFIleName = logJSON( "Post-" + cmd, payload );
	    }

	    HttpRequest request =builder.build();

	    HttpResponse<String> response;
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString());
		} catch ( IOException | InterruptedException e) {

			e.printStackTrace();
			throw new RuntimeException("HTTP failure", e);
		}


        log( "Response Code: " + response.statusCode());
        logComment( "Response Code: " + response.statusCode());

        JSON result = new JSON(this);
        result.setJson(response.body());
        result.setURI(uri);
        log( result.get());
        logJSON( "Result-" + cmd, result);

        this.result = result;

	    return result;
	}


}
