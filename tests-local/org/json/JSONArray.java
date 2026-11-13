package org.json;
import java.util.*;
public class JSONArray {
  private final List<Object> values=new ArrayList<>();
  public JSONArray put(Object o){values.add(o);return this;}
  public int length(){return values.size();}
  public JSONObject optJSONObject(int i){return i>=0&&i<values.size()&&values.get(i) instanceof JSONObject?(JSONObject)values.get(i):null;}
}
