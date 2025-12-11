package com.sudoku;

public interface SudokuObserver {
    void onCellUpdate(int row, int col, int value);
    void onLog(String message);
}