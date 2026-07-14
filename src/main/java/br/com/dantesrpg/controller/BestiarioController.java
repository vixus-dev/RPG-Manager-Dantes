package br.com.dantesrpg.controller;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.enums.PesoEntidade;
import br.com.dantesrpg.model.util.CharacterImageResolver;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BestiarioController {

	private static final String CLASSE_CAMPO_INVALIDO = "bestiary-field-error";

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
	private boolean filtrosComListenersConfigurados;

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

		inputNome.textProperty().addListener((obs, oldVal, newVal) -> carregarImagem(newVal));
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

		if (filtrosComListenersConfigurados) {
			return;
		}
		inputBusca.textProperty().addListener(o -> atualizarLista());
		comboFiltroRaca.valueProperty().addListener(o -> atualizarLista());
		comboFiltroGrau.valueProperty().addListener(o -> atualizarLista());
		filtrosComListenersConfigurados = true;
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
		limparErrosFormulario();
		List<String> erros = new ArrayList<>();
		Map<String, Object> dados = new HashMap<>();
		String idBase = (idSelecionado != null) ? idSelecionado : "CustomMob";

		dados.put("id", idBase);
		dados.put("nome", lerTextoObrigatorio(inputNome, "Nome", erros));
		dados.put("raca", lerTextoObrigatorio(inputRaca, "Raça", erros));
		dados.put("grau", lerCampoNumerico(inputGrau, "Grau", 0, true, erros));
		dados.put("vida", lerCampoNumerico(inputVida, "Vida máxima", 1, false, erros));
		dados.put("mana", lerCampoNumerico(inputMana, "Mana máxima", 0, false, erros));
		dados.put("agilidade", lerCampoNumerico(inputAgi, "Destreza", 0, true, erros));
		double resistencia = lerCampoNumerico(inputDef, "Resistência (%)", 0, true, erros);
		if (resistencia > 90) {
			erros.add("Resistência (%) deve ser no máximo 90.");
		}
		dados.put("defesa", resistencia);
		dados.put("xpReward", lerCampoNumerico(inputXP, "XP", 0, true, erros));
		dados.put("segmentos", lerCampoNumerico(inputSegmentos, "Segmentos", 0, true, erros));
		dados.put("tamanhoX", lerCampoNumerico(inputTamanhoX, "Tamanho X", 1, true, erros));
		dados.put("tamanhoY", lerCampoNumerico(inputTamanhoY, "Tamanho Y", 1, true, erros));

		String peso = comboPeso.getValue();
		if (peso == null || peso.isBlank()) {
			marcarCampoInvalido(comboPeso);
			erros.add("Peso deve ser selecionado.");
		}

		dados.put("arma", comboArma.getValue());
		dados.put("peso", peso);
		dados.put("poderoso", chkPoderoso.isSelected());
		dados.put("propriedades", obterPropriedadesSelecionadas());
		if (idSelecionado != null && bestiarioData.containsKey(idSelecionado)) {
			Map<String, Object> original = bestiarioData.get(idSelecionado);
			dados.put("nomeBaseImagem", original.getOrDefault("nomeBaseImagem", original.get("nome")));
		}

		if (!erros.isEmpty()) {
			throw new IllegalArgumentException("Corrija os campos do Bestiário:\n- " + String.join("\n- ", erros));
		}
		return dados;
	}

	private String lerTextoObrigatorio(TextField campo, String rotulo, List<String> erros) {
		String valor = campo.getText() == null ? "" : campo.getText().trim();
		if (valor.isEmpty()) {
			marcarCampoInvalido(campo);
			erros.add(rotulo + " é obrigatório.");
		}
		return valor;
	}

	private double lerCampoNumerico(TextField campo, String rotulo, double minimo, boolean inteiroObrigatorio,
			List<String> erros) {
		String texto = campo.getText() == null ? "" : campo.getText().trim();
		try {
			double valor = Double.parseDouble(texto);
			if (valor < minimo) {
				marcarCampoInvalido(campo);
				erros.add(rotulo + " deve ser maior ou igual a " + formatarMinimo(minimo) + ".");
			}
			if (inteiroObrigatorio && valor % 1 != 0) {
				marcarCampoInvalido(campo);
				erros.add(rotulo + " deve ser um número inteiro.");
			}
			return valor;
		} catch (NumberFormatException e) {
			marcarCampoInvalido(campo);
			erros.add(rotulo + " deve ser numérico.");
			return minimo;
		}
	}

	private String formatarMinimo(double minimo) {
		if (minimo % 1 == 0) {
			return String.valueOf((int) minimo);
		}
		return String.valueOf(minimo);
	}

	private void limparErrosFormulario() {
		limparCampoInvalido(inputNome);
		limparCampoInvalido(inputRaca);
		limparCampoInvalido(inputGrau);
		limparCampoInvalido(inputVida);
		limparCampoInvalido(inputMana);
		limparCampoInvalido(inputAgi);
		limparCampoInvalido(inputDef);
		limparCampoInvalido(inputXP);
		limparCampoInvalido(inputSegmentos);
		limparCampoInvalido(inputTamanhoX);
		limparCampoInvalido(inputTamanhoY);
		limparCampoInvalido(comboPeso);
	}

	private void marcarCampoInvalido(Control campo) {
		if (!campo.getStyleClass().contains(CLASSE_CAMPO_INVALIDO)) {
			campo.getStyleClass().add(CLASSE_CAMPO_INVALIDO);
		}
	}

	private void limparCampoInvalido(Control campo) {
		campo.getStyleClass().remove(CLASSE_CAMPO_INVALIDO);
	}

	private void configurarListasDePropriedades() {
		listaPropsDisponiveis.setCellFactory(lv -> criarCelulaPropriedade(false));
		listaPropsSelecionadas.setCellFactory(lv -> criarCelulaPropriedade(true));
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

	private ListCell<String> criarCelulaPropriedade(boolean permiteGrau) {
		return new ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setTooltip(null);
					return;
				}

				String nomePropriedade = extrairNomePropriedade(item);
				Tooltip tooltip = new Tooltip(getDescricaoPropriedade(item));
				setTooltip(tooltip);

				if (!permiteGrau) {
					setText(nomePropriedade);
					setGraphic(null);
					return;
				}

				Label nome = new Label(nomePropriedade);
				TextField inputGrauPropriedade = new TextField(extrairGrauInformado(item));
				inputGrauPropriedade.setPromptText("1");
				inputGrauPropriedade.setPrefWidth(46);
				inputGrauPropriedade.setMaxWidth(46);
				inputGrauPropriedade.setTextFormatter(new TextFormatter<String>(mudanca ->
						mudanca.getControlNewText().matches("|[1-9]\\d*") ? mudanca : null));
				inputGrauPropriedade.textProperty().addListener((obs, antigo, novo) -> {
					tooltip.setText(getDescricaoPropriedade(montarPropriedade(nomePropriedade, novo)));
				});
				inputGrauPropriedade.focusedProperty().addListener((obs, tinhaFoco, temFoco) -> {
					if (!temFoco) {
						atualizarPropriedadeComGrau(getIndex(), nomePropriedade, inputGrauPropriedade.getText());
					}
				});
				inputGrauPropriedade.setOnAction(event ->
						atualizarPropriedadeComGrau(getIndex(), nomePropriedade, inputGrauPropriedade.getText()));

				HBox conteudo = new HBox(8, nome, inputGrauPropriedade);
				Tooltip.install(conteudo, tooltip);
				setText(null);
				setGraphic(conteudo);
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
		Set<String> selecionadasSet = selecionadas.stream()
				.map(this::extrairNomePropriedade)
				.collect(Collectors.toCollection(LinkedHashSet::new));
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
		String nomePropriedade = extrairNomePropriedade(selecionada);
		origem.getItems().remove(selecionada);
		if (destino == listaPropsSelecionadas) {
			if (destino.getItems().stream().noneMatch(prop -> extrairNomePropriedade(prop).equals(nomePropriedade))) {
				destino.getItems().add(nomePropriedade);
			}
		} else if (!destino.getItems().contains(nomePropriedade)) {
			destino.getItems().add(nomePropriedade);
		}
	}

	private void atualizarPropriedadeComGrau(int indice, String nomePropriedade, String grau) {
		if (indice < 0 || indice >= listaPropsSelecionadas.getItems().size()) {
			return;
		}
		listaPropsSelecionadas.getItems().set(indice, montarPropriedade(nomePropriedade, grau));
	}

	private String getDescricaoPropriedade(String propriedade) {
		String nomePropriedade = extrairNomePropriedade(propriedade);
		int grau = extrairGrauPropriedade(propriedade);
		String descricao = switch (nomePropriedade) {
			case "IMUNIDADE_CONTROLE" -> "Imunidade a Stun, Lento e Sono.";
			case "IMUNIDADE_DOT" -> "Imunidade a DoTs como Veneno, Sangramento e Queimação.";
			case "EXPLODIR" -> "Ao morrer, cria uma área de fogo de raio 1 por 8 TU.";
			case "EXPLOSIVO" -> "Ao morrer, causa " + (grau * 50) + "% da vida máxima como dano em área de raio 3.";
			case "VAMPIRISMO" -> "Cura " + grau + "% do dano causado.";
			case "BLINDADO" -> "Concede escudo normal de " + (grau * 20) + "% da vida máxima.";
			case "ARMADURADO" -> "Concede escudo normal de " + (grau * 50) + "% da vida máxima.";
			case "REGENERACAO" -> "No início do turno, recupera " + (grau * 10) + "% da vida máxima.";
			case "IMUNE_KNOCKBACK" -> "Ignora movimento forçado.";
			default -> "Propriedade especial do personagem.";
		};
		return descricao + " (Nível " + grau + ").";
	}

	private String extrairNomePropriedade(String propriedade) {
		if (propriedade == null) {
			return "";
		}
		int separador = propriedade.indexOf(':');
		return (separador >= 0 ? propriedade.substring(0, separador) : propriedade).trim();
	}

	private String extrairGrauInformado(String propriedade) {
		if (propriedade == null) {
			return "";
		}
		int separador = propriedade.indexOf(':');
		return separador >= 0 ? propriedade.substring(separador + 1).trim() : "";
	}

	private int extrairGrauPropriedade(String propriedade) {
		String grauInformado = extrairGrauInformado(propriedade);
		if (grauInformado.isBlank()) {
			return 1;
		}
		try {
			int grau = Integer.parseInt(grauInformado);
			return grau > 0 ? grau : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private String montarPropriedade(String nomePropriedade, String grau) {
		String grauLimpo = grau == null ? "" : grau.trim();
		return grauLimpo.isEmpty() ? nomePropriedade : nomePropriedade + ":" + grauLimpo;
	}

	private void carregarImagem(String nomeMonstro) {
		try {
			List<String> nomes = new ArrayList<>();
			nomes.add(nomeMonstro);
			if (idSelecionado != null && bestiarioData != null && bestiarioData.containsKey(idSelecionado)) {
				Map<String, Object> original = bestiarioData.get(idSelecionado);
				Object nomeBaseImagem = original.getOrDefault("nomeBaseImagem", original.get("nome"));
				if (nomeBaseImagem instanceof String) {
					nomes.add((String) nomeBaseImagem);
				}
			}
			Image img = CharacterImageResolver.getTokenPorNomes(nomes, 120, 120);
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
			data.putAll(coletarDadosDoFormulario());

			salvarJsonNoDisco();

			Alert alert = new Alert(Alert.AlertType.INFORMATION, "Monstro salvo com sucesso!");
			alert.show();

			String idAtual = idSelecionado;
			configurarFiltros();
			atualizarLista();
			monstroListView.getSelectionModel().select(idAtual);
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
			String idLimpo = id == null ? "" : id.trim();
			if (idLimpo.isEmpty()) {
				new Alert(Alert.AlertType.ERROR, "ID não pode ficar vazio.").show();
				return;
			}
			if (idLimpo.matches(".*\\s+.*")) {
				new Alert(Alert.AlertType.ERROR, "ID não deve conter espaços.").show();
				return;
			}
			if (bestiarioData.containsKey(idLimpo)) {
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

			bestiarioData.put(idLimpo, novo);
			atualizarLista();
			monstroListView.getSelectionModel().select(idLimpo);
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
	private void onPrepararSpawnClick() {
		try {
			Map<String, Object> dadosCustom = coletarDadosDoFormulario();
			int qtd = (int) sliderSpawnQtd.getValue();

			mainController.entrarModoSpawnMultiploCustom(dadosCustom, qtd);

			stage.setIconified(true);
		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao preparar spawn: " + e.getMessage());
			alert.show();
		}
	}
}
