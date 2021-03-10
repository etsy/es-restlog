package com.etsy.elasticsearch.restlog;

import com.google.common.base.Joiner;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;

final class RestLoggerFilter implements UnaryOperator<RestHandler> {

  private final Logger log;
  private final Predicate<String> pathFilter;
  private final ContentEncoder contentEncoder;
  private final Joiner joiner;
  private final String requestUuidHeader;

  public RestLoggerFilter(Settings settings) {
    log = Loggers.getLogger(RestlogPlugin.class, settings.get("restlog.category", "restlog"));
    pathFilter = pathFilter(settings.get("restlog.path_regex", ""));
    contentEncoder = encoder(settings.get("restlog.content_encoding", "json"));
    joiner = Joiner.on(" ").useForNull(settings.get("restlog.null_value", "-"));
    requestUuidHeader = settings.get("restlog.uuid_header", "");
  }

  @Override
  public RestHandler apply(RestHandler restHandler) {
    if (log.isInfoEnabled()) {
      return new LoggingRestHandler(restHandler);
    }
    return restHandler;
  }

  class LoggingRestHandler implements RestHandler {
    private final RestHandler wrapped;

    public LoggingRestHandler(RestHandler wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client)
        throws Exception {
      try {
        if (pathFilter.test(request.rawPath())) {
          String requestUuidValue = null;
          if (!requestUuidHeader.equals("")) {
            requestUuidValue = request.header(requestUuidHeader);
          }
          String content = request.hasContent() ? encodeContent(request.content()) : "-";
          log.info(
              joiner.join(
                  System.currentTimeMillis(),
                  request.getHttpChannel().getLocalAddress(),
                  request.getHttpChannel().getRemoteAddress(),
                  request.method(),
                  request.uri(),
                  Objects.requireNonNullElse(requestUuidValue, "-"),
                  content));
        }
      } catch (Exception e) {
        // errors shouldn't happen, but if they do, prevent it from interfering with
        // request-processing
        // don't want to emit weird log lines using `log` either, so just send a strace to stderr
        e.printStackTrace(System.err);
      }
      wrapped.handleRequest(request, channel, client);
    }

    @Override
    public boolean canTripCircuitBreaker() {
      return wrapped.canTripCircuitBreaker();
    }

    @Override
    public boolean supportsContentStream() {
      return wrapped.supportsContentStream();
    }
  }

  private String encodeContent(BytesReference content) {
    if (content == null) return null;
    return contentEncoder.encode(content);
  }

  private static Predicate<String> pathFilter(String re) {
    return (re.isEmpty())
        ? s -> true // accept everything
        : Pattern.compile(re).asPredicate();
  }

  private static ContentEncoder encoder(String type) {
    final ContentEncoder encoder = ContentEncoder.valueOf(type.toUpperCase());
    if (encoder == null) {
      throw new RuntimeException(String.format("Invalid content encoder type: '%s'", type));
    }
    return encoder;
  }
}
