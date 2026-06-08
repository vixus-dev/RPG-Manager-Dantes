package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.ArmaRanged;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.racas.Marionette;
import br.com.dantesrpg.model.util.DamageEvent;
import br.com.dantesrpg.model.util.DiceRoller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsável por todo cálculo de dano: estimativas, geração de DamageEvents,
 * modificadores de crítico, redução de armadura e mecânicas especiais (cascata, soco sério).
 */
public class DamageCalculator {

	private final CombatManager combatManager;

	public DamageCalculator(CombatManager combatManager) {
		this.combatManager = combatManager;
	}

	private CombatController getController() {
		return combatManager.getMainController();
	}

	// ========== ESTIMATIVA DE DANO ==========

	public int estimarDano(Personagem ator, Habilidade habilidade, Personagem alvo, int rolagemDadoAtributo,
			int rolagemTrocado) {
		if (ator == null || ator.getArmaEquipada() == null)
			return 0;
		Arma arma = ator.getArmaEquipada();

		double multiplicadorHabilidade = 1.0;
		if (habilidade != null) {
			multiplicadorHabilidade = habilidade.getMultiplicadorModificado(ator, alvo, null);
		}

		int numeroDeTicks = 1;
		AcaoMestreInput inputDummy = new AcaoMestreInput(ator, new ArrayList<>(), habilidade);
		if (habilidade != null && habilidade.getNome().equals("Trocado")) {
			inputDummy.adicionarResultadoDado("DADO_CHANCE_TROCADO", rolagemTrocado);
			numeroDeTicks = habilidade.getTicksModificados(ator, inputDummy);
		} else if (habilidade != null) {
			numeroDeTicks = habilidade.getTicksModificados(ator, inputDummy);
		} else {
			numeroDeTicks = arma.getTicksDeDano();
		}

		double danoTotalEstimado = 0.0;
		rolagemDadoAtributo = Math.max(0, rolagemDadoAtributo);

		double fatorSorte = 1.0 + ator.getSortePercentual();

		boolean isTiroEspecial = ator.getEfeitosAtivos().containsKey("Tiro Especial");
		double bonusTiroEspecial = 0.0;
		if (isTiroEspecial) {
			int sag = ator.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);
			bonusTiroEspecial = sag * 0.1;
		}

		for (int i = 0; i < numeroDeTicks; i++) {
			double modGolpePerfeito = 1.0;

			if (i == 0 && rolagemDadoAtributo > 0) {
				int tipoDado = DiceRoller
						.getTipoDado(ator.getAtributosFinais().getOrDefault(arma.getAtributoMultiplicador(), 1));
				if (rolagemDadoAtributo == tipoDado) {
					modGolpePerfeito = 1.25;
				}
			}

			double dano = arma.getDanoBase() * (1 + (0.075 * rolagemDadoAtributo));
			dano *= fatorSorte;

			if (ator.getBonusDanoPercentual() > 0) {
				dano *= (1.0 + ator.getBonusDanoPercentual());
			}
			dano *= getMultiplicadorDanoDominio(ator);
			dano *= (1.0 + bonusTiroEspecial);

			if (alvo != null) {
				dano *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, null, inputDummy);
			}

			dano *= multiplicadorHabilidade;
			dano *= modGolpePerfeito;

			if (habilidade != null && habilidade.getNome().equals("Trocado")) {
				dano *= (1.0 + (i * 0.25));
			}

