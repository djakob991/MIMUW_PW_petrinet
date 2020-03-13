package petrinet;

import java.util.Collection;
import java.util.Map;

public class Transition<T> {
	
	private final Map<T, Integer> input;
	private final Collection<T> reset;
	private final Collection<T> inhibitor;
	private final Map<T, Integer> output;
	
    	public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
    		
    		this.input = input;
    		this.reset = reset;
    		this.inhibitor = inhibitor;
    		this.output = output;
   	 }
    
    	protected boolean isEnabled(PetriNet<T> net) {
    		for(Map.Entry<T, Integer> entry : input.entrySet()) {
    			if(net.numberOfTokens(entry.getKey()) < entry.getValue()) {
    				return false;
    			}
    		}
    	
    		for(T place : inhibitor) {
    			if(net.numberOfTokens(place) != 0) {
    				return false;
    			}
    		}
    	
    	
    		return true;
    	}
    
    	protected void fire(PetriNet<T> net) {
    		for(Map.Entry<T, Integer> entry : input.entrySet()) {
    			net.incrTokens(entry.getKey(), -1 * entry.getValue());
    		}
    	
    		for(T place : reset) {
    			net.incrTokens(place, -1 * net.numberOfTokens(place));
    		}
    	
    		for(Map.Entry<T, Integer> entry : output.entrySet()) {
    			net.incrTokens(entry.getKey(), entry.getValue());
    		}
    	}
    
	protected PetriNet<T> copyAndFire(PetriNet<T> net){
 	   	PetriNet<T> copy = net.copyOf();
 	   	fire(copy);
 	   	return copy;
    	}
   
    
}
