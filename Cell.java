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

public class Cell implements Comparable {

	private static PApplet parent;

	// Current cell state
	private int age;
	private boolean alive;

	// Cell state that will be the next state
	private boolean new_alive;

	// Static variables that determine how to render a cell
	private static int width, height;
	private static final int DEFAULT_WIDTH = 10;
	private static final int DEFAULT_HEIGHT = 10;
	private static final int COLOR_UPDATE_AMOUNT = 20;
	
	// Cell's own reference as to its position in the automaton
	private int x, y;

	public Cell(PApplet parent, int x, int y) {
		this.parent = parent;
		this.width = DEFAULT_WIDTH;
		this.height = DEFAULT_HEIGHT;
		
		this.x = x;
		this.y = y;
	}

	public static void setWidth(int w) {
		width = w;
	}

	public static void setHeight(int h) {
		height = h;
	}

	public static void setSize(int w, int h) {
		setWidth(w);
		setHeight(h);
	}

	public static int getWidth() {
		return width;
	}

	public static int getHeight() {
		return height;
	}

	/**
	 * render()
	 *
	 * Draw this Cell.
	 **/
	@SuppressWarnings("restriction")
	public void render() {
		parent.colorMode(parent.HSB);
		if (! alive)
			parent.fill(0);
		else
			parent.fill(
					constrain((age-1)*COLOR_UPDATE_AMOUNT, 0, 180),
					255,
					255);
		parent.strokeWeight(2);
		parent.stroke(0);
		parent.rect(0, 0, width, height);
	}

	/**
	 * isAlive()
	 * @return whether or not this Cell is currently (in THIS state) alive.
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * setAlive - Set the next state of this cell
	 * @param alive - The future state of the cell
	 */
	public void setAlive(boolean alive) {
		new_alive = alive;
	}
	
	/**
	 * setCurrentState() - Set the current state (dead/alive) of this Cell.
	 * This should not be confused with setAlive(), which sets the NEXT state
	 * of the cell.
	 * 
	 * @param alive - The revised CURRENT state of the Cell
	 */
	public void setCurrentState(boolean alive) {
		boolean old_alive = this.alive;
		
		this.alive = alive;
		// Unless specified otherwise in setAlive(), assume that this cell will continue on to age.
		new_alive = alive;
		
		// If we were switched on manually, count this cell as having an age of 1 for this generation.
		if (! old_alive && alive)
			age = 1;
	}

	public int getAge() {
		return age;
	}

	/**
	 * tick() - Used by the cellular automata matrix
	 * to age this cell and do any other updates required.
	 *
	 **/
	public void tick() {
		// If we're going to be around the next generation, count current
		// generation towards our age at that point.
		if (alive)
			age++;
	}

	public void confirmState() {
		alive = new_alive;
		if (! alive)
			age = 0;
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
		String ret = "CELL at <"+y+"><"+x+"> is ";
		if (alive)
			ret += "ALIVE at age "+age;
		else
			ret += "DEAD";
		return ret;
	}

}