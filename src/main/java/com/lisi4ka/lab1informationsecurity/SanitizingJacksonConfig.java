package com.lisi4ka.lab1informationsecurity;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SanitizingJacksonConfig {

    @Bean
    public PolicyFactory xssPolicyFactory() {
        // Keep basic formatting/links/images, strip scripts/handlers/JS URLs, etc.
        return Sanitizers.BLOCKS.and(Sanitizers.FORMATTING).and(Sanitizers.LINKS).and(Sanitizers.IMAGES);
    }

    @Bean
    public Module sanitizeAllStringsModule(PolicyFactory policy) {
        SimpleModule m = new SimpleModule();
        m.addDeserializer(String.class, new SanitizingStringDeserializer(policy));
        return m;
    }

    static class SanitizingStringDeserializer extends StdScalarDeserializer<String> {
        private final PolicyFactory policy;

        protected SanitizingStringDeserializer(PolicyFactory policy) {
            super(String.class);
            this.policy = policy;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String raw = p.getValueAsString();
            if (raw == null) return null;
            // Sanitize AND normalize whitespace a bit (optional)
            String cleaned = policy.sanitize(raw);
            return cleaned.strip();
        }
    }
}
