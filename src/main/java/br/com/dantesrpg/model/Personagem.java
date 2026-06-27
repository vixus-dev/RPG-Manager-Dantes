package br.com.dantesrpg.model;

import br.com.dantesrpg.model.classes.Alquimista;
import br.com.dantesrpg.model.classes.Feiticeiro;
import br.com.dantesrpg.model.classes.Invocador;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.PesoEntidade;
import br.com.dantesrpg.model.personagem.PersonagemEffects;
import br.com.dantesrpg.model.personagem.PersonagemHealth;
import br.com.dantesrpg.model.racas.HalfAngel;
import br.com.dantesrpg.model.racas.HalfDemon;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.racas.Marionette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;

public class Personagem {

	// === IDENTIFICAÇÃO ===
	private String nome;
	private Raça raca;
	private Classe classe;

	private int nivel;
	private int experiencia;
	private int placarIniciativa;
	private int iniciativaBase;
	private double vidaMaximaBase;
	private int pontosParaDistribuir;

	private int xpReward = 0;
	private int xpAtual = 0;
	private int grau = 0;

	// === ATRIBUTOS S.P.E.C.I.A.L.I.S.T. ===
	private Map<Atributo, Integer> atributosBase;
	private Map<Atributo, Integer> atributosFinais;

	// === STATUS DE COMBATE PRINCIPAIS ===
	private double vidaAtual;
	private double vidaMaxima;
	private double manaAtual;
	private double manaMaxima;
	private int contadorTU;
	private double escudoNormalAtual;
	private double escudoNormalMaximo;
	private double escudoSangueAtual;
	private double escudoSangueMaximo;
	private double escudoDivinoAtual;
	private double escudoDivinoMaximo;
	private double escudoInfernalAtual = 0.0;
	private int armaduraTotal;
	private double reducaoDanoArmadura;

	// === ESTATÍSTICAS DERIVADAS ===
	private int movimento;
	private double taxaCritica;
	private double danoCritico;
	private double reducaoDanoTopor;
	private double reducaoDoTTopor;
	private double bonusDanoPercentual;
	private double poolRegeneracao = 0.0;
	private double reducaoCuraPercentual = 0.0;
	private double manaMaximaBase = 6.0;

	// === EQUIPAMENTO ===
	private Arma armaEquipada;
	private List<Arma> armasEquipadas = new ArrayList<>();
	private int wieldingMaximo = 2;
	private Armadura armaduraEquipada;
	private Amuleto amuleto1;
	private Amuleto amuleto2;

	// === HABILIDADES E MAGIAS ===
	private List<Habilidade> habilidadesDeClasse;
	private List<Habilidade> habilidadesExtras = new ArrayList<>();
	private Queue<DanoSofrido> historicoDano = new LinkedList<>();
	private FantasmaNobre fantasmaNobre;

	// === INVENTÁRIO E RECURSOS ===
	private Inventario inventario;

	// === GERENCIAMENTO DE EFEITOS ===
	private Map<String, Efeito> efeitosAtivos;

	// === COORDENADAS PARA O MAPA ===
	private int posX = 0;
	private int posY = 0;
	private int movimentoRestanteTurno;
	private boolean fugiu = false;
	private String faccao;
	private String jsonFileName;
	private int segmentosVida = 0;
	private int tamanhoX = 1;
	private int tamanhoY = 1;

	// === CLONES/INVOCAÇÕES ===
	private Personagem mestreInvocador;
	private List<Personagem> clonesAtivos = new ArrayList<>();
	private boolean isClone = false;
	private Personagem criador = null;
	private Habilidade ultimaHabilidadeUsada = null;

	// === FLAGS DE COMBATE ===
	private boolean isProtagonista = false;
	private boolean isAusente = false;
	private boolean poderoso = false;
	private List<String> propriedades = new ArrayList<>();

	// === EMPUXO (KNOCKBACK) ===
	/** Peso da entidade, usado para resistir ao empuxo. Padrão: MEDIO_PADRAO. */
	private PesoEntidade pesoEntidade = PesoEntidade.MEDIO_PADRAO;

	// === CONTRATOS DE VIDA ===
	/** Lista FIFO de contratos de vida de qualquer fonte (racial, skill, boss). */
	private List<br.com.dantesrpg.model.util.ContratoDeVida> contratosDeVida = new ArrayList<>();

	// === SUBSISTEMAS (transient — não serializados pelo Gson) ===
	private transient PersonagemEffects effectsManager;
	private transient PersonagemHealth healthManager;

	public List<br.com.dantesrpg.model.util.ContratoDeVida> getContratosDeVida() {
		if (contratosDeVida == null) {
			contratosDeVida = new ArrayList<>();
		}
		return contratosDeVida;
	}

	// ========== CONSTRUTORES ==========

	public Personagem() {
		this.nome = "Dummy";
		this.atributosBase = new EnumMap<>(Atributo.class);
		for (br.com.dantesrpg.model.enums.Atributo a : br.com.dantesrpg.model.enums.Atributo.values()) {
			this.atributosBase.put(a, 10);
		}
		this.efeitosAtivos = new HashMap<>();
		this.habilidadesDeClasse = new ArrayList<>();
		this.inventario = new Inventario();
		this.escudoNormalAtual = 0.0;
		this.escudoNormalMaximo = 0.0;
		this.escudoSangueAtual = 0.0;
		this.escudoSangueMaximo = 0.0;
		this.escudoDivinoAtual = 0.0;
		this.escudoDivinoMaximo = 0.0;
		this.vidaMaximaBase = 100.0;
		this.iniciativaBase = 10;
		this.vidaMaxima = 100.0;
		this.vidaAtual = 100.0;
		this.manaMaximaBase = 10;
		this.manaMaxima = 10;
		this.manaAtual = 10;
	}

