package com.sudoku;

/**
 * Simulates computational load.
 * Reduced significantly to prevent freezing on Hard levels while still allowing
 * Parallel processing to demonstrate efficiency over Sequential.
 */
public class ComplexitySimulator {
    // Reduced load to prevent freezing
    private static final int LOAD = 150; 

    public static void simulate() {
        for (int i = 0; i < LOAD; i++) {
            Math.tan(Math.sqrt(i * 0.5)); 
        }
    }
}