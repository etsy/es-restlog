# es-restlog

REST request logging for Elasticsearch

## Overview

The `es-restlog` plugin hooks into the Elasticsearch REST request processing chain directly and logs the request before it gets processed, at the instance handling the client request.

Sample log line:

```
1449700736656 /10.96.244.81:9200 /10.96.244.81:58622 GET /twitter/tweet/_search {"query":{"term":{"user":"kimchy"}}}
```

Note that Elasticsearch provides the 'slowlog' mechanism for capturing index or search requests, and you can set the threshold to 0 to log all requests. This approach has some limitations:
  * the slowlog only records a parsed representation of the request, and arbitrary URL parameters are discarded, e.g. request-id's
  * it operates at the shard-request level so you end up with lots of lines logged in case there are multiple shards or query phases involved
  * requests are logged after processing, so if ES ends up crashing due to the processing that request, it will not be logged
  * if it's a bad request that errors out, you won't see it in the slowlog

## Installation

The plugin is available for Elastisearch 7.x

### pre-packaged

Head over to `Releases` on Github to find the latest plugin package.

### packaging

We use [bazel](https://bazel.build/) to build and package the es-restlog elasticsearch plugin.

1. Install [bazelisk](https://docs.bazel.build/versions/master/install-bazelisk.html), a simple bazel wrapper and add it in your `$PATH`.
Bazelisk will automatically download the required bazel version defined in `.bazelversion`.

2. To build the plugin locally:
    ```
    bazelisk build //src/main/java/com/etsy/elasticsearch/restlog:restlog_plugin_versioned
    ``` 

    The plugin `zip` file will be available in the following path:
    ```
    bazel-bin/src/main/java/com/etsy/elasticsearch/restlog/restlog-plugin-X.Y.Z.zip
    ```

3. To update the version of `elasticsearch` before building the plugin:
  - update the ES version in `version.bzl`
  - update the jvm dependencies for the new version by running:
    ```bash
    REPIN=1 bazelisk run @unpinned_jvm_deps//:pin
    ```
  - build the plugin: `bazelisk build //src/main/java/com/etsy/elasticsearch/restlog:restlog_plugin_versioned`

## Configuration

### x-pack security module
`es-restlog` and X-Pack security module can't run together, so the latter needs to be disabled.

Add `xpack.security.enabled: false` to `elasticsearch.yml`

### plugin

The following plugin configuration can be added in `elasticsearch.yml`:

`restlog.category` the logger category to be used, defaults to "restlog".

`restlog.path_regex` allows for filtering what gets logged at the level of the HTTP request path, defaults to blank which implies matching everything. If you only want to include search requests for example, you could set this to `\/_search\/?\??.*`.
 
`restlog.content_encoding` how the request body is encoded in the log line -- valid choices are "json", "base64", or "hex". Default is "json".

`restlog.null_value` how any value that is not available (e.g. if there was no request body) get encoded in the log line, defaults to "-".

`restlog.uuid_header` log this request header value if present in the request (e.g. X-Request-ID), defaults to "" (disabled)