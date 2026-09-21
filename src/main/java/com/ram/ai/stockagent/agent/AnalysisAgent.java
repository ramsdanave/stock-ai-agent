package com.ram.ai.stockagent.agent;

import com.ram.ai.stockagent.tools.StockAnalysisTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AnalysisAgent {

    private final ChatClient chatClient;

    private final StockAnalysisTools stockAnalysisTools;

    public AnalysisAgent(
            ChatClient.Builder chatClientBuilder,
            StockAnalysisTools stockAnalysisTools) {

        this.chatClient = chatClientBuilder.build();

        this.stockAnalysisTools = stockAnalysisTools;
    }

    public String analyze(String query) {

        String result = chatClient.prompt()

                .system("""
                        You are the Analysis Agent of MarketAI.

                        Your ONLY responsibility is stock and technical
                        analysis using the available application tools.

                        STRICT RULES:

                        1. Always use the appropriate application tools
                           when live market or technical information
                           is requested.

                        2. Never invent:

                           - stock prices
                           - RSI values
                           - MACD values
                           - moving averages
                           - volume
                           - technical scores
                           - market data

                        3. Use ONLY information returned by the
                           application tools.

                        4. The analyzeStock tool returns a structured
                           result containing:

                           - status
                           - symbol
                           - message
                           - analysis

                        5. If the tool returns:

                           status = SUCCESS

                           use the returned analysis object.

                        6. If the tool returns:

                           status = FAILED

                           do NOT invent, estimate or calculate
                           replacement technical values.

                           Return exactly:

                           NO_ANALYSIS_RESULT

                        7. Do not substitute general financial knowledge
                           when live tool data is required.

                        8. Do not make guaranteed investment predictions.

                        9. If a tool fails because live or historical
                           market data is unavailable, treat the failure
                           as a normal unavailable-data condition.

                        10. Never expose internal Java exceptions,
                            stack traces or JSON parsing errors to the user.

                        Return concise factual analysis that another
                        MarketAI agent can use.
                        """)

                .user(query)

                .tools(stockAnalysisTools)

                .call()

                .content();

        if (result == null || result.isBlank()) {

            return "NO_ANALYSIS_RESULT";
        }

        return result;
    }
}