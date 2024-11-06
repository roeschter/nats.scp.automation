package com.synadia.automation;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSON {
	public boolean hasData = false;
	public boolean isValid = false;
	public boolean isModified = false;

	private String jsonRaw = null;
	private JSONObject json = null;

	private String uri = null;
	private String fileName = null;
	private String filePath = null;
	private Logger logger= null;

	public JSON() {
		jsonRaw = "{}";
		json = new JSONObject( jsonRaw );
		isValid = true;
		hasData = false;
	}

	public JSON( Logger logger) {
		this();
		this.logger = logger;
	}

	public void setData( String raw ) {
		jsonRaw = raw;
		isValid = false;
		hasData = true;
	}


	public void setJson( String raw )
	{
		jsonRaw = raw;
		try {
			json = new JSONObject( raw );
			isValid = true;
			isModified = true;
			hasData = true;
		} catch (Exception e)
		{
			isValid = false;
			hasData = true;
		}
	}


	public void setJSONObject( JSONObject json )
	{
		this.json = json;
		isModified = true;
		hasData = true;
	}

	public void setURI( String uri ) {
		this.uri = uri;
	}

	public void setFileName( String fName ) {
		fileName = fName;
	}

	public void setFilePath( String fPath ) {
		filePath = fPath;
	}

	public void setLogger( Logger logger ) {
		this.logger = logger;
	}

	public String log( String s, String path)
	{
		if ( logger != null) {
			String log = "Extracting: " + fileName + "#" + path;
			logger.log(log);
			logger.logComment(log);
		}
		return s;
	}

	public String get(String key1 ) {
		return log(json.getString(key1), key1 );
	}

	public String get(String key1, String key2 ) {
		return log(json.getJSONObject(key1).getString(key2), key1+"/"+key2);
	}

	public String get(String key1, String key2, String key3 ) {
		return log(json.getJSONObject(key1).getJSONObject(key2).getString(key3), key1+"/"+key2 + "/" + key3);
	}

	public String get(String list, int idx, String key1 ) {
		JSONArray items = json.getJSONArray(list);
		return log(items.getJSONObject(idx).getString(key1), list+"["+idx+ "]" + key1);
	}

	public String get(String list, int idx, String key1, String key2, String key3 ) {
		JSONArray items = json.getJSONArray(list);
		return log(items.getJSONObject(idx).getJSONObject(key1).getJSONObject(key2).getString(key3), list+"["+idx+ "]" + key1+"/"+key2+"/"+key3);
	}


	public JSON getItemByKey( String key, String val ) {
		JSON ret = new JSON(logger);
		ret.setURI( uri + "/items/"+key+"="+val );
		JSONArray list = json.getJSONArray("items");
		int len = list.length();
		for( int i=0; i<len; i++) {
			JSONObject entry =  list.getJSONObject(i);
			if ( entry.getString(key).equals(val) ) {
				ret.setJSONObject(entry);
			}
		}
		return ret;
	}

	public ArrayList<String>  list( String key1, String key2) {
		JSONArray list = json.getJSONArray(key1);
		int len = list.length();
		ArrayList<String> ret = new ArrayList<String>();

		for( int i=0; i<len; i++) {
			ret.add( list.getJSONObject(i).getString(key2) );
		}

		return ret;
	}

	public String get() {
		if (isModified && isValid ) {
			jsonRaw = json.toString(4);
			isModified = false;
		}

		return jsonRaw;
	}



}