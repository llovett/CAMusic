import java.util.Arrays;
import java.util.Collection;

public class SingleStateRules implements AutomataRules {

    private int[] S;
    private int[] B;

    private int maxState;
	
    public SingleStateRules(int[] S, int[] B) {
	this.S = S;
	this.B = B;
		
	Arrays.sort(S);
	Arrays.sort(B);
    }

    public SingleStateRules(int[] S, int[] B, int maxState) {
	this.S = S;
	this.B = B;
	this.maxState = maxState;
		
	Arrays.sort(S);
	Arrays.sort(B);
    }

    public int getMaxCellState() {
	return maxState;
    }

    public void setMaxCellState(int state) {
	maxState = state;
    }
    
    public int apply(Cell target, Collection<Cell> neighbors) {
	int ret = CELL_AGE;

	int dead, alive;
	dead = alive = 0;

	for (Cell c : neighbors)
	    if (c.isAlive())
		alive++;

	dead = neighbors.size() - alive;

	if (target.isAlive()) {
	    if (has(S, alive) >= 0)
		if (target.getState() < maxState)
		    ret = CELL_AGE;
		else
		    ret = CELL_STASIS;
	    else
		ret = CELL_DEATH;
	}
	else if (! target.isAlive()) {
	    if (has(B, alive) >= 0)
		ret = CELL_BIRTH;
	    else
		ret = CELL_STASIS;
	}

	return ret;
    }

    private int has(int[] arr, int key) {
	int ret = -1;
		
	int insert = Arrays.binarySearch(arr, key);
	if (insert < arr.length && insert >= 0)
	    if (arr[insert] == key)
		ret = insert;
		
	return ret;
    }
	
}
