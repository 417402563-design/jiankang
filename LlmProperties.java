package cn.edu.sdua.jiankang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型问答生成配置(OpenAI 兼容,可选)
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.api")
public class LlmProperties {

    private String url = "";
    private String key = "";
    private String model = "gpt-3.5-turbo";
}
