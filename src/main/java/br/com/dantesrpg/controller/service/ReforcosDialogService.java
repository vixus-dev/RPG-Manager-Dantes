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

import br.com.dantesrpg.model.enums.PesoEntidade;
import javafx.scene.control.Tooltip;
import javafx.scene.control.SelectionMode;
import br.com.dantesrpg.model.util.PartyPreset;
import br.com.dantesrpg.model.util.PartyPresetsData;
import br.com.dantesrpg.model.util.ArmaduraUtils;

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
	private final Supplier<Map<String, Map<String, Object>>> bestiarioSupplier;
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
			Supplier<Map<String, Map<String, Object>>> bestiarioSupplier,
			Supplier<List<String>> listarArquivosPersonagens, Function<String, Personagem> carregarPersonagem,
			Function<String, Raça> mapearRaca, Function<String, Classe> mapearClasse,
			Function<String, FantasmaNobre> instanciarFantasmaNobre, Function<String, Arma> buscarArma,
			Function<String, Item> buscarItem, Consumer<Personagem> salvarPersonagem,
			Runnable atualizarInterfaceRoster) {
		this.controller = controller;
		this.estadoSupplier = estadoSupplier;
		this.armorySupplier = armorySupplier;
		this.itempediaSupplier = itempediaSupplier;
		this.bestiarioSupplier = bestiarioSupplier;
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
		stage.initModality(Modality.NONE);
		stage.setResizable(true);
		stage.setMinWidth(1200);
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

		Scene scene = new Scene(root, 1200, 700);
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
				
				javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
				imgView.setFitWidth(32);
				imgView.setFitHeight(32);
				String tokenName = personagem.getNome().toLowerCase().replace(" ", "_") + ".png";
				try {
					javafx.scene.image.Image img = br.com.dantesrpg.model.util.ImageCache.get("/tokens/" + tokenName, 32, 32);
					if (img != null && !img.isError()) imgView.setImage(img);
				} catch (Exception e) {}

				Label lblName = new Label(personagem.getNome());
				lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
				
				cell.getChildren().addAll(imgView, lblName);
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

		// Panel 3: Bestiario
		VBox panelBestiario = new VBox(10);
		HBox.setHgrow(panelBestiario, javafx.scene.layout.Priority.ALWAYS);
		Label lblBestiario = new Label("Bestiário");
		lblBestiario.setStyle("-fx-text-fill: #8a2be2; -fx-font-size: 16px; -fx-font-weight: bold;");
		ListView<String> lvBestiario = new ListView<>();
		lvBestiario.setStyle("-fx-background-color: #16161e; -fx-control-inner-background: #16161e;");
		VBox.setVgrow(lvBestiario, javafx.scene.layout.Priority.ALWAYS);
		Map<String, Map<String, Object>> bestiarioMap = bestiarioSupplier != null ? bestiarioSupplier.get() : null;
		if (bestiarioMap != null) {
			lvBestiario.getItems().addAll(bestiarioMap.keySet().stream().sorted().collect(Collectors.toList()));
		}
		
		panelBestiario.getChildren().addAll(lblBestiario, lvBestiario);

		// Panel 4: Editor Enemy (Hidden by default)
		VBox panelEditEnemy = new VBox(10);
		HBox.setHgrow(panelEditEnemy, javafx.scene.layout.Priority.ALWAYS);
		panelEditEnemy.setVisible(false);
		panelEditEnemy.setManaged(false);
		panelEditEnemy.setStyle("-fx-background-color: #1a1a24; -fx-padding: 10; -fx-border-color: #8a2be2; -fx-border-width: 2;");

		ScrollPane scrollEdit = new ScrollPane();
		scrollEdit.setFitToWidth(true);
		scrollEdit.setStyle("-fx-background: #1a1a24; -fx-background-color: #1a1a24;");
		VBox.setVgrow(scrollEdit, javafx.scene.layout.Priority.ALWAYS);

		GridPane gridEdit = new GridPane();
		gridEdit.setHgap(10);
		gridEdit.setVgap(10);
		gridEdit.setStyle("-fx-background-color: #1a1a24;");

		Label lblEditTitle = new Label("Adicionar como Aliado");
		lblEditTitle.setStyle("-fx-text-fill: #8a2be2; -fx-font-size: 16px; -fx-font-weight: bold;");

		TextField tfName = new TextField();
		tfName.setStyle("-fx-text-fill: white; -fx-background-color: #222;");
		TextField tfRace = new TextField();
		tfRace.setStyle("-fx-text-fill: white; -fx-background-color: #222;");
		Spinner<Integer> spGrau = new Spinner<>(0, 10, 1);
		spGrau.setEditable(true);
		Spinner<Integer> spHP = new Spinner<>(1, 100000, 100);
		spHP.setEditable(true);
		Spinner<Integer> spMana = new Spinner<>(0, 10000, 10);
		spMana.setEditable(true);
		Spinner<Integer> spAgi = new Spinner<>(0, 1000, 10);
		spAgi.setEditable(true);
		Spinner<Integer> spDef = new Spinner<>(0, 90, 0);
		spDef.setEditable(true);
		
		ComboBox<String> cbArmaEnemy = new ComboBox<>();
		cbArmaEnemy.getItems().add("(Nenhuma)");
		if (armorySupplier != null && armorySupplier.get() != null) {
			cbArmaEnemy.getItems().addAll(armorySupplier.get().keySet());
		}

		ComboBox<PesoEntidade> cbPeso = new ComboBox<>();
		cbPeso.getItems().addAll(PesoEntidade.values());

		// Properties lists
		ListView<String> lvDisponiveis = new ListView<>();
		ListView<String> lvSelecionadas = new ListView<>();
		lvDisponiveis.setPrefHeight(100);
		lvSelecionadas.setPrefHeight(100);
		lvDisponiveis.setStyle("-fx-background-color: #222; -fx-control-inner-background: #222;");
		lvSelecionadas.setStyle("-fx-background-color: #222; -fx-control-inner-background: #222;");
		
		// Fill disponiveis
		String[] propsPossiveis = {"IMUNIDADE_CONTROLE", "IMUNIDADE_DOT", "EXPLODIR", "VAMPIRISMO", "BLINDADO", "REGENERACAO", "IMUNE_KNOCKBACK"};
		lvDisponiveis.getItems().addAll(propsPossiveis);

		// Tooltips via CellFactory
		javafx.util.Callback<ListView<String>, ListCell<String>> cellFactory = lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setTooltip(null);
				} else {
					setText(item);
					setStyle("-fx-text-fill: white;");
					Tooltip tt = new Tooltip(getDescricaoPropriedade(item));
					tt.setStyle("-fx-font-size: 12px;");
					setTooltip(tt);
				}
			}
		};
		lvDisponiveis.setCellFactory(cellFactory);
		lvSelecionadas.setCellFactory(cellFactory);

		// Double click to move
		lvDisponiveis.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				String sel = lvDisponiveis.getSelectionModel().getSelectedItem();
				if (sel != null && !lvSelecionadas.getItems().contains(sel)) {
					lvSelecionadas.getItems().add(sel);
					lvDisponiveis.getItems().remove(sel);
				}
			}
		});
		lvSelecionadas.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				String sel = lvSelecionadas.getSelectionModel().getSelectedItem();
				if (sel != null) {
					lvDisponiveis.getItems().add(sel);
					lvSelecionadas.getItems().remove(sel);
				}
			}
		});
		
		javafx.scene.image.ImageView imgTokenEnemy = new javafx.scene.image.ImageView();
		imgTokenEnemy.setFitWidth(128);
		imgTokenEnemy.setFitHeight(128);

		int r = 0;
		Label lblN = new Label("Nome:"); lblN.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblN, 0, r); gridEdit.add(tfName, 1, r++, 2, 1);
		
		Label lblR = new Label("Raça:"); lblR.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblR, 0, r); gridEdit.add(tfRace, 1, r++, 2, 1);
		
		// Imagem nas linhas dos Spinners (ocupa 5 linhas, na coluna 2)
		gridEdit.add(imgTokenEnemy, 2, r, 1, 5);
		GridPane.setMargin(imgTokenEnemy, new Insets(0, 0, 0, 10));

		Label lblG = new Label("Grau:"); lblG.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblG, 0, r); gridEdit.add(spGrau, 1, r++);
		
		Label lblH = new Label("HP:"); lblH.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblH, 0, r); gridEdit.add(spHP, 1, r++);
		
		Label lblM = new Label("Mana:"); lblM.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblM, 0, r); gridEdit.add(spMana, 1, r++);
		
		Label lblAg = new Label("Agilidade:"); lblAg.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblAg, 0, r); gridEdit.add(spAgi, 1, r++);
		
		Label lblDf = new Label("Resistência (%):"); lblDf.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblDf, 0, r); gridEdit.add(spDef, 1, r++);
		
		Label lblA = new Label("Arma:"); lblA.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblA, 0, r); gridEdit.add(cbArmaEnemy, 1, r++, 2, 1);
		
		Label lblP = new Label("Peso:"); lblP.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblP, 0, r); gridEdit.add(cbPeso, 1, r++, 2, 1);
		
		Label lblPD = new Label("Props Disp.:"); lblPD.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblPD, 0, r); gridEdit.add(lvDisponiveis, 1, r++, 2, 1);
		
		Label lblPS = new Label("Props Selec.:"); lblPS.setStyle("-fx-text-fill: white;");
		gridEdit.add(lblPS, 0, r); gridEdit.add(lvSelecionadas, 1, r++, 2, 1);
		
		Button btnAddInimigoAliado = new Button("Adicionar Inimigo como Aliado");
		btnAddInimigoAliado.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
		gridEdit.add(btnAddInimigoAliado, 0, r, 3, 1);

		scrollEdit.setContent(gridEdit);
		panelEditEnemy.getChildren().addAll(lblEditTitle, scrollEdit);

		// Event when clicking bestiary
		lvBestiario.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && bestiarioMap != null) {
				Map<String, Object> data = bestiarioMap.get(newVal);
				if (data != null) {
					panelEditEnemy.setVisible(true);
					panelEditEnemy.setManaged(true);
					
					tfName.setText((String) data.getOrDefault("nome", ""));
					tfRace.setText((String) data.getOrDefault("raca", ""));
					spGrau.getValueFactory().setValue(((Double) data.getOrDefault("grau", 0.0)).intValue());
					spHP.getValueFactory().setValue(((Double) data.getOrDefault("vida", 10.0)).intValue());
					spMana.getValueFactory().setValue(((Double) data.getOrDefault("mana", 0.0)).intValue());
					spAgi.getValueFactory().setValue(((Double) data.getOrDefault("agilidade", 10.0)).intValue());
					spDef.getValueFactory().setValue(((Double) data.getOrDefault("defesa", 0.0)).intValue());
					
					String armaStr = (String) data.getOrDefault("arma", "");
					if (armaStr != null && cbArmaEnemy.getItems().contains(armaStr)) {
						cbArmaEnemy.setValue(armaStr);
					} else {
						cbArmaEnemy.setValue("(Nenhuma)");
					}

					String pesoStr = (String) data.getOrDefault("peso", "");
					cbPeso.setValue(PesoEntidade.fromJsonId(pesoStr));
					
					String tokenName = tfName.getText().toLowerCase().replace(" ", "_") + ".png";
					try {
						javafx.scene.image.Image img = br.com.dantesrpg.model.util.ImageCache.get("/tokens/" + tokenName, 128, 128);
						if (img != null && !img.isError()) {
							imgTokenEnemy.setImage(img);
						} else {
							imgTokenEnemy.setImage(null);
						}
					} catch (Exception ex) {
						imgTokenEnemy.setImage(null);
					}

					// reset props
					lvDisponiveis.getItems().clear();
					lvDisponiveis.getItems().addAll(propsPossiveis);
					lvSelecionadas.getItems().clear();
					
					Object propsObj = data.get("propriedades");
					if (propsObj instanceof List) {
						List<String> props = (List<String>) propsObj;
						for (String p : props) {
							if (lvDisponiveis.getItems().contains(p)) {
								lvDisponiveis.getItems().remove(p);
								lvSelecionadas.getItems().add(p);
							} else {
								lvSelecionadas.getItems().add(p);
							}
						}
					}
				}
			}
		});

		btnAddInimigoAliado.setOnAction(e -> {
			String nameBase = tfName.getText().trim();
			if (nameBase.isEmpty()) return;
			
			String nameAliado = nameBase;
			boolean exists = false;
			if (estadoSupplier.get() != null) {
				exists = estadoSupplier.get().getCombatentes().stream()
					.anyMatch(p -> p.getNome().equalsIgnoreCase(nameBase));
			}
			if (exists) {
				nameAliado = nameBase + "_aliado";
			}
			String nomeBaseImagem = nameBase;
			String idBestiarioSelecionado = lvBestiario.getSelectionModel().getSelectedItem();
			if (idBestiarioSelecionado != null && bestiarioMap != null) {
				Map<String, Object> data = bestiarioMap.get(idBestiarioSelecionado);
				if (data != null) {
					nomeBaseImagem = (String) data.getOrDefault("nomeBaseImagem", data.getOrDefault("nome", nameBase));
				}
			}
			
			Personagem aliado = criarInimigoAliado(nameAliado, tfRace.getText(), spGrau.getValue(), 
				spHP.getValue(), spMana.getValue(), cbArmaEnemy.getValue(), cbPeso.getValue(), 
				new ArrayList<>(lvSelecionadas.getItems()), spAgi.getValue(), spDef.getValue(), nomeBaseImagem);
			
			adicionarAoCombate(aliado);
			refreshLists.run();
		});

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

		gerenciarLayout.getChildren().addAll(panelReforcos, panelAtivos, panelBestiario, panelEditEnemy);
		tabGerenciar.setContent(gerenciarLayout);
		return tabGerenciar;
	}

	private String getDescricaoPropriedade(String prop) {
		switch (prop) {
			case "IMUNIDADE_CONTROLE": return "Imunidade a Stun, Lento, e Sono.";
			case "IMUNIDADE_DOT": return "Imunidade a DoTs (Veneno, Sangramento, etc).";
			case "EXPLODIR": return "Causa dano em área ao morrer.";
			case "VAMPIRISMO": return "Recupera HP ao causar dano.";
			case "BLINDADO": return "Reduz dano físico recebido.";
			case "REGENERACAO": return "Regenera HP todo turno.";
			case "IMUNE_KNOCKBACK": return "Não pode ser empurrado.";
			default: return "Propriedade especial.";
		}
	}

	private Personagem criarInimigoAliado(String nome, String racaStr, int grau, int vida, int mana, String armaStr, PesoEntidade peso, List<String> propriedades, int agilidade, int defesa, String nomeBaseImagem) {
		Raça racaObj = mapearRaca.apply(racaStr); 
		Classe classeObj = mapearClasse.apply("Placeholder");
		
		Map<Atributo, Integer> atrBase = new HashMap<>();
		for (Atributo atr : Atributo.values()) {
			atrBase.put(atr, 10);
		}
		atrBase.put(Atributo.DESTREZA, agilidade);
		atrBase.put(Atributo.TOPOR, 1);
		
		Personagem p = new Personagem(nome, racaObj, classeObj, 1, atrBase, vida, 0);
		p.setArmaduraNatural(ArmaduraUtils.calcularPontosParaReducaoPercentual(defesa));
		p.setGrau(grau);
		p.setFaccao("JOGADOR");
		p.setNomeBaseImagem(nomeBaseImagem);
		p.setPesoEntidade(peso);
		p.getPropriedades().addAll(propriedades);
		
		if (armaStr != null && !armaStr.equals("(Nenhuma)")) {
			p.setArmaEquipada(buscarArma.apply(armaStr));
		}
		
		p.recalcularAtributosEstatisticas();
		p.setVidaAtual(vida);
		p.setManaMaxima(mana);
		p.setManaAtual(mana);
		
		br.com.dantesrpg.controller.service.BestiarioSpawnService.aplicarFantasmaNobreMonstro(nome, p);
		
		return p;
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
				"Fimbulwinter", "InvocacaoMurasame", "InvocacaoSangrenta", "IraDeAnthyros", "JihoGekkyuden", "LuaSombria",
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

	private String extrairNomePreset(String selectedItem) {
		if (selectedItem == null) return null;
		String presetName = selectedItem;
		if (presetName.startsWith("[Equipado] ")) {
			presetName = presetName.substring("[Equipado] ".length());
		}
		int parenIndex = presetName.indexOf(" (");
		if (parenIndex != -1) {
			presetName = presetName.substring(0, parenIndex);
		}
		return presetName.trim();
	}

	private void atualizarComboDisponiveis(ComboBox<String> cbDisponiveis, PartyPreset preset) {
		cbDisponiveis.getItems().clear();
		List<String> todos = listarArquivosPersonagens.get();
		if (todos != null) {
			for (String nome : todos) {
				if (preset == null || !preset.getCharacterNames().contains(nome)) {
					cbDisponiveis.getItems().add(nome);
				}
			}
		}
		if (!cbDisponiveis.getItems().isEmpty()) {
			cbDisponiveis.setValue(cbDisponiveis.getItems().get(0));
		}
	}

	private void exibirDetalhesPreset(PartyPreset preset, TextField tfEditPresetName, ListView<String> lvMembros,
			ComboBox<String> cbDisponiveis, VBox centerPanel, VBox rightPanelMemberSection) {
		if (preset == null) {
			tfEditPresetName.clear();
			lvMembros.getItems().clear();
			cbDisponiveis.getItems().clear();
			centerPanel.setDisable(true);
			rightPanelMemberSection.setDisable(true);
			return;
		}

		centerPanel.setDisable(false);
		rightPanelMemberSection.setDisable(false);
		tfEditPresetName.setText(preset.getName());
		lvMembros.getItems().setAll(preset.getCharacterNames());
		atualizarComboDisponiveis(cbDisponiveis, preset);
	}

	private void moverPreset(int direcao, ListView<String> lvPresets, Runnable refreshPresetsList) {
		int selectedIndex = lvPresets.getSelectionModel().getSelectedIndex();
		if (selectedIndex == -1) return;

		PartyPresetsData data = PartyPresetsData.carregarPresets();
		List<PartyPreset> list = data.getPresets();
		int newIndex = selectedIndex + direcao;
		if (newIndex >= 0 && newIndex < list.size()) {
			PartyPreset temp = list.get(selectedIndex);
			list.set(selectedIndex, list.get(newIndex));
			list.set(newIndex, temp);
			PartyPresetsData.salvarPresets(data);
			refreshPresetsList.run();
			lvPresets.getSelectionModel().select(newIndex);
		}
	}

	private void moverMembro(int direcao, String presetName, int memberIndex, ListView<String> lvPresets,
			ListView<String> lvMembros, ComboBox<String> cbDisponiveis, VBox centerPanel, VBox rightPanelMemberSection) {
		PartyPresetsData data = PartyPresetsData.carregarPresets();
		PartyPreset preset = data.getPresets().stream()
				.filter(p -> p.getName().equalsIgnoreCase(presetName))
				.findFirst().orElse(null);
		if (preset == null) return;

		List<String> characterNames = preset.getCharacterNames();
		int newIndex = memberIndex + direcao;
		if (newIndex >= 0 && newIndex < characterNames.size()) {
			String temp = characterNames.get(memberIndex);
			characterNames.set(memberIndex, characterNames.get(newIndex));
			characterNames.set(newIndex, temp);

			PartyPresetsData.salvarPresets(data);

			int currentPresetIdx = lvPresets.getSelectionModel().getSelectedIndex();
			atualizarListaPresets(lvPresets);
			lvPresets.getSelectionModel().select(currentPresetIdx);

			lvMembros.getSelectionModel().select(newIndex);
		}
	}

	private Tab criarAbaPresets(ListView<String> lvPresets, ListView<Personagem> lvAtivos,
			Runnable refreshLists, Runnable refreshPresetsList) {
		Tab tabPresets = new Tab("Presets de Equipe");
		tabPresets.setClosable(false);

		HBox mainLayout = new HBox(20);
		mainLayout.setPadding(new Insets(15));
		mainLayout.setStyle("-fx-background-color: #121218;");

		// --- PAINEL ESQUERDO: LISTA DE PRESETS ---
		VBox leftPanel = new VBox(10);
		HBox.setHgrow(leftPanel, javafx.scene.layout.Priority.ALWAYS);
		Label lblPresets = new Label("Slots de Equipe Salvos");
		lblPresets.setStyle("-fx-text-fill: #e94560; -fx-font-size: 16px; -fx-font-weight: bold;");

		VBox.setVgrow(lvPresets, javafx.scene.layout.Priority.ALWAYS);

		HBox leftButtons1 = new HBox(10);
		Button btnEquipar = new Button("Equipar Equipe");
		btnEquipar.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
		Button btnExcluirPreset = new Button("Excluir Slot");
		btnExcluirPreset.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
		leftButtons1.getChildren().addAll(btnEquipar, btnExcluirPreset);

		HBox leftButtons2 = new HBox(10);
		Button btnMoverPresetCima = new Button("Mover ↑");
		btnMoverPresetCima.setStyle("-fx-background-color: #1e1e2f; -fx-text-fill: #8a8af0; -fx-font-weight: bold;");
		Button btnMoverPresetBaixo = new Button("Mover ↓");
		btnMoverPresetBaixo.setStyle("-fx-background-color: #1e1e2f; -fx-text-fill: #8a8af0; -fx-font-weight: bold;");
		leftButtons2.getChildren().addAll(btnMoverPresetCima, btnMoverPresetBaixo);

		leftPanel.getChildren().addAll(lblPresets, lvPresets, leftButtons1, leftButtons2);

		// --- PAINEL CENTRAL: EDIÇÃO DO PRESET SELECIONADO ---
		VBox centerPanel = new VBox(10);
		HBox.setHgrow(centerPanel, javafx.scene.layout.Priority.ALWAYS);
		centerPanel.setDisable(true); // Desabilitado inicialmente

		Label lblCenterTitle = new Label("Edição da Equipe");
		lblCenterTitle.setStyle("-fx-text-fill: #e94560; -fx-font-size: 16px; -fx-font-weight: bold;");

		Label lblNameLabel = new Label("Nome da Equipe:");
		lblNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

		HBox editNameBox = new HBox(8);
		TextField tfEditPresetName = new TextField();
		tfEditPresetName.setPromptText("Renomear equipe...");
		tfEditPresetName.setStyle("-fx-background-color: #16161e; -fx-text-fill: white; -fx-border-color: #808090;");
		HBox.setHgrow(tfEditPresetName, javafx.scene.layout.Priority.ALWAYS);
		Button btnSalvarNome = new Button("Renomear");
		btnSalvarNome.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
		editNameBox.getChildren().addAll(tfEditPresetName, btnSalvarNome);

		Label lblMembros = new Label("Membros da Equipe (Ordem de Turno):");
		lblMembros.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

		ListView<String> lvMembros = new ListView<>();
		lvMembros.setStyle("-fx-background-color: #16161e; -fx-control-inner-background: #16161e;");
		VBox.setVgrow(lvMembros, javafx.scene.layout.Priority.ALWAYS);

		HBox memberButtons = new HBox(8);
		Button btnMoverMembroCima = new Button("Mover Membro ↑");
		btnMoverMembroCima.setStyle("-fx-background-color: #1e1e2f; -fx-text-fill: #8a8af0; -fx-font-weight: bold;");
		Button btnMoverMembroBaixo = new Button("Mover Membro ↓");
		btnMoverMembroBaixo.setStyle("-fx-background-color: #1e1e2f; -fx-text-fill: #8a8af0; -fx-font-weight: bold;");
		Button btnRemoverMembro = new Button("Remover");
		btnRemoverMembro.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
		memberButtons.getChildren().addAll(btnMoverMembroCima, btnMoverMembroBaixo, btnRemoverMembro);

		centerPanel.getChildren().addAll(lblCenterTitle, lblNameLabel, editNameBox, lblMembros, lvMembros, memberButtons);

		// --- PAINEL DIREITO: ADICIONAR MEMBROS / SALVAR ATUAL ---
		VBox rightPanel = new VBox(20);
		rightPanel.setMinWidth(300);
		rightPanel.setPadding(new Insets(0, 0, 0, 10));

		// Seção de Adicionar Membros (ligada à seleção)
		VBox rightPanelMemberSection = new VBox(10);
		rightPanelMemberSection.setDisable(true); // Desabilitado inicialmente
		Label lblAddMembro = new Label("Adicionar Membro à Equipe");
		lblAddMembro.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");

		ComboBox<String> cbDisponiveis = new ComboBox<>();
		cbDisponiveis.setStyle("-fx-background-color: #16161e; -fx-text-fill: white; -fx-border-color: #808090;");
		cbDisponiveis.setMaxWidth(Double.MAX_VALUE);

		Button btnAdicionarMembro = new Button("Adicionar Personagem");
		btnAdicionarMembro.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
		btnAdicionarMembro.setMaxWidth(Double.MAX_VALUE);
		rightPanelMemberSection.getChildren().addAll(lblAddMembro, cbDisponiveis, btnAdicionarMembro);

		javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
		separator.setStyle("-fx-background-color: #333;");

		// Seção de Salvar Equipe Atual (sempre habilitada)
		VBox rightPanelSaveSection = new VBox(10);
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
		rightPanelSaveSection.getChildren().addAll(lblSalvar, lblInfo, tfPresetName, btnSalvarPreset);

		rightPanel.getChildren().addAll(rightPanelMemberSection, separator, rightPanelSaveSection);

		mainLayout.getChildren().addAll(leftPanel, centerPanel, rightPanel);
		tabPresets.setContent(mainLayout);

		// --- OUVINTE DE SELEÇÃO DA LISTA DE PRESETS (SINCRONIZAÇÃO) ---
		lvPresets.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			String presetName = extrairNomePreset(newVal);
			if (presetName == null) {
				exibirDetalhesPreset(null, tfEditPresetName, lvMembros, cbDisponiveis, centerPanel, rightPanelMemberSection);
				return;
			}
			PartyPresetsData data = PartyPresetsData.carregarPresets();
			PartyPreset preset = data.getPresets().stream()
					.filter(p -> p.getName().equalsIgnoreCase(presetName))
					.findFirst().orElse(null);
			exibirDetalhesPreset(preset, tfEditPresetName, lvMembros, cbDisponiveis, centerPanel, rightPanelMemberSection);
		});

		// --- AÇÕES DOS BOTÕES ---

		btnMoverPresetCima.setOnAction(e -> moverPreset(-1, lvPresets, refreshPresetsList));
		btnMoverPresetBaixo.setOnAction(e -> moverPreset(1, lvPresets, refreshPresetsList));

		btnSalvarNome.setOnAction(e -> {
			String selected = lvPresets.getSelectionModel().getSelectedItem();
			if (selected == null) return;
			String presetName = extrairNomePreset(selected);
			String newName = tfEditPresetName.getText().trim();
			if (newName.isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.ERROR, "O nome da equipe não pode estar vazio!");
				alert.getDialogPane().setStyle("-fx-background-color: #222;");
				alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
				alert.showAndWait();
				return;
			}

			PartyPresetsData data = PartyPresetsData.carregarPresets();
			PartyPreset preset = data.getPresets().stream()
					.filter(p -> p.getName().equalsIgnoreCase(presetName))
					.findFirst().orElse(null);

			if (preset != null) {
				if (preset.getName().equalsIgnoreCase(data.getEquippedSlotName())) {
					data.setEquippedSlotName(newName);
				}
				preset.setName(newName);
				PartyPresetsData.salvarPresets(data);

				refreshPresetsList.run();
				for (int i = 0; i < lvPresets.getItems().size(); i++) {
					String item = lvPresets.getItems().get(i);
					if (extrairNomePreset(item).equalsIgnoreCase(newName)) {
						lvPresets.getSelectionModel().select(i);
						break;
					}
				}

				Alert alert = new Alert(Alert.AlertType.INFORMATION, "Equipe renomeada para '" + newName + "' com sucesso!");
				alert.getDialogPane().setStyle("-fx-background-color: #222;");
				alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
				alert.showAndWait();
			}
		});

		btnMoverMembroCima.setOnAction(e -> {
			String selectedPresetName = extrairNomePreset(lvPresets.getSelectionModel().getSelectedItem());
			if (selectedPresetName == null) return;
			int memberIndex = lvMembros.getSelectionModel().getSelectedIndex();
			if (memberIndex == -1) return;

			moverMembro(-1, selectedPresetName, memberIndex, lvPresets, lvMembros, cbDisponiveis, centerPanel, rightPanelMemberSection);
		});

		btnMoverMembroBaixo.setOnAction(e -> {
			String selectedPresetName = extrairNomePreset(lvPresets.getSelectionModel().getSelectedItem());
			if (selectedPresetName == null) return;
			int memberIndex = lvMembros.getSelectionModel().getSelectedIndex();
			if (memberIndex == -1) return;

			moverMembro(1, selectedPresetName, memberIndex, lvPresets, lvMembros, cbDisponiveis, centerPanel, rightPanelMemberSection);
		});

		btnRemoverMembro.setOnAction(e -> {
			String selectedPresetName = extrairNomePreset(lvPresets.getSelectionModel().getSelectedItem());
			if (selectedPresetName == null) return;
			String selectedMember = lvMembros.getSelectionModel().getSelectedItem();
			if (selectedMember == null) return;

			PartyPresetsData data = PartyPresetsData.carregarPresets();
			PartyPreset preset = data.getPresets().stream()
					.filter(p -> p.getName().equalsIgnoreCase(selectedPresetName))
					.findFirst().orElse(null);
			if (preset != null) {
				preset.getCharacterNames().remove(selectedMember);
				PartyPresetsData.salvarPresets(data);

				int currentPresetIdx = lvPresets.getSelectionModel().getSelectedIndex();
				atualizarListaPresets(lvPresets);
				lvPresets.getSelectionModel().select(currentPresetIdx);
			}
		});

		btnAdicionarMembro.setOnAction(e -> {
			String selectedPresetName = extrairNomePreset(lvPresets.getSelectionModel().getSelectedItem());
			if (selectedPresetName == null) return;
			String characterName = cbDisponiveis.getValue();
			if (characterName == null) return;

			PartyPresetsData data = PartyPresetsData.carregarPresets();
			PartyPreset preset = data.getPresets().stream()
					.filter(p -> p.getName().equalsIgnoreCase(selectedPresetName))
					.findFirst().orElse(null);
			if (preset != null) {
				preset.getCharacterNames().add(characterName);
				PartyPresetsData.salvarPresets(data);

				int currentPresetIdx = lvPresets.getSelectionModel().getSelectedIndex();
				atualizarListaPresets(lvPresets);
				lvPresets.getSelectionModel().select(currentPresetIdx);
			}
		});

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

			String presetName = extrairNomePreset(selected);

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

			String presetName = extrairNomePreset(selected);

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
