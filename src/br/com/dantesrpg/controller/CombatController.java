package br.com.dantesrpg.controller;

// Imports JavaFX
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

// Imports Gson
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// Imports IO e Utilities
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Imports do seu Model
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import br.com.dantesrpg.model.classes.Barbaro;
import br.com.dantesrpg.model.classes.Feiticeiro;
import br.com.dantesrpg.model.classes.Ilusionista;
import br.com.dantesrpg.model.classes.Invocador;
import br.com.dantesrpg.model.classes.Ladino;
import br.com.dantesrpg.model.classes.MestreDasBalas;
import br.com.dantesrpg.model.classes.Paladino;
import br.com.dantesrpg.model.classes.Pugilista;
import br.com.dantesrpg.model.elementos.ObjetoDestrutivel;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.racas.AnjoCaido;
import br.com.dantesrpg.model.racas.Anao;
import br.com.dantesrpg.model.racas.Elfo;
import br.com.dantesrpg.model.racas.HalfAngel;
import br.com.dantesrpg.model.racas.HalfDemon;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.racas.Marionette;
import br.com.dantesrpg.model.racas.RaçaPlaceholder;
import br.com.dantesrpg.model.racas.Vampiro;
import br.com.dantesrpg.model.util.BarbaroUtils;
import br.com.dantesrpg.model.util.FileLoader;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;

import br.com.dantesrpg.model.armas.unicas.PunhoInfinito;
import br.com.dantesrpg.model.armas.unicas.Rubrum;
import br.com.dantesrpg.model.armas.unicas.Murasame;
import br.com.dantesrpg.model.armas.unicas.Terrore;
import br.com.dantesrpg.model.armas.unicas.PalidaVigilia;
import br.com.dantesrpg.model.armas.unicas.LaminasDoExterminio;

// Imports para os Fantasmas Nobres
import br.com.dantesrpg.model.fantasmasnobres.RingOfTheUndyingWill;
import br.com.dantesrpg.model.fantasmasnobres.VigiliaEterna;
import br.com.dantesrpg.model.fantasmasnobres.AcertoDeContas;
import br.com.dantesrpg.model.fantasmasnobres.InvocacaoMurasame;
import br.com.dantesrpg.model.fantasmasnobres.ApostadorIncansavel;
import br.com.dantesrpg.model.fantasmasnobres.IraDeAnthyros;
import br.com.dantesrpg.model.items.Consumivel;
import br.com.dantesrpg.model.items.EssenciaInimigo;
import br.com.dantesrpg.model.FantasmaNobre;
import java.io.FileReader;
import br.com.dantesrpg.model.map.MapMetadata;

