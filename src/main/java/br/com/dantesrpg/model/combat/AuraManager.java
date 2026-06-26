package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.classes.Campeao;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.fantasmasnobres.TheMastersCall;
import br.com.dantesrpg.model.map.Dominio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsável por auras posicionais (Bênção da Justiça, Vazio do Zeraphon,
 * Vínculo de Selo), passiva do Campeão e efeitos de início de turno
 * (Ringue da Vontade, Idle Death Gamble).
 */
public class AuraManager {

	private final CombatManager combatManager;

	public AuraManager(CombatManager combatManager) {
		this.combatManager = combatManager;
	}

	private CombatController getController() {
		return combatManager.getMainController();
	}

	// ========== ATUALIZAÇÃO DE AURAS ==========

	public void atualizarAuras(EstadoCombate estado) {
		// --- BÊNÇÃO DA JUSTIÇA (Darrell) ---
		Personagem darrell = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Modo Justiça")).findFirst()
				.orElse(null);

		if (darrell != null) {
			int raio = 3;
			int cx = darrell.getPosX();
			int cy = darrell.getPosY();
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", 0.15);
			mods.put("REDUCAO_DANO_MODIFICADOR", 0.05);

			for (Personagem p : estado.getCombatentes()) {
				if (p.isAtivoNoCombate() && p.getFaccao().equals(darrell.getFaccao()) && !p.equals(darrell)) {
					int dist = Math.max(Math.abs(p.getPosX() - cx), Math.abs(p.getPosY() - cy));
					boolean temBuff = p.getEfeitosAtivos().containsKey("Bênção da Justiça");
					if (dist <= raio && !temBuff) {
						p.adicionarEfeito(new Efeito("Bênção da Justiça", TipoEfeito.BUFF, 999, mods, 0, 0));
						p.recalcularAtributosEstatisticas();
					} else if (dist > raio && temBuff) {
						p.removerEfeito("Bênção da Justiça");
						p.recalcularAtributosEstatisticas();
					}
				}
			}
		} else {
			for (Personagem p : estado.getCombatentes()) {
				if (p.getEfeitosAtivos().containsKey("Bênção da Justiça")) {
					p.removerEfeito("Bênção da Justiça");
					p.recalcularAtributosEstatisticas();
				}
			}
		}

		// --- AURA DO ZERAPHON (O ZERO) ---
		Personagem zeraphon = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Aura do Zero")).findFirst()
				.orElse(null);

		if (zeraphon != null) {
			int raioZero = 3;
			int zx = zeraphon.getPosX();
			int zy = zeraphon.getPosY();

			Map<String, Double> modsZero = new HashMap<>();
			modsZero.put("REDUCAO_DANO_MODIFICADOR", -0.30);
			modsZero.put("DANO_BONUS_PERCENTUAL", -0.50);

			for (Personagem p : estado.getCombatentes()) {
				if (p.isAtivoNoCombate() && !p.getFaccao().equals(zeraphon.getFaccao())) {
					int dist = Math.max(Math.abs(p.getPosX() - zx), Math.abs(p.getPosY() - zy));
					boolean temDebuff = p.getEfeitosAtivos().containsKey("O Vazio");

					if (dist <= raioZero && !temDebuff) {
						System.out.println(">>> " + p.getNome() + " entrou no VAZIO de Zeraphon.");
						p.adicionarEfeito(new Efeito("O Vazio", TipoEfeito.DEBUFF, 999, modsZero, 0, 0));
						p.recalcularAtributosEstatisticas();
					} else if (dist > raioZero && temDebuff) {
						System.out.println(">>> " + p.getNome() + " saiu do VAZIO.");
						p.removerEfeito("O Vazio");
						p.recalcularAtributosEstatisticas();
					}
				}
			}
		} else {
			for (Personagem p : estado.getCombatentes()) {
				if (p.getEfeitosAtivos().containsKey("O Vazio")) {
					p.removerEfeito("O Vazio");
					p.recalcularAtributosEstatisticas();
				}
			}
		}

		// --- AURA DE SANGUE (Lillith — Mergulho) ---
		Personagem lillith = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Aura de Sangue")).findFirst()
				.orElse(null);

		if (lillith != null) {
			int raioLilith = 3;
			int lx = lillith.getPosX();
			int ly = lillith.getPosY();

			Map<String, Double> modsIntoxicado = new HashMap<>();
			modsIntoxicado.put("REDUCAO_DANO_MODIFICADOR", -0.30);

			for (Personagem p : estado.getCombatentes()) {
				if (!p.isAtivoNoCombate())
					continue;

				int dist = Math.max(Math.abs(p.getPosX() - lx), Math.abs(p.getPosY() - ly));
				boolean isAliado = p.getFaccao() != null && p.getFaccao().equals(lillith.getFaccao());

				if (isAliado) {
					boolean temCoagulacao = p.getEfeitosAtivos().containsKey("Coagulação");
					if (dist <= raioLilith && !temCoagulacao) {
						Efeito coagulacao = new Efeito("Coagulação", TipoEfeito.BUFF, 9999, null, 0, 120);
						p.adicionarEfeito(coagulacao);
						p.recalcularAtributosEstatisticas();
						System.out.println(">>> " + p.getNome() + " entrou na Aura de Sangue (Coagulação).");
					} else if (dist > raioLilith && temCoagulacao) {
						p.removerEfeito("Coagulação");
						p.recalcularAtributosEstatisticas();
						System.out.println(">>> " + p.getNome() + " saiu da Aura de Sangue.");
					}
				} else {
					boolean temIntoxicado = p.getEfeitosAtivos().containsKey("Intoxicado");
					if (dist <= raioLilith && !temIntoxicado) {
						Efeito intoxicado = new Efeito("Intoxicado", TipoEfeito.DEBUFF, 9999, modsIntoxicado, 0, 0);
						p.adicionarEfeito(intoxicado);
						p.recalcularAtributosEstatisticas();
						System.out.println(">>> " + p.getNome() + " foi Intoxicado pela Aura de Sangue.");
					} else if (dist > raioLilith && temIntoxicado) {
						p.removerEfeito("Intoxicado");
						p.recalcularAtributosEstatisticas();
						System.out.println(">>> " + p.getNome() + " saiu da Aura de Sangue (Intoxicado removido).");
					}
				}
			}
		} else {
			for (Personagem p : estado.getCombatentes()) {
				if (p.getEfeitosAtivos().containsKey("Coagulação")) {
					p.removerEfeito("Coagulação");
					p.recalcularAtributosEstatisticas();
				}
				if (p.getEfeitosAtivos().containsKey("Intoxicado")) {
					p.removerEfeito("Intoxicado");
					p.recalcularAtributosEstatisticas();
				}
			}
		}

		atualizarTheMastersCall(estado);

		// --- DOMÍNIO JIHŌ GEKKYŪDEN (Lillith) — efeitos por tile ---
		if (getController() != null) {
			Dominio jiho = getController().getDominio("jiho_lillith");
			if (jiho != null) {
				Personagem donoJiho = jiho.getDono();
				String faccaoDono = (donoJiho != null) ? donoJiho.getFaccao() : null;

				for (Personagem p : estado.getCombatentes()) {
					if (!p.isAtivoNoCombate())
						continue;

					boolean dentro = jiho.contemPersonagem(p);
					boolean isAliado = faccaoDono != null && faccaoDono.equals(p.getFaccao());

					if (isAliado) {
						boolean temBuff = p.getEfeitosAtivos().containsKey("Benção do Gekkyūden");
						if (dentro && !temBuff) {
							Map<String, Double> modsBuff = new HashMap<>();
							modsBuff.put("DANO_BONUS_PERCENTUAL", 0.50);
							// intervaloTickTU=5 → tick custom no CombatManager gera +1 escudo de sangue.
							Efeito buff = new Efeito("Benção do Gekkyūden", TipoEfeito.BUFF, 9999, modsBuff, 0, 5);
							p.adicionarEfeito(buff);
							p.recalcularAtributosEstatisticas();
							System.out.println(">>> " + p.getNome() + " recebe Benção do Gekkyūden.");
						} else if (!dentro && temBuff) {
							p.removerEfeito("Benção do Gekkyūden");
							p.recalcularAtributosEstatisticas();
							System.out.println(">>> " + p.getNome() + " saiu do Jihō Gekkyūden (Benção removida).");
						}
					} else {
						boolean temDebuff = p.getEfeitosAtivos().containsKey("Maldição do Gekkyūden");
						if (dentro && !temDebuff) {
							Map<String, Double> modsDebuff = new HashMap<>();
							modsDebuff.put("MOVIMENTO", -2.0);
							// danoPorTick=1, intervaloTickTU=20 → tick custom no CombatManager causa 1 dano.
							Efeito debuff = new Efeito("Maldição do Gekkyūden", TipoEfeito.DEBUFF, 9999, modsDebuff, 1, 20);
							p.adicionarEfeito(debuff);
							p.recalcularAtributosEstatisticas();
							System.out.println(">>> " + p.getNome() + " recebe Maldição do Gekkyūden.");
						} else if (!dentro && temDebuff) {
							p.removerEfeito("Maldição do Gekkyūden");
							p.recalcularAtributosEstatisticas();
							System.out.println(">>> " + p.getNome() + " saiu do Jihō Gekkyūden (Maldição removida).");
						}
					}
				}
			} else {
				for (Personagem p : estado.getCombatentes()) {
					if (p.getEfeitosAtivos().containsKey("Benção do Gekkyūden")) {
						p.removerEfeito("Benção do Gekkyūden");
						p.recalcularAtributosEstatisticas();
					}
					if (p.getEfeitosAtivos().containsKey("Maldição do Gekkyūden")) {
						p.removerEfeito("Maldição do Gekkyūden");
						p.recalcularAtributosEstatisticas();
					}
				}
			}
		}

		// --- AURA DO INEFÁVEL SOL (Escanor / Sol) ---
		Personagem sol = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && p.getPropriedades().stream().anyMatch(prop -> prop.startsWith("AURA_INEFAVEL_SOL:")))
				.findFirst()
				.orElse(null);