	public Personagem(String nome, Raça raca, Classe classe, int nivel, Map<Atributo, Integer> atributosBase,
			double vidaMaximaBase, int iniciativaBase) {
		this.nome = nome;
		this.raca = raca;
		this.classe = classe;
		this.nivel = nivel;
		this.experiencia = 0;
		this.atributosBase = atributosBase;
		this.efeitosAtivos = new HashMap<>();
		this.habilidadesDeClasse = new ArrayList<>();
		this.inventario = new Inventario();
		this.fantasmaNobre = null;
		this.escudoNormalAtual = 0.0;
		this.escudoNormalMaximo = 0.0;
		this.escudoSangueAtual = 0.0;
		this.escudoSangueMaximo = 0.0;
		this.escudoDivinoAtual = 0.0;
		this.escudoDivinoMaximo = 0.0;
		this.vidaMaximaBase = vidaMaximaBase;
		this.iniciativaBase = iniciativaBase;

		if (this.classe != null && this.classe.getHabilidades(this) != null) {
			this.habilidadesDeClasse.addAll(this.classe.getHabilidades(this));
		}

		if (this.raca != null && this.raca.getRacialAbilities(this) != null) {
			this.habilidadesDeClasse.addAll(this.raca.getRacialAbilities(this));
		}

		recalcularAtributosEstatisticas();
		this.vidaMaxima = (double) vidaMaximaBase;
		this.vidaAtual = this.vidaMaxima;
		this.manaMaxima = 6 + (this.atributosFinais.getOrDefault(Atributo.INSPIRACAO, 1) / 2);
		this.manaAtual = this.manaMaxima;
		this.contadorTU = 0;
	}

	// ========== GETTERS DOS SUBSISTEMAS (lazy init para compatibilidade Gson) ==========

	public PersonagemEffects getEffectsManager() {
		if (effectsManager == null) {
			effectsManager = new PersonagemEffects(this);
		}
		return effectsManager;
	}

	public PersonagemHealth getHealthManager() {
		if (healthManager == null) {
			healthManager = new PersonagemHealth(this);
		}
		return healthManager;
	}

	// ========== MÉTODOS DE ACESSO INTERNO (usados pelos subsistemas) ==========

	/** Retorna o mapa mutável de efeitos — uso interno dos subsistemas. */
	public Map<String, Efeito> getEfeitosAtivosMutavel() {
		if (efeitosAtivos == null) {
			efeitosAtivos = new HashMap<>();
		}
		return efeitosAtivos;
	}

	/** Seta vidaAtual diretamente sem hooks — uso interno do PersonagemHealth. */
	public void setVidaAtualInterno(double valor) {
		this.vidaAtual = valor;
	}

	/** Getter para fugiu — uso do PersonagemHealth. */
	public boolean isFugiu() {
		return fugiu;
	}

	/** Retorna a fila de histórico de dano — uso do PersonagemHealth. */
	public Queue<DanoSofrido> getHistoricoDano() {
		if (historicoDano == null) {
			historicoDano = new LinkedList<>();
		}
		return historicoDano;
	}

	// ========== RECÁLCULO DE ATRIBUTOS (decomposto) ==========

	public void recalcularAtributosEstatisticas() {
		resetarStats();
		aplicarModificadoresDeAtributo();
		calcularEstatisticasDerivadas();
		recalcularManaBase();
		aplicarModificadoresDeEquipamento();
		aplicarModificadoresDeEfeitos();
		aplicarBonusEspecificos();
		clampValoresFinais();

		int des = this.atributosFinais.getOrDefault(Atributo.DESTREZA, 1);
		this.placarIniciativa = this.iniciativaBase + des;
	}

	private void resetarStats() {
		this.bonusDanoPercentual = 0.0;
		this.armaduraTotal = 0;
		this.movimento = 0;
		this.reducaoDanoTopor = 0.0;
		this.reducaoDoTTopor = 0.0;
		this.reducaoCuraPercentual = 0.0;
		this.vidaMaxima = (double) this.vidaMaximaBase;
		this.atributosFinais = new EnumMap<>(Atributo.class);
		if (this.atributosBase != null) this.atributosFinais.putAll(this.atributosBase);
	}

	private void aplicarModificadoresDeAtributo() {
		// Classe
		if (this.classe != null && this.classe.getModificadoresDeAtributo() != null) {
			this.classe.getModificadoresDeAtributo().forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
		}

		// Raça (Permanente)
		if (this.raca != null) {
			Map<Atributo, Integer> modificadoresRacaPerm = this.raca.getAttributeModifiers(this);
			if (modificadoresRacaPerm != null) {
				modificadoresRacaPerm.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
			}
			this.bonusDanoPercentual += this.raca.getBonusDanoPercentual(this);
		}

		// Efeitos Ativos (Buffs/Debuffs de Atributo)
		if (this.efeitosAtivos != null) {
			for (Efeito efeito : this.efeitosAtivos.values()) {
				if (efeito != null && efeito.getModificadores() != null) {
					for (Map.Entry<String, Double> modEntry : efeito.getModificadores().entrySet()) {
						try {
							Atributo atr = Atributo.valueOf(modEntry.getKey().toUpperCase());
							int valorMod = modEntry.getValue().intValue();
							this.atributosFinais.merge(atr, valorMod, Integer::sum);
						} catch (IllegalArgumentException e) {
							// Não é atributo — será tratado em aplicarModificadoresStatus
						}
					}
				}
			}
		}

		// Raça (Temporário)
		if (this.raca != null) {
			Map<Atributo, Integer> modificadoresRacaTemp = this.raca.getTemporaryAttributeModifiers(this);
			if (modificadoresRacaTemp != null) {
				modificadoresRacaTemp.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
			}
		}

		// Equipamentos (Atributos: FOR, DES, etc.)
		for (Arma arma : getArmasEquipadas()) {
			if (arma.getModificadoresDeAtributo() != null) {
				arma.getModificadoresDeAtributo()
						.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
			}
		}
		if (this.armaduraEquipada != null && this.armaduraEquipada.getModificadoresDeAtributo() != null) {
			this.armaduraEquipada.getModificadoresDeAtributo()
					.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
		}
		if (this.amuleto1 != null && this.amuleto1.getModificadoresDeAtributo() != null) {
			this.amuleto1.getModificadoresDeAtributo()
					.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
		}
		if (this.amuleto2 != null && this.amuleto2.getModificadoresDeAtributo() != null) {
			this.amuleto2.getModificadoresDeAtributo()
					.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
		}

		// CLAMP (Mínimo 1)
		this.atributosFinais.replaceAll((atr, valor) -> Math.max(1, valor));
	}

