package com.sarah.doolhof.dto.response;

public record CellDTO(int col, int row) {

    public double centerX() {
        return col + 0.5;
    }

    public double centerY() {
        return row + 0.5;
    }
}