			danoTotalEstimado += Math.max(0.0, dano);
		}

		return (int) Math.ceil(danoTotalEstimado);
	}

	// ========== RESOLUÇÃO DE DANO PADRÃO ==========

	public void resolverDanoPadrao(Personagem ator, Arma arma, int rolagemDadoAtributo, List<Personagem> alvos,
			double multiplicadorHabilidade, TipoAcao tipoAcaoDano, Habilidade habilidade,
			EstadoCombate estado, AcaoMestreInput input) {

		boolean estavaEmStealth = processarStealthInicial(ator);
		boolean isTiroEspecial = processarTiroEspecial(ator);
		List<Arma> armasDaAcao = resolverArmasDaAcao(ator, arma, habilidade, input);
		if (armasDaAcao.isEmpty()) {
			System.out.println(ator.getNome() + " esta desarmado!");
			return;
		}
		Arma armaPrincipal = armasDaAcao.get(0);

		Map<Personagem, List<DamageEvent>> matrizDeDanos = new HashMap<>();

		boolean isAtaqueBasico = habilidade == null;
		List<Arma> armasDosTicksBasicos = isAtaqueBasico ? expandirArmasPorTicks(armasDaAcao) : List.of(armaPrincipal);
		int ticksBase = isAtaqueBasico ? armasDosTicksBasicos.size() : habilidade.getTicksModificados(ator, input);
		double danoBaseHabilidade = (!isAtaqueBasico && armasDaAcao.size() >= 2)
				? armasDaAcao.stream().mapToDouble(Arma::getDanoBase).sum() * 0.70
				: -1.0;

		boolean isArmaRanged = (armaPrincipal instanceof ArmaRanged)
				|| "Ranged".equalsIgnoreCase(armaPrincipal.getTipo());
		boolean isModoCoronhada = (input.getModoAtaque() == ModoAtaque.CORONHADA);
		double multiplicadorAtaqueAlternativo = armaPrincipal.getMultiplicadorAtaqueAlternativoBasico();

		boolean isRajada = (habilidade == null && armasDaAcao.size() == 1 && isArmaRanged && !isModoCoronhada);

		int tirosExtrasSolicitados = input.getTirosExtras();
		int tirosExtrasReais = 0;

		// Consumo de Munição (adiado até confirmação da janela de resolução)
		if (habilidade == null && !isModoCoronhada) {
			for (Arma armaSelecionada : armasDaAcao) {
				if (armaSelecionada.isRequerMunicao() && armaSelecionada.getMunicaoAtual() < 1) {
					System.out.println(">>> CLIQUE SECO! " + armaSelecionada.getNome() + " esta sem municao.");
					return;
				}
			}
			int municaoAtual = armaPrincipal.getMunicaoAtual();

			if (isRajada && municaoAtual < 1) {
				System.out.println(">>> CLIQUE SECO! Sem munição.");
				return;
			}

			if (isRajada && tirosExtrasSolicitados > 0) {
				int municaoDisponivel = municaoAtual - 1;
				tirosExtrasReais = Math.min(tirosExtrasSolicitados, municaoDisponivel);
			}

			final int tirosExtrasConfirmados = tirosExtrasReais;
			final List<Arma> armasParaConsumir = new ArrayList<>(armasDaAcao);
			combatManager.setPendingMunicaoConsumption(() -> {
				int totalTirosAGastar = 0;
				for (Arma armaSelecionada : armasParaConsumir) {
					if (armaSelecionada.isRequerMunicao()) {
						armaSelecionada.gastarMunicao();
						totalTirosAGastar++;
					}
				}
				for (int k = 0; k < tirosExtrasConfirmados; k++) {
					armaPrincipal.gastarMunicao();
					totalTirosAGastar++;
				}
				if (totalTirosAGastar > 1) {
					System.out.println(">>> ARMA: Rajada confirmada. " + totalTirosAGastar + " tiros gastos.");
				} else {
					System.out.println(">>> ARMA: Tiro único confirmado.");
				}
			});
		}

		double fatorSorte = 1.0 + ator.getSortePercentual();

		// Geração de Eventos de Dano
		for (Personagem alvo : alvos) {
			if (!isAlvoValido(alvo))
				continue;
			List<DamageEvent> eventosDoAlvo = new ArrayList<>();

			// --- GRUPO A: TICKS BASE ---
			for (int i = 0; i < ticksBase; i++) {
				Arma armaDoTick = isAtaqueBasico ? armasDosTicksBasicos.get(i) : armaPrincipal;
				double modModo = 1.0;
				if (input.getModoAtaque() == ModoAtaque.FRACO)
					modModo = 0.75;
				if (input.getModoAtaque() == ModoAtaque.FORTE)
					modModo = 1.25;
				if (isModoCoronhada)
					modModo = multiplicadorAtaqueAlternativo;

				double multiplicadorFinal = multiplicadorHabilidade * modModo;

				Boolean criticoManual = (input != null) ? input.getCriticoManual() : null;
				double modCritico = calcularModificadorCritico(ator, rolagemDadoAtributo, armaDoTick, i, estavaEmStealth,
						multiplicadorFinal, isTiroEspecial, criticoManual);
				boolean isCrit = (modCritico > 1.0);

				double danoBruto = calcularDanoFinalTick(ator, armaDoTick, rolagemDadoAtributo, alvo, estado, input,
						multiplicadorFinal, modCritico, fatorSorte, isTiroEspecial, danoBaseHabilidade);
				double danoLiquido = aplicarReducaoArmadura(danoBruto, ator, alvo, estado, armaDoTick);

				String labelTick = (ticksBase > 1) ? "Hit " + (i + 1) + " - " + armaDoTick.getNome() : "Ataque";
				eventosDoAlvo.add(criarEventoDano(danoLiquido, labelTick, isCrit, ator, alvo, armaDoTick, input, estado,
						isTiroEspecial, multiplicadorHabilidade));

				// Eco (Combo)
				if (ator.getEfeitosAtivos().containsKey("Combo!") && multiplicadorHabilidade == 1.0) {
					eventosDoAlvo.add(new DamageEvent(danoLiquido * 0.30, "Eco", false, null));
				}
				// Cascata Marionette
				if (ator.getRaca() instanceof Marionette && isCrit && habilidade == null) {
					simularCascataMarionette(ator, alvo, armaDoTick, rolagemDadoAtributo, input, estado, isTiroEspecial,
							eventosDoAlvo, danoLiquido);
				}
			}

			// --- GRUPO B: TICKS DE RAJADA ---
			if (tirosExtrasReais > 0) {
				double danoBaseRajada = calcularDanoFinalTick(ator, armaPrincipal, rolagemDadoAtributo, alvo, estado, input, 1.0,
						1.0, fatorSorte, isTiroEspecial);
				double danoRajadaUnitario = danoBaseRajada * 0.25;

				for (int i = 0; i < tirosExtrasReais; i++) {
					boolean isCritRajada = (Math.random() < ator.getTaxaCritica());
					double modCrit = isCritRajada ? (1.0 + ator.getDanoCritico()) : 1.0;

					double danoFinalRajada = danoRajadaUnitario * modCrit;
					double danoLiquidoRajada = aplicarReducaoArmadura(danoFinalRajada, ator, alvo, estado, armaPrincipal);

					eventosDoAlvo.add(criarEventoDano(danoLiquidoRajada, "Rajada " + (i + 1), isCritRajada, ator, alvo,
							armaPrincipal, input, estado, isTiroEspecial, multiplicadorHabilidade));
				}
			}

			matrizDeDanos.put(alvo, eventosDoAlvo);
		}

		finalizarAcao(ator, isTiroEspecial, alvos);

		if (!matrizDeDanos.isEmpty()) {
			getController().abrirJanelaResolucao(ator, alvos, habilidade, matrizDeDanos);
		}
	}

	public void resolverDeadEye(Personagem ator, Arma arma, int rolagemDadoAtributo, List<Personagem> alvos,
			Habilidade habilidade, EstadoCombate estado, AcaoMestreInput input) {
		if (ator == null || arma == null || alvos == null || alvos.isEmpty()) {
			return;
		}

		boolean estavaEmStealth = processarStealthInicial(ator);
		boolean isTiroEspecial = processarTiroEspecial(ator);
		double fatorSorte = 1.0 + ator.getSortePercentual();
		double bonusCriticoDeadEye = ator.getTaxaCritica() * 2.0;
		double modCritico = 1.0 + ator.getDanoCritico() + bonusCriticoDeadEye;
		Map<Personagem, List<DamageEvent>> matrizDeDanos = new HashMap<>();

		for (Personagem alvo : alvos) {
			if (!isAlvoValido(alvo)) {
				continue;
			}
			double danoBruto = calcularDanoFinalTick(ator, arma, rolagemDadoAtributo, alvo, estado, input,
					habilidade.getMultiplicadorDeDano(), modCritico, fatorSorte, isTiroEspecial);
			if (estavaEmStealth) {
				System.out.println(">>> Stealth consumido antes do DeadEye.");
			}
			double danoLiquido = aplicarReducaoArmadura(danoBruto, ator, alvo, estado, arma);
			List<DamageEvent> eventos = new ArrayList<>();
			eventos.add(criarEventoDano(danoLiquido, "DeadEye", true, ator, alvo, arma, input, estado,
					isTiroEspecial, habilidade.getMultiplicadorDeDano()));
			matrizDeDanos.put(alvo, eventos);
		}

		finalizarAcao(ator, isTiroEspecial, alvos);
		if (!matrizDeDanos.isEmpty()) {
			System.out.println(">>> DeadEye: crítico garantido com bônus crítico adicional de +"
					+ String.format("%.1f", bonusCriticoDeadEye * 100) + "%.");
			getController().abrirJanelaResolucao(ator, alvos, habilidade, matrizDeDanos);
		}
	}

	// ========== SOCO SÉRIO ==========

	public int resolverSocoSerio(Personagem ator, List<Personagem> alvos, Habilidade habilidade,
			AcaoMestreInput input, EstadoCombate estado) {
		System.out.println(">>> " + ator.getNome() + " prepara SOCO SÉRIO!");
		int rolagemDadoAtributo = input.getResultadoDado("DADO_ATRIBUTO");
		Arma arma = ator.getArmaEquipada();

		int cursorX = input.getEpicentroX();
		int cursorY = input.getEpicentroY();
		int px = ator.getPosX();
		int py = ator.getPosY();
		int deltaX = Math.abs(cursorX - px);
		int deltaY = Math.abs(cursorY - py);
		boolean isHorizontal = (deltaX > deltaY);

		Map<Personagem, List<DamageEvent>> matrizDeDanos = new HashMap<>();

		for (Personagem alvo : alvos) {
			boolean estaNaLinhaCentral = false;
			if (isHorizontal && alvo.getPosY() == py)
				estaNaLinhaCentral = true;
			if (!isHorizontal && alvo.getPosX() == px)
				estaNaLinhaCentral = true;

			double danoCalculado = arma.getDanoBase() * (1 + (0.075 * rolagemDadoAtributo));
			if (ator.getBonusDanoPercentual() > 0)
				danoCalculado *= (1.0 + ator.getBonusDanoPercentual());
			danoCalculado *= getMultiplicadorDanoDominio(ator);
			danoCalculado *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, estado, input);

			double multiplicador;
			boolean isCrit = false;

			if (estaNaLinhaCentral) {
				multiplicador = 5.0;
				if (Math.random() < ator.getTaxaCritica()) {
					multiplicador *= (1.0 + ator.getDanoCritico());
					isCrit = true;
				}
			} else {
				multiplicador = 3.0;
			}

			double danoBruto = danoCalculado * multiplicador;
			double danoPosArmadura = aplicarReducaoArmadura(danoBruto, ator, alvo, estado);

			List<DamageEvent> eventos = new ArrayList<>();
			eventos.add(new DamageEvent(danoPosArmadura, estaNaLinhaCentral ? "EPICENTRO" : "ONDA", isCrit, null));
			matrizDeDanos.put(alvo, eventos);
		}

		if (!matrizDeDanos.isEmpty()) {
			getController().abrirJanelaResolucao(ator, alvos, habilidade, matrizDeDanos);
		}

		return 0;
	}

	// ========== CÁLCULO DE DANO POR TICK ==========

	private double calcularDanoFinalTick(Personagem ator, Arma arma, int rolagem, Personagem alvo,
			EstadoCombate estado, AcaoMestreInput input, double modHabilidade, double modCritico,
			double modSorte, boolean isTiroEspecial) {
		return calcularDanoFinalTick(ator, arma, rolagem, alvo, estado, input, modHabilidade, modCritico,
				modSorte, isTiroEspecial, -1.0);
	}

	private double calcularDanoFinalTick(Personagem ator, Arma arma, int rolagem, Personagem alvo,
			EstadoCombate estado, AcaoMestreInput input, double modHabilidade, double modCritico,
			double modSorte, boolean isTiroEspecial, double danoBaseOverride) {
		double danoBase = danoBaseOverride > 0 ? danoBaseOverride : arma.getDanoBase();
		double dano = danoBase * (1 + (0.075 * rolagem));

		dano *= modSorte;

		if (ator.getBonusDanoPercentual() > 0) {
			dano *= (1.0 + ator.getBonusDanoPercentual());
		}
		dano *= getMultiplicadorDanoDominio(ator);

		if (isTiroEspecial) {
			int sagacidade = ator.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);
			double bonusPercent = sagacidade * 0.05;
			dano *= (1.0 + bonusPercent);
		}

		dano *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, estado, input);
		dano *= modHabilidade;
		dano *= modCritico;

		return Math.max(0, dano);
	}

	// ========== MODIFICADOR CRÍTICO ==========

	private double calcularModificadorCritico(Personagem ator, int rolagem, Arma arma, int tickIndex,
			boolean stealth, double modHabilidade, boolean isTiroEspecial, Boolean criticoManual) {
		double mod = 1.0;

		if (tickIndex == 0) {
			int tipoDado = DiceRoller
					.getTipoDado(ator.getAtributosFinais().getOrDefault(arma.getAtributoMultiplicador(), 1));
			if (rolagem == tipoDado) {
				mod *= 1.25;
				System.out.println(">>> GOLPE PERFEITO!");
			}
		}

		if (criticoManual != null) {
			if (criticoManual) {
				mod *= (1 + ator.getDanoCritico());
				System.out.println(">>> ACERTO CRÍTICO! (Manual)");
			}
		} else {
			double bonusCritRate = 0.0;
			if (isTiroEspecial) {
				int sag = ator.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);
				bonusCritRate = sag * 0.01;
			}

			if (Math.random() < (ator.getTaxaCritica() + bonusCritRate)) {
				mod *= (1 + ator.getDanoCritico());
				System.out.println(">>> ACERTO CRÍTICO!");
			}
		}

		if (tickIndex == 0 && stealth && modHabilidade == 1.0 && mod == 1.0) {
			mod = (1 + ator.getDanoCritico());
			System.out.println(">>> Stealth: Crítico Garantido!");
		}

		return mod;
	}

	// ========== REDUÇÃO DE ARMADURA ==========

	public double aplicarReducaoArmadura(double danoBruto, Personagem ator, Personagem alvo, EstadoCombate estado) {
		Arma arma = ator != null ? ator.getArmaEquipada() : null;
		return aplicarReducaoArmadura(danoBruto, ator, alvo, estado, arma);
	}

	public double aplicarReducaoArmadura(double danoBruto, Personagem ator, Personagem alvo, EstadoCombate estado,
			Arma arma) {
		double danoAjustado = aplicarReducaoDanoPreArmadura(danoBruto, ator, alvo, estado);
		double reducaoArmadura = alvo.getReducaoDanoArmadura() + alvo.getReducaoDanoTopor();
		reducaoArmadura = Math.min(reducaoArmadura, 0.90);
		double pularDefesa = 0.0;
		if (ator != null && arma != null) {
			pularDefesa = arma.getIgnorarDefesaPercentual(ator, alvo, estado);
		}
		reducaoArmadura -= (reducaoArmadura * pularDefesa);
		if (alvo.getEfeitosAtivos().containsKey("Ruptura"))
			reducaoArmadura -= 0.25;
		return Math.max(0, danoAjustado * (1.0 - reducaoArmadura));
	}

	public double aplicarReducaoDanoPreArmadura(double danoBruto, Personagem ator, Personagem alvo,
			EstadoCombate estado) {
		if (danoBruto <= 0 || alvo == null || alvo.getRaca() == null) {
			return danoBruto;
		}
		double multiplicador = alvo.getRaca().getMultiplicadorDanoRecebidoPreArmadura(alvo, ator, estado);
		return Math.max(0, danoBruto * Math.max(0.0, multiplicador));
	}

	// ========== MULTIPLICADORES ==========

	public double getMultiplicadorDanoDominio(Personagem ator) {
		double mult = 1.0;
		if (ator.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble")
				&& getController() != null && getController().isPersonagemNoDominio(ator, "dominio_lyria")) {
			mult *= 0.50;
			System.out.println(">>> Idle Death Gamble: -50% dano (dentro do domínio).");
		}
		return mult;
	}

	public double getMultiplicadorBonusDanoComArma(Personagem ator, Arma arma, Personagem alvo,
			EstadoCombate estado, AcaoMestreInput input) {
		if (arma == null) {
			return 1.0;
		}
		double multiplicador = arma.getBonusDanoArma(ator, alvo, estado, input);
		if (ator != null && ator.getRaca() != null) {
			multiplicador *= ator.getRaca().getMultiplicadorBonusDanoArma(ator, arma, alvo, estado, input);
		}
		return multiplicador;
	}

	// ========== CASCATA MARIONETTE ==========

	private void simularCascataMarionette(Personagem ator, Personagem alvo, Arma arma, int rolagemOriginal,
			AcaoMestreInput input, EstadoCombate estado, boolean isTiroEspecial, List<DamageEvent> listaEventos,
			double danoBaseAnterior) {
		double danoParaCascata = arma.getDanoBase() * (1 + (0.075 * rolagemOriginal));
		if (ator.getBonusDanoPercentual() > 0)
			danoParaCascata *= (1.0 + ator.getBonusDanoPercentual());
		danoParaCascata *= getMultiplicadorDanoDominio(ator);
		danoParaCascata *= (1.0 + ator.getSortePercentual());
		danoParaCascata *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, estado, input);

		double reducaoArmadura = alvo.getReducaoDanoArmadura() + alvo.getReducaoDanoTopor();
		if (alvo.getEfeitosAtivos().containsKey("Ruptura"))
			reducaoArmadura -= 0.25;

		int nivel = 1;
		while (nivel <= 7) {
			double danoBrutoCascata = danoParaCascata * 0.50;
			if (danoBrutoCascata < 1.0)
				break;

			boolean cascataCrit = (Math.random() < ator.getTaxaCritica());
			double modCrit = cascataCrit ? (1.0 + ator.getDanoCritico()) : 1.0;

			double danoPosArmadura = aplicarReducaoArmadura(danoBrutoCascata * modCrit, ator, alvo, estado);

			listaEventos.add(new DamageEvent(danoPosArmadura, "Cascata " + nivel, cascataCrit, null));

			danoParaCascata = danoBrutoCascata;
			if (!cascataCrit)
				break;
			nivel++;
		}
	}

	public void executarRecursaoCascata(Personagem ator, Personagem alvo, double danoBaseAnterior, int nivel,
			EstadoCombate estado, TipoAcao tipoAcaoDano) {
		if (nivel > 7) {
			System.out.println(">>> CASCATA (Marionette): Limite de 7 golpes atingido.");
			return;
		}

		double danoAtual = danoBaseAnterior * 0.50;

		if (danoAtual < 1.0) {
			System.out.println(">>> CASCATA: Dano muito baixo, parando.");
			return;
		}

		boolean critico = (Math.random() < ator.getTaxaCritica());
		double modCritico = critico ? (1.0 + ator.getDanoCritico()) : 1.0;

		double danoFinal = danoAtual * modCritico;

		System.out.println(
				">>> CASCATA Nível " + nivel + " (Crítico: " + critico + "): " + String.format("%.1f", danoFinal));

		combatManager.getDamageApplicator().aplicarDanoAoAlvo(ator, alvo, danoFinal, false, tipoAcaoDano, estado);

		if (critico) {
			executarRecursaoCascata(ator, alvo, danoAtual, nivel + 1, estado, tipoAcaoDano);
		}
	}

	// ========== CRIAÇÃO DE EVENTO DE DANO ==========

	private DamageEvent criarEventoDano(double danoEstimado, String label, boolean isCritico, Personagem ator,
			Personagem alvo, Arma arma, AcaoMestreInput input, EstadoCombate estado, boolean isTiroEspecial,
			double modHab) {
		java.util.function.Consumer<Double> onHit = (danoRealPosResolucao) -> {
			combatManager.getEffectProcessor().processarEfeitosOnHit(ator, alvo, arma, danoRealPosResolucao, estado,
					isTiroEspecial);
			combatManager.getEffectProcessor().processarHooksDeSistema(ator, alvo, arma, input, danoRealPosResolucao,
					estado, modHab, isCritico);
		};

		return new DamageEvent(danoEstimado, label, isCritico, onHit);
	}

	// ========== HELPERS ==========

	private List<Arma> resolverArmasDaAcao(Personagem ator, Arma armaFallback, Habilidade habilidade,
			AcaoMestreInput input) {
		List<Arma> armas = new ArrayList<>();
		if (habilidade == null && input != null) {
			armas.addAll(input.getArmasSelecionadas());
		} else if (ator != null) {
			armas.addAll(ator.getArmasEquipadas());
		}
		if (armas.isEmpty() && armaFallback != null) {
			armas.add(armaFallback);
		}
		armas.removeIf(arma -> arma == null);
		return armas;
	}

	private List<Arma> expandirArmasPorTicks(List<Arma> armas) {
		List<Arma> resultado = new ArrayList<>();
		for (Arma arma : armas) {
			for (int i = 0; i < Math.max(1, arma.getTicksDeDano()); i++) {
				resultado.add(arma);
			}
		}
		return resultado;
	}

	private boolean processarStealthInicial(Personagem ator) {
		if (ator.getEfeitosAtivos().containsKey("Stealth")) {
			System.out.println(">>> " + ator.getNome() + " saiu do modo Stealth para atacar!");
			ator.removerEfeito("Stealth");
			ator.recalcularAtributosEstatisticas();
			return true;
		}
		return false;
	}

	private boolean processarTiroEspecial(Personagem ator) {
		if (ator.getEfeitosAtivos().containsKey("Tiro Especial")) {
			System.out.println(">>> ACERTO DE CONTAS (Tiro Especial) ATIVADO!");
			return true;
		}
		return false;
	}

	private void finalizarAcao(Personagem ator, boolean isTiroEspecial, List<Personagem> alvos) {
		if (isTiroEspecial) {
			System.out.println(">>> Tiro Especial consumido.");
			ator.removerEfeito("Tiro Especial");
			ator.recalcularAtributosEstatisticas();
		}
	}

	private boolean isAlvoValido(Personagem alvo) {
		if (alvo == null)
			return false;
		if (alvo.isAtivoNoCombate())
			return true;
		if (alvo instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
			return ((br.com.dantesrpg.model.elementos.ObjetoDestrutivel) alvo).isIntacto();
		}
		return false;
	}
}
