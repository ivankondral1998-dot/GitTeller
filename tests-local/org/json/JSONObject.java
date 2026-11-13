package org.json;
import java.util.*;
public class JSONObject {
  private final Map<String,Object> values=new HashMap<>();
  public JSONObject put(String key, Object o){values.put(key,o);return this;}
  public String optString(String key){return values.get(key)==null?"":values.get(key).toString();}
  public boolean optBoolean(String key,boolean def){Object o=values.get(key);return o instanceof Boolean?(Boolean)o:def;}
  public int optInt(String key){Object o=values.get(key);return o instanceof Number?((Number)o).intValue():0;}
  public JSONArray optJSONArray(String key){Object o=values.get(key);return o instanceof JSONArray?(JSONArray)o:null;}
  public JSONObject optJSONObject(String key){Object o=values.get(key);return o instanceof JSONObject?(JSONObject)o:null;}
}