		double totalEscudoInfernalGlobal = estado.getCombatentes().stream()
				.filter(Personagem::isAtivoNoCombate)
				.mapToDouble(Personagem::getEscudoInfernalAtual)
				.sum();

		if (sol != null) {
			int sx = sol.getPosX() + (sol.getTamanhoX() / 2);
			int sy = sol.getPosY() + (sol.getTamanhoY() / 2);

			String nomeConjurador = sol.getPropriedades().stream()
					.filter(prop -> prop.startsWith("AURA_INEFAVEL_SOL:"))
					.findFirst()
					.map(prop -> prop.split(":")[1].trim())
					.orElse("");

			Personagem conjurador = estado.getCombatentes().stream().filter(p -> p.getNome().equals(nomeConjurador)).findFirst().orElse(null);
			String faccaoSol = conjurador != null ? conjurador.getFaccao() : sol.getFaccao();
			boolean ondaAtiva = sol.getEfeitosAtivos().containsKey("Onda de Calor (Ativa)");

			Dominio dominioSol = getController() != null ? getController().getDominio("heatWave_" + sol.getNome()) : null;

			for (Personagem p : estado.getCombatentes()) {
				if (!p.isAtivoNoCombate() || p.equals(sol)) continue;

				int dist = Math.max(Math.abs(p.getPosX() - sx), Math.abs(p.getPosY() - sy));
				boolean dentro = (dominioSol != null) ? dominioSol.contemPersonagem(p) : (dist <= 3);

				boolean temQueimadura = p.getEfeitosAtivos().containsKey("Queimadura Inefável");
				boolean temMeioDia = p.getEfeitosAtivos().containsKey("Meio Dia");

				if (dentro && ondaAtiva) {
					if (p.getNome().equals("Escanor") || (conjurador != null && p.equals(conjurador))) {
						if (!temMeioDia) {
							Efeito meioDia = new Efeito("Meio Dia", TipoEfeito.BUFF, 9999, new HashMap<>(), 0, 0);
							p.adicionarEfeito(meioDia);
							temMeioDia = true;
							System.out.println(">>> " + p.getNome() + " recebe a benção do Meio Dia.");
						}
						if (temMeioDia) {
							Efeito meioDia = p.getEfeitosAtivos().get("Meio Dia");
							if (meioDia != null && meioDia.getModificadores() != null) {
								meioDia.getModificadores().put("DANO_BONUS_PERCENTUAL", 0.05 * totalEscudoInfernalGlobal);
							}
							p.recalcularAtributosEstatisticas();
						}
					} else {
						// Comparação de facção segura contra nulos
						boolean mesmaFaccao = (p.getFaccao() == null && faccaoSol == null) 
								|| (p.getFaccao() != null && p.getFaccao().equals(faccaoSol));
						if (!mesmaFaccao) {
							if (!temQueimadura) {
								Efeito queimadura = new Efeito("Queimadura Inefável", TipoEfeito.DEBUFF, 9999, new HashMap<>(), 0, 20);
								p.adicionarEfeito(queimadura);
								p.recalcularAtributosEstatisticas();
								System.out.println(">>> " + p.getNome() + " está queimando pela presença do Sol.");
							}
						}
					}
				} else {
					if (temMeioDia) {
						p.removerEfeito("Meio Dia");
						p.recalcularAtributosEstatisticas();
					}
					if (temQueimadura) {
						p.removerEfeito("Queimadura Inefável");
						p.recalcularAtributosEstatisticas();
					}
				}
			}
		} else {
			for (Personagem p : estado.getCombatentes()) {
				if (p.getEfeitosAtivos().containsKey("Meio Dia")) {
					p.removerEfeito("Meio Dia");
					p.recalcularAtributosEstatisticas();
				}
				if (p.getEfeitosAtivos().containsKey("Queimadura Inefável")) {
					p.removerEfeito("Queimadura Inefável");
					p.recalcularAtributosEstatisticas();
				}
			}
		}

