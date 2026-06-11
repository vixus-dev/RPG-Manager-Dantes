package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Inventario;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.items.Consumivel;
import br.com.dantesrpg.model.util.FileLoader;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.HabilidadeFactory;
import javafx.util.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LojaController {

	@FXML private HBox lojaHeader;
	@FXML private Label labelNomeLoja;
	@FXML private Label labelTipoLoja;
	@FXML private Label labelMoedasOuro;
	@FXML private Label labelMoedasPrata;
	@FXML private Label labelMoedasBronze;
	@FXML private VBox itensVendaContainer;
	@FXML private VBox inventarioJogadorContainer;
	@FXML private ComboBox<Personagem> playerSelectorComboBox;
	@FXML private ComboBox<String> comboSelecaoLoja;
	@FXML private TextField txtBusca;
	@FXML private ComboBox<String> comboFiltroCategoria;

	@FXML private Label previewNome;
	@FXML private Label previewDescricao;
	@FXML private VBox previewStatsPane;

	private Personagem jogadorAtual;
	private CombatController mainController;
	private List<Oferta> ofertasAtuais = new ArrayList<>();
	private boolean modoOverclock = false;
	private HBox cardOfertaSelecionado;

	private class Oferta {
		Item item;
		double desconto;

		public Oferta(Item item, double desconto) {
			this.item = item;
			this.desconto = desconto;
		}

		public int getPrecoFinal() {
			return (int) (item.getValorMoedas() * (1.0 - desconto));
		}
	}

	@FXML
	public void initialize() {
		playerSelectorComboBox.setCellFactory(lv -> new ListCell<Personagem>() {
			@Override
			protected void updateItem(Personagem p, boolean empty) {
				super.updateItem(p, empty);
				if (empty || p == null) {
					setText(null);
				} else {
					String info = p.getNome();
					if (p.getClasse() != null) info += " (" + p.getClasse().getNome() + ")";
					setText(info);
				}
			}
		});
		playerSelectorComboBox.setButtonCell(new ListCell<Personagem>() {
			@Override
			protected void updateItem(Personagem p, boolean empty) {
				super.updateItem(p, empty);
				if (empty || p == null) {
					setText(null);
				} else {
					String info = p.getNome();
					if (p.getClasse() != null) info += " (" + p.getClasse().getNome() + ")";
					setText(info);
				}
			}
		});

		comboSelecaoLoja.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				carregarLoja(newVal);
				atualizarUI();
			}
		});

		// Configuração de Filtros e Busca
		comboFiltroCategoria.getItems().addAll("Todas", "Armas ⚔️", "Armaduras 🛡️", "Amuletos 💠", "Consumíveis 🧪", "Outros 📦");
		comboFiltroCategoria.setValue("Todas");
		comboFiltroCategoria.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			atualizarUI();
		});

		txtBusca.textProperty().addListener((obs, oldVal, newVal) -> {
			atualizarUI();
		});
	}

	public void inicializarLoja(CombatController controller, EstadoCombate estado, String idLojaInicial) {
		this.mainController = controller;

		playerSelectorComboBox.getItems().clear();
		List<Personagem> players = estado.getCombatentes().stream().filter(p -> p.getFaccao().equals("JOGADOR"))
				.collect(Collectors.toList());
		playerSelectorComboBox.getItems().addAll(players);

		if (!players.isEmpty()) {
			this.jogadorAtual = players.get(0);
			playerSelectorComboBox.setValue(this.jogadorAtual);
		}

		carregarListaDeArquivosDeLoja();

		if (idLojaInicial != null && comboSelecaoLoja.getItems().contains(idLojaInicial)) {
			comboSelecaoLoja.getSelectionModel().select(idLojaInicial);
		} else if (!comboSelecaoLoja.getItems().isEmpty()) {
			comboSelecaoLoja.getSelectionModel().select(0);
		}
	}

	private void carregarListaDeArquivosDeLoja() {
		comboSelecaoLoja.getItems().clear();
		try {
			URL url = getClass().getResource("/data/lojas/");
			if (url != null) {
				File folder = new File(url.toURI());
				File[] listOfFiles = folder.listFiles();
				if (listOfFiles != null) {
					for (File file : listOfFiles) {
						if (file.isFile() && file.getName().endsWith(".json")) {
							comboSelecaoLoja.getItems().add(file.getName().replace(".json", ""));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onPlayerSelected() {
		Personagem selected = playerSelectorComboBox.getValue();
		if (selected != null && selected != this.jogadorAtual) {
			this.jogadorAtual = selected;
			atualizarUI();
		}
	}

	private void carregarLoja(String nomeArquivo) {
		this.ofertasAtuais.clear();
		this.modoOverclock = false;
		Gson gson = new Gson();
		String resourcePath = "/data/lojas/" + nomeArquivo + ".json";

		try (InputStreamReader reader = new InputStreamReader(FileLoader.carregarArquivo(resourcePath),
				StandardCharsets.UTF_8)) {
			Type mapType = new TypeToken<Map<String, Object>>() {
			}.getType();
			Map<String, Object> data = gson.fromJson(reader, mapType);

			String tipoLoja = (String) data.getOrDefault("tipo", "normal");
			if ("overclock".equalsIgnoreCase(tipoLoja)) {
				this.modoOverclock = true;
			}

			List<Map<String, Object>> listaItens = (List<Map<String, Object>>) data.get("itensOfertados");

			if (listaItens != null) {
				for (Map<String, Object> entry : listaItens) {
					String idItem = (String) entry.get("id");
					double desconto = 0.0;
					if (entry.containsKey("desconto")) {
						desconto = ((Number) entry.get("desconto")).doubleValue();
					}

					Item itemModelo = mainController.getItem(idItem);
					if (itemModelo != null) {
						this.ofertasAtuais.add(new Oferta(itemModelo, desconto));
					}
				}
			}

		} catch (Exception e) {
			System.err.println("Erro ao carregar loja: " + nomeArquivo);
			e.printStackTrace();
		}
	}

	// =============================================
	// === ATUALIZAÇÃO DA UI
	// =============================================

	private void atualizarUI() {
		itensVendaContainer.getChildren().clear();
		inventarioJogadorContainer.getChildren().clear();
		cardOfertaSelecionado = null;

		if (jogadorAtual == null) return;

		atualizarHeader();
		atualizarMoedas();
		atualizarCatalogo();
		atualizarInventarioJogador();
	}

	private void atualizarHeader() {
		String nomeLoja = comboSelecaoLoja.getValue();
		if (nomeLoja != null) {
			labelNomeLoja.setText(nomeLoja.replace("_", " "));
		}

		// Badge de tipo
		if (modoOverclock) {
			labelTipoLoja.setText("\u26A1 OVERCLOCK");
			labelTipoLoja.getStyleClass().removeAll("loja-badge-normal", "loja-badge-overclock");
			labelTipoLoja.getStyleClass().add("loja-badge-overclock");
		} else {
			labelTipoLoja.setText("COMPRA & VENDA");
			labelTipoLoja.getStyleClass().removeAll("loja-badge-normal", "loja-badge-overclock");
			labelTipoLoja.getStyleClass().add("loja-badge-normal");
		}
	}

	private void atualizarMoedas() {
		Inventario inv = jogadorAtual.getInventario();
		labelMoedasOuro.setText("\u2B50 " + inv.getMoedasOuro() + " Ouro");
		labelMoedasPrata.setText("\u25C9 " + inv.getMoedasPrata() + " Prata");
		labelMoedasBronze.setText("\u25CF " + inv.getMoedasBronze() + " Bronze");
	}

	// =============================================
	// === CATÁLOGO DE ITENS À VENDA
	// =============================================

	private void atualizarCatalogo() {
		String busca = txtBusca.getText() != null ? txtBusca.getText().trim().toLowerCase() : "";
		String categoriaSelecionada = comboFiltroCategoria.getValue() != null ? comboFiltroCategoria.getValue() : "Todas";

		List<Oferta> ofertasFiltradas = ofertasAtuais.stream().filter(o -> {
			boolean matchesBusca = busca.isEmpty() 
				|| (o.item.getNome() != null && o.item.getNome().toLowerCase().contains(busca))
				|| (o.item.getDescricao() != null && o.item.getDescricao().toLowerCase().contains(busca));

			if (!matchesBusca) return false;

			if ("Todas".equals(categoriaSelecionada)) {
				return true;
			} else if ("Armas ⚔️".equals(categoriaSelecionada)) {
				return o.item instanceof Arma;
			} else if ("Armaduras 🛡️".equals(categoriaSelecionada)) {
				return o.item instanceof Armadura;
			} else if ("Amuletos 💠".equals(categoriaSelecionada)) {
				return o.item instanceof Amuleto;
			} else if ("Consumíveis 🧪".equals(categoriaSelecionada)) {
				return o.item instanceof Consumivel;
			} else if ("Outros 📦".equals(categoriaSelecionada)) {
				return !(o.item instanceof Arma) && !(o.item instanceof Armadura) 
					&& !(o.item instanceof Amuleto) && !(o.item instanceof Consumivel);
			}
			return true;
		}).collect(Collectors.toList());

		if (ofertasFiltradas.isEmpty()) {
			Label lblVazio = new Label(ofertasAtuais.isEmpty() ? "Nenhum item disponível nesta loja." : "Nenhum item corresponde aos filtros.");
			lblVazio.setStyle("-fx-text-fill: #505060; -fx-font-style: italic;");
			itensVendaContainer.getChildren().add(lblVazio);
			return;
		}

		// Categorizar itens
		List<Oferta> armas = new ArrayList<>();
		List<Oferta> armaduras = new ArrayList<>();
		List<Oferta> amuletos = new ArrayList<>();
		List<Oferta> consumiveis = new ArrayList<>();
		List<Oferta> outros = new ArrayList<>();

		for (Oferta o : ofertasFiltradas) {
			if (o.item instanceof Arma) armas.add(o);
			else if (o.item instanceof Armadura) armaduras.add(o);
			else if (o.item instanceof Amuleto) amuletos.add(o);
			else if (o.item instanceof Consumivel) consumiveis.add(o);
			else outros.add(o);
		}

		adicionarCategoria("\u2694 Armas", armas);
		adicionarCategoria("\uD83D\uDEE1 Armaduras", armaduras);
		adicionarCategoria("\uD83D\uDCAE Amuletos", amuletos);
		adicionarCategoria("\uD83E\uDDEA Consumíveis", consumiveis);
		adicionarCategoria("\uD83D\uDCE6 Outros", outros);
	}

	private void adicionarCategoria(String titulo, List<Oferta> ofertas) {
		if (ofertas.isEmpty()) return;

		Label lblCategoria = new Label(titulo);
		lblCategoria.getStyleClass().add("loja-categoria-header");
		lblCategoria.setMaxWidth(Double.MAX_VALUE);
		itensVendaContainer.getChildren().add(lblCategoria);

		for (Oferta oferta : ofertas) {
			itensVendaContainer.getChildren().add(criarCardOferta(oferta));
		}
	}

	private HBox criarCardOferta(Oferta oferta) {
		HBox card = new HBox(8);
		card.getStyleClass().add("loja-card-oferta");
		card.setAlignment(Pos.CENTER_LEFT);

		card.setOnMouseClicked(e -> {
			popularPreview(oferta.item);
			selecionarCardOferta(card);
		});

		// Ícone de tipo
		Label lblIcone = new Label(getIconeTipo(oferta.item));
		lblIcone.getStyleClass().add("loja-tipo-icone");

		// Nome com cor de raridade
		Label lblNome = new Label(oferta.item.getNomeComOverclock());
		lblNome.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + getCorRaridadeItem(oferta.item) + ";");
		if (oferta.item.isOverclockado() || oferta.item.getNome().contains("Overclock")) {
			lblNome.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold; -fx-font-size: 12px; "
					+ "-fx-effect: dropshadow(gaussian, cyan, 3, 0.2, 0, 0);");
		}

		// Badge de desconto
		HBox nomeBox = new HBox(6);
		nomeBox.setAlignment(Pos.CENTER_LEFT);
		nomeBox.getChildren().add(lblNome);

		if (oferta.desconto > 0) {
			Label lblDesconto = new Label("-" + (int) (oferta.desconto * 100) + "%");
			lblDesconto.getStyleClass().addAll("loja-badge-desconto", getBadgeDescontoClass(oferta.desconto));
			nomeBox.getChildren().add(lblDesconto);
		} else if (oferta.desconto < 0) {
			Label lblAcrescimo = new Label("+" + (int) (Math.abs(oferta.desconto) * 100) + "%");
			lblAcrescimo.getStyleClass().addAll("loja-badge-desconto", "loja-badge-desconto-aumento");
			nomeBox.getChildren().add(lblAcrescimo);
		}

		HBox.setHgrow(nomeBox, Priority.ALWAYS);

		// Preço
		int precoFinal = oferta.getPrecoFinal();
		String moedaTipo = oferta.item.getTipoMoeda();
		Label lblPreco = criarLabelPreco(precoFinal, moedaTipo);

		// Botão Comprar
		Button btnComprar = new Button("Comprar");
		btnComprar.getStyleClass().add("loja-btn-comprar");

		// Seletor de Quantidade
		int[] qtdHolder = { 1 };
		HBox seletorQtdBox = new HBox(4);
		seletorQtdBox.setAlignment(Pos.CENTER);
		
		Button btnMenos = new Button("-");
		btnMenos.getStyleClass().add("loja-btn-qtd");
		
		Label lblQtd = new Label("1");
		lblQtd.getStyleClass().add("loja-lbl-qtd");
		
		Button btnMais = new Button("+");
		btnMais.getStyleClass().add("loja-btn-qtd");
		
		seletorQtdBox.getChildren().addAll(btnMenos, lblQtd, btnMais);

		java.util.function.Consumer<Integer> atualizarQtd = (novaQtd) -> {
			int total = precoFinal * novaQtd;
			boolean ok = verificarPodeComprar(total, moedaTipo);
			btnComprar.setDisable(!ok);
			if (!ok) {
				Tooltip tp = new Tooltip("Moedas insuficientes! Exige " + total + " " + traduzirMoeda(moedaTipo));
				tp.setShowDelay(Duration.millis(300));
				btnComprar.setTooltip(tp);
			} else {
				btnComprar.setTooltip(null);
			}
		};

		btnMenos.setOnAction(e -> {
			if (qtdHolder[0] > 1) {
				qtdHolder[0]--;
				lblQtd.setText(String.valueOf(qtdHolder[0]));
				atualizarQtd.accept(qtdHolder[0]);
			}
		});

		btnMais.setOnAction(e -> {
			if (qtdHolder[0] < 99) {
				qtdHolder[0]++;
				lblQtd.setText(String.valueOf(qtdHolder[0]));
				atualizarQtd.accept(qtdHolder[0]);
			}
		});

		btnComprar.setOnAction(e -> comprarItem(oferta, qtdHolder[0]));

		boolean podeComprar = verificarPodeComprar(precoFinal, moedaTipo);
		btnComprar.setDisable(!podeComprar);
		if (!podeComprar) {
			Tooltip tp = new Tooltip("Moedas insuficientes!");
			tp.setShowDelay(Duration.millis(300));
			btnComprar.setTooltip(tp);
		}

		if (!modoOverclock) {
			Button btnBarganhar = new Button("Barganhar");
			btnBarganhar.getStyleClass().add("loja-btn-barganhar");
			btnBarganhar.setOnAction(e -> abrirModalBarganha(oferta, qtdHolder[0]));
			card.getChildren().addAll(lblIcone, nomeBox, lblPreco, seletorQtdBox, btnBarganhar, btnComprar);
		} else {
			card.getChildren().addAll(lblIcone, nomeBox, lblPreco, seletorQtdBox, btnComprar);
		}

		// Borda com cor de desconto
		if (oferta.desconto > 0) {
			String corBorda = getCorDesconto(oferta.desconto);
			card.setStyle(card.getStyle() + "-fx-border-color: " + corBorda + ";");
		}

		return card;
	}

	private void selecionarCardOferta(HBox novoCard) {
		if (cardOfertaSelecionado != null) {
			cardOfertaSelecionado.getStyleClass().remove("loja-card-oferta-selected");
		}
		novoCard.getStyleClass().add("loja-card-oferta-selected");
		cardOfertaSelecionado = novoCard;
	}

	// =============================================
	// === INVENTÁRIO DO JOGADOR
	// =============================================

	private void atualizarInventarioJogador() {
		Inventario inv = jogadorAtual.getInventario();

		if (modoOverclock) {
			// Token count
			int tokensDisponiveis = contarTokensOverclock();
			Label lblTokens = new Label("\u26A1 Tokens de Overclock: " + tokensDisponiveis);
			lblTokens.getStyleClass().add("loja-token-count");
			lblTokens.setMaxWidth(Double.MAX_VALUE);
			inventarioJogadorContainer.getChildren().add(lblTokens);

			// Equipamento atual
			adicionarSeparadorCategoria("Equipado");

			if (jogadorAtual.getArmaEquipada() != null) {
				inventarioJogadorContainer.getChildren().add(
						criarCardOverclock(jogadorAtual.getArmaEquipada(), "Arma"));
			}
			if (jogadorAtual.getArmaduraEquipada() != null) {
				inventarioJogadorContainer.getChildren().add(
						criarCardOverclock(jogadorAtual.getArmaduraEquipada(), "Armadura"));
			}
			if (jogadorAtual.getAmuleto1() != null) {
				inventarioJogadorContainer.getChildren().add(
						criarCardOverclock(jogadorAtual.getAmuleto1(), "Amuleto 1"));
			}
			if (jogadorAtual.getAmuleto2() != null) {
				inventarioJogadorContainer.getChildren().add(
						criarCardOverclock(jogadorAtual.getAmuleto2(), "Amuleto 2"));
			}

			// Itens do inventário
			Map<String, Integer> inventarioAgrupado = inv.getItensAgrupados();
			boolean temEquipamentoNoInv = false;
			for (Map.Entry<String, Integer> entry : inventarioAgrupado.entrySet()) {
				Item itemModelo = mainController.getItem(entry.getKey());
				if (itemModelo != null) {
					int ocGrau = inv.getOverclockDoItem(entry.getKey());
					if (ocGrau > 0) itemModelo.setGrauOverclock(ocGrau);
				}
				if (itemModelo != null && (itemModelo instanceof Arma || itemModelo instanceof Armadura || itemModelo instanceof Amuleto)) {
					if (!temEquipamentoNoInv) {
						adicionarSeparadorCategoria("Inventário");
						temEquipamentoNoInv = true;
					}
					String slotLabel = "Inv";
					if (itemModelo instanceof Arma) slotLabel = "Arma (Inv)";
					else if (itemModelo instanceof Armadura) slotLabel = "Armad. (Inv)";
					else if (itemModelo instanceof Amuleto) slotLabel = "Amul. (Inv)";
					inventarioJogadorContainer.getChildren().add(
							criarCardOverclock(itemModelo, slotLabel));
				}
			}
		} else {
			// Modo loja normal
			Map<String, Integer> inventarioAgrupado = inv.getItensAgrupados();
			if (inventarioAgrupado.isEmpty()) {
				Label lblVazio = new Label("Inventário vazio.");
				lblVazio.setStyle("-fx-text-fill: #505060; -fx-font-style: italic;");
				inventarioJogadorContainer.getChildren().add(lblVazio);
			} else {
				for (Map.Entry<String, Integer> entry : inventarioAgrupado.entrySet()) {
					Item itemModelo = mainController.getItem(entry.getKey());
					if (itemModelo != null) {
						int ocGrau = inv.getOverclockDoItem(entry.getKey());
						if (ocGrau > 0) itemModelo.setGrauOverclock(ocGrau);
						inventarioJogadorContainer.getChildren().add(criarCardVendaJogador(itemModelo, entry.getValue()));
					}
				}
			}
		}
	}

	private void adicionarSeparadorCategoria(String titulo) {
		Label lbl = new Label(titulo);
		lbl.getStyleClass().add("loja-categoria-header");
		lbl.setMaxWidth(Double.MAX_VALUE);
		inventarioJogadorContainer.getChildren().add(lbl);
	}

	private HBox criarCardVendaJogador(Item item, int quantidade) {
		HBox card = new HBox(8);
		card.getStyleClass().add("loja-card-inventario");
		card.setAlignment(Pos.CENTER_LEFT);
		card.setOnMouseClicked(e -> popularPreview(item));

		// Ícone
		Label lblIcone = new Label(getIconeTipo(item));
		lblIcone.getStyleClass().add("loja-tipo-icone");

		// Nome
		String nomeExibicao = item.isOverclockado() ? item.getNomeComOverclock() : item.getNome();
		Label lblNome = new Label(nomeExibicao);
		lblNome.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getCorRaridadeItem(item) + ";");
		if (item.isOverclockado()) {
			lblNome.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold; -fx-font-size: 12px; "
					+ "-fx-effect: dropshadow(gaussian, cyan, 3, 0.2, 0, 0);");
		}

		// Quantidade
		Label lblQtd = new Label("x" + quantidade);
		lblQtd.setStyle("-fx-text-fill: #808090; -fx-font-size: 11px;");

		HBox nomeBox = new HBox(6);
		nomeBox.setAlignment(Pos.CENTER_LEFT);
		nomeBox.getChildren().addAll(lblNome, lblQtd);
		HBox.setHgrow(nomeBox, Priority.ALWAYS);

		// Botão Vender
		Button btnVender = new Button("Vender");
		btnVender.getStyleClass().add("loja-btn-vender");
		btnVender.setOnAction(e -> venderItemManual(item));

		card.getChildren().addAll(lblIcone, nomeBox, btnVender);
		return card;
	}

	// =============================================
	// === OVERCLOCK
	// =============================================

	private int contarTokensOverclock() {
		Map<String, Integer> itens = jogadorAtual.getInventario().getItensAgrupados();
		return itens.getOrDefault("TokenDeOverclock", 0);
	}

	private void consumirTokenOverclock() {
		Item tokenModelo = mainController.getItem("TokenDeOverclock");
		if (tokenModelo != null) {
			jogadorAtual.getInventario().removerItem(tokenModelo);
		}
	}

	private VBox criarCardOverclock(Item equipamento, String slotNome) {
		VBox card = new VBox(4);
		card.getStyleClass().add("loja-card-overclock");
		card.setOnMouseClicked(e -> popularPreview(equipamento));

		int grauAtual = equipamento.getGrauOverclock();
		boolean isMax = grauAtual >= Item.OVERCLOCK_MAXIMO;

		// Header: slot + nome
		HBox headerRow = new HBox(8);
		headerRow.setAlignment(Pos.CENTER_LEFT);

		Label lblSlot = new Label("[" + slotNome + "]");
		lblSlot.setStyle("-fx-text-fill: #606070; -fx-font-weight: bold; -fx-font-size: 10px;");

		String nomeExibicao = equipamento.isOverclockado() ? equipamento.getNomeComOverclock() : equipamento.getNome();
		Label lblNome = new Label(nomeExibicao);
		if (equipamento.isOverclockado()) {
			lblNome.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold; -fx-font-size: 12px; "
					+ "-fx-effect: dropshadow(gaussian, cyan, 3, 0.2, 0, 0);");
		} else {
			lblNome.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
		}
		HBox.setHgrow(lblNome, Priority.ALWAYS);

		headerRow.getChildren().addAll(lblSlot, lblNome);

		// Barra de progresso do overclock
		StackPane barraOC = criarBarraOverclock(grauAtual);

		// Info row: preview + botão
		HBox infoRow = new HBox(8);
		infoRow.setAlignment(Pos.CENTER_LEFT);

		String previewStats = gerarPreviewOverclock(equipamento);
		Label lblPreview = new Label(previewStats);
		lblPreview.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px;");
		HBox.setHgrow(lblPreview, Priority.ALWAYS);

		Button btnOverclock;
		if (isMax) {
			btnOverclock = new Button("MAX");
			btnOverclock.getStyleClass().add("loja-btn-overclock-max");
			btnOverclock.setDisable(true);
		} else {
			btnOverclock = new Button("\u26A1 Overclockar");
			btnOverclock.getStyleClass().add("loja-btn-overclock");
			boolean temToken = contarTokensOverclock() > 0;
			btnOverclock.setDisable(!temToken);
			if (!temToken) {
				Tooltip tp = new Tooltip("Sem Tokens de Overclock!");
				tp.setShowDelay(Duration.millis(300));
				btnOverclock.setTooltip(tp);
			}
			btnOverclock.setOnAction(e -> executarOverclock(equipamento, slotNome));
		}

		infoRow.getChildren().addAll(lblPreview, btnOverclock);

		card.getChildren().addAll(headerRow, barraOC, infoRow);
		return card;
	}

	private StackPane criarBarraOverclock(int grauAtual) {
		StackPane container = new StackPane();
		container.setPrefHeight(12);
		container.setMaxWidth(Double.MAX_VALUE);

		Region track = new Region();
		track.getStyleClass().add("overclock-bar-track");
		track.setMaxWidth(Double.MAX_VALUE);
		track.setPrefHeight(12);

		double percentual = grauAtual / (double) Item.OVERCLOCK_MAXIMO;
		boolean isMax = grauAtual >= Item.OVERCLOCK_MAXIMO;

		Region fill = new Region();
		fill.getStyleClass().add(isMax ? "overclock-bar-fill-max" : "overclock-bar-fill");
		fill.setPrefHeight(8);
		fill.setMaxHeight(8);
		fill.maxWidthProperty().bind(container.widthProperty().multiply(percentual).subtract(4));
		fill.setMinWidth(0);
		StackPane.setAlignment(fill, Pos.CENTER_LEFT);
		StackPane.setMargin(fill, new Insets(2));

		Label texto = new Label(grauAtual + "/" + Item.OVERCLOCK_MAXIMO);
		texto.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; "
				+ "-fx-effect: dropshadow(gaussian, black, 2, 0.8, 0, 0);");

		container.getChildren().addAll(track, fill, texto);
		return container;
	}

	private String gerarPreviewOverclock(Item equipamento) {
		int grauAtual = equipamento.getGrauOverclock();
		if (grauAtual >= Item.OVERCLOCK_MAXIMO) return "Overclock MAXIMO!";

		double multProximo = 1.0 + Item.OVERCLOCK_BONUS_POR_GRAU * (grauAtual + 1);

		if (equipamento instanceof Arma) {
			Arma arma = (Arma) equipamento;
			int danoAtual = arma.getDanoBase();
			int danoProximo = (int) Math.round(arma.getDanoBaseOriginal() * multProximo);
			return "Dano: " + danoAtual + " \u2192 " + danoProximo;
		} else if (equipamento instanceof Armadura) {
			Armadura arm = (Armadura) equipamento;
			int defAtual = arm.getArmaduraBase();
			int defProximo = (int) Math.round(arm.getArmaduraBaseOriginal() * multProximo);
			return "Defesa: " + defAtual + " \u2192 " + defProximo;
		} else if (equipamento instanceof Amuleto) {
			Amuleto amu = (Amuleto) equipamento;
			int defAtual = amu.getArmaduraBonus();
			int defProximo = (int) Math.round(amu.getArmaduraBonusOriginal() * multProximo);
			if (amu.getArmaduraBonusOriginal() > 0) {
				return "Defesa: " + defAtual + " \u2192 " + defProximo;
			}
			return "+" + (int) (multProximo * 100) + "% nos modificadores";
		}
		return "";
	}

	private void executarOverclock(Item equipamento, String slotNome) {
		if (equipamento.getGrauOverclock() >= Item.OVERCLOCK_MAXIMO) {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Este equipamento já está no grau máximo de Overclock!");
			alert.show();
			return;
		}

		if (contarTokensOverclock() <= 0) {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Você não possui Tokens de Overclock!");
			alert.show();
			return;
		}

		boolean veioDoInventario = slotNome.contains("Inv");
		if (veioDoInventario) {
			equiparItemDoInventario(equipamento);
		}

		int grauAnterior = equipamento.getGrauOverclock();
		int novoGrau = grauAnterior + 1;

		consumirTokenOverclock();
		equipamento.setGrauOverclock(novoGrau);

		mainController.salvarEstadoJogadores();
		atualizarUI();

		Alert info = new Alert(Alert.AlertType.INFORMATION);
		info.setTitle("Overclock Realizado!");
		info.setHeaderText("\u26A1 " + equipamento.getNomeComOverclock());
		String msgExtra = veioDoInventario ? "\nItem foi equipado automaticamente." : "";
		info.setContentText("Grau " + grauAnterior + " \u2192 " + novoGrau
				+ "\nBônus total: +" + (int) (novoGrau * Item.OVERCLOCK_BONUS_POR_GRAU * 100) + "% nos atributos base"
				+ msgExtra);
		info.show();
	}

	private void equiparItemDoInventario(Item equipamento) {
		Inventario inv = jogadorAtual.getInventario();

		if (equipamento instanceof Arma) {
			inv.removerItem(equipamento);
			Arma arma = (Arma) equipamento;
			if (!jogadorAtual.equiparArma(arma)) {
				inv.adicionarItem(equipamento);
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Wielding insuficiente");
				alert.setHeaderText("Nao foi possivel equipar " + arma.getNome());
				alert.setContentText("Wielding livre: " + jogadorAtual.getWieldingDisponivel()
						+ " | arma exige: " + arma.getWielding());
				alert.showAndWait();
				return;
			}
		} else if (equipamento instanceof Armadura) {
			Armadura armaduraAtual = jogadorAtual.getArmaduraEquipada();
			inv.removerItem(equipamento);
			if (armaduraAtual != null) inv.adicionarItem(armaduraAtual);
			jogadorAtual.setArmaduraEquipada((Armadura) equipamento);
		} else if (equipamento instanceof Amuleto) {
			Amuleto amuletoAtual = jogadorAtual.getAmuleto1();
			inv.removerItem(equipamento);
			if (amuletoAtual != null) inv.adicionarItem(amuletoAtual);
			jogadorAtual.setAmuleto1((Amuleto) equipamento);
		}
	}

	// =============================================
	// === COMPRA E VENDA
	// =============================================

	private void comprarItem(Oferta oferta, int quantidade) {
		Inventario inv = jogadorAtual.getInventario();
		int precoUnitario = oferta.getPrecoFinal();
		int precoTotal = precoUnitario * quantidade;
		String moedaTipo = oferta.item.getTipoMoeda();

		boolean sucesso = false;

		if ("OURO".equalsIgnoreCase(moedaTipo)) {
			if (inv.gastarOuro(precoTotal)) sucesso = true;
		} else if ("PRATA".equalsIgnoreCase(moedaTipo)) {
			if (inv.gastarPrata(precoTotal)) sucesso = true;
		} else {
			if (inv.gastarBronze(precoTotal)) sucesso = true;
		}

		if (sucesso) {
			for (int i = 0; i < quantidade; i++) {
				if (oferta.item instanceof Consumivel) {
					Map<String, Double> efeitos = ((Consumivel) oferta.item).getEfeitos();
					if (efeitos != null && efeitos.containsKey("RECEBE_PRATA")) {
						inv.receberPrata(efeitos.get("RECEBE_PRATA").intValue());
					} else if (efeitos != null && efeitos.containsKey("RECEBE_OURO")) {
						inv.receberOuro(efeitos.get("RECEBE_OURO").intValue());
					} else {
						inv.adicionarItem(oferta.item);
					}
				} else {
					inv.adicionarItem(oferta.item);
				}
			}

			// Enviar log de transação normal
			String logMsg = String.format("[LOJA] %s comprou %dx %s por %d %s",
					jogadorAtual.getNome(), quantidade, oferta.item.getNome(), precoTotal, traduzirMoeda(moedaTipo));
			br.com.dantesrpg.model.util.SessionLogger.log(logMsg);

			mainController.salvarEstadoJogadores();
			atualizarUI();
		} else {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Moeda incorreta ou insuficiente!");
			alert.show();
		}
	}

	private void abrirModalBarganha(Oferta oferta, int quantidade) {
		if (oferta == null || jogadorAtual == null) return;

		Item item = oferta.item;
		int precoOriginalUnitario = oferta.getPrecoFinal();
		int precoOriginalTotal = precoOriginalUnitario * quantidade;
		String moedaTipo = item.getTipoMoeda();

		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.setTitle("Barganhar - " + item.getNome());
		dialog.setHeaderText("Negociando preço para " + jogadorAtual.getNome());

		DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.setStyle("-fx-background-color: #16161e; -fx-border-color: #e94560; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8;");
		
		try {
			String cssPath = getClass().getResource("/br/com/dantesrpg/view/style.css").toExternalForm();
			dialogPane.getScene().getStylesheets().add(cssPath);
		} catch (Exception e) {
			// Fallback silencioso
		}

		javafx.scene.Node headerPanel = dialogPane.lookup(".header-panel");
		if (headerPanel != null) {
			headerPanel.setStyle("-fx-background-color: #0b0914;");
		}

		VBox content = new VBox(12);
		content.setPadding(new Insets(15));
		content.setAlignment(Pos.CENTER);
		content.setStyle("-fx-background-color: #16161e;");

		Label lblItemNome = new Label(item.getNome() + (quantidade > 1 ? " (x" + quantidade + ")" : ""));
		lblItemNome.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + getCorRaridadeItem(item) + ";");
		
		Label lblPrecoOriginal = new Label(String.format("Preço da Loja: %d %s (Qtd: %d)", precoOriginalTotal, traduzirMoeda(moedaTipo), quantidade));
		lblPrecoOriginal.setStyle("-fx-text-fill: #808090; -fx-font-size: 13px;");

		Slider slider = new Slider(-100, 100, 0);
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit(50);
		slider.setMinorTickCount(4);
		slider.setBlockIncrement(10);
		slider.setStyle("-fx-text-fill: white;");

		TextField txtPorcentagem = new TextField("0");
		txtPorcentagem.setPrefWidth(60);
		txtPorcentagem.setAlignment(Pos.CENTER);
		txtPorcentagem.setStyle("-fx-background-color: #0d0c15; -fx-text-fill: white; -fx-border-color: #3a3a4a; -fx-border-radius: 4; -fx-background-radius: 4;");

		HBox sliderBox = new HBox(10, slider, txtPorcentagem);
		sliderBox.setAlignment(Pos.CENTER);

		Label lblPrecoRecalculado = new Label();
		lblPrecoRecalculado.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");

		Runnable atualizarValores = () -> {
			try {
				double pct = slider.getValue();
				int pctInt = (int) Math.round(pct);
				
				double fator = 1.0 + (pctInt / 100.0);
				int precoFinalUnitario = (int) Math.round(precoOriginalUnitario * fator);
				if (precoFinalUnitario < 0) precoFinalUnitario = 0;
				int precoFinalTotal = precoFinalUnitario * quantidade;

				if (pctInt < 0) {
					lblPrecoRecalculado.setText(String.format("Preço Final: %d %s (Desconto de %d%%)", precoFinalTotal, traduzirMoeda(moedaTipo), Math.abs(pctInt)));
					lblPrecoRecalculado.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
				} else if (pctInt > 0) {
					lblPrecoRecalculado.setText(String.format("Preço Final: %d %s (Acréscimo de %d%%)", precoFinalTotal, traduzirMoeda(moedaTipo), pctInt));
					lblPrecoRecalculado.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
				} else {
					lblPrecoRecalculado.setText(String.format("Preço Final: %d %s (Sem alteração)", precoFinalTotal, traduzirMoeda(moedaTipo)));
					lblPrecoRecalculado.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		slider.valueProperty().addListener((obs, oldVal, newVal) -> {
			int val = (int) Math.round(newVal.doubleValue());
			if (!txtPorcentagem.isFocused()) {
				txtPorcentagem.setText(String.valueOf(val));
			}
			atualizarValores.run();
		});

		txtPorcentagem.textProperty().addListener((obs, oldVal, newVal) -> {
			if (txtPorcentagem.isFocused()) {
				if (newVal.isEmpty()) return;
				try {
					if (newVal.equals("-")) return;
					int val = Integer.parseInt(newVal);
					if (val < -100) val = -100;
					if (val > 100) val = 100;
					slider.setValue(val);
					atualizarValores.run();
				} catch (NumberFormatException e) {
					txtPorcentagem.setText(oldVal);
				}
			}
		});

		txtPorcentagem.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
			if (!isFocused) {
				if (txtPorcentagem.getText().isEmpty() || txtPorcentagem.getText().equals("-")) {
					txtPorcentagem.setText("0");
					slider.setValue(0);
				} else {
					try {
						int val = Integer.parseInt(txtPorcentagem.getText());
						if (val < -100) val = -100;
						if (val > 100) val = 100;
						txtPorcentagem.setText(String.valueOf(val));
						slider.setValue(val);
					} catch (Exception e) {
						txtPorcentagem.setText("0");
						slider.setValue(0);
					}
				}
				atualizarValores.run();
			}
		});

		atualizarValores.run();

		content.getChildren().addAll(lblItemNome, lblPrecoOriginal, sliderBox, lblPrecoRecalculado);
		dialogPane.setContent(content);

		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
		okButton.setText("Confirmar Compra");
		okButton.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");
		
		Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
		cancelButton.setText("Cancelar");
		cancelButton.setStyle("-fx-background-color: #2a1a1a; -fx-text-fill: #e67e22; -fx-border-color: #e67e22; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");

		okButton.setOnMouseEntered(e -> okButton.setStyle("-fx-background-color: #2a5a2a; -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 4; -fx-background-radius: 4;"));
		okButton.setOnMouseExited(e -> okButton.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 4; -fx-background-radius: 4;"));
		cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #3a2a1a; -fx-text-fill: #e67e22; -fx-border-color: #e67e22; -fx-border-radius: 4; -fx-background-radius: 4;"));
		cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #2a1a1a; -fx-text-fill: #e67e22; -fx-border-color: #e67e22; -fx-border-radius: 4; -fx-background-radius: 4;"));

		Optional<ButtonType> result = dialog.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			int pctInt = (int) Math.round(slider.getValue());
			double fator = 1.0 + (pctInt / 100.0);
			int precoFinalUnitario = (int) Math.round(precoOriginalUnitario * fator);
			if (precoFinalUnitario < 0) precoFinalUnitario = 0;
			int precoFinalTotal = precoFinalUnitario * quantidade;

			if (verificarPodeComprar(precoFinalTotal, moedaTipo)) {
				boolean sucesso = false;
				Inventario inv = jogadorAtual.getInventario();

				if ("OURO".equalsIgnoreCase(moedaTipo)) {
					if (inv.gastarOuro(precoFinalTotal)) sucesso = true;
				} else if ("PRATA".equalsIgnoreCase(moedaTipo)) {
					if (inv.gastarPrata(precoFinalTotal)) sucesso = true;
				} else {
					if (inv.gastarBronze(precoFinalTotal)) sucesso = true;
				}

				if (sucesso) {
					for (int i = 0; i < quantidade; i++) {
						if (item instanceof Consumivel) {
							Map<String, Double> efeitos = ((Consumivel) item).getEfeitos();
							if (efeitos != null && efeitos.containsKey("RECEBE_PRATA")) {
								inv.receberPrata(efeitos.get("RECEBE_PRATA").intValue());
							} else if (efeitos != null && efeitos.containsKey("RECEBE_OURO")) {
								inv.receberOuro(efeitos.get("RECEBE_OURO").intValue());
							} else {
								inv.adicionarItem(item);
							}
						} else {
							inv.adicionarItem(item);
						}
					}

					String tipoAlteracao = pctInt < 0 ? "Desconto" : "Acréscimo";
					String logMsg = String.format("[LOJA] %s barganhou e comprou %dx %s por %d %s (%s de %d%%, preço original: %d %s)",
							jogadorAtual.getNome(), quantidade, item.getNome(), precoFinalTotal, traduzirMoeda(moedaTipo),
							tipoAlteracao, Math.abs(pctInt), precoOriginalTotal, traduzirMoeda(moedaTipo));
					br.com.dantesrpg.model.util.SessionLogger.log(logMsg);

					mainController.salvarEstadoJogadores();
					atualizarUI();
				} else {
					mostrarAlertaErro("Erro de Compra", "Não foi possível realizar o débito de moedas.");
				}
			} else {
				mostrarAlertaErro("Moedas Insuficientes", "Você não possui moedas suficientes para comprar este item no preço negociado!");
			}
		}
	}

	private String traduzirMoeda(String moedaTipo) {
		if ("OURO".equalsIgnoreCase(moedaTipo)) return "Ouro";
		if ("PRATA".equalsIgnoreCase(moedaTipo)) return "Prata";
		return "Bronze";
	}

	private void mostrarAlertaErro(String titulo, String mensagem) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(titulo);
		alert.setHeaderText(null);
		alert.setContentText(mensagem);
		alert.getDialogPane().setStyle("-fx-background-color: #16161e; -fx-text-fill: white;");
		alert.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");
		alert.showAndWait();
	}

	private void venderItemManual(Item item) {
		TextInputDialog dialog = new TextInputDialog("0");
		dialog.setTitle("Venda Manual");
		dialog.setHeaderText("Vendendo: " + item.getNome());
		dialog.setContentText("Por quanto (Bronze) você vai vender?");

		dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");
		dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");
		dialog.getEditor().setStyle("-fx-text-fill: black;");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			try {
				int valorVenda = Integer.parseInt(result.get());
				if (valorVenda < 0) valorVenda = 0;

				jogadorAtual.getInventario().removerItem(item);
				jogadorAtual.getInventario().receber(valorVenda);

				mainController.salvarEstadoJogadores();
				atualizarUI();

			} catch (NumberFormatException e) {
				Alert alert = new Alert(Alert.AlertType.ERROR, "Valor inválido!");
				alert.show();
			}
		}
	}

	// =============================================
	// === PREVIEW DE ITEM
	// =============================================

	private void popularPreview(Item item) {
		if (item == null) return;

		// Nome com cor de raridade
		String nomeExibicao = item.isOverclockado() ? item.getNomeComOverclock() : item.getNome();
		previewNome.setText(nomeExibicao);
		String corNome = getCorRaridadeItem(item);
		previewNome.setStyle("-fx-text-fill: " + corNome + "; -fx-font-weight: bold; -fx-font-size: 16px; "
				+ "-fx-border-color: transparent transparent #3a3a4a transparent; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 8 0;");
		if (item.isOverclockado()) {
			previewNome.setStyle(previewNome.getStyle() + " -fx-effect: dropshadow(gaussian, cyan, 4, 0.3, 0, 0);");
		}

		// Descrição
		previewDescricao.setText(item.getDescricao() != null ? item.getDescricao() : "Sem descrição.");

		previewStatsPane.getChildren().clear();

		// Overclock info
		if (item.isOverclockado()) {
			addPreviewStat("Overclock: Grau " + item.getGrauOverclock() + "/" + Item.OVERCLOCK_MAXIMO
					+ " (+" + (int) (item.getGrauOverclock() * Item.OVERCLOCK_BONUS_POR_GRAU * 100) + "%)", "cyan");
			addPreviewSeparator();
		}

		if (item instanceof Arma) {
			renderPreviewArma((Arma) item);
		} else if (item instanceof Armadura) {
			Armadura armadura = (Armadura) item;
			addPreviewStat("Defesa: " + armadura.getArmaduraBase(), "#2ecc71");
			addPreviewSeparator();
			exibirModificadoresPreview(armadura.getModificadoresDeAtributo(), armadura.getModificadoresStatus());
		} else if (item instanceof Amuleto) {
			Amuleto amuleto = (Amuleto) item;
			if (amuleto.getArmaduraBonus() > 0)
				addPreviewStat("Defesa: +" + amuleto.getArmaduraBonus(), "#2ecc71");
			addPreviewSeparator();
			exibirModificadoresPreview(amuleto.getModificadoresDeAtributo(), amuleto.getModificadoresStatus());
		} else if (item instanceof Consumivel) {
			Consumivel cons = (Consumivel) item;
			addPreviewStat("Custo TU: " + cons.getCustoTU(), "#f39c12");
			if (cons.getEfeitos() != null) {
				addPreviewSeparator();
				cons.getEfeitos().forEach((k, v) -> addPreviewStat(traduzirNomeStatus(k) + ": " + formatarNumero(v), "#2ecc71"));
			}
		}
	}

	private void renderPreviewArma(Arma arma) {
		// Atributo multiplicador
		Atributo atr = arma.getAtributoMultiplicador();
		addPreviewStat("Atributo: " + atr.name(), getCorAtributo(atr));

		// Stats base
		addPreviewStat("Dano Base: " + arma.getDanoBase() + " (x" + arma.getTicksDeDano() + ")", "#e74c3c");
		addPreviewStat("Wielding: " + arma.getWielding(), "#cccccc");
		addPreviewStat("Alcance: " + arma.getAlcance(), "#f1c40f");
		addPreviewStat("Custo TU: " + arma.getCustoTU(), "#3498db");

		// Efeito on hit
		if (arma.getNomeEfeitoOnHit() != null) {
			addPreviewSeparator();
			addPreviewStat("Ao Acertar: " + arma.getNomeEfeitoOnHit(), "cyan");
			double chance = arma.getChanceEfeitoOnHit();
			String corChance = chance >= 0.8 ? "#ffd700" : chance >= 0.5 ? "#2ecc71" : chance >= 0.3 ? "#e67e22" : "#e74c3c";
			addPreviewStat("Chance: " + (int)(chance * 100) + "%", corChance);
		}

		// Habilidades
		if (!arma.getHabilidadesConcedidasNomes().isEmpty()) {
			addPreviewSeparator();
			Label lblHeader = new Label("Habilidades");
			lblHeader.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 12px; -fx-underline: true;");
			previewStatsPane.getChildren().add(lblHeader);

			for (String nomeHab : arma.getHabilidadesConcedidasNomes()) {
				Label lblHab = new Label("\u2022 " + nomeHab);
				lblHab.setStyle("-fx-text-fill: #f39c12; -fx-cursor: hand; -fx-font-size: 12px;");

				Habilidade hab = HabilidadeFactory.criarHabilidadePorNome(nomeHab);
				if (hab != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(hab.getNome()).append("\n");
					sb.append(hab.getDescricao()).append("\n\n");
					sb.append("Tipo: ").append(hab.getTipo()).append("\n");

					if (hab.getTipo() == TipoHabilidade.ATIVA) {
						sb.append("Custo: ").append(hab.getCustoMana()).append(" MP | ").append(hab.getCustoTU())
								.append(" TU\n");
						if (hab.getMultiplicadorDeDano() > 0) {
							sb.append("Dano: ").append(String.format("%.0f%%", hab.getMultiplicadorDeDano() * 100)).append("\n");
						}
						sb.append("Alvo: ").append(hab.getTipoAlvo());
						if (hab.getAlcanceMaximo() > 0) {
							sb.append(" (Alcance: ").append(hab.getAlcanceMaximo()).append(")");
						}
						sb.append("\n");
						if (hab.getTamanhoArea() > 0) {
							sb.append("Área: ").append(hab.getTamanhoArea());
							if (hab.getAnguloCone() > 0) {
								sb.append(" (Cone ").append(hab.getAnguloCone()).append("°)");
							}
							sb.append("\n");
						}
					}

					Tooltip tp = new Tooltip(sb.toString());
					tp.setShowDelay(Duration.millis(100));
					tp.setShowDuration(Duration.seconds(30));
					lblHab.setTooltip(tp);
				}

				previewStatsPane.getChildren().add(lblHab);
			}
		}

		// Modificadores
		if ((arma.getModificadoresDeAtributo() != null && !arma.getModificadoresDeAtributo().isEmpty())
				|| (arma.getModificadoresStatus() != null && !arma.getModificadoresStatus().isEmpty())) {
			addPreviewSeparator();
			exibirModificadoresPreview(arma.getModificadoresDeAtributo(), arma.getModificadoresStatus());
		}
	}

	private void addPreviewStat(String text, String color) {
		Label l = new Label(text);
		l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
		previewStatsPane.getChildren().add(l);
	}

	private void addPreviewSeparator() {
		Region sep = new Region();
		sep.getStyleClass().add("loja-preview-separator");
		sep.setPrefHeight(1);
		previewStatsPane.getChildren().add(sep);
	}

	private void exibirModificadoresPreview(Map<Atributo, Integer> modAtr, Map<String, Double> modStatus) {
		if (modAtr != null && !modAtr.isEmpty()) {
			Label lblHeader = new Label("Atributos");
			lblHeader.setStyle("-fx-text-fill: #707080; -fx-font-size: 10px; -fx-font-weight: bold;");
			previewStatsPane.getChildren().add(lblHeader);

			modAtr.forEach((atr, val) -> {
				String cor = val > 0 ? "#2ecc71" : "#e74c3c";
				addPreviewStat(atr.name().substring(0, 3) + ": " + (val > 0 ? "+" : "") + val, cor);
			});
		}
		if (modStatus != null && !modStatus.isEmpty()) {
			Label lblHeader = new Label("Modificadores");
			lblHeader.setStyle("-fx-text-fill: #707080; -fx-font-size: 10px; -fx-font-weight: bold;");
			previewStatsPane.getChildren().add(lblHeader);

			modStatus.forEach((key, val) -> {
				String nome = traduzirNomeStatus(key);
				double valor = val;
				String sufixo = "";
				if (key.contains("PERCENTUAL") || key.contains("MODIFICADOR") || key.contains("CRITICA") || key.contains("CRITICO")) {
					valor = val * 100;
					sufixo = "%";
				}
				String cor = valor > 0 ? "#2ecc71" : "#e74c3c";
				addPreviewStat(nome + ": " + (valor > 0 ? "+" : "") + String.format("%.1f", valor) + sufixo, cor);
			});
		}
	}

	// =============================================
	// === UTILITÁRIOS
	// =============================================

	private String getIconeTipo(Item item) {
		if (item instanceof Arma) return "\u2694";
		if (item instanceof Armadura) return "\uD83D\uDEE1";
		if (item instanceof Amuleto) return "\uD83D\uDCAE";
		if (item instanceof Consumivel) return "\uD83E\uDDEA";
		return "\uD83D\uDCE6";
	}

	private String getCorRaridadeItem(Item item) {
		if (item instanceof Arma) {
			Raridade r = ((Arma) item).getRaridade();
			if (r != null) return getCorRaridade(r);
		}
		return "#c0c0c0";
	}

	private String getCorRaridade(Raridade r) {
		switch (r) {
			case COMUM: return "#c0c0c0";
			case INCOMUM: return "#2ecc71";
			case RARO: return "#3498db";
			case EPICO: return "#9b59b6";
			case LENDARIO: return "#f39c12";
			case UNICO: return "#e74c3c";
			case MITICO: return "#ff00ff";
			default: return "#c0c0c0";
		}
	}

	private String getCorAtributo(Atributo atr) {
		switch (atr) {
			case FORCA: return "#e74c3c";
			case DESTREZA: return "#00ffff";
			case ENDURANCE: return "#2ecc71";
			case CARISMA: return "#e91e9c";
			case INTELIGENCIA: return "#e67e22";
			case PERCEPCAO: return "#f1c40f";
			case SORTE: return "#00ff00";
			case INSPIRACAO: return "#3498db";
			case SAGACIDADE: return "#ff6347";
			case TOPOR: return "#708090";
			default: return "#808080";
		}
	}

	private Label criarLabelPreco(int preco, String moedaTipo) {
		String icone;
		String classePreco;
		String texto;

		if ("OURO".equalsIgnoreCase(moedaTipo)) {
			icone = "\u2B50";
			classePreco = "loja-preco-ouro";
			texto = icone + " " + preco;
		} else if ("PRATA".equalsIgnoreCase(moedaTipo)) {
			icone = "\u25C9";
			classePreco = "loja-preco-prata";
			texto = icone + " " + preco;
		} else {
			icone = "\u25CF";
			classePreco = "loja-preco-bronze";
			texto = icone + " " + preco;
		}

		Label lbl = new Label(texto);
		lbl.getStyleClass().add(classePreco);
		return lbl;
	}

	private boolean verificarPodeComprar(int preco, String moedaTipo) {
		Inventario inv = jogadorAtual.getInventario();
		if ("OURO".equalsIgnoreCase(moedaTipo)) return inv.getMoedasOuro() >= preco;
		if ("PRATA".equalsIgnoreCase(moedaTipo)) return inv.getMoedasPrata() >= preco;
		return inv.getMoedasBronze() >= preco;
	}

	private String getBadgeDescontoClass(double desconto) {
		if (desconto <= 0.20) return "loja-badge-desconto-baixo";
		if (desconto <= 0.40) return "loja-badge-desconto-medio";
		if (desconto <= 0.60) return "loja-badge-desconto-alto";
		return "loja-badge-desconto-extremo";
	}

	private String getCorDesconto(double desconto) {
		if (desconto <= 0.20) return "#2ecc71";
		if (desconto <= 0.40) return "#f1c40f";
		if (desconto <= 0.60) return "#e67e22";
		return "#e74c3c";
	}

	private String traduzirNomeStatus(String key) {
		switch (key) {
			case "HP_MAXIMO": return "Vida Máx";
			case "MP_MAXIMO": return "Mana Máx";
			case "MOVIMENTO": return "Movimento";
			case "REDUCAO_DANO_MODIFICADOR": return "Red. Dano";
			case "DANO_BONUS_PERCENTUAL": return "Dano";
			case "TAXA_CRITICA": return "Taxa Crit";
			case "DANO_CRITICO": return "Dano Crit";
			case "RECEBE_PRATA": return "Recebe Prata";
			case "RECEBE_OURO": return "Recebe Ouro";
			case "CURA": return "Cura";
			case "CURA_MANA": return "Recupera Mana";
			default: return key.replace("_", " ");
		}
	}

	private String formatarNumero(double valor) {
		double valorArredondado = Math.round(valor * 10.0) / 10.0;
		if (valorArredondado == (long) valorArredondado) {
			return String.format("%d", (long) valorArredondado);
		}
		return String.format("%.1f", valorArredondado);
	}
}
