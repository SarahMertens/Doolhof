package com.sarah.doolhof.dto.response;

import com.sarah.doolhof.common.TileMemory;

public record MazeDTO(int cols, int rows, boolean[] walls, boolean[] lit, TileMemory[] memory) {

    // Arrays can be changed by whoever holds them, so this record keeps its own copies.
    public MazeDTO {
        walls = walls.clone();
        lit = lit.clone();
        memory = memory.clone();
    }

    public boolean contains(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }

    public boolean isWall(int col, int row) {
        return !contains(col, row) || walls[index(col, row)];
    }

    public boolean isOpen(int col, int row) {
        return !isWall(col, row);
    }

    public boolean isLit(int col, int row) {
        return contains(col, row) && lit[index(col, row)];
    }

    public TileMemory memoryOf(int col, int row) {
        return contains(col, row) ? memory[index(col, row)] : TileMemory.UNKNOWN;
    }

    public boolean isKnown(CellDTO cell) {
        return memoryOf(cell.col(), cell.row()) != TileMemory.UNKNOWN;
    }

    @Override
    public boolean[] walls() {
        return walls.clone();
    }

    @Override
    public boolean[] lit() {
        return lit.clone();
    }

    @Override
    public TileMemory[] memory() {
        return memory.clone();
    }

    private int index(int col, int row) {
        return row * cols + col;
    }
}
