package org.json;
import java.util.*;
public class JSONObject {
  public JSONObject(){}
  public JSONObject(String json){throw new UnsupportedOperationException("parser unavailable in local doubles");}
  private final Map<String,Object> values=new HashMap<>();
  public JSONObject put(String key, Object o){values.put(key,o);return this;}
  public boolean has(String key){return values.containsKey(key);}
  public String optString(String key,String def){String v=optString(key);return v.isEmpty()?def:v;}
  public int optInt(String key,int def){return values.containsKey(key)?optInt(key):def;}
  public String optString(String key){return values.get(key)==null?"":values.get(key).toString();}
  public boolean optBoolean(String key,boolean def){Object o=values.get(key);return o instanceof Boolean?(Boolean)o:def;}
  public int optInt(String key){Object o=values.get(key);return o instanceof Number?((Number)o).intValue():0;}
  public long optLong(String key, long def){Object o=values.get(key);return o instanceof Number?((Number)o).longValue():def;}
  public JSONArray optJSONArray(String key){Object o=values.get(key);return o instanceof JSONArray?(JSONArray)o:null;}
  public JSONObject optJSONObject(String key){Object o=values.get(key);return o instanceof JSONObject?(JSONObject)o:null;}
}
