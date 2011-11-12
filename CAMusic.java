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
import java.awt.Point;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Scanner;
import java.util.Random;
import java.io.File;
import java.io.FileNotFoundException;

public class CAMusic extends PApplet implements MouseWheelListener, KeyListener {
    
    //////////////////////////
    // SETTINGS FOR CAMUSIC //
    //////////////////////////
    
    // private static final int ROWS = 200;
    // private static final int COLS = 200;
    // private static final int CELL_WIDTH = 3;
    // private static final int CELL_HEIGHT = 3;
    // Try out the following settings for old 1280 x 800 screen res macbook:
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final int BASS_ROWS = 100;
    private static final int BASS_COLS = 128;
    private static final int PROC_ROWS = 50;
    private static final int PROC_COLS = 64;
    private static final int ROWS_CONTOUR = 160;
    private static final int COLS_CONTOUR = 256;
    private static final int CELL_WIDTH = 5;
    private static final int CELL_HEIGHT = 5;
    private static final int PROC_CELL_WIDTH = 10;
    private static final int PROC_CELL_HEIGHT = 10;

    private static long UPDATE_INTERVAL_BASS = 50; // Bass automaton delay
    private static long UPDATE_INTERVAL_PROC = 500; // Bass automaton delay
    private static long UPDATE_INTERVAL_CONTOUR = 2000;	// Contour Automaton delay

    // Automata rule strings, encoded as S/B lists
    // private static final int[] AB_SLIST = { 5 }; // Extreme periodicity!
    // private static final int[] AB_BLIST = { 3, 4, 5 };
    private static final int[] AB_SLIST = {2, 3}; //{ 1, 3, 5, 8 };
    private static final int[] AB_BLIST = {3}; //{ 3, 5, 7 };
    private static final int[] AB_PROC_SLIST = { 5 }; //{5}; //{ 2, 3 };
    private static final int[] AB_PROC_BLIST = {3, 4, 5}; //{ 3 };

    // Second atomaton settings
    private static final int[] CONT_SLIST = { 1, 2, 3, 4, 5}; //{ 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private static final int[] CONT_BLIST = { 3 };
    // Maximum state of any cell, also determines number of colors available to use for states.
    private static final int MAX_STATE = 100;

    
    ///////////////////
    // I/O Constants //
    ///////////////////
    
    private static final int MOUSE_WHEEL_ADJUST_AMNT = 20;

    ////////////////////////////////
    // VARIOUS INTERNAL CONSTANTS //
    ////////////////////////////////
    
    // Identify Automata by number
    private static final int LIFE1 = 0;
    private static final int SAMP1 = 1;
    private static final int CONTOUR = 3;

    ////////////////////////////
    // COMMUNICATION WITH MAX //
    ////////////////////////////

    // Automaton 1-3 UDP ports, to be listened to by MaxMSP
    private static final int OSC_PORT_SEND1 = 12000;		// Where bass OSC messages are sent
    // private static final int OSC_PORT_SEND2 = 12001;		// Where bass processing OSC messages are sent
    // // Contour automaton port
    // private static final int OSC_PORT_SEND_CONT = 12003; 	// Where contour OSC info is sent
    private static final int OSC_PORT_RCV = 8000; // Port on which to listen for
						  // OSC packets

    // Constants pertaining to the data sent to Max
    private static final int MAX_PITCHES = 3; // Max # of pitches that can be sent simultaneously
    private static final int AUT_MIDI_PITCH_OFFSET = 65; // Highest possible pitch

    // Object to send and receive OSC packets
    private OscP5 osc;
    // Remote address (socket) to send our own info to (to MaxMSP)
    private NetAddress toMax;

    ///////////////////////
    // GLOBAL REFERENCES //
    ///////////////////////
    
    // The automata
    private Automata AutomataBass, AutomataBassProc;
    private Automata AutomataContour;
    private ArrayList<Automata> CAMusicAutomata;

    // Automaton rules
    private AutomataRules BassRules;
    private AutomataRules BassProcRules;
    private AutomataRules ContourRules;

    // The SoundPens that draw on the automata
    private CASoundPen BassPen;
    private CASoundPen ProcPen;
    private CASoundPen ContPen;
    private ArrayList<CASoundPen> SoundPens;
    
    // Timers to schedule updates
    private Timer timerBass;
    private Timer timerProc;
    private Timer timerCont;
    private CAUpdater UpdaterLifes;
    private CAUpdater UpdaterProc;
    private CAUpdater UpdaterContour;

    // What was the population in the previous generation?
    private int prevPop;
    
