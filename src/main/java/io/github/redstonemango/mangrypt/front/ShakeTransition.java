package io.github.redstonemango.mangrypt.front;

import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.util.Duration;

public class ShakeTransition extends Transition {

    private final Node node;
    private double shakeX = 10;
    private double shakeY = 0;
    private int cycles = 6;
    private double[] xValues;
    private double[] yValues;

    public ShakeTransition(Node node) {
        this.node = node;
        setCycleDuration(Duration.millis(400));
        generateShakePattern();
    }

    public ShakeTransition(Duration duration, Node node) {
        this.node = node;
        setCycleDuration(duration);
        generateShakePattern();
    }

    public ShakeTransition setShakeX(double shakeX) {
        this.shakeX = shakeX;
        generateShakePattern();
        return this;
    }

    public ShakeTransition setShakeY(double shakeY) {
        this.shakeY = shakeY;
        generateShakePattern();
        return this;
    }

    public ShakeTransition setCycles(int cycles) {
        this.cycles = cycles;
        generateShakePattern();
        return this;
    }

    @Override
    protected void interpolate(double frac) {
        int index = (int) (frac * xValues.length);
        if (index >= xValues.length) index = xValues.length - 1;

        node.setTranslateX(xValues[index]);
        node.setTranslateY(yValues[index]);
    }

    private void generateShakePattern() {
        int totalFrames = cycles * 2;
        xValues = new double[totalFrames + 1];
        yValues = new double[totalFrames + 1];

        for (int i = 0; i < totalFrames; i++) {
            double factor = (i % 2 == 0) ? 1 : -1;
            xValues[i] = factor * shakeX;
            yValues[i] = factor * shakeY;
        }

        xValues[totalFrames] = 0;
        yValues[totalFrames] = 0;
    }
}