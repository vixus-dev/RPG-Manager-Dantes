package br.com.dantesrpg.controller.service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import br.com.dantesrpg.model.util.PartyPreset;
import br.com.dantesrpg.model.util.PartyPresetsData;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.Atributo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ReforcosDialogService {

	private final CombatController controller;
	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<Map<String, Map<String, Object>>> armorySupplier;
	private final Supplier<Map<String, Map<String, Object>>> itempediaSupplier;
	private final Supplier<List<String>> listarArquivosPersonagens;
	private final Function<String, Personagem> carregarPersonagem;
	private final Function<String, Raça> mapearRaca;
	private final Function<String, Classe> mapearClasse;
	private final Function<String, FantasmaNobre> instanciarFantasmaNobre;
	private final Function<String, Arma> buscarArma;
	private final Function<String, Item> buscarItem;
	private final Consumer<Personagem> salvarPersonagem;
	private final Runnable atualizarInterfaceRoster;

	public ReforcosDialogService(CombatController controller, Supplier<EstadoCombate> estadoSupplier,
			Supplier<Map<String, Map<String, Object>>> armorySupplier,
			Supplier<Map<String, Map<String, Object>>> itempediaSupplier,
			Supplier<List<String>> listarArquivosPersonagens, Function<String, Personagem> carregarPersonagem,
			Function<String, Raça> mapearRaca, Function<String, Classe> mapearClasse,
			Function<String, FantasmaNobre> instanciarFantasmaNobre, Function<String, Arma> buscarArma,
			Function<String, Item> buscarItem, Consumer<Personagem> salvarPersonagem,
			Runnable atualizarInterfaceRoster) {
		this.controller = controller;
		this.estadoSupplier = estadoSupplier;
		this.armorySupplier = armorySupplier;
		this.itempediaSupplier = itempediaSupplier;
		this.listarArquivosPersonagens = listarArquivosPersonagens;
		this.carregarPersonagem = carregarPersonagem;
		this.mapearRaca = mapearRaca;
		this.mapearClasse = mapearClasse;
		this.instanciarFantasmaNobre = instanciarFantasmaNobre;
		this.buscarArma = buscarArma;
		this.buscarItem = buscarItem;
		this.salvarPersonagem = salvarPersonagem;
		this.atualizarInterfaceRoster = atualizarInterfaceRoster;
	}

	public void abrirPainel() {
		Stage stage = new Stage();
		stage.setTitle("Painel de Reforços e Roster");
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setResizable(true);
		stage.setMinWidth(950);
		stage.setMinHeight(650);

		TabPane tabPane = new TabPane();
		tabPane.setStyle("-fx-background-color: #121218;");

		ListView<Personagem> lvReforcos = criarListaPersonagens();
		ListView<Personagem> lvAtivos = criarListaPersonagens();
		Runnable refreshLists = () -> atualizarListas(lvReforcos, lvAtivos);

		ListView<String> lvPresets = new ListView<>();
		lvPresets.setStyle("-fx-background-color: #16161e; -fx-control-inner-background: #16161e;");
		Runnable refreshPresetsList = () -> atualizarListaPresets(lvPresets);

		Tab tabGerenciar = criarAbaGerenciar(lvReforcos, lvAtivos, refreshLists);
		Tab tabCriar = criarAbaCriar(tabPane, refreshLists);
		Tab tabPresets = criarAbaPresets(lvPresets, lvAtivos, refreshLists, refreshPresetsList);
		tabPane.getTabs().addAll(tabGerenciar, tabCriar, tabPresets);

		BorderPane root = new BorderPane();
		root.setCenter(tabPane);

		Scene scene = new Scene(root, 950, 700);
		try {
			scene.getStylesheets().add(controller.getClass()
					.getResource("/br/com/dantesrpg/view/style.css").toExternalForm());
		} catch (Exception ex) {
			System.err.println("Could not load style.css in reinforcements dialog");
		}

		stage.setScene(scene);
		refreshLists.run();
		refreshPresetsList.run();
		stage.showAndWait();
	}

	private ListView<Personagem> criarListaPersonagens() {
		ListView<Personagem> listView = new ListView<>();
		listView.setStyle("-fx-background-color: #16161e; -fx-control-inner-background: #16161e;");
		listView.setPrefHeight(400);
		VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS);
		listView.setCellFactory(param -> new ListCell<Personagem>() {
			@Override
			protected void updateItem(Personagem personagem, boolean empty) {
				super.updateItem(personagem, empty);
				if (empty || personagem == null) {
					setText(null);
					setGraphic(null);
					return;
				}

				HBox cell = new HBox(10);
				cell.setAlignment(Pos.CENTER_LEFT);
				Label lblName = new Label(personagem.getNome());
				lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
				Label lblDetails = new Label(personagem.getRaca().getNome() + " / "
						+ personagem.getClasse().getNome() + " (Nível " + personagem.getNivel() + ")");
				lblDetails.setStyle("-fx-text-fill: #808090; -fx-font-size: 11px;");
				cell.getChildren().addAll(lblName, lblDetails);
				setGraphic(cell);
			}
		});
		return listView;
	}

	private Tab criarAbaGerenciar(ListView<Personagem> lvReforcos, ListView<Personagem> lvAtivos,
			Runnable refreshLists) {
		Tab tabGerenciar = new Tab("Gerenciar Roster");
		tabGerenciar.setClosable(false);

		HBox gerenciarLayout = new HBox(20);
		gerenciarLayout.setPadding(new Insets(15));
		gerenciarLayout.setStyle("-fx-background-color: #121218;");

		VBox panelReforcos = new VBox(10);
		HBox.setHgrow(panelReforcos, javafx.scene.layout.Priority.ALWAYS);
		Label lblReforcos = new Label("Banco de Reforços (Inativos)");
		lblReforcos.setStyle("-fx-text-fill: #e94560; -fx-font-size: 16px; -fx-font-weight: bold;");

		HBox btnControlsReforcos = new HBox(10);
		Button btnAddAoCombate = new Button("Adicionar ao Combate");
		btnAddAoCombate.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
		Button btnExcluir = new Button("Excluir Definitivamente");
		btnExcluir.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
		btnControlsReforcos.getChildren().addAll(btnAddAoCombate, btnExcluir);
		panelReforcos.getChildren().addAll(lblReforcos, lvReforcos, btnControlsReforcos);

		VBox panelAtivos = new VBox(10);
		HBox.setHgrow(panelAtivos, javafx.scene.layout.Priority.ALWAYS);
		Label lblAtivos = new Label("Ativos no Combate");
		lblAtivos.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");
		Button btnRemoverDoCombate = new Button("Recuar (Voltar para Reforços)");
		btnRemoverDoCombate.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
		panelAtivos.getChildren().addAll(lblAtivos, lvAtivos, btnRemoverDoCombate);

		btnAddAoCombate.setOnAction(e -> {
			Personagem personagem = lvReforcos.getSelectionModel().getSelectedItem();
			if (personagem != null) {
				adicionarAoCombate(personagem);
				refreshLists.run();
			}
		});

		btnRemoverDoCombate.setOnAction(e -> {
			Personagem personagem = lvAtivos.getSelectionModel().getSelectedItem();
			EstadoCombate estado = estadoSupplier.get();
			if (personagem != null && estado != null) {
				estado.getCombatentes().remove(personagem);
				atualizarInterfaceRoster.run();
				refreshLists.run();
			}
		});

		btnExcluir.setOnAction(e -> {
			Personagem personagem = lvReforcos.getSelectionModel().getSelectedItem();
			if (personagem != null && confirmarExclusao(personagem)) {
				excluirArquivosPersonagem(personagem);
				refreshLists.run();
			}
		});

		gerenciarLayout.getChildren().addAll(panelReforcos, panelAtivos);
		tabGerenciar.setContent(gerenciarLayout);
		return tabGerenciar;
	}

	private Tab criarAbaCriar(TabPane tabPane, Runnable refreshLists) {
		Tab tabCriar = new Tab("Criador de Personagens");
		tabCriar.setClosable(false);

		ScrollPane scrollCriar = new ScrollPane();
		scrollCriar.setFitToWidth(true);
		scrollCriar.setStyle("-fx-background: #121218; -fx-background-color: #121218;");

		VBox formLayout = new VBox(15);
		formLayout.setPadding(new Insets(20));
		formLayout.setStyle("-fx-background-color: #121218;");

		GridPane grid = criarFormularioCriacao(tabPane, refreshLists);
		formLayout.getChildren().addAll(grid);
		scrollCriar.setContent(formLayout);
		tabCriar.setContent(scrollCriar);
		return tabCriar;
	}

	private GridPane criarFormularioCriacao(TabPane tabPane, Runnable refreshLists) {
		GridPane grid = new GridPane();
		grid.setHgap(15);
		grid.setVgap(12);
		grid.setStyle("-fx-background-color: #121218;");

		grid.add(new Label("Nome:"), 0, 0);
		TextField tfNome = new TextField();
		tfNome.setPromptText("Nome do Personagem");
		grid.add(tfNome, 1, 0);

		grid.add(new Label("Raça:"), 0, 1);
		ComboBox<String> cbRaca = new ComboBox<>();
		cbRaca.getItems().addAll("Humano", "Anao", "Vampiro", "Elfo", "Marionette", "Half-Angel", "Half-Demon",
				"Anjo-Caido", "Lobisomem", "Arcanjo");
		cbRaca.setValue("Humano");
		grid.add(cbRaca, 1, 1);

		grid.add(new Label("Classe:"), 0, 2);
		ComboBox<String> cbClasse = new ComboBox<>();
		cbClasse.getItems().addAll("Barbaro", "Pugilista", "Feiticeiro", "Mestre das Balas", "Pistoleiro",
				"Ladino", "Paladino", "Ilusionista", "Invocador", "Campeão");
		cbClasse.setValue("Pugilista");
		grid.add(cbClasse, 1, 2);

		grid.add(new Label("Nível:"), 0, 3);
		Spinner<Integer> spNivel = new Spinner<>(1, 100, 1);
		spNivel.setEditable(true);
		grid.add(spNivel, 1, 3);

		grid.add(new Label("Vida Máxima Base:"), 0, 4);
		Spinner<Integer> spVida = new Spinner<>(10, 999, 100);
		spVida.setEditable(true);
		grid.add(spVida, 1, 4);

		grid.add(new Label("Iniciativa Base:"), 0, 5);
		Spinner<Integer> spIniciativa = new Spinner<>(-50, 50, 0);
		spIniciativa.setEditable(true);
		grid.add(spIniciativa, 1, 5);

		Label lblAtrTitle = new Label("Atributos Base");
		lblAtrTitle.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold; -fx-font-size: 14px;");
		grid.add(lblAtrTitle, 0, 6, 2, 1);

		Map<Atributo, Spinner<Integer>> spAtributos = new HashMap<>();
		int row = 7;
		for (Atributo atr : Atributo.values()) {
			grid.add(new Label(atr.name() + ":"), 0, row);
			Spinner<Integer> sp = new Spinner<>(1, 30, 10);
			sp.setEditable(true);
			grid.add(sp, 1, row);
			spAtributos.put(atr, sp);
			row++;
		}

		Label lblEquipTitle = new Label("Equipamento e Supremos");
		lblEquipTitle.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold; -fx-font-size: 14px;");
		grid.add(lblEquipTitle, 0, row++, 2, 1);

		ComboBox<String> cbArma = criarComboArmas();
		grid.add(new Label("Arma Equipada:"), 0, row);
		grid.add(cbArma, 1, row++);

		ComboBox<String> cbArmadura = criarComboItemPorClasse("Armadura", "(Nenhuma)");
		grid.add(new Label("Armadura Equipada:"), 0, row);
		grid.add(cbArmadura, 1, row++);

		ComboBox<String> cbAmuleto1 = criarComboItemPorClasse("Amuleto", "(Nenhum)");
		grid.add(new Label("Amuleto 1:"), 0, row);
		grid.add(cbAmuleto1, 1, row++);

		ComboBox<String> cbAmuleto2 = criarComboItemPorClasse("Amuleto", "(Nenhum)");
		grid.add(new Label("Amuleto 2:"), 0, row);
		grid.add(cbAmuleto2, 1, row++);

		ComboBox<String> cbFantasma = criarComboFantasmas();
		grid.add(new Label("Fantasma Nobre:"), 0, row);
		grid.add(cbFantasma, 1, row++);

		grid.add(new Label("Modo de Entrada:"), 0, row);
		HBox radioBox = new HBox(15);
		ToggleGroup tgGroup = new ToggleGroup();
		RadioButton rbReforco = new RadioButton("Salvar como Reforço");
		rbReforco.setToggleGroup(tgGroup);
		rbReforco.setSelected(true);
		rbReforco.setStyle("-fx-text-fill: white;");
		RadioButton rbPrincipal = new RadioButton("Salvar e ir ao Combate");
		rbPrincipal.setToggleGroup(tgGroup);
		rbPrincipal.setStyle("-fx-text-fill: white;");
		radioBox.getChildren().addAll(rbReforco, rbPrincipal);
		grid.add(radioBox, 1, row++);

		Button btnSalvarPersonagem = new Button("Salvar e Registrar Personagem");
		btnSalvarPersonagem.setStyle(
				"-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 14px;");
		grid.add(btnSalvarPersonagem, 1, row++);

		btnSalvarPersonagem.setOnAction(e -> {
			String nome = tfNome.getText().trim();
			if (nome.isEmpty()) {
				mostrarErroNomeVazio();
				return;
			}

			Personagem personagem = criarPersonagem(nome, cbRaca.getValue(), cbClasse.getValue(), spNivel.getValue(),
					spVida.getValue(), spIniciativa.getValue(), spAtributos);
			aplicarEquipamentos(personagem, cbArma.getValue(), cbArmadura.getValue(), cbAmuleto1.getValue(),
					cbAmuleto2.getValue(), cbFantasma.getValue());
			salvarPersonagem.accept(personagem);

			if (rbPrincipal.isSelected()) {
				adicionarAoCombate(personagem);
			}

			resetarFormulario(tfNome, cbRaca, cbClasse, spNivel, spVida, spIniciativa, spAtributos, cbArma, cbArmadura,
					cbAmuleto1, cbAmuleto2, cbFantasma, rbReforco);
			mostrarConfirmacaoCriacao(nome);
			refreshLists.run();
			tabPane.getSelectionModel().select(0);
		});

		return grid;
	}

	private void atualizarListas(ListView<Personagem> lvReforcos, ListView<Personagem> lvAtivos) {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}

		List<Personagem> inativos = new ArrayList<>();
		for (String arquivo : listarArquivosPersonagens.get()) {
			boolean ativo = estado.getCombatentes().stream()
					.anyMatch(p -> p.getNome().equalsIgnoreCase(arquivo)
							|| (p.getJsonFileName() != null
									&& p.getJsonFileName().replace(".json", "").equalsIgnoreCase(arquivo)));
			if (!ativo) {
				Personagem personagem = carregarPersonagem.apply(arquivo);
				if (personagem != null) {
					inativos.add(personagem);
				}
			}
		}
		lvReforcos.getItems().setAll(inativos);

		List<Personagem> ativos = estado.getCombatentes().stream()
				.filter(p -> p != null && "JOGADOR".equals(p.getFaccao()))
				.collect(Collectors.toList());
		lvAtivos.getItems().setAll(ativos);
	}

	private void adicionarAoCombate(Personagem personagem) {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}
		personagem.setFaccao("JOGADOR");
		personagem.recalcularAtributosEstatisticas();
		personagem.setVidaAtual(personagem.getVidaMaxima());
		personagem.setManaAtual(personagem.getManaMaxima());
		posicionarNaEntrada(personagem);
		estado.getCombatentes().add(personagem);
		atualizarInterfaceRoster.run();
	}

	private void posicionarNaEntrada(Personagem personagem) {
		MapController mapa = controller.getPrimaryMap();
		if (mapa != null) {
			javafx.util.Pair<Integer, Integer> pos = mapa.encontrarCelulaLivreMaisProxima(5, 5);
			if (pos != null) {
				personagem.setPosX(pos.getKey());
				personagem.setPosY(pos.getValue());
				return;
			}
		}
		personagem.setPosX(5);
		personagem.setPosY(5);
	}

	private boolean confirmarExclusao(Personagem personagem) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Excluir Personagem");
		alert.setHeaderText("Tem certeza que deseja deletar " + personagem.getNome() + "?");
		alert.setContentText("Esta ação irá deletar permanentemente o arquivo JSON do personagem.");
		alert.getDialogPane().setStyle("-fx-background-color: #222;");
		alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");

		Optional<ButtonType> res = alert.showAndWait();
		return res.isPresent() && res.get() == ButtonType.OK;
	}

	private void excluirArquivosPersonagem(Personagem personagem) {
		String projectPath = System.getProperty("user.dir");
		String[] caminhos = {
				"/src/main/resources/data/players/",
				"/resources/data/players/",
				"/src/data/players/"
		};
		String nomeArquivo = personagem.getJsonFileName();
		if (nomeArquivo == null) {
			nomeArquivo = personagem.getNome().toLowerCase() + ".json";
		}
		for (String caminho : caminhos) {
			File file = new File(projectPath + caminho + nomeArquivo);
			if (file.exists()) {
				file.delete();
			}
		}

		URL urlBin = controller.getClass().getResource("/data/players/" + nomeArquivo);
		if (urlBin != null) {
			try {
				File fileBin = new File(urlBin.toURI());
				if (fileBin.exists()) {
					fileBin.delete();
				}
			} catch (Exception ex) {
			}
		}
	}

	private ComboBox<String> criarComboArmas() {
		ComboBox<String> cbArma = new ComboBox<>();
		cbArma.getItems().add("(Nenhuma)");
		Map<String, Map<String, Object>> armoryDatabase = armorySupplier.get();
		if (armoryDatabase != null) {
			cbArma.getItems().addAll(armoryDatabase.keySet());
		}
		cbArma.setValue("(Nenhuma)");
		return cbArma;
	}

	private ComboBox<String> criarComboItemPorClasse(String classeItem, String valorVazio) {
		ComboBox<String> combo = new ComboBox<>();
		combo.getItems().add(valorVazio);
		Map<String, Map<String, Object>> itempediaDatabase = itempediaSupplier.get();
		if (itempediaDatabase != null) {
			for (Map.Entry<String, Map<String, Object>> entry : itempediaDatabase.entrySet()) {
				String classe = (String) entry.getValue().get("classeItem");
				if (classeItem.equalsIgnoreCase(classe)) {
					combo.getItems().add(entry.getKey());
				}
			}
		}
		combo.setValue(valorVazio);
		return combo;
	}

	private ComboBox<String> criarComboFantasmas() {
		ComboBox<String> cbFantasma = new ComboBox<>();
		cbFantasma.getItems().add("(Nenhum)");
		cbFantasma.getItems().addAll("AcertoDeContas", "AndJusticeForMySelf", "ApostadorIncansavel", "GodsWill",
				"InvocacaoMurasame", "InvocacaoSangrenta", "IraDeAnthyros", "JihoGekkyuden", "LuaSombria",
				"ModoPolaris", "ProfetaDeBehemoth", "RevelacaoDeYaweh", "RingOfTheUndyingWill", "Ritual", "TheMastersCall",
				"VigiliaEterna");
		cbFantasma.setValue("(Nenhum)");
		return cbFantasma;
	}

	private Personagem criarPersonagem(String nome, String racaNome, String classeNome, int nivel, int vidaMaxBase,
			int iniciativaBase, Map<Atributo, Spinner<Integer>> spAtributos) {
		Map<Atributo, Integer> atrBase = new HashMap<>();
		for (Atributo atr : Atributo.values()) {
			atrBase.put(atr, spAtributos.get(atr).getValue());
		}

		Raça raca = mapearRaca.apply(racaNome);
		Classe classe = mapearClasse.apply(classeNome);
		Personagem personagem = new Personagem(nome, raca, classe, nivel, atrBase, vidaMaxBase, iniciativaBase);
		personagem.setFaccao("JOGADOR");
		personagem.setJsonFileName(nome.toLowerCase() + ".json");
		return personagem;
	}

	private void aplicarEquipamentos(Personagem personagem, String armaVal, String armaduraVal, String amuleto1Val,
			String amuleto2Val, String fnVal) {
		if (armaVal != null && !armaVal.equals("(Nenhuma)")) {
			personagem.setArmaEquipada(buscarArma.apply(armaVal));
		}
		if (armaduraVal != null && !armaduraVal.equals("(Nenhuma)")) {
			Item item = buscarItem.apply(armaduraVal);
			if (item instanceof Armadura) {
				personagem.setArmaduraEquipada((Armadura) item);
			}
		}
		if (amuleto1Val != null && !amuleto1Val.equals("(Nenhum)")) {
			Item item = buscarItem.apply(amuleto1Val);
			if (item instanceof Amuleto) {
				personagem.setAmuleto1((Amuleto) item);
			}
		}
		if (amuleto2Val != null && !amuleto2Val.equals("(Nenhum)")) {
			Item item = buscarItem.apply(amuleto2Val);
			if (item instanceof Amuleto) {
				personagem.setAmuleto2((Amuleto) item);
			}
		}
		if (fnVal != null && !fnVal.equals("(Nenhum)")) {
			personagem.setFantasmaNobre(instanciarFantasmaNobre.apply(fnVal));
		}

		personagem.recalcularAtributosEstatisticas();
		personagem.setVidaAtual(personagem.getVidaMaxima());
		personagem.setManaAtual(personagem.getManaMaxima());
	}

	private void mostrarErroNomeVazio() {
		Alert alert = new Alert(Alert.AlertType.ERROR, "O nome do personagem não pode estar vazio!");
		alert.getDialogPane().setStyle("-fx-background-color: #222;");
		alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
		alert.showAndWait();
	}

	private void mostrarConfirmacaoCriacao(String nome) {
		Alert confirmAlert = new Alert(Alert.AlertType.INFORMATION,
				"Personagem " + nome + " criado e salvo com sucesso!");
		confirmAlert.getDialogPane().setStyle("-fx-background-color: #222;");
		confirmAlert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
		confirmAlert.showAndWait();
	}

	private void resetarFormulario(TextField tfNome, ComboBox<String> cbRaca, ComboBox<String> cbClasse,
			Spinner<Integer> spNivel, Spinner<Integer> spVida, Spinner<Integer> spIniciativa,
			Map<Atributo, Spinner<Integer>> spAtributos, ComboBox<String> cbArma, ComboBox<String> cbArmadura,
			ComboBox<String> cbAmuleto1, ComboBox<String> cbAmuleto2, ComboBox<String> cbFantasma,
			RadioButton rbReforco) {
		tfNome.clear();
		cbRaca.setValue("Humano");
		cbClasse.setValue("Pugilista");
		spNivel.getValueFactory().setValue(1);
		spVida.getValueFactory().setValue(100);
		spIniciativa.getValueFactory().setValue(0);
		for (Atributo atr : Atributo.values()) {
			spAtributos.get(atr).getValueFactory().setValue(10);
		}
		cbArma.setValue("(Nenhuma)");
		cbArmadura.setValue("(Nenhuma)");
		cbAmuleto1.setValue("(Nenhum)");
		cbAmuleto2.setValue("(Nenhum)");
		cbFantasma.setValue("(Nenhum)");
		rbReforco.setSelected(true);
	}

	private void atualizarListaPresets(ListView<String> lvPresets) {
		PartyPresetsData data = PartyPresetsData.carregarPresets();
		List<String> items = new ArrayList<>();
		if (data != null && data.getPresets() != null) {
			for (PartyPreset p : data.getPresets()) {
				String prefix = "";
				if (p.getName().equalsIgnoreCase(data.getEquippedSlotName())) {
					prefix = "[Equipado] ";
				}
				String members = String.join(", ", p.getCharacterNames());
				items.add(prefix + p.getName() + " (" + members + ")");
			}
		}
		lvPresets.getItems().setAll(items);
	}

	private Tab criarAbaPresets(ListView<String> lvPresets, ListView<Personagem> lvAtivos,
			Runnable refreshLists, Runnable refreshPresetsList) {
		Tab tabPresets = new Tab("Presets de Equipe");
		tabPresets.setClosable(false);

		HBox mainLayout = new HBox(20);
		mainLayout.setPadding(new Insets(15));
		mainLayout.setStyle("-fx-background-color: #121218;");

		VBox leftPanel = new VBox(10);
		HBox.setHgrow(leftPanel, javafx.scene.layout.Priority.ALWAYS);
		Label lblPresets = new Label("Slots de Equipe Salvos");
		lblPresets.setStyle("-fx-text-fill: #e94560; -fx-font-size: 16px; -fx-font-weight: bold;");

		VBox.setVgrow(lvPresets, javafx.scene.layout.Priority.ALWAYS);

		HBox leftButtons = new HBox(10);
		Button btnEquipar = new Button("Equipar Equipe");
		btnEquipar.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
		Button btnExcluirPreset = new Button("Excluir Slot");
		btnExcluirPreset.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
		leftButtons.getChildren().addAll(btnEquipar, btnExcluirPreset);
		leftPanel.getChildren().addAll(lblPresets, lvPresets, leftButtons);

		VBox rightPanel = new VBox(15);
		rightPanel.setMinWidth(300);
		rightPanel.setPadding(new Insets(0, 0, 0, 10));

		Label lblSalvar = new Label("Salvar Equipe Atual");
		lblSalvar.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

		Label lblInfo = new Label("A equipe ativa no combate atual será salva neste slot.");
		lblInfo.setStyle("-fx-text-fill: #808090; -fx-font-size: 11px;");
		lblInfo.setWrapText(true);

		TextField tfPresetName = new TextField();
		tfPresetName.setPromptText("Nome da Nova Equipe...");
		tfPresetName.setStyle("-fx-background-color: #16161e; -fx-text-fill: white; -fx-border-color: #808090;");

		Button btnSalvarPreset = new Button("Salvar Novo Preset");
		btnSalvarPreset.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #e94560; -fx-font-weight: bold;");
		btnSalvarPreset.setMaxWidth(Double.MAX_VALUE);

		rightPanel.getChildren().addAll(lblSalvar, lblInfo, tfPresetName, btnSalvarPreset);

		mainLayout.getChildren().addAll(leftPanel, rightPanel);
		tabPresets.setContent(mainLayout);

		btnSalvarPreset.setOnAction(e -> {
			String name = tfPresetName.getText().trim();
			if (name.isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.ERROR, "O nome do preset não pode estar vazio!");
				alert.getDialogPane().setStyle("-fx-background-color: #222;");
				alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
				alert.showAndWait();
				return;
			}

			EstadoCombate estado = estadoSupplier.get();
			if (estado == null) return;

			List<String> activeCharNames = estado.getCombatentes().stream()
					.filter(p -> p != null && "JOGADOR".equals(p.getFaccao()))
					.map(p -> p.getJsonFileName() != null ? p.getJsonFileName().replace(".json", "") : p.getNome().toLowerCase())
					.collect(Collectors.toList());

			if (activeCharNames.isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.WARNING, "Não há personagens da facção JOGADOR ativos no combate no momento!");
				alert.getDialogPane().setStyle("-fx-background-color: #222;");
				alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
				alert.showAndWait();
				return;
			}

			PartyPresetsData data = PartyPresetsData.carregarPresets();
			data.getPresets().removeIf(p -> p.getName().equalsIgnoreCase(name));
			data.getPresets().add(new PartyPreset(name, activeCharNames));
			if (data.getEquippedSlotName() == null || data.getEquippedSlotName().isEmpty()) {
				data.setEquippedSlotName(name);
			}
			PartyPresetsData.salvarPresets(data);

			tfPresetName.clear();
			refreshPresetsList.run();

			Alert alert = new Alert(Alert.AlertType.INFORMATION, "Preset '" + name + "' salvo com sucesso!");
			alert.getDialogPane().setStyle("-fx-background-color: #222;");
			alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
			alert.showAndWait();
		});

		btnEquipar.setOnAction(e -> {
			String selected = lvPresets.getSelectionModel().getSelectedItem();
			if (selected == null) return;

			String presetName = selected;
			if (presetName.startsWith("[Equipado] ")) {
				presetName = presetName.substring("[Equipado] ".length());
			}
			int parenIndex = presetName.indexOf(" (");
			if (parenIndex != -1) {
				presetName = presetName.substring(0, parenIndex);
			}
			presetName = presetName.trim();

			PartyPresetsData data = PartyPresetsData.carregarPresets();
			final String finalPresetName = presetName;
			PartyPreset preset = data.getPresets().stream()
					.filter(p -> p.getName().equalsIgnoreCase(finalPresetName))
					.findFirst().orElse(null);

			if (preset == null) return;

			EstadoCombate estado = estadoSupplier.get();
			if (estado == null) return;

			estado.getCombatentes().removeIf(p -> p != null && "JOGADOR".equals(p.getFaccao()));

			int count = 0;
			for (String charName : preset.getCharacterNames()) {
				Personagem p = carregarPersonagem.apply(charName);
				if (p != null) {
					p.setFaccao("JOGADOR");
					p.recalcularAtributosEstatisticas();
					p.setVidaAtual(p.getVidaMaxima());
					p.setManaAtual(p.getManaMaxima());
					posicionarNaEntrada(p);
					estado.getCombatentes().add(p);
					count++;
				}
			}

			data.setEquippedSlotName(preset.getName());
			PartyPresetsData.salvarPresets(data);

			refreshLists.run();
			refreshPresetsList.run();
			atualizarInterfaceRoster.run();

			Alert alert = new Alert(Alert.AlertType.INFORMATION, "Equipe '" + preset.getName() + "' equipada com sucesso! (" + count + " personagens ativos)");
			alert.getDialogPane().setStyle("-fx-background-color: #222;");
			alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
			alert.showAndWait();
		});

		btnExcluirPreset.setOnAction(e -> {
			String selected = lvPresets.getSelectionModel().getSelectedItem();
			if (selected == null) return;

			String presetName = selected;
			if (presetName.startsWith("[Equipado] ")) {
				presetName = presetName.substring("[Equipado] ".length());
			}
			int parenIndex = presetName.indexOf(" (");
			if (parenIndex != -1) {
				presetName = presetName.substring(0, parenIndex);
			}
			presetName = presetName.trim();

			Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja excluir o preset '" + presetName + "'?");
			confirm.getDialogPane().setStyle("-fx-background-color: #222;");
			confirm.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
			Optional<ButtonType> result = confirm.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.OK) {
				PartyPresetsData data = PartyPresetsData.carregarPresets();
				final String finalPresetName = presetName;
				data.getPresets().removeIf(p -> p.getName().equalsIgnoreCase(finalPresetName));
				if (presetName.equalsIgnoreCase(data.getEquippedSlotName())) {
					data.setEquippedSlotName(null);
				}
				PartyPresetsData.salvarPresets(data);

				refreshPresetsList.run();
			}
		});

		return tabPresets;
	}
}