    public void setup() {
	// Set up listeners
	addMouseWheelListener(this);

	// Test settings:
	// size(COLS * CELL_WIDTH * 2, ROWS * CELL_HEIGHT * 2, OPENGL);
	// Sweet macbook settings below:
	size(WIDTH, HEIGHT, OPENGL);
	// Set up OSC stuff
	osc = new OscP5(this, OSC_PORT_RCV);
	toMax = new NetAddress("127.0.0.1", OSC_PORT_SEND1);
	//toMax = new NetAddress("132.162.91.132", OSC_PORT_SEND1);
	// AutBassProcAddr = new NetAddress("127.0.0.1", OSC_PORT_SEND2);
	// ContourAddr = new NetAddress("127.0.0.1", OSC_PORT_SEND_CONT);

	BassRules = new SingleStateRules(AB_SLIST, AB_BLIST, MAX_STATE);
	BassProcRules = new SingleStateRules(AB_PROC_SLIST, AB_PROC_BLIST, MAX_STATE);
	ContourRules = new SingleStateRules(CONT_SLIST, CONT_BLIST, MAX_STATE);

	// Construct the automata
	CAMusicAutomata = new ArrayList<Automata>();

	AutomataBass = new Automata(this,
				    BASS_ROWS,
				    BASS_COLS,
				    BassRules,
				    "life",
				    LIFE1,
				    0,
				    0);

	AutomataBass.setCellSize(CELL_WIDTH, CELL_HEIGHT);

	AutomataBassProc = new Automata(this,
					PROC_ROWS,
					PROC_COLS,
					BassProcRules,
					"samples1",
					SAMP1,
					640,
					0);

	AutomataBassProc.setCellSize(PROC_CELL_WIDTH, PROC_CELL_HEIGHT);

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
	AutomataBass.setColor(0, 0, 0);
	AutomataBass.setStrokeWidth(0);

	AutomataBassProc.setColor(100, 100, 100);
	AutomataBassProc.setStrokeWidth(0);

	AutomataContour.setColor(0, 0, 0, 0); // Completely transparent.
	AutomataContour.setStrokeWidth(0);

	// Add automata to the CAMusicAutomata list for easy management
	CAMusicAutomata.add(AutomataBass);
	CAMusicAutomata.add(AutomataBassProc);
	CAMusicAutomata.add(AutomataContour);

	// Use a Timer to update at arbitrary regular intervals.
	ArrayList<Automata> lifeAuts = new ArrayList<Automata>();
	lifeAuts.add(AutomataBass);
	timerBass = new Timer();
	UpdaterLifes = new CAUpdater(AutomataBass);
	timerBass.schedule(UpdaterLifes,
		       0L,
		       UPDATE_INTERVAL_BASS);

	lifeAuts.add(AutomataBassProc);	
	timerProc = new Timer();
	UpdaterProc = new CAUpdater(AutomataBassProc, new UpdateOperation() {
		public void performOperation() {
		    calculateFrameAndSend();
		}
	    });
	timerProc.schedule(UpdaterProc,
		       0L,
		       UPDATE_INTERVAL_PROC);
	
	// Use a different timer for the contour automaton (create different
	// update "tempi" this way)
	timerCont = new Timer();
	UpdaterContour = new CAUpdater(AutomataContour);
	timerCont.schedule(UpdaterContour,
			   0L,
			   UPDATE_INTERVAL_CONTOUR);

	smooth();

	File initFile = new File("patts/life8");
	AutomataBassProc.initializeCellsFromFile(initFile);
	File initFile2 = new File("patts/life1");
	AutomataBass.initializeCellsFromFile(initFile2);
	// Initialize prevPop here
	prevPop = AutomataBass.getCellCountForOnStates();
	File initFile3 = new File("patts/life3");
	AutomataContour.initializeCellsFromFile(initFile3);
	// Uncomment the following lines for some seriously expensive
	// alpha-blending
	// Not responsible for melted CPUs!
	
	// AutomataBass.setCellFadeout(true);
	// AutomataBassProc.setCellFadeout(true);
	// AutomataContour.setCellFadeout(true);

	// Set up some pens
	SoundPens = new ArrayList<CASoundPen>();
	BassPen = new CASoundPen(AutomataBass);
	ProcPen = new CASoundPen(AutomataBassProc);
	ContPen = new CASoundPen(AutomataContour);
	SoundPens.add(BassPen);
	SoundPens.add(ProcPen);
	SoundPens.add(ContPen);

	OscMessage startMessage = new OscMessage("/startmsg");
	startMessage.add(1);
	osc.send(startMessage, toMax);
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
	return WIDTH;
    }

    public int getHeight() {
	return HEIGHT;
    }

    /******************
     * listeners
     ******************/

