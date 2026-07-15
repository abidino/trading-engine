package com.trading.config;

import com.trading.discovery.domain.model.FilterCatalog;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM native-image hints for reflective/resource access that the AOT engine cannot
 * infer on its own.
 *
 * <p>The only spot in this codebase that reaches beyond what Spring AOT auto-detects is
 * {@code FinvizFilterCatalog}, which Jackson-binds the bundled
 * {@code finviz/filter-catalog.json} resource into the {@link FilterCatalog} record graph.
 * Native image needs (1) reflection metadata for those records and (2) the JSON resource
 * to be embedded in the binary. Everything else (JPA entities, controllers,
 * {@code @ConfigurationProperties}, scheduling, events) is covered automatically by
 * Spring Boot's AOT processing.</p>
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeRuntimeHints.Registrar.class)
public class NativeRuntimeHints {

    static class Registrar implements RuntimeHintsRegistrar {

        private static final MemberCategory[] BINDING = {
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS
        };

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Jackson deserialization target graph for the Finviz filter catalog.
            hints.reflection()
                    .registerType(FilterCatalog.class, BINDING)
                    .registerType(FilterCatalog.Group.class, BINDING)
                    .registerType(FilterCatalog.Filter.class, BINDING)
                    .registerType(FilterCatalog.Option.class, BINDING);

            // The bundled catalog JSON must be embedded in the native image.
            hints.resources().registerPattern("finviz/filter-catalog.json");
        }
    }
}
