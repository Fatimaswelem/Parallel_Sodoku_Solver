package com.sudoku;

import java.util.concurrent.ForkJoinPool;

public class ParallelSudokuSolver {
    private static final ForkJoinPool pool = new ForkJoinPool();

    public static SudokuBoard solve(SudokuBoard board, SudokuObserver observer) {
        SudokuSolverTask task = new SudokuSolverTask(board.copy(), 0, observer);
        return pool.invoke(task);
    }
}