package com.ventionteams;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@WebServlet("/*")
public class MainServlet extends HttpServlet {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            final HttpRequest request = createHttpRequest(req);
            final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            setHeadersToResponse(response, resp);
            resp.setStatus(response.statusCode());

            final ServletOutputStream servletOutputStream = resp.getOutputStream();
            servletOutputStream.write(response.body());
            servletOutputStream.close();
        } catch (InterruptedException exception) {
            final PrintWriter printWriter = resp.getWriter();
            printWriter.write(exception.getMessage());
            printWriter.close();
        }
    }

    private void setHeadersToResponse(final HttpResponse<?> response, final HttpServletResponse resp) {
        final HttpHeaders httpHeaders = response.headers();
        httpHeaders.map().forEach((name, values) -> {
            values.forEach(value -> resp.addHeader(name, value));
        });
    }

    private HttpRequest createHttpRequest(final HttpServletRequest req) {
        final String url = req.getRequestURL().toString();
        final String body = extractBodyFromRequest(req);
        final String[] headers = extractHeadersFromRequest(req);

        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers(headers)
            .method(
                req.getMethod(),
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)
            )
            .build();
    }

    private String[] extractHeadersFromRequest(final HttpServletRequest req) {
        final List<String> headers = new ArrayList<>();

        final Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            final String headerValue = req.getHeader(headerName);
            headers.addAll(List.of(headerName, headerValue));
        }

        return headers.toArray(new String[0]);
    }

    private String extractBodyFromRequest(final HttpServletRequest req) {
        try {
            return req.getReader()
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException exception) {
            log.error("Error during extracting body from request: " + exception.getMessage());
            return null;
        }
    }

}
