package it.polito.tdp.PremierLeague.model;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.PremierLeague.db.PremierLeagueDAO;
import it.polito.tdp.PremierLeague.model.Event.EventType;

public class Model {
	
	private PremierLeagueDAO dao ;
	private SimpleDirectedWeightedGraph<Action, DefaultWeightedEdge> grafo;
	private Map<Integer, Action> actions ;
	
	private double pesoMigliore ;
	private int giocatoreMigliore ;
	
	//Simulazione
	private PriorityQueue<Event> queue ;
	//Parametri input
	private int N ;
	private Match matchSim ;
	private Team homeTeam ;
	private Team awayTeam ;
	//Parametri output
	private int numeroEspulsi ;
	private String risultatoFinale ;
	
	public Model() {
		dao = new PremierLeagueDAO() ;
		pesoMigliore = -1 ;
	}
	
	public List<Match> listAllMatches(){
		return dao.listAllMatches() ;
	}
	
	public void creaGrafo(Match match) {
		grafo = new SimpleDirectedWeightedGraph<Action, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
		actions = new HashMap<Integer, Action>() ;
		
		//Aggiungo i vertici
		dao.listAllMatchActions(match, actions) ;
		Graphs.addAllVertices(this.grafo, actions.values()) ;
		
		//Aggiungo gli archi
		for(Action a : actions.values()) {
			for(Action b : actions.values()) {
				if(a.getTeamID() != b.getTeamID() && a.getEfficienty() != 0 && b.getEfficienty() != 0) {
					double aEfficienty = a.getEfficienty() ;
					double bEfficienty = b.getEfficienty() ;
					//Controllo che non esista già
					DefaultWeightedEdge e = this.grafo.getEdge(a, b);
					if(e == null) {
						if(aEfficienty > bEfficienty) {
							Graphs.addEdgeWithVertices(this.grafo, a, b, aEfficienty-bEfficienty) ;
						}else {
							Graphs.addEdgeWithVertices(this.grafo, b, a, bEfficienty-aEfficienty) ;
						}
					}
				}
			}
		}
		
	}
	
	public int getNVertici() {
		if(this.grafo != null) {
			return this.grafo.vertexSet().size() ;
		}
		return 0 ;
	}
	
	public int getNArchi() {
		if(grafo != null)
			return grafo.edgeSet().size() ;
		
		return 0;
	}
	
	public Set<Action> getVertici(){
		return this.grafo.vertexSet() ;
	}
	
	public void calcolaGiocatoreMigliore(){
	
		for(Action a : this.getVertici()) {
			double peso = 0.0 ;
			for(DefaultWeightedEdge  dwe : this.grafo.outgoingEdgesOf(a)) {
				peso += this.grafo.getEdgeWeight(dwe) ;
			}
			for(DefaultWeightedEdge  dwe : this.grafo.incomingEdgesOf(a)) {
				peso -= this.grafo.getEdgeWeight(dwe) ;
			}
			if(peso > pesoMigliore) {
				pesoMigliore = peso ;
				giocatoreMigliore = a.getPlayerID() ;
			}
		}
		
		
	}

	public double getPesoMigliore() {
		return  pesoMigliore;
	}

	public Player getGiocatoreMigliore() {
		for(Player p : dao.listAllPlayers()) {
			if(p.getPlayerID() == this.giocatoreMigliore) {
				return p ;
			}
		}
		return null;
	}

	public void run(int N, Match match) {
		 
		this.queue = new PriorityQueue<Event>() ;
		this.matchSim = match ;
		this.N = N ;
		this.initTeam() ;
		this.queue.add(new Event(EventType.ESPULSIONE)) ;
		for(int i=1; i<=N; i++) {
			double num = Math.random() ;
			if(num<0.5) {
				this.queue.add(new Event(EventType.GOAL)) ;
			}else if(num<0.8 && num>=0.5) {
				this.queue.add(new Event(EventType.ESPULSIONE)) ;
			}else {
				this.queue.add(new Event(EventType.INFORTUNIO)) ;
			}
		}
		
		//Ciclo di simulazione
		while(!this.queue.isEmpty()) {
			Event e = this.queue.poll();
			processEvent(e) ;
		}
		
	}

	private void processEvent(Event e) {
		switch(e.getType()) {
		case GOAL: 
			if(this.homeTeam.getNumeroPlayers() != this.awayTeam.getNumeroPlayers()) {
				if(this.homeTeam.getNumeroPlayers() > this.awayTeam.getNumeroPlayers()){
					this.homeTeam.setGoal();
				}else {
					this.awayTeam.setGoal();
				}
			}else {
				this.calcolaGiocatoreMigliore();
				for(Action a : this.actions.values()) {
					if(this.giocatoreMigliore == a.playerID) {
						if(a.teamID == this.homeTeam.teamID) {
							this.homeTeam.setGoal() ;
						}else {
							this.awayTeam.setGoal() ;
						}
					}
				}
			}
			break;
		case ESPULSIONE:
			double num = Math.random() ;
			if(num < 0.6) {
				this.calcolaGiocatoreMigliore();
				for(Action a : this.actions.values()) {
					if(this.giocatoreMigliore == a.playerID) {
						if(a.teamID == this.homeTeam.teamID) {
							this.homeTeam.setNumeroPlayers();
						}else {
							this.awayTeam.setNumeroPlayers();
						}
					}
				}
			}else {
				this.calcolaGiocatoreMigliore();
				for(Action a : this.actions.values()) {
					if(this.giocatoreMigliore == a.playerID) {
						if(a.teamID == this.homeTeam.teamID) {
							this.awayTeam.setNumeroPlayers();
							this.numeroEspulsi++ ;
						}else {
							this.homeTeam.setNumeroPlayers();
							this.numeroEspulsi++ ;
						}
					}
				}
			}
			break;
		case INFORTUNIO:
			break ;
		}
		
	}
	
	private void initTeam() {
		for(Team t : dao.listAllTeams()) {
			if(t.getTeamID() == this.matchSim.getTeamHomeID()) {
				this.homeTeam = t ;
			}
			
			if(t.getTeamID() == this.matchSim.getTeamAwayID()) {
				this.awayTeam = t ;
			}
		}
	}

	public int getHomeTeam() {
		return this.homeTeam.getNumeroPlayers();
	}

	public int getAwayTeam() {
		return this.awayTeam.getNumeroPlayers();
	}
	
	
}
