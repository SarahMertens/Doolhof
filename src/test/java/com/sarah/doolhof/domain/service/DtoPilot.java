package com.sarah.doolhof.domain.service;

import com.sarah.doolhof.dto.request.PlayerInputDTO;
import com.sarah.doolhof.dto.response.CellDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.MazeDTO;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

final class DtoPilot {

    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private DtoPilot() {
    }

    static PlayerInputDTO towardsDoor(GameDTO game) {
        MazeDTO maze = game.maze();
        int[] stepsToDoor = stepsTo(maze, game.door());

        int col = (int) Math.floor(game.player().x());
        int row = (int) Math.floor(game.player().y());

        int nextCol = col;
        int nextRow = row;
        for (int[] d : NEIGHBOURS) {
            int c = col + d[0];
            int r = row + d[1];
            if (maze.isOpen(c, r) && stepsToDoor[r * maze.cols() + c] < stepsToDoor[nextRow * maze.cols() + nextCol]) {
                nextCol = c;
                nextRow = r;
            }
        }

        return new PlayerInputDTO(
            direction(nextCol + 0.5 - game.player().x()),
            direction(nextRow + 0.5 - game.player().y()),
            false,
            false);
    }

    /** Breadth-first search from the goal: the number of steps from every open tile. */
    private static int[] stepsTo(MazeDTO maze, CellDTO goal) {
        int[] steps = new int[maze.cols() * maze.rows()];
        Arrays.fill(steps, Integer.MAX_VALUE);
        steps[goal.row() * maze.cols() + goal.col()] = 0;

        Deque<CellDTO> queue = new ArrayDeque<>();
        queue.add(goal);
        while (!queue.isEmpty()) {
            CellDTO cell = queue.poll();
            for (int[] d : NEIGHBOURS) {
                int c = cell.col() + d[0];
                int r = cell.row() + d[1];
                if (maze.isOpen(c, r) && steps[r * maze.cols() + c] == Integer.MAX_VALUE) {
                    steps[r * maze.cols() + c] = steps[cell.row() * maze.cols() + cell.col()] + 1;
                    queue.add(new CellDTO(c, r));
                }
            }
        }
        return steps;
    }

    private static double direction(double delta) {
        return Math.abs(delta) < 0.04 ? 0 : Math.signum(delta);
    }
}
