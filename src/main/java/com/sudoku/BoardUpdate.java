package com.sudoku;

public class BoardUpdate {
    public final int row;
    public final int col;
    public final int value;

    public BoardUpdate(int row, int col, int value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }
}