    /**
     * oscEvent(msg)
     *
     * This method is called when we have received an OscMessage on
     * OSC_PORT_RCV
     *
     * PLEASE NOTE: for some reason, we get an InvocationTargetException here
     * whenever we try to pass 'msg' onto another method. I wanted to modularize
     * this more here by passing the message directly to a CASoundPen, but I'd
     * also like to avoid error messages...
     **/
    public void oscEvent(OscMessage msg) {
	if (msg.addrPattern().equals("/penevent")) {
	    // FORMAT =====
	    // [penevent : AUTID : PRESSURE : TYPE : X : Y]
	    if (msg.arguments().length < 5) {
		System.err.println("Invalid OscMessage received of addrPattern \"/penevent\": ");
		System.err.println("===> "+msg.toString());
		return;
	    }
	    
	    int autID = msg.get(0).intValue();
	    int penPressure = msg.get(1).intValue();
	    int penType = msg.get(2).intValue();
	    Point loc = new Point(msg.get(3).intValue(),
				  msg.get(4).intValue());

	    // Transmit this information to the proper pen
	    CASoundPen thePen = SoundPens.get(autID);
	    thePen.setType(penType);
	    thePen.setPenPressure(penPressure);
	    thePen.setPenPosition(loc);

	    // Use the pen!
	    thePen.applyPen();

	    //BassPen.setPenPressure(msg.get(0).intValue());
	}

	else if (msg.addrPattern().equals("/penPos")) {	
    	    // FORMAT =====
	    // [penpos : amplitude(f) : specdens(f) ]
	    if (msg.arguments().length < 2) {
		System.err.println("Invalid OscMessage received of addrPattern \"/penPos\": ");
		System.err.println("===> "+msg.toString());
		return;
	    }

	    // the pen for the contour automaton
	    CASoundPen thePen = SoundPens.get(2);
	    thePen.setType(CASoundPen.AGE_CELLS);


	    //	    System.err.println("DEBUG : packet received = "+msg.get(1).floatValue());
	    
	    int x = (int)(msg.get(0).floatValue() * AutomataContour.getColumns()) % AutomataContour.getColumns();
	    int y = (int)(msg.get(1).floatValue() * AutomataContour.getRows()) % AutomataContour.getRows();;
	    
	    thePen.setPenPressure(1);
	    thePen.setPenPosition(new Point(x, y));
	    //	    System.err.println("DEBUG : Flipping on contour at "+x+", "+y);
	    thePen.applyPen();
	}

	//BassPen.processMessage(msg);
    }

    public void mouseClicked() {
	activateCellAtXY(mouseX, mouseY);
    }

    public void mousePressed() {

	activateCellAtXY(mouseX, mouseY);
    }

    public void mouseReleased() {
	// TODO: implement as needed
    }

    public void mouseDragged() {
	activateCellAtXY(mouseX, mouseY);
    }

    public void mouseDown() {
	// TODO: implement as needed
    }

    public void keyPressed(KeyEvent e) {
	int c = e.getKeyCode();

	switch(c) {
	case KeyEvent.VK_DELETE:
	    clearCells();
	    break;
	case KeyEvent.VK_SPACE:
	    //	    ProcPen.setPenPressure(1);
	    BassPen.setPenPressure(1);
	    //	    ProcPen.applyPen();
	    BassPen.applyPen();
	    break;
	default:
	    break;
	}
    }

