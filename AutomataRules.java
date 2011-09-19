/**
 * AutomataRules.java
 *
 * Rules for updating a cellular automation.
 *
 * 16 September 2011
 *
 **/

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class AutomataRules {

    // Constants that define how an automaton operating under these rules
    // should act, when applying these rules to a given cell and its neighbors
    public static final int CELL_BIRTH = 0;
    public static final int CELL_DEATH = 1;
    public static final int CELL_AGE = 2;

    // Survival list. Each element of the list gives some number
    // which must exactly correspond  to the number of adjacent live
    // cells to an alive cell in order for that cell to survive to
    // the succeeding generation.
    private ArrayList<Integer> S;
    // The birth list. Each element of the list gives some number
    // which must exactly correspond to the number of adjacent alive
    // cells to a dead cell in order for that cell to be reborn.
    private ArrayList<Integer> B;

    /**
     * CONSTRUCTOR
     *
     * Builds an AutomataRules based on S-B lists, encoded as any type of
     * Collection.
     */
    public AutomataRules(Collection<Integer> S, Collection<Integer> B) {
	this.S = new ArrayList<Integer>();
	this.B = new ArrayList<Integer>();

	this.S.addAll(S);
	this.B.addAll(B);
    }

    /**
     * CONSTRUCTOR
     *
     * Builds an AutomataRules based on S-B lists, encoded in arrays of ints.
     */
    public AutomataRules(int[] S, int[] B) {
	this.S = new ArrayList<Integer>();
	this.B = new ArrayList<Integer>();

	for (int i : S)
	    this.S.add(i);

	for (int i : B)
	    this.B.add(i);
    }

    /**
     * CONSTRUCTOR
     *
     * Builds an AutomataRules based on S-B lists, which are set up according to
     * the following:
     *
     * S = { STATE a1, # required to survive,
     *       STATE a2, # required to survive,
     *       ...
     *       STATE an, # required to survive }
     */
    public AutomataRules(int[][]S, int[][] B) {
	// TODO: Write this constructor
    }
    
    public int apply(Cell target, Collection<Cell> neighbors) {
	int alive, dead;
	alive = dead = 0;
	for (Cell c : neighbors)
	    if (c.isAlive()) alive++;
	dead = neighbors.size() - alive;
		
	if (S.contains(alive) && target.isAlive())
	    return CELL_AGE;
	else if (B.contains(alive) && ! target.isAlive())
	    return CELL_BIRTH;
	else // Could be more complicated later... for now, just means the cell
	    // stays dead
	    return CELL_DEATH;

    }

}