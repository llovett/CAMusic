/**
 * Automata.java
 *
 * A cellular automaton.
 *
 * 16 September 2011
 *
 **/

import processing.core.*;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Collection;
import java.io.*;
import java.util.Arrays;
import java.awt.Dimension;

public class Automata extends DrawableObject {

	// Reference to the PApplet that can render this Automaton
	private PApplet parent;
	// Automaton width and height
	private int w, h;
	// Cell width and height
	private int cW, cH;
	// Automaton rows and columns
	private int rows, columns;
	// Cell matrix
	private Cell[][] cells;
	// Automaton generation
	private int generation;
	// Should we count diagonal neighbors or not?
	private boolean diagonals;
	// Rules of this automaton
	private AutomataRules rules;
	// Title of this automaton, can be useful for sending OSC messages
	private String title;
	private int ID; // Faster way to do identification, rather than with string
	private static int ID_counter = 0;

	// Array of how many cells are in what state. This can be used to do
	// other interesting calculations by the user. Storing this information
	// in an array here allows us to supply this information in constant time.
	int[] stateCounts;

	// Some default settings
	private static final int DEFAULT_CELL_WIDTH = 10;
	private static final int DEFAULT_CELL_HEIGHT = 10;

	/**
	 * CONSTRUCTOR
	 *
	 * @param parent - PApplet rendering this Automata
	 * @param rows, columns - Number of rows and columns, respectively
	 * @param rules - AutomataRules to apply to cells upon a call to tick()
	 * @param title - String that is the title of this Automata
	 * @param ID - ID number of this Automata
	 * @param w, h - Width and height of this Automata, respectively
	 **/
	public Automata(PApplet parent, int rows, int columns, AutomataRules rules, String title, int ID, int x, int y) {
		super(parent);
		this.parent = parent;
		this.rows = rows;
		this.columns = columns;
		this.rules = rules;
		this.title = title;
		this.ID = ID;

		cW = DEFAULT_CELL_WIDTH;
		cH = DEFAULT_CELL_HEIGHT;

		setSize(columns * cW, rows * cH);
		setPos(x, y);

		stateCounts = new int[rules.getMaxCellState()+1];
		Arrays.fill(stateCounts, 0);
		stateCounts[0] = rows*columns;

		// Matrices are <rows><cols>
		cells = new Cell[rows][columns];
		for (int i=0; i<rows; i++)
			for (int j=0; j<columns; j++)
				cells[i][j] = new Cell(parent, j, i, cW, cH);

		diagonals = true;
		generation = 0;

	}

	public Automata(PApplet parent, int rows, int columns, AutomataRules rules, String title, int x, int y) {
		this(parent, rows, columns, rules, title, ID_counter++, x, y);
	}

	public Automata(PApplet parent, int rows, int columns, AutomataRules rules, int ID, int x, int y) {
		this(parent, rows, columns, rules, "", ID, x, y);
	}

	public Automata(PApplet parent, int rows, int columns, AutomataRules rules, int x, int y) {
		this(parent, rows, columns, rules, "", ID_counter++, x, y);
	}

	/**
	 * initializeCellsFromStates(int[][])
	 *
	 * Argument - a matrix containing an entry corresponding to a new
	 * state for each cell.
	 *
	 **/
	public void initializeCellsFromStates(int[][] states) {
		if (states.length != cells.length || states[0].length != cells[0].length)
			System.err.println("Warning: automata received a state matrix of different size than the automaton.");

		// Zero-out all the cells
		for (Cell[] row : cells)
			for (Cell c : row)
				c.setCurrentState(0);
		
		for (int i=0; i<cells.length; i++)
			for (int j=0; j<cells[i].length; j++) {
				cells[i][j].setCurrentState(states[i][j]);

				stateCounts[states[i][j]]++;
			}
	}

	public void setCellSize(int w, int h) {
		cW = w;
		cH = h;
		setSize(getColumns() * cW, getRows() * cH);

		for (Cell[] row : cells) {
			for (Cell c : row)
				c.setSize(cW, cH);
		}
	}

	public Dimension getCellSize() {
		return new Dimension(cW, cH);
	}

