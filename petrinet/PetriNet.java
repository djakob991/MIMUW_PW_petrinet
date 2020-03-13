package petrinet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class PetriNet<T> {
	
	private final Map<T, Integer> marking;
	private final boolean fair;
	
	private final Collection<WaitingThreadData> stopped;
	private final Semaphore fireAccessMutex = new Semaphore(1, true);
	private final Semaphore readAndChangeAccessMutex = new Semaphore(1, true);
	
	public Map<T, Integer> getMarking(){
		return marking;
	}
	
	public PetriNet(Map<T, Integer> initial, boolean fair) {
    		this.marking = initial;
    		this.fair = fair;
    	
    		if(fair) {
    			this.stopped = new LinkedList<WaitingThreadData>();
    		} else {
    			this.stopped = new HashSet<WaitingThreadData>();
    		}
   	}
    
    	protected PetriNet<T> copyOf(){
    		Map<T, Integer> marking_copy = new HashMap<T, Integer>();
    	
    		for(Map.Entry<T, Integer> entry : marking.entrySet()) {
    			marking_copy.put(entry.getKey(), entry.getValue());
   	 	}
    	
    		return new PetriNet<T>(marking_copy, fair);
    	}
	
	private class WaitingThreadData {
		private final String name;
		private final Semaphore mutex;
		private final Collection<Transition<T>> transitions;
		
		public Semaphore getMutex() {
			return mutex;
		}
		
		public Collection<Transition<T>> getTransitions(){
			return transitions;
		}
		
		public WaitingThreadData(String name, Semaphore mutex, Collection<Transition<T>> transitions) {
			this.name = name;
			this.mutex = mutex;
			this.transitions = transitions;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	protected Integer numberOfTokens(T place) {
		Integer val = marking.get(place);
		if(val != null) return val;
		return 0;
	}
	
	protected void incrTokens(T place, int d) {
		Integer oldVal = marking.get(place);
		if(oldVal == null) {
			oldVal = 0;
			marking.put(place, d);
		} else {
			marking.put(place, oldVal + d);
		}
		
		if(oldVal + d == 0) {
			marking.remove(place);
		}
	}
	
	private Transition<T> chooseEnable(Collection<Transition<T>> transitions){
		for(Transition<T> transition : transitions) {
			if(transition.isEnabled(this)) {
				return transition;
			}
		}
		return null;
	}
	
	private void findReachable(Set<Map<T, Integer>> result, PetriNet<T> net, Collection<Transition<T>> transitions) {
	    	if(result.contains(net.getMarking())) {
	    		return;
	    	}
	    	
	    	result.add(net.getMarking());
    	
	    	for(Transition<T> transition : transitions) {
	    		if(transition.isEnabled(net)) {
	    			PetriNet<T> newNet = transition.copyAndFire(net);
	    			findReachable(result, newNet, transitions);
	    		}
	    	}
	    	
	}
    
	public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
		try {
	    		readAndChangeAccessMutex.acquire();
	        	PetriNet<T> copy = this.copyOf();
	        	readAndChangeAccessMutex.release();
        	
	        	Set<Map<T, Integer>> result = new HashSet<Map<T, Integer>>();
	        	findReachable(result, copy, transitions);
        	
	        	return result;
	    	} catch (InterruptedException e) {
	    		System.out.println(Thread.currentThread().getName() + " interrupted in method .reachable");
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private void wake() {
	    	for(WaitingThreadData waiting : stopped) {
	    		if(chooseEnable(waiting.getTransitions()) != null) {
	    			stopped.remove(waiting);
	    			waiting.getMutex().release();
	    			return;
	    		}
	    	}
    	
    		fireAccessMutex.release();
    	}
    
    	public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
    	
    		Semaphore currMutex = new Semaphore(0);
    	
    		fireAccessMutex.acquire();
    	
    		Transition<T> toBeFired = chooseEnable(transitions);
    	
    		if(toBeFired == null) {
    			WaitingThreadData newStopped = new WaitingThreadData(Thread.currentThread().getName(), currMutex, transitions);
    			stopped.add(newStopped);	
    	
    		} else {
    			readAndChangeAccessMutex.acquire();
    			toBeFired.fire(this);
    			readAndChangeAccessMutex.release();
    		
    			wake();
    			return toBeFired;
    		}
    	
    		fireAccessMutex.release();
    		currMutex.acquire();
    	
    		toBeFired = chooseEnable(transitions);
    	
    		readAndChangeAccessMutex.acquire();
		toBeFired.fire(this);
		readAndChangeAccessMutex.release();
    	
		wake();
		return toBeFired;
	}
 
}
