package multiplicator;

import java.util.Scanner;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import petrinet.PetriNet;
import petrinet.Transition;

public class Main {
	
	private static enum Place {
		A, B, Buffer, Dest, Control;
	}
	
	private static Collection<Transition<Place>> transitions = new HashSet<Transition<Place>>();
	private static PetriNet<Place> net;
	
	private static class Helper implements Runnable {
		
		private int counter = 0;
		
		@Override
		public void run() {
			try {
				while(true) {
					net.fire(transitions);
					counter++;
				}
			} catch(InterruptedException e) {
				System.out.println(Thread.currentThread().getName() + " interrupted.");
				Thread.currentThread().interrupt();
			} finally {
				System.out.println(Thread.currentThread().getName() + " odpalił " + counter + " przejść.");
			}
		}
	}
	
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		int A = scan.nextInt();
		int B = scan.nextInt();
		scan.close();
		
		Map<Place, Integer> eaterInput = new HashMap<Place, Integer>();
		eaterInput.put(Place.A, 1);
		eaterInput.put(Place.Control, 1);
		Collection<Place> eaterReset = Collections.emptySet();
		Collection<Place> eaterInhibitor = Collections.singleton(Place.Buffer);
		Map<Place, Integer> eaterOutput = Collections.emptyMap();
		
		Transition<Place> eater = new Transition<Place>(eaterInput, eaterReset, eaterInhibitor, eaterOutput);
		
		
		Map<Place, Integer> renewerInput = Collections.emptyMap();
		Collection<Place> renewerReset = Collections.emptySet();
		Collection<Place> renewerInhibitor = new HashSet<Place>();
		renewerInhibitor.add(Place.B);
		renewerInhibitor.add(Place.Control);
		Map<Place, Integer> renewerOutput = Collections.singletonMap(Place.Control, 1);
		
		Transition<Place> renewer = new Transition<Place>(renewerInput, renewerReset, renewerInhibitor, renewerOutput);
		
		
		Map<Place, Integer> copierInput = Collections.singletonMap(Place.B, 1);
		Collection<Place> copierReset = Collections.emptySet();
		Collection<Place> copierInhibitor = Collections.singleton(Place.Control);
		Map<Place, Integer> copierOutput = new HashMap<Place, Integer>();
		copierOutput.put(Place.Buffer, 1);
		copierOutput.put(Place.Dest, 1);
		
		Transition<Place> copier = new Transition<Place>(copierInput, copierReset, copierInhibitor, copierOutput);
		
		
		Map<Place, Integer> returnerInput = new HashMap<Place, Integer>();
		returnerInput.put(Place.Buffer, 1);
		returnerInput.put(Place.Control, 1);
		Collection<Place> returnerReset = Collections.emptySet();
		Collection<Place> returnerInhibitor = Collections.emptySet();
		Map<Place, Integer> returnerOutput = new HashMap<Place, Integer>();
		returnerOutput.put(Place.B, 1);
		returnerOutput.put(Place.Control, 1);
		
		Transition<Place> returner = new Transition<Place>(returnerInput, returnerReset, returnerInhibitor, returnerOutput);
		
		
		Map<Place, Integer> endInput = Collections.singletonMap(Place.Control, 1);
		Collection<Place> endReset = Collections.emptySet();
		Collection<Place> endInhibitor = new HashSet<Place>();
		endInhibitor.add(Place.A);
		endInhibitor.add(Place.Buffer);
		Map<Place, Integer> endOutput = Collections.singletonMap(Place.Control, 1);
		
		Transition<Place> end = new Transition<Place>(endInput, endReset, endInhibitor, endOutput);
		
		
		transitions.add(eater);
		transitions.add(renewer);
		transitions.add(copier);
		transitions.add(returner);
		
		
		Map<Place, Integer> initial = new HashMap<Place, Integer>();
		
		initial.put(Place.A, A);
		initial.put(Place.B, B);
		initial.put(Place.Control, 1);
		
		net = new PetriNet<Place>(initial, true);
		
		
		Thread h1 = new Thread(new Helper(), "helper1");
		Thread h2 = new Thread(new Helper(), "helper2");
		Thread h3 = new Thread(new Helper(), "helper3");
		Thread h4 = new Thread(new Helper(), "helper4");
		
		h1.start();
		h2.start();
		h3.start();
		h4.start();
		
		try {
			net.fire(Collections.singleton(end));
			System.out.println("Iloczyn: " + net.getMarking().get(Place.Dest));
			
		} catch (InterruptedException e) {
			System.out.println("Main thread interrupted.");
			Thread.currentThread().interrupt();
		} finally {
			h1.interrupt();
			h2.interrupt();
			h3.interrupt();
			h4.interrupt();
		}	
	}
}
