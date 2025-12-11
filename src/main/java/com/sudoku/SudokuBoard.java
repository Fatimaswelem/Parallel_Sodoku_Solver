package com.sudoku;

public class SudokuBoard {
    public static final int SIZE = 9;
    public static final int SUBGRID_SIZE = 3;
    private int[][] board;

    public SudokuBoard() {
        this.board = new int[SIZE][SIZE];
    }
    
    public SudokuBoard(int[][] initialBoard) {
        this.board = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(initialBoard[i], 0, this.board[i], 0, SIZE);
        }
    }

    public int get(int row, int col) {
        return board[row][col];
    }

    public void set(int row, int col, int value) {
        board[row][col] = value;
    }

    public boolean isSafe(int row, int col, int num) {
        // Check row
        for (int d = 0; d < SIZE; d++) {
            if (board[row][d] == num) return false;
        }
        // Check column
        for (int r = 0; r < SIZE; r++) {
            if (board[r][col] == num) return false;
        }
        // Check subgrid
        int boxRowStart = row - row % SUBGRID_SIZE;
        int boxColStart = col - col % SUBGRID_SIZE;
        for (int r = boxRowStart; r < boxRowStart + SUBGRID_SIZE; r++) {
            for (int d = boxColStart; d < boxColStart + SUBGRID_SIZE; d++) {
                if (board[r][d] == num) return false;
            }
        }
        return true;
    }

    public SudokuBoard copy() {
        int[][] newBoard = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(this.board[i], 0, newBoard[i], 0, SIZE);
        }
        return new SudokuBoard(newBoard);
    }
}