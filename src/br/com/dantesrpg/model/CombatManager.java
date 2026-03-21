package br.com.dantesrpg.model;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.classes.Invocador;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.habilidades.classe.Borderline;
import br.com.dantesrpg.model.habilidades.classe.DistortedSolo;
import br.com.dantesrpg.model.habilidades.classe.FulgorNegro;
import br.com.dantesrpg.model.habilidades.classe.Ilusao;
import br.com.dantesrpg.model.habilidades.classe.PlainSolo;
import br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo;
import br.com.dantesrpg.model.racas.Elfo;
import br.com.dantesrpg.model.racas.Marionette;
import br.com.dantesrpg.model.util.BarbaroUtils;
import br.com.dantesrpg.model.util.DamageEvent;
import br.com.dantesrpg.model.util.DiceRoller;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import br.com.dantesrpg.model.map.TerrainData.EfeitoInstance;
import br.com.dantesrpg.model.map.TerrainData.TipoEfeitoSolo;

public class CombatManager {

	private Map<Personagem, TipoAcao> ultimoTipoAcaoPorPersonagem = new HashMap<>();
	private Random random = new Random();
	private CombatController mainController;
	private AcaoMestreInput lastInput;
	private Personagem atorAtualAnterior;
	private TipoAcao ultimoTipoAcao = TipoAcao.MOVIMENTO;
	private int duracaoChuvaRestante = 0;

	public CombatManager(CombatController controller) {
		this.mainController = controller;
	}

	public AcaoMestreInput getLastInput() {
		return this.lastInput;
	}

	public void proximoTurno(EstadoCombate estado) {
		if (!estado.isCombateAtivo())
			return;

		Personagem proximoAtor = estado.getCombatentes().stream().filter(p -> p.isAtivoNoCombate())
				.min(java.util.Comparator.comparingInt(Personagem::getContadorTU)).orElse(null);

		if (proximoAtor == null) {
			System.out.println("Nenhum combatente vivo encontrado.");
			estado.setAtorAtual(null);
			return;
		}

		int tempoDecorrido = proximoAtor.getContadorTU()
				- (atorAtualAnterior != null ? atorAtualAnterior.getContadorTU() : 0);
		if (tempoDecorrido < 0)
			tempoDecorrido = 0;

		for (Personagem p : estado.getCombatentes()) {
			p.reduzirDuracaoEfeitos(tempoDecorrido);
		}

		atualizarAuras(estado);

		// Salva este ator como o anterior para o próximo ciclo
		this.atorAtualAnterior = proximoAtor;

		// Avança o tempo global se necessário (para Spawns, cooldowns globais, etc)
		int tempoParaAvancar = proximoAtor.getContadorTU();
		if (tempoParaAvancar > 0) {
			avancarTempo(tempoParaAvancar, estado);
		}

		estado.setAtorAtual(proximoAtor);
		Personagem atual = estado.getAtorAtual();

		if (atual.getEfeitosAtivos().containsKey("Choque")) {
			System.out.println(">>> " + atual.getNome() + " sofre atraso pelo CHOQUE (+20 TU)!");
			atual.setContadorTU(atual.getContadorTU() + 20);
		}

		if (atual.getEfeitosAtivos().containsKey("Dormindo")) {
			Efeito dormindo = atual.getEfeitosAtivos().get("Dormindo");
			int turnosRestantes = dormindo.getStacks(); 

			System.out.println(">>> " + atual.getNome() + " está dormindo... Zzz... (Restam " + (turnosRestantes - 1)
					+ " turnos)");

			// Consome 1 turno
			dormindo.setStacks(turnosRestantes - 1);

			// Adiciona penalidade de TU (Pula a vez)
			atual.setContadorTU(atual.getContadorTU() + 100);

			// Se acabou os turnos, acorda
			if (dormindo.getStacks() <= 0) {
				System.out.println(">>> " + atual.getNome() + " acordou naturalmente!");
				atual.removerEfeito("Dormindo");
			}

			//Passa para o próximo imediatamente
			proximoTurno(estado);
			return;
		}

		if (atual.getEfeitosAtivos().containsKey("Stun")) {
			System.out.println(">>> " + atual.getNome() + " está atordoado!");
			atual.removerEfeito("Stun"); // Stun dura apenas 1 turno
			atual.setContadorTU(atual.getContadorTU() + 100);
			proximoTurno(estado);
			return;
		}

		// Terreno
		if (mainController != null && mainController.getMapController() != null) {
			resolverTerrenoPerigoso(proximoAtor, mainController.getMapController(), estado);
		}

		// Regeneração
		if (atual.getPropriedades().contains("REGENERACAO") && atual.isVivo()) {
			double cura = atual.getVidaMaxima() * 0.10;
			if (atual.getVidaAtual() < atual.getVidaMaxima()) {
				double novaVida = Math.min(atual.getVidaMaxima(), atual.getVidaAtual() + cura);
				atual.setVidaAtual(novaVida, estado, mainController);
				System.out.println(">>> REGEN: " + atual.getNome() + " recuperou " + (int) cura + " HP.");
			}
		}

		// Lógica de Pular Turno (Sono/Stun)
		boolean estaDormindo = atual.getEfeitosAtivos().containsKey("Dormindo");
		boolean estaStunado = atual.getEfeitosAtivos().containsKey("Stun");

		if (estaDormindo || estaStunado) {
			System.out.println(">>> " + atual.getNome() + " está " + (estaDormindo ? "Dormindo" : "Atordoado")
					+ " e pula o turno!");
			atual.setContadorTU(atual.getContadorTU() + 100);

			// Remove Stun 
			if (estaStunado)
				atual.removerEfeito("Stun");

			// Chama próximo turno imediatamente
			proximoTurno(estado);
			return;
		}

		// Se não pulou, prepara o turno
		checarEfeitosDeInicioDeTurno(atual, estado); // Se tiver este método

		// Hook de Raça (Half-Angel/Half-Demon manutenção)
		if (atual.getRaca() != null) {
			atual.getRaca().onTurnStart(atual, estado);
		}

		atual.setMovimentoRestanteTurno(atual.getMovimento());
		System.out.println("DEBUG: Movimento de " + atual.getNome() + " resetado para " + atual.getMovimento());
	}

	public void avancarTempo(int tempoParaAvancar, EstadoCombate estado) {
		if (estado == null || estado.getCombatentes() == null || tempoParaAvancar <= 0)
			return;

		System.out.println("--- Avançando " + tempoParaAvancar + " TUs (De " + estado.getTickCounter() + " para "
				+ (estado.getTickCounter() + tempoParaAvancar) + ") ---");

		for (int tick = 1; tick <= tempoParaAvancar; tick++) {

			int tempoGlobalAtual = estado.getTickCounter() + 1;
			estado.setTickCounter(tempoGlobalAtual);

			if (mainController != null && mainController.isEfeitoAndarAtivo()) {
				String efeito = mainController.getEfeitoAndarAtual();
				if (efeito.startsWith("2º Andar") && tempoGlobalAtual % 200 == 0) {
					br.com.dantesrpg.model.util.SessionLogger
							.log("⚠️ 2º ANDAR: Turbulência! Todos devem rodar TOPOR para não serem arremessados!");
				} else if (efeito.startsWith("3º Andar") && tempoGlobalAtual % 300 == 0) {
					Personagem alvoDoOlho = estado.getAtorAtual();
					if (alvoDoOlho != null && alvoDoOlho.isAtivoNoCombate()) {
						br.com.dantesrpg.model.util.SessionLogger
								.log("👁️ O OLHO observou " + alvoDoOlho.getNome() + "!");
						Efeito stun = new Efeito("Stun", TipoEfeito.DEBUFF, 100, null, 0, 0);
						Map<String, Double> mods = new HashMap<>();
						mods.put("REDUCAO_DANO_MODIFICADOR", -0.30);
						Efeito olhoDebuff = new Efeito("O Olho", TipoEfeito.DEBUFF, 300, mods, 0, 0);

						if (!alvoDoOlho.isProtagonista()) {
							alvoDoOlho.adicionarEfeito(stun);
							alvoDoOlho.adicionarEfeito(olhoDebuff);
							alvoDoOlho.recalcularAtributosEstatisticas();
						}
					}
				}

				// 4º ANDAR: Dia (Vento Escaldante)
				else if (efeito.contains("Dia (Vento Escaldante)") && tempoGlobalAtual % 200 == 0) {
					br.com.dantesrpg.model.util.SessionLogger.log("☀️ Vento Escaldante queimou a arena!");
					for (Personagem p : estado.getCombatentes()) {
						if (p.isAtivoNoCombate() && !p.isProtagonista()) {
							aplicarDanoAoAlvo(null, p, 10.0, true, TipoAcao.AMBIENTE, estado);
						}
					}
				}

				// 4º ANDAR: Noite (Vento Congelante)
				else if (efeito.contains("Noite (Vento Congelante)") && tempoGlobalAtual % 200 == 0) {
					br.com.dantesrpg.model.util.SessionLogger.log("❄️ Vento Congelante! Habilidades custam +20 TU.");
					Efeito congelante = new Efeito("Vento Congelante", TipoEfeito.DEBUFF, 100, null, 0, 0);
					for (Personagem p : estado.getCombatentes()) {
						if (p.isAtivoNoCombate() && !p.isProtagonista())
							p.adicionarEfeito(congelante);
					}
				}

				// 5º ANDAR: Tempestade (Chuva + Raios)
				else if (efeito.startsWith("5º Andar")) {
					if (tempoGlobalAtual % 100 == 0 && duracaoChuvaRestante <= 0) {
						if (Math.random() < 0.20) {
							duracaoChuvaRestante = 300;
							br.com.dantesrpg.model.util.SessionLogger.log("⛈️ Começou a chover torrencialmente!");
						}
					}
					if (duracaoChuvaRestante > 0) {
						duracaoChuvaRestante--;
						if (duracaoChuvaRestante % 50 == 0) {
							List<Personagem> vivos = estado.getCombatentes().stream()
									.filter(Personagem::isAtivoNoCombate).collect(Collectors.toList());
							if (!vivos.isEmpty()) {
								Personagem alvoRaio = vivos.get(random.nextInt(vivos.size()));
								if (!alvoRaio.isProtagonista()) {
									br.com.dantesrpg.model.util.SessionLogger
											.log("⚡ RAIO atingiu " + alvoRaio.getNome() + "!");
									Efeito choque = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Choque", 100,
											20);
									Efeito queimadura = br.com.dantesrpg.model.util.EffectFactory
											.criarEfeito("Queimadura", 200, 5);
									alvoRaio.adicionarEfeito(choque);
									alvoRaio.adicionarEfeito(queimadura);
									aplicarDanoAoAlvo(null, alvoRaio, 15.0, true, TipoAcao.AMBIENTE, estado);
								}
							}
						}
						if (duracaoChuvaRestante == 0)
							br.com.dantesrpg.model.util.SessionLogger.log("☁️ A chuva parou.");
					}
				}
			}

			if (mainController != null && mainController.getMapController() != null) {
				mainController.getMapController().avancarTempoTerreno(1);
			}

			// --- LOOP DE PERSONAGENS ---
			for (int i = 0; i < estado.getCombatentes().size(); i++) {
				Personagem p = estado.getCombatentes().get(i);
				if (p == null)
					continue;

				// CHECK DE TEMPO DA RAÇA (PRIORIDADE ABSOLUTA)
				// Deve rodar mesmo se estiver desmaiado em empréstimo
				if (p.getRaca() != null) {
					p.getRaca().onTimeAdvanced(p, estado, mainController);
				}

				// Se não estiver ativo (e não for caso especial de humano), pula
				if (!p.isAtivoNoCombate())
					continue;

				// JACKPOT (Regeneração)
				if (p.getEfeitosAtivos().containsKey("JACKPOT!")) {
					p.setManaAtual(p.getManaMaxima());
					int inspiracao = p.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
					double curaPorTick = p.getVidaMaxima() * (inspiracao * 0.0002);
					p.regenerarVidaFracionada(curaPorTick, estado, mainController);
				}

				// Processamento de Efeitos (DoT / Expiração)
				List<String> efeitosARemover = new ArrayList<>();
				List<String> nomesEfeitosAtuais = new ArrayList<>(p.getEfeitosAtivos().keySet());

				for (String nomeEfeito : nomesEfeitosAtuais) {
					Efeito efeito = p.getEfeitosAtivos().get(nomeEfeito);
					if (efeito == null)
						continue;

					efeito.decrementarDuracao(1);

					// Bloco de Dano DoT
					if (efeito.getTipo() == TipoEfeito.DOT && efeito.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeito.getIntervaloTickTU() == 0)) {
						double danoFinalDoT = 0; // Double

						if (efeito.getNome().equals("Hemorragia")) {
							double percentualHemorragia = 0.02;
							if (efeito.getModificadores() != null) {
								percentualHemorragia = efeito.getModificadores().getOrDefault("PERCENTUAL_HP_MAX",
										percentualHemorragia);
							}
							danoFinalDoT = p.getVidaMaxima() * percentualHemorragia;
						} else {
							double danoDoTBruto = efeito.getDanoPorTick();
							danoFinalDoT = danoDoTBruto * (1.0 - p.getReducaoDoTTopor());
						}

						danoFinalDoT = Math.max(0, danoFinalDoT);

						System.out.println(">>> [TU " + tempoGlobalAtual + "] Efeito [" + efeito.getNome() + "] causa "
								+ String.format("%.1f", danoFinalDoT) + " dano em " + p.getNome());

						double vidaAntes = p.getVidaAtual();
						p.setVidaAtual(vidaAntes - danoFinalDoT, estado, mainController);

						// Efeitos Especiais DoT
						if (efeito.getNome().equals("HellFire")) {
							double redAtual = efeito.getModificadores().getOrDefault("REDUCAO_CURA", 0.0);
							efeito.getModificadores().put("REDUCAO_CURA", redAtual + 0.02);
							p.recalcularAtributosEstatisticas();
						}
						if (efeito.getNome().equals("Choque")) {
							p.setContadorTU(p.getContadorTU() + 20);

						}

						if (danoFinalDoT > 0) {
							if (p.getEfeitosAtivos().containsKey("Dormindo")) {
								System.out.println(">>> " + p.getNome() + " ACORDOU devido ao dano do DoT!");
								br.com.dantesrpg.model.util.SessionLogger
										.log(p.getNome() + " acordou com a dor (" + efeito.getNome() + ").");
								p.removerEfeito("Dormindo");
							}
							if (p.getEfeitosAtivos().containsKey("Sono")) {
								System.out.println(">>> " + p.getNome() + " ACORDOU devido ao dano do DoT!");
								p.removerEfeito("Sono");
							}
						}

						double danoReal = vidaAntes - p.getVidaAtual();

						// Morte por DoT (Essência)
						if (vidaAntes > 0 && !p.isAtivoNoCombate() && !p.isVivo()) {
							// Log e Console
							String msgMorte = "💀 " + p.getNome() + " morreu por " + efeito.getNome() + "!";
							System.out.println(">>> " + p.getNome() + " morreu por DoT.");
							br.com.dantesrpg.model.util.SessionLogger.log(msgMorte);

							// XP
							if (p.getXpReward() > 0) {
								System.out.println(">>> XP por DoT: " + p.getXpReward());
								estado.adicionarXpAoPool(p.getXpReward());
							}

							// Murasame (Essência)
							if (efeito.getNome().equals("Toxina")) {
								for (Personagem combatente : estado.getCombatentes()) {
									if (combatente.getArmaEquipada() != null
											&& combatente.getArmaEquipada().getNome().equals("Murasame")) {
										System.out.println(">>> MURASAME: Capturando Essência (DoT)...");
										br.com.dantesrpg.model.util.SessionLogger
												.log("Murasame absorveu a alma de " + p.getNome());
										combatente.getInventario()
												.adicionarItem(new br.com.dantesrpg.model.items.EssenciaInimigo(p));
										break;
									}
								}
							}

							// Remove do Mapa (Visual)
							if (mainController != null) {
								// Se for objeto (Parede destruída por veneno? Raro, mas possível)
								if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
									if (mainController.getMapController() != null) {
										mainController.getMapController().atualizarCelulaParaChao(p.getPosX(),
												p.getPosY());
									}
								}
								// Atualiza listas e timeline
								mainController.atualizarInterfaceAposMorte();
							}
						}
						p.registrarDanoSofrido(danoReal, tempoGlobalAtual);
					}

					if (efeito.getDuracaoTURestante() <= 0) {
						efeitosARemover.add(nomeEfeito);
					}
				}

