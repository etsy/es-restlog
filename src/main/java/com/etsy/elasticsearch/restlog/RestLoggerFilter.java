package com.etsy.elasticsearch.restlog;

import com.google.common.base.Joiner;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestFilter;
import org.elasticsearch.rest.RestFilterChain;
import org.elasticsearch.rest.RestRequest;

import java.util.function.Predicate;
import java.util.regex.Pattern;

final class RestLoggerFilter extends RestFilter {

  private final ESLogger log;
  private final Predicate<String> pathFilter;
  private final ContentEncoder contentEncoder;
  private final Joiner joiner;

  public RestLoggerFilter(Settings settings) {
    log = ESLoggerFactory.getLogger(settings.get("restlog.category", "restlog"));
    pathFilter = pathFilter(settings.get("restlog.path_regex", ""));
    contentEncoder = encoder(settings.get("restlog.content_encoding", "json"));
    joiner = Joiner.on(" ").useForNull(settings.get("restlog.null_value", "-"));
  }

  @Override
  public void process(RestRequest restRequest, RestChannel restChannel, RestFilterChain restFilterChain) throws Exception {
    try {
      if (log.isInfoEnabled() && pathFilter.test(restRequest.rawPath())) {
        log.info(
            joiner.join(
                System.currentTimeMillis(),
                restRequest.getLocalAddress(),
                restRequest.getRemoteAddress(),
                restRequest.method(),
                restRequest.uri(),
                encodeContent(restRequest.content())
            )
        );
      }
    } catch (Exception e) {
      // errors shouldn't happen, but if they do, prevent it from interfering with request-processing
      // don't want to emit weird log lines using `log` either, so just send a strace to stderr
      e.printStackTrace(System.err);
    }
    restFilterChain.continueProcessing(restRequest, restChannel);
  }

  private String encodeContent(BytesReference content) {
    if (content == null) return null;
    final int offset = content.arrayOffset();
    final int length = content.length();
    if (length - offset == 0) return null;
    return contentEncoder.encode(content.array(), offset, length);
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
