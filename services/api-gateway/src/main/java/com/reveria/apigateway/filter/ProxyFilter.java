package com.reveria.apigateway.filter;

import com.reveria.apigateway.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class ProxyFilter implements WebFilter, Ordered {

    private final GatewayProperties gatewayProperties;
    private final Map<String, WebClient> serviceClients;

    public ProxyFilter(GatewayProperties gatewayProperties, WebClient.Builder webClientBuilder) {
        this.gatewayProperties = gatewayProperties;

        // Pre-build a WebClient per route target
        var clients = new java.util.HashMap<String, WebClient>();
        if (gatewayProperties.getRoutes() != null) {
            gatewayProperties.getRoutes().forEach((name, routeConfig) ->
                    clients.put(name, webClientBuilder.baseUrl(routeConfig.getUrl()).build())
            );
        }
        this.serviceClients = Map.copyOf(clients);
    }

    @Override
    public int getOrder() {
        return 0; // Run after JWT filter
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Find matching route
        String matchedService = resolveService(path);
        if (matchedService == null) {
            return notFound(exchange);
        }

        WebClient client = serviceClients.get(matchedService);
        if (client == null) {
            log.error("No WebClient configured for service: {}", matchedService);
            return serverError(exchange);
        }

        return proxyRequest(exchange, client);
    }

    private String resolveService(String path) {
        if (gatewayProperties.getRoutes() == null) return null;

        for (Map.Entry<String, GatewayProperties.RouteConfig> entry : gatewayProperties.getRoutes().entrySet()) {
            for (String prefix : entry.getValue().getPrefixes()) {
                if (path.startsWith(prefix)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private Mono<Void> proxyRequest(ServerWebExchange exchange, WebClient client) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        String uri = query != null ? path + "?" + query : path;
        HttpMethod method = request.getMethod();

        log.debug("Proxying {} {} -> {}", method, uri, client);

        // Build the outbound request, forwarding all headers
        WebClient.RequestBodySpec spec = client.method(method)
                .uri(uri)
                .headers(headers -> {
                    // Forward all original headers
                    request.getHeaders().forEach((name, values) -> {
                        // Skip hop-by-hop headers
                        if (!name.equalsIgnoreCase("Host")
                                && !name.equalsIgnoreCase("Connection")
                                && !name.equalsIgnoreCase("Transfer-Encoding")) {
                            headers.addAll(name, values);
                        }
                    });
                });

        // Forward request body for methods that have one
        Mono<WebClient.RequestHeadersSpec<?>> requestMono;
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            requestMono = Mono.just(
                    spec.body(BodyInserters.fromDataBuffers(request.getBody()))
            );
        } else {
            requestMono = Mono.just(spec);
        }

        return requestMono.flatMap(reqSpec ->
                reqSpec.exchangeToMono(clientResponse -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(clientResponse.statusCode());

                    // Copy response headers
                    clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                        if (!name.equalsIgnoreCase("Transfer-Encoding")) {
                            response.getHeaders().addAll(name, values);
                        }
                    });

                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                })
        ).onErrorResume(WebClientResponseException.class, ex -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.valueOf(ex.getStatusCode().value()));
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] body = ex.getResponseBodyAsByteArray();
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        }).onErrorResume(Exception.class, ex -> {
            log.error("Proxy error: {}", ex.getMessage(), ex);
            return serverError(exchange);
        });
    }

    private Mono<Void> notFound(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"message":"No route found","timestamp":%d}
                """.formatted(System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> serverError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_GATEWAY);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"message":"Service unavailable","timestamp":%d}
                """.formatted(System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}

