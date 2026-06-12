package br.com.dantesrpg.controller;

// Imports JavaFX
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

// Imports IO e Utilities
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Imports do seu Model
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import br.com.dantesrpg.model.classes.Barbaro;
import br.com.dantesrpg.model.classes.Campeao;
import br.com.dantesrpg.model.classes.Feiticeiro;
import br.com.dantesrpg.model.classes.Ilusionista;
import br.com.dantesrpg.model.classes.Invocador;
import br.com.dantesrpg.model.classes.Ladino;
import br.com.dantesrpg.model.classes.MestreDasBalas;
import br.com.dantesrpg.model.classes.Paladino;
import br.com.dantesrpg.model.classes.Pistoleiro;
import br.com.dantesrpg.model.classes.Pugilista;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.map.Dominio;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.racas.AnjoCaido;
import br.com.dantesrpg.model.racas.Anao;
import br.com.dantesrpg.model.racas.Elfo;
import br.com.dantesrpg.model.racas.HalfAngel;
import br.com.dantesrpg.model.racas.HalfDemon;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.racas.Marionette;
import br.com.dantesrpg.model.racas.RaçaPlaceholder;
import br.com.dantesrpg.model.racas.Lobisomem;
import br.com.dantesrpg.model.racas.Vampiro;
import br.com.dantesrpg.model.racas.Arcanjo;
import java.io.File;

import br.com.dantesrpg.model.items.EssenciaInimigo;
import br.com.dantesrpg.model.FantasmaNobre;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.controller.service.BestiarioSpawnService;
import br.com.dantesrpg.controller.service.CatalogoItensService;
import br.com.dantesrpg.controller.service.CombatUiRefresher;
import br.com.dantesrpg.controller.service.EstadoJogadoresService;
import br.com.dantesrpg.controller.service.FantasmaNobreActionService;
import br.com.dantesrpg.controller.service.JanelasCombateCoordinator;
import br.com.dantesrpg.controller.service.MapaCombateCoordinator;
import br.com.dantesrpg.controller.service.PersonagemJsonService;
import br.com.dantesrpg.controller.service.PromptCombateService;
import br.com.dantesrpg.controller.service.ReforcosDialogService;
import br.com.dantesrpg.controller.service.SquadCombateCoordinator;
import br.com.dantesrpg.controller.service.TurnoCombateCoordinator;

public class CombatController {

	@FXML
	private BorderPane rootPane;
	@FXML
	private BorderPane contextPane;
	@FXML
	private Button btnGerenciarCombate;
	@FXML
	private Button btnMovimentoLivre;
	@FXML
	private Button btnIniciarTurno;
	@FXML
	private Button btnEditorMapa;
	@FXML
	private Button btnBestiario;
	@FXML
	private Button btnCarregarArena;
	@FXML
	private Button btnReforcos;
	@FXML
	private HBox timelineContainer;
	@FXML
	private VBox playerListContainer;
	@FXML
	private VBox enemyListContainer;
	@FXML
	private Button btnAbaCombate;
	@FXML
	private Button btnAbaLojas;
	@FXML
	private Button btnAbaEditor;
	@FXML
	private javafx.scene.control.ToggleButton btnCombateState;
	@FXML
	private Button btnConfirmarMovimento;

	// --- Componentes Lógicos ---
	private CombatManager combatManager;
	private EstadoCombate estadoCombate;

	private Node combatViewCenterNode;
	private BorderPane lojaViewNode;
	private LojaController lojaController;
	private BorderPane editorViewNode;
	private EditorJogadorController editorJogadorController;
	private BorderPane criarViewNode;
	private CriarController criarController;

	private Map<String, Map<String, Object>> armoryDatabase;
	private Map<String, Map<String, Object>> itempediaDatabase;
	private Map<String, Map<String, Object>> bestiarioDatabase;
	private final CatalogoItensService catalogoItensService = new CatalogoItensService();
	private final BestiarioSpawnService bestiarioSpawnService = new BestiarioSpawnService(this, catalogoItensService,
			() -> estadoCombate);
	private final CombatUiRefresher combatUiRefresher = new CombatUiRefresher(this, () -> estadoCombate,
			() -> playerListContainer, () -> enemyListContainer, () -> timelineContainer);
	private final FantasmaNobreActionService fantasmaNobreActionService = new FantasmaNobreActionService(
			() -> estadoCombate, () -> combatManager, this::getPrimaryMap, this::forEachMap,
			this::atualizarInterfaceRoster, this::fecharHudEAvançar);
	private final PersonagemJsonService personagemJsonService = new PersonagemJsonService(this, catalogoItensService,
			this::mapearRaca, this::mapearClasse, this::instanciarFantasmaNobre, () -> bestiarioDatabase);
	private final EstadoJogadoresService estadoJogadoresService = new EstadoJogadoresService(() -> estadoCombate,
			this::carregarPersonagemComGson, this::salvarPersonagem, this::isPlayer, this::atualizarInterfaceTotal);
	private final ReforcosDialogService reforcosDialogService = new ReforcosDialogService(this, () -> estadoCombate,
			() -> armoryDatabase, () -> itempediaDatabase, this::listarArquivosPersonagens,
			this::carregarPersonagemComGson, this::mapearRaca, this::mapearClasse, this::instanciarFantasmaNobre,
			this::getArma, this::getItem, this::salvarPersonagem, this::atualizarInterfaceRoster);

	// --- Referências da Pop-up HUD de Turno ---
	private Stage detailedTurnHudStage;
	private DetailedTurnHUDController detailedTurnHudController;

