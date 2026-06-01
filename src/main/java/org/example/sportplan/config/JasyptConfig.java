package org.example.sportplan.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jasypt 配置类，用于对 application.yaml 中的敏感信息进行加解密
 * 启动时通过环境变量 JASYPT_ENCRYPTOR_PASSWORD 传入主密钥
 */
@Configuration
public class JasyptConfig {

    // 默认主密钥（仅开发环境使用，生产环境必须通过环境变量覆盖）
    private static final String DEFAULT_PASSWORD = "SportPlan@Secret2024";

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        // 优先从环境变量读取密钥，不存在则使用默认值
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        config.setPassword(password != null ? password : DEFAULT_PASSWORD);
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}
