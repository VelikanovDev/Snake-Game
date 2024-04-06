package org.example;

public class Button {
    public float x, y, width, height;
    public String label;
    public Runnable action;

    public Button(float x, float y, float width, float height, String label, Runnable action) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.action = action;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}

