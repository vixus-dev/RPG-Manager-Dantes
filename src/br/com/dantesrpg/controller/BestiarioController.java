package br.com.dantesrpg.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.util.FileLoader;

public class BestiarioController {

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
	private ComboBox<String> comboArma;

	// Editor - Extras
	@FXML
	private Label lblHabilidadesArma;
	@FXML
	private TextArea inputPropriedades;

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
		painelDetalhes.setDisable(true); // Começa desabilitado

		// Listener da Lista
		monstroListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null)
				carregarDetalhes(newVal);
		});

		// Listener do Slider
		sliderSpawnQtd.valueProperty().addListener((obs, oldVal, newVal) -> {
			lblQtdSpawn.setText(String.format("%.0f", newVal));
		});

		// Listener da Arma (Atualiza habilidades)
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

	private Map<String, Object> coletarDadosDoFormulario() {
		Map<String, Object> dados = new java.util.HashMap<>();

		// Usa o ID selecionado como base ou gera um temp
		String idBase = (idSelecionado != null) ? idSelecionado : "CustomMob";

		dados.put("id", idBase); // Importante para o Controller saber quem é
		dados.put("nome", inputNome.getText());
		dados.put("raca", inputRaca.getText());

		try {
			dados.put("grau", Double.parseDouble(inputGrau.getText()));
			dados.put("vida", Double.parseDouble(inputVida.getText()));
			dados.put("mana", Double.parseDouble(inputMana.getText()));
			dados.put("agilidade", Double.parseDouble(inputAgi.getText()));
			dados.put("defesa", Double.parseDouble(inputDef.getText()));
			dados.put("xpReward", Double.parseDouble(inputXP.getText()));
		} catch (NumberFormatException e) {
			// Se der erro de numero, usa defaults
			System.err.println("Erro ao ler números do editor. Usando defaults.");
		}

		dados.put("arma", comboArma.getValue());

		// Propriedades
		String propsRaw = inputPropriedades.getText();
		List<String> propsList = new ArrayList<>();
		if (!propsRaw.isEmpty()) {
			propsList = Arrays.stream(propsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
					.collect(Collectors.toList());
		}
		dados.put("propriedades", propsList);

		if (idSelecionado != null && bestiarioData.containsKey(idSelecionado)) {
			Map<String, Object> original = bestiarioData.get(idSelecionado);

			// Se o original tem segmentos, passamos para frente
			if (original.containsKey("segmentos")) {
				dados.put("segmentos", original.get("segmentos"));
			}
		}

		return dados;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	private void configurarFiltros() {
		// Coleta Raças e Graus únicos
		List<String> racas = bestiarioData.values().stream().map(m -> (String) m.getOrDefault("raca", "Desconhecida"))
				.distinct().sorted().collect(Collectors.toList());
		racas.add(0, "Todas");
		comboFiltroRaca.getItems().setAll(racas);
		comboFiltroRaca.getSelectionModel().selectFirst();

		List<String> graus = bestiarioData.values().stream()
				.map(m -> String.valueOf(((Double) m.getOrDefault("grau", 0.0)).intValue())).distinct().sorted()
				.collect(Collectors.toList());
		graus.add(0, "Todos");
		comboFiltroGrau.getItems().setAll(graus);
		comboFiltroGrau.getSelectionModel().selectFirst();

		// Listeners
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
			int grau = ((Double) data.getOrDefault("grau", 0.0)).intValue();

			boolean matchNome = nome.contains(busca) || entry.getKey().toLowerCase().contains(busca);
			boolean matchRaca = racaFiltro == null || racaFiltro.equals("Todas") || raca.equals(racaFiltro);
			boolean matchGrau = grauFiltro == null || grauFiltro.equals("Todos")
					|| String.valueOf(grau).equals(grauFiltro);

			return matchNome && matchRaca && matchGrau;
		}).map(Map.Entry::getKey) // Pega o ID
				.sorted().collect(Collectors.toList());

		monstroListView.getItems().setAll(filtrados);
	}

	private void carregarDetalhes(String id) {
		this.idSelecionado = id;
		Map<String, Object> data = bestiarioData.get(id);
		painelDetalhes.setDisable(false);

		inputID.setText(id);
		inputNome.setText((String) data.getOrDefault("nome", ""));
		inputRaca.setText((String) data.getOrDefault("raca", ""));
		inputGrau.setText(String.valueOf(((Double) data.getOrDefault("grau", 0.0)).intValue()));
		inputVida.setText(String.valueOf(((Double) data.getOrDefault("vida", 10.0)).intValue()));
		inputMana.setText(String.valueOf(((Double) data.getOrDefault("mana", 0.0)).intValue()));
		inputAgi.setText(String.valueOf(((Double) data.getOrDefault("agilidade", 1.0)).intValue()));
		inputDef.setText(String.valueOf(((Double) data.getOrDefault("defesa", 0.0)).intValue()));
		inputXP.setText(String.valueOf(((Double) data.getOrDefault("xpReward", 0.0)).intValue()));

		String nomeArma = (String) data.getOrDefault("arma", "");
		comboArma.setValue(nomeArma);

		// Propriedades (Lista -> String)
		Object propsObj = data.get("propriedades");
		if (propsObj instanceof List) {
			List<String> props = (List<String>) propsObj;
			inputPropriedades.setText(String.join(", ", props));
		} else {
			inputPropriedades.setText("");
		}

		carregarImagem(inputNome.getText());
		atualizarHabilidadesArma(nomeArma);
	}

	private void carregarImagem(String nomeMonstro) {
		// Tenta carregar imagem do token
		// Lógica: nome_do_monstro.png
		String nomeArquivo = nomeMonstro.toLowerCase().replace(" ", "_") + ".png";
		String path = "/tokens/" + nomeArquivo;
		try {
			Image img = new Image(FileLoader.carregarArquivo(path));
			if (!img.isError()) {
				imgTokenPreview.setImage(img);
			} else {
				imgTokenPreview.setImage(null); // Limpa se erro
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

		// Pede para o MainController buscar a arma real
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
		if (idSelecionado == null)
			return;

		try {
			Map<String, Object> data = bestiarioData.get(idSelecionado);

			// Atualiza o Map na memória
			data.put("nome", inputNome.getText());
			data.put("raca", inputRaca.getText());
			data.put("grau", Double.parseDouble(inputGrau.getText()));
			data.put("vida", Double.parseDouble(inputVida.getText()));
			data.put("mana", Double.parseDouble(inputMana.getText()));
			data.put("agilidade", Double.parseDouble(inputAgi.getText()));
			data.put("defesa", Double.parseDouble(inputDef.getText()));
			data.put("xpReward", Double.parseDouble(inputXP.getText()));
			data.put("arma", comboArma.getValue());

			// Propriedades (String -> List)
			String propsRaw = inputPropriedades.getText();
			if (!propsRaw.isEmpty()) {
				List<String> propsList = Arrays.stream(propsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
						.collect(Collectors.toList());
				data.put("propriedades", propsList);
			} else {
				data.put("propriedades", new ArrayList<>());
			}

			// Salva no Disco
			salvarJsonNoDisco();

			// Feedback
			Alert alert = new Alert(Alert.AlertType.INFORMATION, "Monstro salvo com sucesso!");
			alert.show();

			// Atualiza filtros
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
			// Caminho fixo para o bestiário
			String projectPath = System.getProperty("user.dir");
			// Tenta salvar na pasta de desenvolvimento para persistir
			File file = new File(projectPath + "/src/main/resources/data/bestiario.json");

			// Se não achar, tenta fallback para pasta local
			if (!file.getParentFile().exists()) {
				file = new File("data/bestiario.json");
				file.getParentFile().mkdirs();
			}

			try (FileWriter writer = new FileWriter(file)) {
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

			// Cria template vazio
			java.util.HashMap<String, Object> novo = new java.util.HashMap<>();
			novo.put("nome", "Novo Monstro");
			novo.put("raca", "Desconhecido");
			novo.put("vida", 10.0);

			bestiarioData.put(id, novo);
			atualizarLista();
			monstroListView.getSelectionModel().select(id); // Seleciona para editar
		});
	}

	@FXML
	private void onPrepararSpawnClick() {
		Map<String, Object> dadosCustom = coletarDadosDoFormulario();
		int qtd = (int) sliderSpawnQtd.getValue();

		mainController.entrarModoSpawnMultiploCustom(dadosCustom, qtd);

		stage.setIconified(true);
	}
}