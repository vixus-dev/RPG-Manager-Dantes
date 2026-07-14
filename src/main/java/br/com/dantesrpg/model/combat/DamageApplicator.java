package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;

import java.util.List;

/**
 * Responsável por aplicar dano aos alvos com todos os side-effects:
 * escudo (sangue/comum), vampirismo, guardião redirect, morte, XP,
 * explosão, Murasame, hooks raciais, Pesadelo, Stealth evasion.
 */
public class DamageApplicator {

	private final CombatManager combatManager;

	public DamageApplicator(CombatManager combatManager) {
		this.combatManager = combatManager;
	}

	private CombatController getController() {
		return combatManager.getMainController();
	}

	// ========== OVERLOADS PÚBLICOS (COMPATIBILIDADE) ==========

	public void aplicarDanoAoAlvo(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado) {
		aplicarDanoAoAlvoInterno(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, 0, false);
	}

	public void aplicarDanoAoAlvo(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal) {
		aplicarDanoAoAlvoInterno(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, ataqueTotal, false);
	}

	public void aplicarDanoAoAlvoResolvido(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado) {
		aplicarDanoAoAlvoResolvido(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, 0);
	}

	public void aplicarDanoAoAlvoResolvido(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal) {
		aplicarDanoAoAlvoInterno(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, ataqueTotal, true);
	}

	// ========== FANTASMA DO DESERTO ==========

	public boolean aplicarDanoFantasmaDoDeserto(Personagem ator, Personagem alvo, int rolagem1d4, int nivelCascata,
			EstadoCombate estado) {
		if (rolagem1d4 <= 2) {
			System.out.println(">>> Fantasma do Deserto (1d4=" + rolagem1d4 + "): Falha. Cascata interrompida.");
			return false;
		}

		System.out.println(">>> Fantasma do Deserto (1d4=" + rolagem1d4 + "): SUCESSO! (Nível " + nivelCascata + ")");

		if (ator.getArmaEquipada() == null) {
			System.err.println("Erro Fantasma do Deserto: Ator está desarmado.");
			return false;
		}

		double danoCalculado = ator.getArmaEquipada().getDanoBase() * 0.50;
		danoCalculado *= combatManager.getDamageCalculator().getMultiplicadorBonusDanoComArma(
				ator, ator.getArmaEquipada(), alvo, estado, null);
		int danoTick = Math.max(1, (int) danoCalculado);

		aplicarDanoAoAlvo(ator, alvo, danoTick, false, TipoAcao.REACAO_FANTASMA, estado);

		return true;
	}

	// ========== APLICAÇÃO INTERNA DE DANO ==========

	private void aplicarDanoAoAlvoInterno(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal, boolean danoJaResolvido) {
		aplicarDanoAoAlvoInterno(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, ataqueTotal, danoJaResolvido,
				false);
	}

