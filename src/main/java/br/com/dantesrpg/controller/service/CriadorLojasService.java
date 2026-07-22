package br.com.dantesrpg.controller.service;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import br.com.dantesrpg.model.util.FileLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Editor persistente de lojas, baseado no schema consumido por LojaController. */
public class CriadorLojasService {

	private static final String TIPO_PADRAO = "normal";
	private static final List<String> RARIDADES = List.of("COMUM", "INCOMUM", "RARO", "EPICO", "LENDARIO", "UNICO", "MITICO");
	private static final List<String> CATEGORIAS = List.of("Arma", "Armadura", "Amuleto", "Consumível");

	private final Supplier<Map<String, Map<String, Object>>> armorySupplier;
	private final Supplier<Map<String, Map<String, Object>>> itempediaSupplier;
	private final Random aleatorio = new Random();

	private final ObservableList<OfertaEditavel> ofertas = FXCollections.observableArrayList();
	private final Map<String, Map<String, Object>> catalogo = new LinkedHashMap<>();
	private final Map<String, String> categoriasPorItem = new HashMap<>();
	private final Map<String, Spinner<Integer>> regrasRaridade = new LinkedHashMap<>();
	private final Map<String, Spinner<Integer>> regrasCategoria = new LinkedHashMap<>();

	private TextField txtNomeLoja;
	private ComboBox<String> comboModulo;
	private ListView<String> listaAndares;
	private ComboBox<String> comboItemDisponivel;
	private TextField txtBuscaItem;
	private ListView<OfertaEditavel> listaOfertas;
	private Runnable aposSalvar;

	private record OfertaEditavel(String id, double desconto, int preco, String tipoCusto) { }

	public CriadorLojasService(Supplier<Map<String, Map<String, Object>>> armorySupplier,
			Supplier<Map<String, Map<String, Object>>> itempediaSupplier) {
		this.armorySupplier = armorySupplier;
		this.itempediaSupplier = itempediaSupplier;
	}

	public Node criarEditor(Runnable aposSalvar) {
		this.aposSalvar = aposSalvar;
		carregarCatalogo();

		VBox raiz = new VBox(14);
		raiz.setPadding(new Insets(18));
		raiz.getStyleClass().add("bento-card");
		raiz.getChildren().addAll(criarCabecalho(), criarAreaPrincipal(), criarAcoes());
		return raiz;
	}

	private Node criarCabecalho() {
		VBox card = new VBox(10);
		Label titulo = new Label("Criador de Lojas");
		titulo.getStyleClass().add("header-label");
		GridPane campos = new GridPane();
		campos.setHgap(12);
		campos.setVgap(8);

		txtNomeLoja = new TextField();
		txtNomeLoja.setPromptText("Ex.: Mercado da Duna Negra");
		txtNomeLoja.getStyleClass().add("editor-search-field");
		comboModulo = new ComboBox<>(FXCollections.observableArrayList("Padrão", "Overclock", "Loja de Sangue"));
		comboModulo.setValue("Padrão");
		comboModulo.getStyleClass().add("loja-combo");
		ComboBox<String> comboLojas = new ComboBox<>();
		comboLojas.getItems().addAll(listarLojas());
		comboLojas.setPromptText("Carregar loja existente");
		comboLojas.getStyleClass().add("loja-combo");
		comboLojas.setOnAction(evento -> {
			if (comboLojas.getValue() != null) carregarLoja(comboLojas.getValue());
		});
		Button btnNova = new Button("Nova loja");
		btnNova.getStyleClass().add("editor-btn-reset");
		btnNova.setOnAction(evento -> limparEditor());

		campos.add(rotulo("Nome da loja"), 0, 0);
		campos.add(rotulo("Módulo ativo"), 1, 0);
		campos.add(rotulo("Editar loja"), 2, 0);
		campos.add(txtNomeLoja, 0, 1);
		campos.add(comboModulo, 1, 1);
		campos.add(comboLojas, 2, 1);
		campos.add(btnNova, 3, 1);
		card.getChildren().addAll(titulo, campos);
		return card;
	}

