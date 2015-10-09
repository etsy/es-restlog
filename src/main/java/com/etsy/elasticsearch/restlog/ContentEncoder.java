package com.etsy.elasticsearch.restlog;

import com.google.common.io.BaseEncoding;
import org.elasticsearch.common.xcontent.XContentHelper;

import java.io.IOException;

public enum ContentEncoder {

  JSON {
    @Override
    public String encode(byte[] data, int offset, int len) {
      try {
        return XContentHelper.convertToJson(data, offset, len, true);
      } catch (IOException e) {
        return "_failed_to_convert_";
      }
    }
  },
  BASE64 {
    @Override
    public String encode(byte[] data, int offset, int len) {
      return BaseEncoding.base64().encode(data, offset, len);
    }
  },
  HEX {
    @Override
    String encode(byte[] data, int offset, int len) {
      return BaseEncoding.base16().encode(data, offset, len);
    }
  };

  abstract String encode(byte[] data, int offset, int len);

}
