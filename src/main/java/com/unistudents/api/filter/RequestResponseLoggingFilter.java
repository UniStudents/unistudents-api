package com.unistudents.api.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@Order(1)
public class RequestResponseLoggingFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long time = System.currentTimeMillis();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } finally {
            time = System.currentTimeMillis() - time;
            logger.info("method={} path={} fwd={} service={}ms status={}", v("method", req.getMethod()), v("path", req.getRequestURI()), v("fwd", req.getRemoteAddr()), v("service", time), v("status", res.getStatus()));
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
