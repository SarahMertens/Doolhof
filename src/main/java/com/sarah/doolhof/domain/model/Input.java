package com.sarah.doolhof.domain.model;

public record Input(double moveX, double moveY, boolean sprint, boolean flare) {

    public static final Input NONE = new Input(0, 0, false, false);

    public boolean isMoving() {
        return moveX != 0 || moveY != 0;
    }

    public Input withFlare(boolean fire) {
        return new Input(moveX, moveY, sprint, fire);
    }
}
