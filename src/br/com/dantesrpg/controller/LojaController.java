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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LojaController {

	@FXML
	private Label labelMoedasOuro;
	@FXML
	private Label labelMoedasPrata;
	@FXML
	private Label labelMoedasBronze;
	@FXML
	private VBox itensVendaContainer;
	@FXML
	private VBox inventarioJogadorContainer;
	@FXML
	private ComboBox<Personagem> playerSelectorComboBox;
	@FXML
	private ComboBox<String> comboSelecaoLoja;

	@FXML
	private Label previewNome;
	@FXML
	private Label previewDescricao;
	@FXML
	private VBox previewStatsPane;

	private Personagem jogadorAtual;
	private CombatController mainController;
	private List<Oferta> ofertasAtuais = new ArrayList<>(); // Lista de objetos Oferta

	// Classe auxiliar para guardar o item e o desconto específico desta loja
	private class Oferta {
		Item item;
		double desconto; // 0.0 a 1.0 (0.5 = 50% off)

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
		// Configuração das células do ComboBox
		playerSelectorComboBox.setCellFactory(lv -> new ListCell<Personagem>() {
			@Override
			protected void updateItem(Personagem p, boolean empty) {
				super.updateItem(p, empty);
				setText(empty ? "" : (p != null ? p.getNome() : ""));
			}
		});
		playerSelectorComboBox.setButtonCell(new ListCell<Personagem>() {
			@Override
			protected void updateItem(Personagem p, boolean empty) {
				super.updateItem(p, empty);
				setText(empty ? "" : (p != null ? p.getNome() : ""));
			}
		});

		comboSelecaoLoja.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				carregarLoja(newVal);
				atualizarUI();
			}
		});
	}

	public void inicializarLoja(CombatController controller, EstadoCombate estado, String idLojaInicial) {
		this.mainController = controller;

		// Carrega Jogadores
		playerSelectorComboBox.getItems().clear();
		List<Personagem> players = estado.getCombatentes().stream().filter(p -> p.getFaccao().equals("JOGADOR"))
				.collect(Collectors.toList());
		playerSelectorComboBox.getItems().addAll(players);

		if (!players.isEmpty()) {
			this.jogadorAtual = players.get(0);
			playerSelectorComboBox.setValue(this.jogadorAtual);
		}

		// Carrega lista de arquivos de loja
		carregarListaDeArquivosDeLoja();

		// Seleciona loja inicial (se houver, senão a primeira)
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
		Gson gson = new Gson();
		String resourcePath = "/data/lojas/" + nomeArquivo + ".json";

		try (InputStreamReader reader = new InputStreamReader(FileLoader.carregarArquivo(resourcePath),
				StandardCharsets.UTF_8)) {
			Type mapType = new TypeToken<Map<String, List<Map<String, Object>>>>() {
			}.getType();
			Map<String, List<Map<String, Object>>> data = gson.fromJson(reader, mapType);

			// Lê a lista de ofertas com descontos
			List<Map<String, Object>> listaItens = data.get("itensOfertados");

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
			} else {
				// Fallback para lista antiga simples (se houver)
				System.out.println("Aviso: Formato antigo de loja detectado ou lista vazia.");
			}

		} catch (Exception e) {
			System.err.println("Erro ao carregar loja: " + nomeArquivo);
			e.printStackTrace();
		}
	}

	private void atualizarUI() {
		itensVendaContainer.getChildren().clear();
		inventarioJogadorContainer.getChildren().clear();

		if (jogadorAtual == null)
			return;

		Inventario inv = jogadorAtual.getInventario();
		labelMoedasOuro.setText(inv.getMoedasOuro() + " Ouro");
		labelMoedasPrata.setText(inv.getMoedasPrata() + " Prata");
		labelMoedasBronze.setText(inv.getMoedasBronze() + " Bronze");

		// Renderiza Ofertas
		if (ofertasAtuais.isEmpty()) {
			itensVendaContainer.getChildren().add(new Label("Loja vazia."));
		} else {
			for (Oferta oferta : ofertasAtuais) {
				itensVendaContainer.getChildren().add(criarCardOferta(oferta));
			}
		}

		// Renderiza Inventário
		Map<String, Integer> inventarioAgrupado = inv.getItensAgrupados();
		for (Map.Entry<String, Integer> entry : inventarioAgrupado.entrySet()) {
			Item itemModelo = mainController.getItem(entry.getKey());
			if (itemModelo != null) {
				inventarioJogadorContainer.getChildren().add(criarCardVendaJogador(itemModelo, entry.getValue()));
			}
		}
	}

	private HBox criarCardOferta(Oferta oferta) {
		HBox card = new HBox(10.0);
		card.setStyle("-fx-border-color: grey; -fx-padding: 5;");
		card.setOnMouseClicked(e -> popularPreview(oferta.item));

		int precoFinal = oferta.getPrecoFinal(); // Quantidade de moedas
		String moedaTipo = oferta.item.getTipoMoeda(); // Tipo da moeda (OURO, PRATA, BRONZE)

		String textoPreco;
		String corPreco = "white";

		// Formatação Visual Baseada no TIPO, não no VALOR
		if ("OURO".equalsIgnoreCase(moedaTipo)) {
			textoPreco = precoFinal + " Ouro";
			corPreco = "gold";
		} else if ("PRATA".equalsIgnoreCase(moedaTipo)) {
			textoPreco = precoFinal + " Prata";
			corPreco = "silver"; // ou lightgrey
		} else {
			textoPreco = precoFinal + " Bronze";
			corPreco = "#cd7f32";
		}

		Label nomeLabel = new Label(oferta.item.getNome());
		if (oferta.item.getNome().contains("Overclock")) {
			nomeLabel.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold;");
		} else {
			nomeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
		}

		HBox nomeBox = new HBox(5);
		nomeBox.getChildren().add(nomeLabel);

		String corDesconto;

		if (oferta.desconto > 0) {
			double desconto = oferta.desconto;

			if (oferta.item.getNome().contains("Overclock")) {
				corDesconto = "#00FFFF";
			} else if (desconto <= 0.20) {
				corDesconto = "#00FF2E";
			} else if (desconto <= 0.4) {
				corDesconto = "#FFDD00";
			} else if (desconto <= 0.60) {
				corDesconto = "#FF8C00";
			} else if (desconto <= 0.80) {
				corDesconto = "#FF4800";
			} else {
				corDesconto = "#FF0000";
			}

			Label descontoLabel = new Label("[-" + (int) (desconto * 100) + "%]");
			descontoLabel.setStyle("-fx-text-fill: " + corDesconto + ";" + "-fx-font-weight: bold;"
					+ "-fx-effect: dropshadow(gaussian, " + corDesconto + ", 2, 0.1, 0, 0);");

			nomeBox.getChildren().add(descontoLabel);

			card.setStyle("-fx-border-color:" + corDesconto + "; -fx-padding: 5;");
		}

		Label precoLabel = new Label(textoPreco);
		precoLabel.setStyle("-fx-text-fill: " + corPreco + ";");

		Pane spacer = new Pane();
		HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

		Button btnComprar = new Button("Comprar");
		btnComprar.setOnAction(e -> comprarItem(oferta));

		// Validação de Dinheiro Estrita (Baseada no TIPO)
		boolean podeComprar = false;
		Inventario inv = jogadorAtual.getInventario();

		if ("OURO".equalsIgnoreCase(moedaTipo)) {
			podeComprar = inv.getMoedasOuro() >= precoFinal;
		} else if ("PRATA".equalsIgnoreCase(moedaTipo)) {
			podeComprar = inv.getMoedasPrata() >= precoFinal;
		} else { // BRONZE
			podeComprar = inv.getMoedasBronze() >= precoFinal;
		}

		if (!podeComprar)
			btnComprar.setDisable(true);

		card.getChildren().addAll(nomeBox, spacer, precoLabel, btnComprar);
		return card;
	}

	private void comprarItem(Oferta oferta) {
		Inventario inv = jogadorAtual.getInventario();
		int preco = oferta.getPrecoFinal();
		String moedaTipo = oferta.item.getTipoMoeda();

		boolean sucesso = false;

		if ("OURO".equalsIgnoreCase(moedaTipo)) {
			if (inv.gastarOuro(preco)) {
				System.out.println("LOJA: Pagou " + preco + " Ouro.");
				sucesso = true;
			}
		} else if ("PRATA".equalsIgnoreCase(moedaTipo)) {
			if (inv.gastarPrata(preco)) {
				System.out.println("LOJA: Pagou " + preco + " Prata.");
				sucesso = true;
			}
		} else {
			if (inv.gastarBronze(preco)) {
				System.out.println("LOJA: Pagou " + preco + " Bronze.");
				sucesso = true;
			}
		}

		if (sucesso) {
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

			System.out.println("LOJA: Comprou " + oferta.item.getNome());
			mainController.salvarEstadoJogadores();
			atualizarUI(); // Usa atualizarUI em português, corrigi o typo anterior
		} else {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Moeda incorreta ou insuficiente!");
			alert.show();
		}
	}

	private HBox criarCardVendaJogador(Item item, int quantidade) {
		HBox card = new HBox(10.0);
		card.setStyle("-fx-border-color: lightblue; -fx-padding: 5;");
		card.setOnMouseClicked(e -> popularPreview(item));

		Label nomeLabel = new Label(item.getNome() + " (x" + quantidade + ")");
		nomeLabel.setStyle("-fx-text-fill: white;");

		Pane spacer = new Pane();
		HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

		Button btnVender = new Button("Vender");
		btnVender.setOnAction(e -> venderItemManual(item));

		card.getChildren().addAll(nomeLabel, spacer, btnVender);
		return card;
	}

	private void venderItemManual(Item item) {
		TextInputDialog dialog = new TextInputDialog("0");
		dialog.setTitle("Venda Manual");
		dialog.setHeaderText("Vendendo: " + item.getNome());
		dialog.setContentText("Por quanto (Bronze) você vai vender?");

		dialog.getDialogPane().setStyle("-fx-background-color: #222;");
		dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");
		dialog.getEditor().setStyle("-fx-text-fill: black;");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			try {
				int valorVenda = Integer.parseInt(result.get());
				if (valorVenda < 0)
					valorVenda = 0;

				jogadorAtual.getInventario().removerItem(item);
				jogadorAtual.getInventario().receber(valorVenda);

				System.out.println("LOJA: " + jogadorAtual.getNome() + " vendeu " + item.getNome() + " por "
						+ valorVenda + " Bronze.");

				mainController.salvarEstadoJogadores();
				atualizarUI();

			} catch (NumberFormatException e) {
				Alert alert = new Alert(Alert.AlertType.ERROR, "Valor inválido!");
				alert.show();
			}
		}
	}

	private void popularPreview(Item item) {
		if (item == null)
			return;

		previewNome.setText(item.getNome());

		// Descrição para TODOS
		previewDescricao.setText(item.getDescricao() != null ? item.getDescricao() : "Sem descrição.");

		previewStatsPane.getChildren().clear();

		// Stats Específicos de Equipamento
		if (item instanceof Arma) {
			Arma arma = (Arma) item;
			if (arma.getAtributoMultiplicador() == Atributo.FORCA) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "red");
			} else if (arma.getAtributoMultiplicador() == Atributo.CARISMA) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "magenta");
			} else if (arma.getAtributoMultiplicador() == Atributo.DESTREZA) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "aqua");
			} else if (arma.getAtributoMultiplicador() == Atributo.ENDURANCE) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "lawngreen");
			} else if (arma.getAtributoMultiplicador() == Atributo.INSPIRACAO) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "dodgerblue");
			} else if (arma.getAtributoMultiplicador() == Atributo.INTELIGENCIA) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "lightsalmon");
			} else if (arma.getAtributoMultiplicador() == Atributo.PERCEPCAO) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "gold");
			} else if (arma.getAtributoMultiplicador() == Atributo.SAGACIDADE) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "tomato");
			} else if (arma.getAtributoMultiplicador() == Atributo.SORTE) {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "lime");
			} else {
				addStatLabel("Atributo: " + arma.getAtributoMultiplicador(), "slategray");
			}
			addStatLabel("Dano Base: " + arma.getDanoBase() + " (x" + arma.getTicksDeDano() + ")", "crimson");
			addStatLabel("Alcance: " + arma.getAlcance(), "yellow");
			addStatLabel("Custo TU: " + arma.getCustoTU(), "deepskyblue");

			if (arma.getNomeEfeitoOnHit() != null) {
				addStatLabel("Ao Acertar: " + arma.getNomeEfeitoOnHit(), "cyan");
				if (arma.getChanceEfeitoOnHit() <= 0.2) {
					addStatLabel("Chance: " + arma.getChanceEfeitoOnHit() * 100 + "%", "firebrick");
				} else if (arma.getChanceEfeitoOnHit() <= 0.4) {
					addStatLabel("Chance: " + arma.getChanceEfeitoOnHit() * 100 + "%", "crimson");
				} else if (arma.getChanceEfeitoOnHit() <= 0.6) {
					addStatLabel("Chance: " + arma.getChanceEfeitoOnHit() * 100 + "%", "palegreen");
				} else if (arma.getChanceEfeitoOnHit() <= 0.8) {
					addStatLabel("Chance: " + arma.getChanceEfeitoOnHit() * 100 + "%", "lime");
				} else {
					addStatLabel("Chance: " + arma.getChanceEfeitoOnHit() * 100 + "%", "gold");
				}
			}

			if (!arma.getHabilidadesConcedidasNomes().isEmpty()) {
				Label lblHeader = new Label("Habilidades:");
				lblHeader.setStyle("-fx-text-fill: gold; -fx-underline: true;");
				previewStatsPane.getChildren().add(lblHeader);

				for (String nomeHab : arma.getHabilidadesConcedidasNomes()) {
					Label lblHab = new Label("• " + nomeHab);
					lblHab.setStyle("-fx-text-fill: gold; -fx-cursor: help;");

					// Cria a habilidade temporariamente para ler os dados
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
								sb.append("Dano: ").append(String.format("%.0f%%", hab.getMultiplicadorDeDano() * 100))
										.append("\n");
							}

							sb.append("Alvo: ").append(hab.getTipoAlvo());
							if (hab.getAlcanceMaximo() > 0) {
								sb.append(" (Alcance: ").append(hab.getAlcanceMaximo()).append(")");
							}
							sb.append("\n");

							if (hab.getTamanhoArea() > 0) {
								sb.append("Área: ").append(hab.getTamanhoArea());
								if (hab.getAnguloCone() > 0) {
									sb.append(" (Cone ").append(hab.getAnguloCone()).append("º)");
								}
								sb.append("\n");
							}
						}

						Tooltip tp = new Tooltip(sb.toString());
						tp.setShowDelay(Duration.millis(100)); // Mostra quase instantaneamente
						tp.setShowDuration(Duration.seconds(30));
						lblHab.setTooltip(tp);
					}

					previewStatsPane.getChildren().add(lblHab);
				}
			}

			// Exibir modificadores da Arma
			exibirModificadores(arma.getModificadoresDeAtributo(), arma.getModificadoresStatus());
		} else if (item instanceof Armadura) {
			Armadura armadura = (Armadura) item;
			addStatLabel("Defesa: " + armadura.getArmaduraBase(), "lightgreen");
			exibirModificadores(armadura.getModificadoresDeAtributo(), armadura.getModificadoresStatus());
		} else if (item instanceof Amuleto) {
			Amuleto amuleto = (Amuleto) item;
			if (amuleto.getArmaduraBonus() > 0)
				addStatLabel("Defesa: +" + amuleto.getArmaduraBonus(), "lightgreen");
			exibirModificadores(amuleto.getModificadoresDeAtributo(), amuleto.getModificadoresStatus());
		} else if (item instanceof Consumivel) {
			Consumivel cons = (Consumivel) item;
			addStatLabel("Custo TU: " + cons.getCustoTU(), "white");
			if (cons.getEfeitos() != null) {
				cons.getEfeitos().forEach((k, v) -> addStatLabel(k + ": " + v, "lightgreen"));
			}
		}
	}

	private void addStatLabel(String text, String color) {
		Label l = new Label(text);
		l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
		previewStatsPane.getChildren().add(l);
	}

	private void exibirModificadores(Map<Atributo, Integer> modAtr, Map<String, Double> modStatus) {
		if (modAtr != null) {
			modAtr.forEach(
					(atr, val) -> addStatLabel(atr.name().substring(0, 3) + ": " + (val > 0 ? "+" : "") + val, "cyan"));
		}
		if (modStatus != null) {
			modStatus.forEach((key, val) -> {
				String vStr = String.format("%.1f", val);
				if (key.contains("PERCENTUAL"))
					vStr = String.format("%.0f%%", val * 100);
				addStatLabel(key + ": " + (val > 0 ? "+" : "") + vStr, "lime");
			});
		}
	}
}