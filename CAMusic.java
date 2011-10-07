// Processing library
import processing.core.*;
// This requires the entire "processing/opengl" tree to be present in the same
// directory as this file, in order for this to be cross-platform. Using the
// OPENGL renderer as opposed to JAVA2D or P2D (Oh god... never use that one...)
// makes this run SO much faster.
import processing.opengl.*;

// Stuff for IPC with MaxMSP via OSC
import oscP5.*;
import netP5.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Dimension;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CAMusic extends PApplet implements MouseWheelListener, KeyListener {

    // Settings for CAMusic
    // private static final int ROWS = 200;
    // private static final int COLS = 200;
    // private static final int CELL_WIDTH = 3;
    // private static final int CELL_HEIGHT = 3;
    // Try out the following settings for old 1280 x 800 screen res macbook:
    private static final int ROWS_LIFE = 100;
    private static final int COLS_LIFE = 100; // was 84
    private static final int ROWS_CONTOUR = 160; // was 60
    private static final int COLS_CONTOUR = 256;
    private static final int CELL_WIDTH = 5;
    private static final int CELL_HEIGHT = 5;

    private static long UPDATE_INTERVAL_LIFE = 50;   // Life Automata delay
    private static long UPDATE_INTERVAL_CONTOUR = 2000; // Contour Automaton delay
    
    // Private constants
    private static final int MOUSE_WHEEL_ADJUST_AMNT = 20;
    private static final int OSC_PORT_RCV = 8000;

    // Identify Automata by number
    private static final int LIFE1 = 0;
    private static final int SAMP1 = 1;
    private static final int SAMP2 = 2;
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
    
    // The automata
    private Automata AutomataBass, AutomataSamp1, AutomataSamp2;
    private Automata AutomataContour;
    private ArrayList<Automata> CAMusicAutomata;

    // Automaton rules
    private AutomataRules BassRules;
    private AutomataRules Samp1Rules;
    private AutomataRules Samp2Rules;
    private AutomataRules ContourRules;

    // Timer to schedule updates
    private Timer timer;
    private CAUpdater UpdaterLifes;

    // Another timer, different period, for second automaton
    private Timer timer2;
    private CAUpdater UpdaterContour;
    
    // Automata rule strings, encoded as S/B lists
    private static final int[] SLIST = { 2, 3 };
    private static final int[] BLIST = { 3 };
    // Second atomaton settings
    private static final int[] SLIST2 = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private static final int[] BLIST2 = { 3 };
    // Maximum state of any cell
    private static final int MAX_STATE = 40;
    
    public void setup() {
	// Set up listeners
	addMouseWheelListener(this);

	// Test settings:
	// size(COLS * CELL_WIDTH * 2, ROWS * CELL_HEIGHT * 2, OPENGL);
	// Sweet macbook settings below:⁄
	size(1280, 800, JAVA2D);
	
	// Set up OSC stuff
	osc = new OscP5(this, OSC_PORT_RCV);
	Life1Addr = new NetAddress("127.0.0.1", OSC_PORT_SEND1);
	Life2Addr = new NetAddress("127.0.0.1", OSC_PORT_SEND2);
	Life3Addr = new NetAddress("127.0.0.1", OSC_PORT_SEND3);
	ContourAddr = new NetAddress("127.0.0.1", OSC_PORT_SEND_CONT);
	
	BassRules = new SingleStateRules(SLIST, BLIST, MAX_STATE);
        Samp1Rules = new SingleStateRules(SLIST, BLIST, MAX_STATE);
	Samp2Rules = new SingleStateRules(SLIST, BLIST, MAX_STATE);
	ContourRules = new SingleStateRules(SLIST2, BLIST2, MAX_STATE);
	
	// Construct the automata
	CAMusicAutomata = new ArrayList<Automata>();

	AutomataBass = new Automata(this,
				    ROWS_LIFE,
				    COLS_LIFE,
				    BassRules,
				    "life",
				    LIFE1,
				    0,
				    0);

	AutomataBass.setCellSize(CELL_WIDTH, CELL_HEIGHT);
	
	AutomataSamp1 = new Automata(this,
				     ROWS_LIFE,
				     COLS_LIFE,
				     Samp1Rules,
				     "samples1",
				     SAMP1,
				     430,
				     0);

	AutomataSamp1.setCellSize(CELL_WIDTH, CELL_HEIGHT);

	AutomataSamp2 = new Automata(this,
				     ROWS_LIFE,
				     COLS_LIFE,
				     Samp2Rules,
				     "samples2",
				     SAMP2,
				     860,
				     0);

	AutomataSamp2.setCellSize(CELL_WIDTH, CELL_HEIGHT);
	
	AutomataContour = new Automata(this,
				       ROWS_CONTOUR,
				       COLS_CONTOUR,
				       ContourRules,
				       "contour",
				       CONTOUR,
				       0,
				       0);
	
	AutomataContour.setCellSize(CELL_WIDTH, CELL_HEIGHT);

	// Set up some colors for the automata
	AutomataBass.setColor(255, 255, 255);
	AutomataBass.setStrokeWidth(0);
	
	AutomataSamp1.setColor(0, 0, 0);
	AutomataSamp1.setStrokeWidth(0);
	
	AutomataSamp2.setColor(255, 255, 255);
	AutomataSamp2.setStrokeWidth(0);
	
	AutomataContour.setColor(255, 255, 255, 0);
	AutomataContour.setStrokeWidth(0);
	
	// Add automata to the CAMusicAutomata list for easy management
	CAMusicAutomata.add(AutomataBass);
	CAMusicAutomata.add(AutomataSamp1);
	CAMusicAutomata.add(AutomataSamp2);
	CAMusicAutomata.add(AutomataContour);
	
	// Use a Timer to update at arbitrary regular intervals.
	ArrayList<Automata> lifeAuts = new ArrayList<Automata>();
	lifeAuts.add(AutomataBass);
	lifeAuts.add(AutomataSamp1);
	lifeAuts.add(AutomataSamp2);
	timer = new Timer();
	UpdaterLifes = new CAUpdater(lifeAuts);
	timer.schedule(UpdaterLifes,
		       0L,
		       UPDATE_INTERVAL_LIFE);

	// Use a different timer for the contour automaton (create different
	// update "tempi" this way)
 	timer2 = new Timer();
	UpdaterContour = new CAUpdater(AutomataContour);
	timer2.schedule(UpdaterContour,
			0L,
			UPDATE_INTERVAL_CONTOUR);

	smooth();	
    }

    private Automata getAutomatonAtXY(int x, int y) {
	Automata a = null;
	
	for (Automata aut : CAMusicAutomata) {
	    if (x >= aut.getX() &&
		x < aut.getX() + aut.getWidth() &&
		y >= aut.getY() &&
		y < aut.getY() + aut.getHeight()) {
		
		a = aut;
		break;
	    }
	}
	
	return a;
    }	
    
    private void activateCellAtXY(int x, int y) {
	// Check which automaton we are referring to
	Automata a = getAutomatonAtXY(x, y);

	if (null != a) {
	    try {
		Dimension cellDimension = a.getCellSize();
		int w = a.getColumns() * (int)cellDimension.getWidth();
		int h = a.getRows() * (int)cellDimension.getHeight();

		int row = (int)((double)(mouseY - a.getY())/h * a.getRows());
		int col = (int)((double)(mouseX - a.getX())/w * a.getColumns());

		a.setCell(row, col, 1);

	    } catch (Exception e) {
		System.err.println(e.toString());
		e.printStackTrace();
	    }

	}
    }
    
    private void clearCells() {
	for (Automata a : CAMusicAutomata)
	    a.clearCells();
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
	//		AutomataBass.tick();
    }

    public void mousePressed() {

// 	if (getAutomatonAtXY(mouseX, mouseY) != AutomataContour) {
// 	    if (! UpdaterLifes.isPaused())
// 		UpdaterLifes.pause();
// 	}
// 	else {
// 	    if (! UpdaterContour.isPaused())
// 		UpdaterContour.pause();
// 	}
	
	activateCellAtXY(mouseX, mouseY);
    }

    public void mouseReleased() {
// 	if (mouseX < width/2){
// 	    if (UpdaterLifes.isPaused())
// 		UpdaterLifes.unPause();
// 	}
// 	else if (mouseX >= width/2)
// 	    if (UpdaterContour.isPaused())
// 		UpdaterContour.unPause();
    }
    
    public void mouseDragged() {

// 	if (getAutomatonAtXY(mouseX, mouseY) != AutomataContour) {
// 	    if (! UpdaterLifes.isPaused())
// 		UpdaterLifes.pause();
// 	}
// 	else {
// 	    if (! UpdaterContour.isPaused())
// 		UpdaterContour.pause();
// 	}
	
	activateCellAtXY(mouseX, mouseY);
    }

    public void mouseDown() {

// 	if (mouseX < width/2){
// 	    if (! UpdaterLifes.isPaused())
// 		UpdaterLifes.pause();
// 	}
// 	else if (mouseX >= width/2)
// 	    if (! UpdaterContour.isPaused())
// 		UpdaterContour.pause();

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

// 	if (getAutomatonAtXY(mwe.getX(), mwe.getY()) != AutomataContour) {
// 	    int notches = mwe.getWheelRotation();

// 	    long speed_adjust = MOUSE_WHEEL_ADJUST_AMNT * notches;
// 	    UPDATE_INTERVAL_LIFE += speed_adjust;
// 	    UPDATE_INTERVAL_LIFE = (long) constrain(UPDATE_INTERVAL_LIFE, 1, 10000);

// 	    boolean wasPaused = UpdaterLifes.isPaused();
// 	    CAUpdater new_updater = new CAUpdater(AutomataBass);
// 	    if (wasPaused)
// 		new_updater.pause();
	
// 	    UpdaterLifes.cancel();
// 	    UpdaterLifes = new_updater;
	
// 	    timer.cancel();
// 	    timer = new Timer();
// 	    timer.schedule(UpdaterLifes,
// 			   0L,
// 			   UPDATE_INTERVAL_LIFE);
// 	}
// 	else {

// 	    int notches = mwe.getWheelRotation();

// 	    long speed_adjust = MOUSE_WHEEL_ADJUST_AMNT * notches;
// 	    UPDATE_INTERVAL_CONTOUR += speed_adjust;
// 	    UPDATE_INTERVAL_CONTOUR = (long) constrain(UPDATE_INTERVAL_CONTOUR, 1, 10000);

// 	    boolean wasPaused = UpdaterContour.isPaused();
// 	    CAUpdater new_updater2 = new CAUpdater(AutomataBass);
// 	    if (wasPaused)
// 		new_updater2.pause();
	
// 	    UpdaterContour.cancel();
// 	    UpdaterContour = new_updater2;
	
// 	    timer2.cancel();
// 	    timer2 = new Timer();
// 	    timer2.schedule(UpdaterContour,
// 			   0L,
// 			   UPDATE_INTERVAL_CONTOUR);
// 	}
    }

    /******************
     * draw loop
     ******************/

    public void draw() {
	// colorMode(RGB, 255, 255, 255);
	background(255);

	for (Automata a : CAMusicAutomata)
	    try {
		a.render();
	    } catch (ConcurrentModificationException e) { }
    }


    /******************
     * private classes
     ******************/

    private class CAUpdater extends TimerTask {

	private ArrayList<Automata> auts;
	private boolean paused;

	public CAUpdater(Automata a) {
	    super();
	    auts = new ArrayList<Automata>();
	    auts.add(a);
	    paused = false;
	}

	public CAUpdater(ArrayList<Automata> auts) {
	    super();
	    this.auts = auts;
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
		for (Automata a : auts) {
		    a.tick();

		    NetAddress destination = null;
		    AutomataRules rules = null;
		
		    // Send reports about cell populations
		    switch (a.getID()) {
		    case LIFE1:
			destination = Life1Addr;
			rules = BassRules;
			break;
		    case SAMP1:
			destination = Life2Addr;
			rules = Samp1Rules;
			break;
		    case SAMP2:
			destination = Life3Addr;
			rules = Samp2Rules;
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
	    }
	    
	    //System.out.println("Generation: "+a.getGeneration());
	}

    }


}