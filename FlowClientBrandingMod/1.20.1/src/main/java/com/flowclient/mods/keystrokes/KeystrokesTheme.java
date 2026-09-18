package com.flowclient.mods.keystrokes;

public enum KeystrokesTheme {
    FLOW("Flow Blue", 0xC0181C24, 0xFF4A9EE0, 0xFFE8ECF2, 0xFF0B0D12, 0x554A9EE0, 0xFF3A4050),
    MONO("Monochrome", 0xC0101010, 0xFFFFFFFF, 0xFFDDDDDD, 0xFF000000, 0x44FFFFFF, 0xFF555555),
    NEON("Neon", 0xC00A0F18, 0xFF00E5FF, 0xFFB8F4FF, 0xFF020408, 0x6600E5FF, 0xFF1A3040),
    DARK("Dark Purple", 0xC0141020, 0xFF9B6DFF, 0xFFE8DEFF, 0xFF08060F, 0x559B6DFF, 0xFF3A2E55);

    private final String label;
    private final int idleBackground;
    private final int pressedBackground;
    private final int idleText;
    private final int pressedText;
    private final int glow;
    private final int border;

    KeystrokesTheme(
            String label,
            int idleBackground,
            int pressedBackground,
            int idleText,
            int pressedText,
            int glow,
            int border
    ) {
        this.label = label;
        this.idleBackground = idleBackground;
        this.pressedBackground = pressedBackground;
        this.idleText = idleText;
        this.pressedText = pressedText;
        this.glow = glow;
        this.border = border;
    }

    public String label() {
        return this.label;
    }

    public int idleBackground() {
        return this.idleBackground;
    }

    public int pressedBackground() {
        return this.pressedBackground;
    }

    public int idleText() {
        return this.idleText;
    }

    public int pressedText() {
        return this.pressedText;
    }

    public int glow() {
        return this.glow;
    }

    public int border() {
        return this.border;
    }

    public KeystrokesTheme next() {
        KeystrokesTheme[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
