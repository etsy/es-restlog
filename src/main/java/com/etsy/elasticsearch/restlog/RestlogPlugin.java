package com.etsy.elasticsearch.restlog;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestController;

import java.util.Collection;
import java.util.Collections;

public class RestlogPlugin extends AbstractPlugin {

  @Override
  public String name() {
    return "es-restlog";
  }

  @Override
  public String description() {
    return "REST request logging for Elasticsearch";
  }

  @Override
  public Collection<Module> modules(Settings settings) {
    final Module restLoggerModule = binder -> binder.bind(RestLogger.class).asEagerSingleton();
    return Collections.singleton(restLoggerModule);
  }

  public static class RestLogger {

    @Inject
    public RestLogger(RestController restController, Settings settings) {
      restController.registerFilter(new RestLoggerFilter(settings));
    }

  }

}
