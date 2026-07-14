package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EfeitoOnHit;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.util.BarbaroUtils;
import br.com.dantesrpg.model.util.Maldicao;
import br.com.dantesrpg.model.util.MaldicaoUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsável pelo processamento de efeitos: aplicação com imunidades,
 * efeitos on-hit, controle mental, hooks de sistema, Estado Dourado,
 * e mana passiva do Modo Justiça.
 */
public class EffectProcessor {

	private final CombatManager combatManager;

	public EffectProcessor(CombatManager combatManager) {
		this.combatManager = combatManager;
	}

	private CombatController getController() {
		return combatManager.getMainController();
	}

	// ========== APLICAÇÃO DE EFEITO ==========

	public void aplicarEfeito(Personagem alvo, Efeito efeito) {
		if (alvo == null || efeito == null)
			return;

		String nomeEf = efeito.getNome().toLowerCase();
		if (alvo.getValorPropriedade("IMUNIDADE_DOT") > 0 && efeito.getTipo() == TipoEfeito.DOT) {
			System.out.println(">>> IMUNE! " + alvo.getNome() + " é imune a DoT: " + efeito.getNome());
			return;
		}
		if (alvo.getValorPropriedade("IMUNIDADE_CONTROLE") > 0) {
			if (nomeEf.contains("sono") || nomeEf.contains("dormindo") || nomeEf.contains("atordoado")
					|| nomeEf.contains("stun") || nomeEf.contains("medo") || nomeEf.contains("paralisia")
					|| nomeEf.contains("congelado")) {
				System.out.println(">>> IMUNE! " + alvo.getNome() + " ignorou o controle: " + efeito.getNome());
				return;
			}
		}

		if (alvo.isProtagonista()) {
			if (efeito.getTipo() == TipoEfeito.DEBUFF || efeito.getTipo() == TipoEfeito.DOT) {
				System.out.println(
						">>> PROTAGONISTA: " + alvo.getNome() + " ignorou o efeito ruim " + efeito.getNome());
				return;
			}
		}

		alvo.adicionarEfeito(efeito);
		System.out.println(">>> Efeito [" + efeito.getNome() + "] aplicado em " + alvo.getNome() + " ("
				+ efeito.getDuracaoTUInicial() + " TU).");
	}

	// ========== EFEITOS ON-HIT ==========

