package br.com.dantesrpg.model;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.combat.AuraManager;
import br.com.dantesrpg.model.combat.DamageApplicator;
import br.com.dantesrpg.model.combat.DamageCalculator;
import br.com.dantesrpg.model.combat.DomainManager;
import br.com.dantesrpg.model.combat.EffectProcessor;
import br.com.dantesrpg.model.combat.KnockbackProcessor;
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
import br.com.dantesrpg.model.map.TerrainData.EfeitoInstance;
import br.com.dantesrpg.model.map.TerrainData.TipoEfeitoSolo;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CombatManager {
	private static final String PROPRIEDADE_MALDICAO_AO_MORRER = "MALDICAO_AO_MORRER";
	private static final String PROPRIEDADE_MALDITO = "MALDITO";
	private static final int RAIO_ARISE = 1;
	private static final int DURACAO_FOGO_AMALDICOADO_TU = 50;
	private static final double PERCENTUAL_MALDICAO_FOGO_AMALDICOADO = 0.25;

	// ========== CAMPOS ==========

	private Map<Personagem, TipoAcao> ultimoTipoAcaoPorPersonagem = new HashMap<>();
	private Random random = new Random();
	private CombatController mainController;
	private AcaoMestreInput lastInput;
	private Personagem atorAtualAnterior;
	private TipoAcao ultimoTipoAcao = TipoAcao.MOVIMENTO;
	private int duracaoChuvaRestante = 0;
	private Runnable pendingMunicaoConsumption;

	// Subsistemas extraídos
	private final DamageCalculator damageCalculator;
	private final DamageApplicator damageApplicator;
	private final EffectProcessor effectProcessor;
	private final AuraManager auraManager;
	private final DomainManager domainManager;
	private final KnockbackProcessor knockbackProcessor;

	private static final List<DefinicaoPacoteMunicao> DEFINICOES_PACOTES_MUNICAO = List.of(
			new DefinicaoPacoteMunicao("GG", Integer.MAX_VALUE, 160),
			new DefinicaoPacoteMunicao("G", 20, 100),
			new DefinicaoPacoteMunicao("M", 10, 60),
			new DefinicaoPacoteMunicao("P", 5, 40));

	private record DefinicaoPacoteMunicao(String tamanho, int balasConcedidas, int custoTU) {
	}

	public record PacoteMunicao(String tipoItem, String nome, String tamanho, int balasConcedidas, int custoTU,
			int quantidade) {

		public boolean recarregaAteOMaximo() {
			return balasConcedidas == Integer.MAX_VALUE;
		}

		@Override
		public String toString() {
			String balas = recarregaAteOMaximo() ? "munição inteira" : balasConcedidas + " balas";
			return nome + " (x" + quantidade + ", " + balas + ", " + custoTU + " TU)";
		}
	}

	// ========== CONSTRUTOR ==========

	public CombatManager(CombatController controller) {
		this.mainController = controller;
		this.damageCalculator = new DamageCalculator(this);
		this.damageApplicator = new DamageApplicator(this);
		this.effectProcessor = new EffectProcessor(this);
		this.auraManager = new AuraManager(this);
		this.domainManager = new DomainManager(this);
		this.knockbackProcessor = new KnockbackProcessor(this);
	}

	// ========== GETTERS DOS SUBSISTEMAS ==========

	public DamageCalculator getDamageCalculator() {
		return damageCalculator;
	}

	public DamageApplicator getDamageApplicator() {
		return damageApplicator;
	}

	public EffectProcessor getEffectProcessor() {
		return effectProcessor;
	}

	public AuraManager getAuraManager() {
		return auraManager;
	}

	public DomainManager getDomainManager() {
		return domainManager;
	}

	public KnockbackProcessor getKnockbackProcessor() {
		return knockbackProcessor;
	}

	// ========== GETTERS / SETTERS ==========

	public AcaoMestreInput getLastInput() {
		return this.lastInput;
	}

	public CombatController getMainController() {
		return this.mainController;
	}

	public CombatController getController() {
		return this.mainController;
	}

	public TipoAcao getUltimoTipoAcao(Personagem p) {
		return ultimoTipoAcaoPorPersonagem.getOrDefault(p, TipoAcao.NENHUMA);
	}

	public void setUltimoTipoAcao(Personagem p, TipoAcao tipo) {
		ultimoTipoAcaoPorPersonagem.put(p, tipo);
	}

	public void setPendingMunicaoConsumption(Runnable r) {
		this.pendingMunicaoConsumption = r;
	}

	/**
	 * Intercepta a morte dos inimigos marcados para retornarem pelo ARISE.
	 */
	public boolean colocarEmEsperaParaArise(Personagem alvo, EstadoCombate estado) {
		if (alvo == null || estado == null || alvo.getValorPropriedade(PROPRIEDADE_MALDICAO_AO_MORRER) <= 0
				|| alvo.getValorPropriedade(PROPRIEDADE_MALDITO) > 0) {
			return false;
		}

		boolean aguardandoArise = estado.colocarEmEsperaParaArise(alvo);
		if (aguardandoArise) {
			System.out.println(">>> " + alvo.getNome() + " sucumbiu à maldição e aguarda o ARISE.");
		}
		return aguardandoArise;
	}

	/**
	 * Restaura todos os inimigos aguardando ARISE como versões malditas e
	 * desencadeia a explosão de maldição na área onde cada um caiu.
	 */
	public int ativarArise(EstadoCombate estado) {
		if (estado == null || estado.getInimigosAguardandoArise().isEmpty()) {
			return 0;
		}

		Map<Personagem, EstadoCombate.PosicaoMorteAmaldicoada> aguardando = estado.getInimigosAguardandoArise();
		for (Map.Entry<Personagem, EstadoCombate.PosicaoMorteAmaldicoada> entrada : aguardando.entrySet()) {
			Personagem inimigo = entrada.getKey();
			EstadoCombate.PosicaoMorteAmaldicoada posicaoMorte = entrada.getValue();
			if (inimigo == null) {
				continue;
			}

			inimigo.setPosX(posicaoMorte.x());
			inimigo.setPosY(posicaoMorte.y());
			removerDotsAtivos(inimigo);
			br.com.dantesrpg.model.util.MaldicaoUtils.purificarMaldicoes(inimigo);

			boolean aplicouVersaoMaldita = mainController != null
					&& mainController.aplicarVersaoMaldita(inimigo);
			if (!aplicouVersaoMaldita) {
				inimigo.getPropriedades().removeIf(prop -> prop.equals(PROPRIEDADE_MALDICAO_AO_MORRER));
				inimigo.adicionarPropriedade(PROPRIEDADE_MALDITO);
			}
			if (!aplicouVersaoMaldita && !inimigo.getNome().endsWith(" Maldito")) {
				inimigo.setNome(inimigo.getNome() + " Maldito");
			}
			inimigo.setVidaAtual(inimigo.getVidaMaxima(), estado, mainController);
			inimigo.setContadorTU(obterTuDeRetorno(estado));

			criarFogoAmaldicoado(posicaoMorte, inimigo);
			aplicarExplosaoAmaldicoada(posicaoMorte, inimigo, estado);
			estado.removerDaEsperaArise(inimigo);
			System.out.println(">>> ARISE: " + inimigo.getNome() + " retornou como inimigo maldito.");
		}

		if (mainController != null) {
			mainController.atualizarInterfaceTotal();
		}
		return aguardando.size();
	}

	private int obterTuDeRetorno(EstadoCombate estado) {
		Personagem atorAtual = estado.getAtorAtual();
		return (atorAtual != null ? atorAtual.getContadorTU() : estado.getTickCounter()) + 100;
	}

	private void criarFogoAmaldicoado(EstadoCombate.PosicaoMorteAmaldicoada posicao, Personagem criador) {
		if (mainController != null && mainController.getMapController() != null) {
			mainController.getMapController().criarAreaDeFogoAmaldicoado(posicao.x(), posicao.y(), RAIO_ARISE,
					DURACAO_FOGO_AMALDICOADO_TU, criador);
		}
	}

	private void aplicarExplosaoAmaldicoada(EstadoCombate.PosicaoMorteAmaldicoada posicao, Personagem origem,
			EstadoCombate estado) {
		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem == null || !"JOGADOR".equalsIgnoreCase(personagem.getFaccao())
					|| !personagem.isAtivoNoCombate()) {
				continue;
			}
			int distancia = Math.max(Math.abs(personagem.getPosX() - posicao.x()),
					Math.abs(personagem.getPosY() - posicao.y()));
			if (distancia <= RAIO_ARISE) {
				aplicarMaldicaoDoFogoAmaldicoado(personagem, origem.getNome());
			}
		}
	}

	private void removerDotsAtivos(Personagem personagem) {
		List<String> dots = personagem.getEfeitosAtivos().entrySet().stream()
				.filter(entrada -> entrada.getValue() != null && entrada.getValue().getTipo() == TipoEfeito.DOT)
				.map(Map.Entry::getKey)
				.toList();
		for (String nomeDot : dots) {
			personagem.removerEfeito(nomeDot);
		}
	}

	public void aplicarMaldicaoDoFogoAmaldicoado(Personagem alvo, String fonte) {
		if (alvo == null || !"JOGADOR".equalsIgnoreCase(alvo.getFaccao())) {
			return;
		}
		br.com.dantesrpg.model.util.MaldicaoUtils.adicionarMaldicao(alvo,
				new br.com.dantesrpg.model.util.Maldicao("Fogo Amaldiçoado de " + fonte,
						PERCENTUAL_MALDICAO_FOGO_AMALDICOADO, DURACAO_FOGO_AMALDICOADO_TU, false));
	}

	// ========== MUNIÇÃO PENDENTE ==========

	public void confirmarMunicaoPendente() {
		if (pendingMunicaoConsumption != null) {
			pendingMunicaoConsumption.run();
			pendingMunicaoConsumption = null;
		}
	}

	public void cancelarMunicaoPendente() {
		if (pendingMunicaoConsumption != null) {
			System.out.println(">>> ARMA: Resolução cancelada. Munição não consumida.");
			pendingMunicaoConsumption = null;
		}
	}

	// ========== TURNO ==========

	public void proximoTurno(EstadoCombate estado) {
		if (!estado.isCombateAtivo())
			return;

		Personagem proximoAtor = estado.getCombatentes().stream().filter(p -> p.isAtivoNoCombate())
				.min(java.util.Comparator.comparingInt(Personagem::getContadorTU)
						.thenComparing((p1, p2) -> Boolean.compare(p2.isProtagonista(), p1.isProtagonista()))
						.thenComparing((p1, p2) -> Integer.compare(p2.getPlacarIniciativa(), p1.getPlacarIniciativa())))
				.orElse(null);

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

		auraManager.atualizarAuras(estado);

		// Salva este ator como o anterior para o próximo ciclo
		this.atorAtualAnterior = proximoAtor;

		// Avança o tempo global se necessário (para Spawns, cooldowns globais, etc)
		int tempoParaAvancar = proximoAtor.getContadorTU();
		if (tempoParaAvancar > 0) {
			avancarTempo(tempoParaAvancar, estado);
		}

		estado.setAtorAtual(proximoAtor);
		Personagem atual = estado.getAtorAtual();

		if (atual.getEfeitosAtivos().containsKey("Dormindo")) {
			Efeito dormindo = atual.getEfeitosAtivos().get("Dormindo");

			System.out.println(">>> " + atual.getNome() + " está dormindo... Zzz... (Restam "
					+ dormindo.getDuracaoTURestante() + " TU)");

			// Adiciona penalidade de TU (Pula a vez)
			atual.setContadorTU(atual.getContadorTU() + 100);

			// Se expirou por duração (300 TU), acorda
			if (dormindo.expirou()) {
				System.out.println(">>> " + atual.getNome() + " acordou naturalmente!");
				atual.removerEfeito("Dormindo");
			}

			// Passa para o próximo imediatamente
			proximoTurno(estado);
			return;
		}

		if (atual.getEfeitosAtivos().containsKey("STUN")) {
			System.out.println(">>> " + atual.getNome() + " está atordoado!");
			atual.removerEfeito("STUN"); // STUN dura apenas 1 turno
			atual.setContadorTU(atual.getContadorTU() + 100);
			proximoTurno(estado);
			return;
		}

		// Terreno
		if (mainController != null && mainController.getMapController() != null) {
			resolverTerrenoPerigoso(proximoAtor, mainController.getMapController(), estado);
		}

		// Regeneração
		int nivelRegeneracao = atual.getValorPropriedade("REGENERACAO");
		if (nivelRegeneracao > 0 && atual.isVivo()) {
			double cura = atual.getVidaMaxima() * (0.10 * nivelRegeneracao);
			if (atual.getVidaAtual() < atual.getVidaMaxima()) {
				double novaVida = Math.min(atual.getVidaMaxima(), atual.getVidaAtual() + cura);
				atual.setVidaAtual(novaVida, estado, mainController);
				System.out.println(">>> REGEN: " + atual.getNome() + " recuperou " + (int) cura + " HP.");
			}
		}

		// Fallback: Pular turno se Dormindo/Stun (segurança caso efeito aplicado entre checagens)
		if (atual.getEfeitosAtivos().containsKey("Dormindo") || atual.getEfeitosAtivos().containsKey("STUN")) {
			atual.setContadorTU(atual.getContadorTU() + 100);
			if (atual.getEfeitosAtivos().containsKey("STUN"))
				atual.removerEfeito("STUN");
			proximoTurno(estado);
			return;
		}

		// Se não pulou, prepara o turno
		auraManager.checarEfeitosDeInicioDeTurno(atual, estado);

		// Hook de Raça (Half-Angel/Half-Demon manutenção)
		if (atual.getRaca() != null) {
			atual.getRaca().onTurnStart(atual, estado);
		}

		atual.setMovimentoRestanteTurno(atual.getMovimento());
		System.out.println("DEBUG: Movimento de " + atual.getNome() + " resetado para " + atual.getMovimento());
	}

	// ========== CONTRA-ATAQUE ==========

	/**
	 * Processa uma fila de contra-ataques originada da janela de Resolução de
	 * Dano. Cada alvo da fila recebe um TU reduzido de forma que:
	 *
	 * <ul>
	 *   <li>Todos os contra-atacantes agirão antes de qualquer outro combatente
	 *   (mesmo o atacante que acabou de agir).</li>
	 *   <li>A ordem dentro do grupo respeita a ordem de seleção da checkbox
	 *   (o 1º selecionado age primeiro).</li>
	 * </ul>
	 *
	 * Segue o mesmo padrão usado por Sussurro Sombrio
	 * ({@code setContadorTU(menorTU - 1)}) para empurrar o ator para o topo da
	 * iniciativa. Alvos mortos durante a resolução são ignorados silenciosamente.
	 *
	 * @param fila   Lista de alvos ordenada por ordem de seleção (1º = 1º a contra-atacar).
	 * @param estado Estado atual do combate (não-nulo).
	 */
	public void processarContraAtaques(List<Personagem> fila, EstadoCombate estado) {
		if (fila == null || fila.isEmpty() || estado == null || estado.getCombatentes() == null) {
			return;
		}

		// Filtra apenas alvos ainda vivos/ativos — se morreram durante a resolução,
		// obviamente não contra-atacam.
		List<Personagem> filaValida = fila.stream()
				.filter(p -> p != null && p.isAtivoNoCombate())
				.distinct()
				.collect(Collectors.toList());

		if (filaValida.isEmpty()) {
			return;
		}

		// Menor TU entre combatentes que NÃO estão na fila de contra-ataque.
		// Os contra-atacantes ficarão abaixo deste valor, garantindo prioridade.
		int menorTUExterno = estado.getCombatentes().stream()
				.filter(Personagem::isAtivoNoCombate)
				.filter(p -> !filaValida.contains(p))
				.mapToInt(Personagem::getContadorTU)
				.min()
				.orElse(0);

		int n = filaValida.size();
		for (int i = 0; i < n; i++) {
			Personagem p = filaValida.get(i);
			// Primeiro selecionado recebe o menor TU (menorTUExterno - n),
			// os demais ficam em sequência crescente, mas todos abaixo do menor externo.
			int novoTU = menorTUExterno - (n - i);
			p.setContadorTU(novoTU);

			String msg = "⚔️ CONTRA-ATAQUE #" + (i + 1) + ": " + p.getNome()
					+ " agirá imediatamente (TU=" + novoTU + ").";
			System.out.println(">>> " + msg);

}
	}

	// ========== AVANÇO DE TEMPO ==========

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

} else if (efeito.startsWith("3º Andar") && tempoGlobalAtual % 300 == 0) {
					// Popup para selecionar alvos do Olho da Gula
					List<Personagem> combatentesAtivos = estado.getCombatentes().stream()
							.filter(Personagem::isAtivoNoCombate)
							.collect(Collectors.toList());

					if (!combatentesAtivos.isEmpty()) {
						javafx.scene.control.Dialog<List<Personagem>> dialog = new javafx.scene.control.Dialog<>();
						dialog.setTitle("👁️ O Olho da Gula");
						dialog.setHeaderText("O Olho se abre! Selecione os alvos que serão observados.\n(50 de dano fixo + Stun)");
						dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #e94560;");
						dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: #eee; -fx-font-size: 13px;");

						javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
						content.setStyle("-fx-padding: 10;");
						List<javafx.scene.control.CheckBox> checkBoxes = new ArrayList<>();

						for (Personagem p : combatentesAtivos) {
							javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(
									p.getNome() + " (HP: " + (int) p.getVidaAtual() + "/" + (int) p.getVidaMaxima() + ")");
							cb.setUserData(p);
							cb.setStyle("-fx-text-fill: " + (p.isProtagonista() ? "#00d4ff" : "#ff6b6b") + "; -fx-font-size: 12px;");
							checkBoxes.add(cb);
							content.getChildren().add(cb);
						}

						javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(content);
						scroll.setFitToWidth(true);
						scroll.setMaxHeight(400);
						scroll.setStyle("-fx-background: #1a1a2e; -fx-background-color: #1a1a2e;");
						dialog.getDialogPane().setContent(scroll);

						dialog.getDialogPane().getButtonTypes().addAll(
								javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

						dialog.setResultConverter(buttonType -> {
							if (buttonType == javafx.scene.control.ButtonType.OK) {
								List<Personagem> selecionados = new ArrayList<>();
								for (javafx.scene.control.CheckBox cb : checkBoxes) {
									if (cb.isSelected()) {
										selecionados.add((Personagem) cb.getUserData());
									}
								}
								return selecionados;
							}
							return null;
						});

						Optional<List<Personagem>> resultado = dialog.showAndWait();
						if (resultado.isPresent() && resultado.get() != null && !resultado.get().isEmpty()) {
							for (Personagem alvo : resultado.get()) {

damageApplicator.aplicarDanoAoAlvo(null, alvo, 50.0, true, TipoAcao.AMBIENTE, estado);
								Efeito stun = new Efeito("STUN", TipoEfeito.DEBUFF, 100, null, 0, 0);
								alvo.adicionarEfeito(stun);
								alvo.recalcularAtributosEstatisticas();
							}
						}
					}
				}

				// 4º ANDAR: Dia (Vento Escaldante)
				else if (efeito.contains("Dia") && tempoGlobalAtual % 35 == 0) {

for (Personagem p : estado.getCombatentes()) {
						if (p.isAtivoNoCombate() && !p.isProtagonista()) {
							damageApplicator.aplicarDanoAoAlvo(null, p, 2.5, true, TipoAcao.AMBIENTE, estado);
						}
					}
				}

				// 4º ANDAR: Dia (Vento Escaldante)
				else if (efeito.contains("Eclipse") && tempoGlobalAtual % 30 == 0) {
					for (Personagem p : estado.getCombatentes()) {
						if (p.isAtivoNoCombate() && !p.isProtagonista()) {
							damageApplicator.aplicarDanoAoAlvo(null, p, 1.3, true, TipoAcao.AMBIENTE, estado);
						}
					}
				}

				// 4º ANDAR: Noite (Vento Congelante)
				else if (efeito.contains("Noite") && tempoGlobalAtual % 100 == 0) {

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

Efeito choque = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Choque", 1, 20);
									Efeito queimacao = br.com.dantesrpg.model.util.EffectFactory
											.criarEfeito("Queimação", 0, 15);
									alvoRaio.adicionarEfeito(choque);
									alvoRaio.adicionarEfeito(queimacao);
									damageApplicator.aplicarDanoAoAlvo(null, alvoRaio, 15.0, true, TipoAcao.AMBIENTE, estado);
								}
							}
						}
						if (duracaoChuvaRestante == 0) {}
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

				// Avança o tempo das maldições ativas
				br.com.dantesrpg.model.util.MaldicaoUtils.avancarTempoMaldicoes(p, 1);

				// CHECK DE TEMPO DA RAÇA (PRIORIDADE ABSOLUTA)
				if (p.getRaca() != null) {
					p.getRaca().onTimeAdvanced(p, estado, mainController);
				}

				if (!p.isAtivoNoCombate())
					continue;

				// Efeitos de Terreno Constantes (como Areia e Água)
				if (mainController != null && mainController.getMapController() != null) {
					br.com.dantesrpg.controller.MapController map = mainController.getMapController();
					br.com.dantesrpg.model.map.TerrainData.EfeitoInstance efeitoChao = map.getEfeitoNoSolo(p.getPosX(), p.getPosY());
					if (efeitoChao != null && efeitoChao.getTipo() == TipoEfeitoSolo.AREIA && (tempoGlobalAtual % 30 == 0)) {
						System.out.println(">>> " + p.getNome() + " sofreu 2 de dano por estar na Areia.");
						double vidaAntes = p.getVidaAtual();
						p.setVidaAtual(vidaAntes - 2, estado, mainController);
						p.registrarDanoSofrido(2, tempoGlobalAtual);
						if (p.getVidaAtual() <= 0 && !p.isVivo()) {

if (mainController != null) mainController.atualizarInterfaceAposMorte();
						}
					}

					// Efeito de Água
					if (map.getTerreno(p.getPosX(), p.getPosY()) == br.com.dantesrpg.model.map.TerrainData.TipoTerreno.AGUA) {
						int lentoTu = p.getValorPropriedade("AGUA_LENTO_TU") + 1;
						int maldicaoTu = p.getValorPropriedade("AGUA_MALDICAO_TU") + 1;

						p.getPropriedades().removeIf(prop -> prop.startsWith("AGUA_LENTO_TU:"));
						p.getPropriedades().add("AGUA_LENTO_TU:" + lentoTu);

						p.getPropriedades().removeIf(prop -> prop.startsWith("AGUA_MALDICAO_TU:"));
						p.getPropriedades().add("AGUA_MALDICAO_TU:" + maldicaoTu);

						if (lentoTu >= 100) {
							p.getPropriedades().removeIf(prop -> prop.startsWith("AGUA_LENTO_TU:"));
							System.out.println(">>> " + p.getNome() + " permaneceu na água por 100 TU e recebeu Lento!");
							Efeito lento = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Lento", 300, 0);
							effectProcessor.aplicarEfeito(p, lento);
						}

						if (maldicaoTu >= 80) {
							p.getPropriedades().removeIf(prop -> prop.startsWith("AGUA_MALDICAO_TU:"));
							System.out.println(">>> " + p.getNome() + " permaneceu na água por 80 TU e recebeu Maldição (5%)!");
							br.com.dantesrpg.model.util.Maldicao mald = new br.com.dantesrpg.model.util.Maldicao(
									"Água", 0.05, 1000, false);
							br.com.dantesrpg.model.util.MaldicaoUtils.adicionarMaldicao(p, mald);
						}
					} else {
						// Limpa acúmulos se sair da água
						p.getPropriedades().removeIf(prop -> prop.startsWith("AGUA_LENTO_TU:"));
						p.getPropriedades().removeIf(prop -> prop.startsWith("AGUA_MALDICAO_TU:"));
					}
				}

				// JACKPOT (Regeneração)
				if (p.getEfeitosAtivos().containsKey("JACKPOT!")) {
					p.setManaAtual(p.getManaMaxima());
					int inspiracao = p.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
					double curaPorTick = p.getVidaMaxima() * (inspiracao * 0.005);
					p.regenerarVidaFracionada(curaPorTick, estado, mainController);
				}

				// Processamento de Efeitos (DoT / Expiração)
				List<String> efeitosARemover = new ArrayList<>();
				List<String> nomesEfeitosAtuais = new ArrayList<>(p.getEfeitosAtivos().keySet());

				for (String nomeEfeito : nomesEfeitosAtuais) {
					Efeito efeitoObj = p.getEfeitosAtivos().get(nomeEfeito);
					if (efeitoObj == null)
						continue;

					if (nomeEfeito.equals("Gatilho")) {
						p.setContadorTU(p.getContadorTU() + 100);
						p.removerEfeito("Gatilho");
						System.out.println(">>> Gatilho: " + p.getNome() + " foi postergado em +100 TU.");
						continue;
					}
					if (p.getValorPropriedade(PROPRIEDADE_MALDITO) > 0
							&& efeitoObj.getTipo() == TipoEfeito.DOT) {
						efeitosARemover.add(nomeEfeito);
						continue;
					}

					efeitoObj.reduzirDuracao(1);

					// Coagulação: a cada 120 TU gera +20 de escudo de sangue
					if (nomeEfeito.equals("Coagulação") && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						p.adicionarEscudoSangue(20);
						System.out.println(">>> Coagulação: " + p.getNome() + " ganha +20 escudo de sangue (total: "
								+ (int) p.getEscudoSangueAtual() + ").");
					}

					// Benção do Gekkyūden: aliado dentro do domínio de Lillith ganha +1 escudo de sangue a cada 5 TU
					if (nomeEfeito.equals("Benção do Gekkyūden") && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						p.adicionarEscudoSangue(1);
					}

					// Gnosis de Fogo: ganha +10 escudo de sangue a cada 100 TU
					if (nomeEfeito.equals("Gnosis de Fogo") && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						p.adicionarEscudoSangue(10);
					}

					// Maldição do Gekkyūden: inimigo dentro do domínio de Lillith sofre 1 dano a cada 20 TU
					if (nomeEfeito.equals("Maldição do Gekkyūden") && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						double dano = efeitoObj.getDanoPorTick();
						double vidaAntes = p.getVidaAtual();
						p.setVidaAtual(vidaAntes - dano, estado, mainController);
						System.out.println(">>> Jihō Gekkyūden: " + p.getNome() + " sofre "
								+ String.format("%.0f", dano) + " de dano (Maldição do Gekkyūden).");
					}

					// Maldição de Fimbulwinter: inimigo dentro do domínio de Brunhilda ganha 1 stack de Congelamento a cada 100 TU
					if (nomeEfeito.equals("Maldição de Fimbulwinter") && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						br.com.dantesrpg.model.fantasmasnobres.Fimbulwinter.aplicarCongelamento(p, this, 1);
					}

					// Drenagem de Efeitos: perde 1% de bônus de dano por TU
					if (nomeEfeito.equals("Drenagem de Efeitos") && efeitoObj.getModificadores() != null) {
						Double bonusAtual = efeitoObj.getModificadores().get("DANO_BONUS_PERCENTUAL");
						if (bonusAtual != null) {
							double novoBonus = bonusAtual - 0.01;
							if (novoBonus <= 0) {
								novoBonus = 0;
								efeitoObj.setDuracaoTURestante(0);
							}
							efeitoObj.getModificadores().put("DANO_BONUS_PERCENTUAL", novoBonus);
							p.recalcularAtributosEstatisticas();
						}
					}

					// Queimadura Inefável (Escudo Infernal): a cada tick, adiciona +1 escudo infernal e +1 redução max HP via Contrato de Vida
					if (nomeEfeito.equals("Queimadura Inefável") && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						p.setEscudoInfernalAtual(p.getEscudoInfernalAtual() + 1);
						br.com.dantesrpg.model.util.ContratoDeVida contrato = new br.com.dantesrpg.model.util.ContratoDeVida("Queimadura Inefável", 1.0, -1, false);
						br.com.dantesrpg.model.util.ContratoDeVidaUtils.adicionarContrato(p, contrato);
					}

					// Bloco de Dano DoT
					if (efeitoObj.getTipo() == TipoEfeito.DOT && efeitoObj.getIntervaloTickTU() > 0
							&& (tempoGlobalAtual % efeitoObj.getIntervaloTickTU() == 0)) {
						double danoFinalDoT = 0;

						if (efeitoObj.getNome().equals("Hemorragia")) {
							double percentualHemorragia = 0.02;
							if (efeitoObj.getModificadores() != null) {
								percentualHemorragia = efeitoObj.getModificadores().getOrDefault("PERCENTUAL_HP_MAX",
										percentualHemorragia);
							}
							danoFinalDoT = p.getVidaMaxima() * percentualHemorragia;
						} else {
							double danoDoTBruto = efeitoObj.getDanoPorTick();
							danoFinalDoT = danoDoTBruto * (1.0 - p.getReducaoDoTTopor());
						}

						danoFinalDoT = Math.max(0, danoFinalDoT);

						System.out.println(">>> [TU " + tempoGlobalAtual + "] Efeito [" + efeitoObj.getNome() + "] causa "
								+ String.format("%.1f", danoFinalDoT) + " dano em " + p.getNome());

						double vidaAntes = p.getVidaAtual();
						p.setVidaAtual(vidaAntes - danoFinalDoT, estado, mainController);

						// Efeitos Especiais DoT
						if (efeitoObj.getNome().equals("HellFire")) {
							double redAtual = efeitoObj.getModificadores().getOrDefault("REDUCAO_CURA", 0.0);
							efeitoObj.getModificadores().put("REDUCAO_CURA", redAtual + 0.02);
							p.recalcularAtributosEstatisticas();
						}
						if (efeitoObj.getNome().equals("Chama Divina")) {
							double armaduraRed = efeitoObj.getModificadores().getOrDefault("BONUS_ARMADURA_PERCENTUAL", 0.0);
							double curaRed = efeitoObj.getModificadores().getOrDefault("REDUCAO_CURA", 0.0);
							efeitoObj.getModificadores().put("BONUS_ARMADURA_PERCENTUAL", armaduraRed - 0.20);
							efeitoObj.getModificadores().put("REDUCAO_CURA", curaRed + 0.50);
							p.recalcularAtributosEstatisticas();
						}
						if (danoFinalDoT > 0) {
							// Dormindo: acorda após 2 ticks de dano
							if (p.getEfeitosAtivos().containsKey("Dormindo")) {
								Efeito dormindo = p.getEfeitosAtivos().get("Dormindo");
								int hitsRecebidos = dormindo.getStacks() + 1;
								dormindo.setStacks(hitsRecebidos);

								if (hitsRecebidos >= 2) {
									System.out.println(">>> " + p.getNome() + " ACORDOU após 2 ticks de dano!");
									p.removerEfeito("Dormindo");

								} else {
									System.out.println(">>> " + p.getNome() + " recebeu dano dormindo (" + hitsRecebidos + "/2 para acordar).");
								}
							}
							if (p.getEfeitosAtivos().containsKey("Sono")) {
								System.out.println(">>> " + p.getNome() + " ACORDOU devido ao dano do DoT!");
								p.removerEfeito("Sono");
							}
						}

						double danoReal = vidaAntes - p.getVidaAtual();

						// Morte por DoT (Essência)
						if (vidaAntes > 0 && !p.isAtivoNoCombate() && !p.isVivo()) {
							if (colocarEmEsperaParaArise(p, estado)) {
								if (mainController != null) {
									mainController.atualizarInterfaceAposMorte();
								}
								continue;
							}
							String msgMorte = "💀 " + p.getNome() + " morreu por " + efeitoObj.getNome() + "!";
							System.out.println(">>> " + p.getNome() + " morreu por DoT.");

							// XP
							if (p.getXpReward() > 0) {
								System.out.println(">>> XP por DoT: " + p.getXpReward());
								estado.adicionarXpAoPool(p.getXpReward());
							}

							// Murasame (Essência)
							if (efeitoObj.getNome().equals("Toxina")) {
								for (Personagem combatente : estado.getCombatentes()) {
									if (combatente.possuiArmaEquipada("Murasame")) {
										System.out.println(">>> MURASAME: Capturando Essência (DoT)...");
										combatente.getInventario().adicionarItem(new br.com.dantesrpg.model.items.EssenciaInimigo(p));
										break;
									}
								}
							}

							// Remove do Mapa (Visual)
							if (mainController != null) {
								if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
									if (mainController.getMapController() != null) {
										mainController.getMapController().atualizarCelulaParaChao(p.getPosX(),
												p.getPosY());
									}
								}
								mainController.atualizarInterfaceAposMorte();
							}
						}
						p.registrarDanoSofrido(danoReal, tempoGlobalAtual);
					}

					if (efeitoObj.getDuracaoTURestante() <= 0) {
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
								mainController.removerDominio("ringue_alexei");
								domainManager.limparFusoesComDominio("ringue_alexei");
							}
							if (nomeEfeito.equals("Domínio: Idle Death Gamble") && mainController != null) {
								mainController.removerDominio("dominio_lyria");
								domainManager.limparFusoesComDominio("dominio_lyria");
								// Limpa Estrelas da Sorte ao expirar o domínio
								p.removerEfeito("Estrelas da Sorte");
							}
							if (nomeEfeito.equals("Jihō Gekkyūden") && mainController != null) {
								mainController.removerDominio("jiho_lillith");
								domainManager.limparFusoesComDominio("jiho_lillith");

								// Exaustão narrativa: Lillith fica 100 TU sem poder agir
								Efeito exausto = new Efeito("Exausto pelo Jihō Gekkyūden",
										br.com.dantesrpg.model.enums.TipoEfeito.DEBUFF, 100,
										java.util.Map.of(), 0, 0);
								p.adicionarEfeito(exausto);
								System.out.println(">>> " + p.getNome()
										+ " está exausta pela Expansão de Domínio por 100 TU.");
							}
							if (nomeEfeito.equals("Lua Sombria") && mainController != null) {
								mainController.removerDominio("lua_sombria");
								domainManager.limparFusoesComDominio("lua_sombria");
							}
							if (nomeEfeito.equals("Fimbulwinter") && mainController != null) {
								mainController.removerDominio("fimbulwinter_brunhilda");
								domainManager.limparFusoesComDominio("fimbulwinter_brunhilda");
								br.com.dantesrpg.model.fantasmasnobres.Fimbulwinter.reverterFimbulwinter(p);
							}
							if (nomeEfeito.equals("Modo Justiça")) {
								br.com.dantesrpg.model.fantasmasnobres.ModoPolaris.reverterParaPolaris(p);
							}
							if (nomeEfeito.equals("Falsa Justiça") && mainController != null) {
								mainController.removerDominio("falsa_justica");
								domainManager.limparFusoesComDominio("falsa_justica");
								br.com.dantesrpg.model.fantasmasnobres.AndJusticeForMySelf
										.reverterHabilidades(p);
								System.out.println(">>> " + p.getNome()
										+ " — A Falsa Justiça se dissipou.");
							}
							if (nomeEfeito.equals("Despertar Divino")) {
								br.com.dantesrpg.model.fantasmasnobres.RevelacaoDeYaweh
										.reverterDespertarDivino(p);
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
		auraManager.atualizarAuras(estado);

		System.out.println("--- Avanço de tempo concluído. ---");
	}

	// ========== RESOLUÇÃO DE AÇÃO ==========

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
				return;
			}
		}

		String acaoNome = (input.getHabilidade() != null) ? input.getHabilidade().getNome() : "Ataque Básico";

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
						mainController.removerDominio("dominio_lyria");
						domainManager.limparFusoesComDominio("dominio_lyria");
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
				habilidade = fulgorOpt.get();

				AcaoMestreInput inputFulgor = new AcaoMestreInput(ator, alvos, habilidade);
				inputFulgor.adicionarResultadoDado("DADO_ATRIBUTO", input.getResultadoDado("DADO_ATRIBUTO"));
				inputFulgor.adicionarResultadoDado("DADO_CHANCE_RESTRICAO",
						input.getResultadoDado("DADO_CHANCE_RESTRICAO"));
				input = inputFulgor;

			} else {
				System.err.println(
						"Erro: Restrição Celestial ativa, mas Fulgor Negro não encontrado em " + ator.getNome());
			}
		}

		if (habilidade == null) {
			tipoAcaoAtual = TipoAcao.ATAQUE_BASICO;
			custoManaBase = 0;
			Arma arma = ator.getArmaEquipada();
			List<Arma> armasSelecionadas = input.getArmasSelecionadas();
			if (armasSelecionadas.isEmpty()) {
				System.out.println(ator.getNome() + " esta desarmado!");
				return;
			}
			arma = armasSelecionadas.get(0);
			if (arma.isRequerMunicao()) {
				if (arma.getMunicaoAtual() <= 0) {
					System.out.println(">>> CLIQUE SECO! " + ator.getNome() + " tentou atirar sem munição.");
					return;
				}
				System.out.println(
						">>> Bala disparada. Restam: " + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima());
			}
			custoTUBase = calcularCustoTUAtaqueBasico(armasSelecionadas);
			int rolagemDadoAtributo = input.getResultadoDado("DADO_ATRIBUTO");
			if (rolagemDadoAtributo == -1) {
				System.err.println("Erro: Ataque básico sem Resultado Dado Atributo.");
				return;
			}

			damageCalculator.resolverDanoPadrao(ator, arma, rolagemDadoAtributo, alvos, 1.0, tipoAcaoAtual, null, estado, input);

		} else {
			// --- LÓGICA DE HABILIDADE ---
			tipoAcaoAtual = TipoAcao.HABILIDADE;
			custoManaBase = habilidade.getCustoMana();
			custoTUBase = habilidade.getCustoTU();

			if (habilidade instanceof br.com.dantesrpg.model.habilidades.classe.JusticaDourada
					&& !podeExecutarJusticaDourada(ator, input)) {
				return;
			}

			if (habilidade.getTipoAlvo() == TipoAlvo.AREA || habilidade.getTipoAlvo() == TipoAlvo.EQUIPE) {
				System.out.println(">>> Gerando alvos para habilidade centrada no ator: " + habilidade.getNome());
				alvos = encontrarAlvosAoRedorDoAtor(ator, habilidade, estado);
				input.getAlvos().clear();
				input.getAlvos().addAll(alvos);
			}

			if (habilidade.getTipoAlvo() != TipoAlvo.SI_MESMO) {

				int rolagemDadoAtributo = input.getResultadoDado("DADO_ATRIBUTO");

				if (habilidade.getMultiplicadorDeDano() > 0 && rolagemDadoAtributo == -1
						&& !habilidade.getNome().equals("Soco Sério")) {
					System.err.println(
							"Erro: Habilidade ofensiva (" + habilidade.getNome() + ") sem Resultado Dado Atributo.");
					return;
				}

				if (habilidade.getNome().equals("Fulgor Negro")) {
					((FulgorNegro) habilidade).executarFulgorNegro(input, estado, this);
				} else if (habilidade instanceof br.com.dantesrpg.model.habilidades.classe.DeadEye) {
					Arma arma = ator.getArmaEquipada();
					if (arma == null) {
						System.out.println(ator.getNome() + " está desarmado!");
						return;
					}
					damageCalculator.resolverDeadEye(ator, arma, rolagemDadoAtributo, alvos, habilidade, estado, input);
				} else if (habilidade instanceof DistortedSolo) {
					((DistortedSolo) habilidade).executarSolo(input, estado, this);
				} else if (habilidade instanceof WhaWhaSolo) {
					((WhaWhaSolo) habilidade).executarSolo(input, estado, this);
				} else if (habilidade instanceof PlainSolo) {
					((PlainSolo) habilidade).executarSolo(input, estado, this);

				} else if (habilidade.getNome().equals("Soco Sério")) {
					int danoTotalCausado = damageCalculator.resolverSocoSerio(ator, alvos, habilidade, input, estado);
					System.out.println(
							">>> Dano total da ação (" + habilidade.getNome() + "): " + danoTotalCausado + ".");

				} else if (habilidade.getMultiplicadorDeDano() > 0) {
					Arma arma = ator.getArmaEquipada();
					if (arma == null) {
						System.out.println(ator.getNome() + " está desarmado!");
						return;
					}
					damageCalculator.resolverDanoPadrao(ator, arma, rolagemDadoAtributo, alvos, habilidade.getMultiplicadorDeDano(),
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

			} else if (habilidade instanceof br.com.dantesrpg.model.habilidades.classe.SintetizarPocao) {
				String escolha = input.getOpcaoEscolhida();
				if (escolha == null)
					escolha = "Cura";
				((br.com.dantesrpg.model.habilidades.classe.SintetizarPocao) habilidade).executarComEscolha(ator, escolha);

			} else if (habilidade instanceof br.com.dantesrpg.model.habilidades.classe.AprimorarPocao) {
				String escolha = input.getOpcaoEscolhida();
				int sorteRoll = input.getResultadoDado("DADO_ATRIBUTO");
				((br.com.dantesrpg.model.habilidades.classe.AprimorarPocao) habilidade).executarAprimoramento(ator, escolha, sorteRoll);

			} else {
				if (!ator.isClone() || habilidadePodeSerCopiadaPorClone(habilidade)) {
					effectProcessor.aplicarEfeitosDaHabilidade(ator, habilidade, alvos, estado, this);
				}
			}

			int cooldown = habilidade.getCooldownTU();
			if (cooldown > 0) {
				String cooldownEffectName = "CD:" + habilidade.getNome();
				Efeito cooldownEfeito = new Efeito(cooldownEffectName, TipoEfeito.DEBUFF, cooldown, null, 0, 0);
				effectProcessor.aplicarEfeito(ator, cooldownEfeito);
				System.out.println(">>> " + habilidade.getNome() + " entrou em cooldown por " + cooldown + " TU.");
			}
		}

		if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.FRACO) {
			custoTUBase = (int) (custoTUBase * 0.80);
			System.out.println(">>> Modo FRACO: TU reduzido para " + custoTUBase);
		} else if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.FORTE) {
			custoTUBase = (int) (custoTUBase * 1.20);
			System.out.println(">>> Modo FORTE: TU aumentado para " + custoTUBase);
		} else if (input.getModoAtaque() == br.com.dantesrpg.model.enums.ModoAtaque.CORONHADA && ator.getArmaEquipada() != null) {
			double multTU = ator.getArmaEquipada().getCustoTUMultiplierAtaqueAlternativo();
			if (multTU != 1.0) {
				custoTUBase = (int) (custoTUBase * multTU);
				System.out.println(">>> Modo " + ator.getArmaEquipada().getNomeAtaqueAlternativoBasico() + ": TU ajustado para " + custoTUBase);
			}
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

		int custoTUFinal = calcularCustoTUFinal(ator, custoTUBase, habilidade, tipoAcaoAtual, estado);
		double multiplicadorLento = ator.getMultiplicadorCustoTU();
		custoTUFinal = (int) (custoTUFinal * multiplicadorLento);
		if (consumirJusticaDouradaSeAtaqueDisparado(ator, habilidade, tipoAcaoAtual)) {
			custoTUFinal += 100;
			System.out.println(">>> Justiça Dourada: penalidade de +100 TU aplicada ao disparo.");
		}

		if (multiplicadorLento > 1.0) {
			System.out
					.println(">>> Efeito Lento/Muito Lento aplicado! Custo TU: " + custoTUBase + " -> " + custoTUFinal);
		}

		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoTUFinal + " TUs.");

		// Hook onActionUsed
		effectProcessor.chamarHookAcaoUsada(ator, tipoAcaoAtual, estado);

		if (habilidade != null) {
			ator.setUltimaHabilidadeUsada(normalizarHabilidadeCopiavelParaClone(habilidade));
		}
		effectProcessor.verificarManaPassivaModoJustica(ator, estado);
	}

	public boolean tentarAcionarGatilhoVeloz(Personagem defensor, Personagem atacante, EstadoCombate estado) {
		if (defensor == null || atacante == null || estado == null || mainController == null) {
			return false;
		}
		if (!defensor.getEfeitosAtivos().containsKey("Gatilho Veloz")
				|| atacante.getEfeitosAtivos().containsKey("Gatilho")
				|| mesmaFaccao(defensor, atacante)) {
			return false;
		}

		defensor.removerEfeito("Gatilho Veloz");
		boolean sucesso = mainController.solicitarTesteGatilhoVeloz(defensor, atacante);
		if (!sucesso) {
			System.out.println(">>> Gatilho Veloz falhou. O ataque continua.");
			return false;
		}

		Efeito gatilho = new Efeito("Gatilho", TipoEfeito.DEBUFF, 9999, Map.of(), 0, 0);
		atacante.adicionarEfeito(gatilho);
		System.out.println(">>> Gatilho Veloz anulou o ataque de " + atacante.getNome() + "!");
		return true;
	}

	private boolean mesmaFaccao(Personagem a, Personagem b) {
		return a.getFaccao() != null && a.getFaccao().equals(b.getFaccao());
	}

	private boolean consumirJusticaDouradaSeAtaqueDisparado(Personagem ator, Habilidade habilidade,
			TipoAcao tipoAcaoAtual) {
		if (ator == null || !ator.getEfeitosAtivos().containsKey("Justiça Dourada")) {
			return false;
		}
		boolean ataqueDisparado = tipoAcaoAtual == TipoAcao.ATAQUE_BASICO
				|| (habilidade != null && habilidade.getTipoAlvo() != TipoAlvo.SI_MESMO
						&& habilidade.getMultiplicadorDeDano() > 0);
		if (!ataqueDisparado) {
			return false;
		}
		ator.removerEfeito("Justiça Dourada");
		return true;
	}

	// ========== AÇÃO DE ITEM ==========

	private boolean podeExecutarJusticaDourada(Personagem ator, AcaoMestreInput input) {
		int investimento = input != null ? input.getResultadoDado(
				br.com.dantesrpg.model.habilidades.classe.JusticaDourada.INPUT_MOEDAS) : -1;
		if (investimento <= 0) {
			System.out.println(">>> Justiça Dourada bloqueada: informe um investimento maior que 0.");
			return false;
		}
		if (ator.getInventario() == null || !ator.getInventario().podeGastarMoedasPorPeso(investimento)) {
			System.out.println(">>> Justiça Dourada bloqueada: moedas insuficientes para investir "
					+ investimento + ".");
			return false;
		}
		return true;
	}

	public void resolverAcaoItem(AcaoMestreInput input, EstadoCombate estado) {
		Personagem ator = input.getAtor();
		Item item = input.getItemSendoUsado();

		if (item == null)
			return;

		item.usar(ator, estado, mainController);
		if (ator.getInventario() != null) {
			ator.getInventario().removerItem(item);
		}

		int custoTUFinal = calcularCustoTUFinal(ator, item.getCustoTU(), null, TipoAcao.ITEM, estado);
		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoTUFinal + " TUs para usar " + item.getNome() + ".");

		effectProcessor.chamarHookAcaoUsada(ator, TipoAcao.ITEM, estado);
	}

	// ========== AÇÃO DE RECARGA ==========

	public boolean resolverAcaoRecarregar(Personagem ator) {
		List<Arma> armas = getArmasDeFogoParaRecarregar(ator);
		List<PacoteMunicao> pacotes = getPacotesMunicaoDisponiveis(ator);
		if (armas.size() == 1 && pacotes.size() == 1) {
			return resolverAcaoRecarregar(ator, armas.get(0), pacotes.get(0));
		}
		if (armas.isEmpty()) {
			System.out.println(">>> Nenhuma arma de fogo equipada precisa de recarga.");
		} else if (pacotes.isEmpty()) {
			System.out.println(">>> Recarga bloqueada: nenhuma Caixa/Pacote de Munição disponível no inventário.");
		} else {
			System.out.println(">>> Recarga requer seleção de arma e pacote de munição.");
		}
		return false;
	}

	public boolean resolverAcaoRecarregar(Personagem ator, Arma arma, PacoteMunicao pacote) {
		if (!podeRecarregar(ator, arma, pacote)) {
			return false;
		}

		int municaoAntes = arma.getMunicaoAtual();
		int balasConcedidas = pacote.recarregaAteOMaximo()
				? arma.getMunicaoMaxima() - municaoAntes
				: pacote.balasConcedidas();
		arma.setMunicaoAtual(municaoAntes + balasConcedidas);

		int balasRecarregadas = arma.getMunicaoAtual() - municaoAntes;
		int excedente = pacote.recarregaAteOMaximo() ? 0 : Math.max(0, pacote.balasConcedidas() - balasRecarregadas);
		ator.getInventario().removerItemPorTipo(pacote.tipoItem());

		int custoRecarga = calcularCustoTUFinal(ator, pacote.custoTU(), null, TipoAcao.RECARREGAR, null);
		ator.setContadorTU(ator.getContadorTU() + custoRecarga);

		String mensagem = ">>> " + ator.getNome() + " consumiu " + pacote.nome() + " e recarregou "
				+ arma.getNome() + " (" + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima()
				+ "). Custo: " + custoRecarga + " TU.";
		System.out.println(mensagem);

if (excedente > 0) {
			System.out.println(">>> " + excedente + " bala(s) excedente(s) foram perdidas no abismo.");
		}

		effectProcessor.chamarHookAcaoUsada(ator, TipoAcao.RECARREGAR, null);
		return true;
	}

	public List<Arma> getArmasDeFogoParaRecarregar(Personagem ator) {
		if (ator == null) {
			return List.of();
		}
		return ator.getArmasEquipadas().stream()
				.filter(Arma::isRequerMunicao)
				.filter(arma -> arma.getMunicaoAtual() < arma.getMunicaoMaxima())
				.collect(Collectors.toList());
	}

	public List<PacoteMunicao> getPacotesMunicaoDisponiveis(Personagem ator) {
		if (ator == null || ator.getInventario() == null) {
			return List.of();
		}
		List<PacoteMunicao> pacotes = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : ator.getInventario().getItensAgrupados().entrySet()) {
			if (entry.getValue() == null || entry.getValue() <= 0) {
				continue;
			}
			String tipoItem = entry.getKey();
			Item item = mainController != null ? mainController.getItem(tipoItem) : null;
			String nomeItem = item != null ? item.getNome() : tipoItem;
			identificarPacoteMunicao(tipoItem, nomeItem)
					.ifPresent(def -> pacotes.add(new PacoteMunicao(tipoItem, nomeItem, def.tamanho(),
							def.balasConcedidas(), def.custoTU(), entry.getValue())));
		}
		pacotes.sort(Comparator.comparingInt(PacoteMunicao::custoTU));
		return pacotes;
	}

	private boolean podeRecarregar(Personagem ator, Arma arma, PacoteMunicao pacote) {
		if (ator == null || arma == null || pacote == null || ator.getInventario() == null) {
			return false;
		}
		if (!arma.isRequerMunicao()) {
			System.out.println(">>> Recarga bloqueada: " + arma.getNome() + " não usa munição.");
			return false;
		}
		if (!ator.getArmasEquipadas().contains(arma)) {
			System.out.println(">>> Recarga bloqueada: arma não está equipada por " + ator.getNome() + ".");
			return false;
		}
		if (arma.getMunicaoAtual() >= arma.getMunicaoMaxima()) {
			System.out.println(">>> Recarga bloqueada: " + arma.getNome() + " já está com munição cheia.");
			return false;
		}
		if (!ator.getInventario().possuiItem(pacote.tipoItem())) {
			System.out.println(">>> Recarga bloqueada: " + ator.getNome() + " não possui " + pacote.nome() + ".");
			return false;
		}
		return true;
	}

	private Optional<DefinicaoPacoteMunicao> identificarPacoteMunicao(String tipoItem, String nomeItem) {
		String texto = normalizarTexto(tipoItem + " " + nomeItem);
		boolean isPacoteMunicao = texto.contains("caixa de municao")
				|| texto.contains("pacote de municao")
				|| texto.contains("caixamunicao")
				|| texto.contains("pacotemunicao");
		if (!isPacoteMunicao) {
			return Optional.empty();
		}
		return DEFINICOES_PACOTES_MUNICAO.stream()
				.filter(def -> contemMarcadorTamanho(texto, def.tamanho()))
				.findFirst()
				.or(() -> DEFINICOES_PACOTES_MUNICAO.stream()
						.filter(def -> "P".equals(def.tamanho()))
						.findFirst());
	}

	private String normalizarTexto(String texto) {
		if (texto == null) {
			return "";
		}
		return Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
	}

	private boolean contemMarcadorTamanho(String texto, String tamanho) {
		String t = tamanho.toLowerCase(Locale.ROOT);
		return texto.matches(".*[\\s_\\-\\(\\[]+" + t + "[\\s_\\-\\)\\]]+.*")
				|| texto.matches(".*[\\s_\\-\\(\\[]+" + t + "$")
				|| texto.endsWith("municao" + t);
	}

	// ========== AÇÃO COORDENADA (CLONES) ==========

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
				Atributo atr = br.com.dantesrpg.controller.hud.DiceInputsBuilder.resolverAtributo(clone, habilidade);
				int valorAtr = clone.getAtributosFinais().getOrDefault(atr, 1);
				int valorSorte = clone.getAtributosFinais().getOrDefault(Atributo.SORTE, 1);
				int rolagemFinal = br.com.dantesrpg.model.util.DiceRoller.aplicarBonusRankESorte(rolagemGlobal, valorAtr, valorSorte);
				input.adicionarResultadoDado("DADO_ATRIBUTO", rolagemFinal);
				input.adicionarResultadoDado("DADO_ATRIBUTO_NATURAL", rolagemGlobal);
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

		for (Map.Entry<Personagem, List<Personagem>> entry : alvosAgrupados.entrySet()) {
			Personagem alvo = entry.getKey();
			List<Personagem> atacantes = entry.getValue();

			for (Personagem atacante : atacantes) {
				System.out.println("   -> Clone " + atacante.getNome() + " ataca " + alvo.getNome());

				AcaoMestreInput input = new AcaoMestreInput(atacante, List.of(alvo), habilidade);
				Atributo atr = br.com.dantesrpg.controller.hud.DiceInputsBuilder.resolverAtributo(atacante, habilidade);
				int valorAtr = atacante.getAtributosFinais().getOrDefault(atr, 1);
				int valorSorte = atacante.getAtributosFinais().getOrDefault(Atributo.SORTE, 1);
				int rolagemFinal = br.com.dantesrpg.model.util.DiceRoller.aplicarBonusRankESorte(rolagemGlobal, valorAtr, valorSorte);
				input.adicionarResultadoDado("DADO_ATRIBUTO", rolagemFinal);
				input.adicionarResultadoDado("DADO_ATRIBUTO_NATURAL", rolagemGlobal);

				input.setModoAtaque(modoAtaque != null ? modoAtaque : ModoAtaque.NORMAL);
				input.setTirosExtras(Math.max(0, tirosExtras));

				resolverAcao(input, estado);

				int custoDestaAcao = Math.max(0, atacante.getContadorTU() - tuInicialPorClone.get(atacante));
				
				if (false) {
					damageCalculator.resolverDanoPadrao(atacante, atacante.getArmaEquipada(), rolagemGlobal, List.of(alvo),
							habilidade.getMultiplicadorDeDano(), TipoAcao.HABILIDADE, habilidade, estado, input);
					custoDestaAcao = habilidade.getCustoTU();
				} else if (false) {
					damageCalculator.resolverDanoPadrao(atacante, atacante.getArmaEquipada(), rolagemGlobal, List.of(alvo), 1.0,
							TipoAcao.ATAQUE_BASICO, null, estado, input);
					custoDestaAcao = (atacante.getArmaEquipada() != null) ? atacante.getArmaEquipada().getCustoTU()
							: 100;
				}

				custoDestaAcao = calcularCustoTUFinal(atacante, custoDestaAcao, estado);

				custoDestaAcao = Math.max(0, atacante.getContadorTU() - tuInicialPorClone.get(atacante));

				somaTotalTU += custoDestaAcao;
				numeroAcoes++;
			}
		}

		int mediaTU = 0;
		if (numeroAcoes > 0) {
			mediaTU = somaTotalTU / numeroAcoes;
		} else {
			mediaTU = 50;
		}

		System.out.println(
				">>> SQUAD FINAL: Média de TU calculada: " + mediaTU + " (Baseado em " + numeroAcoes + " ações)");

		if (todosOsClonesDoEsquadrao != null) {
			for (Personagem clone : todosOsClonesDoEsquadrao) {
				Integer tuInicial = tuInicialPorClone.get(clone);
				if (clone.isAtivoNoCombate() && tuInicial != null) {
					clone.setContadorTU(tuInicial + mediaTU);
					System.out.println("   -> " + clone.getNome() + " avançou +" + mediaTU + " TU.");
				}
			}
		}

		if (mainController != null && mainController.getMapController() != null) {
			mainController.getMapController().limparDestaquesPeoes();
		}
	}

	// ========== XP ==========

	public void distribuirXpAposCombate(EstadoCombate estado) {
		int xpTotal = estado.sacarXpDoPool();

		System.out.println("DEBUG XP: Tentando distribuir. Pool: " + xpTotal);

		if (xpTotal <= 0) {

return;
		}

		List<Personagem> jogadoresVivos = estado.getCombatentes().stream()
				.filter(p -> p.getFaccao().equals("JOGADOR") && p.isAtivoNoCombate() && !p.isClone())
				.collect(Collectors.toList());

		if (jogadoresVivos.isEmpty())
			return;
		int xpPorCabeca = xpTotal;

		System.out.println("\n=== DISTRIBUIÇÃO DE XP (" + xpTotal + " XP para CADA jogador) ===");

for (Personagem p : jogadoresVivos) {
			p.ganharExperiencia(xpPorCabeca);
		}
	}

	// ========== TERRENO PERIGOSO ==========

	public void resolverTerrenoPerigoso(Personagem ator, MapController map, EstadoCombate estado) {
		EfeitoInstance efeitoSolo = map.getEfeitoNoSolo(ator.getPosX(), ator.getPosY());

		if (efeitoSolo == null)
			return;

		if (efeitoSolo.expirou()) {
			return;
		}

		// --- LÓGICA DO FOGO ---
		if (efeitoSolo.getTipo() == TipoEfeitoSolo.FOGO) {
			System.out.println(">>> ALERTA: " + ator.getNome() + " pisou em chamas!");

			int danoBase = efeitoSolo.getDanoPorTick();

			damageApplicator.aplicarDanoAoAlvo(efeitoSolo.getCriador(), ator, danoBase, true, TipoAcao.OUTRO, estado, 0);

			if (ator.getEfeitosAtivos().containsKey("Queimação")) {
				Efeito existente = ator.getEfeitosAtivos().get("Queimação");
				existente.setDuracaoTURestante(100);
				System.out.println(">>> Debuff Queimação renovado.");
			} else {
				Efeito queimacao = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Queimação", 200, 10);
				effectProcessor.aplicarEfeito(ator, queimacao);
			}
		}

		if (efeitoSolo.getTipo() == TipoEfeitoSolo.FOGO_AMALDICOADO) {
			aplicarMaldicaoDoFogoAmaldicoado(ator, efeitoSolo.getCriador() != null
					? efeitoSolo.getCriador().getNome()
					: "origem desconhecida");
		}

		// --- LÓGICA DO SANGUE ---
		if (efeitoSolo.getTipo() == TipoEfeitoSolo.SANGUE) {
			System.out.println(">>> ALERTA: " + ator.getNome() + " pisou em sangue!");

			if (ator.getEfeitosAtivos().containsKey("Lento")) {
				Efeito existente = ator.getEfeitosAtivos().get("Lento");
				existente.setDuracaoTURestante(300);
				System.out.println(">>> Debuff Lento renovado.");
			} else {
				Efeito lento = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Lento", 300, 0);
				effectProcessor.aplicarEfeito(ator, lento);
			}

			int manaAtual = (int) ator.getManaAtual();
			if (manaAtual > 0) {
				ator.setManaAtual(manaAtual - 1);
				System.out.println(">>> " + ator.getNome() + " perdeu 1 de mana no sangue! Mana restante: " + ator.getManaAtual());
			}
		}

		// --- LÓGICA DO ÁCIDO ---
		if (efeitoSolo.getTipo() == TipoEfeitoSolo.ACIDO) {
			System.out.println(">>> ALERTA: " + ator.getNome() + " pisou em ácido!");

			if (ator.getEfeitosAtivos().containsKey("Toxina")) {
				Efeito existente = ator.getEfeitosAtivos().get("Toxina");
				existente.setDuracaoTURestante(200);
				System.out.println(">>> Debuff Toxina renovado.");
			} else {
				Efeito toxina = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Toxina", 200, 20);
				effectProcessor.aplicarEfeito(ator, toxina);
			}
		}
	}

	// ========== CLONES ==========

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
				effectProcessor.aplicarEfeito(criador, stealth);
			}

			criador.removerCloneMorto(clone);
		}

		if (estado != null) {
			estado.getCombatentes().remove(clone);
		}
	}

	// ========== CÁLCULOS DE CUSTO ==========

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

	private int calcularCustoTUFinal(Personagem conjurador, int custoTUBase, EstadoCombate estado) {
		return calcularCustoTUFinal(conjurador, custoTUBase, null, TipoAcao.OUTRO, estado);
	}

	private int calcularCustoTUAtaqueBasico(List<Arma> armasSelecionadas) {
		if (armasSelecionadas == null || armasSelecionadas.isEmpty()) {
			return 0;
		}
		int somaCusto = armasSelecionadas.stream()
				.mapToInt(Arma::getCustoTU)
				.sum();
		if (armasSelecionadas.size() >= 2) {
			return (int) (somaCusto * 0.65);
		}
		return somaCusto;
	}

	private int calcularCustoTUFinal(Personagem conjurador, int custoTUBase, Habilidade habilidade,
			TipoAcao tipoAcaoAtual, EstadoCombate estado) {
		double modTU = 1.0;
		int custoExtraFixo = 0;

		if (tipoAcaoAtual == TipoAcao.HABILIDADE) {
			for (Efeito e : conjurador.getEfeitosAtivos().values()) {
				if (e.getModificadores() != null && e.getModificadores().containsKey("REDUCAO_TU_HABILIDADES")) {
					modTU -= e.getModificadores().get("REDUCAO_TU_HABILIDADES");
					System.out.println(">>> Redução de TU de Habilidade aplicada (" + e.getNome() + "): -" 
							+ (int) (e.getModificadores().get("REDUCAO_TU_HABILIDADES") * 100) + "%");
				}
			}
		}

		if (conjurador.getEfeitosAtivos().containsKey("Estado Dourado")) {
			modTU -= 0.15;
			System.out.println(">>> Estado Dourado: Custo de TU reduzido!");
		}

		if (conjurador.getEfeitosAtivos().containsKey("Modo Engaged")) {
			modTU -= 0.30;
			System.out.println(">>> Modo Engaged: Custo de TU reduzido!");
		}

		if (conjurador.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble")
				&& mainController != null && mainController.isPersonagemNoDominio(conjurador, "dominio_lyria")) {
			modTU -= 0.25;
			System.out.println(">>> Idle Death Gamble: Custo de TU reduzido em 25%!");
		}

		if (conjurador.getEfeitosAtivos().containsKey("Lento")) {
			Efeito lento = conjurador.getEfeitosAtivos().get("Lento");
			double pct = 0.30;
			if (lento != null && lento.getModificadores() != null && lento.getModificadores().containsKey("CUSTO_TU_PERCENTUAL")) {
				pct = lento.getModificadores().get("CUSTO_TU_PERCENTUAL");
			}
			modTU += pct * 1.6667;
		}

		if (conjurador.getEfeitosAtivos().containsKey("Intoxicado")) {
			modTU += 0.20;
		}

		if (conjurador.getEfeitosAtivos().containsKey("Coagulação")) {
			custoExtraFixo -= 10;
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

		if (conjurador.getEfeitosAtivos().containsKey("Meio Dia") && estado != null) {
			double totalEscudoInfernal = estado.getCombatentes().stream()
					.filter(Personagem::isAtivoNoCombate)
					.mapToDouble(Personagem::getEscudoInfernalAtual)
					.sum();
			int reducao = (int) (totalEscudoInfernal / 10.0);
			if (reducao > 0) {
				custoExtraFixo -= reducao;
				System.out.println(">>> Meio Dia: Custo de TU reduzido em " + reducao + " (Total Escudo: " + (int) totalEscudoInfernal + ")");
			}
		}

		int custoFinalPercentual = (int) (custoTUBase * Math.max(0.0, modTU));
		int finalTU = custoFinalPercentual + custoExtraFixo;
		if (conjurador.getEfeitosAtivos().containsKey("Lua Sombria")) {
			finalTU = finalTU / 2;
			System.out.println(">>> Lua Sombria: Custo de TU cortado pela metade!");
		}
		return finalTU;
	}

	// ========== ALVOS AO REDOR ==========

	private List<Personagem> encontrarAlvosAoRedorDoAtor(Personagem ator, Habilidade habilidade, EstadoCombate estado) {
		List<Personagem> alvosEncontrados = new ArrayList<>();
		int alcance = habilidade.getTamanhoArea();
		boolean global = (alcance >= 99);

		for (Personagem p : estado.getCombatentes()) {
			if (!p.isAtivoNoCombate())
				continue;

			boolean isAliado = p.getFaccao().equals(ator.getFaccao());

			if (p == ator && !habilidade.afetaSiMesmo())
				continue;
			if (isAliado && !habilidade.afetaAliados())
				continue;
			if (!isAliado && !habilidade.afetaInimigos())
				continue;

			if (global) {
				alvosEncontrados.add(p);
			} else {
				int dist = Math.abs(p.getPosX() - ator.getPosX()) + Math.abs(p.getPosY() - ator.getPosY());
				if (dist <= alcance) {
					alvosEncontrados.add(p);
				}
			}
		}
		return alvosEncontrados;
	}

	// ========== WRAPPERS DE COMPATIBILIDADE (delegam aos subsistemas) ==========

	public int estimarDano(Personagem ator, Habilidade habilidade, Personagem alvo, int rolagemDadoAtributo,
			int rolagemNatural, int rolagemTrocado) {
		return damageCalculator.estimarDano(ator, habilidade, alvo, rolagemDadoAtributo, rolagemNatural, rolagemTrocado);
	}

	public void aplicarDanoAoAlvo(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado) {
		damageApplicator.aplicarDanoAoAlvo(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado);
	}

	public void aplicarDanoAoAlvo(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal) {
		damageApplicator.aplicarDanoAoAlvo(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, ataqueTotal);
	}

	public void aplicarDanoAoAlvoResolvido(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado) {
		damageApplicator.aplicarDanoAoAlvoResolvido(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado);
	}

	public void aplicarDanoAoAlvoResolvido(Personagem ator, Personagem alvo, double dano, boolean ignoraEscudo,
			TipoAcao tipoAcaoDano, EstadoCombate estado, int ataqueTotal) {
		damageApplicator.aplicarDanoAoAlvoResolvido(ator, alvo, dano, ignoraEscudo, tipoAcaoDano, estado, ataqueTotal);
	}

	public boolean aplicarDanoFantasmaDoDeserto(Personagem ator, Personagem alvo, int rolagem1d4, int nivelCascata,
			EstadoCombate estado) {
		return damageApplicator.aplicarDanoFantasmaDoDeserto(ator, alvo, rolagem1d4, nivelCascata, estado);
	}

	public void aplicarEfeito(Personagem alvo, Efeito efeito) {
		effectProcessor.aplicarEfeito(alvo, efeito);
	}

	public void aplicarBuffDanoEstadoDourado(Personagem personagem) {
		effectProcessor.aplicarBuffDanoEstadoDourado(personagem);
	}

	public void atualizarAuras(EstadoCombate estado) {
		auraManager.atualizarAuras(estado);
	}
}