    public void mouseWheelMoved(MouseWheelEvent mwe) {

	// 	if (getAutomatonAtXY(mwe.getX(), mwe.getY()) != AutomataContour) {
	// 	    int notches = mwe.getWheelRotation();

	// 	    long speed_adjust = MOUSE_WHEEL_ADJUST_AMNT * notches;
	// 	    UPDATE_INTERVAL_BASS += speed_adjust;
	// 	    UPDATE_INTERVAL_BASS = (long) constrain(UPDATE_INTERVAL_BASS, 1, 10000);

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
	// 			   UPDATE_INTERVAL_BASS);
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

		//		BassPen.applyPen();
	    } catch (ConcurrentModificationException e) { }
    }

    /*******************
     * method for grabbing frames from the automata and
     * sending relevant information over to Max
     *******************/
    private void calculateFrameAndSend() {
	// ---- First, get the current pitch from the top-right automaton ----
	// The way we do this is to go through each row of the Automaton and
	// find out which one(s) have the most alive cells in that row. As each
	// row corresponds to a pitch, we select at most MAX_PITCHES of these
	// (at random) and send them to Max for immediate display on the
	// performer's screen.

	// Array for storing how many cells are alive in each row
	int[] aliveCells = new int[AutomataBassProc.getRows()];
	
	// Keep track of the greatest rows we've found so far
	LinkedList<Integer> greatestRows = new LinkedList<Integer>();
	
	for (int row=0; row < AutomataBassProc.getRows(); row++) {
	    // Note that the point is actually x,y... we will translate into
	    // row, col later.
	    int count = AutomataBassProc.getCellCountForOnStates(new Point(0, row),
								 new Point(AutomataBassProc.getRows()-1, row));
	    aliveCells[row] = count;
	}

	// Figure out which rows were the greatest
	for (int i =0; i<aliveCells.length; i++) {
	    int count = aliveCells[i];
	    if (greatestRows.size() == 0 || count > greatestRows.get(0)) {
		greatestRows.add(i);
		while (count > aliveCells[greatestRows.get(0)])
		    greatestRows.remove(0);
	    }
	}
	
	// Come up with our final list of rows now, by removing enough pitches
	// to fit into our "max chord" size
	int numToRemove = greatestRows.size() - MAX_PITCHES;
	Random rand = new Random();
	while (numToRemove-- > 0)
	    greatestRows.remove(rand.nextInt(greatestRows.size()));

	// Ok, now we convert the row numbers into pitches (MIDI)
	ArrayList<Integer> midiPitches = new ArrayList<Integer>();
	for (Integer i : greatestRows)
	    //	    midiPitches.add(AUT_MIDI_PITCH_OFFSET - i); // This line
	    //	    tries to send a real integer MIDI value
	    // The following line just sends the row number:
	    //	    midiPitches.add(i);
	    // The following line sends the distance (positive or negative) of
	    //	    the row number from the middle row of the automaton:
	    midiPitches.add(i - PROC_ROWS);

	// Sick. Now it's time for the hard part: calculate the rhythm from the
	// frame. ----------
	// The way we do this is by looking at the change (delta) in population
	// in the top-left automaton (AutomataBass). We send this result over to
	// Max, which then will create a "tick" at a position equal to that
	// quantity (units?) away from the "current time", expressed graphically
	// as the position of a scrolling tape relative to an arrow.
	int deltaPop = AutomataBass.getCellCountForOnStates() - prevPop;

	// Ok. We'll let max deal with how it want to use and display deltaPop
	// as a rhythm to the performer. All we have to do now is build and send
	// the OSC Message.
	OscMessage theMsg = new OscMessage("/frameinfo");
	theMsg.add(deltaPop);
	for (Integer i : midiPitches)
	    theMsg.add(i);

	osc.send(theMsg, toMax);

	prevPop = AutomataBass.getCellCountForOnStates();
    }

    /******************
     * private classes
     ******************/

    private class CAUpdater extends TimerTask {

	private ArrayList<Automata> auts;
	private boolean paused;
	private UpdateOperation op;

	public CAUpdater(Automata a, UpdateOperation op) {
	    super();
	    this.op = op;
	    auts = new ArrayList<Automata>();
	    auts.add(a);
	    paused = false;
	}

	public CAUpdater(ArrayList<Automata> auts, UpdateOperation op) {
	    super();
	    this.op = op;
	    this.auts = auts;
	    paused = false;
	}

	public CAUpdater(Automata a) {
	    this (a, null);
	}

	public CAUpdater(ArrayList<Automata> auts) {
	    this(auts, null);
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

		    // Do whatever it is that we do, in addition to updating the automaton.
		    if (null != op)
			op.performOperation();
		    
		    // NetAddress destination = null;
		    // AutomataRules rules = null;

		    // // Send reports about cell populations
		    // switch (a.getID()) {
		    // case LIFE1:
		    // 	destination = AutBassAddr;
		    // 	rules = BassRules;
		    // 	break;
		    // case SAMP1:
		    // 	destination = AutBassProcAddr;
		    // 	rules = BassProcRules;
		    // 	break;
		    // case CONTOUR:
		    // 	destination = ContourAddr;
		    // 	rules = ContourRules;
		    // 	break;
		    // default:
		    // 	break;
		    // }

		    // if (null != destination) {
		    // 	OscMessage popMsg = new OscMessage("/population");
		    // 	popMsg.add(a.getCellCountForOnStates());
		    // 	OscMessage oldPopMsg = new OscMessage("/oldestcount");
		    // 	oldPopMsg.add(a.getCellCountForState(rules.getMaxCellState()));

		    // 	osc.send(popMsg, destination);
		    // 	osc.send(oldPopMsg, destination);
		    // }
		}
	    }

	    //System.out.println("Generation: "+a.getGeneration());
	}

    }

    private interface UpdateOperation {
	public void performOperation();
    }

}