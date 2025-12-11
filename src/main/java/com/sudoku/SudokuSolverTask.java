package com.sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class SudokuSolverTask extends RecursiveTask<SudokuBoard> {
    private static final int THRESHOLD = 3; 
    
    private final SudokuBoard board;
    private final int depth;
    private final SudokuObserver observer;

    public SudokuSolverTask(SudokuBoard board, int depth, SudokuObserver observer) {
        this.board = board;
        this.depth = depth;
        this.observer = observer;
    }

    @Override
    protected SudokuBoard compute() {
        int[] emptyCell = findFirstEmptyCell(board);
        if (emptyCell == null) return board;

        int row = emptyCell[0];
        int col = emptyCell[1];
        String threadName = Thread.currentThread().getName();

        if (depth >= THRESHOLD) {
            return solveSequentially(board, row, col);
        } else {
            List<SudokuSolverTask> subtasks = new ArrayList<>();
            for (int num = 1; num <= SudokuBoard.SIZE; num++) {
                
                ComplexitySimulator.simulate();

                if (board.isSafe(row, col, num)) {
                    SudokuBoard newBoard = board.copy();
                    newBoard.set(row, col, num);

                    if (observer != null) {
                        // Log Parallel Forking steps
                        observer.onLog(String.format("[%s] FORK: Task Value %d at (%d, %d)", threadName, num, row, col));
                        observer.onCellUpdate(row, col, num);
                    }

                    SudokuSolverTask task = new SudokuSolverTask(newBoard, depth + 1, observer);
                    task.fork();
                    subtasks.add(task);
                }
            }

            for (SudokuSolverTask task : subtasks) {
                SudokuBoard result = task.join();
                if (result != null) return result;
            }
            return null;
        }
    }

    private SudokuBoard solveSequentially(SudokuBoard currentBoard, int startRow, int startCol) {
        String threadName = Thread.currentThread().getName();
        
        for (int num = 1; num <= SudokuBoard.SIZE; num++) {
            ComplexitySimulator.simulate();

            if (currentBoard.isSafe(startRow, startCol, num)) {
                
                if (observer != null) {
                    // Log Worker steps
                    observer.onLog(String.format("[%s] WORKER: Placing %d at (%d, %d)", threadName, num, startRow, startCol));
                    observer.onCellUpdate(startRow, startCol, num);
                    try { Thread.sleep(1); } catch (InterruptedException e) {}
                }
                
                SudokuBoard nextBoard = currentBoard.copy();
                nextBoard.set(startRow, startCol, num);

                if (solveRecursive(nextBoard)) return nextBoard;

                // Backtrack
                if (observer != null) {
                    observer.onLog(String.format("[%s] BACKTRACK: Reverting (%d, %d)", threadName, startRow, startCol));
                    observer.onCellUpdate(startRow, startCol, 0);
                }
            }
        }
        return null;
    }

    private boolean solveRecursive(SudokuBoard b) {
        int[] next = findFirstEmptyCell(b);
        if (next == null) return true;
        
        int r = next[0];
        int c = next[1];
        String threadName = Thread.currentThread().getName();

        for (int num = 1; num <= SudokuBoard.SIZE; num++) {
            ComplexitySimulator.simulate();

            if (b.isSafe(r, c, num)) {
                b.set(r, c, num);
                
                if (observer != null) {
                     observer.onLog(String.format("[%s] REC: %d at (%d, %d)", threadName, num, r, c));
                     observer.onCellUpdate(r, c, num);
                }
                
                if (solveRecursive(b)) return true;
                
                b.set(r, c, 0);
                
                if (observer != null) {
                     observer.onLog(String.format("[%s] REC-BACK: Reverting (%d, %d)", threadName, r, c));
                     observer.onCellUpdate(r, c, 0);
                }
            }
        }
        return false;
    }

    private int[] findFirstEmptyCell(SudokuBoard board) {
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                if (board.get(r, c) == 0) return new int[]{r, c};
            }
        }
        return null;
    }
}