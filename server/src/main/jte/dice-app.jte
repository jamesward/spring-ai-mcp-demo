@param String extAppsJs

<!DOCTYPE html>
<html>
<head>
    <title>Dice Roller</title>
    <style>
        body { font-family: sans-serif; text-align: center; margin-top: 50px; }
        .dice { font-size: 80px; }
        button { font-size: 18px; padding: 8px 16px; }

        @media (prefers-color-scheme: dark) {
            body {
                background-color: #1d1e22; /* A dark color */
                color: white; /* A light color for text */
            }}
    </style>
</head>
<body>

<div class="dice">
    <span id="die1">⚀</span>
    <span id="die2">⚀</span>
</div>

<br>
<button id="roll-dice-btn">Roll Dice</button>

<!-- we can't easily load resources from the MCP server, so we string-inject it -->
<script id="ext-apps-src" type="text/plain">
$unsafe{extAppsJs}
</script>

<script type="module">
    // this voodoo is needed to get the App import to work
    const src = document.getElementById('ext-apps-src').textContent;
    const blob = new Blob([src], { type: 'application/javascript' });
    const url = URL.createObjectURL(blob);
    const { App } = await import(url);
    URL.revokeObjectURL(url);

    const app = new App({ name: "roll-the-dice", version: "1.0.0" });

    app.connect().then(() => {
        rollDice().then(() => {});
    });

    const updateContext = async (diceRoll) => {
        const sum = await app.callServerTool({
            name: "add",
            arguments: { x: diceRoll.die1, y: diceRoll.die2 },
        });

        await app.sendMessage({
            role: "user",
            content: [{ type: "text", text: "Die 1 = " + diceRoll.die1 + " - Die 2 = " + diceRoll.die2 + " - Sum = " + sum.content[0].text }],
        });
    };

    // Dice UI functions
    const diceFaces = ["⚀","⚁","⚂","⚃","⚄","⚅"];

    const rollDiceBtn = document.getElementById("roll-dice-btn");
    rollDiceBtn.addEventListener("click", async () => {
        await rollDice();
    });

    const randomIndex = () => {
        return Math.floor(Math.random() * 6);
    };

    const rollDice = async () => {
        const die1 = document.getElementById("die1");
        const die2 = document.getElementById("die2");

        let rolls = 0;
        let final1, final2;

        const animation = setInterval(() => {
            final1 = randomIndex();
            final2 = randomIndex();

            die1.textContent = diceFaces[final1];
            die2.textContent = diceFaces[final2];

            rolls++;
            if (rolls > 15) {
                // Convert to 1–6 instead of 0–5
                let diceRoll = {
                    die1: final1 + 1,
                    die2: final2 + 1
                };

                updateContext(diceRoll).then(() => {});

                clearInterval(animation);
            }
        }, 30);
    }
</script>

</body>
</html>
