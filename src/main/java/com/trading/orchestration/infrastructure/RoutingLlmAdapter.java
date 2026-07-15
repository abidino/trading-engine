package com.trading.orchestration.infrastructure;

import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Primary LlmPort bean — routes each request to the correct provider adapter
 * based on the routing config in application.yml.
 *
 * To switch PORTFOLIO_REVIEW from Ollama to OpenAI, change ONE line in application.yml:
 *   llm.routing.PORTFOLIO_REVIEW: openai
 * No code changes needed.
 */
@Slf4j
@Primary
@Profile("!mock")
@Component
public class RoutingLlmAdapter implements LlmPort {

    private final LlmProperties properties;
    private final Map<String, LlmPort> adaptersByProvider;

    public RoutingLlmAdapter(
            LlmProperties properties,
            @Qualifier("ollamaLlm") LlmPort ollama,
            @Qualifier("openaiLlm") LlmPort openai,
            @Qualifier("anthropicLlm") LlmPort anthropic,
            @Qualifier("proxyLlm") LlmPort proxy,
            @Qualifier("deepseekLlm") LlmPort deepseek) {
        this.properties = properties;
        this.adaptersByProvider = Map.of(
                "ollama", ollama,
                "openai", openai,
                "anthropic", anthropic,
                "proxy", proxy,
                "deepseek", deepseek
        );
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String provider = resolveProvider(request);
        LlmPort adapter = adaptersByProvider.getOrDefault(
                provider, adaptersByProvider.get(properties.getDefaultProvider()));

        log.debug("Routing LLM request type={} to provider={}", request.analysisType(), provider);
        return adapter.complete(request);
    }

    private String resolveProvider(LlmRequest request) {
        if (request.analysisType() != null) {
            String routed = properties.getRouting().get(request.analysisType().name());
            if (routed != null) return routed;
        }
        return properties.getDefaultProvider();
    }
}