	private ArrayList<Cell> getNeighbors(int row, int col) {		
		// Use a set here to prevent the addition of duplicates
		ArrayList<Cell> neighbors = new ArrayList<Cell>();

		// If we are 1x1, there are no neighbors
		if (getRows() == 1 && getColumns() == 1)
			return neighbors;

		int row_up, row_down, col_left, col_right;
		row_up = row_down = row;
		col_left = col_right = col;
		if (getRows() > 1)
			if (row == getRows() - 1) {
				row_up = getRows() - 2;
				row_down = 0;
			}
			else if (row == 0) {
				row_up = getRows() - 1;
				row_down = 1;
			}
			else {
				row_up = row-1;
				row_down = row+1;
			}

		if (getColumns() > 1)
			if (col == getColumns() - 1) {
				col_left = getColumns() - 2;
				col_right = 0;
			}
			else if (col == 0) {
				col_left = getColumns() - 1;
				col_right = 1;
			}
			else {
				col_left = col - 1;
				col_right = col + 1;
			}

		// Annoying cases where one of our dimensions is 2
		if (getColumns() == 2 && getRows() == 2) {
			neighbors.add(cells[row_up][col]);
			neighbors.add(cells[row][col_left]);

			if (diagonals)
				neighbors.add(cells[row_up][col_left]);

			return neighbors;
		}
		else if (getColumns() == 2) {
			neighbors.add(cells[row_up][col]);
			neighbors.add(cells[row_down][col]);
			neighbors.add(cells[row][col_left]);

			if (diagonals) {
				neighbors.add(cells[row_up][col_left]);
				neighbors.add(cells[row_down][col_left]);
			}

			return neighbors;
		}
		else if (getRows() == 2) {
			neighbors.add(cells[row_up][col]);
			neighbors.add(cells[row][col_left]);
			neighbors.add(cells[row][col_right]);

			if (diagonals) {
				neighbors.add(cells[row_up][col_left]);
				neighbors.add(cells[row_up][col_right]);
			}

			return neighbors;
		}

		// Add north, south, east, and west neighbors
		neighbors.add(cells[row_up][col]);
		if (row_up != row_down)
			neighbors.add(cells[row_down][col]);
		neighbors.add(cells[row][col_left]);
		neighbors.add(cells[row][col_right]);

		// Add diagonals if requested
		if (diagonals) {
			// Northeast, Southeast, Southwest, Northwest
			neighbors.add(cells[row_up][col_right]);
			neighbors.add(cells[row_down][col_right]);
			neighbors.add(cells[row_down][col_left]);
			neighbors.add(cells[row_up][col_left]);
		}

		return neighbors;
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	public int getGeneration() {
		return generation;
	}

	public String getTitle() {
		return title;
	}

	public int getID() {
		return ID;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	/**
	 * getCellCountForState(state)
	 *
	 * @return the number of cells that are in state 'state'
	 **/
	public int getCellCountForState(int state) {
		if (state > stateCounts.length) return 0;
		if (state < 0) return 0;

		return stateCounts[state];
	}

	/**
	 * getCellCountForOnStates()
	 *
	 * @return the number of cells that are in an ON state.
	 **/
	public int getCellCountForOnStates() {
		int count = 0;

		for (int i=1; i<stateCounts.length; i++)
			count += stateCounts[i];

		return count;
	}

	public void setDiagonalsUsage(boolean diagonals) {
		this.diagonals = diagonals;
	}

	/**
	 * applyRules() - Uses the given set of AutomataRules to decide
	 * the fate of the cells in the cells[][] matrix.
	 **/
	private void applyRules() {
		// DEBUG: Print out state counts
		// 	System.out.println("# state counts #");
		// 	System.out.print("{ ");
		// 	for (int i=0; i<stateCounts.length; i++) {
		// 	    System.out.print(stateCounts[i]+" ");
		// 	}
		// 	System.out.println(" }");

		for (int row=0; row<cells.length; row++)
			for (int col=0; col<cells[row].length; col++) {
				Collection<Cell> neighbors = getNeighbors(row, col);
				Cell target = cells[row][col];

				//System.out.println(target);
				int result = rules.apply(target, neighbors);

				switch(result) {
				case AutomataRules.CELL_AGE:
					//target.setAlive(true);
					int state = target.getState();
					stateCounts[state]--;
					target.setState(state+1);
					stateCounts[state+1]++;
					break;
				case AutomataRules.CELL_BIRTH:
					//		    target.setAlive(true);
					stateCounts[0]--;
					target.setState(1);    
					stateCounts[1]++;
					break;
				case AutomataRules.CELL_DEATH:
					//		    target.setAlive(false);
					stateCounts[target.getState()]--;
					target.setState(0);
					stateCounts[0]++;
					break;
				case AutomataRules.CELL_STASIS:
					// Do nothing to the cell
					break;
				default:
					break;
				}

			}   

	}

	public void tick() {
		// Apply the automaton rules
		applyRules();

		// Lock in the future generation for the cells
		for (Cell[] row : cells)
			for (Cell c : row) {
				c.confirmState();
				//		c.tick();
			}

		generation++;
	}

	public Cell getCell(int row, int col) {
		if (row >= cells.length || col >= cells[0].length)
			throw new NoSuchElementException("Rows: "+rows+"; Cols: "+columns+"; requested cell at <"+row+"><"+col+">");
		if (row < 0 || col < 0)
			throw new NoSuchElementException("Rows: "+rows+"; Cols: "+columns+"; requested cell at <"+row+"><"+col+">");

		return cells[row][col];
	}

	public void setCell(int row, int col, int state) {
		if (row >= cells.length || col >= cells[0].length)
			throw new NoSuchElementException("Rows: "+rows+"; Cols: "+columns+"; requested cell at <"+row+"><"+col+">");
		if (row < 0 || col < 0)
			throw new NoSuchElementException("Rows: "+rows+"; Cols: "+columns+"; requested cell at <"+row+"><"+col+">");
		// 	if (state > rules.getMaxCellState()) {
		// 	    Exception e = new Exception("state "+state+" is out of range of maximum state "+rules.getMaxCellState());
		// 	    throw e;
		//	}

		Cell c = cells[row][col];
		stateCounts[c.getState()]--;
		cells[row][col].setCurrentState(state);
		stateCounts[state]++;

	}

	public void clearCells() {
		for (Cell[] row : cells)
			for (Cell c : row)
				c.setCurrentState(0);
	}

	public void render() {
		parent.colorMode(PApplet.RGB, 255, 255, 255, 255);
		setupDrawPrefs();

		parent.pushMatrix();
		parent.translate(getX(), getY());

		// "Background" of the automaton
		parent.rect(0, 0, cW * columns, cH * rows);

		for (int row=0; row<cells.length; row++)
			for (int col=0; col<cells[row].length; col++) {
				parent.pushMatrix();
				parent.translate(col*cW, row*cH);

				cells[row][col].render();

				parent.popMatrix();
			}

		parent.popMatrix();
	}

}