/**
 * Automata.java
 *
 * A cellular automaton.
 *
 * 16 September 2011
 *
 **/

import processing.core.*;
import java.util.Scanner;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Collection;
// import java.util.HashMap;
import java.io.*;
import java.util.Arrays;
import java.awt.Dimension;
import java.awt.Point;

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
    // private HashMap<Cell> aliveCells; // speeds up rendering time.
    // Automaton generation
    private int generation;
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
     * @param states - a matrix containing an entry corresponding to a new
     * state for each cell.
     *
     **/
    public void initializeCellsFromStates(int[][] states) {
	if (states.length != cells.length || states[0].length != cells[0].length)
	    System.err.println("Warning: automata received a state matrix of different size than the automaton.");

	// Turn everything "off" first
	for (Cell[] row : cells)
	    for (Cell c : row)
		c.setCurrentState(0);
	
	for (int i=0; i<cells.length; i++)
	    for (int j=0; j<cells[i].length; j++) {
		// Cell c = getCell(i, j);
		// c.setCurrentState(states[i][j]);
		// if (states[i][j] > 0)
		//     aliveCells.
		setCell(i, j, states[i][j]);
	    }
    }

    public void initializeCellsFromFile(File f) {
	Scanner lineScanner = null;
	Scanner tokenScanner = null;
	int[][] initAutStates = new int[rows][columns];
	
	int i, j;
	i = j = 0;
	
	try {
	    lineScanner = new Scanner(f);
	    while (lineScanner.hasNextLine()) {
		j = 0;
		String line = lineScanner.nextLine();
		tokenScanner = new Scanner(line);
		while (tokenScanner.hasNextInt() && i < initAutStates.length && j < initAutStates[i].length) {
		    initAutStates[i][j] = tokenScanner.nextInt();
		    j++;

		}

		// Pad out rest of the row
		while (j < initAutStates[i].length)
		    initAutStates[i][j++] = new Integer(0);
		i++;
	    }

	    // Pad out rest of matrix
	    while (i < initAutStates.length) {
		j = 0;
		while (j < initAutStates[i].length)
		    initAutStates[i][j++] = new Integer(0);
		i++;
	    }

	} catch (FileNotFoundException e) {
	    System.err.println("Could not find file : "+f.toString());
	} catch (NumberFormatException e) {
	    System.err.println("File "+f.toString()+" is an invalid matrix.");
	}

	// System.out.println("Will initialize from this matrix:");
	// for (int[] row : initAutStates) {
	//     for (int k : row)
	// 	System.out.print(k+" ");
	//     System.out.println();
	// }	

	
	initializeCellsFromStates(initAutStates);
    }

    public void setCellFadeout(boolean fadeOut) {
	for (Cell[] row : cells)
	    for (Cell c : row)
		c.setFadeout(fadeOut);
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

    /**
     * getNeighbors(int row, int col)
     * returns an ArrayList of Cells that are the neighbors of the Cell at
     * <row,col>. 
     * TODO: Use modular arithmetic.... seriously.
     **/
    private ArrayList<Cell> getNeighbors(int row, int col) {		
	// Use a set here to prevent the addition of duplicates
	ArrayList<Cell> neighbors = new ArrayList<Cell>();
	
	// If we are 1x1, there are no neighbors
	if (getRows() == 1 && getColumns() == 1)
	    return neighbors;

	if (getRows() == 1) {
	    neighbors.add(cells[row][(col+getColumns()-1)%getColumns()]);
	    neighbors.add(cells[row][(col+getColumns()+1)%getColumns()]);
	    return neighbors;
	}

	if (getColumns() == 1) {
	    neighbors.add(cells[(row+getRows()-1)%getRows()][col]);
	    neighbors.add(cells[(row+getRows()+1)%getRows()][col]);
	    return neighbors;
	}

	for (int i = -1; i <=1; i++)
	    for (int j = -1; j<=1; j++)
		if (! (j == 0 && i == 0))
		    neighbors.add(cells[(row+getRows()+i)%getRows()][(col+getColumns()+j)%getColumns()]);

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

    /////////////////////////////////
    // GETTERS FOR ANALYSIS BY MAX //
    /////////////////////////////////

    /**
     * getCellCountForOnStates(state)
     * Count the number of cells that are in an "on" state within an arbitrary
     * rectangular area.
     *
     * @param start - the starting x,y coordinate for doing the counting
     * @param end - the ending x,y coordinate for doing the counting
     * @return the number of cells that are in state 'state'
     **/
    public int getCellCountForOnStates(Point start, Point end) {
	int count = 0;

	for (int i = 1; i<=rules.getMaxCellState(); i++)
	    count += getCellCountForState(i, start, end);

	return count;
    }
    
    /**
     * getCellCountForState(state)
     * Count the number of cells in a given state within an arbitrary
     * rectangular area.
     *
     * @param state - the state of the cells to be counted
     * @param start - the starting x,y coordinate for doing the counting
     * @param end - the ending x,y coordinate for doing the counting
     * @return the number of cells that are in state 'state'
     **/
    public int getCellCountForState(int state, Point start, Point end) {
	int count = 0;
	
	int sX = (int)start.getX();
	int sY = (int)start.getY();
	int fX = (int)end.getX();
	int fY = (int)end.getY();

	// Make sure sX and sY are actually less than fX and fY
	if (sX > fX) {
	    int temp = fX;
	    fX = sX;
	    sX = temp;
	}
	if (sY > fY) {
	    int temp = fY;
	    fY = sY;
	    sY = temp;
	}

	// Do the counting
	for (int row = sY; row <= fY; row++)
	    for (int col = sX; col <= fX; col++)
		if (cells[row][col].getState() == state) count++;

	return count;
    }
    
    /**
     * getCellCountForState(state)
     *
     * @param state - the state of the cells to be counted
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

    /**
     * getNhoodDensities(int)
     * Get the top 'count' number of neighborhoods with the greatest states per
     * cell with a neighborhood size specified by 'nhoodSize'.
     * 
     * @param nhoodSize - Size of one side of the square that is the
     * neighborhood.
     * @param count - Number of neighborhood centroids to return
     **/
    public Point[] getNhoodDensities(int nhoodSize, int count) {
	Point[] ret = new Point[count];
	if (nhoodSize * nhoodSize >= rows * columns) {
	    for (int i = 0; i<ret.length; i++)
		ret[i] = new Point(rows/2, columns/2);
	}
	else {
	    double[] bigDensities = new double[count];
	    
	    for (int row = 0; row < rows - nhoodSize; row++) {
		for (int col = 0; col < columns - nhoodSize; col++) {
		    int endX = col + nhoodSize;
		    int endY = row + nhoodSize;
		    
		    double curr_dens = 0.0;
		    // Preserve cartesian x, y notation here... later translated
		    // to row, col
		    Point currPoint = new Point(col + nhoodSize/2, row + nhoodSize/2);
		    
		    // Calculate neighborhood density for current neighborhood
		    for (int nstartX = col; nstartX < endX; nstartX++)
			for (int nstartY = row; nstartY < endY; nstartY++)
			    curr_dens += getCell(nstartY, nstartX).getState();

		    curr_dens /= (double)(nhoodSize*nhoodSize);
		    //		    System.out.println("PT: "+currPoint);		    
		    // Check current density against other ones we've seen
		    double tempD = 0.0;
		    Point tempP = null;
		    for (int i = bigDensities.length-1; i>=0; i--) {
			if (curr_dens >= bigDensities[i]) {
			    tempP = ret[i];
			    tempD = bigDensities[i];
			    ret[i] = currPoint;
			    bigDensities[i] = curr_dens;
			    currPoint = tempP;
			    curr_dens = tempD;


			}
		    }
		}
	    }

	    // Build list of points from our densities
	    		    
	}
	return ret;
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

		    state++;
		    
		    target.setState(state);
		    stateCounts[state]++;
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

		    ///////////////////////////////////////////////////////
                    // WARNING WARNING					 //
		    // 	HACK ALERT! HACK ALERT!				 //
		    // 	THIS IS some JJJAAANNNKKK SSSHHHIIITTT		 //
                    ///////////////////////////////////////////////////////

		    // If we are alive and were told to "be still", then
		    // it was because we have reached our max state. Now,
		    // so that we can get a nice "spinning colors" effect,
		    // we will reset to our first "on" state (i.e. 1)
		    if (target.isAlive()) {
			stateCounts[target.getState()]--;
			target.setState(1);
			stateCounts[1]++;
		    }

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
	// if (state > rules.getMaxCellState()) {
	//     //	    throw new Exception("state "+state+" is out of range of maximum state "+rules.getMaxCellState());
	//     System.err.println("WARNING: STATE IS OUT OF RANGE : "+state+"; max is "+rules.getMaxCellState());
	// }

	
	///////////////////////////////////////////////////////
	// WARNING WARNING					 //
	// 	HACK ALERT! HACK ALERT!				 //
	// 	THIS IS some JJJAAANNNKKK SSSHHHIIITTT		 //
	///////////////////////////////////////////////////////
	
	
	Cell c = cells[row][col];
	stateCounts[c.getState()]--;
	if (state >= rules.getMaxCellState()) state = 1;
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