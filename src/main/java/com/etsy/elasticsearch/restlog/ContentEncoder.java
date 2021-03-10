package com.etsy.elasticsearch.restlog;

import com.google.common.io.BaseEncoding;
import java.io.IOException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

public enum ContentEncoder {
  JSON {
    @Override
    public String encode(BytesReference content) {
      try {
        return XContentHelper.convertToJson(content, true, XContentType.JSON);
      } catch (IOException e) {
        return "_failed_to_convert_";
      }
    }
  },
  BASE64 {
    @Override
    public String encode(BytesReference content) {
      return BaseEncoding.base64().encode(BytesReference.toBytes(content));
    }
  },
  HEX {
    @Override
    String encode(BytesReference content) {
      return BaseEncoding.base16().encode(BytesReference.toBytes(content));
    }
  };

  abstract String encode(BytesReference content);
}