	public void processarEfeitosOnHit(Personagem ator, Personagem alvo, Arma arma, double danoCausado,
			EstadoCombate estado, boolean isTiroEspecial) {
		if (danoCausado <= 0)
			return;

		String nomeEfeito = arma != null ? arma.getNomeEfeitoOnHit() : null;
		double chance = arma != null ? arma.getChanceEfeitoOnHit() : 0.0;

		// Yaweh unique weapon special on-hit effect (Chama Divina)
		if (arma != null && "Yaweh".equalsIgnoreCase(arma.getNome())) {
			boolean despertarAtivo = ator.temPropriedade("DESPERTAR_DIVINO_ATIVO");
			double chanceChama = despertarAtivo ? 0.40 : 0.25;

			boolean podeAplicar;
			
			if (despertarAtivo) {
				// Despertar Divino: aplica a todos os inimigos
				podeAplicar = true;
			} else {
				// Normal: apenas contra demônios
				String racaAlvo = (alvo.getRaca() != null) ? alvo.getRaca().getNome().toLowerCase() : "";
				podeAplicar = racaAlvo.contains("demônio") || racaAlvo.contains("demonio") || racaAlvo.contains("demon") || (alvo.getRaca() instanceof br.com.dantesrpg.model.racas.HalfDemon);
			}

			if (podeAplicar && Math.random() <= chanceChama) {
				System.out.println(">>> Yaweh On-Hit: Chama Divina ativado contra " + alvo.getNome()
						+ "!" + (despertarAtivo ? " (Despertar Divino: 40%)" : ""));
				int danoDaSource = Math.max(1, (int) danoCausado);
				Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Chama Divina", 0, danoDaSource);
				efeito.setStacks(1);
				aplicarEfeito(alvo, efeito);
			}
		}

		if (arma != null && !arma.getEfeitosOnHit().isEmpty()) {
			for (EfeitoOnHit efeitoOnHit : arma.getEfeitosOnHit()) {
				processarEfeitoOnHit(ator, alvo, arma, danoCausado, estado, efeitoOnHit);
			}
		} else if (nomeEfeito != null && !nomeEfeito.isEmpty()) {
			if (Math.random() <= chance) {
				System.out.println(">>> Efeito On-Hit ativado: " + nomeEfeito);

				if (nomeEfeito.equalsIgnoreCase("Charm")) {
					int carismaAtor = ator.getAtributosFinais()
							.getOrDefault(Atributo.CARISMA, 1);
					int stacksAplicados = Math.max(1, carismaAtor);

					System.out.println(">>> CHARM! " + alvo.getNome() + " recebe " + stacksAplicados
							+ " acúmulos (Baseado em CAR " + carismaAtor + ")");

					Efeito charmEffect = new Efeito("Charm", TipoEfeito.DEBUFF, 9999, null, 0, 0);
					charmEffect.setStacks(stacksAplicados);
					alvo.adicionarEfeito(charmEffect);

					Efeito charmAtual = alvo.getEfeitosAtivos().get("Charm");
					if (charmAtual != null && charmAtual.getStacks() >= 100) {
						aplicarControleMental(alvo, estado);
					}

				} else if (nomeEfeito.equalsIgnoreCase("Choque")) {
					Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Choque", 1, 20);
					efeito.setStacks(1);
					aplicarEfeito(alvo, efeito);
				} else if (nomeEfeito.equalsIgnoreCase("Maldição") || nomeEfeito.equalsIgnoreCase("Maldicao")) {
					aplicarMaldicaoDaArma(alvo, arma);
				} else {
					int danoDaSource = Math.max(1, (int) danoCausado);
					Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito(nomeEfeito, 0, danoDaSource);
					efeito.setStacks(1);
					aplicarEfeito(alvo, efeito);
				}
			}
		}

		if (ator.getEfeitosAtivos().containsKey(BarbaroUtils.EFEITO_BALANCO_TEMERARIO) && Math.random() <= 0.50) {
			Efeito choque = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Choque", 1, 20);
			aplicarEfeito(alvo, choque);
			System.out.println(">>> Balanço Temerário aplicou Choque em " + alvo.getNome() + " (+20 TU no próximo TU).");
		}
	}

	private void processarEfeitoOnHit(Personagem ator, Personagem alvo, Arma arma, double danoCausado,
			EstadoCombate estado, EfeitoOnHit efeitoOnHit) {
		if (efeitoOnHit == null || !efeitoOnHit.isValido() || Math.random() > efeitoOnHit.getChance()) {
			return;
		}

		String nomeEfeito = efeitoOnHit.getNome();
		System.out.println(">>> Efeito On-Hit ativado: " + nomeEfeito);

		if (nomeEfeito.equalsIgnoreCase("Charm")) {
			int carismaAtor = ator.getAtributosFinais()
					.getOrDefault(Atributo.CARISMA, 1);
			int stacksAplicados = Math.max(1, carismaAtor);

			System.out.println(">>> CHARM! " + alvo.getNome() + " recebe " + stacksAplicados
					+ " acÃºmulos (Baseado em CAR " + carismaAtor + ")");

			Efeito charmEffect = new Efeito("Charm", TipoEfeito.DEBUFF, 9999, null, 0, 0);
			charmEffect.setStacks(stacksAplicados);
			alvo.adicionarEfeito(charmEffect);

			Efeito charmAtual = alvo.getEfeitosAtivos().get("Charm");
			if (charmAtual != null && charmAtual.getStacks() >= 100) {
				aplicarControleMental(alvo, estado);
			}
			return;
		}

		if (nomeEfeito.equalsIgnoreCase("Choque")) {
			Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Choque", 1, 20);
			efeito.setStacks(1);
			aplicarEfeito(alvo, efeito);
			return;
		}

		if (nomeEfeito.equalsIgnoreCase("Maldição") || nomeEfeito.equalsIgnoreCase("Maldicao")) {
			aplicarMaldicaoDaArma(alvo, arma);
			return;
		}

		int danoDaSource = Math.max(1, (int) danoCausado);
		Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito(nomeEfeito, 0, danoDaSource);
		efeito.setStacks(1);
		aplicarEfeito(alvo, efeito);
	}