	private Node criarAreaPrincipal() {
		HBox area = new HBox(14);
		area.setAlignment(Pos.TOP_LEFT);
		VBox.setVgrow(area, Priority.ALWAYS);
		area.getChildren().addAll(criarPainelPool(), criarPainelOfertas(), criarPainelRegras());
		for (Node painel : area.getChildren()) HBox.setHgrow(painel, Priority.ALWAYS);
		return area;
	}

	private Node criarPainelPool() {
		VBox painel = painel("Pool disponível");
		listaAndares = new ListView<>(FXCollections.observableArrayList(listarAndares()));
		listaAndares.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listaAndares.setPrefHeight(105);
		listaAndares.getSelectionModel().getSelectedItems().addListener(
				(javafx.collections.ListChangeListener<String>) alteracao -> atualizarItensDisponiveis());
		txtBuscaItem = new TextField();
		txtBuscaItem.setPromptText("Filtrar pool por nome...");
		txtBuscaItem.getStyleClass().add("editor-search-field");
		txtBuscaItem.textProperty().addListener((obs, anterior, atual) -> atualizarItensDisponiveis());
		comboItemDisponivel = new ComboBox<>();
		comboItemDisponivel.setMaxWidth(Double.MAX_VALUE);
		Button adicionar = new Button("Adicionar item");
		adicionar.getStyleClass().add("editor-btn-save");
		adicionar.setOnAction(evento -> adicionarItemSelecionado());
		painel.getChildren().addAll(rotulo("Andares da pool (Ctrl para selecionar vários; vazio = todos)"), listaAndares, txtBuscaItem, comboItemDisponivel, adicionar);
		atualizarItensDisponiveis();
		return painel;
	}

	private Node criarPainelOfertas() {
		VBox painel = painel("Itens da loja");
		listaOfertas = new ListView<>(ofertas);
		listaOfertas.setPrefHeight(420);
		listaOfertas.setCellFactory(lista -> criarCelulaOferta());
		VBox.setVgrow(listaOfertas, Priority.ALWAYS);
		Label ajuda = new Label("Arraste os cards para alterar a ordem. Edite preço, desconto e custo diretamente.");
		ajuda.setWrapText(true);
		ajuda.setStyle("-fx-text-fill: #a0a0b0; -fx-font-size: 11px;");
		painel.getChildren().addAll(listaOfertas, ajuda);
		return painel;
	}

	private Node criarPainelRegras() {
		VBox painel = painel("Geração aleatória");
		Label instrucao = new Label("Defina quantidades; cada regra adiciona itens da pool selecionada.");
		instrucao.setWrapText(true);
		instrucao.setStyle("-fx-text-fill: #a0a0b0; -fx-font-size: 11px;");
		GridPane regras = new GridPane();
		regras.setHgap(8);
		regras.setVgap(6);
		int linha = 0;
		for (String raridade : RARIDADES) {
			regras.add(new Label(raridade), 0, linha);
			Spinner<Integer> spinner = spinnerQuantidade();
			regras.add(spinner, 1, linha++);
			regrasRaridade.put(raridade, spinner);
		}
		Label categorias = new Label("Por categoria");
		categorias.getStyleClass().add("editor-section-accent");
		regras.add(categorias, 0, linha++, 2, 1);
		for (String categoria : CATEGORIAS) {
			regras.add(new Label(categoria), 0, linha);
			Spinner<Integer> spinner = spinnerQuantidade();
			regras.add(spinner, 1, linha++);
			regrasCategoria.put(categoria, spinner);
		}
		Button gerar = new Button("Gerar itens aleatórios");
		gerar.getStyleClass().add("editor-btn-save");
		gerar.setOnAction(evento -> gerarItensAleatorios());
		painel.getChildren().addAll(instrucao, regras, gerar);
		return painel;
	}

	private Node criarAcoes() {
		HBox acoes = new HBox(10);
		acoes.setAlignment(Pos.CENTER_RIGHT);
		Button salvar = new Button("Salvar loja");
		salvar.getStyleClass().add("editor-btn-save");
		salvar.setOnAction(evento -> salvarLoja());
		acoes.getChildren().add(salvar);
		return acoes;
	}

