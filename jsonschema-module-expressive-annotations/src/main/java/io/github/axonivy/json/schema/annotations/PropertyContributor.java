package io.github.axonivy.json.schema.annotations;

import java.util.List;

public interface PropertyContributor {

  List<Prop> contribute();

  public static record Prop(
      String name,
      Class<?> type,
      String description,
      Object defaultValue,
      List<String> examples,
      boolean deprecated) {

    public Prop(String name, Class<?> type, String description, Object defaultValue, List<String> examples) {
      this(name, type, description, defaultValue, examples, false);
    }

    public static Prop string(String name) {
      return new Prop(name, String.class, null, null, List.of(), false);
    }

    public static Prop bool(String name) {
      return new Prop(name, Boolean.class, null, null, List.of(), false);
    }

    public static Prop integer(String name) {
      return new Prop(name, Integer.class, null, null, List.of(), false);
    }

    public Prop description(String desc) {
      return new Prop(name, type, desc, defaultValue, examples, deprecated);
    }

    public Prop defaultValue(Object value) {
      return new Prop(name, type, description, value, examples, deprecated);
    }

    public Prop examples(List<String> ex) {
      return new Prop(name, type, description, defaultValue, ex, deprecated);
    }

    public Prop deprecated(boolean deprecated) {
      return new Prop(name, type, description, defaultValue, examples, deprecated);
    }
  }

}
