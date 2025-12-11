package com.sudoku;

public class SequentialSudokuSolver {
    private final SudokuBoard board;
    private final SudokuObserver observer;
    private final int solutionsToSkip;
    private int solutionsFoundCount = 0;

    public SequentialSudokuSolver(SudokuBoard board, SudokuObserver observer, int targetIndex) {
        this.board = board;
        this.observer = observer;
        this.solutionsToSkip = targetIndex;
    }

    public boolean solve() {
        return solveRecursive(0, 0);
    }

    private boolean solveRecursive(int row, int col) {
        int[] next = findNextEmpty();
        
        if (next == null) {
            if (solutionsFoundCount == solutionsToSkip) {
                return true; 
            } else {
                solutionsFoundCount++;
                return false; 
            }
        }

        int r = next[0];
        int c = next[1];

        for (int num = 1; num <= 9; num++) {
            // Artificial load to allow accurate speed comparison with Parallel
            ComplexitySimulator.simulate();

            if (board.isSafe(r, c, num)) {
                board.set(r, c, num);
                
                // Visual Simulation ONLY (No Text Log)
                if (observer != null) {
                    observer.onCellUpdate(r, c, num);
                    try { Thread.sleep(1); } catch (InterruptedException e) {}
                }

                if (solveRecursive(r, c)) return true;

                // Backtrack
                board.set(r, c, 0);
                if (observer != null) {
                    observer.onCellUpdate(r, c, 0);
                }
            }
        }
        return false;
    }
    
    private int[] findNextEmpty() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.get(r, c) == 0) return new int[]{r, c};
            }
        }
        return null;
    }

    public SudokuBoard getSolvedBoard() { return board; }
}