	private ListCell<OfertaEditavel> criarCelulaOferta() {
		return new ListCell<>() {
			{
				setOnDragDetected(evento -> {
					if (getItem() == null) return;
					Dragboard quadro = startDragAndDrop(TransferMode.MOVE);
					ClipboardContent conteudo = new ClipboardContent();
					conteudo.putString(Integer.toString(getIndex()));
					quadro.setContent(conteudo);
					evento.consume();
				});
				setOnDragOver(evento -> {
					if (evento.getDragboard().hasString() && getItem() != null) evento.acceptTransferModes(TransferMode.MOVE);
					evento.consume();
				});
				setOnDragDropped(evento -> {
					if (!evento.getDragboard().hasString() || getItem() == null) return;
					int origem = Integer.parseInt(evento.getDragboard().getString());
					int destino = getIndex();
					if (origem != destino) ofertas.add(destino, ofertas.remove(origem));
					evento.setDropCompleted(true);
					evento.consume();
				});
			}

			@Override protected void updateItem(OfertaEditavel oferta, boolean vazio) {
				super.updateItem(oferta, vazio);
				if (vazio || oferta == null) { setGraphic(null); return; }
				VBox card = new VBox(5);
				card.setStyle("-fx-background-color: #252530; -fx-padding: 8; -fx-background-radius: 6;");
				Label nome = new Label(oferta.id() + " · " + categoriasPorItem.getOrDefault(oferta.id(), "Item"));
				nome.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
				TextField preco = new TextField(Integer.toString(oferta.preco()));
				preco.setPrefWidth(72);
				TextField desconto = new TextField(String.format(Locale.ROOT, "%.0f", oferta.desconto() * 100));
				desconto.setPrefWidth(62);
				ComboBox<String> custo = new ComboBox<>(FXCollections.observableArrayList("MOEDAS", "PECADOS", "VIDA_MAXIMA_BASE"));
				custo.setValue(oferta.tipoCusto());
				custo.setVisible("Loja de Sangue".equals(comboModulo.getValue()));
				custo.setManaged(custo.isVisible());
				Button remover = new Button("Remover");
				remover.getStyleClass().add("editor-btn-reset");
				remover.setOnAction(evento -> ofertas.remove(oferta));
				HBox campos = new HBox(6, rotulo("Preço"), preco, rotulo("Desc. %"), desconto, custo, remover);
				preco.setOnAction(evento -> substituirOferta(oferta, preco.getText(), desconto.getText(), custo.getValue()));
				desconto.setOnAction(evento -> substituirOferta(oferta, preco.getText(), desconto.getText(), custo.getValue()));
				preco.focusedProperty().addListener((obs, tinhaFoco, temFoco) -> { if (!temFoco) substituirOferta(oferta, preco.getText(), desconto.getText(), custo.getValue()); });
				desconto.focusedProperty().addListener((obs, tinhaFoco, temFoco) -> { if (!temFoco) substituirOferta(oferta, preco.getText(), desconto.getText(), custo.getValue()); });
				custo.valueProperty().addListener((obs, ant, atual) -> substituirOferta(oferta, preco.getText(), desconto.getText(), atual));
				card.getChildren().addAll(nome, campos);
				setGraphic(card);
			}
		};
	}

	private void substituirOferta(OfertaEditavel anterior, String preco, String desconto, String custo) {
		int indice = ofertas.indexOf(anterior);
		if (indice < 0) return;
		ofertas.set(indice, new OfertaEditavel(anterior.id(), lerDouble(desconto) / 100.0, lerInteiro(preco), custo));
	}

	private void carregarCatalogo() {
		catalogo.clear(); categoriasPorItem.clear();
		adicionarCatalogo(armorySupplier.get(), "Arma");
		adicionarCatalogo(itempediaSupplier.get(), null);
	}

	private void adicionarCatalogo(Map<String, Map<String, Object>> origem, String categoriaPadrao) {
		if (origem == null) return;
		for (Map.Entry<String, Map<String, Object>> entrada : origem.entrySet()) {
			Map<String, Object> dados = entrada.getValue();
			if ("NPC".equalsIgnoreCase(String.valueOf(dados.get("categoria")))) continue;
			String categoria = categoriaPadrao != null ? categoriaPadrao : String.valueOf(dados.getOrDefault("classeItem", "Consumível"));
			catalogo.put(entrada.getKey(), dados);
			categoriasPorItem.put(entrada.getKey(), categoria);
		}
	}