	// --- REFERÊNCIAS DO MAPA ---
	private MapController mapController;
	private MapController embeddedMapController;

	private File arquivoMapaAtual = null;

	private final JanelasCombateCoordinator janelasCombateCoordinator = new JanelasCombateCoordinator(this,
			() -> estadoCombate, () -> combatManager, () -> armoryDatabase, () -> itempediaDatabase,
			() -> bestiarioDatabase, () -> arquivoMapaAtual, map -> this.mapController = map,
			this::carregarMetadadosDoMapa, () -> detailedTurnHudStage);
	private final PromptCombateService promptCombateService = new PromptCombateService(() -> estadoCombate,
			() -> combatManager, () -> detailedTurnHudStage, janelasCombateCoordinator, this::getBonusDificuldadeAndar,
			this::atualizarInterfaceTotal, this::popularListasDeCombatentes);
	private final MapaCombateCoordinator mapaCombateCoordinator = new MapaCombateCoordinator(() -> estadoCombate,
			() -> combatManager, this::getPrimaryMap, this::forEachMap, () -> arquivoMapaAtual,
			file -> this.arquivoMapaAtual = file, () -> rootPane.getScene().getWindow(),
			() -> btnConfirmarMovimento, () -> detailedTurnHudStage, this::reabrirHudParaAtor,
			this::popularListasDeCombatentes, this::atualizarTimelineTU, this::limparEstadoSquadTemporario,
			this::isPlayer, this::resolverAcaoPassarVez);
	private final SquadCombateCoordinator squadCombateCoordinator = new SquadCombateCoordinator(() -> estadoCombate,
			() -> combatManager, () -> detailedTurnHudStage, () -> detailedTurnHudController, this::forEachMap,
			this::avancarTurnoAposAcao, this::aplicarPassarVez, this::fecharHudEAvançar);
	private final TurnoCombateCoordinator turnoCombateCoordinator = new TurnoCombateCoordinator(() -> estadoCombate,
			() -> combatManager, () -> mapController, () -> detailedTurnHudStage, this::forEachMap,
			this::devePassarSquadDeClones, this::passarTurnoSquadAtual, this::aplicarPassarVez, this::limparTUPreview,
			this::popularListasDeCombatentes, this::removerDestaques, this::getProximoAtorCalculado,
			this::verificarFimDeCombate, this::atualizarTimelineTU);

	private String efeitoAndarAtual = "Nenhum";
	private boolean efeitoAndarAtivo = false;