		// Limpeza robusta de domínios visuais de Onda de Calor órfãos ou expirados (Sol inativo ou sem efeito ativo)
		if (getController() != null) {
			List<String> dominiosARemover = new ArrayList<>();
			List<String> chavesAtivas = new ArrayList<>(getController().getDominiosAtivos().keySet());
			for (String idDom : chavesAtivas) {
				if (idDom.startsWith("heatWave_")) {
					Personagem solDono = estado.getCombatentes().stream()
							.filter(p -> p.isAtivoNoCombate() && idDom.equals("heatWave_" + p.getNome()))
							.findFirst()
							.orElse(null);
					if (solDono == null || !solDono.getEfeitosAtivos().containsKey("Onda de Calor (Ativa)")) {
						dominiosARemover.add(idDom);
					}
				}
			}
			for (String idDom : dominiosARemover) {
				System.out.println("MAPA: Limpando domínio [" + idDom + "] (Sol inativo ou Onda de Calor expirada).");
				getController().removerDominio(idDom);
			}
		}

		// --- VÍNCULO DE SELO ---
		Map<Personagem, Integer> contagemSelos = new HashMap<>();

		for (Personagem p : estado.getCombatentes()) {
			if (p.isAtivoNoCombate()) {
				for (String prop : p.getPropriedades()) {
					if (prop.startsWith("PORTADOR_SELO:")) {
						String nomeMestre = prop.split(":")[1].trim();

						Personagem mestre = estado.getCombatentes().stream()
								.filter(m -> m.getNome().trim().equalsIgnoreCase(nomeMestre)).findFirst().orElse(null);

						if (mestre != null) {
							contagemSelos.put(mestre, contagemSelos.getOrDefault(mestre, 0) + 1);
						}
					}
				}
			}
		}