	private void atualizarItensDisponiveis() {
		if (comboItemDisponivel == null) return;
		String busca = txtBuscaItem.getText() == null ? "" : txtBuscaItem.getText().trim().toLowerCase(Locale.ROOT);
		List<String> itens = catalogo.entrySet().stream().filter(entrada -> pertenceAoAndar(entrada.getValue()))
				.map(Map.Entry::getKey).filter(nome -> nome.toLowerCase(Locale.ROOT).contains(busca)).sorted().toList();
		comboItemDisponivel.setItems(FXCollections.observableArrayList(itens));
		if (!itens.isEmpty()) comboItemDisponivel.setValue(itens.get(0));
	}

	private boolean pertenceAoAndar(Map<String, Object> dados) {
		List<String> andaresSelecionados = listaAndares.getSelectionModel().getSelectedItems();
		return andaresSelecionados.isEmpty() || andaresSelecionados.contains(String.valueOf(dados.getOrDefault("andar", "0")));
	}

	private void adicionarItemSelecionado() {
		String id = comboItemDisponivel.getValue();
		if (id == null) return;
		ofertas.add(criarOferta(id));
	}

	private OfertaEditavel criarOferta(String id) {
		int preco = valorBase(id);
		return new OfertaEditavel(id, 0, preco, "MOEDAS");
	}

	private void gerarItensAleatorios() {
		List<String> pool = poolAtual();
		for (Map.Entry<String, Spinner<Integer>> regra : regrasRaridade.entrySet()) {
			adicionarAleatorios(pool.stream().filter(id -> regra.getKey().equalsIgnoreCase(
					String.valueOf(catalogo.get(id).getOrDefault("raridade", "COMUM")))).toList(), regra.getValue().getValue());
		}
		for (Map.Entry<String, Spinner<Integer>> regra : regrasCategoria.entrySet()) {
			adicionarAleatorios(pool.stream().filter(id -> regra.getKey().equalsIgnoreCase(categoriasPorItem.get(id))).toList(), regra.getValue().getValue());
		}
	}

	private void adicionarAleatorios(List<String> candidatos, int quantidade) {
		List<String> disponiveis = new ArrayList<>(candidatos);
		disponiveis.removeAll(ofertas.stream().map(OfertaEditavel::id).toList());
		for (int i = 0; i < quantidade && !disponiveis.isEmpty(); i++) ofertas.add(criarOferta(disponiveis.remove(aleatorio.nextInt(disponiveis.size()))));
	}

	private List<String> poolAtual() {
		return catalogo.entrySet().stream().filter(entrada -> pertenceAoAndar(entrada.getValue())).map(Map.Entry::getKey).toList();
	}

	private void carregarLoja(String nome) {
		try (InputStream entrada = FileLoader.carregarArquivo("/data/Lojas/" + nome + ".json")) {
			if (entrada == null) return;
			Type tipo = new TypeToken<Map<String, Object>>() { }.getType();
			Map<String, Object> dados = new Gson().fromJson(new InputStreamReader(entrada, StandardCharsets.UTF_8), tipo);
			txtNomeLoja.setText(nome);
			String modulo = String.valueOf(dados.getOrDefault("tipo", TIPO_PADRAO));
			comboModulo.setValue("sangue".equalsIgnoreCase(modulo) ? "Loja de Sangue" : "overclock".equalsIgnoreCase(modulo) ? "Overclock" : "Padrão");
			ofertas.clear();
			Object bruto = dados.get("itensOfertados");
			if (bruto instanceof List<?> lista) for (Object objeto : lista) if (objeto instanceof Map<?, ?> mapa) {
				String id = String.valueOf(mapa.get("id"));
				double desconto = mapa.get("desconto") instanceof Number n ? n.doubleValue() : 0;
				int preco = mapa.get("preco") instanceof Number n ? n.intValue() : valorBase(id);
				String custo = mapa.containsKey("tipoCusto") ? String.valueOf(mapa.get("tipoCusto")) : "MOEDAS";
				ofertas.add(new OfertaEditavel(id, desconto, preco, custo));
			}
		} catch (Exception ex) { mostrarAlerta(Alert.AlertType.ERROR, "Não foi possível carregar a loja", ex.getMessage()); }
	}