	private void aplicarDanoAoAlvoInterno(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal, boolean danoJaResolvido,
			boolean skipVinculoDano) {
		if (alvo == null || dano <= 0)
			return;

		if ((tipoAcaoDano == TipoAcao.ATAQUE_BASICO || tipoAcaoDano == TipoAcao.HABILIDADE)
				&& ator != null && ator.getEfeitosAtivos().containsKey("Gatilho")) {
			System.out.println(">>> Ataque de " + ator.getNome() + " anulado pelo Gatilho Veloz.");
			return;
		}

		if ((tipoAcaoDano == TipoAcao.ATAQUE_BASICO || tipoAcaoDano == TipoAcao.HABILIDADE)
				&& combatManager.tentarAcionarGatilhoVeloz(alvo, ator, estado)) {
			return;
		}

		// Dormindo: golpe direto conta como hit (2 hits para acordar)
		if (alvo.getEfeitosAtivos().containsKey("Dormindo")) {
			Efeito dormindo = alvo.getEfeitosAtivos().get("Dormindo");
			int hitsRecebidos = dormindo.getStacks() + 1;
			dormindo.setStacks(hitsRecebidos);

			if (hitsRecebidos >= 2) {
				System.out.println(">>> " + alvo.getNome() + " ACORDOU após 2 hits de dano!");
				alvo.removerEfeito("Dormindo");
			} else {
				System.out.println(">>> " + alvo.getNome() + " recebeu dano dormindo ("
						+ hitsRecebidos + "/2 para acordar).");
			}
		}

		// Vínculo de Dano (Vudu)
		if (!skipVinculoDano) {
			for (String prop : alvo.getPropriedades()) {
				if (prop.startsWith("VINCULO_DANO:")) {
					String nomeDono = prop.split(":")[1];

					Personagem dono = estado.getCombatentes().stream()
							.filter(p -> p.getNome().equals(nomeDono) && p.isAtivoNoCombate()).findFirst()
							.orElse(null);

					if (dono != null) {
						double danoTransferido = dano * 0.50;
						if (danoTransferido >= 1.0) {
							System.out.println(
									">>> VUDU: " + danoTransferido + " de dano transferido para " + dono.getNome());
							aplicarDanoAoAlvoInterno(ator, dono, danoTransferido, true, TipoAcao.OUTRO, estado, 0,
									false, true);
						}
					}
				}
			}
		}

		// Fúria
		if (ator != null) {
			int nivelFuria = ator.getValorPropriedade("FURIA");
			if (nivelFuria > 0 && ator.getVidaAtual() <= (ator.getVidaMaxima() * 0.5)) {
				double bonus = 1.0 + (nivelFuria / 100.0);
				dano *= bonus;
				System.out.println(">>> FÚRIA! " + ator.getNome() + " causou dano aumentado em " + nivelFuria + "%.");
			}
		}

		// Stealth (Anula Dano Básico)
		if (alvo.getEfeitosAtivos().containsKey("Stealth") && tipoAcaoDano == TipoAcao.ATAQUE_BASICO) {
			System.out.println(">>> " + alvo.getNome() + " (Stealth) desviou do ataque completamente!");
			alvo.removerEfeito("Stealth");
			return;
		}

		// Sistema de Guardião
		if (tipoAcaoDano == TipoAcao.ATAQUE_BASICO && !alvo.getEfeitosAtivos().containsKey("Guardião")) {
			String faccaoDoAlvo = alvo.getFaccao();
			if (faccaoDoAlvo == null)
				faccaoDoAlvo = "INIMIGO";

			Personagem guardiaoEncontrado = null;
			int menorDistancia = 99;
			int areaGuarda = 3;

			for (Personagem p : estado.getCombatentes()) {
				if (p.getEfeitosAtivos().containsKey("Guardião") && faccaoDoAlvo.equals(p.getFaccao()) && p != alvo && p.isVivo()) {
					int dist = Math.max(Math.abs(p.getPosX() - alvo.getPosX()),
							Math.abs(p.getPosY() - alvo.getPosY()));
					if (dist <= areaGuarda && dist < menorDistancia) {
						menorDistancia = dist;
						guardiaoEncontrado = p;
					}
				}
			}

			if (guardiaoEncontrado != null) {
				System.out.println(">>> [Auto-Guarda] " + guardiaoEncontrado.getNome()
						+ " interceptou o golpe destinado a " + alvo.getNome() + "!");
				alvo = guardiaoEncontrado;

				if (alvo.getEfeitosAtivos().containsKey("Stealth"))
					alvo.removerEfeito("Stealth");
			}
		}

		// Pesadelo
		if (alvo.getEfeitosAtivos().containsKey("Pesadelo")) {
			dano = (int) (dano * 1.60);
			alvo.removerEfeito("Pesadelo");
			System.out.println(">>> PESADELO! " + alvo.getNome() + " acorda e sofre +60% dano!");
		}

		// Redução pré-armadura (se dano ainda não foi resolvido)
		if (!danoJaResolvido) {
			dano = combatManager.getDamageCalculator().aplicarReducaoDanoPreArmadura(dano, ator, alvo, estado);
			if (dano <= 0) {
				return;
			}
		}

		// Vampirismo
		if (ator != null && ator.isVivo() && !ignoraEscudo) {
			int nivelVamp = ator.getValorPropriedade("VAMPIRISMO");
			if (nivelVamp > 0) {
				double curaVamp = dano * (nivelVamp / 100.0);
				if (curaVamp >= 1.0) {
					ator.setVidaAtual(ator.getVidaAtual() + curaVamp, estado, getController());
					System.out.println(">>> VAMPIRISMO: " + ator.getNome() + " curou " + (int) curaVamp + " HP.");
				}
			}
		}

		// ========== ESCUDOS ==========
		// Ordem: Escudo de Sangue → Escudo Normal → Vida
		// - Sangue: "esponja de dano", ignora TODA redução (armadura, topor, etc.)
		// - Normal: recebe o dano já reduzido pelos modificadores do alvo
		double danoRestante = dano;

		if (!ignoraEscudo) {
			// --- ESCUDO INFERNAL (prioridade máxima, recebe o dano já reduzido) ---
			double escudoInfernal = alvo.getEscudoInfernalAtual();
			if (escudoInfernal > 0) {
				if (danoRestante >= escudoInfernal) {
					danoRestante -= escudoInfernal;
					alvo.setEscudoInfernalAtual(0);
					System.out.println(">>> Escudo Infernal QUEBROU! Dano residual: " + (int) danoRestante);
				} else {
					alvo.setEscudoInfernalAtual(escudoInfernal - danoRestante);
					danoRestante = 0;
				}
			}

			// --- ESCUDO DIVINO (recebe o dano já reduzido) ---
			double escudoDivino = alvo.getEscudoDivinoAtual();
			if (danoRestante > 0 && escudoDivino > 0) {
				if (danoRestante >= escudoDivino) {
					danoRestante -= escudoDivino;
					alvo.setEscudoDivinoAtual(0);
					System.out.println(">>> Escudo Divino QUEBROU! Dano residual: " + (int) danoRestante);
				} else {
					alvo.setEscudoDivinoAtual(escudoDivino - danoRestante);
					danoRestante = 0;
				}
			}

			// --- ESCUDO DE SANGUE (segundo, ignora reduções) ---
			double escudoSangue = alvo.getEscudoSangueAtual();
			if (danoRestante > 0 && escudoSangue > 0) {
				// Reverte a redução que já foi aplicada em aplicarReducaoDanoPreArmadura
				// para obter o valor bruto que o sangue deve absorver.
				double reducaoArmadura = alvo.getReducaoDanoArmadura() + alvo.getReducaoDanoTopor();
				reducaoArmadura = Math.min(reducaoArmadura, 0.90);
				if (alvo.getEfeitosAtivos().containsKey("Ruptura"))
					reducaoArmadura -= 0.25;
				double fatorReducao = Math.max(0.05, 1.0 - reducaoArmadura);

				double danoBrutoContraSangue = danoRestante / fatorReducao;

				System.out.println(">>> ESCUDO DE SANGUE: Ignorou armadura! Dano " + (int) danoRestante
						+ " → bruto " + (int) danoBrutoContraSangue + " (escudo: " + (int) escudoSangue + ")");

				if (danoBrutoContraSangue >= escudoSangue) {
					// Sangue quebra; converte o excedente bruto de volta em líquido
					double excedenteBruto = danoBrutoContraSangue - escudoSangue;
					danoRestante = excedenteBruto * fatorReducao;
					alvo.setEscudoSangueAtual(0);
					System.out.println(">>> Escudo de Sangue QUEBROU! Dano residual: " + (int) danoRestante);
				} else {
					alvo.setEscudoSangueAtual(escudoSangue - danoBrutoContraSangue);
					danoRestante = 0;
				}
			}

			// --- ESCUDO NORMAL (recebe dano já reduzido) ---
			double escudoNormal = alvo.getEscudoNormalAtual();
			if (danoRestante > 0 && escudoNormal > 0) {
				if (danoRestante >= escudoNormal) {
					danoRestante -= escudoNormal;
					alvo.setEscudoNormalAtual(0);
					System.out.println(">>> Escudo normal QUEBROU! Dano residual: " + (int) danoRestante);
				} else {
					alvo.setEscudoNormalAtual(escudoNormal - danoRestante);
					danoRestante = 0;
				}
			}
		}

		if (danoRestante > 0) {
			double vidaPre = alvo.getVidaAtual();

			// CONTRATO DE VIDA: se o alvo tem contratos acumulados >= 100% da vida
			// base, qualquer dano que chegue a HP (depois de escudos) é letal.
			double vidaNova;
			if (br.com.dantesrpg.model.util.ContratoDeVidaUtils.estaSobrecarregado(alvo)) {
				vidaNova = 0;
				System.out.println(">>> CONTRATO LETAL: " + alvo.getNome()
						+ " tem 100% da vida comprometida em contratos — dano letal!");

} else {
				vidaNova = vidaPre - danoRestante;
			}

			alvo.setVidaAtual(vidaNova, estado, getController());
			double danoReal = vidaPre - alvo.getVidaAtual();

			alvo.registrarDanoSofrido(danoReal, estado.getTickCounter());

			System.out.println(
					">>> DANO APLICADO: " + alvo.getNome() + " -" + String.format("%.1f", danoReal) + " HP.");

// ========== EMPUXO (KNOCKBACK) ==========
			// Executado após o dano ser registrado, mas antes da checagem de morte,
			// para que a posição final do alvo reflita o combate mesmo se ele morrer.
			if (ator != null && alvo.isVivo()) {
				Habilidade habilidadeAtiva = combatManager.getLastInput() != null
						? combatManager.getLastInput().getHabilidade()
						: null;
				int forcaEmpuxo = (habilidadeAtiva != null) ? habilidadeAtiva.getForcaEmpuxo() : 0;

				if (forcaEmpuxo > 0) {
					MapController mapController = getController() != null
							? getController().getMapController()
							: null;
					KnockbackProcessor kp = combatManager.getKnockbackProcessor();
					KnockbackResult resultado = kp.calcularEmpuxo(ator, alvo, forcaEmpuxo, mapController);

					if (resultado.houveMomento()) {
						kp.executarEmpuxo(alvo, resultado);

						// Dano de impacto (colisão com parede/borda)
						if (resultado.isColidiu() && resultado.getDanoImpacto() > 0
								&& resultado.getColidiuCom() == null) {
							double danoImpacto = resultado.getDanoImpacto();
							System.out.printf(">>> IMPACTO: %s colidiu com parede, sofrendo %.1f de dano!%n",
									alvo.getNome(), danoImpacto);
							aplicarDanoAoAlvo(null, alvo, danoImpacto, true, TipoAcao.AMBIENTE, estado);
						}
					}
				}
			}
			// ========================================================

			// MORTE
			if (vidaPre > 0 && !alvo.isAtivoNoCombate() && !alvo.isVivo()) {
				// Vínculo de Cura na Morte
				for (String prop : alvo.getPropriedades()) {
					if (prop.startsWith("VINCULO_CURA_MORTE:")) {
						String nomeDono = prop.split(":")[1];
						Personagem dono = estado.getCombatentes().stream()
								.filter(p -> p.getNome().equals(nomeDono))
								.findFirst().orElse(null);

						if (dono != null) {
							double cura = dono.getVidaMaxima() * 0.05;
							dono.setVidaAtual(dono.getVidaAtual() + cura, estado, getController());
							System.out.println(">>> RETORNO VITAL: " + dono.getNome() + " curou " + (int) cura
									+ " HP com a destruição da essência.");
						}
					}
				}

				// XP
				if (alvo.getXpReward() > 0)
					estado.adicionarXpAoPool(alvo.getXpReward());

				// Hook de kill para Raça
				if (ator != null && ator.getRaca() != null) {
					ator.getRaca().onKill(ator, alvo, estado, combatManager);
				}

				// Explosivo
				int nivelExplosivo = alvo.getValorPropriedade("EXPLOSIVO");
				if (nivelExplosivo > 0) {
					System.out.println(">>> PROPRIEDADE ATIVADA: " + alvo.getNome() + " EXPLODIU!");

					int danoExplosao = (int) (alvo.getVidaMaximaBase() * 0.50 * nivelExplosivo);

					Habilidade explosaoDummy = new Habilidade("Explosão", "Dano ao morrer", null, 0, 0, 0,
							TipoAlvo.AREA_QUADRADA, 3, 0, 0, null) {
						@Override
						public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
								CombatManager manager) {
						}
					};

					List<Personagem> vitimas = getController().getMapController().encontrarAlvosNaForma(
							alvo.getPosX(), alvo.getPosY(), explosaoDummy, alvo);

					for (Personagem v : vitimas) {
						if (v.isAtivoNoCombate()) {
							aplicarDanoAoAlvo(alvo, v, danoExplosao, false, TipoAcao.OUTRO, estado);
						}
					}
				}

				// Explodir em Chamas
				if (alvo.getValorPropriedade("EXPLODIR") > 0 || alvo.getNome().contains("Larva")) {
					System.out.println(">>> " + alvo.getNome() + " EXPLODIU EM CHAMAS!");

if (getController() != null && getController().getMapController() != null) {
						getController().getMapController().criarAreaDeFogo(alvo.getPosX(), alvo.getPosY(), 1, 8, alvo);
					}
				}

				// Murasame
				if (ator != null && ator.possuiArmaEquipada("Murasame")) {
					System.out.println(">>> MURASAME (KILL): Alma capturada!");
					ator.getInventario().adicionarItem(new br.com.dantesrpg.model.items.EssenciaInimigo(alvo));

if (getController() != null)
						getController().atualizarInterfaceTotal();
				}

				// Clone Morrendo → Stealth no Criador
				if (alvo.isClone()) {
					combatManager.processarMorteClone(alvo, estado);
				}

				// Objeto Destrutível
				if (alvo instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
					System.out.println(">>> ESTRUTURA DESTRUÍDA: " + alvo.getNome());
					if (getController() != null && getController().getMapController() != null) {
						getController().getMapController().atualizarCelulaParaChao(alvo.getPosX(), alvo.getPosY());
					}
				}

				if (getController() != null)
					getController().atualizarInterfaceAposMorte();
			}

			// Hooks de Reação
			if (alvo.getRaca() != null) {
				alvo.getRaca().onDamageTaken(alvo, ator, danoReal, estado, combatManager);
			}
			if (alvo.getArmaEquipada() != null) {
				alvo.getArmaEquipada().onDamageTaken(alvo, danoReal, estado, getController());
			}
			if (alvo.getArmaduraEquipada() != null) {
				alvo.getArmaduraEquipada().onDamageTaken(alvo, ator, danoReal, estado, getController());
			}

			// Falsa Justiça (Justiceiro Cego) — reflexão de dano
			if (alvo.getEfeitosAtivos().containsKey("Falsa Justiça") && ator != null
					&& ator != alvo && ator.isAtivoNoCombate()) {
				double percentualDano = danoReal / alvo.getVidaMaxima();
				double danoRefletido = ator.getVidaMaxima() * percentualDano;
				System.out.println(">>> FALSA JUSTIÇA: " + ator.getNome() + " recebe "
						+ (int) danoRefletido + " de dano refletido! ("
						+ String.format("%.1f", percentualDano * 100) + "% da vida máxima)");
				ator.setVidaAtual(Math.max(0, ator.getVidaAtual() - danoRefletido));
				ator.recalcularAtributosEstatisticas();
			}

			// Marca do Deserto
			if (alvo.getEfeitosAtivos().containsKey("Marca do Deserto") && getController() != null
					&& tipoAcaoDano != TipoAcao.REACAO_FANTASMA) {
				if (ator != alvo) {
					getController().solicitarRolagemFantasmaDoDeserto(ator, alvo);
				}
			}
		}
	}
}
