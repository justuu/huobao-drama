package com.huobao.drama.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI 配置。
 * ChatModel / ChatClient 不再作为全局 Bean 注入，
 * 改为在 AgentService 中根据数据库 ai_service_configs 动态构建。
 */
@Configuration
public class AiConfig {
}
