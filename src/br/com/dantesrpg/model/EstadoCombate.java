package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EstadoCombate {
	private boolean combateAtivo = true;
	private List<Personagem> combatentes;
	private Personagem atorAtual;
	private int tickCounter; // Contador para gerenciar os ticks de DoT
	private int tempoGlobalCombate = 0;
	private int xpAcumuladoPool = 0;

	public EstadoCombate(List<Personagem> personagensIniciais) {
		this.combatentes = new ArrayList<>(personagensIniciais);

		// Ordena a lista para estabelecer a iniciativa assim que o combate é criado
		this.combatentes.sort(Comparator.comparingInt(Personagem::getPlacarIniciativa).reversed());

		// Inicializa o estado
		this.atorAtual = null; // O primeiro ator será definido pelo CombatManager
		this.tickCounter = 0;

		// Atribui o TU inicial com base na ordem de iniciativa
		System.out.println("--- Ordem de Iniciativa Definida ---");
		for (int i = 0; i < this.combatentes.size(); i++) {
			Personagem p = this.combatentes.get(i);
			p.setContadorTU(i); // 1º=0 TU, 2º=1 TU, etc.
			System.out.println((i + 1) + ". " + p.getNome() + " (DES: " + p.getAtributosFinais().get(Atributo.DESTREZA)
					+ ") - Inicia em " + p.getContadorTU() + " TU");
		}
	}

	public void resetarIniciativa() {
		this.combatentes.sort(Comparator.comparingInt(Personagem::getPlacarIniciativa).reversed());

		System.out.println("--- Resetando Iniciativa ---");
		for (int i = 0; i < this.combatentes.size(); i++) {
			Personagem p = this.combatentes.get(i);
			// Reinicia o TU: 1º lugar = 0, 2º = 1, etc.
			p.setContadorTU(i);
			// Reseta movimento
			p.setMovimentoRestanteTurno(p.getMovimento());
			System.out.println((i + 1) + ". " + p.getNome() + " - TU: " + p.getContadorTU());
		}
		this.atorAtual = (!combatentes.isEmpty()) ? combatentes.get(0) : null;
	}

	public void adicionarXpAoPool(int xp) {
		this.xpAcumuladoPool += xp;
	}

	public int sacarXpDoPool() {
		int total = this.xpAcumuladoPool;
		this.xpAcumuladoPool = 0;
		return total;
	}

	// --- Getters e Setters ---
	public List<Personagem> getCombatentes() {
		return combatentes;
	}

	public Personagem getAtorAtual() {
		return atorAtual;
	}

	public void setAtorAtual(Personagem atorAtual) {
		this.atorAtual = atorAtual;
	}

	public int getTickCounter() {
		return tickCounter;
	}

	public void setTickCounter(int tickCounter) {
		this.tickCounter = tickCounter;
	}

	public boolean isCombateAtivo() {
		return combateAtivo;
	}

	public void setCombateAtivo(boolean combateAtivo) {
		this.combateAtivo = combateAtivo;
	}

	public int getTempoGlobalCombate() {
		return tempoGlobalCombate;
	}

	public void avancarTempoGlobal(int tempo) {
		this.tempoGlobalCombate += tempo;
	}

}