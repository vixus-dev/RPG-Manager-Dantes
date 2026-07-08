package br.com.dantesrpg.model;

import java.util.HashMap;
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
	private int wielding = 1;

	protected String tipo;
	protected int municaoMaxima;
	protected int municaoAtual;
	protected boolean requerMunicao;

	private String nomeEfeitoOnHit; // Ex: "Sangramento", "Veneno"
	private double chanceEfeitoOnHit;

	private Map<Atributo, Integer> modificadoresDeAtributo;
	private Map<String, Double> modificadoresStatus = new HashMap<>();

	private Habilidade habilidadeDaArma;

	private TipoAlvo tipoAlvo = TipoAlvo.INDIVIDUAL;
	private int tamanhoArea = 0;

	// --- Ataque Especial (configurável via JSON) ---
	private boolean hasSpecialAttack = false;
	private String specialAttackName;
	private double specialAttackDmg = 1.0;
	private double specialAttackCd = 1.0;
	private String specialAttackType = "INDIVIDUAL";
	private int specialAttackSize = 0;

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
		return (int) Math.round(danoBase * getMultiplicadorOverclock());
	}

	public int getDanoBaseOriginal() {
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

	public boolean isDuasMaos() {
		return this.wielding >= 2;
	}

	public int getWielding() {
		return Math.max(1, Math.min(2, wielding));
	}

	public void setWielding(int wielding) {
		this.wielding = Math.max(1, Math.min(2, wielding));
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
		if (modificadoresDeAtributo == null || getGrauOverclock() == 0) return modificadoresDeAtributo;
		Map<Atributo, Integer> resultado = new HashMap<>();
		double mult = getMultiplicadorOverclock();
		for (Map.Entry<Atributo, Integer> e : modificadoresDeAtributo.entrySet()) {
			resultado.put(e.getKey(), (int) Math.round(e.getValue() * mult));
		}
		return resultado;
	}

	public Map<Atributo, Integer> getModificadoresDeAtributoOriginais() {
		return modificadoresDeAtributo;
	}

	public Map<String, Double> getModificadoresStatus() {
		if (modificadoresStatus == null || getGrauOverclock() == 0) return modificadoresStatus;
		Map<String, Double> resultado = new HashMap<>();
		double mult = getMultiplicadorOverclock();
		for (Map.Entry<String, Double> e : modificadoresStatus.entrySet()) {
			resultado.put(e.getKey(), e.getValue() * mult);
		}
		return resultado;
	}

	public Map<String, Double> getModificadoresStatusOriginais() {
		return modificadoresStatus;
	}

	public void setModificadoresStatus(Map<String, Double> modificadoresStatus) {
		this.modificadoresStatus = modificadoresStatus;
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
		if (hasSpecialAttack) return true;
		return isRequerMunicao();
	}

	public String getNomeAtaqueAlternativoBasico() {
		if (hasSpecialAttack && specialAttackName != null && !specialAttackName.isEmpty()) {
			return specialAttackName;
		}
		return "Coronhada";
	}

	public String getDescricaoAtaqueAlternativoBasico() {
		if (hasSpecialAttack) {
			String desc = String.format("%.2fx Dano, %.1fx TU", specialAttackDmg, specialAttackCd);
			if (!"INDIVIDUAL".equalsIgnoreCase(specialAttackType)) {
				desc += ", " + specialAttackType;
				if (specialAttackSize > 0) desc += " " + specialAttackSize;
			}
			return desc;
		}
		return "0.5x Dano, alcance 1";
	}

	public double getMultiplicadorAtaqueAlternativoBasico() {
		if (hasSpecialAttack) return specialAttackDmg;
		return 0.50;
	}

	public int getAlcanceAtaqueAlternativoBasico() {
		return 1;
	}

	public TipoAlvo getTipoAlvoAtaqueAlternativoBasico() {
		if (hasSpecialAttack && specialAttackType != null) {
			try {
				return TipoAlvo.valueOf(specialAttackType.toUpperCase());
			} catch (IllegalArgumentException e) {
				// Tipo customizado (ex: "90º"), trata como AREA_QUADRADA
				return TipoAlvo.AREA_QUADRADA;
			}
		}
		return TipoAlvo.INDIVIDUAL;
	}

	public int getAnguloAtaqueAlternativoBasico() {
		if (hasSpecialAttack && "CONE".equalsIgnoreCase(specialAttackType)) {
			return specialAttackSize > 0 ? specialAttackSize : getAnguloCone();
		}
		return getAnguloCone();
	}

	public int getTamanhoAreaAtaqueAlternativoBasico() {
		if (hasSpecialAttack) return specialAttackSize;
		return 0;
	}

	public double getCustoTUMultiplierAtaqueAlternativo() {
		if (hasSpecialAttack) return specialAttackCd;
		return 1.0;
	}

	/**
	 * Retorna a quantidade de mana ganha ao acertar com o ataque alternativo.
	 * -1 = usa a regra normal (Ranged=1, Melee=2).
	 */
	public double getManaGainAtaqueAlternativo() {
		return -1;
	}

	// --- Getters / Setters do Ataque Especial ---

	public boolean isHasSpecialAttack() {
		return hasSpecialAttack;
	}

	public void setHasSpecialAttack(boolean hasSpecialAttack) {
		this.hasSpecialAttack = hasSpecialAttack;
	}

	public String getSpecialAttackName() {
		return specialAttackName;
	}

	public void setSpecialAttackName(String specialAttackName) {
		this.specialAttackName = specialAttackName;
	}

	public double getSpecialAttackDmg() {
		return specialAttackDmg;
	}

	public void setSpecialAttackDmg(double specialAttackDmg) {
		this.specialAttackDmg = specialAttackDmg;
	}

	public double getSpecialAttackCd() {
		return specialAttackCd;
	}

	public void setSpecialAttackCd(double specialAttackCd) {
		this.specialAttackCd = specialAttackCd;
	}

	public String getSpecialAttackType() {
		return specialAttackType;
	}

	public void setSpecialAttackType(String specialAttackType) {
		this.specialAttackType = specialAttackType;
	}

	public int getSpecialAttackSize() {
		return specialAttackSize;
	}

	public void setSpecialAttackSize(int specialAttackSize) {
		this.specialAttackSize = specialAttackSize;
	}

}