	private void calcularEstatisticasDerivadas() {
		int sag = this.atributosFinais.getOrDefault(Atributo.SAGACIDADE, 1);
		int des = this.atributosFinais.getOrDefault(Atributo.DESTREZA, 1);
		int top = this.atributosFinais.getOrDefault(Atributo.TOPOR, 1);

		this.taxaCritica = 0.05 + (sag * 0.01);
		this.danoCritico = 0.50 + (sag * 0.025);
		this.movimento = 3 + (des / 2);
		this.reducaoDanoTopor = top * 0.01;
		this.reducaoDoTTopor = top * 0.025;
	}

	private void recalcularManaBase() {
		this.manaMaxima = this.manaMaximaBase + (this.atributosFinais.getOrDefault(Atributo.INSPIRACAO, 1) / 2.0);
	}

	private void aplicarModificadoresDeEquipamento() {
		for (Arma arma : getArmasEquipadas()) {
			aplicarModificadoresStatus(arma.getModificadoresStatus());
		}
		if (this.armaduraEquipada != null) {
			aplicarModificadoresStatus(this.armaduraEquipada.getModificadoresStatus());
			this.armaduraTotal += this.armaduraEquipada.getArmaduraBase();
		}
		if (this.amuleto1 != null) {
			aplicarModificadoresStatus(this.amuleto1.getModificadoresStatus());
			this.armaduraTotal += this.amuleto1.getArmaduraBonus();
		}
		if (this.amuleto2 != null) {
			aplicarModificadoresStatus(this.amuleto2.getModificadoresStatus());
			this.armaduraTotal += this.amuleto2.getArmaduraBonus();
		}
	}

	private void aplicarModificadoresDeEfeitos() {
		if (this.efeitosAtivos != null) {
			for (Efeito efeito : this.efeitosAtivos.values()) {
				if (efeito != null && efeito.getModificadores() != null) {
					aplicarModificadoresStatus(efeito.getModificadores());
				}
			}
		}
	}

	private void aplicarBonusEspecificos() {
		// Bônus de Classe
		if (this.classe instanceof Feiticeiro)
			this.taxaCritica += 0.25;
		if (this.classe instanceof Invocador)
			this.manaMaxima -= 2;
		if (this.classe instanceof Alquimista)
			this.manaMaxima += 1;

		// Bônus de Raça
		if (this.raca instanceof Marionette || (this.raca != null && "Marionette".equalsIgnoreCase(this.raca.getNome())))
			this.taxaCritica += 0.25;
		if (this.raca instanceof HalfAngel || (this.raca != null && "Half-Angel".equalsIgnoreCase(this.raca.getNome())))
			this.taxaCritica += 0.25;
		if (this.raca instanceof HalfDemon || (this.raca != null && "Half-Demon".equalsIgnoreCase(this.raca.getNome())))
			this.taxaCritica += 0.25;

		// Ilusionista: bônus por clone ativo
		if (this.classe instanceof br.com.dantesrpg.model.classes.Ilusionista) {
			int numClones = this.clonesAtivos.size();
			if (numClones > 0) {
				this.danoCritico += (numClones * 0.30);
			}
		}

		// Raça: reduções e bônus de armadura
		if (this.raca != null) {
			this.vidaMaxima -= this.raca.getReducaoHpMaximo(this);
			double bonusArmPct = this.raca.getBonusArmaduraPercentual(this);
			if (bonusArmPct > 0) {
				this.armaduraTotal += (int) (this.armaduraTotal * bonusArmPct);
			}
		}

		// Poção de Resistencia: aumenta a armadura em IS% do valor atual
		if (this.efeitosAtivos != null) {
			double bonusArmaduraPercentualEfeito = 0.0;
			for (Efeito e : this.efeitosAtivos.values()) {
				if (e.getModificadores() != null && e.getModificadores().containsKey("BONUS_ARMADURA_PERCENTUAL")) {
					bonusArmaduraPercentualEfeito += e.getModificadores().get("BONUS_ARMADURA_PERCENTUAL");
				}
			}
			if (bonusArmaduraPercentualEfeito != 0.0) {
				this.armaduraTotal += (int) (this.armaduraTotal * bonusArmaduraPercentualEfeito);
			}
		}

		// Contratos de Vida: reduzem teto de HP máximo
		this.vidaMaxima -= br.com.dantesrpg.model.util.ContratoDeVidaUtils.getReducaoHpMaximoTotal(this);

		// Escudo Infernal: Reduz HP máximo por ponto, até no mínimo 1.
		if (this.escudoInfernalAtual > 0) {
			this.vidaMaxima -= this.escudoInfernalAtual;
		}
	}

