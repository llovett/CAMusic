import processing.core.*;

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

	// The automaton
	private Automata aut;
	// Automaton rules
	private AutomataRules rules;
	// Rules lists for the AutomataRules
	private TreeSet<Integer> survive, birth;

	// Timer to schedule updates
	private Timer timer;

	// Settings for CAMusic
	private static final int ROWS = 100;
	private static final int COLS = 100;
	private static final int CELL_WIDTH = 20;
	private static final int CELL_HEIGHT = 20;
	private static long UPDATE_INTERVAL = 500; // 1 second

	// Automata rule strings, encoded as B/S lists
	private static final int[] SLIST = { };
	private static final int[] BLIST = { 2 };

	public void setup() {
		// Set up listeners
		addMouseWheelListener(this);

		rules = new SingleStateRules(SLIST, BLIST);

		// Construct the automaton
		aut = new Automata(this,
				ROWS,
				COLS,
				WIDTH,
				HEIGHT,
				rules);

		aut.setDiagonalsUsage(true);
		Cell.setSize(CELL_WIDTH, CELL_HEIGHT);
		size(COLS * CELL_WIDTH, ROWS * CELL_HEIGHT, JAVA2D);

		// ... instead, use a Timer to update at arbitrary regular intervals.
		timer = new Timer();
		timer.schedule(new CAUpdater(aut),
				0L,
				UPDATE_INTERVAL);

	}

	private void activateCellAtXY(int x, int y) {
		try {
			int row = (int)((double)mouseY/width * aut.getRows());
			int col = (int)((double)mouseX/height * aut.getColumns());

			Cell c = aut.getCell(row, col);
			c.setCurrentState(true);
		} catch (NoSuchElementException e) {

		}
	}
	
	private void clearCells() {
		for (int row = 0; row < ROWS; row++)
			for (int col = 0; col < COLS; col++) {
				Cell c = aut.getCell(row, col);
				c.setCurrentState(false);
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

	public void mouseClicked() {
		activateCellAtXY(mouseX, mouseY);
	}

	public void mousePressed() {
		activateCellAtXY(mouseX, mouseY);
	}

	public void mouseDragged() {
		activateCellAtXY(mouseX, mouseY);
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
		int notches = mwe.getWheelRotation();

		long speed_adjust = MOUSE_WHEEL_ADJUST_AMNT * notches;
		UPDATE_INTERVAL += speed_adjust;
		UPDATE_INTERVAL = (long) constrain(UPDATE_INTERVAL, 5, 10000);


		timer.cancel();
		timer = new Timer();
		timer.schedule(new CAUpdater(aut),
				0L,
				UPDATE_INTERVAL);
	}

	/******************
	 * draw loop
	 ******************/

	public void draw() {
		background(255);

		// Draw the automaton
		aut.render();

	}


	/******************
	 * private classes
	 ******************/

	private class CAUpdater extends TimerTask {

		private Automata a;

		public CAUpdater(Automata a) {
			super();
			this.a = a;
		}

		public void run() {
			a.tick();
			//System.out.println("Generation: "+a.getGeneration());
		}

	}


}