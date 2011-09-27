// Processing library
import processing.core.*;

// Stuff for IPC with MaxMSP via OSC
import oscP5.*;
import netP5.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.Timer;
import java.util.TimerTask;

public class CAMusic extends PApplet implements MouseWheelListener, KeyListener {

    // Private constants
    private static final int MOUSE_WHEEL_ADJUST_AMNT = 20;
    private static final int OSC_PORT_RCV = 8001;

    // Identify Automata by number
    private static final int LIFE1 = 0;
    private static final int LIFE2 = 1;
    private static final int LIFE3 = 2;
    private static final int CONTOUR = 3;
    
    // Automaton 1-3 UDP ports, to be listened to by MaxMSP
    private static final int OSC_PORT_SEND1 = 12000;
    private static final int OSC_PORT_SEND2 = 12001;
    private static final int OSC_PORT_SEND3 = 12002;
    // Contour automaton port
    private static final int OSC_PORT_SEND_CONT = 12003;
    
    
    // Object to send and receive OSC packets
    private OscP5 osc;
    // Remote address (socket) to send our own info to (to MaxMSP)
    private NetAddress Life1Addr, Life2Addr, Life3Addr, ContourAddr;
    
    // The automaton
    private Automata aut;

    // Create a second automaton...?
    private Automata aut2;

    // Automaton rules
    private AutomataRules Life1Rules;
    private AutomataRules Life2Rules;
    private AutomataRules Life3Rules;
    private AutomataRules ContourRules;
    // Rules lists for the AutomataRules
    private TreeSet<Integer> survive, birth;

    // Timer to schedule updates
    private Timer timer;
    private CAUpdater updater;

    // Another timer, different period, for second automaton
    private Timer timer2;
    private CAUpdater updater2;
    
    // Settings for CAMusic
    private static final int ROWS = 100;
    private static final int COLS = 100;
    private static final int CELL_WIDTH = 5;
    private static final int CELL_HEIGHT = 5;
    private static long UPDATE_INTERVAL = 2000; // 1 second
    private static long UPDATE_INTERVAL2 = 5000; // 1 second

    // Automata rule strings, encoded as B/S lists
    private static final int[] SLIST = { 2, 3 };
    private static final int[] BLIST = { 3 };
    // Second atomaton settings
    private static final int[] SLIST2 = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private static final int[] BLIST2 = { 3 };
    // Maximum state of any cell
    private static final int MAX_STATE = 10;
    
    public void setup() {
	// Set up listeners
	addMouseWheelListener(this);

	size(COLS * CELL_WIDTH * 2, ROWS * CELL_HEIGHT, JAVA2D);

	// Set up OSC stuff
	osc = new OscP5(this, OSC_PORT_RCV);
	Life1Addr = new NetAddress("127.0.0.1", OSC_PORT_SEND1);
	Life2Addr = new NetAddress("127.0.0.1", OSC_PORT_SEND2);
	Life3Addr = new NetAddress("127.0.0.1", OSC_PORT_SEND3);
	ContourAddr = new NetAddress("127.0.0.1", OSC_PORT_SEND_CONT);
	
	Life1Rules = new SingleStateRules(SLIST, BLIST, MAX_STATE);
	ContourRules = new SingleStateRules(SLIST2, BLIST2, MAX_STATE);
	
	// Construct the automaton
	aut = new Automata(this,
			   ROWS,
			   COLS,
			   Life1Rules,
			   "life",
			   LIFE1);


	aut2 = new Automata(this,
			    ROWS,
			    COLS,
			    ContourRules,
			    "contour",
			    CONTOUR);


	Cell.setSize(CELL_WIDTH, CELL_HEIGHT);

	// ... instead, use a Timer to update at arbitrary regular intervals.
	timer = new Timer();
	updater = new CAUpdater(aut);
	timer.schedule(updater,
		       0L,
		       UPDATE_INTERVAL);

 	timer2 = new Timer();
	updater2 = new CAUpdater(aut2);
	timer2.schedule(updater2,
			0L,
			UPDATE_INTERVAL2);

	
    }

    private void activateCellAtXY(int x, int y) {
	try {
	    // Check which automaton we are referring to
	    // Left automaton
	    if (x < width/2) {

		int w = aut.getColumns() * Cell.getWidth();
		int h = aut.getRows() * Cell.getHeight();
		
		int row = (int)((double)mouseY/h * aut.getRows());
		int col = (int)((double)mouseX/w * aut.getColumns());

		aut.setCell(row, col, 1);
	    }
	    // Right automaton
	    else if (x >= width/2) {
		int w = aut2.getColumns() * Cell.getWidth();
		int h = aut2.getRows() * Cell.getHeight();

		int row = (int)((double)mouseY/h * aut2.getRows());
		int col = (int)((double)(mouseX - width/2)/w * aut2.getColumns());
		
		//		Cell c = aut2.getCell(row, col); // c.setCurrentState(true);

		aut2.setCell(row, col, 1);
	    }
	} catch (Exception e) {
	    System.err.println(e.toString());
	    e.printStackTrace();
	}
    }
	
    private void clearCells() {
	for (int row = 0; row < ROWS; row++)
	    for (int col = 0; col < COLS; col++) {
// 		Cell c = aut.getCell(row, col);
// 		Cell c2 = aut2.getCell(row, col);
// 		//		c.setCurrentState(false);
// 		c.setCurrentState(0);
// 		//		c2.setCurrentState(false);
// 		c2.setCurrentState(0);
		aut.setCell(row, col, 0);
		aut2.setCell(row, col, 0);
	    }
    }


