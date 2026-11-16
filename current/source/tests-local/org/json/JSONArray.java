package org.json;
import java.util.*;
public class JSONArray {
  public JSONArray(){}
  public JSONArray(String json){throw new UnsupportedOperationException("parser unavailable in local doubles");}
  private final List<Object> values=new ArrayList<>();
  public JSONArray put(Object o){values.add(o);return this;}
  public Object get(int i){return values.get(i);}
  public String optString(int i){return i>=0&&i<values.size()?String.valueOf(values.get(i)):"";}
  public int length(){return values.size();}
  public JSONObject optJSONObject(int i){return i>=0&&i<values.size()&&values.get(i) instanceof JSONObject?(JSONObject)values.get(i):null;}
}