	private void aplicarMaldicaoDaArma(Personagem alvo, Arma arma) {
		String fonte = arma != null ? arma.getNome() : "Arma desconhecida";
		double percentual = obterPercentualMaldicao(fonte);
		Maldicao maldicao = new Maldicao(fonte, percentual, 1000, false);
		MaldicaoUtils.adicionarMaldicao(alvo, maldicao);
		System.out.println(">>> Maldição ativada contra " + alvo.getNome() + " por acerto de " + fonte
				+ " (" + (percentual * 100.0) + "% de redução de vida máxima).");
	}

	private double obterPercentualMaldicao(String nomeArma) {
		if (nomeArma == null) {
			return 0.20;
		}

		return switch (nomeArma) {
		case "Sabre do Kraken" -> 0.075;
		case "Lâminas Gêmeas Maré Alta" -> 0.05;
		case "Dreadnought" -> 0.10;
		default -> 0.20;
		};
	}

	// ========== CONTROLE MENTAL ==========

	public void aplicarControleMental(Personagem alvo, EstadoCombate estado) {
		System.out.println(">>> " + alvo.getNome() + " FOI SEDUZIDO! Trocando de lado por 100 TU.");

alvo.removerEfeito("Charm");

		boolean jaSalvo = alvo.getPropriedades().stream().anyMatch(s -> s.startsWith("ORIGINAL_FACTION:"));
		if (!jaSalvo) {
			alvo.adicionarPropriedade("ORIGINAL_FACTION:" + alvo.getFaccao());
		}

		if ("JOGADOR".equals(alvo.getFaccao())) {
			alvo.setFaccao("INIMIGO");
		} else {
			alvo.setFaccao("JOGADOR");
		}

		Efeito controle = new Efeito("Controle Mental", TipoEfeito.DEBUFF, 100, null, 0, 0);
		alvo.adicionarEfeito(controle);

		getController().atualizarInterfaceTotal();
	}

	// ========== HOOKS DE SISTEMA ==========

	public void processarHooksDeSistema(Personagem ator, Personagem alvo, Arma arma, AcaoMestreInput input,
			double danoTick, EstadoCombate estado, double modHabilidade, boolean isCritico) {

		if (danoTick > 0 && ator.getRaca() != null) {
			ator.getRaca().onDamageDealt(ator, alvo, danoTick, estado, getController());
			if (isCritico) {
				ator.getRaca().onCriticalHit(ator, alvo, estado);
			}
		}
		if (danoTick > 0 && ator.getFantasmaNobre() != null) {
			ator.getFantasmaNobre().onDamageDealt(ator, alvo, danoTick, estado, combatManager);
			if (isCritico) {
				ator.getFantasmaNobre().onCriticalHit(ator, alvo, estado, combatManager);
			}
		}

		int rolagem = input.getResultadoDado("DADO_ATRIBUTO");
		int dadoMax = input.getResultadoDado("DADO_MAX");
		if (rolagem != -1 && dadoMax != -1 && arma != null && danoTick > 0) {
			arma.onRollSuccess(ator, alvo, rolagem, dadoMax, (int) Math.ceil(danoTick), estado);
		}

		// Estado Dourado (Elfo)
		if (ator.getEfeitosAtivos().containsKey("Estado Dourado") && danoTick > 0) {
			aplicarBuffDanoEstadoDourado(ator);
		}

		// Combo (Pugilista)
		if (modHabilidade == 1.0 && ator.getEfeitosAtivos().containsKey("Combo!")) {
			double danoEco = danoTick * 0.30;
			System.out.println(">>> Combo! Eco: +" + String.format("%.1f", danoEco));
			combatManager.getDamageApplicator().aplicarDanoAoAlvo(ator, alvo, danoEco, false, TipoAcao.ECO, estado);
		}

		// Custo de Mana por Hit
		if (modHabilidade == 1.0 && danoTick > 0) {
			double custoMP;
			boolean isCoronhada = input != null && input.getModoAtaque() == ModoAtaque.CORONHADA;
			if (isCoronhada && arma.getManaGainAtaqueAlternativo() >= 0) {
				custoMP = arma.getManaGainAtaqueAlternativo();
			} else {
				custoMP = arma.getTipo().equalsIgnoreCase("Ranged") ? 1.0 : 2.0;
			}
			ator.setManaAtual(ator.getManaAtual() + custoMP);
		}
	}