	private void clampValoresFinais() {
		this.taxaCritica = Math.max(0, Math.min(this.taxaCritica, 1.0));
		this.vidaMaxima = Math.max(1.0, this.vidaMaxima);
		this.vidaAtual = Math.min(this.vidaAtual, this.vidaMaxima);
		this.manaAtual = Math.min(this.manaAtual, this.manaMaxima);
		this.armaduraTotal = Math.max(0, this.armaduraTotal);

		this.reducaoDanoArmadura = (double) this.armaduraTotal / (100.0 + this.armaduraTotal);

		// Converte excesso de redução de dano (acima de 90%) em bônus de dano
		double reducaoTotal = this.reducaoDanoArmadura + this.reducaoDanoTopor;
		if (reducaoTotal > 0.90) {
			double excesso = reducaoTotal - 0.90;
			this.bonusDanoPercentual += excesso;
			System.out.println(">>> EXCESSO DE DEFESA: " + String.format("%.1f%%", excesso * 100)
					+ " convertido em bônus de dano para " + this.nome);
		}

		if (this.movimentoRestanteTurno > this.movimento) {
			this.movimentoRestanteTurno = this.movimento;
		}
	}

	private void aplicarModificadoresStatus(Map<String, Double> mods) {
		if (mods == null)
			return;

		for (Map.Entry<String, Double> entry : mods.entrySet()) {
			String chave = entry.getKey().toUpperCase();
			double valor = entry.getValue();

			switch (chave) {
			case "HP_MAXIMO":
				this.vidaMaxima += valor;
				break;
			case "MP_MAXIMO":
				this.manaMaxima += valor;
				break;
			case "DANO_BONUS_PERCENTUAL":
				this.bonusDanoPercentual += valor;
				break;
			case "TAXA_CRITICA":
				this.taxaCritica += valor;
				break;
			case "DANO_CRITICO":
				this.danoCritico += valor;
				break;
			case "MOVIMENTO":
				this.movimento += (int) valor;
				break;
			case "REDUCAO_DANO_MODIFICADOR":
				this.reducaoDanoTopor += valor;
				break;
			case "ARMADURA_TOTAL":
				this.armaduraTotal += (int) valor;
				break;
			case "RESISTENCIA_DOT":
				this.reducaoDoTTopor += valor;
				break;
			case "REDUCAO_CURA":
				this.reducaoCuraPercentual += valor;
				break;
			default:
				break;
			}
		}
	}

	// ========== DELEGAÇÃO: EFEITOS (wrappers de compatibilidade) ==========

	public void adicionarEfeito(Efeito efeito) {
		getEffectsManager().adicionarEfeito(efeito);
	}

	public Efeito removerEfeito(String nomeEfeito) {
		return getEffectsManager().removerEfeito(nomeEfeito);
	}

	public void reduzirDuracaoEfeitos(int tempoDecorrido) {
		getEffectsManager().reduzirDuracaoEfeitos(tempoDecorrido);
	}

	public Map<String, Efeito> getEfeitosAtivos() {
		return getEffectsManager().getEfeitosAtivos();
	}

	// ========== DELEGAÇÃO: SAÚDE (wrappers de compatibilidade) ==========

	public void setVidaAtual(double novaVida, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		getHealthManager().setVidaAtual(novaVida, estado, controller);
	}

	public void setVidaAtual(double novaVida) {
		setVidaAtual(novaVida, null, null);
	}

	public void curarIgnorandoBloqueios(double valor, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		getHealthManager().curarIgnorandoBloqueios(valor, estado, controller);
	}

	public void forcarCura(double valor) {
		getHealthManager().forcarCura(valor);
	}

	public void regenerarVidaFracionada(double quantidade, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		getHealthManager().regenerarVidaFracionada(quantidade, estado, controller);
	}

	public void registrarDanoSofrido(double valor, int tempoGlobalTU) {
		getHealthManager().registrarDanoSofrido(valor, tempoGlobalTU);
	}

	public double getDanoSofridoRecentemente(int duracaoTU, int tempoGlobalAtual) {
		return getHealthManager().getDanoSofridoRecentemente(duracaoTU, tempoGlobalAtual);
	}

	public boolean isAtivoNoCombate() {
		return getHealthManager().isAtivoNoCombate();
	}

	public boolean isVivo() {
		return getHealthManager().isVivo();
	}

	// ========== XP / LEVEL ==========

	public int getXpParaProximoNivel() {
		return 50 * (this.nivel * this.nivel);
	}

	public void ganharExperiencia(int quantidade) {
		this.xpAtual += quantidade;
		System.out.println(">>> " + this.nome + " ganhou " + quantidade + " XP. Total: " + this.xpAtual);

		while (this.xpAtual >= getXpParaProximoNivel()) {
			subirDeNivel();
		}
	}

	private void subirDeNivel() {
		this.xpAtual -= getXpParaProximoNivel();
		this.nivel++;
		this.pontosParaDistribuir += 2;

		this.vidaAtual = this.vidaMaxima;
		this.manaAtual = this.manaMaxima;

		System.out.println(">>> LEVEL UP! " + this.nome + " alcançou o nível " + this.nivel + "! (+2 Pontos)");
		recalcularAtributosEstatisticas();
	}

