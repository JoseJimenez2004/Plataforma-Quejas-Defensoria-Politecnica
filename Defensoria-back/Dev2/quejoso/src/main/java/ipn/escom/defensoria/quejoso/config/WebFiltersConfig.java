package ipn.escom.defensoria.quejoso.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.IOException;

/**
 * Configuración centralizada de filtros web (HTTP).
 * Aquí se registran los filtros de Servlet personalizados para la aplicación.
 */
@Configuration
public class WebFiltersConfig {

    /**
     * Filtro para eliminar la diagonal final (Trailing Slash).
     * Spring Boot 3.x deshabilitó el trailing slash matching por defecto.
     * Este filtro recorta el "/" final de todas las peticiones a la API para evitar errores 404/500.
     */
    @Bean
    public FilterRegistrationBean<Filter> trailingSlashFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpReq = (HttpServletRequest) request;
                String uri = httpReq.getRequestURI();

                if (uri.length() > 1 && uri.endsWith("/")) {
                    String newUri = uri.substring(0, uri.length() - 1);
                    HttpServletRequest wrapped = new HttpServletRequestWrapper(httpReq) {
                        @Override
                        public String getRequestURI() { return newUri; }
                        @Override
                        public String getServletPath() { return newUri; }
                    };
                    chain.doFilter(wrapped, response);
                } else {
                    chain.doFilter(request, response);
                }
            }
        });
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Solución para ERR_INCOMPLETE_CHUNKED_ENCODING.
     * Reemplaza Transfer-Encoding: chunked por un Content-Length fijo calculando el tamaño de la respuesta completa.
     * Evita fallos de conexión prematura con el frontend de Angular.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterBean =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        filterBean.addUrlPatterns("/api/*");
        filterBean.setName("etagFilter");
        return filterBean;
    }
}
