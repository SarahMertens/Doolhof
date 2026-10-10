package com.sarah.doolhof.dto.response;

public record PlayerDTO(
    double x,
    double y,
    double halfSize,
    double stamina,
    boolean exhausted,
    boolean sprinting,
    boolean isProtected) {
}
