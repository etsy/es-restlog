package com.etsy.elasticsearch.restlog;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

public class RestlogPluginTest {

  @Test
  public void testDefaultSettings() throws Exception {
    Settings settings = Settings.EMPTY;
    RestlogPlugin plugin = new RestlogPlugin(settings);
    List<Setting<?>> pluginSettings = plugin.getSettings();
    assertTrue(
        "setting: restlog.category",
        pluginSettings.stream()
            .anyMatch(
                item ->
                    item.match("restlog.category")
                        && item.getDefaultRaw(Settings.EMPTY).equals("restlog")));
    assertTrue(
        "setting: restlog.path_regex",
        pluginSettings.stream()
            .anyMatch(
                item ->
                    item.match("restlog.path_regex")
                        && item.getDefaultRaw(Settings.EMPTY).equals("")));
    assertTrue(
        "setting: restlog.content_encoding",
        pluginSettings.stream()
            .anyMatch(
                item ->
                    item.match("restlog.content_encoding")
                        && item.getDefaultRaw(Settings.EMPTY).equals("json")));
    assertTrue(
        "setting: restlog.null_value",
        pluginSettings.stream()
            .anyMatch(
                item ->
                    item.match("restlog.null_value")
                        && item.getDefaultRaw(Settings.EMPTY).equals("-")));
  }
}