		for (Personagem mestre : estado.getCombatentes()) {
			int qtdSelos = contagemSelos.getOrDefault(mestre, 0);
			String keyBuff = "Vínculo de Selo";

			Efeito efeitoExistente = mestre.getEfeitosAtivos().get(keyBuff);

			if (qtdSelos > 0) {
				if (efeitoExistente == null) {
					Map<String, Double> mods = new HashMap<>();
					mods.put("MP_MAXIMO", (double) qtdSelos);
					Efeito novoBuff = new Efeito(keyBuff, TipoEfeito.BUFF, 9999, mods, 0, 0);
					novoBuff.setStacks(qtdSelos);
					mestre.adicionarEfeito(novoBuff);
				} else {
					if (efeitoExistente.getStacks() != qtdSelos) {
						efeitoExistente.setStacks(qtdSelos);
						efeitoExistente.getModificadores().put("MP_MAXIMO", (double) qtdSelos);
						mestre.recalcularAtributosEstatisticas();
						System.out.println(">>> SELOS: Atualizado para " + qtdSelos);
					}
				}
			} else if (efeitoExistente != null) {
				mestre.removerEfeito(keyBuff);
			}
		}
	}

	// ========== EFEITOS DE INÍCIO DE TURNO ==========

	public void checarEfeitosDeInicioDeTurno(Personagem ator, EstadoCombate estado) {
		if (ator.getRaca() != null) {
			ator.getRaca().onTurnStart(ator, estado);
		}
		if (ator.getFantasmaNobre() != null) {
			ator.getFantasmaNobre().onTurnStart(ator, estado, combatManager);
		}

		processarTurnoTheMastersCall(ator, estado);

		// --- PROTEÇÃO DOS CÉUS (Sarvant / YAWEH) ---
		// Se Despertar Divino estiver ativo, a lógica do escudo é controlada pelo FN (onTurnStart)
		if (ator.getEfeitosAtivos().containsKey("Proteção dos Céus")
				&& !ator.getEfeitosAtivos().containsKey("Despertar Divino")) {
			Efeito efeito = ator.getEfeitosAtivos().get("Proteção dos Céus");
			double currentShield = ator.getEscudoDivinoAtual();
			int lastShield = 20; // fallback padrão
			String propKey = "ProtecaoCeusLastHP:";
			for (String prop : ator.getPropriedades()) {
				if (prop.startsWith(propKey)) {
					try {
						lastShield = Integer.parseInt(prop.substring(propKey.length()));
					} catch (Exception e) {}
					break;
				}
			}

			if (currentShield == lastShield) {
				// Se o HP do escudo não mudou, aumenta a taxa crítica em 5% cumulativamente
				Map<String, Double> mods = efeito.getModificadores();
				if (mods == null) {
					mods = new HashMap<>();
					efeito.setModificadores(mods);
				} else if (!(mods instanceof HashMap)) {
					mods = new HashMap<>(mods);
					efeito.setModificadores(mods);
				}
				double currentCrit = mods.getOrDefault("TAXA_CRITICA", 0.0);
				mods.put("TAXA_CRITICA", currentCrit + 0.05);
				System.out.println(">>> Proteção dos Céus: Escudo intacto! Taxa Crítica acumulada: +" + (int)((currentCrit + 0.05) * 100) + "%");
			} else {
				// Se o HP do escudo diminuiu, restaura o valor para 20
				ator.setEscudoDivinoMaximo(20.0);
				ator.setEscudoDivinoAtual(20.0);
				currentShield = 20.0;
				System.out.println(">>> Proteção dos Céus: Escudo danificado! Restaurado para 20 HP.");
			}

			// Atualiza a propriedade com o novo valor do escudo
			ator.getPropriedades().removeIf(prop -> prop.startsWith(propKey));
			ator.adicionarPropriedade(propKey + (int) currentShield);
			ator.recalcularAtributosEstatisticas();
		}

		// Ringue da Vontade (Alexei)
		if (ator.getEfeitosAtivos().containsKey("Ringue (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'O Ringue da Vontade Inquebrantável'!");

			ator.removerEfeito("Ringue (Preparando)");

			Efeito efeitoRingue = new Efeito("Ringue da Vontade",
					TipoEfeito.BUFF, 400, Map.of(), 0, 0);
			combatManager.getEffectProcessor().aplicarEfeito(ator, efeitoRingue);

			if (getController() != null) {
				Dominio ringue = new Dominio("ringue_alexei", "Ringue da Vontade", ator,
						ator.getPosX(), ator.getPosY(), 7, "zona-dominio-alexei");
				combatManager.getDomainManager().ativarDominioNoMapa(ringue, ator, estado);
			}
		}

		// Domínio Idle Death Gamble (Lyria)
		if (ator.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'Idle Death Gamble'!");

			ator.removerEfeito("Domínio: Idle Death Gamble (Preparando)");

			br.com.dantesrpg.model.fantasmasnobres.ApostadorIncansavel.ativarEfeitoDominio(ator);

			if (getController() != null) {
				Dominio dominio = new Dominio("dominio_lyria", "Domínio: Idle Death Gamble", ator,
						ator.getPosX(), ator.getPosY(), 7, "zona-dominio-lyria");
				combatManager.getDomainManager().ativarDominioNoMapa(dominio, ator, estado);
			}
		}

		// Falsa Justiça (Justiceiro Cego)
		if (ator.getEfeitosAtivos().containsKey("Falsa Justiça (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'And Justice.. FOR MY SELF!'");

			ator.removerEfeito("Falsa Justiça (Preparando)");
			br.com.dantesrpg.model.fantasmasnobres.AndJusticeForMySelf.ativarAura(ator, estado, combatManager);
		}

		// Domínio Jihō Gekkyūden (Lillith)
		if (ator.getEfeitosAtivos().containsKey("Jihō Gekkyūden (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'Jihō Gekkyūden'!");

			ator.removerEfeito("Jihō Gekkyūden (Preparando)");

			// Marcador de duração do domínio (500 TU). Quando expira, o CombatManager
			// remove o domínio do mapa e aplica a exaustão.
			Efeito dominioMarker = new Efeito("Jihō Gekkyūden", TipoEfeito.BUFF, 500, Map.of(), 0, 0);
			combatManager.getEffectProcessor().aplicarEfeito(ator, dominioMarker);

			if (getController() != null) {
				Dominio jiho = new Dominio("jiho_lillith", "Jihō Gekkyūden", ator,
						ator.getPosX(), ator.getPosY(), 7, "zona-dominio-lillith");
				jiho.setTexturePath("/effects/sangue_negro.png");
				combatManager.getDomainManager().ativarDominioNoMapa(jiho, ator, estado);
			}
		}

		// Domínio Lua Sombria (Lua Profana)
		if (ator.getEfeitosAtivos().containsKey("Lua Sombria (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'Lua Sombria'!");

			ator.removerEfeito("Lua Sombria (Preparando)");

			// Efeito "Lua Sombria" dura 1000 TU.
			Efeito dominioMarker = new Efeito("Lua Sombria", TipoEfeito.BUFF, 1000, Map.of(), 0, 0);
			combatManager.getEffectProcessor().aplicarEfeito(ator, dominioMarker);

			if (getController() != null) {
				Dominio luaSombria = new Dominio("lua_sombria", "Lua Sombria", ator,
						ator.getPosX(), ator.getPosY(), 7, "zona-dominio-luasombria");
				luaSombria.setTexturePath("/effects/sombra.png");
				combatManager.getDomainManager().ativarDominioNoMapa(luaSombria, ator, estado);
			}
		}

		// Passiva do Campeão
		if (ator.getClasse() instanceof Campeao) {
			verificarPassivaCampeao(ator, estado);
		}
	}

	private void atualizarTheMastersCall(EstadoCombate estado) {
		Personagem conjuradorCrash = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_CRASH))
				.findFirst()
				.orElse(null);

		if (conjuradorCrash == null) {
			removerGotLuckySemAura(estado);
			return;
		}

		int raio = TheMastersCall.calcularRaio(conjuradorCrash);
		int cx = conjuradorCrash.getPosX();
		int cy = conjuradorCrash.getPosY();
		int duracaoRestante = conjuradorCrash.getEfeitosAtivos().get(TheMastersCall.EFEITO_CRASH)
				.getDuracaoTURestante();

		for (Personagem p : estado.getCombatentes()) {
			if (!p.isAtivoNoCombate()) {
				continue;
			}

			boolean aliado = mesmaFaccao(p, conjuradorCrash);
			int distancia = calcularDistanciaQuadrada(p, cx, cy);
			boolean dentro = aliado && distancia <= raio;
			boolean temGotLucky = p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_GOT_LUCKY);

			if (dentro && !temGotLucky) {
				Efeito gotLucky = new Efeito(TheMastersCall.EFEITO_GOT_LUCKY, TipoEfeito.BUFF,
						Math.max(1, duracaoRestante), Map.of(), 0, 0);
				p.adicionarEfeito(gotLucky);
				p.recalcularAtributosEstatisticas();
				System.out.println(">>> " + p.getNome() + " recebeu Got Lucky.");
			} else if (!dentro && temGotLucky) {
				p.removerEfeito(TheMastersCall.EFEITO_GOT_LUCKY);
				p.recalcularAtributosEstatisticas();
				System.out.println(">>> " + p.getNome() + " saiu da aura Crash of Worlds.");
			}
		}
	}

	private void removerGotLuckySemAura(EstadoCombate estado) {
		for (Personagem p : estado.getCombatentes()) {
			if (p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_GOT_LUCKY)) {
				p.removerEfeito(TheMastersCall.EFEITO_GOT_LUCKY);
				p.recalcularAtributosEstatisticas();
			}
		}
	}

	private void processarTurnoTheMastersCall(Personagem ator, EstadoCombate estado) {
		if (ator == null || !ator.isAtivoNoCombate()) {
			return;
		}

		for (Personagem conjurador : estado.getCombatentes()) {
			if (!conjurador.isAtivoNoCombate()
					|| !conjurador.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_ECSTASY)
					|| !mesmaFaccao(ator, conjurador)) {
				continue;
			}
			curarAliadosNoEcstasy(conjurador, estado);
		}
	}

	private void curarAliadosNoEcstasy(Personagem conjurador, EstadoCombate estado) {
		Efeito aura = conjurador.getEfeitosAtivos().get(TheMastersCall.EFEITO_ECSTASY);
		if (aura == null || aura.getModificadores() == null) {
			return;
		}

		double curaBase = aura.getModificadores().getOrDefault(TheMastersCall.CHAVE_CURA_BASE, 0.0);
		if (curaBase <= 0) {
			return;
		}

		int raio = TheMastersCall.calcularRaio(conjurador);
		int cx = conjurador.getPosX();
		int cy = conjurador.getPosY();
		double divisor = calcularDivisorCura(conjurador, estado);

		for (Personagem aliado : estado.getCombatentes()) {
			if (!aliado.isAtivoNoCombate() || !mesmaFaccao(aliado, conjurador)) {
				continue;
			}

			int distancia = calcularDistanciaQuadrada(aliado, cx, cy);
			if (distancia > raio) {
				continue;
			}

			double multiplicador = TheMastersCall.calcularMultiplicadorCuraPorDistancia(distancia);
			double cura = (curaBase * multiplicador) / divisor;
			if (cura > 0) {
				aliado.regenerarVidaFracionada(cura, estado, getController());
				System.out.println(">>> Ecstasy of Gold cura " + aliado.getNome() + " em "
						+ String.format(java.util.Locale.US, "%.2f", cura) + " HP.");
			}
		}
	}

	private double calcularDivisorCura(Personagem conjurador, EstadoCombate estado) {
		long aliadosAtivos = estado.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && mesmaFaccao(p, conjurador))
				.count();
		return Math.max(1.0, aliadosAtivos / 2.0);
	}

	private int calcularDistanciaQuadrada(Personagem p, int cx, int cy) {
		return Math.max(Math.abs(p.getPosX() - cx), Math.abs(p.getPosY() - cy));
	}

	private boolean mesmaFaccao(Personagem a, Personagem b) {
		return a != null && b != null && a.getFaccao() != null && a.getFaccao().equals(b.getFaccao());
	}

	// ========== PASSIVA DO CAMPEÃO ==========

	public void verificarPassivaCampeao(Personagem ator, EstadoCombate estado) {
		String faccaoAtor = ator.getFaccao();
		int inimigosProximos = 0;

		for (Personagem p : estado.getCombatentes()) {
			if (p == ator || !p.isAtivoNoCombate())
				continue;
			if (faccaoAtor != null && faccaoAtor.equals(p.getFaccao()))
				continue;

			int dist = Math.max(Math.abs(p.getPosX() - ator.getPosX()), Math.abs(p.getPosY() - ator.getPosY()));
			if (dist <= 1) {
				inimigosProximos++;
			}
		}

		boolean temEfeito = ator.getEfeitosAtivos().containsKey("Escudo do Campeão");

		if (inimigosProximos >= 2 && !temEfeito) {
			Efeito escudo = new Efeito("Escudo do Campeão", TipoEfeito.BUFF, 9999,
					Map.of("ARMADURA_TOTAL", 20.0), 0, 0);
			ator.adicionarEfeito(escudo);
			ator.recalcularAtributosEstatisticas();
			System.out.println(">>> Passiva Campeão: " + ator.getNome() + " recebe +20 de armadura! ("
					+ inimigosProximos + " inimigos próximos)");
		} else if (inimigosProximos < 2 && temEfeito) {
			ator.removerEfeito("Escudo do Campeão");
			ator.recalcularAtributosEstatisticas();
			System.out.println(">>> Passiva Campeão: Armadura extra removida (poucos inimigos próximos).");
		}
	}
}
