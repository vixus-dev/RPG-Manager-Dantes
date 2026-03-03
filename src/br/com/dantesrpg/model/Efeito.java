package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.Map;

public class Efeito {
	private String nome;
	private TipoEfeito tipo;

	private int duracaoTUInicial;
	private int duracaoTURestante;

	// Armazena os modificadores. Ex: {"FORCA": 2.0}, {"TAXA_CRITICA": -0.10}
	private Map<String, Double> modificadores;

	// Lógica para Efeitos de Dano Contínuo (DoT)
	private int danoPorTick;
	private int intervaloTickTU;
	private int stacks = 0;

	// CONSTRUTOR COMPLETO
	public Efeito(String nome, TipoEfeito tipo, int duracaoTUInicial, Map<String, Double> modificadores,
			int danoPorTick, int intervaloTickTU) {
		this.nome = nome;
		this.tipo = tipo;
		this.duracaoTUInicial = duracaoTUInicial;
		this.duracaoTURestante = this.duracaoTUInicial;
		this.modificadores = modificadores;
		this.danoPorTick = danoPorTick;
		this.intervaloTickTU = intervaloTickTU;
		this.stacks = 0;
	}

	// --- GETTERS ---
	public String getNome() {
		return nome;
	}

	public TipoEfeito getTipo() {
		return tipo;
	}

	public int getDuracaoTUInicial() {
		return duracaoTUInicial;
	}

	public int getDuracaoTURestante() {
		return duracaoTURestante;
	}

	public Map<String, Double> getModificadores() {
		return modificadores;
	}

	public int getDanoPorTick() {
		return danoPorTick;
	}

	public int getIntervaloTickTU() {
		return intervaloTickTU;
	}

	public void decrementarDuracao(int tempoPassado) {
		this.duracaoTURestante -= tempoPassado;
	}

	public void reduzirDuracao(int tempo) {
		this.duracaoTURestante -= tempo;
	}

	public boolean expirou() {
		return this.duracaoTURestante <= 0;
	}

	public int getStacks() {
		return stacks;
	}

	public void setStacks(int stacks) {
		this.stacks = stacks;
	}

	public void setDuracaoTURestante(int duracao) {
		this.duracaoTURestante = duracao;
	}

}