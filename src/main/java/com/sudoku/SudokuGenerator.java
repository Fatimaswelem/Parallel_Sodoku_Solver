package com.sudoku;

import java.util.Random;

public class SudokuGenerator {
    private static final Random RANDOM = new Random();

    public static SudokuBoard generate(int difficulty) {
        SudokuBoard board = new SudokuBoard();
        fillBoard(board); // Create a full solution first
        
        // Remove cells based on difficulty: 1=Easy, 2=Medium, 3=Hard
        int toRemove = (difficulty == 1) ? 30 : (difficulty == 2) ? 45 : 58;
        removeCells(board, toRemove);
        return board;
    }

    private static boolean fillBoard(SudokuBoard board) {
        for (int row = 0; row < SudokuBoard.SIZE; row++) {
            for (int col = 0; col < SudokuBoard.SIZE; col++) {
                if (board.get(row, col) == 0) {
                    int[] nums = {1, 2, 3, 4, 5, 6, 7, 8, 9};
                    shuffleArray(nums);
                    for (int num : nums) {
                        if (board.isSafe(row, col, num)) {
                            board.set(row, col, num);
                            if (fillBoard(board)) return true;
                            board.set(row, col, 0); // Backtrack
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static void removeCells(SudokuBoard board, int count) {
        while (count > 0) {
            int r = RANDOM.nextInt(9);
            int c = RANDOM.nextInt(9);
            if (board.get(r, c) != 0) {
                board.set(r, c, 0);
                count--;
            }
        }
    }

    private static void shuffleArray(int[] ar) {
        for (int i = ar.length - 1; i > 0; i--) {
            int index = RANDOM.nextInt(i + 1);
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}