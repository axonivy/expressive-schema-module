package io.github.axonivy.json.schema.annotations;

import java.util.List;

public interface PropertyContributor {

  List<Prop> contribute();

  public static record Prop(
      String name,
      Class<?> type,
      String description,
      Object defaultValue,
      List<String> examples) {

    public static Prop string(String name) {
      return new Prop(name, String.class, null, null, List.of());
    }

    public static Prop bool(String name) {
      return new Prop(name, Boolean.class, null, null, List.of());
    }

    public static Prop integer(String name) {
      return new Prop(name, Integer.class, null, null, List.of());
    }

    public Prop description(String desc) {
      return new Prop(name, type, desc, defaultValue, examples);
    }

    public Prop defaultValue(Object value) {
      return new Prop(name, type, description, value, examples);
    }

    public Prop examples(List<String> ex) {
      return new Prop(name, type, description, defaultValue, ex);
    }
  }

}