	public boolean aumentarAtributoBase(Atributo atr) {
		if (this.pontosParaDistribuir > 0) {
			this.pontosParaDistribuir--;
			int valorAtual = this.atributosBase.getOrDefault(atr, 1);
			this.atributosBase.put(atr, valorAtual + 1);
			recalcularAtributosEstatisticas();
			return true;
		}
		return false;
	}

	// ========== HABILIDADES ==========

	public List<Habilidade> getHabilidadesDeClasse() {
		List<Habilidade> combinadas = new ArrayList<>();
		if (habilidadesDeClasse != null)
			combinadas.addAll(habilidadesDeClasse);
		if (habilidadesExtras != null)
			combinadas.addAll(habilidadesExtras);

		for (Arma arma : getArmasEquipadas()) {
			if (arma instanceof br.com.dantesrpg.model.Grimorio) {
				br.com.dantesrpg.model.Grimorio grimorio = (br.com.dantesrpg.model.Grimorio) arma;
				combinadas.addAll(grimorio.getMagiasArmazenadas());
			} else {
				for (String nomeHab : arma.getHabilidadesConcedidasNomes()) {
					Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeHab);
					if (h != null) {
						combinadas.add(h);
					}
				}
			}
		}

		if (this.armaduraEquipada != null && !this.armaduraEquipada.getHabilidadesConcedidasNomes().isEmpty()) {
			for (String nomeHab : this.armaduraEquipada.getHabilidadesConcedidasNomes()) {
				Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeHab);
				if (h != null) combinadas.add(h);
			}
		}

		if (this.amuleto1 != null && !this.amuleto1.getHabilidadesConcedidasNomes().isEmpty()) {
			for (String nomeHab : this.amuleto1.getHabilidadesConcedidasNomes()) {
				Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeHab);
				if (h != null) combinadas.add(h);
			}
		}

		if (this.amuleto2 != null && !this.amuleto2.getHabilidadesConcedidasNomes().isEmpty()) {
			for (String nomeHab : this.amuleto2.getHabilidadesConcedidasNomes()) {
				Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeHab);
				if (h != null) combinadas.add(h);
			}
		}

		return combinadas;
	}

	public void adicionarHabilidadeExtra(Habilidade h) {
		this.habilidadesExtras.add(h);
	}

	public void limparHabilidadesExtras() {
		this.habilidadesExtras.clear();
	}

	public Habilidade getUltimaHabilidadeUsada() {
		return ultimaHabilidadeUsada;
	}

	public void setUltimaHabilidadeUsada(Habilidade h) {
		this.ultimaHabilidadeUsada = h;
	}

	// ========== CLONES / INVOCAÇÕES ==========

	public List<Personagem> getClonesAtivos() {
		return clonesAtivos;
	}

	public void registrarClone(Personagem clone) {
		if (clone == null || this.clonesAtivos.contains(clone))
			return;
		this.clonesAtivos.add(clone);
		clone.setCloneStatus(true, this);
	}

	public void removerCloneMorto(Personagem clone) {
		this.clonesAtivos.remove(clone);
	}

	public void setCloneStatus(boolean isClone, Personagem criador) {
		this.isClone = isClone;
		this.criador = criador;
	}

	public boolean isClone() {
		return isClone;
	}

	public Personagem getCriador() {
		return criador;
	}

	public Personagem getMestreInvocador() {
		return mestreInvocador;
	}

	public void setMestreInvocador(Personagem mestreInvocador) {
		this.mestreInvocador = mestreInvocador;
	}

	public void adicionarClone(Personagem clone) {
		this.clonesAtivos.add(clone);
		clone.setMestreInvocador(this);
	}

	public void removerClone(Personagem clone) {
		this.clonesAtivos.remove(clone);
	}

	public void limparClonesAtivos() {
		this.clonesAtivos.clear();
	}

	// ========== PROPRIEDADES / FLAGS ==========

	public List<String> getPropriedades() {
		if (propriedades == null) {
			propriedades = new ArrayList<>();
		}
		return propriedades;
	}

	public void setPropriedades(List<String> novasPropriedades) {
		this.propriedades = new ArrayList<>();
		if (novasPropriedades != null) {
			this.propriedades.addAll(novasPropriedades);
		}
	}

	public void adicionarPropriedade(String prop) {
		if (this.propriedades == null)
			this.propriedades = new ArrayList<>();
		if (!this.propriedades.contains(prop)) {
			this.propriedades.add(prop);
		}
	}

	public boolean isPoderoso() {
		return poderoso;
	}

	public void setPoderoso(boolean poderoso) {
		this.poderoso = poderoso;
	}

	// === EMPUXO (KNOCKBACK) ===

	public PesoEntidade getPesoEntidade() {
		if (pesoEntidade == null) pesoEntidade = PesoEntidade.MEDIO_PADRAO;
		return pesoEntidade;
	}

	public void setPesoEntidade(PesoEntidade pesoEntidade) {
		this.pesoEntidade = pesoEntidade;
	}

	public boolean temPropriedade(String prop) {
		return getPropriedades().contains(prop);
	}

	public int getValorPropriedade(String chave) {
		for (String prop : getPropriedades()) {
			if (prop.startsWith(chave + ":")) {
				try {
					return Integer.parseInt(prop.split(":")[1]);
				} catch (Exception e) {
					return 0;
				}
			}
			if (prop.equals(chave))
				return 1;
		}
		return 0;
	}

	// ========== POSIÇÃO / MOVIMENTO ==========

	public int getPosX() {
		return posX;
	}

	public void setPosX(int posX) {
		this.posX = posX;
	}

	public int getPosY() {
		return posY;
	}

	public void setPosY(int posY) {
		this.posY = posY;
	}

	public int getMovimentoRestanteTurno() {
		return movimentoRestanteTurno;
	}

	public void setMovimentoRestanteTurno(int movimento) {
		this.movimentoRestanteTurno = Math.max(0, movimento);
	}

	public int getTamanhoX() {
		return tamanhoX;
	}

	public void setTamanhoX(int x) {
		this.tamanhoX = x;
	}

	public int getTamanhoY() {
		return tamanhoY;
	}

	public void setTamanhoY(int y) {
		this.tamanhoY = y;
	}

	public boolean ocupa(int x, int y) {
		return x >= this.posX && x < (this.posX + tamanhoX) && y >= this.posY && y < (this.posY + tamanhoY);
	}

	// ========== GETTERS SIMPLES ==========

	public String getNome() {
		return nome;
	}

	public Raça getRaca() {
		return raca;
	}

	public void setRaca(Raça raca) {
		this.raca = raca;
	}

	public Classe getClasse() {
		return classe;
	}

	public int getNivel() {
		return nivel;
	}

	public int getExperiencia() {
		return experiencia;
	}

	public int getPlacarIniciativa() {
		return placarIniciativa;
	}

	public Map<Atributo, Integer> getAtributosFinais() {
		return atributosFinais != null ? new HashMap<>(atributosFinais) : new HashMap<>();
	}

	public Map<Atributo, Integer> getAtributosBase() {
		return atributosBase;
	}

	public int getContadorTU() {
		return contadorTU;
	}

	public int getMovimento() {
		return movimento;
	}

	public double getVidaAtual() {
		return vidaAtual;
	}

	public double getVidaMaxima() {
		return vidaMaxima;
	}

	public double getManaAtual() {
		return manaAtual;
	}

	public double getManaMaxima() {
		return manaMaxima;
	}

	/**
	 * Soma dos dois tipos de escudo (sangue + normal).
	 * Usado para exibição agregada e compatibilidade com chamadas legadas.
	 */
	public double getEscudoAtual() {
		return escudoNormalAtual + escudoSangueAtual + escudoDivinoAtual + escudoInfernalAtual;
	}

	public double getEscudoNormalAtual() {
		return escudoNormalAtual;
	}

	public double getEscudoNormalMaximo() {
		return escudoNormalMaximo;
	}

	public double getEscudoSangueAtual() {
		return escudoSangueAtual;
	}

	public double getEscudoSangueMaximo() {
		return escudoSangueMaximo;
	}

	public double getEscudoDivinoAtual() {
		return escudoDivinoAtual;
	}

	public double getEscudoDivinoMaximo() {
		return escudoDivinoMaximo;
	}

	public double getTaxaCritica() {
		return taxaCritica;
	}

	public double getDanoCritico() {
		return danoCritico;
	}

	public double getReducaoDanoTopor() {
		return reducaoDanoTopor;
	}

	public double getReducaoDoTTopor() {
		return reducaoDoTTopor;
	}

	public int getArmaduraTotal() {
		return armaduraTotal;
	}

	public double getReducaoDanoArmadura() {
		return reducaoDanoArmadura;
	}

	public double getBonusDanoPercentual() {
		return bonusDanoPercentual;
	}

	public double getReducaoCuraPercentual() {
		return reducaoCuraPercentual;
	}

	public double getSortePercentual() {
		int sorte = this.atributosFinais.getOrDefault(Atributo.SORTE, 1);
		return sorte * 0.01;
	}

	public double getVidaMaximaBase() {
		return vidaMaximaBase;
	}

	public Arma getArmaEquipada() {
		List<Arma> armas = getArmasEquipadas();
		return armas.isEmpty() ? null : armas.get(0);
	}

	public List<Arma> getArmasEquipadas() {
		if (armasEquipadas == null) {
			armasEquipadas = new ArrayList<>();
		}
		armasEquipadas.removeIf(arma -> arma == null);
		if (armasEquipadas.isEmpty() && armaEquipada != null) {
			armasEquipadas.add(armaEquipada);
		}
		armaEquipada = armasEquipadas.isEmpty() ? null : armasEquipadas.get(0);
		return armasEquipadas;
	}

	public int getWieldingMaximo() {
		return wieldingMaximo > 0 ? wieldingMaximo : 2;
	}

	public int getWieldingOcupado() {
		return getArmasEquipadas().stream()
				.mapToInt(Arma::getWielding)
				.sum();
	}

	public int getWieldingDisponivel() {
		return Math.max(0, getWieldingMaximo() - getWieldingOcupado());
	}

	public boolean podeEquiparArma(Arma arma) {
		if (arma == null || arma.getWielding() > getWieldingDisponivel()) {
			return false;
		}
		return getArmasEquipadas().stream()
				.noneMatch(equipada -> equipada == arma);
	}

	public boolean equiparArma(Arma arma) {
		if (!podeEquiparArma(arma)) {
			return false;
		}
		getArmasEquipadas().add(arma);
		sincronizarArmaPrincipal();
		recalcularAtributosEstatisticas();
		return true;
	}

	public boolean desequiparArma(Arma arma) {
		if (arma == null) {
			return false;
		}
		boolean removeu = getArmasEquipadas().remove(arma);
		sincronizarArmaPrincipal();
		recalcularAtributosEstatisticas();
		return removeu;
	}

	public boolean possuiArmaEquipada(String nomeArma) {
		if (nomeArma == null) {
			return false;
		}
		return getArmasEquipadas().stream()
				.anyMatch(arma -> nomeArma.equals(arma.getNome()));
	}

	public Armadura getArmaduraEquipada() {
		return armaduraEquipada;
	}

	public Amuleto getAmuleto1() {
		return amuleto1;
	}

	public Amuleto getAmuleto2() {
		return amuleto2;
	}

	public FantasmaNobre getFantasmaNobre() {
		return fantasmaNobre;
	}

	public Inventario getInventario() {
		return inventario;
	}

	public String getFaccao() {
		return faccao;
	}

	public boolean isProtagonista() {
		return isProtagonista;
	}

	public boolean isAusente() {
		return isAusente;
	}

	public boolean isEscudoDeSangue() {
		return escudoSangueAtual > 0;
	}

	public String getJsonFileName() {
		return jsonFileName;
	}

	public int getXpReward() {
		return xpReward;
	}

	public int getXpAtual() {
		return xpAtual;
	}

	public int getPontosParaDistribuir() {
		return pontosParaDistribuir;
	}

	public int getGrau() {
		return grau;
	}

	public int getSegmentosVida() {
		return segmentosVida;
	}

	// ========== SETTERS SIMPLES ==========

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void setNivel(int nivel) {
		this.nivel = nivel;
	}

	public void setExperiencia(int experiencia) {
		this.experiencia = experiencia;
	}

	public void setContadorTU(int contadorTU) {
		this.contadorTU = contadorTU;
	}

	public void setVidaMaxima(double vidaMaxima) {
		this.vidaMaxima = vidaMaxima;
	}

	public void setVidaMaximaBase(double vidaBase) {
		this.vidaMaximaBase = vidaBase;
	}

	public void setManaAtual(double manaAtual) {
		this.manaAtual = Math.max(0.0, Math.min(manaAtual, this.manaMaxima));
	}

	public void setManaAtual(int manaAtual) {
		this.manaAtual = Math.max(0, Math.min(manaAtual, this.manaMaxima));
	}

	public void setManaMaxima(double manaMaxima) {
		this.manaMaximaBase = manaMaxima;
		this.manaMaxima = this.manaMaximaBase + (this.atributosFinais.getOrDefault(Atributo.INSPIRACAO, 1) / 2.0);
	}

	/**
	 * LEGADO — rota para o escudo NORMAL. Mantido para compatibilidade com
	 * GM tool (editor de combate) e carregamento de saves antigos.
	 * Código novo deve usar {@link #setEscudoNormalAtual}, {@link #setEscudoSangueAtual},
	 * {@link #adicionarEscudoNormal} ou {@link #adicionarEscudoSangue}.
	 */
	public void setEscudoAtual(double escudoAtual) {
		setEscudoNormalAtual(escudoAtual);
		if (this.escudoNormalAtual > this.escudoNormalMaximo) {
			this.escudoNormalMaximo = this.escudoNormalAtual;
		}
	}

	public void setEscudoNormalAtual(double v) {
		this.escudoNormalAtual = Math.max(0.0, v);
		if (this.escudoNormalAtual == 0.0) {
			this.escudoNormalMaximo = 0.0;
		}
	}

	public void setEscudoSangueAtual(double v) {
		this.escudoSangueAtual = Math.max(0.0, v);
		if (this.escudoSangueAtual == 0.0) {
			this.escudoSangueMaximo = 0.0;
		}
		recalcularSeBonusOvertimeDependeDoEscudo();
	}

	/**
	 * Adiciona escudo normal, elevando o "cap flexível" (máximo) se ultrapassado.
	 * O cap representa o pico já atingido e é usado apenas para renderização da barra.
	 */
	public void adicionarEscudoNormal(double v) {
		if (v <= 0) return;
		this.escudoNormalAtual += v;
		if (this.escudoNormalAtual > this.escudoNormalMaximo) {
			this.escudoNormalMaximo = this.escudoNormalAtual;
		}
	}

	/**
	 * Adiciona escudo de sangue (esponja de dano, ignora reduções).
	 * Eleva o "cap flexível" próprio, independente do escudo normal.
	 */
	public void adicionarEscudoSangue(double v) {
		if (v <= 0) return;
		this.escudoSangueAtual += v;
		if (this.escudoSangueAtual > this.escudoSangueMaximo) {
			this.escudoSangueMaximo = this.escudoSangueAtual;
		}
		recalcularSeBonusOvertimeDependeDoEscudo();
	}

	public void setEscudoNormalMaximo(double escudoMax) {
		this.escudoNormalMaximo = escudoMax;
		recalcularAtributosEstatisticas();
	}

	public void setEscudoSangueMaximo(double v) {
		this.escudoSangueMaximo = Math.max(0.0, v);
	}

	public double getEscudoInfernalAtual() {
		return escudoInfernalAtual;
	}

	public void setEscudoInfernalAtual(double escudoInfernalAtual) {
		this.escudoInfernalAtual = Math.max(0, escudoInfernalAtual);
		recalcularAtributosEstatisticas();
	}

	public void setEscudoDivinoAtual(double v) {
		this.escudoDivinoAtual = Math.max(0.0, v);
		if (this.escudoDivinoAtual == 0.0) {
			this.escudoDivinoMaximo = 0.0;
		}
	}

	public void setEscudoDivinoMaximo(double v) {
		this.escudoDivinoMaximo = Math.max(0.0, v);
	}

	public void adicionarEscudoDivino(double v) {
		if (v <= 0) return;
		this.escudoDivinoAtual += v;
		if (this.escudoDivinoAtual > this.escudoDivinoMaximo) {
			this.escudoDivinoMaximo = this.escudoDivinoAtual;
		}
	}

	private void recalcularSeBonusOvertimeDependeDoEscudo() {
		if (this.raca instanceof Humano && this.raca.isV2()) {
			recalcularAtributosEstatisticas();
		}
	}

	public void setArmaEquipada(Arma armaEquipada) {
		this.armaEquipada = armaEquipada;
		if (this.armasEquipadas == null) {
			this.armasEquipadas = new ArrayList<>();
		} else {
			this.armasEquipadas.clear();
		}
		if (armaEquipada != null) {
			this.armasEquipadas.add(armaEquipada);
		}
		recalcularAtributosEstatisticas();
	}

	public void setArmasEquipadas(List<Arma> armasEquipadas) {
		this.armasEquipadas = new ArrayList<>();
		if (armasEquipadas != null) {
			for (Arma arma : armasEquipadas) {
				if (arma != null && arma.getWielding() <= getWieldingMaximo() - getWieldingOcupado()
						&& this.armasEquipadas.stream().noneMatch(equipada -> equipada == arma)) {
					this.armasEquipadas.add(arma);
				}
			}
		}
		sincronizarArmaPrincipal();
		recalcularAtributosEstatisticas();
	}

	public void setWieldingMaximo(int wieldingMaximo) {
		this.wieldingMaximo = Math.max(1, wieldingMaximo);
		boolean removeu = false;
		while (getWieldingOcupado() > getWieldingMaximo() && !getArmasEquipadas().isEmpty()) {
			getArmasEquipadas().remove(getArmasEquipadas().size() - 1);
			removeu = true;
		}
		sincronizarArmaPrincipal();
		if (removeu) {
			recalcularAtributosEstatisticas();
		}
	}

	private void sincronizarArmaPrincipal() {
		if (this.armasEquipadas == null || this.armasEquipadas.isEmpty()) {
			this.armaEquipada = null;
		} else {
			this.armaEquipada = this.armasEquipadas.get(0);
		}
	}

	public void setArmaduraEquipada(Armadura armadura) {
		this.armaduraEquipada = armadura;
	}

	public void setAmuleto1(Amuleto amuleto) {
		this.amuleto1 = amuleto;
	}

	public void setAmuleto2(Amuleto amuleto) {
		this.amuleto2 = amuleto;
	}

	public void setFantasmaNobre(FantasmaNobre fantasmaNobre) {
		this.fantasmaNobre = fantasmaNobre;
	}

	public void setFaccao(String faccao) {
		this.faccao = faccao;
	}

	public void setProtagonista(boolean protagonista) {
		this.isProtagonista = protagonista;
	}

	public void setAusente(boolean ausente) {
		this.isAusente = ausente;
	}

	public void setFugiu(boolean fugiu) {
		this.fugiu = fugiu;
	}

	public void setBonusDanoPercentual(double bonusdeDano) {
		this.bonusDanoPercentual = bonusdeDano;
	}

	public void setPontosParaDistribuir(int pontos) {
		this.pontosParaDistribuir = pontos;
	}

	/**
	 * LEGADO — converte o escudo normal atual em escudo de sangue (se b=true),
	 * ou o escudo de sangue atual em normal (se b=false). Mantido apenas para o
	 * GM tool (checkbox do editor). Código novo deve adicionar direto ao tipo correto.
	 */
	public void setTemEscudoDeSangue(boolean b) {
		if (b) {
			if (this.escudoNormalAtual > 0) {
				this.escudoSangueAtual += this.escudoNormalAtual;
				if (this.escudoSangueAtual > this.escudoSangueMaximo) {
					this.escudoSangueMaximo = this.escudoSangueAtual;
				}
				this.escudoNormalAtual = 0;
				this.escudoNormalMaximo = 0;
			}
		} else {
			if (this.escudoSangueAtual > 0) {
				this.escudoNormalAtual += this.escudoSangueAtual;
				if (this.escudoNormalAtual > this.escudoNormalMaximo) {
					this.escudoNormalMaximo = this.escudoNormalAtual;
				}
				this.escudoSangueAtual = 0;
				this.escudoSangueMaximo = 0;
			}
		}
	}

	public void setJsonFileName(String jsonFileName) {
		this.jsonFileName = jsonFileName;
	}

	public void setXpReward(int xp) {
		this.xpReward = xp;
	}

	public void setXpAtual(int xp) {
		this.xpAtual = xp;
	}

	public void setGrau(int grau) {
		this.grau = grau;
	}

	public void setSegmentosVida(int segmentos) {
		this.segmentosVida = segmentos;
	}

	public void setIniciativaBase(int valor) {
		this.iniciativaBase = valor;
		recalcularAtributosEstatisticas();
	}

	public int getIniciativaBase() {
		return iniciativaBase;
	}

	public void setAtributoBase(Atributo atributo, int valor) {
		if (this.atributosBase == null) {
			this.atributosBase = new EnumMap<>(Atributo.class);
		}
		this.atributosBase.put(atributo, valor);
	}

	// ========== UTILIDADES ==========

	public double getMultiplicadorCustoTU() {
		double mult = 1.0;

		if (efeitosAtivos.containsKey("Lento")) {
			Efeito lento = efeitosAtivos.get("Lento");
			double pct = 0.30;
			if (lento != null && lento.getModificadores() != null && lento.getModificadores().containsKey("CUSTO_TU_PERCENTUAL")) {
				pct = lento.getModificadores().get("CUSTO_TU_PERCENTUAL");
			}
			mult += pct;
		}

		if (efeitosAtivos.containsKey("Muito Lento")) {
			mult += 0.50;
		}

		return mult;
	}

	// ========== CLASSE INTERNA ==========

	public static class DanoSofrido {
		public double valor;
		public int tempoGlobalTU;

		public DanoSofrido(double valor, int tempoGlobalTU) {
			this.valor = valor;
			this.tempoGlobalTU = tempoGlobalTU;
		}
	}
}