				if (!efeitosARemover.isEmpty()) {
					boolean statsChanged = false;
					for (String nomeEfeito : efeitosARemover) {
						Efeito efeitoRemovido = p.removerEfeito(nomeEfeito);
						if (efeitoRemovido != null) {
							System.out.println(">>> [TU " + tempoGlobalAtual + "] Efeito [" + nomeEfeito
									+ "] expirou em " + p.getNome() + ".");
							statsChanged = true;

							// Limpezas Especiais
							if (nomeEfeito.equals("Estado Dourado") && p.getRaca() instanceof Elfo) {
								((Elfo) p.getRaca()).onEstadoDouradoEnd(p, estado);
							}
							if (nomeEfeito.equals("Ringue da Vontade") && mainController != null) {
								mainController.limparRingueDoMapa();
							}
							if (nomeEfeito.equals("Domínio: Idle Death Gamble") && mainController != null) {
								mainController.limparDominioLyriaDoMapa();
							}
							if (nomeEfeito.equals("Modo Justiça")) {
								br.com.dantesrpg.model.fantasmasnobres.ModoPolaris.reverterParaPolaris(p);
							}
						}
					}
					if (statsChanged) {
						p.recalcularAtributosEstatisticas();
					}
				}
			}
		}

		// Atualiza visual TU
		for (Personagem p : estado.getCombatentes()) {
			if (p != null) {
				p.setContadorTU(p.getContadorTU() - tempoParaAvancar);
			}
		}
		atualizarAuras(estado);

		System.out.println("--- Avanço de tempo concluído. ---");
	}

	public void resolverAcao(AcaoMestreInput input, EstadoCombate estado) {
		this.lastInput = input;
		if (input == null || input.getAtor() == null)
			return;
		Personagem ator = input.getAtor();

		String nomeAcao = "";
		if (input.getFantasmaNobre() != null)
			nomeAcao = input.getFantasmaNobre().getNome();
		else if (input.getHabilidade() != null)
			nomeAcao = input.getHabilidade().getNome();

		if (!nomeAcao.isEmpty()) {
			String cdKey = "CD:" + nomeAcao;
			if (ator.getEfeitosAtivos().containsKey(cdKey)) {
				System.out.println(">>> AÇÃO BLOQUEADA: " + nomeAcao + " está em Cooldown!");
				br.com.dantesrpg.model.util.SessionLogger
						.log(ator.getNome() + " tentou usar " + nomeAcao + " mas está em recarga.");
				return; // INTERROMPE A AÇÃO
			}
		}

		String acaoNome = (input.getHabilidade() != null) ? input.getHabilidade().getNome() : "Ataque Básico";
		br.com.dantesrpg.model.util.SessionLogger.log(input.getAtor().getNome() + " usou [" + acaoNome + "]");

		List<Personagem> alvos = input.getAlvos();
		Habilidade habilidade = input.getHabilidade();
		String tipoParaVerificacao = (habilidade == null) ? "ATAQUE" : "HABILIDADE";

		if (ator.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble")
				&& ("ATAQUE".equals(tipoParaVerificacao) || "HABILIDADE".equals(tipoParaVerificacao))) {

			int d1 = input.getResultadoDado("DADO_LYRIA_1");
			int d2 = input.getResultadoDado("DADO_LYRIA_2");
			int d3 = input.getResultadoDado("DADO_LYRIA_3");

			if (d1 > 0) {
				br.com.dantesrpg.model.fantasmasnobres.ApostadorIncansavel.processarAposta(ator, d1, d2, d3, estado);

				if (ator.getEfeitosAtivos().containsKey("JACKPOT!")) {
					if (mainController != null) {
						mainController.limparDominioLyriaDoMapa();
						System.out.println(">>> JACKPOT! O Domínio visual foi encerrado.");
					}
				}
			}
		}

		if (habilidade != null && habilidade.getNome().equals("Caçada")) {

			int numTiros = input.getResultadoDado("DADO_CHANCE_CACADA_1D6");
			if (numTiros <= 0)
				numTiros = 1;
			System.out.println(">>> Caçada: Disparando " + numTiros + " tiros...");

			List<Personagem> inimigosVivos = estado.getCombatentes().stream()
					.filter(p -> p.isAtivoNoCombate() && !p.getFaccao().equals(ator.getFaccao()))
					.collect(Collectors.toList());

			if (inimigosVivos.isEmpty()) {
				System.out.println(">>> Caçada: Nenhum inimigo vivo para mirar.");
			} else {
				List<Personagem> alvosAleatorios = new ArrayList<>();
				for (int i = 0; i < numTiros; i++) {
					alvosAleatorios.add(inimigosVivos.get(random.nextInt(inimigosVivos.size())));
				}
				alvos = alvosAleatorios;
			}
		}

		TipoAcao tipoAcaoAtual;
		int custoTUBase;
		int custoManaBase;

		// --- REGRA 1 (Restrição Celestial): Transforma Ataque Básico em Fulgor Negro
		if (habilidade == null && ator.getEfeitosAtivos().containsKey("Restrição Celestial")) {
			System.out.println(">>> Restrição Celestial transforma Ataque Básico em Fulgor Negro!");
			Optional<Habilidade> fulgorOpt = ator.getHabilidadesDeClasse().stream()
					.filter(h -> "Fulgor Negro".equals(h.getNome())).findFirst();
			if (fulgorOpt.isPresent()) {
				habilidade = fulgorOpt.get(); // Sobrescreve a ação

				// CRIA UM NOVO INPUT COM A HABILIDADE E OS DADOS CORRETOS
				AcaoMestreInput inputFulgor = new AcaoMestreInput(ator, alvos, habilidade);
				inputFulgor.adicionarResultadoDado("DADO_ATRIBUTO", input.getResultadoDado("DADO_ATRIBUTO"));
				inputFulgor.adicionarResultadoDado("DADO_CHANCE_RESTRICAO",
						input.getResultadoDado("DADO_CHANCE_RESTRICAO"));
				input = inputFulgor; // Substitui o input original

			} else {
				System.err.println(
						"Erro: Restrição Celestial ativa, mas Fulgor Negro não encontrado em " + ator.getNome());
			}
		}

		if (habilidade == null) {
			tipoAcaoAtual = TipoAcao.ATAQUE_BASICO;
			custoManaBase = 0;
			Arma arma = ator.getArmaEquipada();
			if (arma.isRequerMunicao()) {
				if (arma.getMunicaoAtual() <= 0) {
					System.out.println(">>> CLIQUE SECO! " + ator.getNome() + " tentou atirar sem munição.");
					return; // Impede o ataque
				}
				System.out.println(
						">>> Bala disparada. Restam: " + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima());
			}
			custoTUBase = arma.getCustoTU();
			int rolagemDadoAtributo = input.getResultadoDado("DADO_ATRIBUTO");
			if (rolagemDadoAtributo == -1) {
				System.err.println("Erro: Ataque básico sem Resultado Dado Atributo.");
				return;
			}

			// Passa null
			resolverDanoPadrao(ator, arma, rolagemDadoAtributo, alvos, 1.0, tipoAcaoAtual, null, estado, input);

		} else {
			// --- LÓGICA DE HABILIDADE ---
			tipoAcaoAtual = TipoAcao.HABILIDADE;
			custoManaBase = habilidade.getCustoMana();
			custoTUBase = habilidade.getCustoTU();

			if (habilidade.getTipoAlvo() == TipoAlvo.AREA || habilidade.getTipoAlvo() == TipoAlvo.EQUIPE) {
				System.out.println(">>> Gerando alvos para habilidade centrada no ator: " + habilidade.getNome());
				alvos = encontrarAlvosAoRedorDoAtor(ator, habilidade, estado);
				input.getAlvos().clear();
				input.getAlvos().addAll(alvos);
			}

			// É uma habilidade OFENSIVA (ataca um alvo)?
			if (habilidade.getTipoAlvo() != TipoAlvo.SI_MESMO) {

				// Habilidades ofensivas PRECISAM de um dado de atributo (na maioria dos casos)
				int rolagemDadoAtributo = input.getResultadoDado("DADO_ATRIBUTO");

				if (habilidade.getMultiplicadorDeDano() > 0 && rolagemDadoAtributo == -1
						&& !habilidade.getNome().equals("Soco Sério")) { // Soco Sério tem sua própria validação
					System.err.println(
							"Erro: Habilidade ofensiva (" + habilidade.getNome() + ") sem Resultado Dado Atributo.");
					return;
				}

				if (habilidade.getNome().equals("Fulgor Negro")) {
					// Chama o método da própria classe, passando as dependências
					((FulgorNegro) habilidade).executarFulgorNegro(input, estado, this);
				} else if (habilidade instanceof DistortedSolo) {
					((DistortedSolo) habilidade).executarSolo(input, estado, this);
				} else if (habilidade instanceof WhaWhaSolo) {
					((WhaWhaSolo) habilidade).executarSolo(input, estado, this);
				} else if (habilidade instanceof PlainSolo) {
					((PlainSolo) habilidade).executarSolo(input, estado, this);

					// --- PONTO DE INSERÇÃO ---
				} else if (habilidade.getNome().equals("Soco Sério")) {
					// Chama a nova lógica de dano híbrida
					int danoTotalCausado = resolverSocoSerio(ator, alvos, habilidade, input, estado);
					System.out.println(
							">>> Dano total da ação (" + habilidade.getNome() + "): " + danoTotalCausado + ".");

				} else if (habilidade.getMultiplicadorDeDano() > 0) {
					Arma arma = ator.getArmaEquipada();
					if (arma == null) {
						System.out.println(ator.getNome() + " está desarmado!");
						return;
					}
					resolverDanoPadrao(ator, arma, rolagemDadoAtributo, alvos, habilidade.getMultiplicadorDeDano(),
							tipoAcaoAtual, habilidade, estado, input);
				}

			} else {
				System.out.println(">>> " + ator.getNome() + " usa " + habilidade.getNome() + " em si mesmo.");
			}

			if (habilidade instanceof br.com.dantesrpg.model.habilidades.classe.Rezar) {
				String escolha = input.getOpcaoEscolhida();
				if (escolha == null)
					escolha = "Força";
				((br.com.dantesrpg.model.habilidades.classe.Rezar) habilidade).executarComEscolha(ator, escolha);

			} else if (habilidade instanceof br.com.dantesrpg.model.habilidades.classe.Bencao) {
				String escolha = input.getOpcaoEscolhida();
				if (escolha == null)
					escolha = "Poder";
				if (!alvos.isEmpty()) {
					((br.com.dantesrpg.model.habilidades.classe.Bencao) habilidade).executarComEscolha(alvos.get(0),
							escolha);
				}

			} else {
				// Execução Padrão (Fulgor, Solos, etc)
				if (!ator.isClone() || habilidadePodeSerCopiadaPorClone(habilidade)) {
					aplicarEfeitosDaHabilidade(ator, habilidade, alvos, estado, this);
				}
			}

			int cooldown = habilidade.getCooldownTU();
			if (cooldown > 0) {
				String cooldownEffectName = "CD:" + habilidade.getNome();
				Efeito cooldownEfeito = new Efeito(cooldownEffectName, TipoEfeito.DEBUFF, cooldown, null, 0, 0);
				aplicarEfeito(ator, cooldownEfeito);
				System.out.println(">>> " + habilidade.getNome() + " entrou em cooldown por " + cooldown + " TU.");
			}
		}

		if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.FRACO) {
			custoTUBase = (int) (custoTUBase * 0.80); // -20% TU
			System.out.println(">>> Modo FRACO: TU reduzido para " + custoTUBase);
		} else if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.FORTE) {
			custoTUBase = (int) (custoTUBase * 1.20); // +20% TU
			System.out.println(">>> Modo FORTE: TU aumentado para " + custoTUBase);
		}

		// Custo de Rajada (Ranged): +10% TU por tiro EXTRA
		if (input.getTirosExtras() > 0) {
			double taxaExtra = input.getTirosExtras() * 0.10;
			custoTUBase = (int) (custoTUBase * (1.0 + taxaExtra));
			System.out.println(">>> RAJADA (+" + input.getTirosExtras() + " tiros): Custo TU total: " + custoTUBase);
		}

		// --- CÁLCULO FINAL DE CUSTO (Universal) ---
		int custoManaFinal = calcularCustoManaFinal(ator, habilidade, custoManaBase);
		if (custoManaFinal > 0)
			ator.setManaAtual(ator.getManaAtual() - custoManaFinal);
		else if (custoManaFinal < 0)
			ator.setManaAtual(ator.getManaAtual() - custoManaFinal);

		int custoTUFinal = calcularCustoTUFinal(ator, custoTUBase, habilidade, tipoAcaoAtual);
		double multiplicadorLento = ator.getMultiplicadorCustoTU();
		custoTUFinal = (int) (custoTUFinal * multiplicadorLento);

		if (multiplicadorLento > 1.0) {
			System.out
					.println(">>> Efeito Lento/Muito Lento aplicado! Custo TU: " + custoTUBase + " -> " + custoTUFinal);
		}

		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoTUFinal + " TUs.");

		// Hook onActionUsed (Movido para o final)
		chamarHookAcaoUsada(ator, tipoAcaoAtual, estado);

		if (habilidade != null) {
			ator.setUltimaHabilidadeUsada(normalizarHabilidadeCopiavelParaClone(habilidade));
		}
		verificarManaPassivaModoJustica(ator, estado);
	}

	public int estimarDano(Personagem ator, Habilidade habilidade, Personagem alvo, int rolagemDadoAtributo,
			int rolagemTrocado) {
		if (ator == null || ator.getArmaEquipada() == null)
			return 0;
		Arma arma = ator.getArmaEquipada();

		// Define o multiplicador
		double multiplicadorHabilidade = 1.0;
		if (habilidade != null) {
			// Se alvo for nulo (ex: estimativa sem alvo selecionado), assume 1.0 ou usa um dummy
			multiplicadorHabilidade = habilidade.getMultiplicadorModificado(ator, alvo, null);
		}

		// Define os ticks
		int numeroDeTicks = 1;
		// Cria um input temporário apenas para calcular ticks variáveis
		AcaoMestreInput inputDummy = new AcaoMestreInput(ator, new ArrayList<>(), habilidade);
		if (habilidade != null && habilidade.getNome().equals("Trocado")) {
			// Injeta o dado do Trocado no input dummy
			inputDummy.adicionarResultadoDado("DADO_CHANCE_TROCADO", rolagemTrocado);
			numeroDeTicks = habilidade.getTicksModificados(ator, inputDummy);
		} else if (habilidade != null) {
			numeroDeTicks = habilidade.getTicksModificados(ator, inputDummy);
		} else {
			numeroDeTicks = arma.getTicksDeDano();
		}

		double danoTotalEstimado = 0.0;
		rolagemDadoAtributo = Math.max(0, rolagemDadoAtributo);

		// Fatores Globais
		double fatorSorte = 1.0 + ator.getSortePercentual();

		// Verifica Tiro Especial para estimativa
		boolean isTiroEspecial = ator.getEfeitosAtivos().containsKey("Tiro Especial");
		double bonusTiroEspecial = 0.0;
		if (isTiroEspecial) {
			int sag = ator.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);
			bonusTiroEspecial = sag * 0.1;
		}

		for (int i = 0; i < numeroDeTicks; i++) {
			double modGolpePerfeito = 1.0;

			// Simula Golpe Perfeito no 1º tick
			if (i == 0 && rolagemDadoAtributo > 0) {
				int tipoDado = DiceRoller
						.getTipoDado(ator.getAtributosFinais().getOrDefault(arma.getAtributoMultiplicador(), 1));
				if (rolagemDadoAtributo == tipoDado) {
					modGolpePerfeito = 1.25;
				}
			}

			// Cálculo Base (Idêntico ao calcularDanoFinalTick)
			double dano = arma.getDanoBase() * (1 + (0.075 * rolagemDadoAtributo));

			dano *= fatorSorte;

			if (ator.getBonusDanoPercentual() > 0) {
				dano *= (1.0 + ator.getBonusDanoPercentual());
			}

			dano *= (1.0 + bonusTiroEspecial);

			// Bonus da Arma (requer alvo, se null assume 1.0)
			if (alvo != null) {
				dano *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, null, inputDummy);
			}

			dano *= multiplicadorHabilidade;
			dano *= modGolpePerfeito;

			// Trocado: Lógica crescente
			if (habilidade != null && habilidade.getNome().equals("Trocado")) {
				dano *= (1.0 + (i * 0.25));
			}

			danoTotalEstimado += Math.max(0.0, dano);
		}

		return (int) Math.ceil(danoTotalEstimado);
	}

	private void resolverDanoPadrao(Personagem ator, Arma arma, int rolagemDadoAtributo, List<Personagem> alvos,
			double multiplicadorHabilidade, br.com.dantesrpg.model.enums.TipoAcao tipoAcaoDano, Habilidade habilidade,
			EstadoCombate estado, AcaoMestreInput input) {

		// Preparação
		boolean estavaEmStealth = processarStealthInicial(ator);
		boolean isTiroEspecial = processarTiroEspecial(ator);

		Map<Personagem, List<DamageEvent>> matrizDeDanos = new HashMap<>();

		// Configuração de Ticks Base
		int ticksBase = (habilidade != null) ? habilidade.getTicksModificados(ator, input) : arma.getTicksDeDano();

		// Lógica de Rajada 
		// Verifica se é instância de ArmaRanged OU se o tipo é "Ranged"
		boolean isArmaRanged = (arma instanceof br.com.dantesrpg.model.ArmaRanged)
				|| "Ranged".equalsIgnoreCase(arma.getTipo());
		boolean isModoCoronhada = (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.CORONHADA);
		double multiplicadorAtaqueAlternativo = arma.getMultiplicadorAtaqueAlternativoBasico();

		// Só é rajada se for arma ranged, sem habilidade e não for coronhada
		boolean isRajada = (habilidade == null && isArmaRanged && !isModoCoronhada);

		int tirosExtrasSolicitados = input.getTirosExtras();
		int tirosExtrasReais = 0;

		// Consumo de Munição
		if (arma.isRequerMunicao() && habilidade == null && !isModoCoronhada) {
			int municaoAtual = arma.getMunicaoAtual();

			// Custo do tiro base (sempre 1)
			if (municaoAtual < 1) {
				System.out.println(">>> CLIQUE SECO! Sem munição.");
				return;
			}

			// Gasta o tiro base
			arma.gastarMunicao();

			if (isRajada && tirosExtrasSolicitados > 0) {
				// Calcula quantos extras podemos dar com a munição restante
				int municaoRestante = arma.getMunicaoAtual();
				tirosExtrasReais = Math.min(tirosExtrasSolicitados, municaoRestante);

				// Gasta as balas extras
				for (int k = 0; k < tirosExtrasReais; k++) {
					arma.gastarMunicao();
				}
				System.out.println(">>> ARMA: Rajada de +" + tirosExtrasReais + " tiros. (Total gasto: "
						+ (1 + tirosExtrasReais) + ")");
			} else {
				System.out.println(">>> ARMA: Tiro único disparado.");
			}
		}

		double fatorSorte = 1.0 + ator.getSortePercentual();

		// Geração de Eventos de Dano
		for (Personagem alvo : alvos) {
			if (!isAlvoValido(alvo))
				continue;
			List<DamageEvent> eventosDoAlvo = new ArrayList<>();

			// --- GRUPO A: TICKS BASE (Dano Normal) ---
			for (int i = 0; i < ticksBase; i++) {
				// Modificadores de Modo (Afetam o tiro principal)
				double modModo = 1.0;
				if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.FRACO)
					modModo = 0.75;
				if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.FORTE)
					modModo = 1.25;
				if (isModoCoronhada)
					modModo = multiplicadorAtaqueAlternativo;

				double multiplicadorFinal = multiplicadorHabilidade * modModo;

				// Crítico
				double modCritico = calcularModificadorCritico(ator, rolagemDadoAtributo, arma, i, estavaEmStealth,
						multiplicadorFinal, isTiroEspecial);
				boolean isCrit = (modCritico > 1.0);

				// Cálculo
				double danoBruto = calcularDanoFinalTick(ator, arma, rolagemDadoAtributo, alvo, estado, input,
						multiplicadorFinal, modCritico, fatorSorte, isTiroEspecial);
				double danoLiquido = aplicarReducaoArmadura(danoBruto, ator, alvo, estado);

				// Adiciona Evento Base
				String labelTick = (ticksBase > 1) ? "Hit " + (i + 1) : "Ataque";
				eventosDoAlvo.add(criarEventoDano(danoLiquido, labelTick, isCrit, ator, alvo, arma, input, estado,
						isTiroEspecial, multiplicadorHabilidade));

				// SUBTICKS ESPECIAIS (Apenas gerados a partir dos ticks base)
				// Eco (Combo)
				if (ator.getEfeitosAtivos().containsKey("Combo!") && multiplicadorHabilidade == 1.0) {
					eventosDoAlvo.add(new DamageEvent(danoLiquido * 0.30, "Eco", false, null));
				}
				// Cascata Marionette
				if (ator.getRaca() instanceof Marionette && isCrit && habilidade == null) {
					simularCascataMarionette(ator, alvo, arma, rolagemDadoAtributo, input, estado, isTiroEspecial,
							eventosDoAlvo, danoLiquido);
				}
			}

			// --- GRUPO B: TICKS DE RAJADA (Subticks Extras) ---
			if (tirosExtrasReais > 0) {
				// Dano Base de Rajada (Sem modificador de modo Forte/Fraco, apenas 25% do dano padrão da arma)
				// Usando 1.0 no multiplicador para pegar o dano 'cru' da arma + atributos
				double danoBaseRajada = calcularDanoFinalTick(ator, arma, rolagemDadoAtributo, alvo, estado, input, 1.0,
						1.0, fatorSorte, isTiroEspecial);

				// Regra: "0.25% do dano do tick base" (Assumo que quis dizer 25%, ou 0.25x)
				double danoRajadaUnitario = danoBaseRajada * 0.25;

				for (int i = 0; i < tirosExtrasReais; i++) {
					// Rajada pode critar individualmente
					boolean isCritRajada = (Math.random() < ator.getTaxaCritica());
					double modCrit = isCritRajada ? (1.0 + ator.getDanoCritico()) : 1.0;

					double danoFinalRajada = danoRajadaUnitario * modCrit;
					double danoLiquidoRajada = aplicarReducaoArmadura(danoFinalRajada, ator, alvo, estado);

					// Adiciona como Subtick Visual
					eventosDoAlvo.add(criarEventoDano(danoLiquidoRajada, "Rajada " + (i + 1), isCritRajada, ator, alvo,
							arma, input, estado, isTiroEspecial, multiplicadorHabilidade));
				}
			}

			matrizDeDanos.put(alvo, eventosDoAlvo);
		}

		finalizarAcao(ator, isTiroEspecial, alvos);

		if (!matrizDeDanos.isEmpty()) {
			mainController.abrirJanelaResolucao(ator, alvos, habilidade, matrizDeDanos);
		}
	}

	// --- Métodos Auxiliares para Limpar o Código Acima ---

	private double getMultiplicadorBonusDanoComArma(Personagem ator, Arma arma, Personagem alvo, EstadoCombate estado,
			AcaoMestreInput input) {
		if (arma == null) {
			return 1.0;
		}

		double multiplicador = arma.getBonusDanoArma(ator, alvo, estado, input);
		if (ator != null && ator.getRaca() != null) {
			multiplicador *= ator.getRaca().getMultiplicadorBonusDanoArma(ator, arma, alvo, estado, input);
		}
		return multiplicador;
	}

	private double aplicarReducaoDanoPreArmadura(double danoBruto, Personagem ator, Personagem alvo,
			EstadoCombate estado) {
		if (danoBruto <= 0 || alvo == null || alvo.getRaca() == null) {
			return danoBruto;
		}

		double multiplicador = alvo.getRaca().getMultiplicadorDanoRecebidoPreArmadura(alvo, ator, estado);
		return Math.max(0, danoBruto * Math.max(0.0, multiplicador));
	}

	private double aplicarReducaoArmadura(double danoBruto, Personagem ator, Personagem alvo, EstadoCombate estado) {
		double danoAjustado = aplicarReducaoDanoPreArmadura(danoBruto, ator, alvo, estado);
		double reducaoArmadura = alvo.getReducaoDanoArmadura() + alvo.getReducaoDanoTopor();
		double pularDefesa = 0.0;
		if (ator != null && ator.getArmaEquipada() != null) {
			pularDefesa = ator.getArmaEquipada().getIgnorarDefesaPercentual(ator, alvo, estado);
		}
		reducaoArmadura -= (reducaoArmadura * pularDefesa);
		if (alvo.getEfeitosAtivos().containsKey("Ruptura"))
			reducaoArmadura -= 0.25;
		return Math.max(0, danoAjustado * (1.0 - reducaoArmadura));
	}

	private DamageEvent criarEventoDano(double danoEstimado, String label, boolean isCritico, Personagem ator,
			Personagem alvo, Arma arma, AcaoMestreInput input, EstadoCombate estado, boolean isTiroEspecial,
			double modHab) {
		// Consumer atualizado para receber o dano real
		java.util.function.Consumer<Double> onHit = (danoRealPosResolucao) -> {

			processarEfeitosOnHit(ator, alvo, arma, danoRealPosResolucao, estado, isTiroEspecial);

			processarHooksDeSistema(ator, alvo, arma, input, danoRealPosResolucao, estado, modHab, isCritico);
		};

		return new DamageEvent(danoEstimado, label, isCritico, onHit);
	}

	private void verificarSubticksEspeciais(Personagem ator, Personagem alvo, Arma arma, int rolagem,
			AcaoMestreInput input, EstadoCombate estado, boolean isTiroEspecial, List<DamageEvent> lista,
			double danoBase, boolean isCrit, double modHab, Habilidade hab) {
		// Eco
		if (ator.getEfeitosAtivos().containsKey("Combo!") && modHab == 1.0) {
			lista.add(new DamageEvent(danoBase * 0.30, "Eco", false, null));
		}
		// Cascata Marionette
		if (ator.getRaca() instanceof Marionette && isCrit && hab == null) {
			simularCascataMarionette(ator, alvo, arma, rolagem, input, estado, isTiroEspecial, lista, danoBase);
		}
	}

	private void simularCascataMarionette(Personagem ator, Personagem alvo, Arma arma, int rolagemOriginal,
			AcaoMestreInput input, EstadoCombate estado, boolean isTiroEspecial, List<DamageEvent> listaEventos,
			double danoBaseAnterior) {
		double danoParaCascata = arma.getDanoBase() * (1 + (0.075 * rolagemOriginal));
		if (ator.getBonusDanoPercentual() > 0)
			danoParaCascata *= (1.0 + ator.getBonusDanoPercentual());
		danoParaCascata *= (1.0 + ator.getSortePercentual());
		danoParaCascata *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, estado, input);

		// Redução de Armadura do Alvo (Para o dano exibido ser real)
		double reducaoArmadura = alvo.getReducaoDanoArmadura() + alvo.getReducaoDanoTopor();
		if (alvo.getEfeitosAtivos().containsKey("Ruptura"))
			reducaoArmadura -= 0.25;
		if (ator.getBonusDanoPercentual() > 0)
			danoParaCascata *= (1.0 + ator.getBonusDanoPercentual());
		danoParaCascata *= (1.0 + ator.getSortePercentual());
		danoParaCascata *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, estado, input);

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

	private void executarRecursaoCascata(Personagem ator, Personagem alvo, double danoBaseAnterior, int nivel,
			EstadoCombate estado, TipoAcao tipoAcaoDano) {
		if (nivel > 7) {
			System.out.println(">>> CASCATA (Marionette): Limite de 7 golpes atingido.");
			return;
		}

		// Regra: Cada hit extra causa 50% do dano do anterior
		double danoAtual = danoBaseAnterior * 0.50;

		if (danoAtual < 1.0) {
			System.out.println(">>> CASCATA: Dano muito baixo, parando.");
			return;
		}

		// Rola Crítico para este hit da cascata
		// Nota: Cascata pode critar independentemente
		boolean critico = (Math.random() < ator.getTaxaCritica());
		double modCritico = critico ? (1.0 + ator.getDanoCritico()) : 1.0;

		double danoFinal = danoAtual * modCritico;

		System.out.println(
				">>> CASCATA Nível " + nivel + " (Crítico: " + critico + "): " + String.format("%.1f", danoFinal));

		aplicarDanoAoAlvo(ator, alvo, danoFinal, false, tipoAcaoDano, estado);

		// Se critou, continua a cascata recursivamente
		if (critico) {
			executarRecursaoCascata(ator, alvo, danoAtual, nivel + 1, estado, tipoAcaoDano);
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

	private double calcularDanoFinalTick(Personagem ator, Arma arma, int rolagem, Personagem alvo, EstadoCombate estado,
			AcaoMestreInput input, double modHabilidade, double modCritico, double modSorte, boolean isTiroEspecial) {
		// Base
		double dano = arma.getDanoBase() * (1 + (0.075 * rolagem));

		// Multiplicadores
		dano *= modSorte;

		if (ator.getBonusDanoPercentual() > 0) {
			dano *= (1.0 + ator.getBonusDanoPercentual());
		}

		// Bônus Especiais
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

	private void processarEfeitosOnHit(Personagem ator, Personagem alvo, Arma arma, double danoCausado,
			EstadoCombate estado, boolean isTiroEspecial) {
		if (danoCausado <= 0)
			return;

		// Efeito da Arma (DoT, Debuff, Charm)
		String nomeEfeito = arma.getNomeEfeitoOnHit();
		double chance = arma.getChanceEfeitoOnHit();

		if (nomeEfeito != null && !nomeEfeito.isEmpty()) {
			// Rola a chance (ex: 10%)
			if (Math.random() <= chance) {
				System.out.println(">>> Efeito On-Hit ativado: " + nomeEfeito);

				// --- LÓGICA ESPECÍFICA DE CHARM ---
				if (nomeEfeito.equalsIgnoreCase("Charm")) {

					// Calcula Stacks baseados no Carisma do Atacante
					int carismaAtor = ator.getAtributosFinais()
							.getOrDefault(br.com.dantesrpg.model.enums.Atributo.CARISMA, 1);
					int stacksAplicados = Math.max(1, carismaAtor); // Mínimo 1

					System.out.println(">>> CHARM! " + alvo.getNome() + " recebe " + stacksAplicados
							+ " acúmulos (Baseado em CAR " + carismaAtor + ")");

					// Cria/Aplica o efeito Charm com os stacks corretos Duração 9999 (Infinito até estourar)
					Efeito charmEffect = new Efeito("Charm", TipoEfeito.DEBUFF, 9999, null, 0, 0);
					charmEffect.setStacks(stacksAplicados);

					alvo.adicionarEfeito(charmEffect);

					// 3. Verifica Estouro (>= 100)
					Efeito charmAtual = alvo.getEfeitosAtivos().get("Charm");
					if (charmAtual != null && charmAtual.getStacks() >= 100) {
						aplicarControleMental(alvo, estado);
					}

				}
				// --- OUTROS EFEITOS PADRÃO (Veneno, Sangramento, etc) ---
				else {
					Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito(nomeEfeito, 200,
							(arma.getDanoBase() / 4));
					efeito.setStacks(1);
					aplicarEfeito(alvo, efeito);
				}
			}
		}

		if (ator.getEfeitosAtivos().containsKey(BarbaroUtils.EFEITO_BALANCO_TEMERARIO) && Math.random() <= 0.50) {
			int danoChoque = Math.max(1, (int) Math.ceil(arma.getDanoBase() * 0.25));
			Efeito choque = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Choque", 100, danoChoque);
			aplicarEfeito(alvo, choque);
			System.out.println(">>> Balanço Temerário aplicou Choque em " + alvo.getNome() + ".");
		}
	}

	private void aplicarControleMental(Personagem alvo, EstadoCombate estado) {
		System.out.println(">>> " + alvo.getNome() + " FOI SEDUZIDO! Trocando de lado por 300 TU.");
		br.com.dantesrpg.model.util.SessionLogger.log("❤ " + alvo.getNome() + " teve a mente controlada! ❤");

		// Remove os stacks de Charm
		alvo.removerEfeito("Charm");

		// Salva a facção original nas propriedades (se já não estiver salvo)
		boolean jaSalvo = alvo.getPropriedades().stream().anyMatch(s -> s.startsWith("ORIGINAL_FACTION:"));
		if (!jaSalvo) {
			alvo.adicionarPropriedade("ORIGINAL_FACTION:" + alvo.getFaccao());
		}

		// Troca a Facção
		if ("JOGADOR".equals(alvo.getFaccao())) {
			alvo.setFaccao("INIMIGO");
		} else {
			alvo.setFaccao("JOGADOR");
		}

		// Aplica o Efeito Temporizador
		// Esse efeito não faz nada nos stats, serve apenas para contar o tempo (300 TU) Quando ele expirar, o Personagem.removerEfeito vai disparar o hook de eversão.
		Efeito controle = new Efeito("Controle Mental", TipoEfeito.DEBUFF, 300, null, 0, 0);
		alvo.adicionarEfeito(controle);

		// Atualiza UI imediatamente para refletir a mudança de cor/time
		mainController.atualizarInterfaceTotal();
	}

	private double calcularModificadorCritico(Personagem ator, int rolagem, Arma arma, int tickIndex, boolean stealth,
			double modHabilidade, boolean isTiroEspecial) {
		double mod = 1.0;

		// Golpe Perfeito (Apenas 1º tick)
		if (tickIndex == 0) {
			int tipoDado = DiceRoller
					.getTipoDado(ator.getAtributosFinais().getOrDefault(arma.getAtributoMultiplicador(), 1));
			if (rolagem == tipoDado) {
				mod *= 1.25;
				System.out.println(">>> GOLPE PERFEITO!");
			}
		}

		// Crítico Normal
		double bonusCritRate = 0.0;
		if (isTiroEspecial) {
			int sag = ator.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);
			bonusCritRate = sag * 0.01;
		}

		if (Math.random() < (ator.getTaxaCritica() + bonusCritRate)) {
			mod *= (1 + ator.getDanoCritico());
			System.out.println(">>> ACERTO CRÍTICO!");
		}

		// Stealth (Crítico Garantido no 1º hit básico)
		if (tickIndex == 0 && stealth && modHabilidade == 1.0 && mod == 1.0) {
			mod = (1 + ator.getDanoCritico());
			System.out.println(">>> Stealth: Crítico Garantido!");
		}

		return mod;
	}

	private void processarHooksDeSistema(Personagem ator, Personagem alvo, Arma arma, AcaoMestreInput input,
			double danoTick, EstadoCombate estado, double modHabilidade, boolean isCritico) {

		if (danoTick > 0 && ator.getRaca() != null) {
			// Notifica que causou dano (Half-Angel ganha +1)
			ator.getRaca().onDamageDealt(ator, alvo, danoTick, estado, this.mainController);

			// Notifica se foi crítico (Half-Angel ganha +1 extra)
			if (isCritico) {
				ator.getRaca().onCriticalHit(ator, alvo, estado);
			}
		}
		if (danoTick > 0 && ator.getFantasmaNobre() != null) {
			ator.getFantasmaNobre().onDamageDealt(ator, alvo, danoTick, estado, this);
			if (isCritico) {
				ator.getFantasmaNobre().onCriticalHit(ator, alvo, estado, this);
			}
		}
		// Hook de Dados Extras
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
			// Eco não gera stacks de raça
			aplicarDanoAoAlvo(ator, alvo, danoEco, false, TipoAcao.ECO, estado);
		}

		// Custo de Mana por Hit (Regra Geral)
		if (modHabilidade == 1.0 && danoTick > 0) {
			double custoMP = arma.getTipo().equalsIgnoreCase("Ranged") ? 1.0 : 2.0;
			ator.setManaAtual(ator.getManaAtual() + custoMP);
		}
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

	private int resolverSocoSerio(Personagem ator, List<Personagem> alvos, Habilidade habilidade, AcaoMestreInput input,
			EstadoCombate estado) {
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

			// Base
			double danoCalculado = arma.getDanoBase() * (1 + (0.075 * rolagemDadoAtributo));
			if (ator.getBonusDanoPercentual() > 0)
				danoCalculado *= (1.0 + ator.getBonusDanoPercentual());
			danoCalculado *= getMultiplicadorBonusDanoComArma(ator, arma, alvo, estado, input);

			double multiplicador;
			boolean isCrit = false;

			if (estaNaLinhaCentral) {
				multiplicador = 5.0; // 500%
				if (Math.random() < ator.getTaxaCritica()) {
					multiplicador *= (1.0 + ator.getDanoCritico());
					isCrit = true;
				}
			} else {
				multiplicador = 3.0; // 300%
			}

			double danoBruto = danoCalculado * multiplicador;

			double danoPosArmadura = aplicarReducaoArmadura(danoBruto, ator, alvo, estado);

			List<DamageEvent> eventos = new ArrayList<>();

			eventos.add(new DamageEvent(danoPosArmadura, estaNaLinhaCentral ? "EPICENTRO" : "ONDA", isCrit, null));

			matrizDeDanos.put(alvo, eventos);
		}

		// Abre Janela
		if (!matrizDeDanos.isEmpty()) {
			mainController.abrirJanelaResolucao(ator, alvos, habilidade, matrizDeDanos);
		}

		return 0;
	}

	public void resolverAcaoItem(AcaoMestreInput input, EstadoCombate estado) {
		Personagem ator = input.getAtor();
		Item item = input.getItemSendoUsado();

		if (item == null)
			return;

		// Usa o item (lógica de cura, etc. está no item)
		item.usar(ator, estado, mainController);

		// Remove o item do inventário
		ator.getInventario().removerItem(item);

		// Aplica custo de TU
		int custoTUFinal = calcularCustoTUFinal(ator, item.getCustoTU());
		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoTUFinal + " TUs para usar " + item.getNome() + ".");

		// Chama Hook de Ação (Elfo)
		chamarHookAcaoUsada(ator, TipoAcao.ITEM, estado);
	}

	private int calcularCustoManaFinal(Personagem conjurador, Habilidade habilidade, int custoManaBase) {
		if (habilidade != null && habilidade.getNome().equals("Fulgor Negro")
				&& conjurador.getEfeitosAtivos().containsKey("Restrição Celestial")) {
			return 0;
		}

		int custoFinal = custoManaBase;
		if (conjurador.getEfeitosAtivos().containsKey("Estado Dourado")) {
			custoFinal = Math.max((custoManaBase <= 0 ? 0 : 1), custoManaBase - 1);
		}
		return custoFinal;
	}

	private int calcularCustoTUFinal(Personagem conjurador, int custoTUBase) {
		return calcularCustoTUFinal(conjurador, custoTUBase, null, TipoAcao.OUTRO);
	}

	private int calcularCustoTUFinal(Personagem conjurador, int custoTUBase, Habilidade habilidade,
			TipoAcao tipoAcaoAtual) {
		double modTU = 1.0; // 100%
		int custoExtraFixo = 0;

		// Bônus 1: Estado Dourado
		if (conjurador.getEfeitosAtivos().containsKey("Estado Dourado")) {
			modTU -= 0.15; // Redução de 15%
			System.out.println(">>> Estado Dourado: Custo de TU reduzido!");
		}

		if (conjurador.getEfeitosAtivos().containsKey("Modo Engaged")) {
			modTU -= 0.30;
			System.out.println(">>> Modo Engaged: Custo de TU reduzido!");
		}

		if (conjurador.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble")) {
			modTU -= 0.25;
			System.out.println(">>> Idle Death Gamble: Custo de TU reduzido em 25%!");
		}

		if (conjurador.getEfeitosAtivos().containsKey("Lento")) {
			modTU += 0.50; // Exemplo: +50% custo
		}

		if (conjurador.getEfeitosAtivos().containsKey("Sono")) {
			int stacks = conjurador.getEfeitosAtivos().get("Sono").getStacks();
			custoExtraFixo += (stacks * 20);
		}

		if (conjurador.getEfeitosAtivos().containsKey("Vento Congelante")) {
			custoExtraFixo += 20;
		}

		if (conjurador.getRaca() != null) {
			double reducaoRaca = conjurador.getRaca().getReducaoTUPercentual(conjurador);
			if (reducaoRaca > 0) {
				modTU -= reducaoRaca;
			}
			custoExtraFixo += conjurador.getRaca().getCustoTUExtra(conjurador, habilidade, tipoAcaoAtual);
		}

		int custoFinalPercentual = (int) (custoTUBase * Math.max(0.0, modTU));
		return custoFinalPercentual + custoExtraFixo;
	}

	private void chamarHookAcaoUsada(Personagem ator, TipoAcao tipoAcaoAtual, EstadoCombate estado) {
		TipoAcao tipoAcaoAnterior = ultimoTipoAcaoPorPersonagem.getOrDefault(ator, TipoAcao.NENHUMA);

		if (ator.getRaca() != null) {
			ator.getRaca().onActionUsed(ator, tipoAcaoAnterior, tipoAcaoAtual, estado);
		}
		ultimoTipoAcaoPorPersonagem.put(ator, tipoAcaoAtual);
	}

	public void aplicarBuffDanoEstadoDourado(Personagem personagem) {
		String nomeEfeito = "Dano Dourado";
		double bonusPorStack = 0.05; // 5%
		int duracao = 300;
		Efeito efeitoExistente = personagem.getEfeitosAtivos().get(nomeEfeito);

		if (efeitoExistente != null) {
			double novoBonus = efeitoExistente.getModificadores().getOrDefault("DANO_BONUS_PERCENTUAL", 0.0)
					+ bonusPorStack;
			efeitoExistente.getModificadores().put("DANO_BONUS_PERCENTUAL", novoBonus);
			efeitoExistente.setDuracaoTURestante(duracao); // Reseta a duração
			efeitoExistente.setStacks(efeitoExistente.getStacks() + 1); // Incrementa stack
			System.out.println(">>> Dano Dourado acumulado! Stacks: " + efeitoExistente.getStacks() + ". Bônus total: +"
					+ (novoBonus * 100) + "%");
		} else {
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", bonusPorStack);
			Efeito novoBuffDano = new Efeito(nomeEfeito, TipoEfeito.BUFF, duracao, mods, 0, 0);
			novoBuffDano.setStacks(1); // Define stack inicial como 1
			this.aplicarEfeito(personagem, novoBuffDano);
		}

		// Dispara o recálculo de stats para atualizar o bonusDanoPercentual
		personagem.recalcularAtributosEstatisticas();
	}

	public void aplicarEfeito(Personagem alvo, Efeito efeito) {
		if (alvo == null || efeito == null)
			return;

		String nomeEf = efeito.getNome().toLowerCase();
		List<String> props = alvo.getPropriedades();

		if (props.contains("IMUNIDADE_DOT")) {
			if (efeito.getTipo() == TipoEfeito.DOT) {
				System.out.println(">>> IMUNE! " + alvo.getNome() + " é imune a DoT: " + efeito.getNome());
				return;
			}

			if (nomeEf.contains("sangramento") || nomeEf.contains("toxina") || nomeEf.contains("veneno")
					|| nomeEf.contains("queima") || nomeEf.contains("ruptura") || nomeEf.contains("dilacera")
					|| nomeEf.contains("podridão")) {

				System.out.println(">>> IMUNE! " + alvo.getNome() + " resistiu ao efeito: " + efeito.getNome());
				return;
			}
		}
		if (props.contains("IMUNIDADE_CONTROLE")) {
			// Lista de efeitos de controle de grupo (CC)
			if (nomeEf.contains("sono") || nomeEf.contains("dormindo") || nomeEf.contains("atordoado")
					|| nomeEf.contains("stun") || nomeEf.contains("medo") || nomeEf.contains("paralisia")
					|| nomeEf.contains("congelado")) {

				System.out.println(">>> IMUNE! " + alvo.getNome() + " ignorou o controle: " + efeito.getNome());
				return;
			}
		}

		if (alvo.isProtagonista()) {
			if (efeito.getTipo() == TipoEfeito.DEBUFF || efeito.getTipo() == TipoEfeito.DOT) {
				System.out
						.println(">>> PROTAGONISTA: " + alvo.getNome() + " ignorou o efeito ruim " + efeito.getNome());
				return;
			}
		}

		alvo.adicionarEfeito(efeito);
		System.out.println(">>> Efeito [" + efeito.getNome() + "] aplicado em " + alvo.getNome() + " ("
				+ efeito.getDuracaoTUInicial() + " TU).");
		alvo.recalcularAtributosEstatisticas(); // Continua essencial
	}

	private void aplicarEfeitosDaHabilidade(Personagem conjurador, Habilidade habilidade, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {
		// Pega as coordenadas do input (se disponível)
		if (lastInput != null) {
			habilidade.executar(conjurador, lastInput.getEpicentroX(), lastInput.getEpicentroY(), alvos, estado,
					manager);
		} else {
			// Fallback se não houver input salvo (ex: IA simples)
			habilidade.executar(conjurador, alvos, estado, manager);
		}
	}

	public void aplicarDanoAoAlvo(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado) {
		// Sobrecarga para manter compatibilidade com códigos antigos (dot, ambiente)
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

	private void aplicarDanoAoAlvoInterno(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal, boolean danoJaResolvido) {
		if (alvo == null || dano <= 0)
			return;

		if (alvo.getEfeitosAtivos().containsKey("Dormindo")) {
			System.out.println(">>> " + alvo.getNome() + " ACORDOU COM O GOLPE!");
			alvo.removerEfeito("Dormindo");
		}

		for (String prop : alvo.getPropriedades()) {
			if (prop.startsWith("VINCULO_DANO:")) {
				String nomeDono = prop.split(":")[1];

				// Encontra o dono original na lista
				Personagem dono = estado.getCombatentes().stream()
						.filter(p -> p.getNome().equals(nomeDono) && p.isAtivoNoCombate()).findFirst().orElse(null);

				if (dono != null) {
					double danoTransferido = dano * 0.50; // 50%
					if (danoTransferido >= 1.0) {
						System.out.println(
								">>> VUDU: " + danoTransferido + " de dano transferido para " + dono.getNome());
						// Aplica dano direto (tipo OUTRO para não gerar loop infinito ou reações)
						aplicarDanoAoAlvo(ator, dono, danoTransferido, true, TipoAcao.OUTRO, estado, 0);
					}
				}
			}
		}

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
				if (p.getEfeitosAtivos().containsKey("Guardião") && faccaoDoAlvo.equals(p.getFaccao()) && p != alvo) {
					int dist = Math.max(Math.abs(p.getPosX() - alvo.getPosX()), Math.abs(p.getPosY() - alvo.getPosY()));
					if (dist <= areaGuarda && dist < menorDistancia) {
						menorDistancia = dist;
						guardiaoEncontrado = p;
					}
				}
			}

			if (guardiaoEncontrado != null) {
				System.out.println(">>> [Auto-Guarda] " + guardiaoEncontrado.getNome()
						+ " interceptou o golpe destinado a " + alvo.getNome() + "!");
				alvo = guardiaoEncontrado; // Redireciona o alvo

				if (alvo.getEfeitosAtivos().containsKey("Stealth"))
					alvo.removerEfeito("Stealth");
			}
		}

		// Acorda alvos (Lógica Mantida)
		if (alvo.getEfeitosAtivos().containsKey("Pesadelo")) {
			dano = (int) (dano * 1.60);
			alvo.removerEfeito("Pesadelo");
		}
		if (alvo.getEfeitosAtivos().containsKey("Dormindo")) {
			alvo.removerEfeito("Dormindo");
		}

		if (!danoJaResolvido) {
			dano = aplicarReducaoDanoPreArmadura(dano, ator, alvo, estado);
			if (dano <= 0) {
				return;
			}
		}

		if (ator != null && ator.isVivo() && !ignoraEscudo) {
			int nivelVamp = ator.getValorPropriedade("VAMPIRISMO");
			if (nivelVamp > 0) {
				double curaVamp = dano * (nivelVamp / 100.0);
				if (curaVamp >= 1.0) {
					ator.setVidaAtual(ator.getVidaAtual() + curaVamp, estado, mainController);
					System.out.println(">>> VAMPIRISMO: " + ator.getNome() + " curou " + (int) curaVamp + " HP.");
				}
			}
		}

		// Escudo
		double danoRestante = dano;
		double escudoOriginal = alvo.getEscudoAtual();

		if (!ignoraEscudo && escudoOriginal > 0) {

			// LÓGICA DE ESCUDO DE SANGUE
			if (alvo.isEscudoDeSangue()) {
				// O Escudo de Sangue não tem redução de dano.

				double reducaoArmadura = alvo.getReducaoDanoArmadura() + alvo.getReducaoDanoTopor();
				if (alvo.getEfeitosAtivos().containsKey("Ruptura"))
					reducaoArmadura -= 0.25;

				// Evita divisão por zero ou números negativos absurdos
				double fatorReducao = Math.max(0.05, 1.0 - reducaoArmadura);

				// Dano Bruto Estimado = Dano Liquido / (1 - %Redução)
				double danoBrutoContraEscudo = dano / fatorReducao;

				System.out.println(">>> ESCUDO DE SANGUE: Ignorou armadura! Dano aumentado de " + (int) dano + " para "
						+ (int) danoBrutoContraEscudo);

				if (danoBrutoContraEscudo >= escudoOriginal) {
					// Quebrou o escudo!
					// O excedente deve ser aplicado na VIDA, mas a VIDA TEM ARMADURA.
					// Então convertemos o excedente bruto de volta para líquido.

					double excedenteBruto = danoBrutoContraEscudo - escudoOriginal;
					double excedenteLiquido = excedenteBruto * fatorReducao; // Reaplica armadura no resto

					alvo.setEscudoAtual(0);
					// Se o escudo quebrou, remove a flag de sangue
					alvo.setTemEscudoDeSangue(false);

					danoRestante = excedenteLiquido;
					System.out.println(">>> Escudo de Sangue Quebrou! Dano restante na vida: " + (int) danoRestante);

				} else {
					// Tankou tudo no escudo
					alvo.setEscudoAtual(escudoOriginal - danoBrutoContraEscudo);
					danoRestante = 0;
				}

			}
			// LÓGICA DE ESCUDO COMUM
			else {
				if (danoRestante >= escudoOriginal) {
					danoRestante -= escudoOriginal;
					alvo.setEscudoAtual(0);
					System.out.println(">>> Escudo comum quebrou!");
				} else {
					alvo.setEscudoAtual(escudoOriginal - danoRestante);
					danoRestante = 0;
				}
			}
		}
		if (danoRestante > 0) {
			double vidaPre = alvo.getVidaAtual();

			// Aplica na Vida (Redução de armadura já foi feita na Janela ou no cálculo do DoT)
			alvo.setVidaAtual(vidaPre - danoRestante, estado, mainController);
			double danoReal = vidaPre - alvo.getVidaAtual();

			alvo.registrarDanoSofrido(danoReal, estado.getTempoGlobalCombate());

			System.out
					.println(">>> DANO APLICADO: " + alvo.getNome() + " -" + String.format("%.1f", danoReal) + " HP.");
			br.com.dantesrpg.model.util.SessionLogger
					.log(alvo.getNome() + " sofreu " + String.format("%.1f", danoReal) + " de dano.");

			if (vidaPre > 0 && !alvo.isAtivoNoCombate() && !alvo.isVivo()) {
				for (String prop : alvo.getPropriedades()) {
					if (prop.startsWith("VINCULO_CURA_MORTE:")) {
						String nomeDono = prop.split(":")[1];
						Personagem dono = estado.getCombatentes().stream().filter(p -> p.getNome().equals(nomeDono))
								.findFirst().orElse(null);

						if (dono != null) {
							double cura = dono.getVidaMaxima() * 0.05; // 5% Max HP
							dono.setVidaAtual(dono.getVidaAtual() + cura, estado, mainController);
							System.out.println(">>> RETORNO VITAL: " + dono.getNome() + " curou " + (int) cura
									+ " HP com a destruição da essência.");
						}
					}
				}
				// XP
				if (alvo.getXpReward() > 0)
					estado.adicionarXpAoPool(alvo.getXpReward());

				if (alvo.getPropriedades().contains("EXPLOSIVO")) {
					System.out.println(">>> PROPRIEDADE ATIVADA: " + alvo.getNome() + " EXPLODIU!");
					br.com.dantesrpg.model.util.SessionLogger.log("💥 " + alvo.getNome() + " explode ao morrer!");

					int danoExplosao = (int) (alvo.getVidaMaximaBase() * 0.50); // 20% da vida máx como dano

					Habilidade explosaoDummy = new Habilidade("Explosão", "Dano ao morrer", null, 0, 0, 0,
							br.com.dantesrpg.model.enums.TipoAlvo.AREA_QUADRADA, 3, // Tamanho 3 (3x3)
							0, 0, null) {
						@Override
						public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
								CombatManager manager) {

						}
					};

					// Busca as vitimas usando o objeto dummy criado acima
					List<Personagem> vitimas = mainController.getMapController().encontrarAlvosNaForma(alvo.getPosX(),
							alvo.getPosY(), explosaoDummy, alvo);

					for (Personagem v : vitimas) {
						// Dano direto e inevitável
						if (v.isAtivoNoCombate()) {
							aplicarDanoAoAlvo(alvo, v, danoExplosao, false, TipoAcao.OUTRO, estado, 0);
						}
					}
				}

				if (alvo.getPropriedades().contains("EXPLODIR") || alvo.getNome().contains("Larva")) {

					System.out.println(">>> " + alvo.getNome() + " EXPLODIU EM CHAMAS!");
					br.com.dantesrpg.model.util.SessionLogger
							.log("💥 " + alvo.getNome() + " deixou um rastro de magma!");

					if (mainController != null && mainController.getMapController() != null) {
						mainController.getMapController().criarAreaDeFogo(alvo.getPosX(), alvo.getPosY(), 1, 8, alvo);
					}

				}

				if (ator != null && ator.getArmaEquipada() != null
						&& ator.getArmaEquipada().getNome().equals("Murasame")) {
					System.out.println(">>> MURASAME (KILL): Alma capturada!");
					ator.getInventario().adicionarItem(new br.com.dantesrpg.model.items.EssenciaInimigo(alvo));
					br.com.dantesrpg.model.util.SessionLogger
							.log(ator.getNome() + " capturou a alma de " + alvo.getNome());
					if (mainController != null)
						mainController.atualizarInterfaceTotal();
				}

				// Clone Morrendo -> Stealth no Criador
				if (alvo.isClone()) {
					processarMorteClone(alvo, estado);
				}

				// Objeto Destrutível
				if (alvo instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
					System.out.println(">>> ESTRUTURA DESTRUÍDA: " + alvo.getNome());
					if (mainController != null && mainController.getMapController() != null) {
						mainController.getMapController().atualizarCelulaParaChao(alvo.getPosX(), alvo.getPosY());
					}
				}

				if (mainController != null)
					mainController.atualizarInterfaceAposMorte();
			}

			// Hooks de Reação (Anjo Caído, Pálida Vigília, Marca do Deserto)
			if (alvo.getRaca() != null) {
				alvo.getRaca().onDamageTaken(alvo, ator, danoReal, estado, this);
			}
			if (alvo.getArmaEquipada() != null) {
				alvo.getArmaEquipada().onDamageTaken(alvo, danoReal, estado, mainController);
			}

			// Marca do Deserto (Como Reação)
			if (alvo.getEfeitosAtivos().containsKey("Marca do Deserto") && mainController != null
					&& tipoAcaoDano != TipoAcao.REACAO_FANTASMA) {
				if (ator != alvo) {
					// Abre o pop-up para rolar 1d4 e causar dano extra recursivo
					mainController.solicitarRolagemFantasmaDoDeserto(ator, alvo);
				}
			}
		}
	}

	public void atualizarAuras(EstadoCombate estado) {
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
			// Limpeza se Darrell não estiver ativo
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
			int raioZero = 3; // 6x6 (Raio 3 do centro)
			int zx = zeraphon.getPosX();
			int zy = zeraphon.getPosY();

			// Debuff: +30% Dano Sofrido (Redução Negativa), -50% Dano Causado
			Map<String, Double> modsZero = new HashMap<>();
			modsZero.put("REDUCAO_DANO_MODIFICADOR", -0.30);
			modsZero.put("DANO_BONUS_PERCENTUAL", -0.50);

			for (Personagem p : estado.getCombatentes()) {
				// Afeta INIMIGOS do Zeraphon
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
			// Limpeza se Zeraphon morrer ou perder a aura
			for (Personagem p : estado.getCombatentes()) {
				if (p.getEfeitosAtivos().containsKey("O Vazio")) {
					p.removerEfeito("O Vazio");
					p.recalcularAtributosEstatisticas();
				}
			}
		}

		Map<Personagem, Integer> contagemSelos = new HashMap<>();

		for (Personagem p : estado.getCombatentes()) {
			if (p.isAtivoNoCombate()) {
				for (String prop : p.getPropriedades()) {
					if (prop.startsWith("PORTADOR_SELO:")) {
						String nomeMestre = prop.split(":")[1].trim();

						// Busca o mestre ignorando maiúsculas/minúsculas e espaços
						Personagem mestre = estado.getCombatentes().stream()
								.filter(m -> m.getNome().trim().equalsIgnoreCase(nomeMestre)).findFirst().orElse(null);

						if (mestre != null) {
							contagemSelos.put(mestre, contagemSelos.getOrDefault(mestre, 0) + 1);
						}
					}
				}
			}
		}

		// Aplica o bônus nos Mestres identificados
		for (Personagem mestre : estado.getCombatentes()) {
			int qtdSelos = contagemSelos.getOrDefault(mestre, 0);
			String keyBuff = "Vínculo de Selo";

			Efeito efeitoExistente = mestre.getEfeitosAtivos().get(keyBuff);

			if (qtdSelos > 0) {

				if (efeitoExistente == null) {
					// Cria pela primeira vez
					Map<String, Double> mods = new HashMap<>();
					mods.put("MP_MAXIMO", (double) qtdSelos);
					Efeito novoBuff = new Efeito(keyBuff, TipoEfeito.BUFF, 9999, mods, 0, 0);
					novoBuff.setStacks(qtdSelos);
					mestre.adicionarEfeito(novoBuff);
				} else {
					// APENAS ATUALIZA O VALOR, NÃO CHAMA adicionarEfeito
					if (efeitoExistente.getStacks() != qtdSelos) {
						efeitoExistente.setStacks(qtdSelos);
						efeitoExistente.getModificadores().put("MP_MAXIMO", (double) qtdSelos);
						mestre.recalcularAtributosEstatisticas(); // Aplica a mudança
						System.out.println(">>> SELOS: Atualizado para " + qtdSelos);
					}
				}
			} else if (efeitoExistente != null) {
				mestre.removerEfeito(keyBuff);
			}
		}
	}

	private void verificarManaPassivaModoJustica(Personagem ator, EstadoCombate estado) {
		// Se quem agiu foi o Darrell, ignora
		if (ator.getEfeitosAtivos().containsKey("Modo Justiça"))
			return;

		// Procura Darrell
		Personagem darrell = null;
		for (Personagem p : estado.getCombatentes()) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Modo Justiça")) {
				darrell = p;
				break;
			}
		}
		if (darrell == null)
			return;

		// Verifica se são aliados
		if (!ator.getFaccao().equals(darrell.getFaccao()))
			return;

		// Verifica se o ATOR está dentro da área (tem o buff)
		if (ator.getEfeitosAtivos().containsKey("Bênção da Justiça")) {
			System.out.println(">>> MODO JUSTIÇA (Passiva): Darrell ganha +1 Mana pela ação de " + ator.getNome());
			darrell.setManaAtual(darrell.getManaAtual() + 1);
		}
	}

	public void executarAtaqueCoordenado(Map<Personagem, Personagem> ataques, Habilidade habilidade, int rolagemGlobal,
			ModoAtaque modoAtaque, int tirosExtras, EstadoCombate estado, List<Personagem> todosOsClonesDoEsquadrao) {
		System.out.println("=== EXECUTANDO ACAO COORDENADA DE CLONES ===");

		if (todosOsClonesDoEsquadrao == null || todosOsClonesDoEsquadrao.isEmpty())
			return;

		Map<Personagem, Integer> tuInicialPorClone = new HashMap<>();
		for (Personagem clone : todosOsClonesDoEsquadrao) {
			if (clone == null || !clone.isAtivoNoCombate())
				continue;

			int tuInicial = clone.getContadorTU();
			tuInicialPorClone.put(clone, tuInicial);
		}

		if (tuInicialPorClone.isEmpty())
			return;

		int somaTotalTU = 0;
		int numeroAcoes = 0;
		boolean exigeAlvo = ataques != null && !ataques.isEmpty();

		if (!exigeAlvo) {
			for (Personagem clone : todosOsClonesDoEsquadrao) {
				if (!tuInicialPorClone.containsKey(clone))
					continue;

				System.out.println("   -> Clone " + clone.getNome() + " executa "
						+ (habilidade != null ? habilidade.getNome() : "Ataque Basico") + ".");

				AcaoMestreInput input = new AcaoMestreInput(clone, new ArrayList<>(), habilidade);
				input.adicionarResultadoDado("DADO_ATRIBUTO", rolagemGlobal);
				input.setModoAtaque(modoAtaque != null ? modoAtaque : ModoAtaque.NORMAL);
				input.setTirosExtras(Math.max(0, tirosExtras));

				resolverAcao(input, estado);

				int custoDestaAcao = Math.max(0, clone.getContadorTU() - tuInicialPorClone.get(clone));
				somaTotalTU += custoDestaAcao;
				numeroAcoes++;
			}
		}

		Map<Personagem, List<Personagem>> alvosAgrupados = new HashMap<>();
		if (exigeAlvo) {
			for (Map.Entry<Personagem, Personagem> ataque : ataques.entrySet()) {
				Personagem clone = ataque.getKey();
				Personagem alvo = ataque.getValue();

				if (!tuInicialPorClone.containsKey(clone) || alvo == null || !alvo.isAtivoNoCombate())
					continue;

				alvosAgrupados.computeIfAbsent(alvo, key -> new ArrayList<>()).add(clone);
			}
		}

		// Resolve por Alvo e Calcula Custos (SEM aplicar ainda)
		for (Map.Entry<Personagem, List<Personagem>> entry : alvosAgrupados.entrySet()) {
			Personagem alvo = entry.getKey();
			List<Personagem> atacantes = entry.getValue();

			for (Personagem atacante : atacantes) {
				System.out.println("   -> Clone " + atacante.getNome() + " ataca " + alvo.getNome());

				AcaoMestreInput input = new AcaoMestreInput(atacante, List.of(alvo), habilidade);
				input.adicionarResultadoDado("DADO_ATRIBUTO", rolagemGlobal);

				input.setModoAtaque(modoAtaque != null ? modoAtaque : ModoAtaque.NORMAL);
				input.setTirosExtras(Math.max(0, tirosExtras));

				resolverAcao(input, estado);

				int custoDestaAcao = Math.max(0, atacante.getContadorTU() - tuInicialPorClone.get(atacante));
				if (false) {

					// Lógica de Habilidade
					resolverDanoPadrao(atacante, atacante.getArmaEquipada(), rolagemGlobal, List.of(alvo),
							habilidade.getMultiplicadorDeDano(), TipoAcao.HABILIDADE, habilidade, estado, input);
					custoDestaAcao = habilidade.getCustoTU();
				} else if (false) {
					// Lógica de Ataque Básico
					resolverDanoPadrao(atacante, atacante.getArmaEquipada(), rolagemGlobal, List.of(alvo), 1.0,
							TipoAcao.ATAQUE_BASICO, null, estado, input);
					// Pega o custo da arma ou padrão 100
					custoDestaAcao = (atacante.getArmaEquipada() != null) ? atacante.getArmaEquipada().getCustoTU()
							: 100;
				}

				// Calcula reduções (ex: Estado Dourado, etc) sem aplicar no personagem ainda
				custoDestaAcao = calcularCustoTUFinal(atacante, custoDestaAcao);

				custoDestaAcao = Math.max(0, atacante.getContadorTU() - tuInicialPorClone.get(atacante));

				somaTotalTU += custoDestaAcao;
				numeroAcoes++;
			}
		}

		// Calcula a Média
		int mediaTU = 0;
		if (numeroAcoes > 0) {
			mediaTU = somaTotalTU / numeroAcoes;
		} else {
			// Se ninguém atacou (turno vazio?), aplica um custo base para não travar em
			// loop infinito
			mediaTU = 50;
		}

		System.out.println(
				">>> SQUAD FINAL: Média de TU calculada: " + mediaTU + " (Baseado em " + numeroAcoes + " ações)");

		// APLICA A MÉDIA A TODOS OS CLONES (Sincronização)
		if (todosOsClonesDoEsquadrao != null) {
			for (Personagem clone : todosOsClonesDoEsquadrao) {
				// Só aplica se o clone estiver vivo/ativo
				Integer tuInicial = tuInicialPorClone.get(clone);
				if (clone.isAtivoNoCombate() && tuInicial != null) {
					clone.setContadorTU(tuInicial + mediaTU);
					System.out.println("   -> " + clone.getNome() + " avançou +" + mediaTU + " TU.");
				}
			}
		}

		// Limpa destaque visual
		if (mainController != null && mainController.getMapController() != null) {
			mainController.getMapController().limparDestaquesPeoes();
		}
	}

	public void distribuirXpAposCombate(EstadoCombate estado) {
		int xpTotal = estado.sacarXpDoPool();

		System.out.println("DEBUG XP: Tentando distribuir. Pool: " + xpTotal);

		if (xpTotal <= 0) {
			br.com.dantesrpg.model.util.SessionLogger.log("Fim de combate. Nenhum XP acumulado.");
			return;
		}

		List<Personagem> jogadoresVivos = estado.getCombatentes().stream()
				.filter(p -> p.getFaccao().equals("JOGADOR") && p.isAtivoNoCombate() && !p.isClone())
				.collect(Collectors.toList());

		if (jogadoresVivos.isEmpty())
			return;
		int xpPorCabeca = xpTotal;

		System.out.println("\n=== DISTRIBUIÇÃO DE XP (" + xpTotal + " XP para CADA jogador) ===");
		br.com.dantesrpg.model.util.SessionLogger.log("Cada jogador recebeu " + xpPorCabeca + " XP.");

		for (Personagem p : jogadoresVivos) {
			p.ganharExperiencia(xpPorCabeca);
		}
	}

	public TipoAcao getUltimoTipoAcao(Personagem p) {
		return ultimoTipoAcaoPorPersonagem.getOrDefault(p, TipoAcao.NENHUMA);
	}

	public void setUltimoTipoAcao(Personagem p, TipoAcao tipo) {
		ultimoTipoAcaoPorPersonagem.put(p, tipo);
	}

	public boolean aplicarDanoFantasmaDoDeserto(Personagem ator, Personagem alvo, int rolagem1d4, int nivelCascata,
			EstadoCombate estado) {

		// Regra: Falha com 2 ou menos
		if (rolagem1d4 <= 2) {
			System.out.println(">>> Fantasma do Deserto (1d4=" + rolagem1d4 + "): Falha. Cascata interrompida.");
			return false; // Para a cascata
		}

		System.out.println(">>> Fantasma do Deserto (1d4=" + rolagem1d4 + "): SUCESSO! (Nível " + nivelCascata + ")");

		if (ator.getArmaEquipada() == null) {
			System.err.println("Erro Fantasma do Deserto: Ator está desarmado.");
			return false;
		}

		double danoCalculado = ator.getArmaEquipada().getDanoBase() * 0.50;
		danoCalculado *= getMultiplicadorBonusDanoComArma(ator, ator.getArmaEquipada(), alvo, estado, null);
		int danoTick = Math.max(1, (int) danoCalculado);

		aplicarDanoAoAlvo(ator, alvo, danoTick, false, TipoAcao.REACAO_FANTASMA, estado);

		return true;
	}

	private void checarEfeitosDeInicioDeTurno(Personagem ator, EstadoCombate estado) {

		// Chama o hook de início de turno da Raça (para Elfos, etc.)
		if (ator.getRaca() != null) {
			ator.getRaca().onTurnStart(ator, estado);
		}
		if (ator.getFantasmaNobre() != null) {
			ator.getFantasmaNobre().onTurnStart(ator, estado, this);
		}

		// Verifica se o Ringue do Alexei está sendo preparado
		if (ator.getEfeitosAtivos().containsKey("Ringue (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'O Ringue da Vontade Inquebrantável'!");

			// Remove o efeito de preparação
			ator.removerEfeito("Ringue (Preparando)");

			// Aplica o efeito real (duração de 200 TU)
			Efeito efeitoRingue = new Efeito("Ringue da Vontade", // O nome que a Arma (PunhoInfinito) verifica
					TipoEfeito.BUFF, 400, // Duração real do ringue
					Map.of(), 0, 0);
			aplicarEfeito(ator, efeitoRingue); // Usa o método do CombatManager
			// Chama o CombatController para desenhar a zona 7x7
			if (mainController != null) {
				mainController.desenharRingueDoMapa(ator, 7); // 7x7
			}
		}

		if (ator.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble (Preparando)")) {
			System.out.println(">>> Início do Turno: Ativando 'Idle Death Gamble'!");

			ator.removerEfeito("Domínio: Idle Death Gamble (Preparando)");

			br.com.dantesrpg.model.fantasmasnobres.ApostadorIncansavel.ativarEfeitoDominio(ator);
			// Desenha no mapa
			if (mainController != null) {
				mainController.desenharDominioLyriaNoMapa(ator);
			}
		}
	}

	public void solicitarSpawnClone(Personagem invocador) {
		if (mainController != null) {
			mainController.spawnarCloneIlusao(invocador);
		}
	}

	public boolean habilidadePodeSerCopiadaPorClone(Habilidade habilidade) {
		if (habilidade == null)
			return false;

		return !(habilidade instanceof Ilusao) && !(habilidade instanceof Borderline);
	}

	private Habilidade normalizarHabilidadeCopiavelParaClone(Habilidade habilidade) {
		return habilidadePodeSerCopiadaPorClone(habilidade) ? habilidade : null;
	}

	public void processarMorteClone(Personagem clone, EstadoCombate estado) {
		if (clone == null || !clone.isClone())
			return;

		Personagem criador = clone.getCriador();
		if (criador != null) {
			if (criador.isAtivoNoCombate()) {
				System.out.println(">>> Clone destruido! " + criador.getNome() + " ganha Stealth!");
				Efeito stealth = new Efeito("Stealth", TipoEfeito.BUFF, 9999, Map.of(), 0, 0);
				aplicarEfeito(criador, stealth);
			}

			criador.removerCloneMorto(clone);
		}

		if (estado != null) {
			estado.getCombatentes().remove(clone);
		}
	}

	public void resolverAcaoRecarregar(Personagem ator) {
		Arma arma = ator.getArmaEquipada();
		if (arma != null && arma.isRequerMunicao()) {
			System.out.println(">>> " + ator.getNome() + " está recarregando " + arma.getNome() + "...");
			arma.recarregar();
			int custoRecarga = arma.getMunicaoMaxima() * 10;
			ator.setContadorTU(ator.getContadorTU() + custoRecarga);
			System.out.println(">>> " + arma.getNome() + " recarregada! (Custo: " + custoRecarga + " TU)");

			chamarHookAcaoUsada(ator, TipoAcao.RECARREGAR, null);
		}
	}

	public void resolverTerrenoPerigoso(Personagem ator, MapController map, EstadoCombate estado) {
		// 1. Pega o efeito no pé do personagem
		EfeitoInstance efeitoSolo = map.getEfeitoNoSolo(ator.getPosX(), ator.getPosY());

		if (efeitoSolo == null)
			return;

		// Se o efeito já expirou (e não é permanente), remove e ignora
		if (efeitoSolo.expirou()) {
			return;
		}

		// --- LÓGICA DO FOGO ---
		if (efeitoSolo.getTipo() == TipoEfeitoSolo.FOGO) {
			System.out.println(">>> ALERTA: " + ator.getNome() + " pisou em chamas!");

			int danoBase = efeitoSolo.getDanoPorTick();

			// Dano Imediato (Ao pisar)
			aplicarDanoAoAlvo(efeitoSolo.getCriador(), ator, danoBase, true, TipoAcao.OUTRO, estado, 0);

			if (ator.getEfeitosAtivos().containsKey("Queimação")) {
				Efeito existente = ator.getEfeitosAtivos().get("Queimação");
				existente.setDuracaoTURestante(100);
				System.out.println(">>> Debuff Queimação renovado.");
			} else {
				Efeito queimacao = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Queimação", 200, 10);
				aplicarEfeito(ator, queimacao);
			}
		}

	}

	public CombatController getMainController() {
		return this.mainController;
	}

	public CombatController getController() {
		return this.mainController;
	}

	private List<Personagem> encontrarAlvosAoRedorDoAtor(Personagem ator, Habilidade habilidade, EstadoCombate estado) {
		List<Personagem> alvosEncontrados = new ArrayList<>();
		int alcance = habilidade.getTamanhoArea(); // Usamos 'TamanhoArea' como 'Raio'
		boolean global = (alcance >= 99); // 99+ significa alcance global

		for (Personagem p : estado.getCombatentes()) {
			if (!p.isAtivoNoCombate())
				continue; // Pula mortos

			// Filtra por facção
			boolean isAliado = p.getFaccao().equals(ator.getFaccao());

			if (p == ator && !habilidade.afetaSiMesmo())
				continue;
			if (isAliado && !habilidade.afetaAliados())
				continue;
			if (!isAliado && !habilidade.afetaInimigos())
				continue;

			if (global) {
				alvosEncontrados.add(p); // Alcance global, adiciona
			} else {
				int dist = Math.abs(p.getPosX() - ator.getPosX()) + Math.abs(p.getPosY() - ator.getPosY());
				if (dist <= alcance) {
					alvosEncontrados.add(p);
				}
			}
		}
		return alvosEncontrados;
	}

}
