/**
 * DrawableObject.java
 *
 * Just a nice way to use object-oriented-style programming with
 * drawing, using the Processing library.
 *
 * @author okami89
 **/

import processing.core.*;
import java.awt.Color;

public class DrawableObject {

    // Member responsible for doing the actual rendering.
    private PApplet parent;

    // Width, height, x, y
    private int w, h, x, y;

    // Stroke width, bgcolor, stroke color
    private int strokeWidth;
    private int r,g,b,a;
    private int sr,sg,sb,sa;

    public DrawableObject(PApplet parent) {
	this.parent = parent;
	r = g = b = a = sr = sg = sb = sa = 0;
	w = h = x = y = 0;
	strokeWidth = 0;
    }

    public int getX() {
	return x;
    }

    public int getY() {
	return y;
    }

    public int getWidth() {
	return w;
    }

    public int getHeight() {
	return h;
    }

    public int getStrokeWidth() {
	return strokeWidth;
    }

    public int[] getColor() {
	return new int[]{ r, g, b, a };
    }

    public int[] getStrokeColor() {
	return new int[]{ sr, sg, sb, sa };
    }

    /******************
     * MUTATOR METHODS *
     ******************/
 
    public void setX(int x) {
	this.x = x;
    }

    public void setY(int y) {
	this.y = y;
    }

    public void setPos(int x, int y) {
	this.x = x;
	this.y = y;
    }
    
    public void setWidth(int w) {
	this.w = w;
    }

    public void setHeight(int h) {
	this.h = h;
    }

    public void setSize(int w, int h) {
	this.w = w;
	this.h = h;
    }

    public void setStrokeWidth(int strokeWidth) {
	this.strokeWidth = strokeWidth;
    }

    public void setColor(int r, int g, int b) {
	this.r = r;
	this.g = g;
	this.b = b;
    }

    public void setColor(Color c) {
	setColor(c.getRed(),
		 c.getGreen(),
		 c.getBlue(),
		 c.getAlpha());
    }
    
    public void setColor(int r, int g, int b, int a) {
	this.r = r;
	this.g = g;
	this.b = b;
	this.a = a;
    }
    
    public void setStrokeColor(int sr, int sg, int sb) {
	this.sr = sr;
	this.sg = sg;
	this.sb = sb;
    }

    public void setStrokeColor(int sr, int sg, int sb, int sa) {
	this.sr = sr;
	this.sg = sg;
	this.sb = sb;
	this.sa = sa;
    }
    
    /**
     * setupDrawPrefs()
     *
     * Set up the drawing environment for rendering this
     * object properly.
     **/
    public void setupDrawPrefs() {
	parent.fill(r, g, b, a);
	parent.stroke(sr, sg, sb, sa);
	parent.strokeWeight(strokeWidth);
    }

}