package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.control.ScrollPane;

import javafx.scene.shape.Polygon;
import javafx.scene.shape.Line;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;

import java.util.Optional;

public class EditorJogadorController {

	// --- Coluna 1 ---
	@FXML
	private VBox playerListContainer;

	// --- Coluna 2 ---
	@FXML
	private Label labelPontosParaDistribuir;
	@FXML
	private GridPane attributesGrid;
	@FXML
	private Pane radarChartPlaceholder;
	@FXML
	private Label labelRanks;

	// --- Coluna 3 (Equipado) ---
	@FXML
	private VBox armaDetailsPane;
	@FXML
	private VBox armaduraDetailsPane;
	@FXML
	private VBox amuleto1DetailsPane;
	@FXML
	private VBox amuleto2DetailsPane;
	@FXML
	private Button btnDesequiparArma;
	@FXML
	private Button btnDesequiparArmadura;
	@FXML
	private Button btnDesequiparAmuleto1;
	@FXML
	private Button btnDesequiparAmuleto2;
	@FXML
	private ScrollPane detailedAttributesScrollPane;
	@FXML
	private VBox detailedAttributesPane;
	@FXML
	private Label labelNivelXP;

	// --- Coluna 4 (Inventário) ---
	@FXML
	private ListView<Item> inventarioListView;
	@FXML
	private ListView<Habilidade> habilidadesListView;

	// --- Lógica ---
	private List<Personagem> todosOsJogadores;
	private Personagem jogadorSelecionado;
	private CombatController mainController;

