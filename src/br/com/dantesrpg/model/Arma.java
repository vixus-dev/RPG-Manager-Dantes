package br.com.dantesrpg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;

public abstract class Arma extends Item {
	private String categoria;
	private Raridade raridade;

	private int danoBase;
	private int ticksDeDano;

	private Atributo atributoMultiplicador;
	private int custoTU;
	private int alcance;

	protected String tipo;
	protected int municaoMaxima;
	protected int municaoAtual;
	protected boolean requerMunicao;

	private String nomeEfeitoOnHit; // Ex: "Sangramento", "Veneno"
	private double chanceEfeitoOnHit;

	private Map<Atributo, Integer> modificadoresDeAtributo;
	private Map<String, Double> modificadoresStatus = new HashMap<>();

	private Habilidade habilidadeDaArma;
	protected List<String> habilidadesConcedidasNomes = new ArrayList<>();

	private TipoAlvo tipoAlvo = TipoAlvo.INDIVIDUAL;
	private int tamanhoArea = 0;

	// Construtor principal
	public Arma(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			int ticksDeDano, Atributo atributo, int custoTU, int alcance, Habilidade habilidadeDaArma, String tipo,
			int municaoMaxima) {
		super(nome, descricao, valorMoedas, false);
		this.categoria = categoria;
		this.raridade = raridade;
		this.danoBase = danoBase;
		this.ticksDeDano = ticksDeDano;
		this.atributoMultiplicador = atributo;
		this.custoTU = custoTU;
		this.alcance = alcance;
		this.habilidadeDaArma = habilidadeDaArma;
		this.tipo = tipo;
		this.municaoMaxima = municaoMaxima;
		this.municaoAtual = municaoMaxima;
		this.requerMunicao = (municaoMaxima > 0);
	}

	// Construtor Simplificado
	public Arma(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			int ticksDeDano, Atributo atributo, int custoTU, int alcance, String tipo, int municaoMaxima) {
		this(nome, categoria, descricao, raridade, valorMoedas, danoBase, ticksDeDano, atributo, custoTU, alcance, null,
				tipo, municaoMaxima);
	}

	// --- Getters ---
	public String getCategoria() {
		return categoria;
	}

	public Raridade getRaridade() {
		return raridade;
	}

	public int getDanoBase() {
		return danoBase;
	}

	public int getTicksDeDano() {
		return ticksDeDano;
	}

	public Atributo getAtributoMultiplicador() {
		return atributoMultiplicador;
	}

	public int getCustoTU() {
		return custoTU;
	}

	public Habilidade getHabilidadeDaArma() {
		return habilidadeDaArma;
	}

	@Override
	public String getTipo() {
		return this.getNome();
	}

	public int getAlcance() {
		return alcance;
	}

	public int getMunicaoAtual() {
		return municaoAtual;
	}

	public int getMunicaoMaxima() {
		return municaoMaxima;
	}

	public boolean isRequerMunicao() {
		return requerMunicao;
	}

	public TipoAlvo getTipoAlvo() {
		return tipoAlvo;
	}

	public void setTipoAlvo(TipoAlvo tipoAlvo) {
		this.tipoAlvo = tipoAlvo;
	}

	public int getTamanhoArea() {
		return tamanhoArea;
	}

	public void setTamanhoArea(int tamanhoArea) {
		this.tamanhoArea = tamanhoArea;
	}

	public boolean gastarMunicao() {
		if (municaoAtual > 0) {
			municaoAtual--;
			return true;
		}
		return false;
	}

	public String getNomeEfeitoOnHit() {
		return nomeEfeitoOnHit;
	}

	public void setNomeEfeitoOnHit(String nome) {
		this.nomeEfeitoOnHit = nome;
	}

	public double getChanceEfeitoOnHit() {
		return chanceEfeitoOnHit;
	}

	public void setChanceEfeitoOnHit(double chance) {
		this.chanceEfeitoOnHit = chance;
	}

	public void setModificadoresDeAtributo(Map<Atributo, Integer> mods) {
		this.modificadoresDeAtributo = mods;
	}

	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		return modificadoresDeAtributo;
	}

	public Map<String, Double> getModificadoresStatus() {
		return modificadoresStatus;
	}

	public void setModificadoresStatus(Map<String, Double> modificadoresStatus) {
		this.modificadoresStatus = modificadoresStatus;
	}

	public List<String> getHabilidadesConcedidasNomes() {
		return habilidadesConcedidasNomes;
	}

	public void addHabilidadeConcedida(String nome) {
		if (nome != null && !nome.isEmpty()) {
			this.habilidadesConcedidasNomes.add(nome);
		}
	}

	public void recarregar() {
		this.municaoAtual = this.municaoMaxima;
	}

	public double getIgnorarDefesaPercentual(Personagem ator, Personagem alvo, EstadoCombate estado) {
		return 0.0; // Padrão
	}

	public void setMunicaoAtual(int municao) {
		this.municaoAtual = Math.max(0, Math.min(municao, this.municaoMaxima));
	}

	public void onCombatStart(Personagem ator, EstadoCombate estado) {
		// Padrão: não faz nada
	}

	public double getBonusDanoArma(Personagem ator, Personagem alvo, EstadoCombate estado, AcaoMestreInput input) {
		return 1.0; // Padrão
	}

	public void onRollSuccess(Personagem ator, Personagem alvo, int rolagem, int dadoMax, double danoDoTick,
			EstadoCombate estado) {
		// Padrão: não faz nada
	}

	public void setCustoTU(int custoTU) {
		this.custoTU = custoTU;
	}

	public br.com.dantesrpg.model.Habilidade getHabilidadeInstancia(String nomeHab) {
		return null;
	}

	public boolean isDanoHibrido(Personagem ator) {
		return false; // Padrão
	}

	public void onAttackHit(Personagem ator, Personagem alvo, double danoCausado, EstadoCombate estado) {
		// Padrão: não faz nada
	}

	public void onDamageTaken(Personagem ator, double danoRecebido, EstadoCombate estado, CombatController controller) {

	}

	public int getAnguloCone() {
		return tamanhoArea;
	}

	public boolean hasAtaqueAlternativoBasico() {
		return isRequerMunicao();
	}

	public String getNomeAtaqueAlternativoBasico() {
		return "Coronhada";
	}

	public String getDescricaoAtaqueAlternativoBasico() {
		return "0.5x Dano, alcance 1";
	}

	public double getMultiplicadorAtaqueAlternativoBasico() {
		return 0.50;
	}

	public int getAlcanceAtaqueAlternativoBasico() {
		return 1;
	}

	public TipoAlvo getTipoAlvoAtaqueAlternativoBasico() {
		return TipoAlvo.INDIVIDUAL;
	}

	public int getAnguloAtaqueAlternativoBasico() {
		return getAnguloCone();
	}

}
