package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.List;

public abstract class Habilidade {
	// --- Metadados Essenciais ---
	private String nome;
	private String descricao;
	private TipoHabilidade tipo;
	private int custoMana;
	private int custoTU;
	private int nivelNecessario;

	// --- Parâmetros de Efeito ---
	private TipoAlvo alvo;
	private int tamanhoArea;
	private double multiplicadorDeDano;
	private int ticksDeDano;
	private List<Efeito> efeitosAplicados;

	public Habilidade(String nome, String descricao, TipoHabilidade tipo, int custoMana, int custoTU,
			int nivelNecessario, TipoAlvo alvo, int tamanhoArea, double multiplicadorDeDano, int ticksDeDano,
			List<Efeito> efeitosAplicados) {
		this.nome = nome;
		this.descricao = descricao;
		this.tipo = tipo;
		this.custoMana = custoMana;
		this.custoTU = custoTU;
		this.nivelNecessario = nivelNecessario;
		this.alvo = alvo;
		this.tamanhoArea = tamanhoArea;
		this.multiplicadorDeDano = multiplicadorDeDano;
		this.ticksDeDano = ticksDeDano;
		this.efeitosAplicados = efeitosAplicados;
	}

	// construtor antigo para manter compatibilidade
	public Habilidade(String nome, String descricao, TipoHabilidade tipo, int custoMana, int custoTU,
			int nivelNecessario, TipoAlvo alvo, double multiplicadorDeDano, int ticksDeDano,
			List<Efeito> efeitosAplicados) {
		this.nome = nome;
		this.descricao = descricao;
		this.tipo = tipo;
		this.custoMana = custoMana;
		this.custoTU = custoTU;
		this.nivelNecessario = nivelNecessario;
		this.alvo = alvo;
		this.multiplicadorDeDano = multiplicadorDeDano;
		this.ticksDeDano = ticksDeDano;
		this.efeitosAplicados = efeitosAplicados;
	}

	// --- Getters ---
	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public TipoHabilidade getTipo() {
		return tipo;
	}

	public int getCustoMana() {
		return custoMana;
	}

	public int getCustoTU() {
		return custoTU;
	}

	public int getNivelNecessario() {
		return nivelNecessario;
	}

	public int getTamanhoArea() {
		return tamanhoArea;
	}

	public TipoAlvo getTipoAlvo() {
		return alvo;
	}

	public double getMultiplicadorDeDano() {
		return multiplicadorDeDano;
	}

	public int getTicksDeDano() {
		return ticksDeDano;
	}

	public List<Efeito> getEfeitosAplicados() {
		return efeitosAplicados;
	}

	public boolean ignoraParedes() {
		return false;
	}

	public List<Integer> getAngulosDesvio() {
		return java.util.Collections.singletonList(0);
	}

	public List<String> getOpcoesSelection() {
		return null;
	}

	public boolean afetaInimigos() {
		return true; // Padrão: Habilidades de AoE são ofensivas
	}

	public boolean afetaAliados() {
		return true; // Padrão: Habilidades de AoE causam fogo amigo
	}

	public boolean afetaSiMesmo() {
		return false; // Padrão: Habilidades de AoE não atingem o centro
	}

	public double getMultiplicadorModificado(Personagem ator, Personagem alvo, EstadoCombate estado) {
		return this.getMultiplicadorDeDano();
	}

	public int getTicksModificados(Personagem ator, AcaoMestreInput input) {
		return this.getTicksDeDano(); // Retorna o valor base (ex: 1, 3, etc.)
	}

	public int getCooldownTU() {
		return 0; // Padrão: Sem cooldown
	}

	public int getNumeroDeAlvos() {
		return 1; // Padrão
	}

	public int getAlcanceMaximo() {
		return -1;
	}

	public int getAnguloCone() {
		return 30; // Padrão de 30°
	}

	public abstract void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager);

	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		executar(conjurador, alvos, estado, manager);
	}
}