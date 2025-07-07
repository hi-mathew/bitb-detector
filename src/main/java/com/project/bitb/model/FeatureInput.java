package com.project.bitb.model;

public class FeatureInput {
    private int windowWidth;
    private int windowHeight;
    private boolean hasDragBehavior;

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public boolean isHasDragBehavior() {
        return hasDragBehavior;
    }

    public void setHasDragBehavior(boolean hasDragBehavior) {
        this.hasDragBehavior = hasDragBehavior;
    }

    @Override
    public String toString() {
        return "FeatureInput{" +
                "windowWidth=" + windowWidth +
                ", windowHeight=" + windowHeight +
                ", hasDragBehavior=" + hasDragBehavior +
                '}';
    }
}
