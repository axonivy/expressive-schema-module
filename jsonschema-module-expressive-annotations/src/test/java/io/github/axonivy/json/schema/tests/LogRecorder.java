package io.github.axonivy.json.schema.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogRecorder {

  private final List<LogRecord> records = new ArrayList<>();

  public <T> T record(String logName, Supplier<T> action) {
    var handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        records.add(record);
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    Logger logger = Logger.getLogger(logName);
    logger.addHandler(handler);
    try {
      return action.get();
    } finally {
      logger.removeHandler(handler);
    }
  }

  public List<String> warnings() {
    return logs(Level.WARNING).stream()
               .map(LogRecord::getMessage)
               .toList();
  }

  private List<LogRecord> logs(Level level) {
    return records.stream()
                .filter(record -> record.getLevel().intValue() >= level.intValue())
                .toList();
  }

}
