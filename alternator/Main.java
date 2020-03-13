package alternator;

import java.util.Collections;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import petrinet.PetriNet;
import petrinet.Transition;

public class Main {
	
	private static enum Place {
		AReady, BReady, CReady,
		ALast, BLast, CLast,
		CriticalSection;
	}
	
	private static Transition<Place> AEnter, BEnter, CEnter, ALeave, BLeave, CLeave;
	private static PetriNet<Place> net;
	
	private static class Writer implements Runnable {
		private final char sign;
		private final Transition<Place> enter, leave;
		
		public Writer(char sign, Transition<Place> enter, Transition<Place> leave) {
			this.sign = sign;
			this.enter = enter;
			this.leave = leave;
		}
		
		@Override
		public void run() {
			
			try {
				while(true) {
					net.fire(Collections.singleton(enter));
					System.out.print(sign);
					System.out.print('.');
					net.fire(Collections.singleton(leave));
				}
			
			} catch (InterruptedException e) {
	    			System.out.println(Thread.currentThread().getName() + " interrupted.");
				Thread.currentThread().interrupt();
	    		}
		}
	}
	
	private static Transition<Place> enterTransition(Place ready, Place last){
		Map<Place, Integer> input = Collections.singletonMap(ready, 1);
		Collection<Place> reset = Collections.emptySet();
		Collection<Place> inhibitor = new HashSet<Place>();
		inhibitor.add(last);
		inhibitor.add(Place.CriticalSection);
		Map<Place, Integer> output = Collections.singletonMap(Place.CriticalSection, 1);
		
		return new Transition<Place>(input, reset, inhibitor, output);
	}
	
	private static Transition<Place> leaveTransition(Place ready, Place last1, Place last2, Place last3){
		Map<Place, Integer> input = Collections.singletonMap(Place.CriticalSection, 1);
		Collection<Place> reset = new HashSet<Place>();
		reset.add(last1);
		reset.add(last2);
		Collection<Place> inhibitor = Collections.singleton(ready);
		Map<Place, Integer> output = new HashMap<Place, Integer>();
		output.put(ready, 1);
		output.put(last3, 1);
		
		return new Transition<Place>(input, reset, inhibitor, output);
	}
	
	public static void main(String args[]) {
		AEnter = enterTransition(Place.AReady, Place.ALast);
		BEnter = enterTransition(Place.BReady, Place.BLast);
		CEnter = enterTransition(Place.CReady, Place.CLast);
		
		ALeave = leaveTransition(Place.AReady, Place.BLast, Place.CLast, Place.ALast);
		BLeave = leaveTransition(Place.BReady, Place.ALast, Place.CLast, Place.BLast);
		CLeave = leaveTransition(Place.CReady, Place.ALast, Place.BLast, Place.CLast);
		
		Map<Place, Integer> initial = new HashMap<Place, Integer>();
		initial.put(Place.AReady, 1);
		initial.put(Place.BReady, 1);
		initial.put(Place.CReady, 1);

		net = new PetriNet<Place>(initial, true);
		
		Collection<Transition<Place>> allTransitions = new HashSet<Transition<Place>>();
		
		allTransitions.add(AEnter);
		allTransitions.add(BEnter);
		allTransitions.add(CEnter);
		allTransitions.add(ALeave);
		allTransitions.add(BLeave);
		allTransitions.add(CLeave);
		
		Set<Map<Place, Integer>> reachable = net.reachable(allTransitions);
		
		System.out.println("Liczba osiągalnych znakowań: " + reachable.size());
		for(Map<Place, Integer> marking : reachable) {
			Integer inCriticalSection = marking.get(Place.CriticalSection);
			if(inCriticalSection == null) {
				inCriticalSection = 0;
			}
			System.out.println("W sekcji krytycznej: " + inCriticalSection);
		}
		
		Thread A = new Thread(new Writer('A', AEnter, ALeave), "A");
		Thread B = new Thread(new Writer('B', BEnter, BLeave), "B");
		Thread C = new Thread(new Writer('C', CEnter, CLeave), "C");
		
		A.start();
		B.start();
		C.start();
		
		try {
			TimeUnit.SECONDS.sleep(30);
	
		} catch (InterruptedException e) {
			System.out.println("Main thread interrupted.");
			Thread.currentThread().interrupt();
		
		} finally {
			A.interrupt();
			B.interrupt();
			C.interrupt();
		}
	}
}
