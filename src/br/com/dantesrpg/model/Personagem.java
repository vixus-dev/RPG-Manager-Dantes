package br.com.dantesrpg.model;

import br.com.dantesrpg.model.classes.Feiticeiro;
import br.com.dantesrpg.model.classes.Invocador;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.racas.HalfAngel;
import br.com.dantesrpg.model.racas.HalfDemon;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.racas.Marionette;

import java.util.ArrayList;
import java.util.Collections;
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
	private double vidaMaximaBase;
	private int pontosParaDistribuir;

	private int xpReward = 0; // Quanto XP esse personagem dá ao morrer (para inimigos)
	private int xpAtual = 0; // xp acumulado pel
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
	private double escudoAtual;
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
	private Armadura armaduraEquipada;
	private Amuleto amuleto1;
	private Amuleto amuleto2;

	// === HABILIDADES E MAGIAS ===
	private List<Habilidade> habilidadesDeClasse;
	private List<Habilidade> habilidadesExtras = new ArrayList<>();
	private Queue<DanoSofrido> historicoDano = new LinkedList<>();
	private FantasmaNobre fantasmaNobre;
	// private List<Magia> magiasDeGrimorio;

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
	private boolean temEscudoDeSangue = false;
	private int segmentosVida = 0;
	private int tamanhoX = 1; // Largura
	private int tamanhoY = 1; // Altura

	// blah blah coisas extas das sombras
	private Personagem mestreInvocador; // Se este char for um clone, quem o criou?
	private List<Personagem> clonesAtivos = new ArrayList<>();
	private boolean isClone = false;
	private Personagem criador = null;
	private Habilidade ultimaHabilidadeUsada = null; // null = Ataque Básico

	// gerenciador de combate momento
	private boolean isProtagonista = false;
	private boolean isAusente = false;
	private List<String> propriedades = new ArrayList<>();

	// --- Construtor ---
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
		this.escudoAtual = 0.0;
		this.vidaMaximaBase = vidaMaximaBase;

		if (this.classe != null && this.classe.getHabilidades(this) != null) {
			this.habilidadesDeClasse.addAll(this.classe.getHabilidades(this));
		}

		if (this.raca != null && this.raca.getRacialAbilities(this) != null) {
			this.habilidadesDeClasse.addAll(this.raca.getRacialAbilities(this));
		}

		recalcularAtributosEstatisticas();
		int des = this.atributosFinais.getOrDefault(Atributo.DESTREZA, 1);
		this.placarIniciativa = iniciativaBase + des;
		this.vidaMaxima = (double) vidaMaximaBase;
		this.vidaAtual = this.vidaMaxima;
		this.manaMaxima = 6 + (this.atributosFinais.getOrDefault(Atributo.INSPIRACAO, 1) / 2);
		this.manaAtual = this.manaMaxima;
		this.contadorTU = 0;

	}

	public void registrarDanoSofrido(double valor, int tempoGlobalTU) {
		if (valor > 0) {
			this.historicoDano.add(new DanoSofrido(valor, tempoGlobalTU));
		}
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

	public void recalcularAtributosEstatisticas() {
		// RESET GERAL
		this.bonusDanoPercentual = 0.0;
		this.armaduraTotal = 0;
		this.movimento = 0;
		this.reducaoDanoTopor = 0.0;
		this.reducaoDoTTopor = 0.0;
		this.vidaMaxima = (double) this.vidaMaximaBase;

		// RESET ATRIBUTOS
		this.atributosFinais = new HashMap<>(this.atributosBase != null ? this.atributosBase : Collections.emptyMap());

		// SOMA TODOS OS MODIFICADORES DE ATRIBUTO

		// Classe
		if (this.classe != null && this.classe.getModificadoresDeAtributo() != null) {
			this.classe.getModificadoresDeAtributo()
					.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
		}

		// Raça (Permanente)
		if (this.raca != null) {
			Map<Atributo, Integer> modificadoresRacaPerm = this.raca.getAttributeModifiers(this);
			if (modificadoresRacaPerm != null) {
				modificadoresRacaPerm.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
			}
			// Bônus de dano da raça (ex: Humano Empréstimo)
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
							// Ignora se não for atributo (será tratado depois)
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
		if (this.armaEquipada != null && this.armaEquipada.getModificadoresDeAtributo() != null) {
			this.armaEquipada.getModificadoresDeAtributo()
					.forEach((atr, mod) -> this.atributosFinais.merge(atr, mod, Integer::sum));
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

		// CALCULA DERIVADOS (Usa os atributos finais corretos)
		calcularEstatisticasDerivadas();

		// Recalcula Mana Base (pois calcularEstatisticasDerivadas pode não fazer isso ou usar lógica antiga)
		this.manaMaxima = this.manaMaximaBase + (this.atributosFinais.getOrDefault(Atributo.INSPIRACAO, 1) / 2.0);

		// APLICA MODIFICADORES DE STATUS (Map<String, Double>)
		if (this.armaEquipada != null) {
			aplicarModificadoresStatus(this.armaEquipada.getModificadoresStatus());
		}

		// Equipamentos (Status: HP, Move, etc.)
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

		// Efeitos Ativos (Status Especiais)
		if (this.efeitosAtivos != null) {
			for (Efeito efeito : this.efeitosAtivos.values()) {
				if (efeito != null && efeito.getModificadores() != null) {
					// Reutiliza o método auxiliar para não duplicar switch-case
					aplicarModificadoresStatus(efeito.getModificadores());
				}
			}
		}

		// Bônus de Classe/Raça Específicos
		if (this.classe instanceof Feiticeiro)
			this.taxaCritica += 0.25;
		if (this.classe instanceof Invocador)
			this.manaMaxima -= 2;
		if (this.raca instanceof Marionette)
			this.taxaCritica += 0.25;
		if (this.raca instanceof HalfAngel)
			this.taxaCritica += 0.25;
		if (this.raca instanceof HalfDemon)
			this.taxaCritica += 0.25;

		if (this.classe instanceof br.com.dantesrpg.model.classes.Ilusionista) {
			int numClones = this.clonesAtivos.size();
			if (numClones > 0) {
				this.danoCritico += (numClones * 0.30);
			}
		}

		if (this.raca != null) {
			this.vidaMaxima -= this.raca.getReducaoHpMaximo(this);
		}

		// CLAMPS FINAIS
		this.taxaCritica = Math.max(0, Math.min(this.taxaCritica, 1.0));
		this.vidaMaxima = Math.max(1.0, this.vidaMaxima);
		this.vidaAtual = Math.min(this.vidaAtual, this.vidaMaxima);
		this.manaAtual = Math.min(this.manaAtual, this.manaMaxima);
		this.armaduraTotal = Math.max(0, this.armaduraTotal);

		this.reducaoDanoArmadura = (double) this.armaduraTotal / (100.0 + this.armaduraTotal);

		// Importante: Não resetar movimentoRestanteTurno se estivermos no meio de um turno!
		if (this.movimentoRestanteTurno > this.movimento) {
			this.movimentoRestanteTurno = this.movimento;
		}
		// Se estiver fora de combate (ou turno novo), o CombatManager chama setMovimentoRestanteTurno(movimento).
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

	public double getEscudoAtual() {
		return escudoAtual;
	}

	public void setEscudoAtual(double escudoAtual) {
		this.escudoAtual = Math.max(0.0, escudoAtual);
		if (this.escudoAtual == 0) {
			this.temEscudoDeSangue = false;
		}
	}

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

	public boolean temPropriedade(String prop) {
		return getPropriedades().contains(prop);
	}

	public boolean isProtagonista() {
		return isProtagonista;
	}

	public void setProtagonista(boolean protagonista) {
		this.isProtagonista = protagonista;
	}

	public boolean isAusente() {
		return isAusente;
	}

	public void setAusente(boolean ausente) {
		this.isAusente = ausente;
	}

	public void setVidaMaxima(double vidaMaxima) {
		this.vidaMaxima = vidaMaxima;
	}

	public void setManaAtual(double manaAtual) {
		this.manaAtual = Math.max(0.0, Math.min(manaAtual, this.manaMaxima));
	}

	public void setVidaAtual(double novaVida, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {

		if (this.isProtagonista && novaVida < this.vidaAtual) {
			System.out.println(">>> " + this.nome + " é PROTAGONISTA e ignorou o dano.");
			return;
		}

		double vidaAntiga = this.vidaAtual;

		if (Math.abs(novaVida - vidaAntiga) < 0.01)
			return;

		double curaRecebida = novaVida - vidaAntiga;

		if (curaRecebida > 0) {
			if (this.reducaoCuraPercentual > 0) {
				double fatorCura = Math.max(0.0, 1.0 - this.reducaoCuraPercentual);
				double curaReduzida = curaRecebida * fatorCura;
				curaRecebida = curaReduzida;
			}

			if (this.efeitosAtivos.containsKey("Ruptura")) {
				curaRecebida = curaRecebida * 0.50;
			} else if (this.efeitosAtivos.containsKey("Dilaceramento")) {
				curaRecebida = curaRecebida * 0.75;
			}

			if (efeitosAtivos.containsKey("Corta Cura")) {
				curaRecebida *= 0.75; // Reduz 25%
				System.out.println(">>> " + this.nome + " teve a cura reduzida em 25% (Corta Cura).");
			}

			// Verifica Corta Cura+
			if (efeitosAtivos.containsKey("Corta Cura+")) {
				curaRecebida *= 0.60; // Reduz 40%
				System.out.println(">>> " + this.nome + " teve a cura reduzida em 40% (Corta Cura+).");
			}

			if (this.raca instanceof Marionette) {
				return;
			}

			if (this.raca != null) {
				// Raça pode modificar a cura (Humano pagando dívida)
				curaRecebida = this.raca.onCuraAttempt(this, curaRecebida);
				novaVida = vidaAntiga + curaRecebida;
			}
		}

		boolean racaLidouComMudanca = false;
		if (this.raca != null) {
			racaLidouComMudanca = this.raca.onHpChangeAttempt(this, vidaAntiga, novaVida, estado, controller);
		}

		if (racaLidouComMudanca) {
			this.vidaAtual = novaVida; // Permite negativo (Humano)
		} else {
			this.vidaAtual = Math.max(0.0, Math.min(novaVida, this.vidaMaxima)); // Clamp padrão
		}

		if (this.raca != null && Math.abs(this.vidaAtual - vidaAntiga) > 0.01) {
			this.raca.onHpChanged(this, vidaAntiga, this.vidaAtual, estado, controller);
		}
	}

	public void forcarCura(double valor) {
		this.vidaAtual = Math.min(this.vidaMaxima, this.vidaAtual + valor);
	}

	public void regenerarVidaFracionada(double quantidade, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		if (this.getVidaAtual() < this.getVidaMaxima()) {
			// Repassa o controller
			this.setVidaAtual(this.getVidaAtual() + quantidade, estado, controller);
			System.out.println(">>> Regeneração: +" + String.format("%.1f", quantidade) + " HP.");
		}
	}

	public double getDanoSofridoRecentemente(int duracaoTU, int tempoGlobalAtual) {
		int tempoLimite = tempoGlobalAtual - duracaoTU;
		double danoTotalRecente = 0.0;

		while (historicoDano.peek() != null && historicoDano.peek().tempoGlobalTU < tempoLimite) {
			historicoDano.poll();
		}

		for (DanoSofrido evento : historicoDano) {
			danoTotalRecente += evento.valor;
		}
		return danoTotalRecente;
	}

	public void adicionarEfeito(Efeito efeito) {
		if (efeito == null || this.efeitosAtivos == null)
			return;

		// Imunidade de Protagonista 
		if (this.isProtagonista) {
			if (efeito.getTipo() == br.com.dantesrpg.model.enums.TipoEfeito.DEBUFF
					|| efeito.getTipo() == br.com.dantesrpg.model.enums.TipoEfeito.DOT) {
				if (!efeito.getNome().equals("Charm") && !efeito.getNome().equals("Controle Mental")) {
					System.out.println(">>> PROTAGONISTA: " + this.nome + " ignorou o efeito ruim " + efeito.getNome());
					return;
				}
			}
		}

		// Imunidade DoT
		if (efeito.getTipo() == br.com.dantesrpg.model.enums.TipoEfeito.DOT) {
			if (this.efeitosAtivos.containsKey("Bênção da Vigília") || this.efeitosAtivos.containsKey("JACKPOT!")) {
				System.out.println(">>> IMUNIDADE! " + this.nome + " resistiu ao efeito [" + efeito.getNome() + "].");
				return;
			}
		}

		if (efeito.getNome().equalsIgnoreCase("Sono")) {
			// Se já está dormindo, não acumula mais sono
			if (this.efeitosAtivos.containsKey("Dormindo"))
				return;

			// Busca pelo efeito existente usando chave fixa "Sono"
			Efeito sonoAtual = this.efeitosAtivos.get("Sono");
			int stacksAtuais = (sonoAtual != null) ? sonoAtual.getStacks() : 0;

			// Incremento forçado: Sempre soma +1 hit
			int novosStacks = stacksAtuais + 1;

			if (novosStacks >= 5) {
				// ESTOUROU: Vira "Dormindo"
				this.removerEfeito("Sono"); // Limpa o acumulador

				// Cria o efeito "Dormindo" (6 Stacks = 6 Turnos)
				Efeito dormindo = new Efeito("Dormindo", br.com.dantesrpg.model.enums.TipoEfeito.DEBUFF, 99999, null, 0,
						0);
				dormindo.setStacks(6);

				this.efeitosAtivos.put("Dormindo", dormindo);
				System.out.println(">>> " + this.getNome() + " caiu no sono profundo! (6 Turnos)");

			} else {
				// Apenas acumula
				if (sonoAtual != null) {
					sonoAtual.setStacks(novosStacks);
					sonoAtual.setDuracaoTURestante(400); // Renova duração (não deixa expirar entre turnos)
				} else {
					// Primeiro stack: Cria um efeito LIMPO localmente
					Efeito novoSono = new Efeito("Sono", br.com.dantesrpg.model.enums.TipoEfeito.DEBUFF, 400, null, 0,
							0);
					novoSono.setStacks(novosStacks);
					this.efeitosAtivos.put("Sono", novoSono);
				}
				System.out.println(">>> " + this.getNome() + " está sonolento (" + novosStacks + "/5).");
			}

			recalcularAtributosEstatisticas();
			return; // Sai, pois já tratamos o sono
		}

		// Lógica Genérica de Atualização/Acúmulo
		if (efeitosAtivos.containsKey(efeito.getNome())) {
			Efeito existente = efeitosAtivos.get(efeito.getNome());

			// Renova duração (pega a maior)
			existente.setDuracaoTURestante(Math.max(existente.getDuracaoTURestante(), efeito.getDuracaoTURestante()));

			// Soma stacks (Crucial para Charm e Half-Demon)
			if (efeito.getStacks() > 0) {
				existente.setStacks(existente.getStacks() + efeito.getStacks());
			}
		} else {
			// Adiciona novo
			efeitosAtivos.put(efeito.getNome(), efeito);
		}

		System.out.println("DEBUG [" + nome + "]: Efeito aplicado: " + efeito.getNome());
		recalcularAtributosEstatisticas();
	}

	public Efeito removerEfeito(String nomeEfeito) {
		if (efeitosAtivos.containsKey(nomeEfeito)) {
			Efeito removido = efeitosAtivos.remove(nomeEfeito);

			if ("Controle Mental".equals(nomeEfeito)) {
				reverterControleMental();
			}

			recalcularAtributosEstatisticas();
			return removido;
		}
		return null;
	}

	private void reverterControleMental() {
		// Procura a propriedade que guardou a facção original
		String faccaoOriginal = null;
		String propParaRemover = null;

		for (String prop : this.propriedades) {
			if (prop.startsWith("ORIGINAL_FACTION:")) {
				faccaoOriginal = prop.split(":")[1];
				propParaRemover = prop;
				break;
			}
		}

		if (faccaoOriginal != null) {
			this.setFaccao(faccaoOriginal);
			this.propriedades.remove(propParaRemover);
			System.out.println(">>> " + this.getNome() + " recobrou a consciência! Voltou para: " + faccaoOriginal);
		}
	}

	// Método que deve ser chamado no início do turno do personagem (no CombatManager)
	public void reduzirDuracaoEfeitos(int tempoDecorrido) {
		// Cria uma lista de cópia para evitar ConcurrentModificationException
		java.util.List<String> paraRemover = new java.util.ArrayList<>();

		for (Efeito e : efeitosAtivos.values()) {
			if (!e.getNome().equals("Charm")) { // Charm não expira por tempo, só por estouro ou cura
				e.reduzirDuracao(tempoDecorrido);
				if (e.expirou()) {
					paraRemover.add(e.getNome());
				}
			}
		}

		for (String nome : paraRemover) {
			System.out.println(">>> Efeito expirou: " + nome);
			removerEfeito(nome); // Chama o método que tem o Hook de reversão
		}
	}

	// Getters
	public String getNome() {
		return nome;
	}

	public Raça getRaca() {
		return raca;
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
	} // Retorna cópia

	public int getContadorTU() {
		return contadorTU;
	}

	public int getMovimento() {
		return movimento;
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

	public int getArmaduraTotal() {
		return armaduraTotal;
	}

	public double getReducaoDanoArmadura() {
		return reducaoDanoArmadura;
	}

	public double getReducaoDoTTopor() {
		return reducaoDoTTopor;
	}

	public Arma getArmaEquipada() {
		return armaEquipada;
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

	public Map<String, Efeito> getEfeitosAtivos() {
		return efeitosAtivos != null ? Collections.unmodifiableMap(efeitosAtivos) : Collections.emptyMap(); // Retorna
																											// Visão
																											// Segura
	}

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

	// Setters
	public void setMovimentoRestanteTurno(int movimento) {
		this.movimentoRestanteTurno = Math.max(0, movimento);
	}

	public void setNivel(int nivel) {
		this.nivel = nivel;
	}

	public void setExperiencia(int experiencia) {
		this.experiencia = experiencia;
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

	public double getSortePercentual() {
		int sorte = this.atributosFinais.getOrDefault(Atributo.SORTE, 1);
		return sorte * 0.01; // 20 Sorte = 0.20 (+20%)
	}

	// --- SOBRECARGA (Overload) ---
	public void setVidaAtual(double novaVida) {
		setVidaAtual(novaVida, null, null);
	}

	public void setManaAtual(int manaAtual) {
		this.manaAtual = Math.max(0, Math.min(manaAtual, this.manaMaxima));
	}

	public void setContadorTU(int contadorTU) {
		this.contadorTU = contadorTU;
	}

	public void setArmaEquipada(Arma armaEquipada) {
		this.armaEquipada = armaEquipada;
	}

	public void setFantasmaNobre(FantasmaNobre fantasmaNobre) {
		this.fantasmaNobre = fantasmaNobre;
	}

	public double getBonusDanoPercentual() {
		return bonusDanoPercentual;
	}

	public void setBonusDanoPercentual(double bonusdeDano) {
		this.bonusDanoPercentual = bonusdeDano;
	}

	public int getPontosParaDistribuir() {
		return pontosParaDistribuir;
	}

	public void setPontosParaDistribuir(int pontos) {
		this.pontosParaDistribuir = pontos;
	}

	public boolean aumentarAtributoBase(Atributo atr) {
		if (this.pontosParaDistribuir > 0) {
			this.pontosParaDistribuir--; // Gasta o ponto

			// Pega o valor atual, soma 1, e coloca de volta
			int valorAtual = this.atributosBase.getOrDefault(atr, 1);
			this.atributosBase.put(atr, valorAtual + 1);

			// Recalcula todos os stats (HP, Dano, etc.)
			recalcularAtributosEstatisticas();
			return true;
		}
		return false; // Sem pontos
	}

	public void setFugiu(boolean fugiu) {
		this.fugiu = fugiu;
	}

	public int getXpReward() {
		return xpReward;
	}

	public void setXpReward(int xp) {
		this.xpReward = xp;
	}

	public int getXpAtual() {
		return xpAtual;
	}

	public void setXpAtual(int xp) {
		this.xpAtual = xp;
	}

	public int getXpParaProximoNivel() {
		return 50 * (this.nivel * this.nivel);
	}

	public void ganharExperiencia(int quantidade) {
		this.xpAtual += quantidade;
		System.out.println(">>> " + this.nome + " ganhou " + quantidade + " XP. Total: " + this.xpAtual);

		// Loop para permitir subir múltiplos níveis de uma vez
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

	public boolean isAtivoNoCombate() {
		if (this.isAusente)
			return false;
		if (this.fugiu)
			return false;

		if (this.raca instanceof Humano) {
			Humano h = (Humano) this.raca;
			if (h.getEstadoAtual() == Humano.EstadoEmprestimo.ATIVO) {
				return true;
			}
		}

		return this.vidaAtual > 0;
	}

	public boolean isVivo() {
		if (this.raca instanceof Humano) {
			Humano h = (Humano) this.raca;
			if (h.getEstadoAtual() == Humano.EstadoEmprestimo.ATIVO
					|| h.getEstadoAtual() == Humano.EstadoEmprestimo.PENDENTE_RESOLUCAO) {
				return true;
			}
		}
		return this.vidaAtual > 0;
	}

	public String getFaccao() {
		return faccao;
	}

	public void setFaccao(String faccao) {
		this.faccao = faccao;
	}

	public Map<Atributo, Integer> getAtributosBase() {
		return atributosBase;
	}

	public double getVidaMaximaBase() {
		return vidaMaximaBase;
	}

	public List<Personagem> getClonesAtivos() {
		return clonesAtivos;
	}

	public void registrarClone(Personagem clone) {
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

	public Habilidade getUltimaHabilidadeUsada() {
		return ultimaHabilidadeUsada;
	}

	public void setUltimaHabilidadeUsada(Habilidade h) {
		this.ultimaHabilidadeUsada = h;
	}

	public void adicionarHabilidadeExtra(Habilidade h) {
		this.habilidadesExtras.add(h);
	}

	public void limparHabilidadesExtras() {
		this.habilidadesExtras.clear();
	}

	public double getReducaoCuraPercentual() {
		return reducaoCuraPercentual;
	}

	public boolean isEscudoDeSangue() {
		return temEscudoDeSangue;
	}

	public void setTemEscudoDeSangue(boolean b) {
		this.temEscudoDeSangue = b;
	}

	public String getJsonFileName() {
		return jsonFileName;
	}

	public void setJsonFileName(String jsonFileName) {
		this.jsonFileName = jsonFileName;
	}

	public List<Habilidade> getHabilidadesDeClasse() {
		List<Habilidade> combinadas = new ArrayList<>();
		if (habilidadesDeClasse != null)
			combinadas.addAll(habilidadesDeClasse);
		if (habilidadesExtras != null)
			combinadas.addAll(habilidadesExtras);

		if (armaEquipada instanceof br.com.dantesrpg.model.Grimorio) {
			br.com.dantesrpg.model.Grimorio grimorio = (br.com.dantesrpg.model.Grimorio) armaEquipada;
			combinadas.addAll(grimorio.getMagiasArmazenadas());
		}

		else if (armaEquipada != null) {
			for (String nomeHab : armaEquipada.getHabilidadesConcedidasNomes()) {
				Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeHab);
				if (h != null) {
					combinadas.add(h);
				}
			}
		}

		return combinadas;
	}

	private void aplicarModificadoresStatus(Map<String, Double> mods) {
		if (mods == null)
			return;

		for (Map.Entry<String, Double> entry : mods.entrySet()) {
			String chave = entry.getKey().toUpperCase();
			double valor = entry.getValue();

			switch (chave) {
			case "HP_MAXIMO": // Adiciona Vida Plana
				this.vidaMaxima += valor;
				break;
			case "MP_MAXIMO":
				this.manaMaxima += valor;
				break;
			case "DANO_BONUS_PERCENTUAL":
				this.bonusDanoPercentual += valor; // Ex: 0.10 para +10%
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
				this.reducaoDanoTopor += valor; // Soma na redução percentual
				break;
			case "ARMADURA_TOTAL":
				this.armaduraTotal += (int) valor;
				break;
			case "RESISTENCIA_DOT":
				this.reducaoDoTTopor += valor;
				break;
			default:
				// Se não for um status direto, pode ser ignorado ou logado
				break;
			}
		}
	}

	public void setManaMaxima(double manaMaxima) {
		this.manaMaximaBase = manaMaxima;
		this.manaMaxima = this.manaMaximaBase + (this.atributosFinais.getOrDefault(Atributo.INSPIRACAO, 1) / 2.0);
	}

	public int getValorPropriedade(String chave) {
		for (String prop : getPropriedades()) {
			// Caso 1: Formato "CHAVE:VALOR"
			if (prop.startsWith(chave + ":")) {
				try {
					return Integer.parseInt(prop.split(":")[1]);
				} catch (Exception e) {
					return 0;
				}
			}
			// Caso 2: Formato "CHAVE" (Booleano, conta como 1)
			if (prop.equals(chave))
				return 1;
		}
		return 0;
	}

	public void setVidaMaximaBase(double vidaBase) {
		this.vidaMaximaBase = vidaBase;
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

	public void setAtributoBase(Atributo atributo, int valor) {
		if (this.atributosBase == null) {
			this.atributosBase = new HashMap<>();
		}
		this.atributosBase.put(atributo, valor);
	}

	public void setIniciativaBase(int valor) {
		this.placarIniciativa = valor;
	}

	public int getGrau() {
		return grau;
	}

	public void setGrau(int grau) {
		this.grau = grau;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getSegmentosVida() {
		return segmentosVida;
	}

	public void setSegmentosVida(int segmentos) {
		this.segmentosVida = segmentos;
	}

	public double getMultiplicadorCustoTU() {
		double mult = 1.0;

		if (efeitosAtivos.containsKey("Lento")) {
			mult += 0.30; // +30%
		}

		if (efeitosAtivos.containsKey("Muito Lento")) {
			mult += 0.50; // +50%
		}

		// Se tiver os dois, soma: 1.0 + 0.3 + 0.5 = 1.8x custo
		return mult;
	}
}

class DanoSofrido {
	double valor;
	int tempoGlobalTU; // O "relógio" do combate quando o dano ocorreu

	public DanoSofrido(double valor, int tempoGlobalTU) {
		this.valor = valor;
		this.tempoGlobalTU = tempoGlobalTU;
	}
}