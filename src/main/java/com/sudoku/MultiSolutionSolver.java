package com.sudoku;

import java.util.ArrayList;
import java.util.List;

public class MultiSolutionSolver {
    private final SudokuBoard originalBoard;
    private final List<SudokuBoard> solutions = new ArrayList<>();
    
    public MultiSolutionSolver(SudokuBoard board) {
        this.originalBoard = board;
    }

    public List<SudokuBoard> findAll(int max) {
        solutions.clear();
        solve(originalBoard.copy(), max);
        return solutions;
    }

    private void solve(SudokuBoard b, int max) {
        if (solutions.size() >= max) return;

        int[] empty = findEmpty(b);
        if (empty == null) {
            solutions.add(b.copy());
            return;
        }

        int r = empty[0];
        int c = empty[1];

        for (int num = 1; num <= 9; num++) {
            if (b.isSafe(r, c, num)) {
                b.set(r, c, num);
                solve(b, max);
                b.set(r, c, 0);
                if (solutions.size() >= max) return;
            }
        }
    }

    private int[] findEmpty(SudokuBoard b) {
        for (int i=0; i<9; i++) {
            for (int j=0; j<9; j++) {
                if (b.get(i, j) == 0) return new int[]{i,j};
            }
        }
        return null;
    }
}