	@FXML
	public void initialize() {
		this.combatViewCenterNode = rootPane.getCenter();
		System.out.println("COMBATE: Iniciando...");

		loadArmoryDatabase();
		loadItempediaDatabase();
		loadBestiarioDatabase();

		this.combatManager = new CombatManager(this);
		List<Personagem> todosCombatentes = criarTodosOsCombatentes();

		if (todosCombatentes.isEmpty()) {
			System.err.println("Erro Crítico: Nenhum combatente carregado.");
			btnIniciarTurno.setDisable(true);
			return;
		}
		this.estadoCombate = new EstadoCombate(todosCombatentes);
		this.estadoCombate.setCombatManager(combatManager);

		for (Personagem p : estadoCombate.getCombatentes()) {
			if (p.getArmaEquipada() != null) {
				p.getArmaEquipada().onCombatStart(p, estadoCombate);
			}
			if (p.getFantasmaNobre() != null) {
				p.getFantasmaNobre().onCombatStart(p, estadoCombate, combatManager);
			}
		}

		rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene != null) {
				newScene.setOnKeyPressed(event -> {
					if (event.getCode() == javafx.scene.input.KeyCode.SPACE) {
						onSpacebarPressed();
					}
				});
			}
		});

		setupEmbeddedMap();

		popularListasDeCombatentes();
		atualizarTimelineTU();

		onAbaCombateClick();
	}

	private void onSpacebarPressed() {
		if (estadoCombate == null || !estadoCombate.isCombateAtivo())
			return;

		Personagem ator = estadoCombate.getAtorAtual();
		if (ator == null) {
			onIniciarTurnoClick();
			return;
		}

		if (isPlayer(ator)) {
			// Cria um input vazio apenas para passar a vez
			AcaoMestreInput input = new AcaoMestreInput(ator, new ArrayList<>(), (Habilidade) null);
			resolverAcaoPassarVez(input);
			System.out.println("ATALHO: Espaço pressionado -> Passar Vez.");
		} else {
			AcaoMestreInput input = new AcaoMestreInput(ator, new ArrayList<>(), (Habilidade) null);
			resolverAcaoPassarVez(input);
			System.out.println("ATALHO: Espaço pressionado -> Iniciar Turno (Inimigo).");
		}
	}

	private void launchMapWindow() {
		janelasCombateCoordinator.abrirMapaExterno();
	}


	/**
	 * Carrega o mapa embedded (sem toolbar) e injeta no centro do contextPane.
	 * Chamado uma vez no initialize. O embedded é o controller PRIMÁRIO do mapa
	 * — sempre existe enquanto o CombatView estiver aberto.
	 */
	private void setupEmbeddedMap() {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/EmbeddedMapView.fxml"));
			Parent embeddedRoot = loader.load();
			this.embeddedMapController = loader.getController();
			this.embeddedMapController.setMainController(this);
			contextPane.setCenter(embeddedRoot);
		} catch (Exception e) {
			System.err.println("Erro crítico ao carregar EmbeddedMapView.fxml:");
			e.printStackTrace();
		}
	}

	/**
	 * Executa uma ação em todas as instâncias ativas de MapController
	 * (embedded sempre, externo se aberto). Usado para manter ambos sincronizados.
	 */
	public void forEachMap(java.util.function.Consumer<MapController> action) {
		if (embeddedMapController != null)
			action.accept(embeddedMapController);
		if (mapController != null)
			action.accept(mapController);
	}

	/**
	 * Retorna o MapController primário para consultas de leitura.
	 * Prefere o embedded (sempre existe); cai para o externo se necessário.
	 */
	public MapController getPrimaryMap() {
		return embeddedMapController != null ? embeddedMapController : mapController;
	}

	private void atualizarInterfaceRoster() {
		popularListasDeCombatentes();
		atualizarTimelineTU();
		if (estadoCombate != null) {
			forEachMap(m -> m.desenharPeoes(estadoCombate.getCombatentes()));
		}
	}

	private void loadArmoryDatabase() {
		catalogoItensService.carregarArmaria();
		this.armoryDatabase = catalogoItensService.getArmoryDatabase();
	}

	private List<Personagem> criarTodosOsCombatentes() {
		List<Personagem> combatentes = new ArrayList<>();
		combatentes.addAll(criarJogadoresDeTeste());
		return combatentes;
	}

	public void abrirJanelaResolucao(Personagem atacante, List<Personagem> alvos, Habilidade habilidade,
			Map<Personagem, List<br.com.dantesrpg.model.util.DamageEvent>> mapaDanos) {
		janelasCombateCoordinator.abrirJanelaResolucao(atacante, alvos, habilidade, mapaDanos);
	}


	@FXML
	private void onCarregarArenaClick() {
		mapaCombateCoordinator.carregarArenaComSeletor();
	}

	@FXML
	private void onToggleCombateClick() {
		boolean isCombate = btnCombateState.isSelected();
		estadoCombate.setCombateAtivo(isCombate);
		btnIniciarTurno.setDisable(!isCombate); // Só avança turno se combate ativo

		if (isCombate) {
			// --- COMBATE INICIADO (ON) ---
			System.out.println("\n=== COMBATE INICIADO ===");
			btnCombateState.setText("COMBATE: ON");
			btnCombateState.setStyle(
					"-fx-background-color: #550000; -fx-text-fill: red; -fx-border-color: red; -fx-font-weight: bold; -fx-font-size: 14px;");

			// Aplica Contratos Pendentes (Humanos) e hooks de início de combate
			for (Personagem p : estadoCombate.getCombatentes()) {
				if (p.getRaca() instanceof Humano) {
					((Humano) p.getRaca()).avancarProximoContrato(p);
				}
				// Hook de início de combate (Raça V2)
				if (p.getRaca() != null) {
					p.getRaca().onCombatStart(p, estadoCombate);
				}
				// Reset de movimento para começar fresco
				p.setMovimentoRestanteTurno(p.getMovimento());
			}

			// Estabelece Iniciativa
			estadoCombate.resetarIniciativa();

		} else {
			// --- COMBATE ENCERRADO (OFF) ---
			System.out.println("\n=== COMBATE ENCERRADO ===");
			btnCombateState.setText("COMBATE: OFF");
			btnCombateState.setStyle(
					"-fx-background-color: #333; -fx-text-fill: gray; -fx-border-color: gray; -fx-font-weight: bold; -fx-font-size: 14px;");

			// Distribui XP
			combatManager.distribuirXpAposCombate(estadoCombate);
			br.com.dantesrpg.model.util.SessionLogger.log("--- Combate Finalizado pelo Mestre ---");
			mapaCombateCoordinator.encerrarEmprestimosOvertime();
			mapaCombateCoordinator.encerrarContratosBarbaros();
			mapaCombateCoordinator.encerrarPosturasAnao();
			limparClonesDoCombate();

			// Limpa dados temporários
			estadoCombate.setAtorAtual(null);
		}

		atualizarInterfaceTotal();
	}

	private void popularListasDeCombatentes() {
		combatUiRefresher.popularListasDeCombatentes();
	}

	// --- Métodos de Carregamento com Gson ---

	private Personagem carregarPersonagemComGson(String nomeArquivo) {
		return personagemJsonService.carregarPersonagemComGson(nomeArquivo);
	}

	public void resolverAcaoFantasmaNobre(AcaoMestreInput input) {
		fantasmaNobreActionService.resolverAcaoFantasmaNobre(input);
	}

	public void resolverAcaoInvocacao(Personagem ator, FantasmaNobre fn, EssenciaInimigo essencia) {
		fantasmaNobreActionService.resolverAcaoInvocacao(ator, fn, essencia);
	}
	private Raça mapearRaca(String nomeRaca) {
		if ("Humano".equalsIgnoreCase(nomeRaca))
			return new Humano();
		if ("Anao".equalsIgnoreCase(nomeRaca) || "Anao".equalsIgnoreCase(nomeRaca))
			return new Anao();
		if ("Vampiro".equalsIgnoreCase(nomeRaca))
			return new Vampiro();
		if ("Elfo".equalsIgnoreCase(nomeRaca))
			return new Elfo();
		if ("Marionette".equalsIgnoreCase(nomeRaca))
			return new Marionette();
		if ("Half-Angel".equalsIgnoreCase(nomeRaca))
			return new HalfAngel();
		if ("Half-Demon".equalsIgnoreCase(nomeRaca))
			return new HalfDemon();
		if ("Anjo-Caido".equalsIgnoreCase(nomeRaca))
			return new AnjoCaido();
		if ("Lobisomem".equalsIgnoreCase(nomeRaca))
			return new Lobisomem();
		if ("Arcanjo".equalsIgnoreCase(nomeRaca))
			return new Arcanjo();
		System.err.println("Raça não reconhecida: " + nomeRaca);
		return new RaçaPlaceholder();
	}

	private Classe mapearClasse(String nomeClasse) {
		if ("Barbaro".equalsIgnoreCase(nomeClasse) || "Bárbaro".equalsIgnoreCase(nomeClasse))
			return new Barbaro();
		if ("Pugilista".equalsIgnoreCase(nomeClasse))
			return new Pugilista();
		if ("Feiticeiro".equalsIgnoreCase(nomeClasse))
			return new Feiticeiro();
		if ("Mestre das Balas".equalsIgnoreCase(nomeClasse))
			return new MestreDasBalas();
		if ("Pistoleiro".equalsIgnoreCase(nomeClasse))
			return new Pistoleiro();
		if ("Ladino".equalsIgnoreCase(nomeClasse))
			return new Ladino();
		if ("Paladino".equalsIgnoreCase(nomeClasse))
			return new Paladino();
		if ("Ilusionista".equalsIgnoreCase(nomeClasse))
			return new Ilusionista();
		if ("Invocador".equalsIgnoreCase(nomeClasse))
			return new Invocador();
		if ("Campeão".equalsIgnoreCase(nomeClasse))
			return new Campeao();
		System.err.println("Classe não reconhecida: " + nomeClasse);
		return new ClassePlaceholder();
	}

	public void carregarEstadoJogadores() {
		estadoJogadoresService.carregarEstadoJogadores();
	}

	private List<Personagem> criarJogadoresDeTeste() {
		return estadoJogadoresService.criarJogadoresIniciais();
	}
	public int getBonusDificuldadeAndar() {
		if (this.efeitoAndarAtual == null)
			return 0;

		String andarAtual = this.efeitoAndarAtual;

		if (andarAtual.startsWith("2º"))
			return 0;
		if (andarAtual.startsWith("3º") || andarAtual.startsWith("4º"))
			return 1;
		if (andarAtual.startsWith("5º") || andarAtual.startsWith("6º"))
			return 2;
		if (andarAtual.startsWith("7º"))
			return 3;
		if (andarAtual.startsWith("8º"))
			return 4;
		if (andarAtual.startsWith("9º"))
			return 5;

		return 0;
	}

	public boolean solicitarTesteDeAtributo(Personagem p, Atributo atr, int dificuldadeNA) {
		return promptCombateService.solicitarTesteDeAtributo(p, atr, dificuldadeNA);
	}


	@FXML
	private void onIniciarTurnoClick() {
		if (estadoCombate == null || !estadoCombate.isCombateAtivo())
			return;

		for (Personagem p : estadoCombate.getCombatentes()) {
			if (p.getRaca() instanceof Humano) {
				Humano humano = (Humano) p.getRaca();

				if (humano.getEstadoAtual() == Humano.EstadoEmprestimo.PENDENTE_RESOLUCAO) {
					// PAUSA TUDO E ABRE O POP-UP
					abrirResolucaoEmprestimo(p, humano);
					return; // NÃO AVANÇA O TURNO AINDA
				}
			}
		}

		// Avança para o próximo ator no Manager
		combatManager.proximoTurno(estadoCombate);
		Personagem atorAtual = estadoCombate.getAtorAtual();

		if (atorAtual == null) {
			verificarFimDeCombate();
			return;
		}

		// Verifica se é Clone
		if (atorAtual.isClone() && atorAtual.getCriador() != null) {
			System.out.println(">>> Turno de Clone Detectado! Preparando Esquadrão.");

			// Identifica todos os irmãos
			List<Personagem> clonesDoTurno = new ArrayList<>();
			for (Personagem p : estadoCombate.getCombatentes()) {
				if (p.isAtivoNoCombate() && p.isClone() && p.getCriador() == atorAtual.getCriador()) {
					p.setMovimentoRestanteTurno(p.getMovimento());
					clonesDoTurno.add(p);
				}
			}

			// Salva a lista para usar quando o botão da HUD for clicado
			squadCombateCoordinator.definirClonesDoTurno(clonesDoTurno);

			// Abre a HUD apenas para o PRIMEIRO
			iniciarTurnoParaAtor(atorAtual);

		} else {
			// Turno Normal
			squadCombateCoordinator.limparClonesDoTurno();
			iniciarTurnoParaAtor(atorAtual);
		}
	}

	private void abrirResolucaoEmprestimo(Personagem p, Humano humano) {
		promptCombateService.abrirResolucaoEmprestimo(p, humano);
	}

	public boolean solicitarTesteGatilhoVeloz(Personagem pistoleiro, Personagem atacante) {
		return promptCombateService.solicitarTesteGatilhoVeloz(pistoleiro, atacante);
	}


	private void limparEstadoSquadTemporario() {
		squadCombateCoordinator.limparEstadoTemporario();
	}

	public void iniciarAtaqueSquad(Habilidade habilidadeAcao, Habilidade habilidadeSelecao, int rolagemGlobal,
			ModoAtaque modoAtaque, int tirosExtras) {
		squadCombateCoordinator.iniciarAtaqueSquad(habilidadeAcao, habilidadeSelecao, rolagemGlobal, modoAtaque,
				tirosExtras);
	}

	public void executarAcaoClonesSemAlvo(Habilidade habilidade, int rolagemGlobal) {
		squadCombateCoordinator.executarAcaoClonesSemAlvo(habilidade, rolagemGlobal);
	}

	public void retornarDoSquadComAlvos(Map<Personagem, Personagem> ataquesDefinidos) {
		squadCombateCoordinator.retornarDoSquadComAlvos(ataquesDefinidos);
	}

	public void executarAtaqueSquadFinal() {
		squadCombateCoordinator.executarAtaqueSquadFinal();
	}

	public void resolverAtaqueCoordenado(Map<Personagem, Personagem> ataques, Habilidade habilidade,
			int rolagemGlobal) {
		squadCombateCoordinator.resolverAtaqueCoordenado(ataques, habilidade, rolagemGlobal);
	}

	private void iniciarTurnoParaAtor(Personagem ator) {
		estadoCombate.setAtorAtual(ator);
		System.out.println("É o turno de: " + ator.getNome());

		destacarCardAtor(ator);
		// if (mapController != null) mapController.centralizarEm(ator); <- não apagar

		try {
			if (detailedTurnHudStage == null) {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/br/com/dantesrpg/view/DetailedTurnHUD.fxml"));
				if (loader.getLocation() == null)
					throw new IOException("DetailedTurnHUD.fxml não encontrado");
				Parent detailedTurnHudRoot = loader.load();
				detailedTurnHudController = loader.getController();
				detailedTurnHudStage = new Stage();
				Window ownerWindow = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
				if (ownerWindow != null)
					detailedTurnHudStage.initOwner(ownerWindow);
				detailedTurnHudStage.setResizable(false);
				detailedTurnHudStage.setMinWidth(920);
				detailedTurnHudStage.setMinHeight(520);
				detailedTurnHudStage.setScene(new Scene(detailedTurnHudRoot));
			}
			detailedTurnHudStage.setTitle("Ações Detalhadas de " + ator.getNome());
			detailedTurnHudController.setAtor(ator, this);
			detailedTurnHudStage.show();
		} catch (Exception e) {
			System.err.println("Erro crítico ao carregar/mostrar DetailedTurnHUD.fxml:");
			e.printStackTrace();
		}
	}

	@FXML
	private void onAbaLojasClick() {
		System.out.println("Botão 'Lojas' clicado.");
		try {
			// Carrega a view da loja (apenas na primeira vez)
			if (lojaViewNode == null) {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/LojaView.fxml"));
				lojaViewNode = loader.load();
				lojaController = loader.getController();
			}

			// mudar baseando na loja uau
			lojaController.inicializarLoja(this, this.estadoCombate, "LOJA_PRINCIPAL_TESTE");

			rootPane.setCenter(lojaViewNode);

		} catch (IOException e) {
			System.err.println("Erro ao carregar LojaView.fxml:");
			e.printStackTrace();
		}
	}

	@FXML
	private void onAbaCombateClick() {
		System.out.println("Botão 'Combate' clicado.");

		rootPane.setCenter(combatViewCenterNode);
		popularListasDeCombatentes();
		atualizarTimelineTU();
	}

	@FXML
	private void onAbaEditorClick() {
		System.out.println("Botão 'Editor' clicado.");
		try {
			// Carrega a view do editor (apenas na primeira vez)
			if (editorViewNode == null) {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/br/com/dantesrpg/view/EditorJogadorView.fxml"));
				editorViewNode = loader.load(); // Carrega como BorderPane (ou o root que você usou)
				editorJogadorController = loader.getController();
			}

			// Inicializa o editor com os dados de combate atuais
			editorJogadorController.inicializar(this, this.estadoCombate);

			// Define o "miolo" da tela como a view do Editor
			rootPane.setCenter(editorViewNode);

		} catch (IOException e) {
			System.err.println("Erro ao carregar EditorJogadorView.fxml:");
			e.printStackTrace();
		}
	}

	private FantasmaNobre instanciarFantasmaNobre(String nomeExibicao) {
		return fantasmaNobreActionService.instanciarFantasmaNobre(nomeExibicao);
	}
	private List<String> listarArquivosPersonagens() {
		return estadoJogadoresService.listarArquivosPersonagens();
	}
	@FXML
	private void onReforcosClick() {
		reforcosDialogService.abrirPainel();
	}

	public void resolverAcaoDoMestre(AcaoMestreInput input) {
		turnoCombateCoordinator.resolverAcaoDoMestre(input);
	}

	public void resolverAcaoPassarVez(AcaoMestreInput input) {
		turnoCombateCoordinator.resolverAcaoPassarVez(input);
	}

	private boolean devePassarSquadDeClones(Personagem ator) {
		return squadCombateCoordinator.devePassarSquadDeClones(ator);
	}

	private void aplicarPassarVez(Personagem ator) {
		if (ator == null)
			return;

		if (!(ator.getRaca() instanceof Marionette)) {
			System.out.println(ator.getNome() + " decide Passar a Vez.");

			double cura = ator.getVidaMaxima() * 0.05;
			if (cura < 1.0)
				cura = 1.0; // Mínimo 1 HP

			ator.setVidaAtual(ator.getVidaAtual() + cura, estadoCombate, this);
			System.out.println(">>> Recuperou " + String.format("%.1f", cura) + " HP.");
		}

		ator.setManaAtual(ator.getManaAtual() + 1);
		ator.setContadorTU(ator.getContadorTU() + 100);

		// Hook do Elfo
		TipoAcao tipoAcaoAtual = TipoAcao.PASSAR_VEZ; // Enum
		TipoAcao tipoAcaoAnterior = combatManager.getUltimoTipoAcao(ator);
		if (ator.getRaca() != null) {
			ator.getRaca().onActionUsed(ator, tipoAcaoAnterior, tipoAcaoAtual, estadoCombate);
		}
		combatManager.setUltimoTipoAcao(ator, tipoAcaoAtual);
	}

	public void passarTurnoSquadAtual() {
		squadCombateCoordinator.passarTurnoSquadAtual();
	}

	public void verificarInteracaoTerreno(Personagem ator) {
		turnoCombateCoordinator.verificarInteracaoTerreno(ator);
	}

	private void fecharHudEAvançar() {
		turnoCombateCoordinator.fecharHudEAvancar();
	}

	public void iniciarSelecaoDeAlvo(Habilidade habilidade, Personagem ator) {
		turnoCombateCoordinator.iniciarSelecaoDeAlvo(habilidade, ator);
	}

	private void avancarParaProximoTurno() {
		turnoCombateCoordinator.avancarParaProximoTurno();
	}

	public Optional<Personagem> encontrarAlvoValido(boolean atacanteIsPlayer) {
		if (estadoCombate == null || estadoCombate.getCombatentes() == null)
			return Optional.empty();
		return estadoCombate.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && (atacanteIsPlayer != isPlayer(p))).findFirst();
	}

	private void atualizarTimelineTU() {
		combatUiRefresher.atualizarTimelineTU();
	}

	public void mostrarTUPreview(Personagem ator, int tuPrevisto) {
		combatUiRefresher.mostrarTUPreview(ator, tuPrevisto);
	}

	public void limparTUPreview() {
		combatUiRefresher.limparTUPreview();
	}

	private Personagem getProximoAtorCalculado() {
		return combatUiRefresher.getProximoAtorCalculado();
	}

	private boolean verificarFimDeCombate() {
		if (estadoCombate == null)
			return true;

		// Se o botão de combate estiver DESLIGADO, o combate "acabou"
		if (!estadoCombate.isCombateAtivo())
			return true;
		return false;
	}

	public void carregarNovaArenaLogic() {
		mapaCombateCoordinator.carregarNovaArenaLogic();
	}

	private void limparClonesDoCombate() {
		mapaCombateCoordinator.limparClonesDoCombate();
	}

	private void carregarMetadadosDoMapa(File imagemFile) {
		mapaCombateCoordinator.carregarMetadadosDoMapa(imagemFile);
	}
	@FXML
	private void onSalvarEstadoClick() {
		salvarEstadoJogadores(); // Chama o método que já criamos anteriormente

		// Feedback visual simples no console
		System.out.println("GM: Botão 'Salvar Estado' acionado na Toolbar.");

		javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
				javafx.scene.control.Alert.AlertType.INFORMATION);
		alert.setTitle("Sistema");
		alert.setHeaderText(null);
		alert.setContentText("Estado de todos os jogadores salvo com sucesso!");
		alert.show();
	}

	// Ação do Botão Salvar
	public void onSalvarMapaJsonClick() {
		mapaCombateCoordinator.salvarMapaJson();
	}

	private void removerDestaques() {
		combatUiRefresher.removerDestaques();
	}

	private void destacarCardAtor(Personagem atorAtual) {
		combatUiRefresher.destacarCardAtor(atorAtual);
	}

	private void aplicarEstiloDestaque(Personagem atorParaDestacar) {
		combatUiRefresher.destacarCardAtor(atorParaDestacar);
	}

	public boolean isPlayer(Personagem p) {
		if (p == null || p.getFaccao() == null)
			return false;
		return p.getFaccao().equals("JOGADOR");
	}

	public void resolverAcaoItem(AcaoMestreInput input) {
		turnoCombateCoordinator.resolverAcaoItem(input);
	}

	public List<Personagem> getCombatentes() {
		if (this.estadoCombate != null) {
			return this.estadoCombate.getCombatentes();
		}
		return new ArrayList<>();
	}

	public void adicionarAlvoSelecionado(Personagem alvo) {
		if (detailedTurnHudController != null) {
			detailedTurnHudController.adicionarAlvo(alvo);

			// REABRE A HUD
			if (detailedTurnHudStage != null) {
				detailedTurnHudStage.show();
				detailedTurnHudStage.toFront();
			}
		}
	}

	// Chamado pelo MapController quando seleciona área
	public void adicionarAlvosArea(List<Personagem> alvos, int x, int y) {
		if (detailedTurnHudController != null) {
			detailedTurnHudController.adicionarAlvosArea(alvos, x, y);

			// REABRE A HUD
			if (detailedTurnHudStage != null) {
				detailedTurnHudStage.show();
				detailedTurnHudStage.toFront();
			}
		}
	}

	public void notificarMovimentoRealizado() {
		mapaCombateCoordinator.notificarMovimentoRealizado();
	}

	public void adicionarAlvosSelecionados(List<Personagem> alvos) {
		if (detailedTurnHudController != null) {
			// Atualiza a lista na HUD
			detailedTurnHudController.adicionarAlvos(alvos);

			if (detailedTurnHudStage != null) {
				detailedTurnHudStage.show();
				detailedTurnHudStage.toFront();
				detailedTurnHudStage.requestFocus();
			}
		}
	}

	public void solicitarRolagemFantasmaDoDeserto(Personagem ator, Personagem alvo) {
		promptCombateService.solicitarRolagemFantasmaDoDeserto(ator, alvo);
	}

	public void resolverRolagemReacao(String rolagemStr) {
		promptCombateService.resolverRolagemReacao(rolagemStr);
	}


	public CombatManager getCombatManager() {
		return this.combatManager;
	}

	public void resolverAcaoFugir(Personagem ator) {
		turnoCombateCoordinator.resolverAcaoFugir(ator);
	}

	// === SISTEMA GENÉRICO DE DOMÍNIOS ===

	/** Registra e desenha um domínio nos mapas (embedded + externo se aberto). */
	public void registrarDominio(Dominio dominio) {
		mapaCombateCoordinator.registrarDominio(dominio);
	}

	/** Remove um domínio pelo ID. */
	public void removerDominio(String dominioId) {
		mapaCombateCoordinator.removerDominio(dominioId);
	}

	/** Verifica se um personagem está dentro de um domínio específico. */
	public boolean isPersonagemNoDominio(Personagem p, String dominioId) {
		return mapaCombateCoordinator.isPersonagemNoDominio(p, dominioId);
	}

	/** Retorna todos os domínios ativos no mapa. */
	public java.util.Map<String, Dominio> getDominiosAtivos() {
		return mapaCombateCoordinator.getDominiosAtivos();
	}

	/** Retorna um domínio ativo pelo ID, ou null. */
	public Dominio getDominio(String dominioId) {
		return mapaCombateCoordinator.getDominio(dominioId);
	}

	// === MÉTODOS RETROCOMPATÍVEIS (delegam ao sistema genérico) ===

	public void limparRingueDoMapa() {
		removerDominio("ringue_alexei");
	}

	public void desenharRingueDoMapa(Personagem centro, int tamanho) {
		mapaCombateCoordinator.desenharRingueDoMapa(centro, tamanho);
	}

	public void desenharDominioLyriaNoMapa(Personagem centro, int tamanho) {
		mapaCombateCoordinator.desenharDominioLyriaNoMapa(centro, tamanho);
	}

	public void limparDominioLyriaDoMapa() {
		removerDominio("dominio_lyria");
	}

	public void spawnarCloneIlusao(Personagem invocador) {
		System.out.println(">>> ILUSÃO: Criando clone de " + invocador.getNome());

		// Cria os atributos mínimos
		Map<Atributo, Integer> atributosMinimos = new HashMap<>();
		for (Atributo a : Atributo.values()) {
			atributosMinimos.put(a, 2);
		}

		Personagem clone = new Personagem("Clone de " + invocador.getNome(),
				new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, atributosMinimos, 1, 10);

		clone.setFaccao(invocador.getFaccao());
		clone.setVidaAtual(1);
		clone.setVidaMaxima(1);

		Arma armaCopia = criarCopiaDaArma(invocador.getArmaEquipada());
		clone.setArmaEquipada(armaCopia);

		MapController mapaBusca = getPrimaryMap();
		if (mapaBusca != null) {
			javafx.util.Pair<Integer, Integer> posLivre = mapaBusca
					.encontrarCelulaLivreMaisProxima(invocador.getPosX(), invocador.getPosY());

			if (posLivre != null) {
				clone.setPosX(posLivre.getKey());
				clone.setPosY(posLivre.getValue());

				this.estadoCombate.getCombatentes().add(clone);
				clone.setContadorTU(invocador.getContadorTU() + 50);
				invocador.registrarClone(clone);

				popularListasDeCombatentes();
				atualizarTimelineTU();
				forEachMap(m -> m.desenharPeoes(this.estadoCombate.getCombatentes()));
				System.out.println(">>> Clone spawnado em (" + clone.getPosX() + "," + clone.getPosY() + ")");
			} else {
				System.out.println(">>> FALHA AO SPAWNAR CLONE: Sem espaço livre!");
			}
		}
	}

	public void atualizarInterfaceAposMorte() {
		popularListasDeCombatentes();
		atualizarTimelineTU();
		forEachMap(m -> m.desenharPeoes(estadoCombate.getCombatentes())); // Remove peões mortos
	}

	public void avancarTurnoAposAcao() {
		fecharHudEAvançar();
	}

	public void salvarPersonagem(Personagem personagem) {
		personagemJsonService.salvarPersonagem(personagem);
	}

	private Arma getArma(String nomeArma) {
		return catalogoItensService.getArma(nomeArma);
	}

	public Personagem recarregarPersonagem(String nomeArquivoSemExtensao) {
		return estadoJogadoresService.recarregarPersonagem(nomeArquivoSemExtensao);
	}
	private void loadItempediaDatabase() {
		catalogoItensService.carregarItempedia();
		this.itempediaDatabase = catalogoItensService.getItempediaDatabase();
	}

	public Item getItem(String tipoItem) {
		return catalogoItensService.getItem(tipoItem, bestiarioDatabase);
	}

	private void loadBestiarioDatabase() {
		bestiarioSpawnService.carregarBestiario();
		this.bestiarioDatabase = bestiarioSpawnService.getBestiarioDatabase();
	}

	public void entrarModoSpawnMultiploCustom(Map<String, Object> dadosCustom, int quantidade) {
		bestiarioSpawnService.entrarModoSpawnMultiploCustom(dadosCustom, quantidade);
	}

	public void spawnarMonstro(String idMonstro, int x, int y) {
		bestiarioSpawnService.spawnarMonstro(idMonstro, x, y);
	}

	// Getter auxiliar para a UI do Bestiário listar os nomes
	public Map<String, Map<String, Object>> getBestiarioDatabase() {
		return bestiarioSpawnService.getBestiarioDatabase();
	}

	public void entrarModoSpawn(String idMonstro) {
		bestiarioSpawnService.entrarModoSpawn(idMonstro);
	}

	public void entrarModoSpawnMultiplo(String idMonstro, int quantidade) {
		bestiarioSpawnService.entrarModoSpawnMultiplo(idMonstro, quantidade);
	}

	/**
	 * Chamado por qualquer MapController quando as cargas locais de spawn esgotam.
	 * Limpa o estado global de spawn e sincroniza o outro MapController
	 * (embedded/externo).
	 */
	public void notifySpawnConcluido() {
		bestiarioSpawnService.notifySpawnConcluido();
	}

	public void resolverSpawn(String idMonstro, int x, int y) {
		bestiarioSpawnService.resolverSpawn(idMonstro, x, y);
	}

	public void alvosIdentificadosNoMapa(List<Personagem> alvos) {
		if (detailedTurnHudController != null) {
			// Atualiza a HUD com a lista de alvos potenciais (o que habilita o botão
			// Confirmar)
			detailedTurnHudController.adicionarAlvos(alvos);
		}
	}

	public List<String> getListaNomesArmas() {
		return catalogoItensService.getListaNomesArmas();
	}

	// Chamado pelo MapController quando o mouse sai de uma área válida
	public void limparSelecaoDeAlvo() {
		if (detailedTurnHudController != null) {
			detailedTurnHudController.limparAlvosHover();
		}
	}

	public void atualizarInterfaceTotal() {
		popularListasDeCombatentes();
		atualizarTimelineTU();
		forEachMap(m -> m.desenharPeoes(estadoCombate.getCombatentes()));
		// Notifica o Painel do Mestre para sincronização em tempo real
		notificarGerenciadorCombate();
	}

	public void setEfeitoAndar(String efeito, boolean ativo) {
		this.efeitoAndarAtual = efeito;
		this.efeitoAndarAtivo = ativo;
		System.out.println("GM: Efeito de Andar alterado para: " + efeito + " (Ativo: " + ativo + ")");
	}

	public String getEfeitoAndarAtual() {
		return efeitoAndarAtual;
	}

	public boolean isEfeitoAndarAtivo() {
		return efeitoAndarAtivo;
	}

	public void salvarEstadoJogadores() {
		estadoJogadoresService.salvarEstadoJogadores();
	}
	public void acionarTransicaoDeMapa(Personagem jogadorQuePisou) {
		mapaCombateCoordinator.acionarTransicaoDeMapa(jogadorQuePisou);
	}

	public void iniciarMovimentoTatico(Personagem ator) {
		mapaCombateCoordinator.iniciarMovimentoTatico(ator);
	}

	public void iniciarMovimentoTaticoComRetorno(Personagem ator) {
		mapaCombateCoordinator.iniciarMovimentoTaticoComRetorno(ator);
	}

	@FXML
	private void onConfirmarMovimentoClick() {
		mapaCombateCoordinator.confirmarMovimento();
	}

	private void reabrirHudParaAtor(Personagem ator) {
		if (detailedTurnHudStage != null && detailedTurnHudController != null) {
			detailedTurnHudController.setAtor(ator, this);
			detailedTurnHudStage.setTitle("Ações Detalhadas de " + ator.getNome());
			detailedTurnHudStage.show();
		}
	}
	public void criarObjetoNoMapa(int x, int y) {
		mapaCombateCoordinator.criarObjetoNoMapa(x, y);
	}

	// Chamado pelo Editor de Mapa (ao apagar) ou pelo CombatManager (ao destruir)
	public void removerObjetoNoMapa(int x, int y) {
		mapaCombateCoordinator.removerObjetoNoMapa(x, y);
	}
	public MapController getMapController() {
		return getPrimaryMap();
	}

	// Método auxiliar para limpar e carregar
	private void carregarNovaArenaLogic(File mapaFile) {
		mapaCombateCoordinator.carregarNovaArenaLogic(mapaFile);
	}
	private Arma criarCopiaDaArma(Arma original) {
		return catalogoItensService.criarCopiaDaArma(original);
	}

	// --- Ações GM Toolbar Placeholders ---
	@FXML
	private void onGerenciarCombateClick() {
		janelasCombateCoordinator.abrirGerenciadorCombate();
	}

	public void notificarGerenciadorCombate() {
		janelasCombateCoordinator.notificarGerenciadorCombate();
	}

	@FXML
	private void onMovimentoLivreClick() {
		mapaCombateCoordinator.toggleMovimentoLivre();
	}

	@FXML
	private void onEditorMapaClick() {
		mapaCombateCoordinator.toggleEditorMapa();
	}

	@FXML
	private void onBestiarioClick() {
		abrirJanelaBestiario();
	}

	@FXML
	private void onAbrirMapaExternoClick() {
		launchMapWindow();
	}

	public void abrirJanelaBestiario() {
		janelasCombateCoordinator.abrirJanelaBestiario();
	}

	@FXML
	private void onAbaLogClick() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/SessionLogView.fxml"));
			Parent logRoot = loader.load();
			rootPane.setCenter(logRoot);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onAbaCriarClick() {
		System.out.println("Botão 'Criar' clicado.");
		try {
			if (criarViewNode == null) {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/br/com/dantesrpg/view/CriarView.fxml"));
				criarViewNode = loader.load();
				criarController = loader.getController();
			}
			criarController.inicializar(this);
			rootPane.setCenter(criarViewNode);
		} catch (IOException e) {
			System.err.println("Erro ao carregar CriarView.fxml:");
			e.printStackTrace();
		}
	}

	public void recarregarBancosDeDados() {
		loadArmoryDatabase();
		loadItempediaDatabase();
	}

}
