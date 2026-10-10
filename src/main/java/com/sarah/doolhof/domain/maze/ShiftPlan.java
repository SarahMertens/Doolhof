package com.sarah.doolhof.domain.maze;

import java.util.List;

public record ShiftPlan(List<Cell> opening, List<Cell> closing) {

    public ShiftPlan {
        opening = List.copyOf(opening);
        closing = List.copyOf(closing);
    }

    public boolean isEmpty() {
        return opening.isEmpty() && closing.isEmpty();
    }

    public void applyTo(Maze maze) {
        opening.forEach(cell -> maze.setWall(cell, false));
        closing.forEach(cell -> maze.setWall(cell, true));
    }
}
