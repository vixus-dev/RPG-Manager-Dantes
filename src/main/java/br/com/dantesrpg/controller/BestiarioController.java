package br.com.dantesrpg.controller;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.enums.PesoEntidade;
import br.com.dantesrpg.model.util.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BestiarioController {

	private static final List<String> PROPRIEDADES_PADRAO = List.of(
			"IMUNIDADE_CONTROLE",
			"IMUNIDADE_DOT",
			"EXPLODIR",
			"EXPLOSIVO",
			"VAMPIRISMO",
			"BLINDADO",
			"ARMADURADO",
			"REGENERACAO",
			"IMUNE_KNOCKBACK");

	// Lista e Filtros
	@FXML
	private TextField inputBusca;
	@FXML
	private ComboBox<String> comboFiltroRaca;
	@FXML
	private ComboBox<String> comboFiltroGrau;
	@FXML
	private ListView<String> monstroListView;

	// Editor - Header
	@FXML
	private ImageView imgTokenPreview;
	@FXML
	private TextField inputID;
	@FXML
	private TextField inputNome;

	// Editor - Grid
	@FXML
	private TextField inputRaca;
	@FXML
	private TextField inputGrau;
	@FXML
	private TextField inputVida;
	@FXML
	private TextField inputMana;
	@FXML
	private TextField inputAgi;
	@FXML
	private TextField inputDef;
	@FXML
	private TextField inputXP;
	@FXML
	private TextField inputSegmentos;
	@FXML
	private ComboBox<String> comboPeso;
	@FXML
	private TextField inputTamanhoX;
	@FXML
	private TextField inputTamanhoY;
	@FXML
	private ComboBox<String> comboArma;
	@FXML
	private CheckBox chkPoderoso;

	// Editor - Extras
	@FXML
	private Label lblHabilidadesArma;
	@FXML
	private ListView<String> listaPropsDisponiveis;
	@FXML
	private ListView<String> listaPropsSelecionadas;
	@FXML
	private TextField inputPropriedadeCustom;

	// Spawn
	@FXML
	private Slider sliderSpawnQtd;
	@FXML
	private Label lblQtdSpawn;
	@FXML
	private Button btnPrepararSpawn;
	@FXML
	private VBox painelDetalhes;

	private CombatController mainController;
	private Map<String, Map<String, Object>> bestiarioData;
	private Stage stage;
	private String idSelecionado;

	@FXML
	public void initialize() {
		painelDetalhes.setDisable(true);
		configurarComboPeso();
		configurarListasDePropriedades();

		monstroListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				carregarDetalhes(newVal);
			}
		});

		sliderSpawnQtd.valueProperty().addListener((obs, oldVal, newVal) -> {
			lblQtdSpawn.setText(String.format("%.0f", newVal));
		});

		comboArma.valueProperty().addListener((obs, oldVal, newVal) -> atualizarHabilidadesArma(newVal));
	}

	public void inicializar(CombatController controller, Map<String, Map<String, Object>> dados) {
		this.mainController = controller;
		this.bestiarioData = dados;

		configurarFiltros();
		atualizarLista();

		List<String> todasArmas = mainController.getListaNomesArmas();
		java.util.Collections.sort(todasArmas);
		comboArma.getItems().setAll(todasArmas);
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	private void configurarComboPeso() {
		List<String> pesos = new ArrayList<>();
		for (PesoEntidade peso : PesoEntidade.values()) {
			pesos.add(peso.getJsonId());
		}
		comboPeso.getItems().setAll(pesos);
		comboPeso.setValue(PesoEntidade.MEDIO_PADRAO.getJsonId());
	}

	private void configurarFiltros() {
		List<String> racas = bestiarioData.values().stream()
				.map(m -> (String) m.getOrDefault("raca", "Desconhecida"))
				.distinct()
				.sorted()
				.collect(Collectors.toList());
		racas.add(0, "Todas");
		comboFiltroRaca.getItems().setAll(racas);
		comboFiltroRaca.getSelectionModel().selectFirst();

		List<String> graus = bestiarioData.values().stream()
				.map(m -> String.valueOf(lerNumero(m, "grau", 0).intValue()))
				.distinct()
				.sorted()
				.collect(Collectors.toList());
		graus.add(0, "Todos");
		comboFiltroGrau.getItems().setAll(graus);
		comboFiltroGrau.getSelectionModel().selectFirst();

		inputBusca.textProperty().addListener(o -> atualizarLista());
		comboFiltroRaca.valueProperty().addListener(o -> atualizarLista());
		comboFiltroGrau.valueProperty().addListener(o -> atualizarLista());
	}

	private void atualizarLista() {
		String busca = inputBusca.getText().toLowerCase();
		String racaFiltro = comboFiltroRaca.getValue();
		String grauFiltro = comboFiltroGrau.getValue();

		List<String> filtrados = bestiarioData.entrySet().stream().filter(entry -> {
			Map<String, Object> data = entry.getValue();
			String nome = ((String) data.getOrDefault("nome", "")).toLowerCase();
			String raca = (String) data.getOrDefault("raca", "");
			int grau = lerNumero(data, "grau", 0).intValue();

			boolean matchNome = nome.contains(busca) || entry.getKey().toLowerCase().contains(busca);
			boolean matchRaca = racaFiltro == null || racaFiltro.equals("Todas") || raca.equals(racaFiltro);
			boolean matchGrau = grauFiltro == null || grauFiltro.equals("Todos")
					|| String.valueOf(grau).equals(grauFiltro);

			return matchNome && matchRaca && matchGrau;
		}).map(Map.Entry::getKey)
				.sorted()
				.collect(Collectors.toList());

		monstroListView.getItems().setAll(filtrados);
	}

	private void carregarDetalhes(String id) {
		this.idSelecionado = id;
		Map<String, Object> data = bestiarioData.get(id);
		painelDetalhes.setDisable(false);

		inputID.setText(id);
		inputNome.setText((String) data.getOrDefault("nome", ""));
		inputRaca.setText((String) data.getOrDefault("raca", ""));
		inputGrau.setText(formatarInteiro(data, "grau", 0));
		inputVida.setText(formatarInteiro(data, "vida", 10));
		inputMana.setText(formatarInteiro(data, "mana", 0));
		inputAgi.setText(formatarInteiro(data, "agilidade", 1));
		inputDef.setText(formatarInteiro(data, "defesa", 0));
		inputXP.setText(formatarInteiro(data, "xpReward", 0));
		inputSegmentos.setText(formatarInteiro(data, "segmentos", 0));
		inputTamanhoX.setText(formatarInteiro(data, "tamanhoX", 1));
		inputTamanhoY.setText(formatarInteiro(data, "tamanhoY", 1));
		comboPeso.setValue((String) data.getOrDefault("peso", PesoEntidade.MEDIO_PADRAO.getJsonId()));
		chkPoderoso.setSelected(Boolean.TRUE.equals(data.get("poderoso")));

		String nomeArma = (String) data.getOrDefault("arma", "");
		comboArma.setValue(nomeArma);

		carregarPropriedades(data);
		carregarImagem(inputNome.getText());
		atualizarHabilidadesArma(nomeArma);
	}

	private String formatarInteiro(Map<String, Object> data, String chave, int padrao) {
		return String.valueOf(lerNumero(data, chave, padrao).intValue());
	}

	private Number lerNumero(Map<String, Object> data, String chave, int padrao) {
		Object valor = data.get(chave);
		if (valor instanceof Number) {
			return (Number) valor;
		}
		return padrao;
	}

	private Map<String, Object> coletarDadosDoFormulario() {
		Map<String, Object> dados = new java.util.HashMap<>();
		String idBase = (idSelecionado != null) ? idSelecionado : "CustomMob";

		dados.put("id", idBase);
		dados.put("nome", inputNome.getText());
		dados.put("raca", inputRaca.getText());

		try {
			dados.put("grau", Double.parseDouble(inputGrau.getText()));
			dados.put("vida", Double.parseDouble(inputVida.getText()));
			dados.put("mana", Double.parseDouble(inputMana.getText()));
			dados.put("agilidade", Double.parseDouble(inputAgi.getText()));
			dados.put("defesa", Double.parseDouble(inputDef.getText()));
			dados.put("xpReward", Double.parseDouble(inputXP.getText()));
			dados.put("segmentos", Double.parseDouble(inputSegmentos.getText()));
			dados.put("tamanhoX", Double.parseDouble(inputTamanhoX.getText()));
			dados.put("tamanhoY", Double.parseDouble(inputTamanhoY.getText()));
		} catch (NumberFormatException e) {
			System.err.println("Erro ao ler números do editor. Usando defaults.");
		}

		dados.put("arma", comboArma.getValue());
		dados.put("peso", comboPeso.getValue());
		dados.put("poderoso", chkPoderoso.isSelected());
		dados.put("propriedades", obterPropriedadesSelecionadas());

		return dados;
	}

	private void configurarListasDePropriedades() {
		listaPropsDisponiveis.setCellFactory(lv -> criarCelulaPropriedade());
		listaPropsSelecionadas.setCellFactory(lv -> criarCelulaPropriedade());
		listaPropsDisponiveis.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				moverPropriedade(listaPropsDisponiveis, listaPropsSelecionadas);
			}
		});
		listaPropsSelecionadas.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				moverPropriedade(listaPropsSelecionadas, listaPropsDisponiveis);
			}
		});
		atualizarPropriedadesDisponiveis(List.of());
	}

	private ListCell<String> criarCelulaPropriedade() {
		return new ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setTooltip(null);
					return;
				}
				setText(item);
				setTooltip(new Tooltip(getDescricaoPropriedade(item)));
			}
		};
	}

	private void carregarPropriedades(Map<String, Object> data) {
		List<String> selecionadas = new ArrayList<>();
		Object propsObj = data.get("propriedades");
		if (propsObj instanceof List<?>) {
			for (Object prop : (List<?>) propsObj) {
				if (prop instanceof String valor && !valor.isBlank()) {
					selecionadas.add(valor.trim());
				}
			}
		}
		listaPropsSelecionadas.getItems().setAll(selecionadas);
		atualizarPropriedadesDisponiveis(selecionadas);
	}

	private void atualizarPropriedadesDisponiveis(List<String> selecionadas) {
		Set<String> selecionadasSet = new LinkedHashSet<>(selecionadas);
		List<String> disponiveis = PROPRIEDADES_PADRAO.stream()
				.filter(prop -> !selecionadasSet.contains(prop))
				.collect(Collectors.toList());
		listaPropsDisponiveis.getItems().setAll(disponiveis);
	}

	private List<String> obterPropriedadesSelecionadas() {
		return new ArrayList<>(listaPropsSelecionadas.getItems());
	}

	private void moverPropriedade(ListView<String> origem, ListView<String> destino) {
		String selecionada = origem.getSelectionModel().getSelectedItem();
		if (selecionada == null) {
			return;
		}
		origem.getItems().remove(selecionada);
		if (!destino.getItems().contains(selecionada)) {
			destino.getItems().add(selecionada);
		}
	}

	private void adicionarPropriedadeSelecionada(String propriedade) {
		if (propriedade == null || propriedade.isBlank()) {
			return;
		}
		String normalizada = propriedade.trim();
		if (!listaPropsSelecionadas.getItems().contains(normalizada)) {
			listaPropsSelecionadas.getItems().add(normalizada);
		}
		listaPropsDisponiveis.getItems().remove(normalizada);
	}

	private String getDescricaoPropriedade(String propriedade) {
		if (propriedade == null) {
			return "";
		}
		return switch (propriedade) {
			case "IMUNIDADE_CONTROLE" -> "Imunidade a Stun, Lento e Sono.";
			case "IMUNIDADE_DOT" -> "Imunidade a DoTs como Veneno, Sangramento e Queimação.";
			case "EXPLODIR" -> "Explode em chamas ao morrer.";
			case "EXPLOSIVO" -> "Detona efeito explosivo ao morrer.";
			case "VAMPIRISMO" -> "Cura parte do dano causado.";
			case "BLINDADO" -> "Concede escudo normal baseado na vida máxima. Aceita BLINDADO:N.";
			case "ARMADURADO" -> "Concede escudo maior baseado na vida máxima. Aceita ARMADURADO:N.";
			case "REGENERACAO" -> "Regenera HP no início do turno.";
			case "IMUNE_KNOCKBACK" -> "Ignora movimento forçado.";
			default -> "Propriedade customizada do personagem.";
		};
	}

	private void carregarImagem(String nomeMonstro) {
		String nomeArquivo = nomeMonstro.toLowerCase().replace(" ", "_") + ".png";
		String path = "/tokens/" + nomeArquivo;
		try {
			Image img = ImageCache.get(path, 120, 120);
			if (img != null && !img.isError()) {
				imgTokenPreview.setImage(img);
			} else {
				imgTokenPreview.setImage(null);
			}
		} catch (Exception e) {
			imgTokenPreview.setImage(null);
		}
	}

	private void atualizarHabilidadesArma(String nomeArma) {
		if (nomeArma == null || nomeArma.isEmpty()) {
			lblHabilidadesArma.setText("Nenhuma (Desarmado)");
			return;
		}

		br.com.dantesrpg.model.Item item = mainController.getItem(nomeArma);
		if (item instanceof Arma) {
			Arma arma = (Arma) item;
			List<String> habs = arma.getHabilidadesConcedidasNomes();
			if (habs != null && !habs.isEmpty()) {
				lblHabilidadesArma.setText(String.join("\n", habs));
			} else {
				lblHabilidadesArma.setText("Nenhuma");
			}
		} else {
			lblHabilidadesArma.setText("Arma não encontrada no banco de dados.");
		}
	}

	@FXML
	private void onSalvarClick() {
		if (idSelecionado == null) {
			return;
		}

		try {
			Map<String, Object> data = bestiarioData.get(idSelecionado);

			data.put("nome", inputNome.getText());
			data.put("raca", inputRaca.getText());
			data.put("grau", Double.parseDouble(inputGrau.getText()));
			data.put("vida", Double.parseDouble(inputVida.getText()));
			data.put("mana", Double.parseDouble(inputMana.getText()));
			data.put("agilidade", Double.parseDouble(inputAgi.getText()));
			data.put("defesa", Double.parseDouble(inputDef.getText()));
			data.put("xpReward", Double.parseDouble(inputXP.getText()));
			data.put("segmentos", Double.parseDouble(inputSegmentos.getText()));
			data.put("tamanhoX", Double.parseDouble(inputTamanhoX.getText()));
			data.put("tamanhoY", Double.parseDouble(inputTamanhoY.getText()));
			data.put("peso", comboPeso.getValue());
			data.put("poderoso", chkPoderoso.isSelected());
			data.put("arma", comboArma.getValue());
			data.put("propriedades", obterPropriedadesSelecionadas());

			salvarJsonNoDisco();

			Alert alert = new Alert(Alert.AlertType.INFORMATION, "Monstro salvo com sucesso!");
			alert.show();

			configurarFiltros();
			atualizarLista();
		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao salvar: " + e.getMessage());
			alert.show();
		}
	}

	private void salvarJsonNoDisco() {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String projectPath = System.getProperty("user.dir");
			File file = new File(projectPath + "/src/main/resources/data/bestiario.json");

			if (!file.getParentFile().exists()) {
				file = new File("data/bestiario.json");
				file.getParentFile().mkdirs();
			}

			try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
				gson.toJson(bestiarioData, writer);
				System.out.println("BESTIÁRIO: Arquivo atualizado em " + file.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Falha de IO ao salvar JSON");
		}
	}

	@FXML
	private void onNovoMonstroClick() {
		TextInputDialog dialog = new TextInputDialog("NovoMonstro");
		dialog.setTitle("Novo Monstro");
		dialog.setHeaderText("Defina o ID único (sem espaços):");
		dialog.setContentText("ID:");

		dialog.showAndWait().ifPresent(id -> {
			if (bestiarioData.containsKey(id)) {
				new Alert(Alert.AlertType.ERROR, "ID já existe!").show();
				return;
			}

			java.util.HashMap<String, Object> novo = new java.util.HashMap<>();
			novo.put("nome", "Novo Monstro");
			novo.put("raca", "Desconhecido");
			novo.put("grau", 0.0);
			novo.put("vida", 10.0);
			novo.put("mana", 0.0);
			novo.put("agilidade", 1.0);
			novo.put("defesa", 0.0);
			novo.put("arma", "Punhos");
			novo.put("xpReward", 0.0);
			novo.put("segmentos", 0.0);
			novo.put("tamanhoX", 1.0);
			novo.put("tamanhoY", 1.0);
			novo.put("peso", PesoEntidade.MEDIO_PADRAO.getJsonId());
			novo.put("propriedades", new ArrayList<>());
			novo.put("poderoso", false);

			bestiarioData.put(id, novo);
			atualizarLista();
			monstroListView.getSelectionModel().select(id);
		});
	}

	@FXML
	private void onAdicionarPropriedadeClick() {
		moverPropriedade(listaPropsDisponiveis, listaPropsSelecionadas);
	}

	@FXML
	private void onRemoverPropriedadeClick() {
		moverPropriedade(listaPropsSelecionadas, listaPropsDisponiveis);
	}

	@FXML
	private void onAdicionarPropriedadeCustomClick() {
		adicionarPropriedadeSelecionada(inputPropriedadeCustom.getText());
		inputPropriedadeCustom.clear();
	}

	@FXML
	private void onPrepararSpawnClick() {
		Map<String, Object> dadosCustom = coletarDadosDoFormulario();
		int qtd = (int) sliderSpawnQtd.getValue();

		mainController.entrarModoSpawnMultiploCustom(dadosCustom, qtd);

		stage.setIconified(true);
	}
}
