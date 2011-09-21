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
import java.util.TreeSet;
import java.util.Collection;

public class Automata {

	// Reference to the PApplet that can render this Automaton
	private PApplet parent;
	// Automaton width and height
	private int w, h;
	// Automaton rows and columns
	private int r, c;
	// Cell matrix
	private Cell[][] cells;
	// Automaton generation
	private int generation;
	// Should we count diagonal neighbors or not?
	private boolean diagonals;
	// Rules of this automaton
	private AutomataRules rules;

	public Automata(PApplet parent, int r, int c, int w, int h, AutomataRules rules) {
		this.parent = parent;
		this.r = r;
		this.c = c;
		this.w = w;
		this.h = h;
		this.rules = rules;

		// Matrices are <rows><cols>
		cells = new Cell[r][c];
		for (int i=0; i<r; i++)
			for (int j=0; j<c; j++)
				cells[i][j] = new Cell(parent, j, i);

//		Cell.setSize((int)Math.floor((double)w/c),
//				(int)Math.floor((double)h/r));

		
		diagonals = true;
		generation = 0;
		
	}

	private TreeSet<Cell> getNeighbors(int row, int col) {
		// Use a set here to prevent the addition of duplicates
		TreeSet<Cell> neighbors = new TreeSet<Cell>();

		if (getRows() > 1 && getColumns() > 1) {

			if (0 == row) {
				// Add the cell at the bottom of the matrix in the same column
				neighbors.add(cells[getRows()-1][col]);
				// Add the cells to the left and right of this cell
				if (col == 0) {
					neighbors.add(cells[row][getColumns()-1]);
					neighbors.add(cells[row][col+1]);
					// Add the cells on the diagonals, if required

					if (diagonals) {
						neighbors.add(cells[getRows()-1][getColumns()-1]);
						neighbors.add(cells[getRows()-1][col+1]);
						neighbors.add(cells[row+1][getColumns()-1]);
						neighbors.add(cells[row+1][col+1]);
					}
				}
				else if (getColumns()-1 == col) {
					neighbors.add(cells[row][col-1]);
					neighbors.add(cells[row][0]);

					// Add the cells on the diagonals, if required
					if (diagonals) {
						neighbors.add(cells[getRows()-1][0]);
						neighbors.add(cells[getRows()-1][col-1]);
						neighbors.add(cells[row+1][0]);
						neighbors.add(cells[row+1][col-1]);
					}
				}
				else {
					neighbors.add(cells[row][col-1]);
					neighbors.add(cells[row][col+1]);

					// Add the cells on the diagonals, if required
					if (diagonals) {
						neighbors.add(cells[getRows()-1][col+1]);
						neighbors.add(cells[getRows()-1][col-1]);
						neighbors.add(cells[row+1][col+1]);
						neighbors.add(cells[row+1][col-1]);
					}
				}
				// Add the cell just below this one
				neighbors.add(cells[row+1][col]);
			}

			else if (getRows()-1 == row) {
				// Add the cell just above this one
				neighbors.add(cells[row-1][col]);
				// Add the cells to the left and right of this cell
				if (col == 0) {
					neighbors.add(cells[row][getColumns()-1]);
					neighbors.add(cells[row][col+1]);
					// Add the cells on the diagonals, if required

					if (diagonals) {
						neighbors.add(cells[row-1][getColumns()-1]);
						neighbors.add(cells[row-1][col+1]);
						neighbors.add(cells[0][getColumns()-1]);
						neighbors.add(cells[0][col+1]);
					}
				}
				else if (getColumns()-1 == col) {
					neighbors.add(cells[row][col-1]);
					neighbors.add(cells[row][0]);

					// Add the cells on the diagonals, if required
					if (diagonals) {
						neighbors.add(cells[row-1][0]);
						neighbors.add(cells[row-1][col-1]);
						neighbors.add(cells[0][0]);
						neighbors.add(cells[0][col-1]);
					}
				}
				else {
					neighbors.add(cells[row][col-1]);
					neighbors.add(cells[row][col+1]);

					// Add the cells on the diagonals, if required
					if (diagonals) {
						neighbors.add(cells[row-1][col+1]);
						neighbors.add(cells[row-1][col-1]);
						neighbors.add(cells[0][col+1]);
						neighbors.add(cells[0][col-1]);
					}
				}
				// Add the cell at the top of the matrix in the same column
				neighbors.add(cells[0][col]);
			}
			
			else {
				// Add the cell just above this one
				neighbors.add(cells[row-1][col]);
				// Add the cells to the left and right of this cell
				if (col == 0) {
					neighbors.add(cells[row][getColumns()-1]);
					neighbors.add(cells[row][col+1]);
					// Add the cells on the diagonals, if required

					if (diagonals) {
						neighbors.add(cells[row-1][getColumns()-1]);
						neighbors.add(cells[row-1][col+1]);
						neighbors.add(cells[row+1][getColumns()-1]);
						neighbors.add(cells[row+1][col+1]);
					}
				}
				else if (getColumns()-1 == col) {
					neighbors.add(cells[row][col-1]);
					neighbors.add(cells[row][0]);

					// Add the cells on the diagonals, if required
					if (diagonals) {
						neighbors.add(cells[row-1][0]);
						neighbors.add(cells[row-1][col-1]);
						neighbors.add(cells[row+1][0]);
						neighbors.add(cells[row+1][col-1]);
					}
				}
				else {
					neighbors.add(cells[row][col-1]);
					neighbors.add(cells[row][col+1]);

					// Add the cells on the diagonals, if required
					if (diagonals) {
						neighbors.add(cells[row-1][col+1]);
						neighbors.add(cells[row-1][col-1]);
						neighbors.add(cells[row+1][col+1]);
						neighbors.add(cells[row+1][col-1]);
					}
				}
				// Add the cell just below this one
				neighbors.add(cells[row+1][col]);
			}

		}

		return neighbors;
	}

	public int getRows() {
		return r;
	}
	
	public int getColumns() {
		return c;
	}
	
	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	public int getGeneration() {
		return generation;
	}
	
	public void setDiagonalsUsage(boolean diagonals) {
		this.diagonals = diagonals;
	}
	
	/**
	 * applyRules() - Uses the given set of AutomataRules to decide
	 * the fate of the cells in the cells[][] matrix.
	 **/
	private void applyRules() {
		for (int row=0; row<cells.length; row++)
			for (int col=0; col<cells[row].length; col++) {
				Collection<Cell> neighbors = getNeighbors(row, col);
				Cell target = cells[row][col];

				//System.out.println(target);
				int result = rules.apply(target, neighbors);

				switch(result) {
				case AutomataRules.CELL_AGE:
					//target.setAlive(true);
					break;
				case AutomataRules.CELL_BIRTH:
					target.setAlive(true);
					break;
				case AutomataRules.CELL_DEATH:
					target.setAlive(false);
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
				c.tick();
			}

		generation++;
	}
	
	public Cell getCell(int row, int col) {
		if (row >= cells.length || col >= cells[0].length)
			throw new NoSuchElementException("Rows: "+r+"; Cols: "+c+"; requested cell at <"+row+"><"+col+">");
		if (row < 0 || col < 0)
			throw new NoSuchElementException("Rows: "+r+"; Cols: "+c+"; requested cell at <"+row+"><"+col+">");
		
		return cells[row][col];
	}

	public void render() {
		for (int row=0; row<cells.length; row++)
			for (int col=0; col<cells[row].length; col++) {
				parent.pushMatrix();
				parent.translate(col*Cell.getWidth(), row*Cell.getHeight());
				cells[row][col].render();
				parent.popMatrix();
			}
	}

}