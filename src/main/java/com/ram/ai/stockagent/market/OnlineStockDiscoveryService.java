package com.ram.ai.stockagent.market;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class OnlineStockDiscoveryService {

    private static final String NIFTY_INDICES_BASE_URL =
            "https://www.niftyindices.com/IndexConstituent/";

    private final HttpClient httpClient;

    public OnlineStockDiscoveryService() {

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(20)
                        )
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();
    }

    // =========================================================
    // PUBLIC METHOD
    // =========================================================

    public List<String> discoverStocks(String sector) {

        if (sector == null
                || sector.trim().isEmpty()) {

            sector = "ALL";
        }

        String normalized =
                sector.trim()
                        .toUpperCase();

        if ("ALL".equals(normalized)) {

            return discoverAll();
        }

        String fileName =
                getConstituentFile(normalized);

        if (fileName == null) {

            System.err.println(
                    "No online constituent mapping found for: "
                            + normalized
            );

            return Collections.emptyList();
        }

        return downloadConstituents(fileName);
    }

    // =========================================================
    // SECTOR -> ONLINE NIFTY CONSTITUENT FILE
    // =========================================================

    private String getConstituentFile(
            String sector) {

        return switch (sector) {

            case "BANK",
                    "BANKING",
                    "NIFTY BANK" ->
                    "ind_niftybanklist.csv";

            case "IT",
                    "TECHNOLOGY",
                    "INFORMATION TECHNOLOGY",
                    "NIFTY IT" ->
                    "ind_niftyitlist.csv";

            case "PHARMA",
                    "PHARMACEUTICAL",
                    "PHARMACEUTICALS",
                    "NIFTY PHARMA" ->
                    "ind_niftypharmalist.csv";

            case "AUTO",
                    "AUTOMOBILE",
                    "AUTOMOTIVE",
                    "NIFTY AUTO" ->
                    "ind_niftyautolist.csv";

            case "FMCG",
                    "CONSUMER",
                    "NIFTY FMCG" ->
                    "ind_niftyfmcglist.csv";

            case "METAL",
                    "METALS",
                    "NIFTY METAL" ->
                    "ind_niftymetallist.csv";

            case "MEDIA",
                    "NIFTY MEDIA" ->
                    "ind_niftymedialist.csv";

            case "REALTY",
                    "REAL ESTATE",
                    "NIFTY REALTY" ->
                    "ind_niftyrealtylist.csv";

            case "PSU BANK",
                    "PSUBANK",
                    "NIFTY PSU BANK" ->
                    "ind_niftypsubanklist.csv";

            case "PRIVATE BANK",
                    "PRIVATEBANK",
                    "NIFTY PRIVATE BANK" ->
                    "ind_nifty_privatebanklist.csv";

            case "FINANCE",
                    "FINANCIAL SERVICES",
                    "NIFTY FINANCIAL SERVICES" ->
                    "ind_niftyfinancelist.csv";

            case "ENERGY",
                    "NIFTY ENERGY" ->
                    "ind_niftyenergylist.csv";

            default ->
                    null;
        };
    }

    // =========================================================
    // DOWNLOAD ONLINE CONSTITUENTS
    // =========================================================

    private List<String> downloadConstituents(
            String fileName) {

        String url =
                NIFTY_INDICES_BASE_URL
                        + fileName;

        System.out.println(
                "========================================"
        );

        System.out.println(
                "ONLINE STOCK DISCOVERY"
        );

        System.out.println(
                "Source: Nifty Indices"
        );

        System.out.println(
                "URL: " + url
        );

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .timeout(
                                    Duration.ofSeconds(20)
                            )
                            .header(
                                    "User-Agent",
                                    getUserAgent()
                            )
                            .header(
                                    "Accept",
                                    "text/csv,"
                                            + "text/plain,"
                                            + "*/*"
                            )
                            .header(
                                    "Accept-Language",
                                    "en-US,en;q=0.9"
                            )
                            .header(
                                    "Referer",
                                    "https://www.niftyindices.com/"
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString(
                                            StandardCharsets.UTF_8
                                    )
                    );

            System.out.println(
                    "HTTP Status: "
                            + response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                System.err.println(
                        "Online constituent request failed."
                );

                System.err.println(
                        "HTTP Status: "
                                + response.statusCode()
                );

                return Collections.emptyList();
            }

            return parseCsv(
                    response.body()
            );

        } catch (Exception e) {

            System.err.println(
                    "Online stock discovery failed: "
                            + e.getClass()
                                    .getSimpleName()
                            + " - "
                            + e.getMessage()
            );

            return Collections.emptyList();
        }
    }

    // =========================================================
    // CSV PARSER
    // =========================================================

    private List<String> parseCsv(
            String csv) {

        if (csv == null
                || csv.isBlank()) {

            System.err.println(
                    "Online constituent response is empty."
            );

            return Collections.emptyList();
        }

        Set<String> symbols =
                new LinkedHashSet<>();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new java.io.ByteArrayInputStream(
                                                csv.getBytes(
                                                        StandardCharsets.UTF_8
                                                )
                                        ),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;

            boolean headerFound = false;

            while ((line = reader.readLine())
                    != null) {

                line =
                        line.replace(
                                "\uFEFF",
                                ""
                        ).trim();

                if (line.isEmpty()) {
                    continue;
                }

                /*
                 * First line is normally:
                 *
                 * Company Name,Industry,Symbol,Series,ISIN Code
                 */
                if (!headerFound) {

                    headerFound = true;

                    continue;
                }

                String[] columns =
                        splitCsvLine(line);

                if (columns.length == 0) {
                    continue;
                }

                /*
                 * Nifty constituent CSV normally contains
                 * Symbol in the third column.
                 *
                 * We also search the row defensively so
                 * minor CSV format changes don't break
                 * discovery.
                 */
                String symbol =
                        extractSymbol(columns);

                if (symbol == null
                        || symbol.isBlank()) {

                    continue;
                }

                symbol =
                        normalizeSymbol(
                                symbol
                        );

                if (!symbol.isBlank()) {

                    symbols.add(symbol);
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "CSV parsing failed: "
                            + e.getMessage()
            );

            return Collections.emptyList();
        }

        List<String> result =
                new ArrayList<>(
                        symbols
                );

        System.out.println(
                "Online candidates discovered: "
                        + result.size()
        );

        if (!result.isEmpty()) {

            System.out.println(
                    "Candidates:"
            );

            result.stream()
                    .limit(20)
                    .forEach(
                            symbol ->
                                    System.out.println(
                                            "  "
                                                    + symbol
                                    )
                    );
        }

        System.out.println(
                "========================================"
        );

        return result;
    }

    // =========================================================
    // SYMBOL EXTRACTION
    // =========================================================

    private String extractSymbol(
            String[] columns) {

        /*
         * Current Nifty constituent files normally have:
         *
         * 0 = Company Name
         * 1 = Industry
         * 2 = Symbol
         * 3 = Series
         * 4 = ISIN Code
         */

        if (columns.length >= 3) {

            String symbol =
                    cleanCsvValue(
                            columns[2]
                    );

            if (looksLikeSymbol(symbol)) {

                return symbol;
            }
        }

        /*
         * Defensive fallback.
         */
        for (String column : columns) {

            String value =
                    cleanCsvValue(
                            column
                    );

            if (looksLikeSymbol(value)) {

                return value;
            }
        }

        return null;
    }

    // =========================================================
    // CSV SPLITTER
    // =========================================================

    private String[] splitCsvLine(
            String line) {

        List<String> values =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean insideQuotes =
                false;

        for (int i = 0;
             i < line.length();
             i++) {

            char c =
                    line.charAt(i);

            if (c == '"') {

                insideQuotes =
                        !insideQuotes;

            } else if (c == ','
                    && !insideQuotes) {

                values.add(
                        current.toString()
                );

                current.setLength(0);

            } else {

                current.append(c);
            }
        }

        values.add(
                current.toString()
        );

        return values.toArray(
                new String[0]
        );
    }

    // =========================================================
    // SYMBOL VALIDATION
    // =========================================================
    private String cleanCsvValue(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\uFEFF", "")
                .replace("\"", "")
                .replace("'", "")
                .trim();
    }
    private boolean looksLikeSymbol(
            String value) {

        if (value == null
                || value.isBlank()) {

            return false;
        }

        String normalized =
                value.trim()
                        .toUpperCase();

        if (normalized.equals(
                "SYMBOL"
        )) {

            return false;
        }

        if (normalized.equals(
                "COMPANY NAME"
        )) {

            return false;
        }

        if (normalized.equals(
                "INDUSTRY"
        )) {

            return false;
        }

        if (normalized.equals(
                "SERIES"
        )) {

            return false;
        }

        if (normalized.equals(
                "ISIN CODE"
        )) {

            return false;
        }

        /*
         * Exclude obvious company-name fields.
         */
        if (normalized.contains(
                " LIMITED"
        )
                || normalized.contains(
                " LTD"
        )
                || normalized.contains(
                " PRIVATE"
        )) {

            return false;
        }

        return normalized.matches(
                "[A-Z0-9&._-]{2,30}"
        );
    }

    // =========================================================
    // CLEAN SYMBOL
    // =========================================================

    private String normalizeSymbol(
            String symbol) {

        if (symbol == null) {

            return "";
        }

        String normalized =
                symbol.trim()
                        .toUpperCase()
                        .replace(
                                "\"",
                                ""
                        )
                        .replace(
                                "'",
                                ""
                        )
                        .trim();

        /*
         * Angel One expects the equity symbol
         * and its existing provider resolves it
         * against the Angel One instrument master.
         */
        if (normalized.endsWith(
                "-EQ"
        )) {

            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 3
                    );
        }

        return normalized;
    }

    // =========================================================
    // ALL ONLINE SECTORS
    // =========================================================

    private List<String> discoverAll() {

        Set<String> all =
                new LinkedHashSet<>();

        String[] sectors = {

                "IT",

                "BANKING",

                "PHARMA",

                "AUTO",

                "FMCG",

                "METAL",

                "MEDIA",

                "REALTY",

                "PSU BANK",

                "PRIVATE BANK",

                "FINANCE",

                "ENERGY"
        };

        for (String sector : sectors) {

            List<String> stocks =
                    discoverStocks(
                            sector
                    );

            all.addAll(
                    stocks
            );
        }

        return new ArrayList<>(
                all
        );
    }

    // =========================================================
    // USER AGENT
    // =========================================================

    private String getUserAgent() {

        return "Mozilla/5.0 "
                + "(Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 "
                + "(KHTML, like Gecko) "
                + "Chrome/141.0.0.0 "
                + "Safari/537.36";
    }
}