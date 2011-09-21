/**
 * AutomataRules.java
 *
 * Rules for updating a cellular automation.
 *
 * 16 September 2011
 *
 **/

import java.util.Collection;

public interface AutomataRules {

	public static final int CELL_AGE = -1;
	public static final int CELL_BIRTH = -2;
	public static final int CELL_DEATH = -3;
	
	public int apply(Cell target, Collection<Cell> neighbors);

}