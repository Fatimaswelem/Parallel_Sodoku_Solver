package com.sudoku;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SudokuGUI extends JFrame {

    private final JTextField[][] cells = new JTextField[SudokuBoard.SIZE][SudokuBoard.SIZE];
    private SudokuBoard currentBoard;
    private SudokuBoard initialBoard;
    private final JTextArea logArea;
    
    // Stats Labels
    private JLabel seqTimeLabel;
    private JLabel parTimeLabel;
    private JLabel speedUpLabel;
    private Double sequentialTime = null;
    private Double parallelTime = null;
    
    // Controls
    private JButton solveButton;
    private JButton generateButton;
    private JButton nextSolutionBtn;
    private JButton clearButton;
    private JComboBox<String> solverTypeBox;
    
    private int currentSolutionTarget = 0;
    private int totalSolutionsPossible = 0;
    private boolean isPuzzleGenerated = false;
    private boolean isBoardSolved = false; 
    private String lastMethodUsed = ""; 

    public SudokuGUI() {
        super("Sudoku Solver (Sequential vs. Parallel)");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createGridPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);

        logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Thread Server Log (IPC Stream)"));
        add(logScroll, BorderLayout.SOUTH);

        currentBoard = new SudokuBoard();
        initialBoard = currentBoard.copy();
        updateGridFromBoard();

        pack();
        setLocationRelativeTo(null);
    }

    // --- SWING WORKER CLASS ---
    private class SolverWorker extends SwingWorker<SudokuBoard, BoardUpdate> {
        private final boolean isSequential;
        private final int targetIndex;
        private final SudokuBoard boardToSolve;
        private final String methodName;
        private long startTime;
        private long endTime;

        public SolverWorker(boolean isSequential, String methodName, int targetIndex, SudokuBoard board) {
            this.isSequential = isSequential;
            this.methodName = methodName;
            this.targetIndex = targetIndex;
            this.boardToSolve = board;
        }

        @Override
        protected SudokuBoard doInBackground() throws Exception {
            startTime = System.currentTimeMillis();

            // Observer to bridge Threads -> GUI
            SudokuObserver workerObserver = new SudokuObserver() {
                @Override
                public void onCellUpdate(int row, int col, int value) {
                    publish(new BoardUpdate(row, col, value));
                }
                @Override
                public void onLog(String message) {
                    SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
                }
            };

            SudokuBoard result = null;
            if (isSequential) {
                // Sequential logic (No text logs inside)
                SequentialSudokuSolver solver = new SequentialSudokuSolver(boardToSolve, workerObserver, targetIndex);
                if (solver.solve()) result = solver.getSolvedBoard();
            } else {
                if (targetIndex == 0) {
                    // Parallel logic (Has detailed logs)
                    result = ParallelSudokuSolver.solve(boardToSolve, workerObserver);
                } else {
                    // "Next Solution" usually uses sequential search to find the Nth result deterministically.
                    // This will assume the properties of the SequentialSolver (silent log, visual only).
                    SequentialSudokuSolver solver = new SequentialSudokuSolver(boardToSolve, workerObserver, targetIndex);
                    if (solver.solve()) result = solver.getSolvedBoard();
                }
            }
            
            endTime = System.currentTimeMillis();
            return result;
        }

        @Override
        protected void process(List<BoardUpdate> chunks) {
            for (BoardUpdate update : chunks) {
                cells[update.row][update.col].setText(update.value == 0 ? "" : String.valueOf(update.value));
                cells[update.row][update.col].setForeground(new Color(139, 0, 139));
            }
        }

        @Override
        protected void done() {
            try {
                SudokuBoard result = get();
                double duration = (endTime - startTime);

                if (result != null) {
                    currentBoard = result;
                    isBoardSolved = true;
                    updateGridFromBoard();
                    
                    log(methodName + " Solved in " + duration + "ms");

                    if (isSequential) {
                        sequentialTime = duration;
                        seqTimeLabel.setText(String.format("Sequential Time: %.0f ms", sequentialTime));
                    } else {
                        parallelTime = duration;
                        parTimeLabel.setText(String.format("Parallel Time: %.0f ms", parallelTime));
                    }
                    updateSpeedUp();
                    checkIfMoreSolutionsExist();
                } else {
                    log("No solution found.");
                    isBoardSolved = false;
                    if (targetIndex > 0) currentSolutionTarget--;
                }
            } catch (InterruptedException | ExecutionException e) {
                log("Error: " + e.getMessage());
            } finally {
                setControlsEnabled(true);
            }
        }
    }

    private void runSolver(boolean isSequential, String methodName, int targetIndex) {
        if (!isPuzzleGenerated) {
            JOptionPane.showMessageDialog(this, "Please generate a puzzle first.");
            return;
        }

        currentBoard = initialBoard.copy();
        updateGridFromBoard();
        
        lastMethodUsed = methodName;
        setControlsEnabled(false);

        String logMsg = methodName + " Simulation started...";
        log(logMsg);

        SolverWorker worker = new SolverWorker(isSequential, methodName, targetIndex, currentBoard);
        worker.execute();
    }

    private void checkIfMoreSolutionsExist() {
        new Thread(() -> {
            MultiSolutionSolver multi = new MultiSolutionSolver(initialBoard);
            List<SudokuBoard> list = multi.findAll(10);
            totalSolutionsPossible = list.size();
            
            SwingUtilities.invokeLater(() -> {
                log("Analysis: " + totalSolutionsPossible + " total solution(s) possible.");
                if (currentSolutionTarget + 1 < totalSolutionsPossible) {
                    nextSolutionBtn.setEnabled(true);
                    nextSolutionBtn.setText("Simulate Solution " + (currentSolutionTarget + 2));
                } else {
                    nextSolutionBtn.setEnabled(false);
                    nextSolutionBtn.setText("No More Solutions");
                }
            });
        }).start();
    }

    private void setControlsEnabled(boolean enabled) {
        solveButton.setEnabled(enabled);
        generateButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        solverTypeBox.setEnabled(enabled);
    }

    private void resetStats() {
        sequentialTime = null;
        parallelTime = null;
        
        // Reset labels
        seqTimeLabel.setText("Sequential Time: N/A");
        parTimeLabel.setText("Parallel Time: N/A");
        speedUpLabel.setText("Speed-up: N/A");
    }

    private void updateSpeedUp() {
        if (sequentialTime != null && parallelTime != null) {
            double pTime = (parallelTime == 0) ? 0.001 : parallelTime;
            double speedup = sequentialTime / pTime;
            speedUpLabel.setText(String.format("Speed-up: %.2fx", speedup));
            if (speedup >= 1.0) speedUpLabel.setForeground(new Color(0, 100, 0));
            else speedUpLabel.setForeground(Color.RED);
        }
    }

    private JPanel createGridPanel() {
        JPanel panel = new JPanel(new GridLayout(9, 9));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(240, 240, 255));
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                JTextField cell = new JTextField();
                cell.setHorizontalAlignment(JTextField.CENTER);
                cell.setFont(new Font("Arial", Font.BOLD, 20));
                cell.setPreferredSize(new Dimension(40, 40));
                int top = (i % 3 == 0 && i != 0) ? 3 : 1;
                int left = (j % 3 == 0 && j != 0) ? 3 : 1;
                cell.setBorder(BorderFactory.createMatteBorder(top, left, 1, 1, Color.DARK_GRAY));
                cells[i][j] = cell;
                panel.add(cell);
            }
        }
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(230, 230, 240));

        panel.add(new JLabel("Generate Puzzle:"));
        JComboBox<String> difficultyBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        panel.add(difficultyBox);

        generateButton = new JButton("Generate");
        generateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateButton.addActionListener(e -> {
            int diff = difficultyBox.getSelectedIndex() + 1;
            currentBoard = SudokuGenerator.generate(diff);
            initialBoard = currentBoard.copy();
            isPuzzleGenerated = true;
            isBoardSolved = false;
            lastMethodUsed = ""; 
            nextSolutionBtn.setEnabled(false);
            currentSolutionTarget = 0;
            updateGridFromBoard();
            logArea.setText("");
            log("Generated new " + difficultyBox.getSelectedItem() + " puzzle.");
            resetStats();
        });
        panel.add(generateButton);
        panel.add(Box.createVerticalStrut(10));

        panel.add(new JLabel("Solving Method:"));
        solverTypeBox = new JComboBox<>(new String[]{"Sequential", "Parallel (Fork/Join)"});
        solverTypeBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        solverTypeBox.addActionListener(e -> {
            if (isPuzzleGenerated) {
                currentBoard = initialBoard.copy();
                isBoardSolved = false; 
                updateGridFromBoard();
                log("Method changed. Board reset.");
            }
        });
        panel.add(solverTypeBox);
        panel.add(Box.createVerticalStrut(10));

        solveButton = new JButton("Solve");
        solveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        solveButton.addActionListener(e -> {
            String selectedMethod = (String) solverTypeBox.getSelectedItem();
            boolean isSequential = selectedMethod.equals("Sequential");

            // Already Solved Check
            if (isBoardSolved && lastMethodUsed.equals(selectedMethod)) {
                JOptionPane.showMessageDialog(this, 
                    "This puzzle is already solved by the " + selectedMethod + " method!", 
                    "Already Solved", JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentSolutionTarget = 0; 
            runSolver(isSequential, selectedMethod, 0);
        });
        panel.add(solveButton);
        panel.add(Box.createVerticalStrut(10));

        nextSolutionBtn = new JButton("Simulate Next Solution");
        nextSolutionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextSolutionBtn.setEnabled(false);
        nextSolutionBtn.addActionListener(e -> {
            currentSolutionTarget++;
            boolean treatAsSequential = lastMethodUsed.equals("Sequential");
            runSolver(treatAsSequential, lastMethodUsed, currentSolutionTarget);
        });
        panel.add(nextSolutionBtn);
        panel.add(Box.createVerticalStrut(10));

        JPanel speedUpPanel = new JPanel(new GridLayout(3, 1));
        speedUpPanel.setBorder(BorderFactory.createTitledBorder("Speed-up Comparison"));
        speedUpPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        seqTimeLabel = new JLabel("Sequential Time: N/A");
        parTimeLabel = new JLabel("Parallel Time: N/A");
        speedUpLabel = new JLabel("Speed-up: N/A");
        speedUpPanel.add(seqTimeLabel);
        speedUpPanel.add(parTimeLabel);
        speedUpPanel.add(speedUpLabel);
        panel.add(speedUpPanel);
        panel.add(Box.createVerticalStrut(10));

        clearButton = new JButton("Clear Board");
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.addActionListener(e -> {
            currentBoard = new SudokuBoard();
            initialBoard = currentBoard.copy();
            isPuzzleGenerated = false;
            isBoardSolved = false;
            lastMethodUsed = "";
            nextSolutionBtn.setEnabled(false);
            updateGridFromBoard();
            resetStats();
            logArea.setText("");
        });
        panel.add(clearButton);

        return panel;
    }

    private void updateGridFromBoard() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int val = currentBoard.get(i, j);
                cells[i][j].setText(val == 0 ? "" : String.valueOf(val));
                boolean isInitial = (initialBoard.get(i, j) != 0);
                cells[i][j].setEnabled(!isInitial);
                if (isInitial) {
                    cells[i][j].setBackground(new Color(221, 160, 221));
                    cells[i][j].setForeground(Color.BLACK);
                } else {
                    cells[i][j].setBackground(new Color(255, 192, 203));
                    cells[i][j].setForeground(new Color(139, 0, 139));
                }
            }
        }
    }
    
    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SudokuGUI().setVisible(true));
    }
}