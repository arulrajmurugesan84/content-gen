Filter Registration Options
Option A: FilterRegistrationBean (Recommended)
Register directly in a @Configuration class — no need to touch WebMvcConfigurer. This is the most explicit and Spring Boot-friendly approach.
java@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<FlowableVariableInjectingFilter> flowableVariableFilter(
            ObjectMapper objectMapper) {

        FilterRegistrationBean<FlowableVariableInjectingFilter> registration = 
            new FilterRegistrationBean<>();

        registration.setFilter(new FlowableVariableInjectingFilter(objectMapper));
        registration.addUrlPatterns(
            "/process-api/runtime/process-instances",
            "/process-api/runtime/process-instances/*"
        );
        registration.setName("flowableVariableInjectingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
Option B: @WebFilter annotation on the filter class itself
java@WebFilter(urlPatterns = {
    "/process-api/runtime/process-instances",
    "/process-api/runtime/process-instances/*"
})
But this requires @ServletComponentScan on your main class, making Option A cleaner.

Note: When using FilterRegistrationBean, remove @Component from the filter class — otherwise Spring Boot auto-registers it for all URLs in addition to your restricted registration, causing the filter to run twice.


Complete Code
FlowableVariableInjectingFilter.java
javapublic class FlowableVariableInjectingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FlowableVariableInjectingFilter.class);

    private final ObjectMapper objectMapper;

    public FlowableVariableInjectingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Secondary guard — only process POST and PUT requests
        String method = request.getMethod();
        return !("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            ModifiedBodyRequestWrapper wrappedRequest = 
                new ModifiedBodyRequestWrapper(request, objectMapper);
            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            log.error("Failed to modify Flowable request body, falling back to original request", e);
            filterChain.doFilter(request, response);
        }
    }
}

ModifiedBodyRequestWrapper.java
javapublic class ModifiedBodyRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger log = LoggerFactory.getLogger(ModifiedBodyRequestWrapper.class);
    private static final String URLS_VARIABLE_NAME = "urls";

    private final byte[] modifiedBody;

    public ModifiedBodyRequestWrapper(HttpServletRequest request,
                                      ObjectMapper objectMapper) throws IOException {
        super(request);
        byte[] originalBodyBytes = request.getInputStream().readAllBytes();
        this.modifiedBody = buildModifiedBody(originalBodyBytes, request, objectMapper);
    }

    private byte[] buildModifiedBody(byte[] originalBodyBytes,
                                     HttpServletRequest request,
                                     ObjectMapper objectMapper) throws IOException {
        if (originalBodyBytes == null || originalBodyBytes.length == 0) {
            return originalBodyBytes;
        }

        Map<String, Object> bodyMap = objectMapper.readValue(
            originalBodyBytes,
            new TypeReference<Map<String, Object>>() {}
        );

        List<Map<String, Object>> variables = extractVariablesList(bodyMap);

        injectOrUpdateUrlsVariable(variables, request);

        bodyMap.put("variables", variables);
        return objectMapper.writeValueAsBytes(bodyMap);
    }

    private void injectOrUpdateUrlsVariable(List<Map<String, Object>> variables,
                                             HttpServletRequest request) {
        List<String> extraUrls = resolveExtraUrls(request);

        if (extraUrls.isEmpty()) {
            return;
        }

        Optional<Map<String, Object>> existingUrlsVar = variables.stream()
            .filter(v -> URLS_VARIABLE_NAME.equals(v.get("name")))
            .findFirst();

        if (existingUrlsVar.isPresent()) {
            Map<String, Object> urlsVar = existingUrlsVar.get();
            List<String> mergedUrls = mergeUrls(urlsVar, extraUrls);
            urlsVar.put("value", mergedUrls);
            log.debug("Updated existing '{}' variable. Total URLs: {}", 
                URLS_VARIABLE_NAME, mergedUrls.size());
        } else {
            Map<String, Object> newUrlsVar = new HashMap<>();
            newUrlsVar.put("name", URLS_VARIABLE_NAME);
            newUrlsVar.put("value", extraUrls);
            newUrlsVar.put("type", "json");
            variables.add(newUrlsVar);
            log.debug("Injected new '{}' variable with {} URLs", 
                URLS_VARIABLE_NAME, extraUrls.size());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> mergeUrls(Map<String, Object> urlsVar, List<String> extraUrls) {
        List<String> existingUrls = new ArrayList<>();
        Object value = urlsVar.get("value");

        if (value instanceof List) {
            ((List<?>) value).forEach(item -> {
                if (item instanceof String) {
                    existingUrls.add((String) item);
                }
            });
        } else if (value instanceof String) {
            try {
                List<String> parsed = new ObjectMapper()
                    .readValue((String) value, new TypeReference<List<String>>() {});
                existingUrls.addAll(parsed);
            } catch (Exception e) {
                log.warn("Could not parse '{}' value as JSON array, treating as single URL", 
                    URLS_VARIABLE_NAME);
                existingUrls.add((String) value);
            }
        }

        // Merge and deduplicate, preserving order — existing URLs first
        LinkedHashSet<String> merged = new LinkedHashSet<>(existingUrls);
        merged.addAll(extraUrls);
        return new ArrayList<>(merged);
    }

    private List<String> resolveExtraUrls(HttpServletRequest request) {
        List<String> urls = new ArrayList<>();

        // Example 1: from a custom request header (comma-separated)
        String urlHeader = request.getHeader("X-Inject-URLs");
        if (urlHeader != null && !urlHeader.isBlank()) {
            Arrays.stream(urlHeader.split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .forEach(urls::add);
        }

        // Example 2: hardcoded/config-driven URLs always injected
        urls.add("https://internal.mycompany.com/callback");
        urls.add("https://audit.mycompany.com/log");

        return urls;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractVariablesList(Map<String, Object> bodyMap) {
        Object existing = bodyMap.get("variables");
        if (existing instanceof List) {
            List<Map<String, Object>> copy = new ArrayList<>();
            ((List<?>) existing).forEach(item -> {
                if (item instanceof Map) {
                    copy.add(new HashMap<>((Map<String, Object>) item));
                }
            });
            return copy;
        }
        return new ArrayList<>();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(modifiedBody);
        return new ServletInputStream() {
            @Override public int read() { return byteArrayInputStream.read(); }
            @Override public boolean isFinished() { return byteArrayInputStream.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {}
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() { return modifiedBody.length; }

    @Override
    public long getContentLengthLong() { return modifiedBody.length; }
}

FilterConfig.java
java@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<FlowableVariableInjectingFilter> flowableVariableFilter(
            ObjectMapper objectMapper) {

        FilterRegistrationBean<FlowableVariableInjectingFilter> registration =
            new FilterRegistrationBean<>();

        registration.setFilter(new FlowableVariableInjectingFilter(objectMapper));
        registration.addUrlPatterns(
            "/process-api/runtime/process-instances",
            "/process-api/runtime/process-instances/*"
        );
        registration.setName("flowableVariableInjectingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

Summary of what each class does
ClassResponsibilityFlowableVariableInjectingFilterEntry point — guards on HTTP method, wraps request, handles errors safelyModifiedBodyRequestWrapperAll body mutation logic — parse, find/update/add urls, reserializeFilterConfigRegisters the filter restricted to specific Flowable URL patterns only