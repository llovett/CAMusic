/**
 * This class imitates a "virtual pen," to be controlled by OSC messages send
 * out from MaxMSP. The pen may apply varying pressure on its canvas (an
 * arbitrary Automaton).
 *
 * Parameters to be controlled:
 * X, Y position
 * pressure
 **/

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;
import java.io.File;
import java.io.FileNotFoundException;

public class CASoundPen {

    // Types of pen actions
    public static final int IMPOSE_MATRIX = 0;
    public static final int FLIP_CELLS = 1;
    public static final int AGE_CELLS = 2;
    public static final int ERASE_CELLS = 3;
    public static final int AGE_POPULATION = 4;

    // Private fields, controlling various aspects of how the pen draws
    private int penPressure; // -1 = erase, 0 = neutral, > 0 = set the cell state
    private int penX, penY;
    private int type; // impose-matrix, draw-line, etc.
    private int width;

    // Private constants, thresholds, etc.
    private static final int MAX_MAT_SIZE = 100;
    private Automata aut;
    private Dimension cellSize;
    private int nhood_size = 6; // 6x6 square neighborhood
    private static ArrayList<Integer[][]> penMatrices; // Patterns to insert at pen point
	
    /**
     * CONSTRUCTOR
     *
     * @param aut - Automaton on which this CASoundPen draws.
     **/
    public CASoundPen(Automata aut) {
	this.aut = aut;
	cellSize = aut.getCellSize();

	// No reason to waste time doing this for every pen.
	if (null == penMatrices) loadMatrices();
    }

    /**
     * loadMatrices()
     * a simple initialization performed by the soundPen to load all of its
     * matrices from the files provided.
     **/
    private static void loadMatrices() {
	penMatrices = new ArrayList<Integer[][]>();
	    
	File dir = new File("./spmats");
	File[] spmats = dir.listFiles();

	for (File f : spmats) {
	    Scanner lineScanner = null;
	    Scanner tokenScanner = null;
	    Integer[][] initMatStates = new Integer[MAX_MAT_SIZE][MAX_MAT_SIZE];
	
	    int i, j;
	    i = j = 0;
	
	    try {
		lineScanner = new Scanner(f);
		while (lineScanner.hasNextLine()) {
		    j = 0;
		    String line = lineScanner.nextLine();
		    tokenScanner = new Scanner(line);
		    while (tokenScanner.hasNextInt() && i < initMatStates.length && j < initMatStates[i].length) {
			initMatStates[i][j] = new Integer(tokenScanner.nextInt());
			j++;
		    }
		    
		    // Pad out rest of the row
		    while (j < initMatStates[i].length)
			initMatStates[i][j++] = new Integer(0);

		    i++;
		}

		// Pad out rest of matrix
		while (i < initMatStates.length) {
		    j = 0;
		    while (j < initMatStates[i].length)
			initMatStates[i][j++] = new Integer(0);
		    i++;
		}

	    } catch (FileNotFoundException e) {
		System.err.println("Could not find file : "+f.toString());
	    } catch (NumberFormatException e) {
		System.err.println("File "+f.toString()+" is an invalid matrix.");
	    }
		
	    penMatrices.add(initMatStates);

	}
	    
    }

    /**
     * imposeMatrix(row, col, matrix)
     * Impose a pattern (as a matrix of 0 and 1) on top of an automaton. The
     * cells that are set to "on" will be set to a state equivalent to the
     * current penPressure. "row" and "col" describe the upper-left corner
     * of where to impose the matrix.
     */
    private void imposeMatrix(int row, int col, Integer[][] matrix) {
    	for (int r = 0; r < matrix.length && r + row < aut.getRows(); r++)
    	    for (int c = 0; c < matrix[r].length && c + col < aut.getColumns(); c++) {
    		if (matrix == null) System.err.println("MATRIX IS NULL!");
    		if (matrix[r][c] == null) System.err.println("ENTRy IN MATRIX SI NULL");
    		aut.setCell(r + row, c + col, matrix[r][c] * penPressure);
	    }
    }
    
    public void setPenPosition(int x, int y) {
	penX = x;
	penY = y;
    }

    public void setPenPosition(Point p) {
	penX = (int)p.getX();
	penY = (int)p.getY();
    }

    public void setPenPressure(int pressure) {
	penPressure = pressure;
    }

    public void setPenWidth(int width) {
	this.width = width;
    }

    public void setType(int type) {
	this.type = type;
    }

    /**
     * applyPen()
     *
     * Applies current pen attributes in order to affect its automaton.
     **/
    public void applyPen() {
	// Random object to use if necessary
	Random rand = new Random();
	
	switch (type) {
	case IMPOSE_MATRIX:
	    Point p = aut.getNhoodDensities(nhood_size, 2)[0];
	    //	    System.out.println("DEBUG : "+p.toString());
	    int cellX = (int)p.getX();
	    int cellY = (int)p.getY();
		
	    //aut.setCell(cellY, cellX, 1);
	    imposeMatrix(cellY, cellX, penMatrices.get(rand.nextInt(penMatrices.size())));
	    break;
	case FLIP_CELLS:
	    double onOrOff = 0.0;
	    for (int i = penX; i < penX + width; i++)
		for (int j = penY; j < penY + width; j++) {
		    int posX = i % aut.getColumns();
		    int posY = j % aut.getRows();
		    if (aut.getCell(posY, posX).isAlive()) onOrOff++;
		}
	    onOrOff /= Math.pow(width, 2);
	    for (int i = penX; i < penX + width; i++)
		for (int j = penY; j < penY + width; j++) {
		    int posX = i % aut.getColumns();
		    int posY = j % aut.getRows();
		    aut.setCell(posY, posX, (onOrOff >= 0.25? 0 : penPressure));
		}
	    
	    break;
	case AGE_CELLS:
	    // Age just one cell
	    for (int i = penX; i < penX + width; i++)
		for (int j = penY; j < penY + width; j++) {
		    int posX = i % aut.getColumns();
		    int posY = j % aut.getRows();
		    aut.setCell(posY, posX, penPressure);
		}
	    break;
	case AGE_POPULATION:
	    // Age all "on" cells
	    for (int i = 0; i<aut.getRows(); i++)
		for (int j = 0; j<aut.getColumns(); j++) {
		    int state = aut.getCell(i,j).getState();
		    aut.setCell(i,j,(state > 0? state + 1 : 0));
		}
	    break;
	case ERASE_CELLS:
	    // Erase all cells
	    aut.clearCells();

	    // Set color offsets
	    aut.setColorOffset(rand.nextInt(256));
	
	    break;
	default:
	    System.err.println("Warning: unrecognized pen type: "+type);
	    break;
	}
    }
}
