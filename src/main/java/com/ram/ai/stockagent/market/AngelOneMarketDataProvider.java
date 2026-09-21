package com.ram.ai.stockagent.market;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AngelOneMarketDataProvider implements MarketDataProvider {

    // =========================================================
    // ANGEL ONE API
    // =========================================================

    private static final String BASE_URL =
            "https://apiconnect.angelone.in";

    /*
     * Angel One public instrument master.
     *
     * IMPORTANT:
     * No stock token is hardcoded anywhere in this class.
     *
     * Stock tokens are obtained dynamically from this file.
     */
    private static final String SCRIPT_MASTER_URL =
            "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";

    /*
     * JWT session validity.
     *
     * The application reuses the JWT during this period.
     * If Angel One returns HTTP 401, the session is refreshed.
     */
    private static final long SESSION_VALIDITY_MILLIS =
            20L * 60L * 60L * 1000L;

    /*
     * Instrument master cache validity.
     *
     * We do not download the large instrument master
     * for every stock request.
     */
    private static final long SCRIPT_MASTER_VALIDITY_MILLIS =
            6L * 60L * 60L * 1000L;

    // =========================================================
    // SPRING / HTTP / JSON
    // =========================================================

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    // =========================================================
    // ANGEL ONE CONFIGURATION
    // =========================================================

    @Value("${angelone.api-key}")
    private String apiKey;

    @Value("${angelone.client-id}")
    private String clientId;

    @Value("${angelone.pin}")
    private String pin;

    @Value("${angelone.totp-secret}")
    private String totpSecret;

    // =========================================================
    // JWT SESSION
    // =========================================================

    private volatile String jwtToken;

    private volatile long jwtCreatedAt = 0L;

    private final Object authenticationLock =
            new Object();

    // =========================================================
    // DYNAMIC INSTRUMENT CACHE
    // =========================================================

    /*
     * Key:
     *     normalized stock symbol
     *
     * Example:
     *     INFY
     *     TCS
     *     RELIANCE
     *
     * Value:
     *     Angel One instrument information
     *
     * IMPORTANT:
     * Tokens are populated ONLY from Angel One's
     * instrument master.
     */
    private volatile Map<String, InstrumentInfo> instrumentCache =
            new HashMap<>();

    private volatile long scriptMasterLoadedAt = 0L;

    private final Object scriptMasterLock =
            new Object();
    

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AngelOneMarketDataProvider() {

        this.objectMapper =
                new ObjectMapper();

        this.httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
    }

    // =========================================================
    // LIVE MARKET QUOTE
    // =========================================================

    @Override
    public StockQuote getQuote(String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        try {

            authenticateIfRequired();

            /*
             * Resolve the stock dynamically from
             * Angel One instrument master.
             *
             * NO HARDCODED TOKEN.
             */
            InstrumentInfo instrument =
                    resolveInstrument(normalizedSymbol);

            System.out.println(
                    "===== ANGEL ONE LIVE QUOTE ====="
            );

            System.out.println(
                    "Requested Symbol : "
                            + normalizedSymbol
            );

            System.out.println(
                    "Trading Symbol   : "
                            + instrument.getTradingSymbol()
            );

            System.out.println(
                    "Exchange         : "
                            + instrument.getExchange()
            );

            System.out.println(
                    "Token            : "
                            + instrument.getToken()
            );

            Map<String, String> requestBody =
                    new HashMap<>();

            requestBody.put(
                    "exchange",
                    instrument.getExchange()
            );

            requestBody.put(
                    "tradingsymbol",
                    instrument.getTradingSymbol()
            );

            requestBody.put(
                    "symboltoken",
                    instrument.getToken()
            );

            String jsonBody =
                    objectMapper.writeValueAsString(
                            requestBody
                    );

            HttpRequest request =
                    buildAuthenticatedRequest(
                            "/rest/secure/angelbroking/order/v1/getLtpData",
                            jsonBody
                    );

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            /*
             * If JWT expired, authenticate again and retry once.
             */
            if (isAuthenticationFailure(response)) {

                System.out.println(
                        "Angel One session expired. "
                                + "Re-authenticating..."
                );

                invalidateSession();

                authenticateIfRequired();

                request =
                        buildAuthenticatedRequest(
                                "/rest/secure/angelbroking/order/v1/getLtpData",
                                jsonBody
                        );

                response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );
            }

            String responseBody =
                    response.body();

            System.out.println(
                    "Angel One LTP HTTP STATUS: "
                            + response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Angel One LTP HTTP error "
                                + response.statusCode()
                                + ": "
                                + responseBody
                );
            }

            if (responseBody == null
                    || responseBody.isBlank()) {

                throw new RuntimeException(
                        "Angel One returned an empty LTP response."
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            if (!root.path("status").asBoolean()) {

                throw new RuntimeException(
                        "Angel One market data failed: "
                                + root.path("message")
                                .asText("Unknown Angel One error")
                );
            }

            JsonNode data =
                    root.path("data");

            if (data.isMissingNode()
                    || data.isNull()
                    || !data.isObject()) {

                throw new RuntimeException(
                        "Angel One returned empty market data for "
                                + normalizedSymbol
                );
            }

            double ltp =
                    data.path("ltp").asDouble(0.0);

            double open =
                    data.path("open").asDouble(0.0);

            double high =
                    data.path("high").asDouble(0.0);

            double low =
                    data.path("low").asDouble(0.0);

            double close =
                    data.path("close").asDouble(0.0);

            /*
             * Do not treat an invalid response as
             * valid market data.
             */
            if (ltp <= 0.0) {

                throw new RuntimeException(
                        "Angel One returned an invalid LTP for "
                                + normalizedSymbol
                );
            }

            StockQuote quote =
                    new StockQuote(
                            normalizedSymbol,
                            data.path("exchange")
                                    .asText(
                                            instrument.getExchange()
                                    ),
                            data.path("tradingsymbol")
                                    .asText(
                                            instrument.getTradingSymbol()
                                    ),
                            ltp,
                            open,
                            high,
                            low,
                            close
                    );

            quote.setAvailable(true);

            quote.setErrorMessage(null);

            System.out.println(
                    "LTP : "
                            + quote.getLtp()
            );

            System.out.println(
                    "================================"
            );

            return quote;

        } catch (Exception e) {

            System.err.println(
                    "===== ANGEL ONE LIVE QUOTE ERROR ====="
            );

            System.err.println(
                    "Symbol : "
                            + normalizedSymbol
            );

            System.err.println(
                    "Error  : "
                            + e.getMessage()
            );

            System.err.println(
                    "======================================"
            );

            throw new RuntimeException(
                    "Failed to get Angel One quote for "
                            + normalizedSymbol
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // ANGEL ONE AUTHENTICATION
    // =========================================================

    private void authenticateIfRequired() {

        if (isSessionValid()) {
            return;
        }

        synchronized (authenticationLock) {

            if (isSessionValid()) {
                return;
            }

            authenticate();
        }
    }

    private boolean isSessionValid() {

        return jwtToken != null
                && !jwtToken.isBlank()
                && jwtCreatedAt > 0
                && (
                        System.currentTimeMillis()
                                - jwtCreatedAt
                ) < SESSION_VALIDITY_MILLIS;
    }

    private void invalidateSession() {

        synchronized (authenticationLock) {

            jwtToken = null;

            jwtCreatedAt = 0L;
        }
    }

    private void authenticate() {

        synchronized (authenticationLock) {

            if (isSessionValid()) {
                return;
            }

            try {

                System.out.println(
                        "===== ANGEL ONE AUTHENTICATION ====="
                );

                System.out.println(
                        "Authenticating with Angel One..."
                );

                /*
                 * TOTP is generated dynamically from
                 * the secret stored in application configuration.
                 *
                 * No TOTP token is hardcoded.
                 */
                String totp =
                        generateTotp(totpSecret);

                Map<String, String> body =
                        new HashMap<>();

                body.put(
                        "clientcode",
                        clientId
                );

                body.put(
                        "password",
                        pin
                );

                body.put(
                        "totp",
                        totp
                );

                String jsonBody =
                        objectMapper.writeValueAsString(
                                body
                        );

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                BASE_URL
                                                        + "/rest/auth/angelbroking/user/v1/loginByPassword"
                                        )
                                )
                                .header(
                                        "Content-Type",
                                        "application/json"
                                )
                                .header(
                                        "Accept",
                                        "application/json"
                                )
                                .header(
                                        "X-PrivateKey",
                                        apiKey
                                )
                                .header(
                                        "X-UserType",
                                        "USER"
                                )
                                .header(
                                        "X-SourceID",
                                        "WEB"
                                )
                                .header(
                                        "X-ClientLocalIP",
                                        "127.0.0.1"
                                )
                                .header(
                                        "X-ClientPublicIP",
                                        "127.0.0.1"
                                )
                                .header(
                                        "X-MACAddress",
                                        "00:00:00:00:00:00"
                                )
                                .POST(
                                        HttpRequest.BodyPublishers
                                                .ofString(jsonBody)
                                )
                                .build();

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                String responseBody =
                        response.body();

                System.out.println(
                        "Angel One login HTTP status: "
                                + response.statusCode()
                );

                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {

                    throw new RuntimeException(
                            "Angel One login HTTP error "
                                    + response.statusCode()
                                    + ": "
                                    + responseBody
                    );
                }

                if (responseBody == null
                        || responseBody.isBlank()) {

                    throw new RuntimeException(
                            "Angel One login returned empty response."
                    );
                }

                JsonNode root =
                        objectMapper.readTree(
                                responseBody
                        );

                if (!root.path("status").asBoolean()) {

                    throw new RuntimeException(
                            "Angel One authentication failed: "
                                    + root.path("message")
                                    .asText(
                                            "Unknown authentication error"
                                    )
                    );
                }

                String newJwtToken =
                        root.path("data")
                                .path("jwtToken")
                                .asText();

                if (newJwtToken == null
                        || newJwtToken.isBlank()) {

                    throw new RuntimeException(
                            "Angel One returned an empty JWT."
                    );
                }

                jwtToken =
                        newJwtToken;

                jwtCreatedAt =
                        System.currentTimeMillis();

                System.out.println(
                        "Angel One authentication SUCCESS"
                );

                System.out.println(
                        "JWT session stored and reused."
                );

                System.out.println(
                        "===================================="
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Angel One authentication failed: "
                                + e.getMessage(),
                        e
                );
            }
        }
    }

    // =========================================================
    // BUILD AUTHENTICATED REQUEST
    // =========================================================

    private HttpRequest buildAuthenticatedRequest(
            String endpoint,
            String jsonBody) {

        return HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                BASE_URL + endpoint
                        )
                )
                .header(
                        "Authorization",
                        "Bearer " + jwtToken
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .header(
                        "Accept",
                        "application/json"
                )
                .header(
                        "X-PrivateKey",
                        apiKey
                )
                .header(
                        "X-UserType",
                        "USER"
                )
                .header(
                        "X-SourceID",
                        "WEB"
                )
                .header(
                        "X-ClientLocalIP",
                        "127.0.0.1"
                )
                .header(
                        "X-ClientPublicIP",
                        "127.0.0.1"
                )
                .header(
                        "X-MACAddress",
                        "00:00:00:00:00:00"
                )
                .POST(
                        HttpRequest.BodyPublishers
                                .ofString(jsonBody)
                )
                .build();
    }

    // =========================================================
    // AUTHENTICATION FAILURE
    // =========================================================

    private boolean isAuthenticationFailure(
            HttpResponse<String> response) {

        if (response == null) {
            return false;
        }

        return response.statusCode() == 401;
    }

    // =========================================================
    // RESOLVE STOCK INSTRUMENT
    // =========================================================

    private InstrumentInfo resolveInstrument(
            String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        /*
         * First check the in-memory cache.
         */
        InstrumentInfo cached =
                instrumentCache.get(
                        normalizedSymbol
                );

        if (cached != null) {

            System.out.println(
                    "Using cached Angel One instrument: "
                            + normalizedSymbol
                            + " -> "
                            + cached.getToken()
            );

            return cached;
        }

        /*
         * Download the Angel One instrument master
         * only when necessary.
         */
        loadScriptMasterIfRequired();

        InstrumentInfo instrument =
                instrumentCache.get(
                        normalizedSymbol
                );

        if (instrument != null) {

            System.out.println(
                    "Resolved Angel One instrument: "
                            + normalizedSymbol
                            + " -> "
                            + instrument.getToken()
            );

            return instrument;
        }

        /*
         * One extra refresh can help if the cache was
         * loaded earlier and the requested symbol was
         * not present at that time.
         */
        synchronized (scriptMasterLock) {

            loadScriptMaster();

            instrument =
                    instrumentCache.get(
                            normalizedSymbol
                    );
        }

        if (instrument == null) {

            throw new IllegalArgumentException(
                    "Angel One NSE equity instrument not found for symbol: "
                            + normalizedSymbol
            );
        }

        return instrument;
    }

    // =========================================================
    // LOAD SCRIPT MASTER IF REQUIRED
    // =========================================================

    private void loadScriptMasterIfRequired() {

        if (isScriptMasterValid()) {
            return;
        }

        synchronized (scriptMasterLock) {

            if (isScriptMasterValid()) {
                return;
            }

            loadScriptMaster();
        }
    }

    // =========================================================
    // CHECK SCRIPT MASTER CACHE
    // =========================================================

    private boolean isScriptMasterValid() {

        return scriptMasterLoadedAt > 0L
                && instrumentCache != null
                && !instrumentCache.isEmpty()
                && (
                        System.currentTimeMillis()
                                - scriptMasterLoadedAt
                ) < SCRIPT_MASTER_VALIDITY_MILLIS;
    }

    // =========================================================
    // LOAD ANGEL ONE SCRIPT MASTER
    // =========================================================

    private void loadScriptMaster() {

        try {

            System.out.println(
                    "===== ANGEL ONE INSTRUMENT MASTER ====="
            );

            System.out.println(
                    "Downloading Angel One instrument master..."
            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            SCRIPT_MASTER_URL
                                    )
                            )
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Instrument master HTTP status: "
                            + response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Angel One instrument master HTTP error: "
                                + response.statusCode()
                );
            }

            String responseBody =
                    response.body();

            if (responseBody == null
                    || responseBody.isBlank()) {

                throw new RuntimeException(
                        "Angel One instrument master returned empty data."
                );
            }

            /*
             * Remove UTF-8 BOM if the downloaded file contains one.
             */
            if (!responseBody.isEmpty()
                    && responseBody.charAt(0) == '\uFEFF') {

                responseBody =
                        responseBody.substring(1);
            }

            responseBody =
                    responseBody.trim();

            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            if (!root.isArray()) {

                throw new RuntimeException(
                        "Angel One instrument master response is not an array."
                );
            }

            System.out.println(
                    "Instrument master records received: "
                            + root.size()
            );

            /*
             * Temporary map.
             *
             * We replace the existing cache only after
             * successful processing.
             */
            Map<String, InstrumentInfo> newInstrumentMap =
                    new HashMap<>();

            int totalRecords = 0;

            int nseRecords = 0;

            int equityRecords = 0;

            int usableRecords = 0;

            for (JsonNode instrument : root) {

                totalRecords++;

                if (instrument == null
                        || !instrument.isObject()) {

                    continue;
                }

                String exchange =
                        readText(
                                instrument,
                                "exch_seg"
                        );

                if (exchange == null
                        || !"NSE".equalsIgnoreCase(
                                exchange.trim()
                        )) {

                    continue;
                }

                nseRecords++;

                String token =
                        readText(
                                instrument,
                                "token"
                        );

                String tradingSymbol =
                        readText(
                                instrument,
                                "symbol"
                        );

                String name =
                        readText(
                                instrument,
                                "name"
                        );

                String instrumentType =
                        readText(
                                instrument,
                                "instrumenttype"
                        );

                /*
                 * Token is mandatory.
                 */
                if (token == null
                        || token.isBlank()) {

                    continue;
                }

                /*
                 * Trading symbol is mandatory.
                 */
                if (tradingSymbol == null
                        || tradingSymbol.isBlank()) {

                    continue;
                }

                tradingSymbol =
                        tradingSymbol.trim();

                /*
                 * We want normal NSE equity symbols.
                 *
                 * Angel One's master generally identifies
                 * equity instruments with -EQ.
                 *
                 * We intentionally do NOT rely only on
                 * instrumenttype because the master format
                 * can vary between datasets.
                 */
                boolean looksLikeEquity =
                        tradingSymbol
                                .toUpperCase()
                                .endsWith("-EQ")
                        || (
                                instrumentType != null
                                && (
                                        "EQ".equalsIgnoreCase(
                                                instrumentType.trim()
                                        )
                                        || "EQUITY".equalsIgnoreCase(
                                                instrumentType.trim()
                                        )
                                )
                        );

                if (!looksLikeEquity) {
                    continue;
                }

                equityRecords++;

                InstrumentInfo info =
                        new InstrumentInfo(
                                exchange.trim().toUpperCase(),
                                tradingSymbol,
                                token.trim(),
                                name
                        );

                /*
                 * Example:
                 *
                 * Angel One:
                 *     symbol = INFY-EQ
                 *
                 * Application key:
                 *     INFY
                 */
                String baseSymbol =
                        removeEqSuffix(
                                tradingSymbol
                        );

                if (!baseSymbol.isBlank()) {

                    newInstrumentMap.put(
                            baseSymbol,
                            info
                    );
                }

                /*
                 * Also store the exact trading symbol.
                 *
                 * This allows direct resolution of:
                 * INFY-EQ
                 */
                newInstrumentMap.put(
                        normalizeSymbol(
                                tradingSymbol
                        ),
                        info
                );

                /*
                 * Store the company name as an additional
                 * lookup key when available.
                 *
                 * Example:
                 * Infosys Limited -> INFY instrument
                 *
                 * This does NOT replace natural-language
                 * processing. It is simply an additional
                 * instrument lookup key.
                 */
                if (name != null
                        && !name.isBlank()) {

                    String normalizedName =
                            normalizeSymbol(name);

                    if (!normalizedName.isBlank()) {

                        newInstrumentMap.putIfAbsent(
                                normalizedName,
                                info
                        );
                    }
                }

                usableRecords++;
            }

            /*
             * IMPORTANT:
             *
             * If Angel One returned an array but our parser
             * found zero usable instruments, fail clearly.
             */
            if (newInstrumentMap.isEmpty()) {

                throw new RuntimeException(
                        "No usable NSE equity instruments found "
                                + "in Angel One instrument master. "
                                + "Total records="
                                + totalRecords
                                + ", NSE records="
                                + nseRecords
                                + ", equity records="
                                + equityRecords
                );
            }

            /*
             * Replace cache atomically.
             */
            instrumentCache =
                    newInstrumentMap;

            scriptMasterLoadedAt =
                    System.currentTimeMillis();

            System.out.println(
                    "===== ANGEL ONE INSTRUMENT MASTER SUCCESS ====="
            );

            System.out.println(
                    "Total records       : "
                            + totalRecords
            );

            System.out.println(
                    "NSE records         : "
                            + nseRecords
            );

            System.out.println(
                    "NSE equity records  : "
                            + equityRecords
            );

            System.out.println(
                    "Usable instruments  : "
                            + usableRecords
            );

            System.out.println(
                    "Cached lookup keys  : "
                            + newInstrumentMap.size()
            );

            /*
             * Verify the requested common case if present.
             * This is only diagnostic; there is no hardcoded
             * token here.
             */
            InstrumentInfo infy =
                    newInstrumentMap.get("INFY");

            if (infy != null) {

                System.out.println(
                        "INFY resolved dynamically: "
                                + infy.getTradingSymbol()
                                + " -> "
                                + infy.getToken()
                );
            }

            System.out.println(
                    "Angel One instrument master loaded successfully."
            );

            System.out.println(
                    "================================================"
            );

        } catch (Exception e) {

            System.err.println(
                    "===== ANGEL ONE INSTRUMENT MASTER ERROR ====="
            );

            System.err.println(
                    e.getMessage()
            );

            System.err.println(
                    "=============================================="
            );

            throw new RuntimeException(
                    "Unable to load Angel One instrument master: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // READ JSON TEXT SAFELY
    // =========================================================

    private String readText(
            JsonNode node,
            String fieldName) {

        if (node == null
                || fieldName == null) {

            return null;
        }

        JsonNode value =
                node.get(fieldName);

        if (value == null
                || value.isNull()
                || !value.isValueNode()) {

            return null;
        }

        String text =
                value.asText();

        if (text == null
                || text.isBlank()) {

            return null;
        }

        return text.trim();
    }

    // =========================================================
    // REMOVE -EQ SUFFIX
    // =========================================================

    private String removeEqSuffix(
            String tradingSymbol) {

        if (tradingSymbol == null) {
            return "";
        }

        String value =
                tradingSymbol.trim().toUpperCase();

        if (value.endsWith("-EQ")) {

            return value.substring(
                    0,
                    value.length() - 3
            );
        }

        return value;
    }

    // =========================================================
    // SYMBOL NORMALIZATION
    // =========================================================

    private String normalizeSymbol(
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Stock symbol cannot be empty."
            );
        }

        String normalized =
                symbol.trim()
                        .toUpperCase();

        /*
         * Remove common punctuation around the symbol.
         */
        normalized =
                normalized
                        .replace("\"", "")
                        .replace("'", "")
                        .trim();

        /*
         * If user/tool gives:
         *
         * INFY-EQ
         *
         * internally use:
         *
         * INFY
         */
        if (normalized.endsWith("-EQ")) {

            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 3
                    );
        }

        return normalized;
    }

    // =========================================================
    // TOTP GENERATION
    // =========================================================

    private String generateTotp(
            String secret)
            throws Exception {

        if (secret == null
                || secret.isBlank()) {

            throw new IllegalArgumentException(
                    "Angel One TOTP secret is not configured."
            );
        }

        byte[] key =
                base32Decode(secret);

        long timeStep =
                Instant.now().getEpochSecond()
                        / 30L;

        byte[] data =
                ByteBuffer
                        .allocate(8)
                        .putLong(timeStep)
                        .array();

        Mac mac =
                Mac.getInstance(
                        "HmacSHA1"
                );

        mac.init(
                new SecretKeySpec(
                        key,
                        "HmacSHA1"
                )
        );

        byte[] hash =
                mac.doFinal(data);

        int offset =
                hash[hash.length - 1]
                        & 0x0F;

        int binary =
                ((hash[offset] & 0x7F) << 24)
                        | ((hash[offset + 1] & 0xFF) << 16)
                        | ((hash[offset + 2] & 0xFF) << 8)
                        | (hash[offset + 3] & 0xFF);

        int otp =
                binary % 1_000_000;

        return String.format(
                "%06d",
                otp
        );
    }

    // =========================================================
    // BASE32 DECODER
    // =========================================================

    private byte[] base32Decode(
            String value) {

        String alphabet =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        String normalized =
                value
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("=", "")
                        .toUpperCase();

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        normalized.length() * 5 / 8 + 1
                );

        int bits = 0;

        int valueBuffer = 0;

        for (char c :
                normalized.toCharArray()) {

            int index =
                    alphabet.indexOf(c);

            if (index < 0) {

                throw new IllegalArgumentException(
                        "Invalid Base32 character in TOTP secret."
                );
            }

            valueBuffer =
                    (valueBuffer << 5)
                            | index;

            bits += 5;

            if (bits >= 8) {

                bits -= 8;

                buffer.put(
                        (byte)
                                (
                                        (valueBuffer >> bits)
                                                & 0xFF
                                )
                );
            }
        }

        byte[] result =
                new byte[buffer.position()];

        buffer.flip();

        buffer.get(result);

        return result;
    }

    // =========================================================
    // HISTORICAL MARKET DATA
    // =========================================================

    @Override
    public List<Candle> getHistoricalData(
            String symbol,
            String interval,
            LocalDateTime from,
            LocalDateTime to) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        List<Candle> candles =
                new ArrayList<>();

        try {

            if (from == null) {

                throw new IllegalArgumentException(
                        "Historical data 'from' date cannot be null."
                );
            }

            if (to == null) {

                throw new IllegalArgumentException(
                        "Historical data 'to' date cannot be null."
                );
            }

            if (from.isAfter(to)) {

                throw new IllegalArgumentException(
                        "Historical data 'from' date cannot be after 'to' date."
                );
            }

            authenticateIfRequired();

            /*
             * Resolve token dynamically from Angel One
             * instrument master.
             */
            InstrumentInfo instrument =
                    resolveInstrument(
                            normalizedSymbol
                    );

            String normalizedInterval =
                    normalizeInterval(interval);

            System.out.println(
                    "===== ANGEL ONE HISTORICAL REQUEST ====="
            );

            System.out.println(
                    "Symbol         : "
                            + normalizedSymbol
            );

            System.out.println(
                    "Trading Symbol : "
                            + instrument.getTradingSymbol()
            );

            System.out.println(
                    "Token          : "
                            + instrument.getToken()
            );

            System.out.println(
                    "Interval       : "
                            + normalizedInterval
            );

            System.out.println(
                    "From           : "
                            + from
            );

            System.out.println(
                    "To             : "
                            + to
            );

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm"
                    );

            Map<String, String> body =
                    new HashMap<>();

            body.put(
                    "exchange",
                    instrument.getExchange()
            );

            body.put(
                    "symboltoken",
                    instrument.getToken()
            );

            body.put(
                    "interval",
                    normalizedInterval
            );

            body.put(
                    "fromdate",
                    from.format(formatter)
            );

            body.put(
                    "todate",
                    to.format(formatter)
            );

            String jsonBody =
                    objectMapper.writeValueAsString(
                            body
                    );

            HttpRequest request =
                    buildAuthenticatedRequest(
                            "/rest/secure/angelbroking/historical/v1/getCandleData",
                            jsonBody
                    );

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            /*
             * Retry once if JWT expired.
             */
            if (isAuthenticationFailure(response)) {

                System.out.println(
                        "Angel One session expired. "
                                + "Re-authenticating once..."
                );

                invalidateSession();

                authenticateIfRequired();

                request =
                        buildAuthenticatedRequest(
                                "/rest/secure/angelbroking/historical/v1/getCandleData",
                                jsonBody
                        );

                response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );
            }

            String responseBody =
                    response.body();

            System.out.println(
                    "Historical API HTTP STATUS: "
                            + response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Angel One historical API HTTP "
                                + response.statusCode()
                                + ": "
                                + responseBody
                );
            }

            if (responseBody == null
                    || responseBody.isBlank()) {

                throw new RuntimeException(
                        "Angel One returned an empty historical response."
                );
            }

            String trimmedResponse =
                    responseBody.trim();

            if (!trimmedResponse.startsWith("{")) {

                throw new RuntimeException(
                        "Angel One returned a non-JSON historical response: "
                                + trimmedResponse
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            trimmedResponse
                    );

            if (!root.path("status").asBoolean()) {

                throw new RuntimeException(
                        "Angel One historical data failed: "
                                + root.path("message")
                                .asText(
                                        "Unknown Angel One historical data error"
                                )
                );
            }

            JsonNode data =
                    root.path("data");

            if (data.isMissingNode()
                    || data.isNull()
                    || !data.isArray()) {

                System.out.println(
                        "Angel One returned no candle array."
                );

                return candles;
            }

            for (JsonNode candleNode :
                    data) {

                if (candleNode == null
                        || !candleNode.isArray()
                        || candleNode.size() < 6) {

                    continue;
                }

                try {

                    String timestampText =
                            candleNode
                                    .get(0)
                                    .asText();

                    if (timestampText == null
                            || timestampText.isBlank()) {

                        continue;
                    }

                    timestampText =
                            timestampText
                                    .replace(
                                            "T",
                                            " "
                                    )
                                    .trim();

                    /*
                     * Angel One normally returns:
                     *
                     * 2026-08-24T09:15:00+05:30
                     *
                     * We only need LocalDateTime.
                     */
                    if (timestampText.length() > 16) {

                        timestampText =
                                timestampText.substring(
                                        0,
                                        16
                                );
                    }

                    LocalDateTime timestamp =
                            LocalDateTime.parse(
                                    timestampText,
                                    formatter
                            );

                    double open =
                            candleNode
                                    .get(1)
                                    .asDouble();

                    double high =
                            candleNode
                                    .get(2)
                                    .asDouble();

                    double low =
                            candleNode
                                    .get(3)
                                    .asDouble();

                    double close =
                            candleNode
                                    .get(4)
                                    .asDouble();

                    long volume =
                            candleNode
                                    .get(5)
                                    .asLong();

                    candles.add(
                            new Candle(
                                    timestamp,
                                    open,
                                    high,
                                    low,
                                    close,
                                    volume
                            )
                    );

                } catch (Exception candleException) {

                    /*
                     * Do not destroy the complete historical
                     * response because one candle is malformed.
                     */
                    System.err.println(
                            "Skipping malformed candle: "
                                    + candleException.getMessage()
                    );
                }
            }

            System.out.println(
                    "Historical candles received: "
                            + candles.size()
            );

            System.out.println(
                    "=========================================="
            );

            return candles;

        } catch (Exception e) {

            System.err.println(
                    "===== HISTORICAL DATA ERROR ====="
            );

            System.err.println(
                    "Symbol: "
                            + normalizedSymbol
            );

            System.err.println(
                    "Error: "
                            + e.getMessage()
            );

            System.err.println(
                    "================================="
            );

            /*
             * Important:
             *
             * Return an empty list instead of crashing the
             * entire AI request.
             *
             * Your analysis engine can then decide how to
             * handle missing historical data.
             */
            return candles;
        }
    }

    // =========================================================
    // NORMALIZE ANGEL ONE INTERVAL
    // =========================================================

    private String normalizeInterval(
            String interval) {

        if (interval == null
                || interval.isBlank()) {

            return "ONE_DAY";
        }

        String value =
                interval
                        .trim()
                        .toUpperCase();

        return switch (value) {

            case "1MIN",
                    "1_MINUTE",
                    "ONE_MINUTE" ->
                    "ONE_MINUTE";

            case "3MIN",
                    "3_MINUTE",
                    "THREE_MINUTE" ->
                    "THREE_MINUTE";

            case "5MIN",
                    "5_MINUTE",
                    "FIVE_MINUTE" ->
                    "FIVE_MINUTE";

            case "10MIN",
                    "10_MINUTE",
                    "TEN_MINUTE" ->
                    "TEN_MINUTE";

            case "15MIN",
                    "15_MINUTE",
                    "FIFTEEN_MINUTE" ->
                    "FIFTEEN_MINUTE";

            case "30MIN",
                    "30_MINUTE",
                    "THIRTY_MINUTE" ->
                    "THIRTY_MINUTE";

            case "1H",
                    "1HR",
                    "ONE_HOUR" ->
                    "ONE_HOUR";

            case "1D",
                    "DAY",
                    "DAILY",
                    "ONE_DAY" ->
                    "ONE_DAY";

            case "1W",
                    "WEEK",
                    "WEEKLY",
                    "ONE_WEEK" ->
                    "ONE_WEEK";

            default ->
                    value;
        };
    }

    // =========================================================
    // INSTRUMENT INFORMATION
    // =========================================================

    private static final class InstrumentInfo {

        private final String exchange;

        private final String tradingSymbol;

        private final String token;

        private final String name;

        private InstrumentInfo(
                String exchange,
                String tradingSymbol,
                String token,
                String name) {

            this.exchange =
                    exchange;

            this.tradingSymbol =
                    tradingSymbol;

            this.token =
                    token;

            this.name =
                    name;
        }

        private String getExchange() {

            return exchange;
        }

        private String getTradingSymbol() {

            return tradingSymbol;
        }

        private String getToken() {

            return token;
        }

        @SuppressWarnings("unused")
        private String getName() {

            return name;
        }
    }
}