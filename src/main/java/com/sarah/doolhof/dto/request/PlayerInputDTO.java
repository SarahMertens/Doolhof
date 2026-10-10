package com.sarah.doolhof.dto.request;

public record PlayerInputDTO(double moveX, double moveY, boolean sprint, boolean flare) {

    public static final PlayerInputDTO NONE = new PlayerInputDTO(0, 0, false, false);

    public PlayerInputDTO {
        if (!isDirection(moveX) || !isDirection(moveY)) {
            throw new IllegalArgumentException("A direction must be between -1 and 1");
        }
    }

    private static boolean isDirection(double value) {
        return value >= -1 && value <= 1;
    }
}
