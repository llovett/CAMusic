import java.util.Arrays;
import java.util.Collection;

public class MultiStatesRules implements AutomataRules {

    private int[][] S;
    private int[][] B;

    private int maxState;

    public MultiStatesRules(int[][] S, int[][] B, int maxState) {		
	this.S = S;
	this.B = B;

	this.maxState = maxState;
    }

    @Override
	public int apply(Cell target, Collection<Cell> neighbors) {
	int[] statesCountList = new int[(S.length > B.length? S.length : B.length)];
	Arrays.fill(statesCountList, 0);

	// Get a count of all the neighbors in each state
	for (Cell c : neighbors)
	    if (c.getState() < statesCountList.length)
		statesCountList[c.getState()]++;

	int result = CELL_AGE;

	// Possible results for an alive cell: age, death
	if (target.isAlive()) {
	    boolean age = true;

	    // See if the number of states we've seen in our neighbors
	    // corresponds to anything in our S_l list
	    for (int state = 0; state < S.length; state++) {
		// counts_for_state is the list of how many neighbors we need
		// at "state" for given target cell to survive to the next generation.
		int[] counts_for_state = S[state];
		if (! (has(counts_for_state, statesCountList[state]) >= 0))
		    // If the number of neighbors we have in a particular state
		    // does not match anything in our survival list, then the
		    // given cell does not survive.
		    age = false;
	    }

	    if (age) result = CELL_AGE;
	    else result = CELL_DEATH;
	}
	// Possible results for a dead cell: death, birth
	else if (! target.isAlive()) {
	    boolean birth = true;

	    // See if the number of states we've seen in our neighbors
	    // corresponds to anything in our S_l list
	    for (int state = 0; state < B.length; state++) {
		// counts_for_state is the list of how many neighbors we need
		// at "state" for given target cell to be born into the next generation.
		int[] counts_for_state = B[state];
		if (! (has(counts_for_state, statesCountList[state]) >= 0))
		    // If the number of neighbors we have in a particular state
		    // does not match anything in our birth list, then the
		    // given cell is not born into the next generation.
		    birth = false;
	    }

	    if (birth) result = CELL_BIRTH;
	    else result = CELL_DEATH;
	}

	return result;
    }

    public int getMaxCellState() {
	return maxState;
    }

    public void setMaxCellState(int state) {
	maxState = state;
    }
    
    
    public int has(int[] arr, int key) {
	int ret = -1;

	int insert = Arrays.binarySearch(arr, key);
	if (insert < arr.length && insert >= 0)
	    if (arr[insert] == key)
		ret = insert;

	return ret;
    }

}
