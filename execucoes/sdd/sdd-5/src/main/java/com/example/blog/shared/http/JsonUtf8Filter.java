package com.example.blog.shared.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Garante o cabeçalho {@code Content-Type: application/json;charset=UTF-8} exigido pelo contrato em todas as
 * respostas JSON, inclusive as de erro. O conversor JSON do Spring omite o charset por padrão.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JsonUtf8Filter extends OncePerRequestFilter {

    private static final String CHARSET_UTF8 = ";charset=UTF-8";

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        cadeia.doFilter(requisicao, new RespostaComCharset(resposta));
    }

    static String comCharset(String tipo) {
        if (tipo == null) {
            return null;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(tipo);
            if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON) && mediaType.getCharset() == null) {
                return tipo + CHARSET_UTF8;
            }
        } catch (InvalidMediaTypeException excecao) {
            return tipo;
        }
        return tipo;
    }

    private static final class RespostaComCharset extends HttpServletResponseWrapper {

        RespostaComCharset(HttpServletResponse resposta) {
            super(resposta);
        }

        @Override
        public void setContentType(String tipo) {
            super.setContentType(comCharset(tipo));
        }

        @Override
        public void setHeader(String nome, String valor) {
            super.setHeader(nome, HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(nome) ? comCharset(valor) : valor);
        }

        @Override
        public void addHeader(String nome, String valor) {
            super.addHeader(nome, HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(nome) ? comCharset(valor) : valor);
        }
    }
}
