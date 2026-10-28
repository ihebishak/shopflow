package com.shopflow.starter.correlation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void reuses_the_correlation_id_sent_by_the_gateway() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Correlation.HTTP_HEADER, "corr-from-gateway");
        AtomicReference<String> seenDuringRequest = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenDuringRequest.set(Correlation.current().orElse(null));
            }
        });

        assertThat(seenDuringRequest.get()).isEqualTo("corr-from-gateway");
        assertThat(response.getHeader(Correlation.HTTP_HEADER)).isEqualTo("corr-from-gateway");
        assertThat(Correlation.current()).as("MDC nettoyé après la requête").isEmpty();
    }

    @Test
    void creates_a_correlation_id_when_none_is_sent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader(Correlation.HTTP_HEADER)).isNotBlank();
    }

    @Test
    void replaces_a_malicious_correlation_id() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Correlation.HTTP_HEADER, "evil\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(Correlation.HTTP_HEADER)).doesNotContain("evil").matches("[A-Za-z0-9-]+");
    }
}
