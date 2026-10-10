package com.sarah.doolhof.domain.service;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.TileMemory;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;
import com.sarah.doolhof.domain.maze.ShiftPlan;
import com.sarah.doolhof.domain.model.Hunter;
import com.sarah.doolhof.domain.model.Input;
import com.sarah.doolhof.domain.model.Player;
import com.sarah.doolhof.domain.model.World;
import com.sarah.doolhof.dto.request.PlayerInputDTO;
import com.sarah.doolhof.dto.response.CellDTO;
import com.sarah.doolhof.dto.response.GameDTO;
import com.sarah.doolhof.dto.response.HunterDTO;
import com.sarah.doolhof.dto.response.LevelDTO;
import com.sarah.doolhof.dto.response.LightDTO;
import com.sarah.doolhof.dto.response.MazeDTO;
import com.sarah.doolhof.dto.response.PlayerDTO;
import com.sarah.doolhof.dto.response.WallShiftDTO;
import java.util.ArrayList;
import java.util.List;

final class GameMapper {

    private GameMapper() {
    }

    static Input toInput(PlayerInputDTO dto) {
        return new Input(dto.moveX(), dto.moveY(), dto.sprint(), dto.flare());
    }

    static GameDTO toDto(World world, LevelDTO level, List<GameEvent> events, boolean newRecord) {
        ShiftPlan pendingShift = world.pendingShift();

        return new GameDTO(
            level,
            world.status(),
            toDto(world, world.maze()),
            new LightDTO(world.view().xs(), world.view().ys()),
            toDto(world.player(), world.isProtected()),
            toHunterDtos(world),
            toDtos(world.keys()),
            world.keysCollected(),
            world.totalKeys(),
            toDto(world.door()),
            world.isDoorOpen(),
            world.lives(),
            GameRules.START_LIVES,
            world.flares(),
            world.elapsed(),
            world.isFlareActive(),
            world.flareStrength(),
            pendingShift != null,
            pendingShift == null ? WallShiftDTO.NONE : toDto(pendingShift),
            toDto(world.lastShift()),
            world.shiftCountdown(),
            world.isPlayerChased(),
            world.nearestHunterDistance(),
            events,
            newRecord);
    }

    private static MazeDTO toDto(World world, Maze maze) {
        boolean[] walls = new boolean[maze.tileCount()];
        TileMemory[] memory = new TileMemory[maze.tileCount()];

        for (int row = 0; row < maze.rows(); row++) {
            for (int col = 0; col < maze.cols(); col++) {
                int index = maze.index(col, row);
                walls[index] = maze.isWall(col, row);
                memory[index] = world.memoryOf(col, row);
            }
        }
        return new MazeDTO(maze.cols(), maze.rows(), walls, world.view().seen(), memory);
    }

    private static PlayerDTO toDto(Player player, boolean isProtected) {
        return new PlayerDTO(
            player.x(),
            player.y(),
            GameRules.BODY_HALF_SIZE,
            player.stamina(),
            player.isExhausted(),
            player.isSprinting(),
            isProtected);
    }

    private static List<HunterDTO> toHunterDtos(World world) {
        List<HunterDTO> dtos = new ArrayList<>();
        for (Hunter hunter : world.hunters()) {
            dtos.add(new HunterDTO(
                dtos.size(),
                hunter.x(),
                hunter.y(),
                hunter.state(),
                hunter.distanceTo(world.player()),
                world.isInPlayersLight(hunter)));
        }
        return dtos;
    }

    private static WallShiftDTO toDto(ShiftPlan plan) {
        return new WallShiftDTO(toDtos(plan.opening()), toDtos(plan.closing()));
    }

    private static List<CellDTO> toDtos(List<Cell> cells) {
        return cells.stream()
            .map(GameMapper::toDto)
            .toList();
    }

    private static CellDTO toDto(Cell cell) {
        return new CellDTO(cell.col(), cell.row());
    }
}