	private void salvarLoja() {
		String nome = txtNomeLoja.getText() == null ? "" : txtNomeLoja.getText().trim();
		if (nome.isEmpty()) { mostrarAlerta(Alert.AlertType.ERROR, "Nome obrigatório", "Informe o nome da loja."); return; }
		Map<String, Object> dados = new LinkedHashMap<>();
		String modulo = comboModulo.getValue();
		if ("Overclock".equals(modulo)) dados.put("tipo", "overclock");
		if ("Loja de Sangue".equals(modulo)) dados.put("tipo", "sangue");
		List<Map<String, Object>> itens = new ArrayList<>();
		for (OfertaEditavel oferta : ofertas) {
			Map<String, Object> item = new LinkedHashMap<>(); item.put("id", oferta.id());
			if ("Loja de Sangue".equals(modulo)) { item.put("tipoCusto", oferta.tipoCusto()); item.put("preco", oferta.preco()); }
			else item.put("desconto", calcularDescontoParaPreco(oferta));
			itens.add(item);
		}
		dados.put("itensOfertados", itens);
		File arquivo = new File(System.getProperty("user.dir"), "src/main/resources/data/Lojas/" + nome + ".json");
		try { arquivo.getParentFile().mkdirs(); try (Writer escritor = new FileWriter(arquivo, StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(dados, escritor); }
			mostrarAlerta(Alert.AlertType.INFORMATION, "Loja salva", "'" + nome + "' foi salva com " + itens.size() + " itens."); if (aposSalvar != null) aposSalvar.run();
		} catch (Exception ex) { mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar loja", ex.getMessage()); }
	}

	private double calcularDescontoParaPreco(OfertaEditavel oferta) { int base = valorBase(oferta.id()); return base <= 0 ? oferta.desconto() : 1.0 - ((double) oferta.preco() / base); }
	private int valorBase(String id) { Object valor = catalogo.getOrDefault(id, Map.of()).get("valorMoedas"); return valor instanceof Number numero ? numero.intValue() : 0; }
	private List<String> listarAndares() { return catalogo.values().stream().map(dados -> String.valueOf(dados.getOrDefault("andar", "0"))).distinct().sorted(Comparator.comparingInt(this::andarNumerico)).toList(); }
	private int andarNumerico(String andar) { try { return Integer.parseInt(andar); } catch (NumberFormatException ex) { return Integer.MAX_VALUE; } }
	private List<String> listarLojas() { return FileLoader.listarArquivosDeDiretorio("/data/Lojas/", ".json").stream().map(nome -> nome.replaceFirst("(?i)\\.json$", "")).sorted().toList(); }
	private void limparEditor() { txtNomeLoja.clear(); comboModulo.setValue("Padrão"); ofertas.clear(); }
	private Spinner<Integer> spinnerQuantidade() { Spinner<Integer> spinner = new Spinner<>(0, 99, 0); spinner.setEditable(true); spinner.setPrefWidth(70); return spinner; }
	private VBox painel(String titulo) { VBox painel = new VBox(9); painel.setPadding(new Insets(10)); painel.setStyle("-fx-background-color: #1b1b25; -fx-background-radius: 8;"); Label label = new Label(titulo); label.getStyleClass().add("editor-section-accent"); painel.getChildren().add(label); return painel; }
	private Label rotulo(String texto) { Label label = new Label(texto); label.getStyleClass().add("editor-field-label"); return label; }
	private int lerInteiro(String valor) { try { return Math.max(0, Integer.parseInt(valor)); } catch (NumberFormatException ex) { return 0; } }
	private double lerDouble(String valor) { try { return Double.parseDouble(valor.replace(',', '.')); } catch (NumberFormatException ex) { return 0; } }
	private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) { Alert alerta = new Alert(tipo, mensagem); alerta.setTitle(titulo); alerta.setHeaderText(null); alerta.showAndWait(); }
}
