/**
 * Cell.java
 *
 * A Cell in a cellular automata system.
 *
 * An implementation note:
 * Since we want to decide the future generation of cells by looking
 * at the contemporary state of all the cells currently, methods like
 * setAlive() will set the FUTURE state of the cell. In other words, if
 * we have a Cell c, and do c.setAlive(false) where c is alive in the current
 * generation, and we query c.isAlive(), we will have a value of "true".
 * After we run confirmState(), we take the new state and apply it to the
 * older one. Then when we do c.isAlive(), we will get "false".
 *
 * 16 September 2011
 *
 **/

import processing.core.*;

public class Cell extends DrawableObject implements Comparable {

    // Static variables that determine how to render a cell
    private static int width, height;
    private static final int COLOR_UPDATE_AMOUNT = 5;
	
    private static PApplet parent;

    // Current cell state
    private int state;
    // Cell state at the next generation
    private int newstate;

    // Cell's own reference as to its position in the automaton
    private int x, y;
    
    public Cell(PApplet parent, int x, int y, int w, int h) {
	super(parent);
	
	this.parent = parent;



	state = 0;
	newstate = 0;

	// Graphical things
	setSize(w, h);
	setPos(x, y);
	setStrokeWidth(0);
    }

    /**
     * render()
     *
     * Draw this Cell.
     **/
    public void render() {
	if (isAlive()) {
	    setupDrawPrefs();

	    // EXPENSIVE: use HSB with an alpha channel
	    parent.colorMode(PApplet.HSB, 255, 255, 255, 255);
	    parent.ellipse(0, 0, getWidth()*2, getHeight()*2);
	}
    }

    /**
     * isAlive()
     * @return whether or not this Cell is currently (in THIS state) alive.
     */
    public boolean isAlive() {
	return state != 0;
    }

//     /**
//      * setAlive - Set the next state of this cell
//      * @param alive - The future state of the cell
//      */
//     public void setAlive(boolean alive) {
// 	new_alive = alive;
//     }
	
//     /**
//      * setCurrentState() - Set the current state (dead/alive) of this Cell.
//      * This should not be confused with setAlive(), which sets the NEXT state
//      * of the cell.
//      * 
//      * @param alive - The revised CURRENT state of the Cell
//      */
//     public void setCurrentState(boolean alive) {
// 	boolean old_alive = this.alive;
		
// 	this.alive = alive;
// 	// Unless specified otherwise in setAlive(), assume that this cell will continue on to age.
// 	new_alive = alive;
		
// 	// If we were switched on manually, count this cell as having an age of 1 for this generation.
// 	if (! old_alive && alive)
// 	    state = 1;
//     }

    public void setCurrentState(int state) {
	setState(state);
	confirmState();

	// Assume that we will age at the next generation
	newstate = state+1;
    }
	

    public void setState(int state) {
	newstate = state;
    }
    
    public int getState() {
	return state;
    }

    public void confirmState() {
	state = newstate;

	//	parent.colorMode(PApplet.HSB, 255, 255, 255, 255);
	if (isAlive())
	    setColor(constrain((state-1)*COLOR_UPDATE_AMOUNT, 0, 180),
		     255,
		     255,
		     255);
	else
	    setColor(255, 255, 255, 0);
    }

    @Override
	public int compareTo(Object arg0) {
	Cell comp = (Cell)arg0;
		
	if (y > comp.y)
	    return 1;
	else if (y < comp.y)
	    return -1;
	else {
	    if (x > comp.x)
		return 1;
	    else if (x == comp.x)
		return 0;
	    else
		return -1;
	}
		
    }
	
    public boolean equals(Cell c) {
	return (this.x == c.x && this.y == c.y);
    }
	
    private int constrain(int x, int min, int max) {
	int ret = (x < min? min : x);
	return (ret > max? max : ret);
    }
	
    public String toString() {
	return "CELL at <"+y+"><"+x+"> @ STATE "+state;
    }

}