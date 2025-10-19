package webframe.core;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

@WebFilter("/*")
public class ResourceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // Remove context path to get the resource path
        String resourcePath = uri.substring(contextPath.length());
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }

        // Decode and validate path to prevent path traversal attacks
        String decodedPath = URLDecoder.decode(resourcePath, "UTF-8");

        // Security validation: reject dangerous path patterns
        if (containsPathTraversalAttempt(decodedPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if the request is for a JSP or HTML resource
        if (decodedPath.endsWith(".jsp") || decodedPath.endsWith(".html")) {
            // Sanitize the path to ensure it's safe
            String sanitizedPath = sanitizeResourcePath(decodedPath);
            if (sanitizedPath != null) {
                // Check if resource exists in the web application
                ServletContext context = request.getServletContext();
                try (InputStream resourceStream = context.getResourceAsStream(sanitizedPath)) {
                    if (resourceStream != null) {
                        // Resource exists, forward to it (with null check for security)
                        RequestDispatcher dispatcher = context.getRequestDispatcher(sanitizedPath);
                        if (dispatcher != null) {
                            dispatcher.forward(request, response);
                            return;
                        }
                    }
                } catch (Exception e) {
                    // Resource not found or error accessing it, continue to dispatcher
                }
            }
        }

        // Not a resource or not available, continue to the chain (DispatcherServlet)
        chain.doFilter(request, response);
    }

    /**
     * Check if the path contains potential path traversal attempts
     * @param path the decoded path to check
     * @return true if the path contains dangerous patterns
     */
    private boolean containsPathTraversalAttempt(String path) {
        if (path == null) {
            return true;
        }

        // Convert to lowercase for case-insensitive checking
        String lowerPath = path.toLowerCase();

        // Check for common path traversal patterns
        return lowerPath.contains("..") ||
               lowerPath.contains("%2e%2e") ||
               lowerPath.contains("..%2f") ||
               lowerPath.contains("..\\") ||
               lowerPath.contains("%2e%2e%2f") ||
               lowerPath.contains("%2e%2e\\") ||
               !path.startsWith("/");
    }

    /**
     * Sanitize and validate the resource path to prevent security issues
     * @param path the path to sanitize
     * @return the sanitized path if safe, null otherwise
     */
    private String sanitizeResourcePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // Ensure path starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // Remove any double slashes
        path = path.replaceAll("//+", "/");

        // Split path into segments and validate each
        String[] segments = path.split("/");
        StringBuilder sanitizedPath = new StringBuilder();

        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue; // Skip empty segments
            }

            // Reject dangerous segments
            if (segment.equals("..") ||
                segment.equals(".") ||
                segment.contains("\\") ||
                segment.toLowerCase().contains("%2e")) {
                return null;
            }

            sanitizedPath.append("/").append(segment);
        }

        String result = sanitizedPath.toString();
        if (result.isEmpty()) {
            result = "/";
        }

        // Final validation: ensure it's still a JSP or HTML resource
        if (!result.endsWith(".jsp") && !result.endsWith(".html")) {
            return null;
        }

        return result;
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