	@FXML
	public void initialize() {
		inventarioListView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) { // Equipar com Duplo clique
				Item itemSelecionado = inventarioListView.getSelectionModel().getSelectedItem();
				if (itemSelecionado != null) {
					equiparItemDoInventario(itemSelecionado);
				}
			}
		});

		inventarioListView.setCellFactory(lv -> new ListCell<Item>() {
			@Override
			protected void updateItem(Item item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setTooltip(null);
				} else {
					int quantidade = 0;
					if (jogadorSelecionado != null) {
						quantidade = jogadorSelecionado.getInventario().getItensAgrupados().getOrDefault(item.getTipo(),
								0);
					}
					setText(item.getNome() + " (x" + quantidade + ")");

					// --- USANDO O NOVO GERADOR DE TOOLTIP ---
					Tooltip tp = new Tooltip(gerarTextoDetalhesItem(item));
					tp.setShowDelay(javafx.util.Duration.millis(200)); // Mostra rápido
					setTooltip(tp);
				}
			}
		});

		habilidadesListView.setCellFactory(lv -> new ListCell<Habilidade>() {
			@Override
			protected void updateItem(Habilidade hab, boolean empty) {
				super.updateItem(hab, empty);
				setText(empty ? null : hab.getNome());
				if (hab != null) {
					setTooltip(new Tooltip(hab.getDescricao()));
				} else {
					setTooltip(null);
				}
			}
		});

		radarChartPlaceholder.widthProperty().addListener((obs, oldVal, newVal) -> desenharGraficoRadar());
		radarChartPlaceholder.heightProperty().addListener((obs, oldVal, newVal) -> desenharGraficoRadar());
	}

	public void inicializar(CombatController mainController, EstadoCombate estado) {
		this.mainController = mainController;
		this.todosOsJogadores = estado.getCombatentes().stream().filter(p -> p.getFaccao().equals("JOGADOR"))
				.collect(Collectors.toList());

		popularListaJogadores();

		if (!todosOsJogadores.isEmpty()) {
			selecionarJogador(todosOsJogadores.get(0));
		}
	}

	private void popularListaJogadores() {
		playerListContainer.getChildren().clear();
		for (Personagem p : todosOsJogadores) {
			Button btnJogador = new Button(p.getNome());
			btnJogador.setMaxWidth(Double.MAX_VALUE);
			btnJogador.setOnAction(e -> selecionarJogador(p));
			playerListContainer.getChildren().add(btnJogador);
		}
	}

	private void selecionarJogador(Personagem p) {
		this.jogadorSelecionado = p;
		System.out.println("EDITOR: Carregando dados de " + p.getNome());
		atualizarTudo();
	}

	private void atualizarTudo() {
		if (jogadorSelecionado == null)
			return;

		jogadorSelecionado.recalcularAtributosEstatisticas();

		atualizarAtributosBase();
		atualizarEquipamento();
		atualizarAtributosDetalhados();
		atualizarInventario();
		atualizarHabilidades();
	}

	// --- Coluna 2 ---
	private void atualizarAtributosBase() {
		attributesGrid.getChildren().clear();
		labelPontosParaDistribuir.setText("Pontos restantes: " + jogadorSelecionado.getPontosParaDistribuir());

		if (labelNivelXP != null) {
			int xpAtual = jogadorSelecionado.getXpAtual();
			int xpProx = jogadorSelecionado.getXpParaProximoNivel();
			labelNivelXP.setText("Nível " + jogadorSelecionado.getNivel() + " (" + xpAtual + " / " + xpProx + " XP)");

			// Muda a cor se tiver pontos sobrando
			if (jogadorSelecionado.getPontosParaDistribuir() > 0) {
				labelNivelXP.setStyle("-fx-text-fill: lightgreen; -fx-font-weight: bold;");
			} else {
				labelNivelXP.setStyle("-fx-text-fill: white;");
			}
		}

		int i = 0;

		for (Atributo atr : Atributo.values()) {
			// Pega o valor FINAL (com buffs/equipamentos)
			int valorFinal = jogadorSelecionado.getAtributosFinais().getOrDefault(atr, 1);
			// Pega o valor BASE (sem buffs)
			int valorBase = jogadorSelecionado.getAtributosBase().getOrDefault(atr, 1);

			String rank = getRank(valorFinal);

			// Coluna 0: Nome
			Label lblNome = new Label(atr.name().substring(0, 3) + ":"); // "FOR:"
			lblNome.setStyle("-fx-text-fill: lightgrey;");
			attributesGrid.add(lblNome, 0, i);

			// Coluna 1: Valor (AGORA MOSTRA O FINAL)
			Label lblValor = new Label(String.valueOf(valorFinal));

			// Estilização baseada na diferença
			if (valorFinal > valorBase) {
				// Buffado (Verde)
				lblValor.setStyle("-fx-text-fill: lightgreen; -fx-font-weight: bold;");
			} else if (valorFinal < valorBase) {
				// Debuffado (Vermelho)
				lblValor.setStyle("-fx-text-fill: lightcoral; -fx-font-weight: bold;");
			} else {
				// Normal (Branco)
				lblValor.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
			}
			attributesGrid.add(lblValor, 1, i);

			// Coluna 2: Botão + (Aumenta o BASE)
			Button btnMais = new Button("+");
			btnMais.setStyle("-fx-font-size: 10px; -fx-padding: 2 5;"); // Botão menor
			btnMais.setDisable(jogadorSelecionado.getPontosParaDistribuir() <= 0);
			btnMais.setOnAction(e -> {
				boolean sucesso = jogadorSelecionado.aumentarAtributoBase(atr);
				if (sucesso) {
					atualizarTudo();
				}
			});
			attributesGrid.add(btnMais, 2, i);

			String corRank;
			if (valorFinal >= 22)
				corRank = "-fx-text-fill: gold; -fx-font-weight: bold;";
			else if (valorFinal == 21)
				corRank = "#FF2B00; -fx-font-weight: bold;";
			else if (valorFinal == 20)
				corRank = "#FF2B00;";
			else if (valorFinal == 19)
				corRank = "#D12300; -fx-font-weight: bold;";
			else if (valorFinal == 18)
				corRank = "#D12300;";
			else if (valorFinal == 17)
				corRank = "#A31B00; -fx-font-weight: bold;";
			else if (valorFinal == 16)
				corRank = "#A31B00;";
			else if (valorFinal == 15)
				corRank = "#751400;";
			else if (valorFinal == 14)
				corRank = "#FF9000; -fx-font-weight: bold;";
			else if (valorFinal == 13)
				corRank = "#FF9000;";
			else if (valorFinal == 12)
				corRank = "#D17600;";
			else if (valorFinal == 11)
				corRank = "#FFDD00; -fx-font-weight: bold;";
			else if (valorFinal == 10)
				corRank = "#FFDD00;";
			else if (valorFinal == 9)
				corRank = "#D1B500;";
			else if (valorFinal == 8)
				corRank = "#44FF00; -fx-font-weight: bold;";
			else if (valorFinal == 7)
				corRank = "#44FF00;";
			else if (valorFinal == 6)
				corRank = "#38D100;";
			else if (valorFinal == 5)
				corRank = "#0033FF; -fx-font-weight: bold;";
			else if (valorFinal == 4)
				corRank = "#0033FF;";
			else if (valorFinal == 3)
				corRank = "#002AD1;";
			else if (valorFinal == 2)
				corRank = "lightgray;";
			else
				corRank = "grey;";

			// Cálculo do Dado e Texto Formatado ---
			int dado = br.com.dantesrpg.model.util.DiceRoller.getTipoDado(valorFinal);
			StringBuilder rankText = new StringBuilder();
			rankText.append("[").append(rank).append("] (d").append(dado).append(")");

			// Diferença (Buff/Debuff)
			int diferenca = valorFinal - valorBase;
			if (diferenca != 0) {
				String sinal = diferenca > 0 ? "+" : "";
				rankText.append(" (").append(sinal).append(diferenca).append(")");
			}

			Label lblRank = new Label(rankText.toString());
			lblRank.setStyle("-fx-text-fill: " + corRank);

			attributesGrid.add(lblRank, 3, i);
			i++;
		}

		desenharGraficoRadar();
	}

	// --- Coluna 3 ---
	private void atualizarEquipamento() {
		// Limpa os painéis de detalhes
		armaDetailsPane.getChildren().clear();
		armaduraDetailsPane.getChildren().clear();
		amuleto1DetailsPane.getChildren().clear();
		amuleto2DetailsPane.getChildren().clear();

		// Arma
		Arma arma = jogadorSelecionado.getArmaEquipada();
		if (arma != null) {
			armaDetailsPane.getChildren().add(new Label(arma.getNome()));

			if (arma instanceof Grimorio) {
				Grimorio grimorio = (Grimorio) arma;
				int slotsOcupados = grimorio.getMagiasArmazenadas().size();
				int slotsTotal = grimorio.getMaxSlots();

				Label lblSlots = new Label("Magias (" + slotsOcupados + "/" + slotsTotal + "):");
				lblSlots.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold;");
				armaDetailsPane.getChildren().add(lblSlots);

				// Lista as magias atuais com botão de remover
				for (Habilidade magia : grimorio.getMagiasArmazenadas()) {
					HBox row = new HBox(5);
					Label lblMagia = new Label("- " + magia.getNome());
					lblMagia.setStyle("-fx-text-fill: lightgrey;");

					Button btnRemover = new Button("x");
					btnRemover.setStyle("-fx-background-color: #500; -fx-text-fill: white; -fx-font-size: 8px;");
					btnRemover.setOnAction(e -> {
						grimorio.esquecerMagia(magia);
						atualizarTudo(); // Refresh
					});

					row.getChildren().addAll(btnRemover, lblMagia);
					armaDetailsPane.getChildren().add(row);
				}

				// Botão de Adicionar Magia (se houver espaço)
				if (slotsOcupados < slotsTotal) {
					Button btnAddMagia = new Button("+ Adicionar Magia");
					btnAddMagia.setStyle("-fx-background-color: #005; -fx-text-fill: white; -fx-font-size: 10px;");
					btnAddMagia.setMaxWidth(Double.MAX_VALUE);

					btnAddMagia.setOnAction(e -> abrirDialogoAdicionarMagia(grimorio));
					armaDetailsPane.getChildren().add(btnAddMagia);
				}
			} else {
				// Arma comum
				armaDetailsPane.getChildren().add(new Label("Tipo: " + arma.getTipo()));
				List<String> habilidades = arma.getHabilidadesConcedidasNomes();
				if (habilidades != null && !habilidades.isEmpty()) {
					for (String hab : habilidades) {
						Label lblHab = new Label("Hab: " + hab);
						lblHab.setStyle("-fx-text-fill: gold;");
						armaDetailsPane.getChildren().add(lblHab);
					}
				}
			}

			// Stats comuns
			armaDetailsPane.getChildren().add(new Label("Dano: " + arma.getDanoBase()));
			if (arma.getModificadoresStatus() != null) {
				arma.getModificadoresStatus().forEach((key, val) -> {
					String txt = key.replace("_", " ") + ": " + String.format("%.1f", val);
					if (key.contains("PERCENTUAL") || key.contains("MODIFICADOR"))
						txt += "%";
					Label lblStat = new Label(txt);
					lblStat.setStyle("-fx-text-fill: lightgreen; -fx-font-size: 10px;");
					armaDetailsPane.getChildren().add(lblStat);
				});
			}
			btnDesequiparArma.setDisable(false);
		} else {
			armaDetailsPane.getChildren().add(new Label("Nenhum"));
			btnDesequiparArma.setDisable(true);
		}

		// Armadura
		br.com.dantesrpg.model.Armadura armadura = jogadorSelecionado.getArmaduraEquipada();
		if (armadura != null) {
			String infoCompleta = gerarTextoDetalhesItem(armadura);
			String[] linhas = infoCompleta.split("\n");

			for (String linha : linhas) {
				Label l = new Label(linha);
				if (linha.contains(":"))
					l.setStyle("-fx-text-fill: cyan;"); // Destaca stats
				else
					l.setStyle("-fx-text-fill: lightgrey; -fx-font-style: italic;"); // Descrição
				armaduraDetailsPane.getChildren().add(l);
			}
			btnDesequiparArmadura.setDisable(false);
		} else {
			armaduraDetailsPane.getChildren().add(new Label("Nenhum"));
			btnDesequiparArmadura.setDisable(true);
		}

		// Amuleto 1
		Amuleto am1 = jogadorSelecionado.getAmuleto1();
		if (am1 != null) {

			String infoCompleta = gerarTextoDetalhesItem(am1);
			String[] linhas = infoCompleta.split("\n");

			for (String linha : linhas) {
				Label l = new Label(linha);
				if (linha.contains(":"))
					l.setStyle("-fx-text-fill: cyan;");
				else
					l.setStyle("-fx-text-fill: lightgrey; -fx-font-style: italic;");
				amuleto1DetailsPane.getChildren().add(l);
			}

			btnDesequiparAmuleto1.setDisable(false);

		} else {
			amuleto1DetailsPane.getChildren().add(new Label("Nenhum"));
			btnDesequiparAmuleto1.setDisable(true);
		}

		// Amuleto 2
		Amuleto am2 = jogadorSelecionado.getAmuleto2();
		if (am2 != null) {

			String infoCompleta = gerarTextoDetalhesItem(am2);
			String[] linhas = infoCompleta.split("\n");

			for (String linha : linhas) {
				Label l = new Label(linha);
				if (linha.contains(":"))
					l.setStyle("-fx-text-fill: cyan;");
				else
					l.setStyle("-fx-text-fill: lightgrey; -fx-font-style: italic;");
				amuleto2DetailsPane.getChildren().add(l);
			}

			btnDesequiparAmuleto2.setDisable(false);

		} else {
			amuleto2DetailsPane.getChildren().add(new Label("Nenhum"));
			btnDesequiparAmuleto2.setDisable(true);
		}
	}

	private void abrirDialogoAdicionarMagia(Grimorio grimorio) {
		// Pega a lista da Factory
		List<String> opcoes = new ArrayList<>(br.com.dantesrpg.model.util.HabilidadeFactory.getNomesDisponiveis());

		ChoiceDialog<String> dialog = new ChoiceDialog<>(null, opcoes);
		dialog.setTitle("Escrever no Grimório");
		dialog.setHeaderText("Escolha uma magia para adicionar:");
		dialog.setContentText("Magia:");

		// Estilização Dark básica
		dialog.getDialogPane().setStyle("-fx-background-color: #222;");
		dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(nomeMagia -> {
			Habilidade novaMagia = br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(nomeMagia);
			if (novaMagia != null) {
				boolean aprendeu = grimorio.aprenderMagia(novaMagia);
				if (aprendeu) {
					System.out.println("EDITOR: Magia " + nomeMagia + " adicionada ao grimório.");
					atualizarTudo();
				}
			}
		});
	}

	private void atualizarAtributosDetalhados() {
		detailedAttributesPane.getChildren().clear();
		detailedAttributesPane.getChildren()
				.add(new Label("HP: " + jogadorSelecionado.getVidaAtual() + "/" + jogadorSelecionado.getVidaMaxima()));
		detailedAttributesPane.getChildren()
				.add(new Label("MP: " + jogadorSelecionado.getManaAtual() + "/" + jogadorSelecionado.getManaMaxima()));
		detailedAttributesPane.getChildren().add(new Label("Movimento: " + jogadorSelecionado.getMovimento()));
		detailedAttributesPane.getChildren()
				.add(new Label(String.format("Taxa Crítica: %.1f%%", jogadorSelecionado.getTaxaCritica() * 100)));
		detailedAttributesPane.getChildren()
				.add(new Label(String.format("Dano Crítico: +%.1f%%", jogadorSelecionado.getDanoCritico() * 100)));
		detailedAttributesPane.getChildren().add(new Label("Armadura Total: " + jogadorSelecionado.getArmaduraTotal()));
		detailedAttributesPane.getChildren().add(
				new Label(String.format("Red. Armadura: %.1f%%", jogadorSelecionado.getReducaoDanoArmadura() * 100)));
		detailedAttributesPane.getChildren()
				.add(new Label(String.format("Red. Topor: %.1f%%", jogadorSelecionado.getReducaoDanoTopor() * 100)));
		Label lblBonus = new Label(
				String.format("Bônus Dano Total: +%.1f%%", jogadorSelecionado.getBonusDanoPercentual() * 100));
		detailedAttributesPane.getChildren().add(lblBonus);
	}

	// --- Coluna 4 ---
	private void atualizarInventario() {
		inventarioListView.getItems().clear();

		if (jogadorSelecionado != null && jogadorSelecionado.getInventario() != null && mainController != null) {

			Map<String, Integer> inventarioAgrupado = jogadorSelecionado.getInventario().getItensAgrupados();
			for (String tipoItem : inventarioAgrupado.keySet()) {
				Item itemModelo = mainController.getItem(tipoItem);

				if (itemModelo != null) {
					inventarioListView.getItems().add(itemModelo);
				} else {
					System.err.println("Editor UI: Não foi possível encontrar o item modelo para: " + tipoItem);
				}
			}
		}
	}

	private void atualizarHabilidades() {
		habilidadesListView.getItems().clear();
		if (jogadorSelecionado != null && jogadorSelecionado.getHabilidadesDeClasse() != null) {
			habilidadesListView.getItems().addAll(jogadorSelecionado.getHabilidadesDeClasse());
		}
	}

	// --- LÓGICA DE EQUIPAR / DESEQUIPAR ---

	private void equiparItemDoInventario(Item item) {
		if (item == null || jogadorSelecionado == null)
			return;

		Inventario inv = jogadorSelecionado.getInventario();

		if (item instanceof Arma) {
			Arma armaAntiga = jogadorSelecionado.getArmaEquipada();
			if (armaAntiga != null)
				inv.adicionarItem(armaAntiga);

			jogadorSelecionado.setArmaEquipada((Arma) item);
			inv.removerItem(item);

		} else if (item instanceof Armadura) {
			Armadura armaduraAntiga = jogadorSelecionado.getArmaduraEquipada();
			if (armaduraAntiga != null)
				inv.adicionarItem(armaduraAntiga);

			jogadorSelecionado.setArmaduraEquipada((Armadura) item);
			inv.removerItem(item);

		} else if (item instanceof Amuleto) {
			Amuleto novoAmuleto = (Amuleto) item;
			Amuleto slot1 = jogadorSelecionado.getAmuleto1();
			Amuleto slot2 = jogadorSelecionado.getAmuleto2();

			// Caso 1: Slot 1 vazio -> Equipa no 1
			if (slot1 == null) {
				jogadorSelecionado.setAmuleto1(novoAmuleto);
				inv.removerItem(novoAmuleto);

				// Caso 2: Slot 2 vazio -> Equipa no 2
			} else if (slot2 == null) {
				jogadorSelecionado.setAmuleto2(novoAmuleto);
				inv.removerItem(novoAmuleto);

				// Caso 3: AMBOS cheios -> Pergunta ao usuário
			} else {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Slots de Amuleto Cheios");
				alert.setHeaderText("Onde você deseja equipar " + novoAmuleto.getNome() + "?");
				alert.setContentText("Escolha qual amuleto substituir:");

				// Cria botões customizados com o nome dos itens atuais
				ButtonType btnSlot1 = new ButtonType("Slot 1:\n" + slot1.getNome());
				ButtonType btnSlot2 = new ButtonType("Slot 2:\n" + slot2.getNome());
				ButtonType btnCancelar = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);

				alert.getButtonTypes().setAll(btnSlot1, btnSlot2, btnCancelar);

				// Mostra o pop-up e espera a resposta
				Optional<ButtonType> result = alert.showAndWait();

				if (result.isPresent()) {
					if (result.get() == btnSlot1) {
						// Troca pelo Slot 1
						inv.adicionarItem(slot1);
						jogadorSelecionado.setAmuleto1(novoAmuleto);
						inv.removerItem(novoAmuleto);

					} else if (result.get() == btnSlot2) {
						// Troca pelo Slot 2
						inv.adicionarItem(slot2);
						jogadorSelecionado.setAmuleto2(novoAmuleto);
						inv.removerItem(novoAmuleto);
					}
				}
			}
		} else {
			System.out.println("EDITOR: " + item.getNome() + " não é um item equipável.");
			return;
		}

		System.out.println("EDITOR: " + jogadorSelecionado.getNome() + " equipou " + item.getNome());
		atualizarTudo(); // Refresh na UI
	}

	@FXML
	private void onDesequiparArmaClick() {
		Arma arma = jogadorSelecionado.getArmaEquipada();
		if (arma != null) {
			jogadorSelecionado.getInventario().adicionarItem(arma);
			jogadorSelecionado.setArmaEquipada(null);
			atualizarTudo();
		}
	}

	@FXML
	private void onDesequiparArmaduraClick() {
		Armadura armadura = jogadorSelecionado.getArmaduraEquipada();
		if (armadura != null) {
			jogadorSelecionado.getInventario().adicionarItem(armadura);
			jogadorSelecionado.setArmaduraEquipada(null);
			atualizarTudo();
		}
	}

	@FXML
	private void onDesequiparAmuleto1Click() {
		Amuleto am = jogadorSelecionado.getAmuleto1();
		if (am != null) {
			jogadorSelecionado.getInventario().adicionarItem(am);
			jogadorSelecionado.setAmuleto1(null);
			atualizarTudo();
		}
	}

	@FXML
	private void onDesequiparAmuleto2Click() {
		Amuleto am = jogadorSelecionado.getAmuleto2();
		if (am != null) {
			jogadorSelecionado.getInventario().adicionarItem(am);
			jogadorSelecionado.setAmuleto2(null);
			atualizarTudo();
		}
	}

	@FXML
	private void onSalvarClick() {
		if (jogadorSelecionado == null || mainController == null)
			return;

		System.out.println("EDITOR: Botão Salvar clicado para " + jogadorSelecionado.getNome());
		mainController.salvarPersonagem(jogadorSelecionado);
	}

	@FXML
	private void onResetarClick() {
		if (jogadorSelecionado == null || mainController == null)
			return;

		String nomeArquivo = jogadorSelecionado.getJsonFileName().replace(".json", "");

		Personagem personagemRecarregado = mainController.recarregarPersonagem(nomeArquivo);
		if (personagemRecarregado != null) {
			todosOsJogadores.remove(jogadorSelecionado);
			todosOsJogadores.add(personagemRecarregado);
			selecionarJogador(personagemRecarregado);
		}
	}

	private void desenharGraficoRadar() {
		radarChartPlaceholder.getChildren().clear();

		if (jogadorSelecionado == null)
			return;

		double width = radarChartPlaceholder.getWidth();
		double height = radarChartPlaceholder.getHeight();

		if (width <= 0)
			width = 250;
		if (height <= 0)
			height = 200;

		double centerX = width / 2;
		double centerY = height / 2;

		double maxRadius = (Math.min(width, height) / 2) * 0.70;

		int numAtributos = Atributo.values().length;
		double angleStep = 2 * Math.PI / numAtributos;

		// desenhando a "teia" do fundo
		int[] ranksValues = { 4, 7, 10, 14, 16, 20 };

		for (int val : ranksValues) {
			Polygon rankPoly = new Polygon();
			rankPoly.setFill(Color.TRANSPARENT);
			rankPoly.setStroke(Color.web("#444444"));
			if (val == 20)
				rankPoly.setStrokeWidth(2);
			else
				rankPoly.getStrokeDashArray().addAll(5.0, 5.0);

			for (int i = 0; i < numAtributos; i++) {
				double angle = i * angleStep - Math.PI / 2;
				double r = (val / 20.0) * maxRadius;
				double x = centerX + Math.cos(angle) * r;
				double y = centerY + Math.sin(angle) * r;
				rankPoly.getPoints().addAll(x, y);
			}
			radarChartPlaceholder.getChildren().add(rankPoly);
		}

		// desenhando o texto e os labels
		for (int i = 0; i < numAtributos; i++) {
			double angle = i * angleStep - Math.PI / 2;
			double x = centerX + Math.cos(angle) * maxRadius;
			double y = centerY + Math.sin(angle) * maxRadius;

			Line axis = new Line(centerX, centerY, x, y);
			axis.setStroke(Color.web("#333333"));
			radarChartPlaceholder.getChildren().add(axis);

			String nomeAtr = Atributo.values()[i].name().substring(0, 3);
			double labelX = centerX + Math.cos(angle) * (maxRadius + 20);
			double labelY = centerY + Math.sin(angle) * (maxRadius + 20);

			Text text = new Text(nomeAtr);
			text.setFill(Color.WHITE);
			text.setFont(new Font("Arial", 10));

			text.setX(labelX - text.getLayoutBounds().getWidth() / 2);
			text.setY(labelY + text.getLayoutBounds().getHeight() / 4);

			radarChartPlaceholder.getChildren().add(text);
		}

		// desenhando o poligono dos atributos
		Polygon statsPoly = new Polygon();
		statsPoly.setFill(Color.web("rgba(0, 255, 255, 0.3)"));
		statsPoly.setStroke(Color.CYAN);
		statsPoly.setStrokeWidth(2);

		Map<Atributo, Integer> atributosAtuais = jogadorSelecionado.getAtributosFinais();

		for (int i = 0; i < numAtributos; i++) {
			Atributo atr = Atributo.values()[i];
			// Se não tiver na lista, assume 1
			int valor = atributosAtuais.getOrDefault(atr, 1);

			double valorVisual = Math.max(0, valor);

			double angle = i * angleStep - Math.PI / 2;
			// Regra de 3: Se 20 é o raio máximo...
			double r = (valorVisual / 20.0) * maxRadius;

			double x = centerX + Math.cos(angle) * r;
			double y = centerY + Math.sin(angle) * r;

			statsPoly.getPoints().addAll(x, y);

			Circle dot = new Circle(x, y, 3, Color.CYAN);
			radarChartPlaceholder.getChildren().add(dot);
		}

		radarChartPlaceholder.getChildren().add(statsPoly);
	}

	private String gerarTextoDetalhesItem(Item item) {
		StringBuilder sb = new StringBuilder();
		sb.append(item.getNome()).append("\n");
		sb.append(item.getDescricao()).append("\n\n");

		Map<Atributo, Integer> modAtr = null;
		Map<String, Double> modStatus = null;

		if (item instanceof br.com.dantesrpg.model.Armadura) {
			br.com.dantesrpg.model.Armadura a = (br.com.dantesrpg.model.Armadura) item;
			sb.append("Defesa: ").append(a.getArmaduraBase()).append("\n");
			modAtr = a.getModificadoresDeAtributo();
			modStatus = a.getModificadoresStatus();

		} else if (item instanceof br.com.dantesrpg.model.Amuleto) {
			br.com.dantesrpg.model.Amuleto a = (br.com.dantesrpg.model.Amuleto) item;
			if (a.getArmaduraBonus() > 0)
				sb.append("Defesa: +").append(a.getArmaduraBonus()).append("\n");
			modAtr = a.getModificadoresDeAtributo();
			modStatus = a.getModificadoresStatus();

		} else if (item instanceof br.com.dantesrpg.model.Arma) {
			br.com.dantesrpg.model.Arma a = (br.com.dantesrpg.model.Arma) item;
			sb.append("Dano: ").append(a.getDanoBase()).append("\n");
			sb.append("Atributo: ").append(a.getAtributoMultiplicador()).append("\n");

			modAtr = a.getModificadoresDeAtributo();
			modStatus = a.getModificadoresStatus();

			// Lista Habilidades da Arma
			if (!a.getHabilidadesConcedidasNomes().isEmpty()) {
				sb.append("\nHabilidades:\n");
				for (String hab : a.getHabilidadesConcedidasNomes()) {
					sb.append("• ").append(hab).append("\n");
				}
			}
		}

		// Lista Atributos (FOR, DES...)
		if (modAtr != null && !modAtr.isEmpty()) {
			modAtr.forEach((atr, val) -> {
				sb.append(atr.name().substring(0, 3)).append(": ").append(val > 0 ? "+" : "").append(val).append("\n");
			});
		}

		// Lista Status Especiais
		if (modStatus != null && !modStatus.isEmpty()) {
			modStatus.forEach((key, val) -> {
				String nomeAmigavel = key;
				if (key.equals("HP_MAXIMO"))
					nomeAmigavel = "Vida Máx";
				if (key.equals("MP_MAXIMO"))
					nomeAmigavel = "Mana Máx";
				if (key.equals("MOVIMENTO"))
					nomeAmigavel = "Movimento";
				if (key.equals("REDUCAO_DANO_MODIFICADOR")) {
					nomeAmigavel = "Red. Dano";
					val = val * 100;
				} // %
				if (key.equals("DANO_BONUS_PERCENTUAL")) {
					nomeAmigavel = "Dano";
					val = val * 100;
				} // %
				if (key.equals("TAXA_CRITICA")) {
					nomeAmigavel = "Taxa Critica";
					val = val * 100;
				} // %
				if (key.equals("TAXA_CRITICA")) {
					nomeAmigavel = "Dano rítico";
					val = val * 100;
				} // %

				String sulfixo = (key.contains("PERCENTUAL") || key.contains("MODIFICADOR") || key.contains("CRITICA"))
						? "%"
						: "";

				sb.append(nomeAmigavel).append(": ").append(val > 0 ? "+" : "").append(String.format("%.1f", val))
						.append(sulfixo).append("\n");
			});
		}

		return sb.toString();
	}

	// --- Lógica do Gráfico (Ranks) ---
	private String getRank(int pontos) {
		if (pontos >= 22)
			return "P";
		if (pontos == 21)
			return "SSS+";
		if (pontos == 20)
			return "SSS";
		if (pontos == 19)
			return "SS+";
		if (pontos == 18)
			return "SS";
		if (pontos == 17)
			return "S+";
		if (pontos == 16)
			return "S";
		if (pontos == 15)
			return "S-";
		if (pontos == 14)
			return "A+";
		if (pontos == 13)
			return "A";
		if (pontos == 12)
			return "A-";
		if (pontos == 11)
			return "B+";
		if (pontos == 10)
			return "B";
		if (pontos == 9)
			return "B-";
		if (pontos == 8)
			return "C+";
		if (pontos == 7)
			return "C";
		if (pontos == 6)
			return "C-";
		if (pontos == 5)
			return "D+";
		if (pontos == 4)
			return "D";
		if (pontos == 3)
			return "D-";
		if (pontos == 2)
			return "E+";
		return "E";
	}
}