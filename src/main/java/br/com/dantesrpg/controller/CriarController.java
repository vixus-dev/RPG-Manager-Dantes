package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CriarController {

	// --- Informações Gerais ---
	@FXML private TextField txtNome;
	@FXML private ComboBox<String> comboCategoria;
	@FXML private TextField txtDescricao;
	@FXML private TextField txtValorMoedas;
	@FXML private ComboBox<String> comboTipoMoeda;
	@FXML private ComboBox<String> comboRaridade;
	@FXML private CheckBox chkShiny;
	@FXML private TextField txtAndar;

	// --- Abas de Categorias ---
	@FXML private VBox paneArma;
	@FXML private VBox paneArmaduraAmuleto;
	@FXML private VBox paneConsumivel;

	// --- Arma Específico ---
	@FXML private ComboBox<String> comboArmaTipo;
	@FXML private ComboBox<String> comboArmaAtributo;
	@FXML private ComboBox<String> comboArmaWielding;
	@FXML private TextField txtArmaDano;
	@FXML private TextField txtArmaTicks;
	@FXML private TextField txtArmaTU;
	@FXML private TextField txtArmaAlcance;
	@FXML private ComboBox<String> comboArmaAlvo;
	@FXML private TextField txtArmaTamanhoArea;
	@FXML private ComboBox<String> comboArmaEfeito;
	@FXML private TextField txtArmaEfeitoChance;
	@FXML private VBox boxArmaMunicao;
	@FXML private TextField txtArmaMunicao;
	@FXML private VBox boxArmaSlots;
	@FXML private TextField txtArmaSlots;
	@FXML private TextField txtArmaHabilidades;

	// --- Defesas Específico ---
	@FXML private TextField txtDefesaBase;
	@FXML private VBox containerModsAtributo;
	@FXML private VBox containerModsStatus;
	@FXML private TextField txtDefesaHabilidades;
	@FXML private VBox boxArmaduraEfeitos;
	@FXML private ComboBox<String> comboArmaduraEfeito;
	@FXML private TextField txtArmaduraEfeitoChance;
	@FXML private ComboBox<String> comboArmaduraEfeitoAlvo;

	// --- Consumível Específico ---
	@FXML private TextField txtConsumivelTU;
	@FXML private ComboBox<String> comboConsumivelUsavel;
	@FXML private VBox containerEfeitosConsumivel;

	// --- Botões ---
	@FXML private Button btnResetarForm;
	@FXML private Button btnConfirmarCriar;
	@FXML private Button btnAbaCriarItem;

	private CombatController mainController;

	// Maps para coletar inputs dinâmicos
	private final Map<Atributo, TextField> mapTxtAtributos = new LinkedHashMap<>();
	private final Map<String, TextField> mapTxtStatus = new LinkedHashMap<>();
	private final Map<String, TextField> mapTxtEfeitosConsumivel = new LinkedHashMap<>();

	// Modificadores de status padrão do jogo
	private static final String[] LISTA_MOD_STATUS = {
		"HP_MAXIMO", "MP_MAXIMO", "MOVIMENTO", "REDUCAO_DANO_MODIFICADOR", 
		"DANO_BONUS_PERCENTUAL", "TAXA_CRITICA", "DANO_CRITICO", 
		"RESISTENCIA_DOT", "REDUCAO_CURA"
	};

	// Efeitos consumíveis padrão do jogo
	private static final String[] LISTA_EFEITOS_CONSUMIVEL = {
		"CURA_HP", "CURA_MP", "CURA_MARIONETTE", "ESCUDO", "REMOVE_VENENO", "REMOVE_TOXINA", 
		"REMOVE_CHARM", "BUFF_FORCA", "BONUS_DANO", "DURACAO"
	};

	// Efeitos on-hit de armas
	private static final String[] LISTA_EFEITOS_ARMAS = {
		"Nenhum", "Sangramento", "Veneno", "Toxina", "Queimação", "HellFire", 
		"Hemorragia", "Sono", "Dormindo", "Choque", "Stun", "Lento", "Charm", 
		"Pesadelo", "Ruptura", "Dilaceramento", "Corta Cura", "Corta Cura+", 
		"Armadura Quebrada"
	};

	@FXML
	public void initialize() {
		// 1. Configurar ComboBoxes Básicas
		comboCategoria.getItems().addAll("Arma", "Armadura", "Amuleto", "Consumivel");
		comboCategoria.setValue("Arma");

		comboTipoMoeda.getItems().addAll("BRONZE", "PRATA", "OURO");
		comboTipoMoeda.setValue("BRONZE");

		for (Raridade r : Raridade.values()) {
			comboRaridade.getItems().add(r.name());
		}
		comboRaridade.setValue(Raridade.COMUM.name());
		chkShiny.setSelected(false);

		// 2. Configurar ComboBoxes Arma
		comboArmaTipo.getItems().addAll("Melee", "Ranged", "Magico", "Grimorio");
		comboArmaTipo.setValue("Melee");

		for (Atributo a : Atributo.values()) {
			comboArmaAtributo.getItems().add(a.name());
		}
		comboArmaAtributo.setValue(Atributo.FORCA.name());

		comboArmaWielding.getItems().addAll("1 Mão", "2 Mãos");
		comboArmaWielding.setValue("1 Mão");

		for (TipoAlvo t : TipoAlvo.values()) {
			comboArmaAlvo.getItems().add(t.name());
		}
		comboArmaAlvo.setValue(TipoAlvo.INDIVIDUAL.name());

		comboArmaEfeito.getItems().addAll(LISTA_EFEITOS_ARMAS);
		comboArmaEfeito.setValue("Nenhum");

		// 2.5 Configurar ComboBoxes Armadura
		comboArmaduraEfeito.getItems().addAll(LISTA_EFEITOS_ARMAS);
		comboArmaduraEfeito.setValue("Nenhum");

		comboArmaduraEfeitoAlvo.getItems().addAll("ATACANTE", "USUARIO");
		comboArmaduraEfeitoAlvo.setValue("ATACANTE");

		// 3. Configurar ComboBox Consumível
		comboConsumivelUsavel.getItems().addAll("Sim", "Não");
		comboConsumivelUsavel.setValue("Sim");

		// 4. Montar Inputs Dinâmicos: Atributos
		for (Atributo atr : Atributo.values()) {
			HBox row = new HBox(10);
			row.setAlignment(Pos.CENTER_LEFT);
			Label lbl = new Label(atr.name());
			lbl.setStyle("-fx-text-fill: #a0a0b0; -fx-font-size: 11px; -fx-min-width: 90;");
			TextField tf = new TextField("0");
			tf.getStyleClass().add("editor-search-field");
			tf.setPrefWidth(60);
			row.getChildren().addAll(lbl, tf);
			containerModsAtributo.getChildren().add(row);
			mapTxtAtributos.put(atr, tf);
		}

		// 5. Montar Inputs Dinâmicos: Status
		for (String statusKey : LISTA_MOD_STATUS) {
			HBox row = new HBox(10);
			row.setAlignment(Pos.CENTER_LEFT);
			Label lbl = new Label(statusKey.replace("_", " "));
			lbl.setStyle("-fx-text-fill: #a0a0b0; -fx-font-size: 11px; -fx-min-width: 150;");
			TextField tf = new TextField("0.0");
			tf.getStyleClass().add("editor-search-field");
			tf.setPrefWidth(60);
			row.getChildren().addAll(lbl, tf);
			containerModsStatus.getChildren().add(row);
			mapTxtStatus.put(statusKey, tf);
		}

		// 6. Montar Inputs Dinâmicos: Efeitos Consumível
		for (String efeitoKey : LISTA_EFEITOS_CONSUMIVEL) {
			HBox row = new HBox(10);
			row.setAlignment(Pos.CENTER_LEFT);
			Label lbl = new Label(efeitoKey.replace("_", " "));
			lbl.setStyle("-fx-text-fill: #a0a0b0; -fx-font-size: 11px; -fx-min-width: 150;");
			TextField tf = new TextField("0.0");
			tf.getStyleClass().add("editor-search-field");
			tf.setPrefWidth(60);
			row.getChildren().addAll(lbl, tf);
			containerEfeitosConsumivel.getChildren().add(row);
			mapTxtEfeitosConsumivel.put(efeitoKey, tf);
		}

		// Listeners para Atualizações Dinâmicas
		comboCategoria.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			onCategoriaChanged(newVal);
		});

		comboArmaTipo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isRanged = "Ranged".equals(newVal);
			boxArmaMunicao.setVisible(isRanged);
			boxArmaMunicao.setManaged(isRanged);

			boolean isGrimorio = "Grimorio".equals(newVal);
			boxArmaSlots.setVisible(isGrimorio);
			boxArmaSlots.setManaged(isGrimorio);
		});

		// Trigger inicial de layout
		onCategoriaChanged("Arma");
	}

	public void inicializar(CombatController controller) {
		this.mainController = controller;
	}

	private void onCategoriaChanged(String novaCategoria) {
		paneArma.setVisible("Arma".equals(novaCategoria));
		paneArma.setManaged("Arma".equals(novaCategoria));

		boolean isDefesa = "Armadura".equals(novaCategoria) || "Amuleto".equals(novaCategoria);
		paneArmaduraAmuleto.setVisible(isDefesa);
		paneArmaduraAmuleto.setManaged(isDefesa);

		boolean isArmadura = "Armadura".equals(novaCategoria);
		boxArmaduraEfeitos.setVisible(isArmadura);
		boxArmaduraEfeitos.setManaged(isArmadura);

		paneConsumivel.setVisible("Consumivel".equals(novaCategoria));
		paneConsumivel.setManaged("Consumivel".equals(novaCategoria));

		boolean equipamento = !"Consumivel".equals(novaCategoria);
		chkShiny.setVisible(equipamento);
		chkShiny.setManaged(equipamento);
	}

	@FXML
	private void onResetarFormClick() {
		txtNome.clear();
		txtDescricao.clear();
		txtValorMoedas.clear();
		txtAndar.clear();
		comboCategoria.setValue("Arma");
		comboTipoMoeda.setValue("BRONZE");
		comboRaridade.setValue(Raridade.COMUM.name());
		chkShiny.setSelected(false);

		comboArmaTipo.setValue("Melee");
		comboArmaAtributo.setValue(Atributo.FORCA.name());
		comboArmaWielding.setValue("1 Mão");
		txtArmaDano.clear();
		txtArmaTicks.clear();
		txtArmaTU.clear();
		txtArmaAlcance.clear();
		comboArmaAlvo.setValue(TipoAlvo.INDIVIDUAL.name());
		txtArmaTamanhoArea.clear();
		comboArmaEfeito.setValue("Nenhum");
		txtArmaEfeitoChance.clear();
		txtArmaMunicao.clear();
		txtArmaSlots.clear();
		txtArmaHabilidades.clear();

		txtDefesaBase.clear();
		txtDefesaHabilidades.clear();
		comboArmaduraEfeito.setValue("Nenhum");
		txtArmaduraEfeitoChance.clear();
		comboArmaduraEfeitoAlvo.setValue("ATACANTE");
		mapTxtAtributos.values().forEach(tf -> tf.setText("0"));
		mapTxtStatus.values().forEach(tf -> tf.setText("0.0"));

		txtConsumivelTU.clear();
		comboConsumivelUsavel.setValue("Sim");
		mapTxtEfeitosConsumivel.values().forEach(tf -> tf.setText("0.0"));
	}

	@FXML
	private void onConfirmarCriarClick() {
		String nome = txtNome.getText();
		if (nome == null || nome.trim().isEmpty()) {
			mostrarAlerta("Erro", "O nome do item é obrigatório!", AlertType.ERROR);
			return;
		}

		String categoria = comboCategoria.getValue();
		if (categoria == null) {
			mostrarAlerta("Erro", "Selecione uma categoria!", AlertType.ERROR);
			return;
		}

		try {
			int valor = parseInteger(txtValorMoedas.getText(), 0);
			String moeda = comboTipoMoeda.getValue();
			String desc = txtDescricao.getText() != null ? txtDescricao.getText() : "";
			String raridade = comboRaridade.getValue();
			String andar = txtAndar.getText() != null ? txtAndar.getText() : "0";

			Map<String, Object> data = new LinkedHashMap<>();

			if ("Arma".equals(categoria)) {
				data.put("nome", nome);
				data.put("categoria", "Custom");
				data.put("descricao", desc);
				data.put("raridade", raridade);
				data.put("andar", andar);
				data.put("valorMoedas", valor);
				data.put("tipoMoeda", moeda);
				data.put("shiny", chkShiny.isSelected());

				int dano = parseInteger(txtArmaDano.getText(), 5);
				int ticks = parseInteger(txtArmaTicks.getText(), 1);
				int tu = parseInteger(txtArmaTU.getText(), 100);
				int alcance = parseInteger(txtArmaAlcance.getText(), 1);
				int hands = "2 Mãos".equals(comboArmaWielding.getValue()) ? 2 : 1;
				String armaTipo = comboArmaTipo.getValue();

				data.put("danoBase", dano);
				data.put("ticksDeDano", ticks);
				data.put("atributoMultiplicador", comboArmaAtributo.getValue());
				data.put("custoTU", tu);
				data.put("tipo", armaTipo);
				data.put("alcance", alcance);
				data.put("wielding", hands);

				String aoe = comboArmaAlvo.getValue();
				if (!TipoAlvo.INDIVIDUAL.name().equals(aoe)) {
					data.put("tipoAlvo", aoe);
					data.put("tamanhoArea", parseInteger(txtArmaTamanhoArea.getText(), 0));
				}

				String efeito = comboArmaEfeito.getValue();
				if (!"Nenhum".equals(efeito)) {
					data.put("efeitoAoAcertar", efeito);
					data.put("chanceEfeito", parseDouble(txtArmaEfeitoChance.getText(), 1.0));
				}

				if ("Ranged".equals(armaTipo)) {
					data.put("municaoMaxima", parseInteger(txtArmaMunicao.getText(), 6));
				} else if ("Grimorio".equals(armaTipo)) {
					data.put("maxSlots", parseInteger(txtArmaSlots.getText(), 3));
				}

				String habsStr = txtArmaHabilidades.getText();
				if (habsStr != null && !habsStr.trim().isEmpty()) {
					List<String> habs = new ArrayList<>();
					for (String s : habsStr.split(",")) {
						if (!s.trim().isEmpty()) {
							habs.add(s.trim());
						}
					}
					if (!habs.isEmpty()) {
						data.put("habilidadesConcedidas", habs);
					}
				}

			} else if ("Armadura".equals(categoria) || "Amuleto".equals(categoria)) {
				data.put("classeItem", categoria);
				data.put("nome", nome);
				data.put("descricao", desc);
				data.put("raridade", raridade);
				data.put("andar", andar);
				data.put("valorMoedas", valor);
				data.put("tipoMoeda", moeda);
				data.put("shiny", chkShiny.isSelected());

				int defVal = parseInteger(txtDefesaBase.getText(), 10);
				if ("Armadura".equals(categoria)) {
					data.put("armaduraBase", defVal);

					String efeitoDano = comboArmaduraEfeito.getValue();
					if (efeitoDano != null && !"Nenhum".equals(efeitoDano)) {
						data.put("efeitoAoTomarDano", efeitoDano);
						data.put("chanceEfeitoAoTomarDano", parseDouble(txtArmaduraEfeitoChance.getText(), 1.0));
						data.put("alvoEfeitoAoTomarDano", comboArmaduraEfeitoAlvo.getValue());
					}
				} else {
					data.put("armaduraBonus", defVal);
				}

				String habsStr = txtDefesaHabilidades.getText();
				if (habsStr != null && !habsStr.trim().isEmpty()) {
					List<String> habs = new ArrayList<>();
					for (String s : habsStr.split(",")) {
						if (!s.trim().isEmpty()) {
							habs.add(s.trim());
						}
					}
					if (!habs.isEmpty()) {
						data.put("habilidadesConcedidas", habs);
					}
				}

				// Modificadores de atributo
				Map<String, Integer> modsAtr = new HashMap<>();
				for (Map.Entry<Atributo, TextField> e : mapTxtAtributos.entrySet()) {
					int val = parseInteger(e.getValue().getText(), 0);
					if (val != 0) {
						modsAtr.put(e.getKey().name(), val);
					}
				}
				data.put("modificadoresDeAtributo", modsAtr);

				// Modificadores de status
				Map<String, Double> modsStatus = new HashMap<>();
				for (Map.Entry<String, TextField> e : mapTxtStatus.entrySet()) {
					double val = parseDouble(e.getValue().getText(), 0.0);
					if (val != 0.0) {
						modsStatus.put(e.getKey(), val);
					}
				}
				data.put("modificadoresStatus", modsStatus);

			} else if ("Consumivel".equals(categoria)) {
				data.put("classeItem", "Consumivel");
				data.put("nome", nome);
				data.put("descricao", desc);
				data.put("valorMoedas", valor);
				data.put("tipoMoeda", moeda);
				data.put("custoTU", parseInteger(txtConsumivelTU.getText(), 100));
				data.put("usavelEmCombate", "Sim".equals(comboConsumivelUsavel.getValue()));

				// Efeitos do consumível
				Map<String, Double> efeitos = new HashMap<>();
				for (Map.Entry<String, TextField> e : mapTxtEfeitosConsumivel.entrySet()) {
					double val = parseDouble(e.getValue().getText(), 0.0);
					if (val != 0.0) {
						efeitos.put(e.getKey(), val);
					}
				}
				data.put("efeitos", efeitos);
			}

			// Salvar em arquivo
			salvarItemNoBancoDeDados(categoria, nome, data);

		} catch (Exception e) {
			mostrarAlerta("Erro", "Erro ao processar os inputs: " + e.getMessage(), AlertType.ERROR);
			e.printStackTrace();
		}
	}

	private void salvarItemNoBancoDeDados(String categoria, String nome, Map<String, Object> data) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String projectPath = System.getProperty("user.dir");
		String filename;

		if ("Arma".equalsIgnoreCase(categoria)) {
			filename = "armas.json";
		} else if ("Armadura".equalsIgnoreCase(categoria)) {
			filename = "armaduras.json";
		} else if ("Amuleto".equalsIgnoreCase(categoria)) {
			filename = "amuletos.json";
		} else {
			filename = "consumiveis.json";
		}

		// Procurar caminhos válidos do projeto
		File file = new File(projectPath + "/src/main/resources/data/" + filename);
		if (!file.exists()) {
			file = new File(projectPath + "/src/data/" + filename);
		}
		if (!file.exists()) {
			// fallback para criar no local correto
			file = new File(projectPath + "/src/main/resources/data/" + filename);
		}

		try {
			file.getParentFile().mkdirs();
			Map<String, Map<String, Object>> database = new LinkedHashMap<>();

			// Ler existentes se arquivo existir
			if (file.exists() && file.length() > 0) {
				try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
					Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
					Map<String, Map<String, Object>> existing = gson.fromJson(reader, mapType);
					if (existing != null) {
						database.putAll(existing);
					}
				}
			}

			// Adicionar/Sobrescrever item
			database.put(nome, data);

			// Gravar
			try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(database, writer);
				System.out.println("Salvo no arquivo local: " + file.getAbsolutePath());
			}

			// Salvar no diretório BIN (Hotfix para execução em IDE sem recompilar na hora)
			URL urlBin = getClass().getResource("/data/" + filename);
			if (urlBin != null) {
				try {
					File fileBin = new File(urlBin.toURI());
					try (Writer writerBin = new FileWriter(fileBin, StandardCharsets.UTF_8)) {
						gson.toJson(database, writerBin);
						System.out.println("Salvo também na pasta compilada BIN.");
					}
				} catch (Exception ex) {
					// Ignora falhas menores no BIN se ocorrerem
				}
			}

			// Recarregar os bancos de dados em tempo real no CombatController
			if (mainController != null) {
				mainController.recarregarBancosDeDados();
				mainController.atualizarInterfaceTotal();
			}

			mostrarAlerta("Sucesso", "Item '" + nome + "' criado e adicionado com sucesso!", AlertType.INFORMATION);
			onResetarFormClick();

		} catch (Exception e) {
			mostrarAlerta("Erro Crítico", "Erro ao gravar no arquivo JSON: " + e.getMessage(), AlertType.ERROR);
			e.printStackTrace();
		}
	}

	private int parseInteger(String text, int defaultValue) {
		if (text == null || text.trim().isEmpty()) return defaultValue;
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private double parseDouble(String text, double defaultValue) {
		if (text == null || text.trim().isEmpty()) return defaultValue;
		try {
			return Double.parseDouble(text.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void mostrarAlerta(String titulo, String texto, AlertType tipo) {
		Alert alert = new Alert(tipo);
		alert.setTitle(titulo);
		alert.setHeaderText(null);
		alert.setContentText(texto);
		alert.showAndWait();
	}
}
