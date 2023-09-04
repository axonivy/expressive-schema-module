package io.github.axonivy.json.schema.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicRefs {

  private static final String START = "\\$\\{";
  private static final String END = "\\}";
  private static final Pattern PROPERTIES = Pattern.compile(START + "([^\\}]+)" + END);

  private final Map<String, String> properties = new HashMap<>();

  public void property(String key, String value) {
    this.properties.put(key, value);
  }

  public String resolve(String ref) {
    Matcher matcher = PROPERTIES.matcher(ref);
    var replaced = new StringBuilder();
    while(matcher.find()) {
      String property = matcher.group(1);
      String value = properties.get(property);
      if (value == null) {
        value = START+property+END;
      }
      matcher.appendReplacement(replaced, value);
    }
    matcher.appendTail(replaced);
    return replaced.toString();
  }
}