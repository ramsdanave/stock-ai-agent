const chatArea = document.getElementById("chatArea");
const welcome = document.getElementById("welcome");
const messageInput = document.getElementById("messageInput");
const sendButton = document.getElementById("sendButton");
const newChatButton = document.getElementById("newChatButton");

// =========================================================
// BACKEND URL
// =========================================================
//
// Because the UI is inside the Spring Boot application,
// always send API requests to Spring Boot on port 8080.
//
// =========================================================

const API_URL = "http://localhost:8080";

let conversationId = "web-" + Date.now();


// =========================================================
// SEND MESSAGE
// =========================================================

async function sendMessage(message) {

    message = message.trim();

    if (!message) {
        return;
    }

    // Remove welcome screen
    if (welcome) {
        welcome.remove();
    }

    // Show user message
    addMessage(message, "user");

    // Clear input
    messageInput.value = "";
    autoResize();

    // Disable send button
    sendButton.disabled = true;

    // Typing indicator
    const typingElement = addTypingIndicator();

    try {

        console.log("Sending request to:", API_URL + "/api/chat");
        console.log("Message:", message);

        const response = await fetch(
            API_URL + "/api/chat",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },

                body: JSON.stringify({
                    message: message
                })
            }
        );

        console.log("Backend HTTP status:", response.status);

        if (!response.ok) {

            let errorText = "";

            try {
                errorText = await response.text();
            } catch (e) {
                errorText = "";
            }

            throw new Error(
                "Backend returned HTTP " +
                response.status +
                (errorText ? " - " + errorText : "")
            );
        }

        const data = await response.json();

        console.log("Backend response:", data);

        // Remove typing indicator
        typingElement.remove();

        // Display AI response
        addMessage(
            data.response ||
            "I couldn't generate a response.",
            "ai"
        );

    } catch (error) {

        console.error("StockAI backend error:", error);

        typingElement.remove();

        addMessage(
            "Unable to connect to the StockAI backend.\n\n" +
            "Backend: " + API_URL + "\n" +
            "Endpoint: /api/chat\n\n" +
            "Error: " + error.message,
            "ai"
        );

    } finally {

        sendButton.disabled = false;

        messageInput.focus();
    }
}


// =========================================================
// ADD MESSAGE
// =========================================================

function addMessage(text, type) {

    const row = document.createElement("div");

    row.className =
        "message-row " + type;


    const avatar = document.createElement("div");

    avatar.className =
        "avatar " +
        (type === "ai" ? "ai" : "user");


    avatar.textContent =
        type === "ai"
            ? "🤖"
            : "👤";


    const message = document.createElement("div");

    message.className =
        "message " + type;


    message.textContent = text;


    if (type === "ai") {

        row.appendChild(avatar);
        row.appendChild(message);

    } else {

        row.appendChild(message);
        row.appendChild(avatar);
    }


    chatArea.appendChild(row);

    scrollToBottom();
}


// =========================================================
// TYPING INDICATOR
// =========================================================

function addTypingIndicator() {

    const row = document.createElement("div");

    row.className =
        "message-row ai";


    const avatar = document.createElement("div");

    avatar.className =
        "avatar ai";

    avatar.textContent = "🤖";


    const message = document.createElement("div");

    message.className =
        "message ai";


    const typing = document.createElement("div");

    typing.className = "typing";


    for (let i = 0; i < 3; i++) {

        const dot = document.createElement("span");

        typing.appendChild(dot);
    }


    message.appendChild(typing);

    row.appendChild(avatar);
    row.appendChild(message);

    chatArea.appendChild(row);

    scrollToBottom();

    return row;
}


// =========================================================
// SCROLL
// =========================================================

function scrollToBottom() {

    chatArea.scrollTop =
        chatArea.scrollHeight;
}


// =========================================================
// TEXTAREA AUTO RESIZE
// =========================================================

function autoResize() {

    messageInput.style.height = "auto";

    messageInput.style.height =
        Math.min(
            messageInput.scrollHeight,
            120
        ) + "px";
}


// =========================================================
// ENTER KEY
// =========================================================

messageInput.addEventListener(
    "keydown",
    function(event) {

        if (
            event.key === "Enter" &&
            !event.shiftKey
        ) {

            event.preventDefault();

            sendMessage(
                messageInput.value
            );
        }
    }
);


// =========================================================
// SEND BUTTON
// =========================================================

sendButton.addEventListener(
    "click",
    function() {

        sendMessage(
            messageInput.value
        );
    }
);


// =========================================================
// NEW CHAT
// =========================================================

newChatButton.addEventListener(
    "click",
    function() {

        conversationId =
            "web-" + Date.now();


        chatArea.innerHTML = `

            <div class="welcome">

                <div class="welcome-icon">
                    📈
                </div>

                <h1>
                    What would you like to analyze?
                </h1>

                <p>
                    Ask StockAI about stocks,
                    sectors, momentum, volume
                    and quantitative analysis.
                </p>

                <div class="suggestions">

                    <button
                        class="suggestion"
                        data-question="Analyze INFY">

                        <strong>
                            Analyze INFY
                        </strong>

                        <span>
                            Get quantitative analysis
                        </span>

                    </button>


                    <button
                        class="suggestion"
                        data-question="Which IT stock is strongest based on the available data?">

                        <strong>
                            Best IT stock
                        </strong>

                        <span>
                            Find the strongest stock
                        </span>

                    </button>


                    <button
                        class="suggestion"
                        data-question="Compare INFY and TCS">

                        <strong>
                            Compare stocks
                        </strong>

                        <span>
                            Compare two companies
                        </span>

                    </button>


                    <button
                        class="suggestion"
                        data-question="Show me the IT stocks available">

                        <strong>
                            IT sector
                        </strong>

                        <span>
                            View available IT stocks
                        </span>

                    </button>

                </div>

            </div>
        `;

        attachSuggestionEvents();
    }
);


// =========================================================
// SUGGESTION BUTTONS
// =========================================================

function attachSuggestionEvents() {

    const buttons =
        document.querySelectorAll(
            "[data-question]"
        );


    buttons.forEach(
        function(button) {

            button.addEventListener(
                "click",
                function() {

                    const question =
                        button.dataset.question;

                    sendMessage(question);
                }
            );

        }
    );
}


// =========================================================
// INITIAL SUGGESTION EVENTS
// =========================================================

attachSuggestionEvents();