import br.com.dantesrpg.model.AcaoMestreInput;

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

	private Habilidade habilidadeSquadTemp;
	private int rolagemSquadTemp;
	private ModoAtaque modoAtaqueSquadTemp = ModoAtaque.NORMAL;
	private int tirosExtrasSquadTemp;
	private Map<Personagem, Personagem> ataquesSquadTemp;

	private Map<String, Map<String, Object>> armoryDatabase;
	private Map<String, Map<String, Object>> itempediaDatabase;
	private Map<String, Map<String, Object>> bestiarioDatabase;
	private Map<String, Object> templateSpawnCustomizado = null;

	// --- Referências da Pop-up HUD de Turno ---
	private Stage detailedTurnHudStage;
	private DetailedTurnHUDController detailedTurnHudController;

	// --- REFERÊNCIAS DO MAPA ---
	private Stage mapStage;
	private MapController mapController;

	private File arquivoMapaAtual = null;

	private Stage diceRollStage;
	private DiceRollPromptController diceRollController;

	// --- Referencias do bestiario ---
	private Stage bestiarioStage;
	private BestiarioController bestiarioController;

	private Personagem reacao_Ator;
	private Personagem reacao_Alvo;
	private int reacao_NivelCascata;
	private java.util.Queue<Personagem> filaClonesParaAgir = new java.util.LinkedList<>();

	private boolean modoSpawnAtivo = false;
	private String idMonstroParaSpawn;

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

		launchMapWindow();

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
		try {
			if (mapStage == null) {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/MapView.fxml"));
				Parent mapRoot = loader.load();

				this.mapController = loader.getController();
				this.mapController.setMainController(this);

				this.mapStage = new Stage();
				this.mapStage.setTitle("Modo Combate");
				this.mapStage.setScene(new Scene(mapRoot));
				this.mapStage.setResizable(true);
				this.mapStage.show();
			}
		} catch (Exception e) { // Captura Exception geral (por causa do erro da Imagem)
			System.err.println("Erro crítico ao carregar MapView.fxml ou imagem do mapa:");
			e.printStackTrace();
		}
	}

	private void loadArmoryDatabase() {
		this.armoryDatabase = new HashMap<>();
		Gson gson = new Gson();
		String resourcePath = "/data/armas.json";

		// USA O NOVO MÉTODO
		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null) {
				System.err.println("Erro Crítico: Arquivo da Armaria não encontrado: " + resourcePath);
				return;
			}
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {
				}.getType();
				this.armoryDatabase = gson.fromJson(reader, mapType);
				System.out.println("ARMORIA: Carregada com sucesso.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<Personagem> criarTodosOsCombatentes() {
		List<Personagem> combatentes = new ArrayList<>();
		combatentes.addAll(criarJogadoresDeTeste());
		return combatentes;
	}

	public void abrirJanelaResolucao(Personagem atacante, List<Personagem> alvos, Habilidade habilidade,
			Map<Personagem, List<br.com.dantesrpg.model.util.DamageEvent>> mapaDanos) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/DamageResolutionView.fxml"));
			Parent root = loader.load();

			br.com.dantesrpg.controller.DamageResolutionController controller = loader.getController();

			// INJEÇÃO DE DEPENDÊNCIA
			controller.setMainController(this);

			controller.setupResolution(atacante, habilidade, mapaDanos, this.estadoCombate);

			Stage stage = new Stage();
			stage.setTitle("Resolução de Dano");
			stage.setScene(new Scene(root));

			javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

			// Limita a altura a 80% da tela para não cortar o botão
			double maxHeight = screenBounds.getHeight() * 0.80;

			stage.setHeight(maxHeight);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Erro ao abrir janela de resolução de dano.");
		}
	}

	@FXML
	private void onCarregarArenaClick() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Abrir Arquivo de Arena Tática");

		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("Imagens de Mapa", "*.png", "*.jpg", "*.jpeg"));

		// Começa na pasta do seu projeto (ou na pasta "user")
		String mapPath = System.getProperty("user.dir") + File.separator + "resources" + File.separator + "mapas";
		File initialDirectory = new File(mapPath);
		if (!initialDirectory.exists() || !initialDirectory.isDirectory()) {
			System.err.println("Aviso: Pasta de Mapas não encontrada em " + mapPath + ". Usando diretório padrão.");
			initialDirectory = new File(System.getProperty("user.dir"));
			if (!initialDirectory.exists()) {
				initialDirectory = new File(System.getProperty("user.home"));
			}
		}
		fileChooser.setInitialDirectory(initialDirectory);

		// Abre a janela de seleção
		File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

		if (selectedFile != null) {
			this.arquivoMapaAtual = selectedFile;
			if (mapController != null) {
				// Chama o método atualizado do mapController
				mapController.carregarMapaDeImagem(selectedFile);
				carregarMetadadosDoMapa(selectedFile);

				// Redesenha os peões (eles podem estar em posições de um mapa antigo)
				if (estadoCombate != null) {
					mapController.desenharPeoes(estadoCombate.getCombatentes());
				}
			} else {
				System.err.println("Erro: MapController não está pronto.");
			}
		}
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

			// Aplica Contratos Pendentes (Humanos)
			for (Personagem p : estadoCombate.getCombatentes()) {
				if (p.getRaca() instanceof Humano) {
					((Humano) p.getRaca()).avancarProximoContrato(p);
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
			encerrarContratosBarbaros();
			encerrarPosturasAnao();
			limparClonesDoCombate();

			// Limpa dados temporários
			estadoCombate.setAtorAtual(null);
		}

		atualizarInterfaceTotal();
	}

	private void popularListasDeCombatentes() {
		if (playerListContainer == null || enemyListContainer == null || estadoCombate == null
				|| estadoCombate.getCombatentes() == null)
			return;

		playerListContainer.getChildren().clear();
		enemyListContainer.getChildren().clear();

		if (mapController != null) {
			mapController.desenharPeoes(new ArrayList<>());
		}

		List<Personagem> combatentes = estadoCombate.getCombatentes();

		int playerIndex = 0;
		int enemyIndex = 0;

		for (Personagem p : combatentes) {
			if (p == null)
				continue;
			if (p.isAusente())
				continue;

			if (!isPlayer(p) && !p.isAtivoNoCombate()) {
				continue;
			}
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/PlayerCard.fxml"));
				AnchorPane cardNode = loader.load();
				PlayerCardController cardController = loader.getController();
				cardNode.setUserData(cardController);
				cardController.setPersonagem(p, isPlayer(p) ? "player" : "enemy");

				VBox.setMargin(cardNode, new javafx.geometry.Insets(0, 0, 0, 0));

				if (isPlayer(p)) {
					playerListContainer.getChildren().add(cardNode);

					if (playerIndex % 2 != 0) {
						cardNode.setTranslateX(70);
					} else {
						cardNode.setTranslateX(10);
					}
					playerIndex++;

				} else {
					enemyListContainer.getChildren().add(cardNode);

					if (enemyIndex % 2 != 0) {
						cardNode.setTranslateX(60);
					} else {
						cardNode.setTranslateX(0);
					}
					enemyIndex++;
				}

			} catch (Exception e) {
				System.err.println("Erro ao carregar PlayerCard.fxml para: " + p.getNome());
				e.printStackTrace();
			}
		}

		if (mapController != null && estadoCombate != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes());
		}
	}

	// --- Métodos de Carregamento com Gson ---

	private Personagem carregarPersonagemComGson(String nomeArquivo) {
		Gson gson = new Gson();
		String resourcePath = "/data/players/" + nomeArquivo + ".json";

		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null) {
				System.err.println("Arquivo JSON não encontrado: " + resourcePath);
				return null;
			}
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Type mapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				Map<String, Object> data = gson.fromJson(reader, mapType);

				// campos getOrDefault
				String nome = (String) data.getOrDefault("nome", "Nome Padrão");
				String nomeRaca = (String) data.getOrDefault("raca", "Placeholder");
				String nomeClasse = (String) data.getOrDefault("classe", "Placeholder");
				int nivel = ((Double) data.getOrDefault("nivel", 1.0)).intValue();
				int vidaMaximaBase = ((Double) data.getOrDefault("vidaMaximaBase", 10.0)).intValue();
				int iniciativaBase = ((Double) data.getOrDefault("iniciativaBase", 0.0)).intValue();
				int pontos = ((Double) data.getOrDefault("pontosParaDistribuir", 0.0)).intValue();
				int xpSalvo = 0;
				if (data.containsKey("xpAtual")) {
					xpSalvo = ((Double) data.get("xpAtual")).intValue();
				}
				Map<String, Double> atributosJsonDouble = (Map<String, Double>) data.get("atributosBase");
				Map<Atributo, Integer> atributosBase = new HashMap<>();
				if (atributosJsonDouble != null) {
					for (Map.Entry<String, Double> entry : atributosJsonDouble.entrySet()) {
						try {
							atributosBase.put(Atributo.valueOf(entry.getKey().toUpperCase()),
									entry.getValue().intValue());
						} catch (IllegalArgumentException e) {
							System.err.println("Atributo inválido no JSON '" + nomeArquivo + "': " + entry.getKey());
						}
					}
				} else {
					System.err.println("Aviso: 'atributosBase' não encontrado ou inválido no JSON '" + nomeArquivo
							+ "'. Usando padrão.");
					for (Atributo atr : Atributo.values()) {
						atributosBase.putIfAbsent(atr, 1);
					}
				}

				Raça raca = mapearRaca(nomeRaca);
				Classe classe = mapearClasse(nomeClasse);
				if (raca == null || classe == null)
					return null; // Aborta se mapeamento falhar

				int grau = 0;
				if (data.containsKey("grau")) {
					grau = ((Double) data.get("grau")).intValue();
				}

				// NOVO: Leitura de Segmentos
				int segmentos = 0;
				if (data.containsKey("segmentos")) {
					segmentos = ((Double) data.get("segmentos")).intValue();
				}

				Personagem p = new Personagem(nome, raca, classe, nivel, atributosBase, vidaMaximaBase, iniciativaBase);
				p.setXpAtual(xpSalvo);
				p.setGrau(grau);
				p.setSegmentosVida(segmentos);
				p.setFaccao("JOGADOR");
				p.setPontosParaDistribuir(pontos);
				p.setJsonFileName(nomeArquivo + ".json");

				Arma arma = null;
				Object armaInfo = data.get("armaEquipada");

				if (armaInfo instanceof String) {
					arma = getArma((String) armaInfo);
				} else if (armaInfo instanceof Map) {
					Map<String, Object> armaData = (Map<String, Object>) armaInfo;
					String nomeArma = (String) armaData.get("nome");
					arma = getArma(nomeArma);
					if (arma instanceof Grimorio && armaData.containsKey("magiasSalvas")) {
						Grimorio grimorio = (Grimorio) arma;

						// Limpa as magias padrão
						grimorio.getMagiasArmazenadas().clear();

						List<String> magiasSalvas = (List<String>) armaData.get("magiasSalvas");
						for (String nomeMagia : magiasSalvas) {
							Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory
									.criarHabilidadePorNome(nomeMagia);
							if (h != null) {
								grimorio.aprenderMagia(h);
							}
						}
						System.out.println("GRIMÓRIO CARREGADO: " + nomeArma + " com " + magiasSalvas.size()
								+ " magias customizadas.");
					}
				}

				p.setArmaEquipada(arma);

				Armadura armadura = null;
				Object armaduraObj = data.get("armaduraEquipada");

				if (armaduraObj instanceof String) {
					Item item = getItem((String) armaduraObj);
					if (item instanceof Armadura) {
						armadura = (Armadura) item;
					}
				} else if (armaduraObj instanceof Map) {
					armadura = mapearArmadura((Map<String, Object>) armaduraObj);
				}

				if (armadura != null) {
					p.setArmaduraEquipada(armadura);
				}

				Amuleto amuleto1 = null;
				Object amuleto1Obj = data.get("amuleto1");

				if (amuleto1Obj instanceof String) {
					Item item = getItem((String) amuleto1Obj);
					if (item instanceof Amuleto)
						amuleto1 = (Amuleto) item;
				} else if (amuleto1Obj instanceof Map) {
					amuleto1 = mapearAmuleto((Map<String, Object>) amuleto1Obj);
				}

				if (amuleto1 != null) {
					p.setAmuleto1(amuleto1);
				}

				Amuleto amuleto2 = null;
				Object amuleto2Obj = data.get("amuleto2");

				if (amuleto2Obj instanceof String) {
					Item item = getItem((String) amuleto2Obj);
					if (item instanceof Amuleto)
						amuleto2 = (Amuleto) item;
				} else if (amuleto2Obj instanceof Map) {
					amuleto2 = mapearAmuleto((Map<String, Object>) amuleto2Obj);
				}

				if (amuleto2 != null) {
					p.setAmuleto2(amuleto2);
				}

				Map<String, ?> inventarioData = (Map<String, ?>) data.get("inventario");

				Map<String, Double> carteiraData = (Map<String, Double>) data.get("carteira");
				if (carteiraData != null) {
					// Gson lê números como Double, convertemos para int
					if (carteiraData.containsKey("bronze"))
						p.getInventario().setMoedasBronze(carteiraData.get("bronze").intValue());
					if (carteiraData.containsKey("prata"))
						p.getInventario().setMoedasPrata(carteiraData.get("prata").intValue());
					if (carteiraData.containsKey("ouro"))
						p.getInventario().setMoedasOuro(carteiraData.get("ouro").intValue());
				}

				if (inventarioData != null) {
					for (Map.Entry<String, ?> entry : inventarioData.entrySet()) {
						String tipo = entry.getKey();

						int quantidade = 0;
						if (entry.getValue() instanceof Number) {
							quantidade = ((Number) entry.getValue()).intValue();
						} else {
							System.err.println("Aviso: Quantidade de item inválida para " + tipo);
							continue;
						}

						Item itemModelo = getItem(tipo);

						if (itemModelo != null) {
							for (int i = 0; i < quantidade; i++) {
								p.getInventario().adicionarItem(itemModelo);
							}
						}
					}
				}

				if (nome.equals("Alexei")) {
					p.setFantasmaNobre(new RingOfTheUndyingWill());
				}
				if (nome.equals("Ayame")) {
					p.setFantasmaNobre(new InvocacaoMurasame());
				}
				if (nome.equals("Trakin")) {
					p.setFantasmaNobre(new AcertoDeContas());
				}
				if (nome.equals("Lyria")) {
					p.setFantasmaNobre(new ApostadorIncansavel());
				}
				if (nome.equals("Eidan")) {
					p.setFantasmaNobre(new VigiliaEterna());
				}
				if (nome.equals("Darrell")) {
					p.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.ModoPolaris());
					p.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.DistortedSolo());
					p.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo());
					p.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.PlainSolo());
				}
				if (nome.equals("Lilith")) {
					p.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.InvocacaoSangrenta());
				}
				if (nome.equals("Arkos")) {
					p.setFantasmaNobre(new IraDeAnthyros());
				}

				if (data.containsKey("racaData") && p.getRaca() instanceof Humano) {
					Map<String, Object> racaData = (Map<String, Object>) data.get("racaData");
					Humano h = (Humano) p.getRaca();

					if (racaData.containsKey("filaContratos")) {
						List<Double> lista = (List<Double>) racaData.get("filaContratos");
						h.setFilaContratos(lista); // Precisa do setter
					}
					if (racaData.containsKey("vidaNegativaAcumulada")) {
						h.setVidaNegativaAcumulada(((Number) racaData.get("vidaNegativaAcumulada")).doubleValue());
					}
				}

				p.recalcularAtributosEstatisticas();

				// --- CARREGAMENTO DE ESTADO (Save Game) ---
				if (data.containsKey("vidaAtual")) {
					double vidaSalva = ((Number) data.get("vidaAtual")).doubleValue();
					p.setVidaAtual(vidaSalva, null, this); // Passa null no estado para não triggar logicas de combate
				} else {
					p.setVidaAtual(p.getVidaMaxima()); // Padrão: Cheio
				}

				if (data.containsKey("manaAtual")) {
					double manaSalva = ((Number) data.get("manaAtual")).doubleValue();
					p.setManaAtual(manaSalva);
				} else {
					p.setManaAtual(p.getManaMaxima()); // Padrão: Cheio
				}

				if (data.containsKey("escudoAtual")) {
					double escudoSalvo = ((Number) data.get("escudoAtual")).doubleValue();
					p.setEscudoAtual(escudoSalvo);
				}

				if (data.containsKey("contadorTU")) {
					p.setContadorTU(((Number) data.get("contadorTU")).intValue());
				}

				if (data.containsKey("posX") && data.containsKey("posY")) {
					p.setPosX(((Number) data.get("posX")).intValue());
					p.setPosY(((Number) data.get("posY")).intValue());
				}

				// Se salvou facção customizada, carrega
				if (data.containsKey("faccao")) {
					p.setFaccao((String) data.get("faccao"));
				}
				System.out.println("Personagem carregado: " + p.getNome() + " com Arma: "
						+ (arma != null ? arma.getNome() : "Nenhuma"));
				return p;
			}
		} catch (Exception e) {
			System.err.println("Erro Crítico ao ler ou processar JSON com Gson para: " + nomeArquivo);
			e.printStackTrace();
			return null;
		}
	}

	public void resolverAcaoFantasmaNobre(AcaoMestreInput input) {
		if (input.getAtor() != estadoCombate.getAtorAtual() || !estadoCombate.isCombateAtivo())
			return;

		Personagem ator = input.getAtor();
		FantasmaNobre fn = ator.getFantasmaNobre(); // Pega o FN do ator
		if (fn == null) {
			System.err.println("Erro: " + ator.getNome() + " tentou usar FN, mas nao tem um FN equipado.");
			return;
		}

		String cooldownEffectNameFn = "CD:" + fn.getNome();
		if (ator.getEfeitosAtivos().containsKey(cooldownEffectNameFn)) {
			System.out.println(">>> " + fn.getNome() + " ainda esta em recarga.");
			return;
		}

		if (ator.getManaAtual() < fn.getCustoMana()) {
			System.out.println(">>> " + ator.getNome() + " nao tem mana suficiente para usar " + fn.getNome() + ".");
			return;
		}

		String motivoBloqueio = fn.getMotivoBloqueio(ator);
		if (motivoBloqueio != null) {
			System.out.println(">>> " + motivoBloqueio);
			return;
		}

		if (fn == null) {
			System.err.println("Erro: " + ator.getNome() + " tentou usar FN, mas não tem um FN equipado.");
			return;
		}

		System.out.println(">>> " + ator.getNome() + " está ativando: " + fn.getNome() + "!");

		// Executa a lógica complexa
		fn.executar(ator, input.getAlvos(), estadoCombate, input, combatManager);

		// Aplica Custos
		int custoManaFinal = fn.getCustoMana(); // (Não estamos aplicando reduções do Elfo a FNs por enquanto)
		int custoTUFinal = fn.getCustoTU();

		ator.setManaAtual(ator.getManaAtual() - custoManaFinal);
		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoManaFinal + " MP e " + custoTUFinal + " TUs.");

		// 3. Aplica Cooldown (se houver)
		int cooldown = fn.getCooldownTU();
		if (cooldown > 0) {
			String cooldownEffectName = "CD:" + fn.getNome();
			Efeito cooldownEfeito = new Efeito(cooldownEffectName, TipoEfeito.DEBUFF, cooldown, null, 0, 0);
			combatManager.aplicarEfeito(ator, cooldownEfeito); // Usa o método do manager
			System.out.println(">>> " + fn.getNome() + " entrou em cooldown por " + cooldown + " TU.");
		}

		// Hook de Ação (Elfo)
		combatManager.setUltimoTipoAcao(ator, TipoAcao.FANTASMA_NOBRE);

		// Fecha a HUD e avança
		fecharHudEAvançar();
	}

	public void resolverAcaoInvocacao(Personagem ator, FantasmaNobre fn, EssenciaInimigo essencia) {
		if (ator != estadoCombate.getAtorAtual() || !estadoCombate.isCombateAtivo())
			return;

		System.out.println(">>> " + ator.getNome() + " está invocando " + essencia.getNome() + "!");
		br.com.dantesrpg.model.util.SessionLogger.log(ator.getNome() + " invocou " + essencia.getNome() + "!");
		// Consome o item
		ator.getInventario().removerItem(essencia);
		// Cria o Servo (nova criatura)
		Personagem servoInvocado = criarServo(ator, essencia);
		// Adiciona o Servo ao combate
		adicionarInvocacaoAoCombate(servoInvocado, ator);
		// Aplica Custos e Cooldowns (lendo do FN)
		int custoManaFinal = fn.getCustoMana();
		int custoTUFinal = fn.getCustoTU();
		ator.setManaAtual(ator.getManaAtual() - custoManaFinal);
		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoManaFinal + " MP e " + custoTUFinal + " TUs.");
		int cooldown = fn.getCooldownTU();
		if (cooldown > 0) {
			String cooldownEffectName = "CD:" + fn.getNome();
			Efeito cooldownEfeito = new Efeito(cooldownEffectName, TipoEfeito.DEBUFF, cooldown, null, 0, 0);
			combatManager.aplicarEfeito(ator, cooldownEfeito);
			System.out.println(">>> " + fn.getNome() + " entrou em cooldown por " + cooldown + " TU.");
		}
		// Hook de Ação (Elfo)
		combatManager.setUltimoTipoAcao(ator, TipoAcao.FANTASMA_NOBRE);
		// Fecha a HUD e avança
		fecharHudEAvançar();
	}

	private Personagem criarServo(Personagem invocador, EssenciaInimigo essencia) {
		// Pega os dados base da essência
		String nomeOriginal = essencia.getNomeInimigoOriginal();
		Map<Atributo, Integer> statsBase = essencia.getAtributosBaseInimigo();
		double vidaBase = essencia.getVidaMaximaBaseInimigo();
		Arma armaOriginal = essencia.getArmaInimigo();

		// Calcula o multiplicador de stats (2.5% por ponto de IN)
		int inteligencia = invocador.getAtributosFinais().getOrDefault(Atributo.INTELIGENCIA, 0);
		double modStats = 0.05 * inteligencia; // scaling servo
		System.out.println(">>> Invocando Servo com " + (modStats * 100) + "% dos stats originais.");

		// Cria os novos stats
		Map<Atributo, Integer> statsServo = new HashMap<>();
		for (Map.Entry<Atributo, Integer> entry : statsBase.entrySet()) {
			int statModificada = (int) (entry.getValue() * modStats);
			statsServo.put(entry.getKey(), Math.max(1, statModificada));
		}
		int vidaServo = Math.max(10, (int) (vidaBase * modStats));

		Personagem servo = new Personagem("Servo: " + nomeOriginal, new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, statsServo, vidaServo, 10);
		// Define arma, facção e recalcula
		servo.setArmaEquipada(armaOriginal);
		servo.setFaccao("JOGADOR");
		servo.recalcularAtributosEstatisticas();
		servo.setVidaAtual(servo.getVidaMaxima());
		servo.setManaAtual(servo.getManaMaxima());
		return servo;
	}

	private void adicionarInvocacaoAoCombate(Personagem invocacao, Personagem invocador) {
		if (invocacao == null)
			return;

		if (mapController != null) {
			javafx.util.Pair<Integer, Integer> posLivre = mapController
					.encontrarCelulaLivreMaisProxima(invocador.getPosX(), invocador.getPosY());

			if (posLivre != null) {
				invocacao.setPosX(posLivre.getKey());
				invocacao.setPosY(posLivre.getValue());
			} else {
				System.out.println(
						">>> AVISO: Invocação sem espaço. Colocando no lugar do invocador (Sobreposição de emergência).");
				invocacao.setPosX(invocador.getPosX());
				invocacao.setPosY(invocador.getPosY());
			}
		}

		invocacao.setContadorTU(invocador.getContadorTU() + 1);
		this.estadoCombate.getCombatentes().add(invocacao);
		popularListasDeCombatentes();
		atualizarTimelineTU();
		if (mapController != null) {
			mapController.desenharPeoes(this.estadoCombate.getCombatentes());
		}
		System.out.println(">>> " + invocacao.getNome() + " foi adicionado ao combate!");
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
		if ("Ladino".equalsIgnoreCase(nomeClasse))
			return new Ladino();
		if ("Paladino".equalsIgnoreCase(nomeClasse))
			return new Paladino();
		if ("Ilusionista".equalsIgnoreCase(nomeClasse))
			return new Ilusionista();
		if ("Invocador".equalsIgnoreCase(nomeClasse))
			return new Invocador();
		System.err.println("Classe não reconhecida: " + nomeClasse);
		return new ClassePlaceholder();
	}

	public void carregarEstadoJogadores() {
		System.out.println("SISTEMA: Recarregando estado dos jogadores...");

		List<Personagem> combatentesAtuais = estadoCombate.getCombatentes();

		for (int i = 0; i < combatentesAtuais.size(); i++) {
			Personagem p = combatentesAtuais.get(i);

			// Só recarrega jogadores que têm arquivo JSON associado
			if (isPlayer(p) && p.getJsonFileName() != null) {

				// Remove a extensão para o método carregar (se o seu método pedir sem extensão)
				String nomeArquivo = p.getJsonFileName().replace(".json", "");

				// Carrega do DISCO (lê o arquivo original, ignorando memória atual)
				Personagem originalDoDisco = carregarPersonagemComGson(nomeArquivo);

				if (originalDoDisco != null) {
					// Restaura dados de sessão que não salvam (Posição, TU, Ausência)
					originalDoDisco.setPosX(p.getPosX());
					originalDoDisco.setPosY(p.getPosY());
					originalDoDisco.setContadorTU(p.getContadorTU());
					originalDoDisco.setAusente(p.isAusente());
					originalDoDisco.setProtagonista(p.isProtagonista());
					originalDoDisco.setMovimentoRestanteTurno(p.getMovimentoRestanteTurno());

					// Substitui o objeto na lista principal
					combatentesAtuais.set(i, originalDoDisco);
					System.out.println("Recarregado: " + p.getNome());
				}
			}
		}

		// Força a atualização visual total
		atualizarInterfaceTotal();
	}

	private Arma mapearArma(Map<String, Object> armaData) {
		try {
			String nome = (String) armaData.getOrDefault("nome", "Arma Desconhecida");
			String categoria = (String) armaData.getOrDefault("categoria", "Desconhecida");
			String descricao = (String) armaData.getOrDefault("descricao", "Uma arma.");
			String tipoMoeda = (String) armaData.getOrDefault("tipoMoeda", "BRONZE");
			Raridade raridade = Raridade.valueOf(((String) armaData.getOrDefault("raridade", "COMUM")).toUpperCase());
			int danoBase = ((Double) armaData.getOrDefault("danoBase", 1.0)).intValue();
			int valorMoedas = ((Double) armaData.getOrDefault("valorMoedas", 0.0)).intValue();
			int ticks = ((Double) armaData.getOrDefault("ticksDeDano", 1.0)).intValue();
			Atributo atributo = Atributo
					.valueOf(((String) armaData.getOrDefault("atributoMultiplicador", "FORCA")).toUpperCase());
			int custoTU = ((Double) armaData.getOrDefault("custoTU", 100.0)).intValue();

			String efeitoHit = (String) armaData.getOrDefault("efeitoAoAcertar", null);
			double chanceHit = 0.0;
			Object chanceObj = armaData.get("chanceEfeito");
			if (chanceObj instanceof Number) {
				chanceHit = ((Number) chanceObj).doubleValue();
			}

			int alcanceJson = -1;
			if (armaData.containsKey("alcance")) {
				alcanceJson = ((Double) armaData.get("alcance")).intValue();
			}

			String tipo = (String) armaData.getOrDefault("tipo", "Melee");

			// --- LEITURA DE MODIFICADORES DE ATRIBUTO ---
			Map<String, Double> modsJson = (Map<String, Double>) armaData.get("modificadoresDeAtributo");
			Map<Atributo, Integer> modsFinais = null;
			if (modsJson != null) {
				modsFinais = new HashMap<>();
				for (Map.Entry<String, Double> entry : modsJson.entrySet()) {
					try {
						modsFinais.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			Map<String, Double> modsStatusJson = (Map<String, Double>) armaData.get("modificadoresStatus");
			Map<String, Double> modsStatusFinais = parseModificadoresStatus(modsStatusJson);

			Arma armaFinal = null;

			if ("Melee".equalsIgnoreCase(tipo)) {
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 1;
				armaFinal = new br.com.dantesrpg.model.ArmaMelee(nome, categoria, descricao, raridade, valorMoedas,
						danoBase, ticks, atributo, custoTU, alcanceFinal);

			} else if ("Ranged".equalsIgnoreCase(tipo)) {
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 5;
				int municao = ((Double) armaData.getOrDefault("municaoMaxima", 6.0)).intValue();
				armaFinal = new br.com.dantesrpg.model.ArmaRanged(nome, categoria, descricao, raridade, valorMoedas,
						danoBase, ticks, atributo, custoTU, alcanceFinal, municao);

			} else if ("Magico".equalsIgnoreCase(tipo)) {
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 3;
				armaFinal = new br.com.dantesrpg.model.ImplementoMagico(nome, categoria, descricao, raridade,
						valorMoedas, danoBase, atributo, custoTU, alcanceFinal);
			} else if ("Grimorio".equalsIgnoreCase(tipo)) {
				int maxSlots = ((Double) armaData.getOrDefault("maxSlots", 3.0)).intValue();
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 4;

				Grimorio grimorio = new Grimorio(nome, categoria, descricao, raridade, valorMoedas, danoBase, atributo,
						custoTU, alcanceFinal, maxSlots);
				List<String> nomesMagias = (List<String>) armaData.get("magias");
				if (nomesMagias != null) {
					for (String nomeMagia : nomesMagias) {
						Habilidade h = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeMagia);
						if (h != null)
							grimorio.aprenderMagia(h);
					}
				}
				armaFinal = grimorio;
			}

			if (armaFinal != null) {
				armaFinal.setNomeEfeitoOnHit(efeitoHit);
				armaFinal.setChanceEfeitoOnHit(chanceHit);
				
				// Suporte a itens antigos (somente string)
				String habilidadeUnica = (String) armaData.getOrDefault("habilidadeConcedida", null);
				if (habilidadeUnica != null) {
					armaFinal.addHabilidadeConcedida(habilidadeUnica);
				}

				// Suporte a Nova Lista (Array de Strings no JSON)
				List<String> listaHabilidades = (List<String>) armaData.get("habilidadesConcedidas");
				if (listaHabilidades != null) {
					for (String hab : listaHabilidades) {
						armaFinal.addHabilidadeConcedida(hab);
					}
				}

				try {
					String tipoAlvoStr = (String) armaData.getOrDefault("tipoAlvo", "INDIVIDUAL");
					armaFinal.setTipoAlvo(TipoAlvo.valueOf(tipoAlvoStr.toUpperCase()));
				} catch (Exception e) {
				}

				int tamanhoArea = ((Double) armaData.getOrDefault("tamanhoArea", 0.0)).intValue();
				armaFinal.setTamanhoArea(tamanhoArea);

				armaFinal.setTipoMoeda(tipoMoeda);
				armaFinal.setModificadoresDeAtributo(modsFinais);
				armaFinal.setModificadoresStatus(modsStatusFinais);
				return armaFinal;
			}

			System.err.println("Tipo de arma não reconhecido: " + tipo);
			return null;
		} catch (Exception e) {
			System.err.println("Erro ao mapear dados da arma do JSON:");
			e.printStackTrace();
			return null;
		}
	}

	// --- Métodos de Criação de Teste ---
	private List<Personagem> criarJogadoresDeTeste() {
		List<Personagem> players = new ArrayList<>();
		Personagem alexei = carregarPersonagemComGson("alexei");
		Personagem lyria = carregarPersonagemComGson("lyria");
		Personagem lilith = carregarPersonagemComGson("lilith");
		Personagem eidan = carregarPersonagemComGson("eidan");
		Personagem darrell = carregarPersonagemComGson("darrell");
		Personagem Arkos = carregarPersonagemComGson("Arkos");

		// personagens extras
		Personagem Aristóteles = carregarPersonagemComGson("Aristóteles");
		Personagem Atlas = carregarPersonagemComGson("Atlas");
		Personagem Platão = carregarPersonagemComGson("Platão");
		Personagem Julius = carregarPersonagemComGson("Julius");
		Personagem Virgilio = carregarPersonagemComGson("Virgilio");
		Personagem vivian = carregarPersonagemComGson("Vivian");

		// personagens inutilizados
		Personagem trakin = carregarPersonagemComGson("trakin");
		Personagem ayame = carregarPersonagemComGson("ayame");

		if (alexei != null) {
			alexei.setPosX(4);
			alexei.setPosY(4);
			players.add(alexei);
		}

		if (lilith != null) {
			lilith.setPosX(4);
			lilith.setPosY(5);
			players.add(lilith);
		}

		if (eidan != null) {
			eidan.setPosX(5);
			eidan.setPosY(4);
			players.add(eidan);
		}
		
		if (darrell != null) {
			darrell.setPosX(5);
			darrell.setPosY(5);
			players.add(darrell);
		} 
		
		
		
		/*
		 
		if (Arkos != null) {
			Arkos.setPosX(5);
			Arkos.setPosY(10);
			players.add(Arkos);
		}
		
		if (vivian != null) {
			vivian.setPosX(4);
			vivian.setPosY(6);
			players.add(vivian);
		} 
		
		*/

		
		if (Virgilio != null) {
			Virgilio.setPosX(5);
			Virgilio.setPosY(6);
			players.add(Virgilio);
		}


		/*
		 * Personagens inutilizados if (Julius != null) { Julius.setPosX(4);
		 * Julius.setPosY(7); players.add(Julius); }
		 * 
		 * if (Platão != null) { Platão.setPosX(5); Platão.setPosY(7);
		 * players.add(Platão); }
		 * 
		 * if (Atlas != null) { Atlas.setPosX(6); Atlas.setPosY(7); players.add(Atlas);
		 * }
		 * 
		 * 
		 * // Primeira Leva
		 * 
		 * // Segunda leva Leva
		 * 
		 * if (Aristóteles != null) { Aristóteles.setPosX(6); Aristóteles.setPosY(5); players.add(Aristóteles); }
		 * 
		 * /* Personagens inutilizados
		 * 
		 * if (ayame != null) { ayame.setPosX(5); ayame.setPosY(6); // Define Posição
		 * players.add(ayame); }
		 * 
		 * if (trakin != null) { trakin.setPosX(4); trakin.setPosY(6); // Define Posição
		 * players.add(trakin); }
		 * 
		 * if (lyria != null) { lyria.setPosX(4); lyria.setPosY(5); players.add(lyria);
		 * }
		 * 
		 * 
		 */

		return players;
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
		if (andarAtual.startsWith("8"))
			return 4;
		if (andarAtual.startsWith("9º"))
			return 5;

		return 0;
	}

	public boolean solicitarTesteDeAtributo(Personagem p, Atributo atr, int dificuldadeNA) {
		// Cria um diálogo simples pedindo o valor
		javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
		dialog.setTitle("Teste de Resistência: " + p.getNome());
		dialog.setHeaderText("Teste de " + atr.name() + " (NA " + dificuldadeNA + ")");
		dialog.setContentText("O Jogador deve rodar 1d20 + " + atr.name() + ".\nInsira o resultado final:");

		// Estilização Dark (Opcional)
		dialog.getDialogPane().setStyle("-fx-background-color: #222;");
		dialog.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");

		// Bloqueia o jogo até responder <- pensando em remover
		Optional<String> result = dialog.showAndWait();

		if (result.isPresent()) {
			try {
				int valor = Integer.parseInt(result.get());
				boolean sucesso = valor >= dificuldadeNA;

				String msg = sucesso ? "SUCESSO" : "FALHA";
				System.out.println(">>> TESTE (" + valor + " vs NA " + dificuldadeNA + "): " + msg);
				br.com.dantesrpg.model.util.SessionLogger.log(p.getNome() + " rolou " + valor + " no teste de "
						+ atr.name() + " (NA " + dificuldadeNA + "): " + msg);

				return sucesso;
			} catch (NumberFormatException e) {
				System.out.println(">>> Entrada inválida. Considerando Falha.");
				return false;
			}
		}
		return false; // Cancelou = Falha
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
			this.clonesSquadAtuais = clonesDoTurno;

			// Abre a HUD apenas para o PRIMEIRO
			iniciarTurnoParaAtor(atorAtual);

		} else {
			// Turno Normal
			this.clonesSquadAtuais = null;
			iniciarTurnoParaAtor(atorAtual);
		}
	}

	private void abrirResolucaoEmprestimo(Personagem p, Humano humano) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/AttributeTestPrompt.fxml"));
			Parent root = loader.load();
			AttributeTestPromptController controller = loader.getController();

			Stage stage = new Stage();
			stage.setTitle("Fim do Empréstimo");
			stage.setScene(new Scene(root));
			stage.initModality(Modality.APPLICATION_MODAL); // Bloqueia o jogo
			// Remove o botão fechar para obrigar a escolha
			stage.setOnCloseRequest(e -> e.consume());

			int na = humano.calcularDificuldadeTeste(p);
			// Aplica bônus do Andar
			na += getBonusDificuldadeAndar();
			String difTexto = humano.getTextoDificuldade(na - getBonusDificuldadeAndar()); // Texto base

			controller.setDados(stage, p.getNome(), na, difTexto);

			stage.showAndWait(); // Espera o usuário confirmar

			if (controller.isConfirmado()) {
				int rolagem = controller.getResultadoRolagem();
				int atributoEndurance = p.getAtributosFinais().getOrDefault(Atributo.ENDURANCE, 0);
				int total = rolagem + atributoEndurance;
				boolean sucesso = (total >= na);

				humano.resolverResultadoTeste(p, sucesso, total);

				// Feedback visual e atualizações
				br.com.dantesrpg.model.util.SessionLogger
						.log(p.getNome() + " Rolagem: " + rolagem + " + END(" + atributoEndurance + ") = " + total
								+ " vs NA " + na + " -> " + (sucesso ? "SUCESSO" : "FALHA"));
				atualizarInterfaceTotal();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Variável para guardar o estado
	private List<Personagem> clonesSquadAtuais;

	private void limparEstadoSquadTemporario() {
		habilidadeSquadTemp = null;
		ataquesSquadTemp = null;
		rolagemSquadTemp = 0;
		modoAtaqueSquadTemp = ModoAtaque.NORMAL;
		tirosExtrasSquadTemp = 0;
		clonesSquadAtuais = null;
	}

	public void iniciarAtaqueSquad(Habilidade habilidadeAcao, Habilidade habilidadeSelecao, int rolagemGlobal,
			ModoAtaque modoAtaque, int tirosExtras) {
		this.habilidadeSquadTemp = habilidadeAcao;
		this.rolagemSquadTemp = rolagemGlobal;
		this.modoAtaqueSquadTemp = modoAtaque != null ? modoAtaque : ModoAtaque.NORMAL;
		this.tirosExtrasSquadTemp = Math.max(0, tirosExtras);
		if (detailedTurnHudStage != null)
			detailedTurnHudStage.hide();
		if (mapController != null && clonesSquadAtuais != null) {
			mapController.iniciarSelecaoSquad(clonesSquadAtuais, habilidadeSelecao, rolagemGlobal);
		}
	}

	public void executarAcaoClonesSemAlvo(Habilidade habilidade, int rolagemGlobal) {
		if (this.clonesSquadAtuais == null)
			this.clonesSquadAtuais = new ArrayList<>();

		if (detailedTurnHudStage != null)
			detailedTurnHudStage.hide();

		combatManager.executarAtaqueCoordenado(new HashMap<>(), habilidade, rolagemGlobal, ModoAtaque.NORMAL, 0,
				estadoCombate, this.clonesSquadAtuais);

		limparEstadoSquadTemporario();
		avancarTurnoAposAcao();
	}

	public void retornarDoSquadComAlvos(Map<Personagem, Personagem> ataquesDefinidos) {
		this.ataquesSquadTemp = (ataquesDefinidos != null) ? new HashMap<>(ataquesDefinidos) : new HashMap<>();

		if (this.ataquesSquadTemp.isEmpty()) {
			System.out.println("SQUAD: Nenhum alvo foi travado. Encerrando o turno dos clones.");
			passarTurnoSquadAtual();
			return;
		}

		System.out.println("SQUAD: Todos os alvos selecionados. Retornando à HUD.");

		if (detailedTurnHudController != null) {
			detailedTurnHudController.adicionarAlvos(new ArrayList<>(this.ataquesSquadTemp.values()));
			detailedTurnHudController.configurarConfirmacaoSquad(this.ataquesSquadTemp.size());
			detailedTurnHudStage.show();
			detailedTurnHudStage.toFront(); // Garante que venha para frente
		}
	}

	public void executarAtaqueSquadFinal() {
		// Garante que o mapa não é nulo
		if (ataquesSquadTemp == null)
			ataquesSquadTemp = new HashMap<>();

		// Garante que a lista de clones não é nula
		if (this.clonesSquadAtuais == null)
			this.clonesSquadAtuais = new ArrayList<>();

		// Chama o manager passando a lista COMPLETA de clones
		combatManager.executarAtaqueCoordenado(ataquesSquadTemp, habilidadeSquadTemp, rolagemSquadTemp,
				modoAtaqueSquadTemp, tirosExtrasSquadTemp, estadoCombate, this.clonesSquadAtuais);

		// Limpa temp
		limparEstadoSquadTemporario();

		// Encerra o turno e atualiza a timeline
		avancarTurnoAposAcao();
	}

	public void resolverAtaqueCoordenado(Map<Personagem, Personagem> ataques, Habilidade habilidade,
			int rolagemGlobal) {
		List<Personagem> envolvidos = new ArrayList<>(ataques.keySet());

		combatManager.executarAtaqueCoordenado(ataques, habilidade, rolagemGlobal, ModoAtaque.NORMAL, 0,
				this.estadoCombate, envolvidos);

		avancarTurnoAposAcao();
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
				detailedTurnHudStage.initModality(Modality.WINDOW_MODAL);
				Window ownerWindow = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
				if (ownerWindow != null)
					detailedTurnHudStage.initOwner(ownerWindow);
				detailedTurnHudStage.setResizable(false);
				detailedTurnHudStage.setMinWidth(820);
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

	private Armadura mapearArmadura(Map<String, Object> data) {
		if (data == null)
			return null;
		try {
			String nome = (String) data.getOrDefault("nome", "Armadura Desconhecida");
			String descricao = (String) data.getOrDefault("descricao", "Uma armadura resistente.");
			String tipoMoeda = (String) data.getOrDefault("tipoMoeda", "BRONZE");
			int valor = data.containsKey("valorMoedas") ? ((Double) data.get("valorMoedas")).intValue() : 0;
			int armaduraBase = ((Double) data.getOrDefault("armaduraBase", 0.0)).intValue();

			// Lê modificadores de atributo (FOR, DES...)
			Map<Atributo, Integer> modificadores = new HashMap<>();
			Map<String, Double> modsJson = (Map<String, Double>) data.get("modificadoresDeAtributo");
			if (modsJson != null) {
				for (Map.Entry<String, Double> entry : modsJson.entrySet()) {
					modificadores.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
				}
			}
			Map<String, Double> modStatus = parseModificadoresStatus(
					(Map<String, Double>) data.get("modificadoresStatus"));
			Armadura a = new br.com.dantesrpg.model.Armadura(nome, descricao, valor, armaduraBase, modificadores,
					modStatus);
			a.setTipoMoeda(tipoMoeda);
			return a;

		} catch (Exception e) {
			System.err.println("Erro ao mapear dados da Armadura do JSON:");
			e.printStackTrace();
			return null;
		}
	}

	private Amuleto mapearAmuleto(Map<String, Object> data) {
		if (data == null)
			return null;
		try {
			String nome = (String) data.getOrDefault("nome", "Amuleto Desconhecido");
			String descricao = (String) data.getOrDefault("descricao", "Sem descrição.");
			String tipoMoeda = (String) data.getOrDefault("tipoMoeda", "BRONZE");
			int valor = data.containsKey("valorMoedas") ? ((Double) data.get("valorMoedas")).intValue() : 0;
			int armaduraBonus = ((Double) data.getOrDefault("armaduraBonus", 0.0)).intValue();

			Map<Atributo, Integer> modificadores = new HashMap<>();
			Map<String, Double> modsJson = (Map<String, Double>) data.get("modificadoresDeAtributo");
			if (modsJson != null) {
				for (Map.Entry<String, Double> entry : modsJson.entrySet()) {
					modificadores.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
				}
			}

			Map<String, Double> modStatus = parseModificadoresStatus(
					(Map<String, Double>) data.get("modificadoresStatus"));
			Amuleto a = new br.com.dantesrpg.model.Amuleto(nome, descricao, valor, armaduraBonus, modificadores,
					modStatus);
			a.setTipoMoeda(tipoMoeda);
			return a;

		} catch (Exception e) {
			System.err.println("Erro ao mapear dados do Amuleto do JSON:");
			e.printStackTrace();
			return null;
		}
	}

	public void resolverAcaoDoMestre(AcaoMestreInput input) {
		if (input.getAtor() != estadoCombate.getAtorAtual() || !estadoCombate.isCombateAtivo())
			return;

		combatManager.resolverAcao(input, estadoCombate);

		fecharHudEAvançar();
	}

	public void resolverAcaoPassarVez(AcaoMestreInput input) {
		if (input.getAtor() != estadoCombate.getAtorAtual() || !estadoCombate.isCombateAtivo())
			return;

		Personagem ator = input.getAtor();

		if (devePassarSquadDeClones(ator)) {
			System.out.println("SQUAD: Encerrando o turno de todos os clones restantes.");
			passarTurnoSquadAtual();
			return;
		}

		aplicarPassarVez(ator);
		fecharHudEAvançar();
	}

	private boolean devePassarSquadDeClones(Personagem ator) {
		if (ator == null || !ator.isClone() || clonesSquadAtuais == null || clonesSquadAtuais.isEmpty())
			return false;

		Personagem criador = ator.getCriador();
		return criador != null && clonesSquadAtuais.stream()
				.anyMatch(clone -> clone != null && clone.isAtivoNoCombate() && clone.isClone()
						&& clone.getCriador() == criador);
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
		if (clonesSquadAtuais == null || clonesSquadAtuais.isEmpty()) {
			aplicarPassarVez(estadoCombate != null ? estadoCombate.getAtorAtual() : null);
			limparEstadoSquadTemporario();
			fecharHudEAvançar();
			return;
		}

		Personagem criador = null;
		Personagem atorAtual = estadoCombate != null ? estadoCombate.getAtorAtual() : null;
		if (atorAtual != null && atorAtual.isClone()) {
			criador = atorAtual.getCriador();
		}

		if (criador == null) {
			for (Personagem clone : clonesSquadAtuais) {
				if (clone != null && clone.isAtivoNoCombate() && clone.isClone()) {
					criador = clone.getCriador();
					break;
				}
			}
		}

		for (Personagem clone : new ArrayList<>(clonesSquadAtuais)) {
			if (clone == null || !clone.isAtivoNoCombate() || !clone.isClone())
				continue;
			if (criador != null && clone.getCriador() != criador)
				continue;

			aplicarPassarVez(clone);
		}

		limparEstadoSquadTemporario();
		fecharHudEAvançar();
	}

	public void verificarInteracaoTerreno(Personagem ator) {
		if (combatManager != null && mapController != null) {
			combatManager.resolverTerrenoPerigoso(ator, mapController, estadoCombate);
		}
	}

	private void fecharHudEAvançar() {
		if (detailedTurnHudStage != null && detailedTurnHudStage.isShowing())
			detailedTurnHudStage.hide();
		avancarParaProximoTurno();
	}

	public void iniciarSelecaoDeAlvo(Habilidade habilidade, Personagem ator) {
		// Esconde a HUD primeiro
		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		// Configura e foca o mapa
		if (mapController != null && mapStage != null) {
			// Garante que o stage não está minimizado
			if (mapStage.isIconified())
				mapStage.setIconified(false);

			mapStage.show();
			mapController.entrarModoSelecao(habilidade, ator);

			javafx.application.Platform.runLater(() -> {
				mapStage.toFront();
				mapStage.requestFocus();
			});
		} else {
			System.err.println("Erro: MapController ou MapStage não iniciado.");
		}
	}

	private void avancarParaProximoTurno() {
		// Atualiza a interface visual
		popularListasDeCombatentes();
		removerDestaques();

		Personagem atorQueAgiu = estadoCombate.getAtorAtual();

		// Lógica de Reação/Turno Extra (Ex: Sussurro Sombrio)
		if (atorQueAgiu != null && atorQueAgiu.getEfeitosAtivos().containsKey("Sussurro Sombrio")) {
			// Rola a chance de 25%
			if (Math.random() < 0.25) {
				System.out.println(">>> SUSSURRO SOMBRIO ATIVADO! " + atorQueAgiu.getNome() + " age novamente!");

				int menorTuNaBatalha = estadoCombate.getCombatentes().stream().filter(Personagem::isAtivoNoCombate)
						.mapToInt(Personagem::getContadorTU).min().orElse(0);

				atorQueAgiu.setContadorTU(menorTuNaBatalha - 1);
			} else {
				System.out.println(">>> Sussurro Sombrio (25%) falhou.");
			}
		}

		// Verifica quem é o próximo (Apenas para Log visual)
		Personagem proximoCalculado = getProximoAtorCalculado();

		// Verifica Fim de Combate e Atualiza Timeline
		if (!verificarFimDeCombate()) {
			if (estadoCombate.isCombateAtivo()) {
				System.out.println("\n...Ação Concluída...\nPróximo: "
						+ (proximoCalculado != null ? proximoCalculado.getNome() : "Ninguém")
						+ ". Aguardando 'Iniciar Turno'.");
				atualizarTimelineTU();
			}
		}
	}

	public Optional<Personagem> encontrarAlvoValido(boolean atacanteIsPlayer) {
		if (estadoCombate == null || estadoCombate.getCombatentes() == null)
			return Optional.empty();
		return estadoCombate.getCombatentes().stream()
				.filter(p -> p.isAtivoNoCombate() && (atacanteIsPlayer != isPlayer(p))).findFirst();
	}

	private void atualizarTimelineTU() {
		if (timelineContainer == null || estadoCombate == null || estadoCombate.getCombatentes() == null)
			return;
		timelineContainer.getChildren().clear();

		List<Personagem> ordenadosPorTU = new ArrayList<>(estadoCombate.getCombatentes());
		ordenadosPorTU.sort(Comparator.comparingInt(Personagem::getContadorTU));

		// Conjunto para rastrear quais mestres já tiveram um clone exibido
		Set<Personagem> mestresComCloneExibido = new HashSet<>();

		for (Personagem p : ordenadosPorTU) {
			if (p.isAtivoNoCombate()) {
				if (p.isClone()) {
					Personagem criador = p.getCriador();
					if (mestresComCloneExibido.contains(criador)) {
						continue;
					}
					mestresComCloneExibido.add(criador);

					Label marcador = new Label("Clones (" + criador.getNome() + ") [" + p.getContadorTU() + "]");
					marcador.setStyle("-fx-text-fill: violet; -fx-font-weight: bold;");
					timelineContainer.getChildren().add(marcador);
					continue;
				}
				
				Label marcador = new Label(p.getNome() + " [" + p.getContadorTU() + "]");
				marcador.setStyle(
						isPlayer(p) ? "-fx-text-fill: cyan; -fx-font-weight: bold;" : "-fx-text-fill: lightcoral;");
				timelineContainer.getChildren().add(marcador);
			}
		}
	}

	private Personagem getProximoAtorCalculado() {
		if (estadoCombate == null || estadoCombate.getCombatentes() == null || estadoCombate.getCombatentes().isEmpty())
			return null;
		return estadoCombate.getCombatentes().stream().filter(p -> p.isAtivoNoCombate())
				.min(Comparator.comparingInt(Personagem::getContadorTU)).orElse(null);
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
		System.out.println(">>> Viajando para nova área...");
		// Remove inimigos mortos/vivos antigos
		encerrarContratosBarbaros();
		encerrarPosturasAnao();
		limparClonesDoCombate();
		estadoCombate.getCombatentes().removeIf(p -> !isPlayer(p));

		// Reseta TUs baseados na iniciativa
		estadoCombate.resetarIniciativa();

		popularListasDeCombatentes();
		atualizarTimelineTU();

		if (mapController != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes());
		}
	}

	private void limparClonesDoCombate() {
		if (estadoCombate == null)
			return;

		for (Personagem p : estadoCombate.getCombatentes()) {
			if (p != null && !p.isClone()) {
				p.limparClonesAtivos();
			}
		}

		estadoCombate.getCombatentes().removeIf(Personagem::isClone);
		limparEstadoSquadTemporario();

		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		if (mapController != null) {
			mapController.sairModoSelecao();
		}
	}

	private void encerrarContratosBarbaros() {
		if (estadoCombate == null) {
			return;
		}

		for (Personagem personagem : estadoCombate.getCombatentes()) {
			BarbaroUtils.encerrarContrato(personagem);
		}
	}

	private void encerrarPosturasAnao() {
		if (estadoCombate == null) {
			return;
		}

		for (Personagem personagem : estadoCombate.getCombatentes()) {
			if (personagem.getRaca() instanceof Anao) {
				((Anao) personagem.getRaca()).encerrarPostura(personagem);
			}
		}
	}

	private void carregarMetadadosDoMapa(File imagemFile) {
		// Troca a extensão .png/.jpg por .json
		String pathJson = imagemFile.getAbsolutePath().substring(0, imagemFile.getAbsolutePath().lastIndexOf('.'))
				+ ".json";
		File jsonFile = new File(pathJson);

		if (jsonFile.exists()) {
			System.out.println("MAPA: Metadados encontrados: " + jsonFile.getName());
			try (FileReader reader = new FileReader(jsonFile)) {
				Gson gson = new Gson();
				MapMetadata meta = gson.fromJson(reader, MapMetadata.class);
				mapController.aplicarMetadados(meta);
			} catch (Exception e) {
				System.err.println("Erro ao ler JSON do mapa: " + e.getMessage());
			}
		} else {
			System.out.println("MAPA: Nenhum JSON de metadados encontrado. Usando imagem pura.");
		}
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
		if (mapController == null || arquivoMapaAtual == null) {
			System.err.println("Erro: Nenhum mapa carregado para salvar.");
			return;
		}

		// Extrai dados
		br.com.dantesrpg.model.map.MapMetadata meta = mapController.extrairMetadados();

		// Define caminho
		String pathJson = arquivoMapaAtual.getAbsolutePath().substring(0,
				arquivoMapaAtual.getAbsolutePath().lastIndexOf('.')) + ".json";

		// Salva
		try (Writer writer = new FileWriter(pathJson)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(meta, writer);
			System.out.println("MAPA SALVO COM SUCESSO: " + pathJson);

			// Feedback
			javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
					javafx.scene.control.Alert.AlertType.INFORMATION);
			alert.setTitle("Editor de Mapa");
			alert.setHeaderText(null);
			alert.setContentText("Metadados do mapa salvos em:\n" + pathJson);
			alert.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void removerDestaques() {
		System.out.println("Removendo destaques...");
		aplicarEstiloDestaque(null);
	}

	private void destacarCardAtor(Personagem atorAtual) {
		aplicarEstiloDestaque(atorAtual);
	}

	private void aplicarEstiloDestaque(Personagem atorParaDestacar) {
		List<Node> allCards = new ArrayList<>();
		if (playerListContainer != null)
			allCards.addAll(playerListContainer.getChildren());
		if (enemyListContainer != null)
			allCards.addAll(enemyListContainer.getChildren());
		for (Node node : allCards) {
			if (node != null && node.getUserData() instanceof PlayerCardController) {
				PlayerCardController controller = (PlayerCardController) node.getUserData();
				if (controller != null && controller.getPersonagem() != null) {
					boolean destacar = (controller.getPersonagem() == atorParaDestacar);
					controller.setHighlight(destacar);
				}
			}
		}
	}

	public boolean isPlayer(Personagem p) {
		if (p == null || p.getFaccao() == null)
			return false;
		return p.getFaccao().equals("JOGADOR");
	}

	public void resolverAcaoItem(AcaoMestreInput input) {
		if (input.getAtor() != estadoCombate.getAtorAtual() || !estadoCombate.isCombateAtivo())
			return;

		Item item = input.getItemSendoUsado();
		if (item == null) {
			System.err.println("Erro: Ação de item sem item definido.");
			return;
		}

		// Passa para o motor de regras
		combatManager.resolverAcaoItem(input, estadoCombate);

		fecharHudEAvançar(); // Fecha a pop-up e atualiza a UI
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
		if (btnConfirmarMovimento != null && estadoCombate.getAtorAtual() != null) {
			Personagem ator = estadoCombate.getAtorAtual();
			btnConfirmarMovimento.setText("CONCLUIR MOVIMENTO (" + ator.getMovimentoRestanteTurno() + " restos)");
		}
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
		System.out.println(">>> REAÇÃO: Fantasma do Deserto ativado!");
		this.reacao_Ator = ator;
		this.reacao_Alvo = alvo;
		this.reacao_NivelCascata = 1; // Inicia a cascata

		// "Pausa" a HUD de turno principal
		if (detailedTurnHudStage != null && detailedTurnHudStage.isShowing()) {
			detailedTurnHudStage.getScene().getRoot().setDisable(true);
		}

		abrirJanelaDeRolagem("Fantasma do Deserto! (Nível " + reacao_NivelCascata + ")", "1d4 (Falha em 2 ou menos)");
	}

	public void resolverRolagemReacao(String rolagemStr) {
		int rolagem;
		try {
			rolagem = Integer.parseInt(rolagemStr);
		} catch (Exception e) {
			System.err.println("Rolagem inválida! Insira um número.");
			return; // Não fecha o pop-up, espera um número
		}

		// Chama o CombatManager para aplicar o dano E ver se continua
		boolean continuarCascata = combatManager.aplicarDanoFantasmaDoDeserto(this.reacao_Ator, this.reacao_Alvo,
				rolagem, this.reacao_NivelCascata, this.estadoCombate);

		if (continuarCascata) {
			// Sucesso! Pede outro dado.
			this.reacao_NivelCascata++;
			diceRollController.preparar("Moeda Extra! (Nível " + reacao_NivelCascata + ")",
					"1d4 (Falha em 2 ou menos)");
		} else {
			System.out.println(">>> REAÇÃO: Fantasma do Deserto terminou.");
			diceRollStage.hide();

			if (detailedTurnHudStage != null && detailedTurnHudStage.isShowing()) {
				detailedTurnHudStage.getScene().getRoot().setDisable(false);
				detailedTurnHudStage.requestFocus();
			}

			popularListasDeCombatentes();
		}
	}

	private void abrirJanelaDeRolagem(String textoPrompt, String promptDado) {
		try {
			if (diceRollStage == null) {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/br/com/dantesrpg/view/DiceRollPrompt.fxml"));
				Parent root = loader.load();
				diceRollController = loader.getController();
				diceRollController.setMainController(this); // Dá ao pop-up a referência

				diceRollStage = new Stage();
				diceRollStage.initModality(Modality.APPLICATION_MODAL); // Trava o jogo
				diceRollStage.initOwner(detailedTurnHudStage); // Trava na HUD de turno
				diceRollStage.setScene(new Scene(root));
				diceRollStage.setTitle("Rolagem de Reação Requerida!");
				// Impede o Mestre de fechar no "X" e quebrar o loop
				diceRollStage.setOnCloseRequest(e -> e.consume());
			}
			diceRollController.preparar(textoPrompt, promptDado);
			diceRollStage.show();
			diceRollController.requestFocus(); // Foca o TextField
		} catch (Exception e) {
			System.err.println("Erro crítico ao carregar DiceRollPrompt.fxml:");
			e.printStackTrace();
		}
	}

	public CombatManager getCombatManager() {
		return this.combatManager;
	}

	public void resolverAcaoFugir(Personagem ator) {
		if (ator != estadoCombate.getAtorAtual() || !estadoCombate.isCombateAtivo()) {
			return; // Garante que só o ator atual pode fugir
		}

		System.out.println(">>> " + ator.getNome() + " conseguiu fugir do combate!");

		// Marca o personagem como "fugiu"
		ator.setFugiu(true);

		// Remove o peão do mapa
		if (mapController != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes());
		}

		// Fecha a HUD de turno e avança
		fecharHudEAvançar();
	}

	public void limparRingueDoMapa() {
		if (mapController != null) {
			mapController.limparRingueAlexei();
		}
	}

	public void desenharRingueDoMapa(Personagem centro, int tamanho) {
		if (mapController != null) {
			mapController.desenharRingueAlexei(centro, tamanho);
		}
	}

	public void desenharDominioLyriaNoMapa(Personagem centro) {
		if (mapController != null) {
			mapController.desenharDominioLyria(centro, 5); // Tamanho 5 (raio 2)
		}
	}

	public void limparDominioLyriaDoMapa() {
		if (mapController != null) {
			mapController.limparDominioLyria();
		}
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

		if (mapController != null) {
			javafx.util.Pair<Integer, Integer> posLivre = mapController
					.encontrarCelulaLivreMaisProxima(invocador.getPosX(), invocador.getPosY());

			if (posLivre != null) {
				clone.setPosX(posLivre.getKey());
				clone.setPosY(posLivre.getValue());

				this.estadoCombate.getCombatentes().add(clone);
				clone.setContadorTU(invocador.getContadorTU() + 50);
				invocador.registrarClone(clone);

				popularListasDeCombatentes();
				atualizarTimelineTU();
				mapController.desenharPeoes(this.estadoCombate.getCombatentes());
				System.out.println(">>> Clone spawnado em (" + clone.getPosX() + "," + clone.getPosY() + ")");
			} else {
				System.out.println(">>> FALHA AO SPAWNAR CLONE: Sem espaço livre!");
			}
		}
	}

	public void atualizarInterfaceAposMorte() {
		popularListasDeCombatentes();
		atualizarTimelineTU();
		if (mapController != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes()); // Remove peões mortos
		}
	}

	public void avancarTurnoAposAcao() {
		fecharHudEAvançar();
	}

	public void salvarPersonagem(Personagem personagem) {
		if (personagem == null || personagem.getJsonFileName() == null) {
			System.err.println("EDITOR: Falha ao salvar. Personagem ou nome do arquivo nulo.");
			return;
		}

		String nomeArquivo = personagem.getJsonFileName();
		System.out.println("EDITOR: Salvando " + nomeArquivo + "...");

		try {
			Map<String, Object> data = new HashMap<>();

			// --- DADOS FIXOS ---
			data.put("nome", personagem.getNome());
			data.put("raca", personagem.getRaca().getNome());
			data.put("classe", personagem.getClasse().getNome());
			data.put("nivel", personagem.getNivel());
			data.put("xpAtual", personagem.getXpAtual());
			data.put("pontosParaDistribuir", personagem.getPontosParaDistribuir());
			data.put("vidaMaximaBase", personagem.getVidaMaximaBase());

			// Recalcula iniciativa base para salvar o valor puro
			int des = personagem.getAtributosFinais().getOrDefault(Atributo.DESTREZA, 1);
			data.put("iniciativaBase", personagem.getPlacarIniciativa() - des);

			data.put("atributosBase", personagem.getAtributosBase());

			// --- DADOS DE EQUIPAMENTO (Strings/IDs) ---
			if (personagem.getArmaEquipada() != null) {
				Arma arma = personagem.getArmaEquipada();

				// SE FOR GRIMÓRIO: Salva como Objeto com a lista de magias atuais
				if (arma instanceof Grimorio) {
					Grimorio g = (Grimorio) arma;
					Map<String, Object> grimorioData = new HashMap<>();
					grimorioData.put("nome", g.getNome());

					// Converte a lista de objetos Habilidade para lista de Strings (Nomes)
					List<String> nomesMagias = g.getMagiasArmazenadas().stream().map(Habilidade::getNome)
							.collect(Collectors.toList());
					grimorioData.put("magiasSalvas", nomesMagias);

					data.put("armaEquipada", grimorioData);
				}
				// SE FOR ARMA COMUM: Salva apenas o nome (String)
				else {
					data.put("armaEquipada", arma.getNome());
				}
			}
			if (personagem.getArmaduraEquipada() != null) {
				data.put("armaduraEquipada", personagem.getArmaduraEquipada().getNome());
			}
			if (personagem.getAmuleto1() != null) {
				data.put("amuleto1", personagem.getAmuleto1().getNome());
			}
			if (personagem.getAmuleto2() != null) {
				data.put("amuleto2", personagem.getAmuleto2().getNome());
			}
			data.put("inventario", personagem.getInventario().getItensAgrupados());

			Map<String, Integer> moedas = new HashMap<>();
			moedas.put("bronze", personagem.getInventario().getMoedasBronze());
			moedas.put("prata", personagem.getInventario().getMoedasPrata());
			moedas.put("ouro", personagem.getInventario().getMoedasOuro());
			data.put("carteira", moedas);

			// --- DADOS DE ESTADO (SAVE GAME)
			data.put("vidaAtual", personagem.getVidaAtual());
			data.put("manaAtual", personagem.getManaAtual());
			data.put("escudoAtual", personagem.getEscudoAtual());
			data.put("contadorTU", personagem.getContadorTU());
			data.put("posX", personagem.getPosX());
			data.put("posY", personagem.getPosY());
			data.put("faccao", personagem.getFaccao());
			// ---------------------------------------------------------

			if (personagem.getRaca() instanceof Humano) {
				Humano h = (Humano) personagem.getRaca();
				Map<String, Object> dadosHumano = new HashMap<>();
				dadosHumano.put("filaContratos", h.getFilaContratos()); // Precisa do getter na classe Humano
				dadosHumano.put("vidaNegativaAcumulada", h.getVidaNegativaAcumulada()); // Precisa do getter
				data.put("racaData", dadosHumano);
			}

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			String projectPath = System.getProperty("user.dir");
			String resourcePath = projectPath + "/src/main/resources/data/players/" + nomeArquivo;

			File file = new File(resourcePath);

			if (!file.getParentFile().exists()) {
				resourcePath = projectPath + "/src/data/players/" + nomeArquivo;
				file = new File(resourcePath);
			}

			// Garante diretórios
			file.getParentFile().mkdirs();

			try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
				System.out.println("SUCESSO: Arquivo salvo em: " + file.getAbsolutePath());

				URL urlBin = getClass().getResource("/data/players/" + nomeArquivo);
				if (urlBin != null) {
					File fileBin = new File(urlBin.toURI());
					try (Writer writerBin = new FileWriter(fileBin, StandardCharsets.UTF_8)) {
						gson.toJson(data, writerBin);
						System.out.println("HOTFIX: Salvo também na pasta BIN.");
					}
				}

			}

		} catch (Exception e) {
			System.err.println("Erro CRÍTICO ao salvar JSON: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private Arma getArma(String nomeArma) {
		if (nomeArma == null || nomeArma.isEmpty()) {
			return null;
		}

		if (nomeArma.equals("Punho Infinito"))
			return new PunhoInfinito();
		if (nomeArma.equals("Murasame"))
			return new Murasame();
		if (nomeArma.equals("Terrore"))
			return new Terrore();
		if (nomeArma.equals("Pálida Vigília"))
			return new PalidaVigilia();
		if (nomeArma.equals("Rubrum"))
			return new Rubrum();
		if (nomeArma.equals("Laminas Do Exterminio"))
			return new LaminasDoExterminio();

		if (nomeArma.equals("Espada-Serra"))
			return new br.com.dantesrpg.model.armas.boss.EspadaSerra();

		if (armoryDatabase.containsKey(nomeArma)) {
			Map<String, Object> armaData = armoryDatabase.get(nomeArma);
			return mapearArma(armaData);
		}

		System.err.println("FÁBRICA DE ARMAS: Arma '" + nomeArma + "' não encontrada em .java ou .json!");
		return null;
	}

	public Personagem recarregarPersonagem(String nomeArquivoSemExtensao) {
		System.out.println("EDITOR: Resetando alterações de " + nomeArquivoSemExtensao + "...");

		Personagem personagemRecarregado = carregarPersonagemComGson(nomeArquivoSemExtensao);
		if (personagemRecarregado == null)
			return null;

		if (estadoCombate != null) {
			Personagem personagemAntigo = null;
			for (Personagem p : estadoCombate.getCombatentes()) {

				if (p.getJsonFileName() != null
						&& p.getJsonFileName().equals(personagemRecarregado.getJsonFileName())) {
					personagemAntigo = p;
					break;
				}
			}

			if (personagemAntigo != null) {
				// Preserva a Posição e TU do personagem antigo
				personagemRecarregado.setPosX(personagemAntigo.getPosX());
				personagemRecarregado.setPosY(personagemAntigo.getPosY());
				personagemRecarregado.setContadorTU(personagemAntigo.getContadorTU());

				estadoCombate.getCombatentes().remove(personagemAntigo);
				estadoCombate.getCombatentes().add(personagemRecarregado);
				System.out.println("EDITOR: " + nomeArquivoSemExtensao + " foi recarregado.");
			}
		}

		return personagemRecarregado;
	}

	private InputStream getInputStreamRecente(String caminhoRelativo) {
		try {
			// Tenta ler direto da pasta SRC do projeto (Ambiente de Desenvolvimento)
			String projectPath = System.getProperty("user.dir");

			// Tenta estrutura Maven (src/main/resources)
			File arquivoDev = new File(projectPath + "/src/main/resources" + caminhoRelativo);
			if (!arquivoDev.exists()) {
				// Tenta estrutura Eclipse simples (src)
				arquivoDev = new File(projectPath + "/src" + caminhoRelativo);
			}

			if (arquivoDev.exists()) {
				System.out.println("IO: Lendo versão DEV de: " + arquivoDev.getAbsolutePath());
				return new java.io.FileInputStream(arquivoDev);
			}

		} catch (Exception e) {
			// Ignora erro de dev e cai no fallback
		}

		// Fallback: Lê do classpath/bin (Ambiente Compilado)
		System.out.println("IO: Lendo versão BIN de: " + caminhoRelativo);
		return getClass().getResourceAsStream(caminhoRelativo);
	}

	private void loadItempediaDatabase() {
		this.itempediaDatabase = new HashMap<>();
		Gson gson = new Gson();
		String[] arquivos = { "/data/consumiveis.json", "/data/armaduras.json", "/data/amuletos.json" };

		for (String resourcePath : arquivos) {
			try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
				if (is == null)
					continue;

				try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
					Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {
					}.getType();
					Map<String, Map<String, Object>> dados = gson.fromJson(reader, mapType);
					this.itempediaDatabase.putAll(dados);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Item getItem(String tipoItem) {
		if (tipoItem == null)
			return null;

		// Verifica se está na Itempedia (Itens Gerais: Consumíveis, Armaduras, Amuletos)
		if (itempediaDatabase != null && itempediaDatabase.containsKey(tipoItem)) {
			try {
				Map<String, Object> data = itempediaDatabase.get(tipoItem);

				// Campo discriminador (se não tiver, assume Consumível por compatibilidade ou verifica campos)
				String classeItem = (String) data.getOrDefault("classeItem", "Consumivel");
				String descricao = (String) data.getOrDefault("descricao", "Sem descrição.");
				String tipoMoeda = (String) data.getOrDefault("tipoMoeda", "BRONZE");
				int valor = ((Double) data.getOrDefault("valorMoedas", 0.0)).intValue();

				String nome = (String) data.getOrDefault("nome", tipoItem);
				String desc = (String) data.getOrDefault("descricao", "");

				Map<String, Double> modStatus = parseModificadoresStatus(
						(Map<String, Double>) data.get("modificadoresStatus"));

				// --- TIPO: CONSUMÍVEL ---
				if ("Consumivel".equalsIgnoreCase(classeItem)) {
					int custoTU = ((Double) data.getOrDefault("custoTU", 100.0)).intValue();
					boolean usavel = (Boolean) data.getOrDefault("usavelEmCombate", false);
					Map<String, Double> efeitos = (Map<String, Double>) data.get("efeitos");
					Consumivel c = new Consumivel(tipoItem, nome, descricao, valor, custoTU, usavel, efeitos);
					c.setTipoMoeda(tipoMoeda);
					return c;
				}

				if ("Armadura".equalsIgnoreCase(classeItem)) {
					int armaduraBase = ((Double) data.getOrDefault("armaduraBase", 0.0)).intValue();
					Map<String, Double> modsJson = (Map<String, Double>) data.get("modificadoresDeAtributo");
					Map<Atributo, Integer> modsAtr = parseModificadores(modsJson);

					// Passa o modStatus novo
					return new br.com.dantesrpg.model.Armadura(nome, descricao, valor, armaduraBase, modsAtr,
							modStatus);
				}

				else if ("Amuleto".equalsIgnoreCase(classeItem)) {
					int armaduraBonus = ((Double) data.getOrDefault("armaduraBonus", 0.0)).intValue();
					Map<String, Double> modsJson = (Map<String, Double>) data.get("modificadoresDeAtributo");
					Map<Atributo, Integer> modsAtr = parseModificadores(modsJson);

					// Passa o modStatus novo
					return new br.com.dantesrpg.model.Amuleto(nome, descricao, valor, armaduraBonus, modsAtr,
							modStatus);
				}

			} catch (Exception e) {
				System.err.println("Erro ao mapear Item da Itempedia: " + tipoItem);
				e.printStackTrace();
				return null;
			}
		}

		// Verifica Essências (Especial)
		if (tipoItem.startsWith("Essência de ")) {
			String nomeSujo = tipoItem.substring("Essência de ".length());
			String nomeMonstroAlvo = tipoItem.substring("Essência de ".length());
			String nomeLimpo = nomeSujo.replaceAll("[0-9]+$", "").trim();
			System.out.println("DEBUG ESSÊNCIA: Sujo='" + nomeSujo + "' -> Limpo='" + nomeLimpo + "'");
			Map<String, Object> dadosMonstro = null;

			if (bestiarioDatabase != null) {
				for (Map.Entry<String, Map<String, Object>> entry : bestiarioDatabase.entrySet()) {
					String nomeNoBestiario = (String) entry.getValue().get("nome");

					// Comparação segura
					if (nomeNoBestiario != null && nomeNoBestiario.trim().equalsIgnoreCase(nomeLimpo)) {
						dadosMonstro = entry.getValue();
						break;
					}
				}
			}

			if (dadosMonstro != null) {
				String raca = (String) dadosMonstro.getOrDefault("raca", "Monstro");
				int vida = ((Double) dadosMonstro.getOrDefault("vida", 10.0)).intValue();
				int agi = ((Double) dadosMonstro.getOrDefault("agilidade", 1.0)).intValue();
				int def = ((Double) dadosMonstro.getOrDefault("defesa", 0.0)).intValue();
				String nomeArma = (String) dadosMonstro.getOrDefault("arma", null);

				// Monta atributos basicos
				Map<Atributo, Integer> atr = new HashMap<>();
				for (Atributo a : Atributo.values())
					atr.put(a, 1);
				atr.put(Atributo.DESTREZA, def);
				atr.put(Atributo.TOPOR, def);

				Personagem dummy = new Personagem(nomeMonstroAlvo, new RaçaPlaceholder(), new ClassePlaceholder(), 1,
						atr, vida, 0);
				if (nomeArma != null)
					dummy.setArmaEquipada(getArma(nomeArma));

				return new EssenciaInimigo(dummy);
			} else {
				// Fallback se não achar no bestiário (ex: monstro deletado ou nome alterado)
				// Cria uma essência genérica para não crashar o inventário
				System.err.println(
						"Aviso: Essência de '" + nomeMonstroAlvo + "' não encontrada no bestiário. Criando genérica.");
				Personagem dummyGenerico = new Personagem(nomeMonstroAlvo, new RaçaPlaceholder(),
						new ClassePlaceholder(), 1, new HashMap<>(), 10, 0);
				return new EssenciaInimigo(dummyGenerico);
			}
		}

		// Verifica Armaria (Armas)
		Item itemComoEquipamento = getArma(tipoItem);
		if (itemComoEquipamento != null) {
			return itemComoEquipamento;
		}

		System.err.println("FÁBRICA DE ITENS: Tipo de item desconhecido: " + tipoItem);
		return null;
	}

	// Método auxiliar para ler os modificadores (evita repetição de código)
	private Map<Atributo, Integer> parseModificadores(Map<String, Double> modsJson) {
		Map<Atributo, Integer> mods = new HashMap<>();
		if (modsJson != null) {
			for (Map.Entry<String, Double> entry : modsJson.entrySet()) {
				try {
					mods.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return mods;
	}

	private void loadBestiarioDatabase() {
		this.bestiarioDatabase = new HashMap<>();
		Gson gson = new Gson();
		String resourcePath = "/data/bestiario.json";
		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null)
				return;
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {
				}.getType();
				this.bestiarioDatabase = gson.fromJson(reader, mapType);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Erro ao ler bestiario.json:");
		}
	}

	public void entrarModoSpawnMultiploCustom(Map<String, Object> dadosCustom, int quantidade) {
		this.templateSpawnCustomizado = dadosCustom; // Guarda os dados
		this.modoSpawnAtivo = true;
		// O ID serve apenas de referência visual ou log agora
		this.idMonstroParaSpawn = (String) dadosCustom.getOrDefault("id", "Custom");

		if (mapController != null && mapStage != null) {
			if (mapStage.isIconified())
				mapStage.setIconified(false);
			mapStage.show();
			mapStage.toFront();
			mapStage.requestFocus();

			mapController.entrarModoSpawn(idMonstroParaSpawn, quantidade);
		}
	}

	public void spawnarMonstro(String idMonstro, int x, int y) {
		if (bestiarioDatabase == null || !bestiarioDatabase.containsKey(idMonstro)) {
			System.err.println("Erro: Monstro '" + idMonstro + "' não encontrado no bestiário.");
			return;
		}

		Map<String, Object> data = bestiarioDatabase.get(idMonstro);

		// Extrai dados
		String nomeBase = (String) data.getOrDefault("nome", idMonstro);
		String nomeRaca = (String) data.getOrDefault("raca", "Monstro");
		int vidaMax = ((Double) data.getOrDefault("vida", 10.0)).intValue();
		int agilidade = ((Double) data.getOrDefault("agilidade", 1.0)).intValue();
		int defesa = ((Double) data.getOrDefault("defesa", 0.0)).intValue();
		String nomeArma = (String) data.getOrDefault("arma", null);
		int xpReward = ((Double) data.getOrDefault("xpReward", 0.0)).intValue();

		int tamanhoX = data.containsKey("tamanhoX") ? ((Number) data.get("tamanhoX")).intValue() : 1;
		int tamanhoY = data.containsKey("tamanhoY") ? ((Number) data.get("tamanhoY")).intValue() : 1;

		int segmentos = ((Number) data.getOrDefault("segmentos", 0.0)).intValue();
		int grau = ((Number) data.getOrDefault("grau", 0.0)).intValue();

		// Constrói Atributos
		Map<Atributo, Integer> atributos = new HashMap<>();
		for (Atributo a : Atributo.values())
			atributos.put(a, 1);
		atributos.put(Atributo.DESTREZA, agilidade);
		atributos.put(Atributo.TOPOR, defesa);

		// Calcula Nome Único
		long qtdExistente = estadoCombate.getCombatentes().stream().filter(p -> p.getNome().startsWith(nomeBase))
				.count();
		String nomeFinal = nomeBase + " " + (qtdExistente + 1);

		Personagem monstro = new Personagem(nomeFinal, new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, atributos, vidaMax, 0);

		monstro.setFaccao("INIMIGO");
		monstro.setXpReward(xpReward);

		monstro.setPosX(x);
		monstro.setPosY(y);

		monstro.setTamanhoX(tamanhoX);
		monstro.setTamanhoY(tamanhoY);

		if (idMonstro.equalsIgnoreCase("Nebrion")) {
			monstro.setTamanhoX(7);
			monstro.setTamanhoY(7);
		}

		// Aplica o valor corrigido
		monstro.setGrau(grau);
		monstro.setSegmentosVida(segmentos);

		// Propriedades
		List<String> props = new ArrayList<>();
		Object propsObj = data.getOrDefault("propriedades", null);
		if (propsObj instanceof List) {
			props.addAll((List<String>) propsObj);
		}
		monstro.setPropriedades(props);

		if (nomeArma != null) {
			Arma arma = getArma(nomeArma);
			monstro.setArmaEquipada(arma);
		} else {
			monstro.setArmaEquipada(getArma("Punhos"));
		}

		// Lógica de Blindado (Escudo ao nascer)
		int nivelBlindado = monstro.getValorPropriedade("BLINDADO");
		if (nivelBlindado > 0) {
			double porcentagem = 0.20 * nivelBlindado;
			monstro.setEscudoAtual(vidaMax * porcentagem);
		}

		int nivelARMADURADO = monstro.getValorPropriedade("ARMADURADO");
		if (nivelARMADURADO > 0) {
			double porcentagem = 0.50 * nivelARMADURADO;
			monstro.setEscudoAtual(vidaMax * porcentagem);
		}

		monstro.recalcularAtributosEstatisticas();
		monstro.setVidaAtual(monstro.getVidaMaxima());

		estadoCombate.getCombatentes().add(monstro);

		popularListasDeCombatentes();
		atualizarTimelineTU();
		if (mapController != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes());
		}

		System.out.println("SPAWN: " + nomeFinal + " (Segmentos: " + segmentos + ")");
	}

	// Getter auxiliar para a UI do Bestiário listar os nomes
	public Map<String, Map<String, Object>> getBestiarioDatabase() {
		return this.bestiarioDatabase;
	}

	public void entrarModoSpawn(String idMonstro) {
		this.idMonstroParaSpawn = idMonstro;
		this.modoSpawnAtivo = true;

		if (mapController != null && mapStage != null) {
			mapStage.requestFocus();
			mapController.entrarModoSpawn(idMonstro, 1);
		}
		System.out.println("GM: Modo SPAWN ATIVO para: " + idMonstro);
	}

	public void entrarModoSpawnMultiplo(String idMonstro, int quantidade) {
		this.idMonstroParaSpawn = idMonstro;
		this.modoSpawnAtivo = true;

		if (mapController != null && mapStage != null) {
			if (mapStage.isIconified())
				mapStage.setIconified(false);
			mapStage.show();
			mapStage.toFront();
			mapStage.requestFocus();

			mapController.entrarModoSpawn(idMonstro, quantidade);
		}
	}

	public void resolverSpawn(String idMonstro, int x, int y) {
		if (!modoSpawnAtivo)
			return;

		Map<String, Object> dadosMonstro;

		// Decide a fonte dos dados (Custom ou JSON Original)
		if (this.templateSpawnCustomizado != null) {
			dadosMonstro = this.templateSpawnCustomizado;
		} else {
			// Usa 'bestiarioDatabase'
			dadosMonstro = this.bestiarioDatabase.get(idMonstro);
		}

		if (dadosMonstro == null) {
			System.err.println("Erro: Template de monstro não encontrado: " + idMonstro);
			return;
		}

		// Extração de Dados (com Defaults seguros)
		String nome = (String) dadosMonstro.getOrDefault("nome", idMonstro);
		String racaNome = (String) dadosMonstro.getOrDefault("raca", "Monstro");

		double vida = ((Number) dadosMonstro.getOrDefault("vida", 10.0)).doubleValue();
		double mana = ((Number) dadosMonstro.getOrDefault("mana", 0.0)).doubleValue();

		// No JSON o nome pode ser 'agilidade' ou 'iniciativaBase'
		int agi = 1;
		if (dadosMonstro.containsKey("agilidade"))
			agi = ((Number) dadosMonstro.get("agilidade")).intValue();
		else if (dadosMonstro.containsKey("iniciativaBase"))
			agi = ((Number) dadosMonstro.get("iniciativaBase")).intValue();

		int def = ((Number) dadosMonstro.getOrDefault("defesa", 0.0)).intValue();

		int tx = dadosMonstro.containsKey("tamanhoX") ? ((Number) dadosMonstro.get("tamanhoX")).intValue() : 1;
		int ty = dadosMonstro.containsKey("tamanhoY") ? ((Number) dadosMonstro.get("tamanhoY")).intValue() : 1;

		System.out.println("DEBUG JSON KEYS: " + dadosMonstro.keySet());
		int xp = ((Number) dadosMonstro.getOrDefault("xpReward", 0.0)).intValue();
		int grau = ((Number) dadosMonstro.getOrDefault("grau", 0.0)).intValue();
		int segmentos = ((Number) dadosMonstro.getOrDefault("segmentos", 0.0)).intValue();
		String nomeArma = (String) dadosMonstro.getOrDefault("arma", null);

		if (idMonstro.equalsIgnoreCase("Nebrion")) {
			tx = 7;
			ty = 7;
		}

		if (idMonstro.startsWith("MinosFase")) {
			tx = 24;
			ty = 24;
		}

		// Extração de Propriedades
		List<String> props = new ArrayList<>();
		Object propsObj = dadosMonstro.getOrDefault("propriedades", null);
		if (propsObj instanceof List) {
			props.addAll((List<String>) propsObj);
		}

		long count = estadoCombate.getCombatentes().stream().filter(p -> p.getNome().startsWith(nome)).count();

		List<Personagem> existentes = estadoCombate.getCombatentes().stream().filter(p -> p.getNome().startsWith(nome))
				.collect(Collectors.toList());

		String nomeFinal;

		if (existentes.isEmpty()) {
			// Cenário A: É o primeiro. Nome limpo.
			nomeFinal = nome;
		} else {
			// Cenário B: Já existem outros.

			// Verifica se existe algum com o nome "puro" (sem número) e renomeia para "Nome 1"
			Optional<Personagem> purista = existentes.stream().filter(p -> p.getNome().equals(nome)).findFirst();

			if (purista.isPresent()) {
				purista.get().setNome(nome + " 1");
				System.out.println("GM: " + nome + " original foi renomeado para " + nome + " 1");
			}

			// O novo recebe o próximo número (Total + 1)
			nomeFinal = nome + " " + (existentes.size() + 1);
		}
		Map<Atributo, Integer> atributosBase = new HashMap<>();
		for (Atributo a : Atributo.values())
			atributosBase.put(a, 1);
		atributosBase.put(Atributo.DESTREZA, agi);
		atributosBase.put(Atributo.TOPOR, def);

		Personagem monstro = new Personagem(nomeFinal, new br.com.dantesrpg.model.racas.RaçaPlaceholder(), // Raça
																											// Genérica
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), // Classe Genérica
				1, atributosBase, vida, // Vida Base
				0);

		// Overrides de Stats
		monstro.setVidaMaxima(vida);
		monstro.setVidaAtual(vida);

		monstro.setManaMaxima(mana);
		monstro.setManaAtual(mana);

		monstro.setXpReward(xp);
		monstro.setGrau(grau);
		monstro.setSegmentosVida(segmentos);
		System.out.println("DEBUG SPAWN: " + nomeFinal + " criado com " + segmentos + " segmentos.");

		monstro.setTamanhoX(tx);
		monstro.setTamanhoY(ty);

		monstro.setPosX(x);
		monstro.setPosY(y);

		monstro.setFaccao("INIMIGO");

		// Seta Propriedades
		monstro.setPropriedades(props);

		// Equipamento (Arma)
		if (nomeArma != null && !nomeArma.isEmpty()) {
			Arma arma = getArma(nomeArma);
			if (arma != null) {
				monstro.setArmaEquipada(arma);
			} else {
				monstro.setArmaEquipada(getArma("Punhos"));
			}
		} else {
			// Garante ao menos Punhos para não quebrar combate
			monstro.setArmaEquipada(getArma("Punhos"));
		}

		// Aplica Propriedades "On Spawn"
		int nivelBlindado = monstro.getValorPropriedade("BLINDADO");
		if (nivelBlindado > 0) {
			double porcentagem = 0.20 * nivelBlindado;
			monstro.setEscudoAtual(vida * porcentagem);
			System.out.println(">>> PROPRIEDADE: Blindado Nível " + nivelBlindado + " (" + (int) (porcentagem * 100)
					+ "% Escudo)");
		}

		int nivelARMADURADO = monstro.getValorPropriedade("ARMADURADO");
		if (nivelARMADURADO > 0) {
			double porcentagem = 0.50 * nivelARMADURADO;
			monstro.setEscudoAtual(vida * porcentagem);
		}

		// Finalização
		monstro.recalcularAtributosEstatisticas(); // Garante que stats derivados (como Defesa) estejam certos
		monstro.setVidaAtual(monstro.getVidaMaxima()); // Cura total após recalculo

		estadoCombate.getCombatentes().add(monstro);

		// Atualiza a UI
		popularListasDeCombatentes();
		atualizarTimelineTU();
		if (mapController != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes());
		}

		System.out.println(
				"GM: Monstro spawnado: " + nomeFinal + " [Custom: " + (this.templateSpawnCustomizado != null) + "]");
	}

	public void alvosIdentificadosNoMapa(List<Personagem> alvos) {
		if (detailedTurnHudController != null) {
			// Atualiza a HUD com a lista de alvos potenciais (o que habilita o botão Confirmar)
			detailedTurnHudController.adicionarAlvos(alvos);
		}
	}

	public List<String> getListaNomesArmas() {
		if (armoryDatabase == null)
			return new ArrayList<>();
		return new ArrayList<>(armoryDatabase.keySet());
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
		if (mapController != null) {
			mapController.desenharPeoes(estadoCombate.getCombatentes());
		}
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
		System.out.println("SISTEMA: Salvando estado de todos os jogadores...");
		if (estadoCombate == null)
			return;

		for (Personagem p : estadoCombate.getCombatentes()) {
			if (isPlayer(p)) {
				salvarPersonagem(p);
			}
		}
		System.out.println("SISTEMA: Salvamento concluído.");
	}

	public void acionarTransicaoDeMapa(Personagem jogadorQuePisou) {
		System.out.println(">>> TRANSICAO: " + jogadorQuePisou.getNome() + " encontrou a saída!");

		// Pergunta ao Mestre se quer viajar
		javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
				javafx.scene.control.Alert.AlertType.CONFIRMATION);
		alert.setTitle("Transição de Mapa");
		alert.setHeaderText(jogadorQuePisou.getNome() + " pisou na Zona de Saída.");
		alert.setContentText("Deseja encerrar o combate atual, distribuir XP e carregar a próxima arena?");

		Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {

			// Distribui XP e Encerra Combate Atual
			br.com.dantesrpg.model.util.SessionLogger.log("--- FIM DO COMBATE (Transição) ---");
			combatManager.distribuirXpAposCombate(estadoCombate);

			// Abre Seletor de Novo Mapa
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Selecione a Próxima Arena");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg"));

			// Tenta abrir na pasta resources/mapas
			String mapPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "mapas";
			File initialDir = new File(mapPath);
			if (initialDir.exists())
				fileChooser.setInitialDirectory(initialDir);

			File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

			if (selectedFile != null) {
				carregarNovaArenaLogic(selectedFile);
			}
		}
	}

	public void iniciarMovimentoTatico(Personagem ator) {
		// Esconde a HUD
		if (detailedTurnHudStage != null)
			detailedTurnHudStage.hide();

		// Configura o mapa para Movimento Tático (Regras de Jogo)
		if (mapController != null) {
			if (mapStage != null) {
				mapStage.show();
				mapStage.toFront();
				mapStage.requestFocus();
			}

			mapController.entrarModoSelecao(null, ator);
		}
		if (btnConfirmarMovimento != null) {
			btnConfirmarMovimento.setVisible(true);
			btnConfirmarMovimento.setManaged(true);
			btnConfirmarMovimento.setText("CONCLUIR MOVIMENTO (" + ator.getNome() + ")");
			btnConfirmarMovimento.toFront();
		}
	}

	@FXML
	private void onConfirmarMovimentoClick() {
		// Desativa modo mover no mapa
		if (mapController != null) {
			// Sai do modo de seleção/movimento
			mapController.sairModoSelecao();
			if (mapController.isModoMovimentoLivre()) {
				mapController.toggleModoMovimentoLivre();
			}
		}

		// Esconde o botão verde
		if (btnConfirmarMovimento != null) {
			btnConfirmarMovimento.setVisible(false);
			btnConfirmarMovimento.setManaged(false);
		}

		// Aplica Custo de TU
		Personagem ator = estadoCombate.getAtorAtual();
		if (ator != null) {
			ator.setContadorTU(ator.getContadorTU() + 100);
			System.out.println(">>> Movimento Tático concluído. +100 TU.");
			if (combatManager != null) {
				combatManager.atualizarAuras(estadoCombate);
			}
			resolverAcaoPassarVez(new AcaoMestreInput(ator, new ArrayList<>(), (Habilidade) null));
		}
	}

	public void criarObjetoNoMapa(int x, int y) {
		// Verifica se já tem algo lá
		removerObjetoNoMapa(x, y);

		// Cria um objeto padrão 
		ObjetoDestrutivel barreira = new ObjetoDestrutivel("Barricada de Madeira", 50, 5, true);
		barreira.setPosX(x);
		barreira.setPosY(y);

		// Adiciona à lista geral para ser alvo de ataques
		this.estadoCombate.getCombatentes().add(barreira);

		System.out.println("EDITOR: Objeto criado em (" + x + "," + y + ")");
	}

	// Chamado pelo Editor de Mapa (ao apagar) ou pelo CombatManager (ao destruir)
	public void removerObjetoNoMapa(int x, int y) {
		if (estadoCombate == null)
			return;

		// Remove da lista lógica
		estadoCombate.getCombatentes()
				.removeIf(p -> p instanceof ObjetoDestrutivel && p.getPosX() == x && p.getPosY() == y);
	}

	public MapController getMapController() {
		return this.mapController;
	}

	// Método auxiliar para limpar e carregar
	private void carregarNovaArenaLogic(File mapaFile) {
		br.com.dantesrpg.model.util.SessionLogger.log(">>> Viajando para nova área: " + mapaFile.getName());

		// Remove Inimigos e Clones antigos
		encerrarContratosBarbaros();
		limparClonesDoCombate();
		estadoCombate.getCombatentes().removeIf(p -> !isPlayer(p));

		// Carrega o Mapa Visual
		if (mapController != null) {
			mapController.carregarMapaDeImagem(mapaFile);
		}

		// Reseta Iniciativa e Posições (Coloca todos no canto 0,0 )
		estadoCombate.resetarIniciativa();

		// Zera posições para evitar estar dentro de parede no mapa novo
		for (Personagem p : estadoCombate.getCombatentes()) {
			p.setPosX(0);
			p.setPosY(0);
			p.setMovimentoRestanteTurno(p.getMovimento());
		}

		// Atualiza UI
		popularListasDeCombatentes();
		atualizarTimelineTU();

		System.out.println("NOVA ARENA CARREGADA COM SUCESSO.");
	}

	private Map<String, Double> parseModificadoresStatus(Map<String, Double> input) {
		if (input == null)
			return new HashMap<>();
		return new HashMap<>(input); // Retorna uma cópia limpa
	}

	private Arma criarCopiaDaArma(Arma original) {
		if (original == null)
			return null;

		// Cria nova instância
		Arma copia = getArma(original.getNome());

		if (copia == null)
			return null;

		if (copia.isRequerMunicao()) {
			copia.recarregar();
		}

		// Copia magias (Grimório)
		if (original instanceof Grimorio && copia instanceof Grimorio) {
			Grimorio originalG = (Grimorio) original;
			Grimorio copiaG = (Grimorio) copia;
			copiaG.getMagiasArmazenadas().clear();
			copiaG.getMagiasArmazenadas().addAll(originalG.getMagiasArmazenadas());
		}

		return copia;
	}

	// --- Ações GM Toolbar Placeholders ---
	@FXML
	private void onGerenciarCombateClick() {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/GerenciadorCombateView.fxml"));
			Parent root = loader.load();

			GerenciadorCombateController controller = loader.getController();
			controller.setMainController(this, this.estadoCombate);

			List<String> todosItens = new ArrayList<>();
			if (itempediaDatabase != null)
				todosItens.addAll(itempediaDatabase.keySet());
			if (armoryDatabase != null)
				todosItens.addAll(armoryDatabase.keySet());
			controller.setListaItensMestre(todosItens);

			Stage stage = new Stage();
			stage.setTitle("Painel do Mestre");
			stage.setScene(new Scene(root));
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onMovimentoLivreClick() {
		System.out.println("Botão 'Movimento Livre' clicado.");
		if (mapController != null) {
			mapController.toggleModoMovimentoLivre();
		}
	}

	@FXML
	private void onEditorMapaClick() {
		System.out.println("Botão 'Editor de Mapa' clicado.");
		if (mapController != null) {
			mapController.toggleModoEditor();
		}
	}

	public void abrirJanelaBestiario() {
		try {
			if (bestiarioStage == null) {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/BestiarioView.fxml"));
				Parent root = loader.load();
				bestiarioController = loader.getController();

				bestiarioStage = new Stage();
				bestiarioStage.setTitle("Bestiário");
				bestiarioStage.setScene(new Scene(root));
				bestiarioStage.initModality(Modality.WINDOW_MODAL);
				bestiarioController.setStage(bestiarioStage);
			}
			bestiarioController.inicializar(this, this.bestiarioDatabase);
			bestiarioStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

}
