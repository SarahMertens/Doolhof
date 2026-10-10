package com.sarah.doolhof.dto.response;

public record LightDTO(double[] xs, double[] ys) {

    public LightDTO {
        if (xs.length != ys.length) {
            throw new IllegalArgumentException("Every corner needs an x and a y");
        }
        xs = xs.clone();
        ys = ys.clone();
    }

    public int cornerCount() {
        return xs.length;
    }

    public double x(int corner) {
        return xs[corner];
    }

    public double y(int corner) {
        return ys[corner];
    }

    @Override
    public double[] xs() {
        return xs.clone();
    }

    @Override
    public double[] ys() {
        return ys.clone();
    }
}
