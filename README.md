# es-restlog

REST request logging for Elasticsearch

# Overview

The `es-restlog` plugin hooks into the Elasticsearch REST request processing chain directly and logs the request before it gets processed, at the entry point i.e. the instance receiving the request.

Sample log line:

```
[2015-11-06 15:49:00,711] /10.96.244.81:58622 GET /twitter/tweet/_search {"query":{"term":{"user":"kimchy"}}}
```

Note that Elasticsearch provides the 'slowlog' mechanism for capturing index or search requests, and you can set the threshold to 0 to log all requests. This approach has some limitations:
  * the slowlog only records a parsed representation of the request, and arbitrary URL parameters are discarded, e.g. request-id's
  * it operates at the shard-request level so you end up with lots of lines logged in case there are multiple shards or query phases involved
  * requests are logged after processing, so if ES ends up crashing due to the processing that request, it will not be logged
  * if it's a bad request that errors out, you won't see it in the slowlog

# Installation

The plugin is available for Elastisearch 2.x and 1.x. For 1.x it has only been tested on ES 1.7, but will _probably_ work against older releases as well.

## pre-packaged

The release naming scheme is `es-restlog-${plugin.version}-es${es.major.version}.zip`.

Head over to `Releases` on Github to find the latest plugin package.

## packaging

Use the [sbt](http://www.scala-sbt.org/#install) target `pack`, which will generate a plugin zip under `target/`.

Note that you can supply a `file:///path/to/plugin.zip` URL to the plugin script. 

# Configuration

## plugin

`restlog.category` the logger category to be used, defaults to "restlog".

`restlog.path_regex` allows for filtering what gets logged at the level of the HTTP request path, defaults to blank which implies matching everything. If you only want to include search requests for example, you could set this to `\/_search\/?\??.*`.
 
`restlog.content_encoding` how the request body is encoded in the log line -- valid choices are "json", "base64", or "hex". Default is "json".

`restlog.null_value` how any value that is not available (e.g. if there was no request body) get encoded in the log line, defaults to "-".

## logging

Note that the plugin uses `INFO` level for logging at the configured category.

You will probably want to direct the restlog output to a dedicated logfile. The configuration can be based off how the slowlogs get configured in `logging.yml`, for example:

```yaml
logger:
  restlog: INFO, restlog

additivity:
  restlog: false

appender:
  restlog:
    type: dailyRollingFile
    file: ${path.logs}/${cluster.name}_rest.log
    datePattern: "'.'yyyy-MM-dd"
    layout:
      type: pattern
      conversionPattern: "[%d{ISO8601}] %m%n"
```