    /************************
     * accessors & mutators
     ************************/

    public int getWidth() {
	return width;
    }

    public int getHeight() {
	return height;
    }

    /******************
     * listeners
     ******************/

    /**
     * oscEvent(msg)
     *
     * This method is called when we have received an OscMessage on
     * OSC_PORT_RCV
     **/
    public void oscEvent(OscMessage msg) {
	// TODO: implement this method as needed
    }
    
    public void mouseClicked() {
	activateCellAtXY(mouseX, mouseY);
	//		aut.tick();
    }

    public void mousePressed() {
	if (mouseX < width/2){
	    if (! updater.isPaused())
		updater.pause();
	}
	else if (mouseX >= width/2)
	    if (! updater2.isPaused())
		updater2.pause();

	
	activateCellAtXY(mouseX, mouseY);
    }

    public void mouseReleased() {
	if (mouseX < width/2){
	    if (updater.isPaused())
		updater.unPause();
	}
	else if (mouseX >= width/2)
	    if (updater2.isPaused())
		updater2.unPause();
    }
    
    public void mouseDragged() {
	if (mouseX < width/2){
	    if (! updater.isPaused())
		updater.pause();
	}
	else if (mouseX >= width/2)
	    if (! updater2.isPaused())
		updater2.pause();

	
	activateCellAtXY(mouseX, mouseY);
    }

    public void mouseDown() {
	if (mouseX < width/2){
	    if (! updater.isPaused())
		updater.pause();
	}
	else if (mouseX >= width/2)
	    if (! updater2.isPaused())
		updater2.pause();
    }
	
    public void keyPressed(KeyEvent e) {
	int c = e.getKeyCode();
		
	switch(c) {
	case KeyEvent.VK_DELETE:
	    clearCells();
	    break;
	default:
	    break;
	}
    }

    public void mouseWheelMoved(MouseWheelEvent mwe) {
	int x = mwe.getX();
	if (x < width/2) {
	
	    int notches = mwe.getWheelRotation();

	    long speed_adjust = MOUSE_WHEEL_ADJUST_AMNT * notches;
	    UPDATE_INTERVAL += speed_adjust;
	    UPDATE_INTERVAL = (long) constrain(UPDATE_INTERVAL, 1, 10000);

	    boolean wasPaused = updater.isPaused();
	    CAUpdater new_updater = new CAUpdater(aut);
	    if (wasPaused)
		new_updater.pause();
	
	    updater.cancel();
	    updater = new_updater;
	
	    timer.cancel();
	    timer = new Timer();
	    timer.schedule(updater,
			   0L,
			   UPDATE_INTERVAL);
	}
	else if (x >= width/2) {

	    int notches = mwe.getWheelRotation();

	    long speed_adjust = MOUSE_WHEEL_ADJUST_AMNT * notches;
	    UPDATE_INTERVAL2 += speed_adjust;
	    UPDATE_INTERVAL2 = (long) constrain(UPDATE_INTERVAL2, 1, 10000);

	    boolean wasPaused = updater2.isPaused();
	    CAUpdater new_updater2 = new CAUpdater(aut);
	    if (wasPaused)
		new_updater2.pause();
	
	    updater2.cancel();
	    updater2 = new_updater2;
	
	    timer2.cancel();
	    timer2 = new Timer();
	    timer2.schedule(updater2,
			   0L,
			   UPDATE_INTERVAL2);
	}
    }

    /******************
     * draw loop
     ******************/

    public void draw() {
	background(255);

	// Draw the automaton
	aut.render();
	
	pushMatrix();
	translate(width/2, 0);
	aut2.render();
	popMatrix();
    }


    /******************
     * private classes
     ******************/

    private class CAUpdater extends TimerTask {

	private Automata a;
	private boolean paused;

	public CAUpdater(Automata a) {
	    super();
	    this.a = a;
	    paused = false;
	}

	public void pause() {
	    paused = true;
	}

	public void unPause() {
	    paused = false;
	}

	public boolean isPaused() {
	    return paused;
	}

	public void run() {
	    if (! this.paused) {
		a.tick();

		NetAddress destination = null;
		AutomataRules rules = null;
		
		// Send reports about cell populations
		switch (a.getID()) {
		case LIFE1:
		    destination = Life1Addr;
		    rules = Life1Rules;
		    break;
		case LIFE2:
		    destination = Life2Addr;
		    rules = Life2Rules;
		    break;
		case LIFE3:
		    destination = Life3Addr;
		    rules = Life3Rules;
		    break;
		case CONTOUR:
		    destination = ContourAddr;
		    rules = ContourRules;
		    break;
		default:
			break;
		}

		if (null != destination) {
		    OscMessage popMsg = new OscMessage("/population");
		    popMsg.add(a.getCellCountForOnStates());
		    OscMessage oldPopMsg = new OscMessage("/oldestcount");
		    oldPopMsg.add(a.getCellCountForState(rules.getMaxCellState()));

		    osc.send(popMsg, destination);
		    osc.send(oldPopMsg, destination);
		}
		    
	    }
	    
	    //System.out.println("Generation: "+a.getGeneration());
	}

    }


}