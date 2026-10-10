package com.sarah.doolhof.presentation.render;

final class Camera {

    private static final double FOLLOW_SPEED = 9;

    private double x;
    private double y;
    private double shake;
    private double shakeX;
    private double shakeY;

    void snapTo(int mazeCols, int mazeRows, double targetX, double targetY, double viewCols, double viewRows) {
        x = clampAxis(targetX, mazeCols, viewCols);
        y = clampAxis(targetY, mazeRows, viewRows);
    }

    void follow(int mazeCols, int mazeRows, double targetX, double targetY, double viewCols, double viewRows, double dt) {
        double share = 1 - Math.exp(-FOLLOW_SPEED * dt);
        x += (clampAxis(targetX, mazeCols, viewCols) - x) * share;
        y += (clampAxis(targetY, mazeRows, viewRows) - y) * share;

        shake = Math.max(0, shake - dt * 2.2);
        double strength = shake * shake * 0.35;
        shakeX = (Math.random() * 2 - 1) * strength;
        shakeY = (Math.random() * 2 - 1) * strength;
    }

    void addShake(double amount) {
        shake = Math.min(1.2, shake + amount);
    }

    double left(double viewCols) {
        return x - viewCols / 2 + shakeX;
    }

    double top(double viewRows) {
        return y - viewRows / 2 + shakeY;
    }

    private static double clampAxis(double target, double mazeSize, double viewSize) {
        if (mazeSize <= viewSize) {
            return mazeSize / 2;
        }
        return Math.clamp(target, viewSize / 2, mazeSize - viewSize / 2);
    }
}
