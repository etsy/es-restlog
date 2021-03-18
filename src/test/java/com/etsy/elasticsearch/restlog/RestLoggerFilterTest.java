package com.etsy.elasticsearch.restlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.test.rest.FakeRestChannel;
import org.elasticsearch.test.rest.FakeRestRequest;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RestLoggerFilterTest {
  /* This will hold the string passed to log.info as received by the mocked logger */
  private final LinkedList testingLogCalls = new LinkedList<String>();

  @Test
  public void testRestlogNoSettings() throws Exception {
    // clear any previous captured log calls just in case
    testingLogCalls.clear();
    Logger mockLogger = getMockLogger();
    RestLoggerFilter rlogger = new RestLoggerFilter(Settings.EMPTY);
    // Make sure we use our mocked logger
    injectMockLogger(rlogger, mockLogger);

    TestRestHandler testRequestHandler = new TestRestHandler();
    RestHandler restHandler = rlogger.apply(testRequestHandler);
    // ES data
    BytesReference testContent = new BytesArray("{\"query\": {\"match_all\": {}} }");
    FakeRestRequest testReq =
        new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(RestRequest.Method.POST)
            .withPath("/index1/_search?routing=1")
            .withContent(testContent, XContentType.JSON)
            .build();
    FakeRestChannel testChannel = new FakeRestChannel(testReq, false, 1);
    // actual call
    restHandler.handleRequest(testReq, testChannel, mock(NodeClient.class));
    String outputLogLine = ((String) testingLogCalls.pop());
    assertTrue(
        "restlog no settings, actual output: " + outputLogLine,
        outputLogLine.contains("POST /index1/_search?routing=1 - {\"query\":{\"match_all\":{}}}"));
  }

  @Test
  public void testRestlogUUID() throws Exception {
    // clear any previous captured log calls just in case
    testingLogCalls.clear();
    Logger mockLogger = getMockLogger();
    Settings uuidHeader =
        Settings.builder().put("restlog.uuid_header", "X-Query-Request-Uuid").build();
    RestLoggerFilter rlogger = new RestLoggerFilter(uuidHeader);
    // Make sure we use our mocked logger
    injectMockLogger(rlogger, mockLogger);

    TestRestHandler testRequestHandler = new TestRestHandler();
    RestHandler restHandler = rlogger.apply(testRequestHandler);
    // ES data
    Map<String, List<String>> uuidHeaders =
        Map.of("X-Query-Request-Uuid", Collections.singletonList("test-uuid"));
    BytesReference testContent = new BytesArray("{\"query\": {\"match_all\": {}} }");
    FakeRestRequest testReq =
        new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(RestRequest.Method.POST)
            .withPath("/index1/_search?routing=1")
            .withContent(testContent, XContentType.JSON)
            .withHeaders(uuidHeaders)
            .build();
    FakeRestChannel testChannel = new FakeRestChannel(testReq, false, 1);
    // actual call
    restHandler.handleRequest(testReq, testChannel, mock(NodeClient.class));
    String outputLogLine = ((String) testingLogCalls.pop());
    assertTrue(
        "restlog uuid_header, actual output: " + outputLogLine,
        outputLogLine.contains(
            "POST /index1/_search?routing=1 test-uuid {\"query\":{\"match_all\":{}}}"));
  }

  @Test
  public void testRestlogInvalidJson() throws Exception {
    testingLogCalls.clear();
    Logger mockLogger = getMockLogger();
    RestLoggerFilter rlogger = new RestLoggerFilter(Settings.EMPTY);
    injectMockLogger(rlogger, mockLogger);

    TestRestHandler testRequestHandler = new TestRestHandler();
    RestHandler restHandler = rlogger.apply(testRequestHandler);
    BytesReference testContent = new BytesArray("{\"query\": ");
    FakeRestRequest testReq =
        new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(RestRequest.Method.POST)
            .withPath("/index1/_search?routing=1")
            .withContent(testContent, XContentType.JSON)
            .build();
    FakeRestChannel testChannel = new FakeRestChannel(testReq, false, 1);
    restHandler.handleRequest(testReq, testChannel, mock(NodeClient.class));
    String outputLogLine = ((String) testingLogCalls.pop());
    assertTrue(
        "restlog with invalid json, actual output: " + outputLogLine,
        outputLogLine.contains("POST /index1/_search?routing=1 - _failed_to_convert_"));
  }

  @Test
  public void testRestlogBase64() throws Exception {
    testingLogCalls.clear();
    Logger mockLogger = getMockLogger();
    Settings base64Encoding = Settings.builder().put("restlog.content_encoding", "base64").build();
    RestLoggerFilter rlogger = new RestLoggerFilter(base64Encoding);
    injectMockLogger(rlogger, mockLogger);

    TestRestHandler testRequestHandler = new TestRestHandler();
    RestHandler restHandler = rlogger.apply(testRequestHandler);
    BytesReference testContent = new BytesArray("{\"query\": {\"match_all\": {}} }");
    FakeRestRequest testReq =
        new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(RestRequest.Method.POST)
            .withPath("/index1/_search?routing=1")
            .withContent(testContent, XContentType.JSON)
            .build();
    FakeRestChannel testChannel = new FakeRestChannel(testReq, false, 1);
    restHandler.handleRequest(testReq, testChannel, mock(NodeClient.class));
    String outputLogLine = ((String) testingLogCalls.pop());
    assertTrue(
        "restlog with base64 encoding, actual output: " + outputLogLine,
        outputLogLine.contains(
            "POST /index1/_search?routing=1 - eyJxdWVyeSI6IHsibWF0Y2hfYWxsIjoge319IH0="));
  }

  @Test
  public void testRestlogWithPathRegex() throws Exception {
    testingLogCalls.clear();
    Logger mockLogger = getMockLogger();
    // configure regex for a specific path
    Settings regexSetting =
        Settings.builder().put("restlog.path_regex", "\\/_search\\/?\\??.*").build();
    RestLoggerFilter rlogger = new RestLoggerFilter(regexSetting);
    injectMockLogger(rlogger, mockLogger);

    TestRestHandler testRequestHandler = new TestRestHandler();
    RestHandler restHandler = rlogger.apply(testRequestHandler);
    BytesReference testContent = new BytesArray("{\"query\": {\"match_all\": {}} }");

    FakeRestRequest testSearchReq =
        new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(RestRequest.Method.POST)
            .withPath("/index1/_search?routing=1")
            .withContent(testContent, XContentType.JSON)
            .build();
    FakeRestChannel testChannel = new FakeRestChannel(testSearchReq, false, 1);
    restHandler.handleRequest(testSearchReq, testChannel, mock(NodeClient.class));
    String outputLogLine = ((String) testingLogCalls.pop());
    assertTrue(
        "restlog contains search query, actual output: " + outputLogLine,
        outputLogLine.contains("POST /index1/_search?routing=1 - {\"query\":{\"match_all\":{}}}"));
    BytesReference testBulkContent =
        new BytesArray(
            "{\"index\":{\"_index\":\"test\",\"_id\":\"885895542-4\",\"_search\":\"72271777\"}");
    FakeRestRequest testBulk =
        new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(RestRequest.Method.POST)
            .withPath("/_bulk?timeout=1m")
            .withContent(testBulkContent, XContentType.JSON)
            .build();
    restHandler.handleRequest(testBulk, testChannel, mock(NodeClient.class));
    assertEquals("no restlog output", 0, testingLogCalls.size());
  }

  @Test
  public void testRestlogWithMethodRegex() throws Exception {
    testingLogCalls.clear();
    Logger mockLogger = getMockLogger();
    Settings regexSetting =
            Settings.builder().put("restlog.method_regex", "(POST|PUT)").build();
    RestLoggerFilter rlogger = new RestLoggerFilter(regexSetting);
    injectMockLogger(rlogger, mockLogger);

    TestRestHandler testRequestHandler = new TestRestHandler();
    RestHandler restHandler = rlogger.apply(testRequestHandler);

    BytesReference testContent = new BytesArray("{\"query\": {\"match_all\": {}} }");

    sendRequest(restHandler, RestRequest.Method.POST,"/index1/_search?routing=1", testContent);
    String outputLogLine = ((String) testingLogCalls.pop());
    assertTrue(
            "restlog contains search query, actual output: " + outputLogLine,
            outputLogLine.contains("POST /index1/_search?routing=1 - {\"query\":{\"match_all\":{}}}"));
    sendRequest(restHandler, RestRequest.Method.GET, "/_cluster/health", new BytesArray(""));
    assertEquals("no restlog output", 0, testingLogCalls.size());
  }


  private Logger getMockLogger() {
    Logger mockLogger = mock(Logger.class);
    when(mockLogger.isInfoEnabled()).thenReturn(true);

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                testingLogCalls.push(invocationOnMock.getArguments()[0]);
                return null;
              }
            })
        .when(mockLogger)
        .info(anyString());
    return mockLogger;
  }

  private void injectMockLogger(RestLoggerFilter restLogFilter, Logger mockLogger)
      throws NoSuchFieldException, IllegalAccessException {
    Field injected = RestLoggerFilter.class.getDeclaredField("log");
    injected.setAccessible(true);
    injected.set(restLogFilter, mockLogger);
  }

  private void sendRequest(RestHandler restHandler, RestRequest.Method method, String path, BytesReference testContent)
    throws Exception {
    FakeRestRequest testSearchReq =
            new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
                    .withMethod(method)
                    .withPath(path)
                    .withContent(testContent, XContentType.JSON)
                    .build();
    FakeRestChannel testChannel = new FakeRestChannel(testSearchReq, false, 1);
    restHandler.handleRequest(testSearchReq, testChannel, mock(NodeClient.class));
  }
}

class TestRestHandler implements RestHandler {

  @Override
  public void handleRequest(RestRequest request, RestChannel channel, NodeClient client)
      throws Exception {}
}
