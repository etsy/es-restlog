package com.etsy.elasticsearch.restlog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;

public class RestlogPlugin extends Plugin implements ActionPlugin {

  private final Settings settings;

  public RestlogPlugin(Settings settings) {
    this.settings = settings;
  }

  @Override
  public UnaryOperator<RestHandler> getRestHandlerWrapper(ThreadContext threadContext) {
    return new RestLoggerFilter(this.settings);
  }

  @Override
  public List<Setting<?>> getSettings() {
    List<Setting<?>> settings = new ArrayList<>();
    settings.add(
        new Setting<>(
            "restlog.category", "restlog", Function.identity(), Setting.Property.NodeScope));
    settings.add(
        new Setting<>("restlog.path_regex", "", Function.identity(), Setting.Property.NodeScope));
    settings.add(
      new Setting<>("restlog.method_regex", "", Function.identity(), Setting.Property.NodeScope));
    settings.add(
        new Setting<>(
            "restlog.content_encoding", "json", Function.identity(), Setting.Property.NodeScope));
    settings.add(
        new Setting<>("restlog.null_value", "-", Function.identity(), Setting.Property.NodeScope));
    settings.add(
        new Setting<>("restlog.uuid_header", "", Function.identity(), Setting.Property.NodeScope));
    return settings;
  }
}
