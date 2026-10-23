package com.markethub.common.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markethub.common.exception.BusinessException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/** 把下游的 R 错误体还原成 BusinessException，保住业务提示。 */
@Slf4j
public class RErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                String body = Util.toString(response.body().asReader(Util.UTF_8));
                JsonNode node = objectMapper.readTree(body);
                JsonNode code = node.get("code");
                JsonNode message = node.get("message");
                if (code != null && message != null) {
                    return new BusinessException(code.asInt(), message.asText());
                }
            }
        } catch (Exception e) {
            log.warn("解析下游错误响应失败: method={}, {}", methodKey, e.getMessage());
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