	// ========== ESTADO DOURADO ==========

	public void aplicarBuffDanoEstadoDourado(Personagem personagem) {
		String nomeEfeito = "Dano Dourado";
		double bonusPorStack = 0.05;
		int duracao = 300;
		Efeito efeitoExistente = personagem.getEfeitosAtivos().get(nomeEfeito);

		if (efeitoExistente != null) {
			double novoBonus = efeitoExistente.getModificadores().getOrDefault("DANO_BONUS_PERCENTUAL", 0.0)
					+ bonusPorStack;
			efeitoExistente.getModificadores().put("DANO_BONUS_PERCENTUAL", novoBonus);
			efeitoExistente.setDuracaoTURestante(duracao);
			efeitoExistente.setStacks(efeitoExistente.getStacks() + 1);
			System.out.println(">>> Dano Dourado acumulado! Stacks: " + efeitoExistente.getStacks() + ". Bônus total: +"
					+ (novoBonus * 100) + "%");
		} else {
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", bonusPorStack);
			Efeito novoBuffDano = new Efeito(nomeEfeito, TipoEfeito.BUFF, duracao, mods, 0, 0);
			novoBuffDano.setStacks(1);
			this.aplicarEfeito(personagem, novoBuffDano);
		}

		personagem.recalcularAtributosEstatisticas();
	}

	// ========== HOOK DE AÇÃO USADA ==========

	public void chamarHookAcaoUsada(Personagem ator, TipoAcao tipoAcaoAtual, EstadoCombate estado) {
		TipoAcao tipoAcaoAnterior = combatManager.getUltimoTipoAcao(ator);

		if (ator.getRaca() != null) {
			ator.getRaca().onActionUsed(ator, tipoAcaoAnterior, tipoAcaoAtual, estado);
		}

		if (ator.getEfeitosAtivos().containsKey("Fimbulwinter")) {
			CombatController controller = combatManager.getMainController();
			if (controller != null) {
				List<Personagem> combatentes = controller.getCombatentes();
				if (combatentes != null) {
					for (Personagem p : combatentes) {
						if (p.isAtivoNoCombate() && p.getFaccao() != null && !p.getFaccao().equals(ator.getFaccao())) {
							if (controller.isPersonagemNoDominio(p, "fimbulwinter_brunhilda")) {
								p.setContadorTU(p.getContadorTU() + 20);
								System.out.println(">>> Fimbulwinter: " + p.getNome() + " ganha +20 TU devido à ação de " + ator.getNome() + ".");
							}
						}
					}
				}
			}
		}

		combatManager.setUltimoTipoAcao(ator, tipoAcaoAtual);
	}

	// ========== MANA PASSIVA MODO JUSTIÇA ==========

	public void verificarManaPassivaModoJustica(Personagem ator, EstadoCombate estado) {
		if (ator.getEfeitosAtivos().containsKey("Modo Justiça"))
			return;

		Personagem darrell = null;
		for (Personagem p : estado.getCombatentes()) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Modo Justiça")) {
				darrell = p;
				break;
			}
		}
		if (darrell == null)
			return;

		if (!ator.getFaccao().equals(darrell.getFaccao()))
			return;

		if (ator.getEfeitosAtivos().containsKey("Bênção da Justiça")) {
			System.out.println(">>> MODO JUSTIÇA (Passiva): Darrell ganha +1 Mana pela ação de " + ator.getNome());
			darrell.setManaAtual(darrell.getManaAtual() + 1);
		}
	}

	// ========== EFEITOS DA HABILIDADE ==========

	public void aplicarEfeitosDaHabilidade(Personagem conjurador, Habilidade habilidade, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {
		AcaoMestreInput lastInput = combatManager.getLastInput();
		if (lastInput != null) {
			habilidade.executar(conjurador, lastInput.getEpicentroX(), lastInput.getEpicentroY(), alvos, estado,
					manager);
		} else {
			habilidade.executar(conjurador, alvos, estado, manager);
		}
	}
}
