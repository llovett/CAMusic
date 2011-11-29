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
import java.awt.Color;

public class Cell extends DrawableObject implements Comparable {

    // RENDERING OPTIONS
    private static final int COLOR_UPDATE_AMOUNT = 1;
    private static final int FADEOUT_UPDATE_AMOUNT = 2;
    private boolean fadeOut; // Creates a "fade out" effect when turned on
    
    // Static variables that determine how to render a cell
    private int width, height;
    private static PApplet parent;

    // Current cell state
    private int state;
    // Cell state at the next generation
    private int newstate;
    // This is used for fading out, if "fadeOut" is turned on:
    private int offFor;
    private Color lastColor;
    private int colorOffset = 0;

    
    // Cell's own reference as to its position in the automaton
    private int x, y;
    
    public Cell(PApplet parent, int x, int y, int w, int h) {
	super(parent);
	
	this.parent = parent;

	fadeOut = false;
	offFor = 0;
	lastColor = new Color(255, 0, 255, 0);

	state = 0;
	newstate = 0;

	// Graphical things
	setSize(w, h);
	setPos(x, y);
	setStrokeWidth(0);
    }

    public void setFadeout(boolean fadeOut) {
	this.fadeOut = fadeOut;
    }
    
    /**
     * isAlive()
     * @return whether or not this Cell is currently (in THIS state) alive.
     */
    public boolean isAlive() {
	return state != 0;
    }

    public void setCurrentState(int state) {
	setState(state);
	confirmState();

	// Assume that we will age at the next generation if our current
	// state is an "on" state.
	if (state > 0)
	    newstate = state+1;
	else
	    newstate = 0;
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
	if (isAlive()) {
	    offFor = 0;
	    lastColor = new Color(constrain(colorOffset + (state-1)*COLOR_UPDATE_AMOUNT, 0, 255),
	    			  255,
	    			  255,
	    			  255);
	    setColor(lastColor);
	    // } else {
	    // 	// Set to transparent
	    //     offFor++;
	    //     if (fadeOut) {
	    // 	// lastColor = new Color(lastColor.getRed(),
	    // 	// 		      lastColor.getGreen(),
	    // 	// 		      lastColor.getBlue(),
	    // 	// 		      255);//				      constrain(lastColor.getAlpha() - FADEOUT_UPDATE_AMOUNT, 0, 255));

	    // 	lastColor = new Color(lastColor.getRed(),
	    // 			      constrain(lastColor.getGreen() - FADEOUT_UPDATE_AMOUNT, 0, 255),
	    // 			      255,
	    // 			      255);

	    // 	setColor(lastColor);
    
	} else {
	    setColor(255, 255, 255, 0);
	}
	
    }


    /**
     * setColorOffset(int offset)
     *
     * adjust the color offset amount (changes the starting color point)
     * */
    public void setColorOffset(int offset) {
	this.colorOffset = offset;
    }

    /**
     * render()
     *
     * Draw this Cell.
     **/
    public void render() {
	if (isAlive()) {
	    // EXPENSIVE: use HSB with an alpha channel
	    parent.colorMode(PApplet.HSB, 255, 255, 255, 255);
	    setupDrawPrefs();


	    parent.ellipse(0, 0, getWidth()*2, getHeight()*2);
	}
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