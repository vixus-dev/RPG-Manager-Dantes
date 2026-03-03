package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.DiceRoller;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

public class DetailedTurnHUDController {

	// --- Coluna 1 ---
	@FXML
	private Label labelNomeAtor, labelClasseRaca, labelHP, labelMP, labelTU;
	@FXML
	private VBox effectsContainer;
	@FXML
	private ScrollPane detailedAttributesScrollPane;
	@FXML
	private VBox detailedAttributesPane;
	@FXML
	private Button btnDetalhes;
	@FXML
	private GridPane attributesGrid;
	@FXML
	private Label labelVidaFormula, labelManaFormula, labelMovimento, labelTaxaCritica, labelDanoCritico,
			labelArmaduraTotal, labelReducaoArmadura, labelToporReducao, labelReducaoTotalHits, labelToporDotReducao;

	// --- Coluna 2 ---
	@FXML
	private Button btnAbaAtaques, btnAbaItens, btnAbaTaticas;
	@FXML
	private GridPane actionsGrid;

	// --- Coluna 3 (Detalhes) ---
	@FXML
	private VBox actionDetailsColumn;
	@FXML
	private Label lblActionTitle, lblActionDesc;

	// Opções de Ataque
	@FXML
	private VBox attackOptionsBox;
	@FXML
	private ToggleGroup grupoModo;
	@FXML
	private ToggleButton toggleFraco, toggleNormal, toggleForte;
	@FXML
	private Label lblInfoModo;
	@FXML
	private Button btnCoronhada;
	@FXML
	private VBox boxRajada;
	@FXML
	private Slider sliderRajada;
	@FXML
	private Label lblInfoRajada;

	// Inputs e Confirmação
	@FXML
	private VBox diceInputsBox;
	@FXML
	private Label labelEstimativaDano;
	@FXML
	private Button btnSelecionarAlvo;
	@FXML
	private Button btnConfirmarAcao;

	// --- Lógica Interna ---
	private Personagem atorAtual;
	private CombatController mainController;
	private ToggleGroup toggleGroupOpcoes;

	// Estado da Ação Preparada
	private Habilidade habilidadeSelecionada;
	private Item itemSelecionado;
	private FantasmaNobre fantasmaNobreSelecionado;
	private boolean isAtaqueBasico = false;
	private boolean isModoCoronhadaSelecionado = false;

	// Inputs Dinâmicos
	private TextField inputDadoAtributo;
	private Map<String, TextField> inputsExtras = new HashMap<>();

	// Alvos selecionados no mapa
	private List<Personagem> alvosNoMapa = new ArrayList<>();
	private int epicentroX = -1, epicentroY = -1;

	@FXML
	public void initialize() {
		// Listener para o Slider de Rajada
		sliderRajada.valueProperty().addListener((obs, oldVal, newVal) -> {
			int tiros = newVal.intValue();
			lblInfoRajada.setText("+" + tiros + " Tiros (+" + (tiros * 10) + "% TU)");
			atualizarEstimativaDano();
		});

		// Listener para o Grupo de Modos
		grupoModo.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == null) {
				// Impede desmarcar tudo (volta pro normal)
				toggleNormal.setSelected(true);
			} else {
				atualizarTextoModo();
				atualizarEstimativaDano();
			}
		});
	}

	public void setAtor(Personagem ator, CombatController controller) {
		this.atorAtual = ator;
		this.mainController = controller;
		this.alvosNoMapa.clear();

		atualizarInfoPersonagem();

		// Abre na aba de ataques por padrão
		onAbaAtaquesClick();

		// Esconde a coluna de detalhes até selecionar algo
		actionDetailsColumn.setVisible(false);
		actionDetailsColumn.setManaged(false);
	}

	// --- ABAS ---

	@FXML
	private void onAbaAtaquesClick() {
		estilizarBotaoAba(btnAbaAtaques, true);
		estilizarBotaoAba(btnAbaItens, false);
		estilizarBotaoAba(btnAbaTaticas, false);
		actionsGrid.getChildren().clear();

		int row = 0;
		int col = 0;

		// Ataque Básico (Arma) - Permitido para todos (incluindo Clones)
		if (!atorAtual.getEfeitosAtivos().containsKey("Modo Justiça")) {
			Arma arma = atorAtual.getArmaEquipada();
			String textoAtaque = "Ataque Básico";
			if (arma != null && arma.isRequerMunicao()) {
				textoAtaque += "\n(" + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima() + ")";
			}
			Button btnAtaque = criarBotaoAcao(textoAtaque, "-fx-base: #500;");
			btnAtaque.setOnAction(e -> prepararAcao("Ataque Básico", null, null, null, true));
			adicionarAoGrid(btnAtaque, col++, row);
			if (col > 1) {
				col = 0;
				row++;
			}
		} else {
			Label lblBloqueado = new Label("Ataque Físico Bloqueado\n(Modo Justiça Ativo)");
			lblBloqueado.setStyle("-fx-text-fill: gray; -fx-font-style: italic; -fx-font-size: 10px;");
			adicionarAoGrid(lblBloqueado, col++, row);
			if (col > 1) {
				col = 0;
				row++;
			}
		}

		// Habilidades (Lógica Diferenciada para Clones)
		List<Habilidade> habilidadesParaMostrar = new ArrayList<>();

		if (atorAtual.isClone() && atorAtual.getCriador() != null) {
			// --- REGRA DO CLONE ---
			// Apenas a última habilidade usada pelo criador
			Habilidade ultimaHab = atorAtual.getCriador().getUltimaHabilidadeUsada();
			if (ultimaHab != null) {
				habilidadesParaMostrar.add(ultimaHab);
			}
		} else {
			// --- REGRA PADRÃO ---
			// Todas as habilidades
			if (atorAtual.getHabilidadesDeClasse() != null) {
				habilidadesParaMostrar.addAll(atorAtual.getHabilidadesDeClasse());
			}
		}

		// Renderiza os botões das habilidades filtradas
		for (Habilidade hab : habilidadesParaMostrar) {
			if (hab.getTipo() == TipoHabilidade.ATIVA) {
				String label = hab.getNome() + "\n" + hab.getCustoMana() + " MP | " + hab.getCustoTU() + " TU";
				Button btnHab = criarBotaoAcao(label, "-fx-base: #333;");

				// Verifica Cooldown (no próprio Clone)
				String cdName = "CD:" + hab.getNome();
				if (atorAtual.getEfeitosAtivos().containsKey(cdName)) {
					btnHab.setDisable(true);
					btnHab.setText(hab.getNome() + "\n(Recarga)");
				} else if (atorAtual.getManaAtual() < hab.getCustoMana()) {
					btnHab.setDisable(true);
					btnHab.setStyle("-fx-opacity: 0.5;");
				}

				btnHab.setOnAction(e -> prepararAcao(hab.getNome(), hab, null, null, false));
				adicionarAoGrid(btnHab, col++, row);
				if (col > 1) {
					col = 0;
					row++;
				}
			}
		}

		// Fantasma Nobre
		if (!atorAtual.isClone()) {
			FantasmaNobre fn = atorAtual.getFantasmaNobre();
			if (fn != null) {
				Button btnFN = criarBotaoAcao("FN: " + fn.getNome(), "-fx-base: #400040; -fx-border-color: violet;");

				if (atorAtual.getEfeitosAtivos().containsKey("CD:" + fn.getNome())) {
					btnFN.setDisable(true);
					btnFN.setText(fn.getNome() + " (Recarga)");
				}

				btnFN.setOnAction(e -> {
					if (fn instanceof br.com.dantesrpg.model.fantasmasnobres.InvocacaoMurasame) {
						lidarComInvocacaoAyame(fn);
					} else {
						prepararAcao(fn.getNome(), null, null, fn, false);
					}
				});

				adicionarAoGrid(btnFN, col++, row);
			}
		}
	}

	private void lidarComInvocacaoAyame(FantasmaNobre fn) {
		// Busca essências
		List<br.com.dantesrpg.model.items.EssenciaInimigo> essencias = new ArrayList<>();
		if (atorAtual.getInventario() != null) {
			for (String key : atorAtual.getInventario().getItensAgrupados().keySet()) {
				Item item = mainController.getItem(key);
				if (item instanceof br.com.dantesrpg.model.items.EssenciaInimigo) {
					essencias.add((br.com.dantesrpg.model.items.EssenciaInimigo) item);
				}
			}
		}

		if (essencias.isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Sem Essências");
			alert.setHeaderText("Você não possui almas capturadas.");
			alert.setContentText("Use a Murasame para matar inimigos.");
			alert.showAndWait();
			return;
		}

		ChoiceDialog<br.com.dantesrpg.model.items.EssenciaInimigo> dialog = new ChoiceDialog<>(essencias.get(0),
				essencias);
		dialog.setTitle("Invocação Profana");
		dialog.setHeaderText("Escolha a alma para invocar:");
		dialog.setContentText("Essência:");

		dialog.setResultConverter(buttonType -> {
			if (buttonType == ButtonType.OK)
				return dialog.getSelectedItem();
			return null;
		});

		dialog.getDialogPane().setStyle("-fx-background-color: #222;");
		dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");

		Optional<br.com.dantesrpg.model.items.EssenciaInimigo> result = dialog.showAndWait();

		if (result.isPresent()) {
			br.com.dantesrpg.model.items.EssenciaInimigo essenciaEscolhida = result.get();

			// Truque para passar a essência
			prepararAcao(fn.getNome(), null, null, fn, false);
			this.itemSelecionado = essenciaEscolhida;

			lblActionDesc.setText("Invocando: " + essenciaEscolhida.getNome() + "\n" + fn.getDescricao());

			btnConfirmarAcao.setOnAction(ev -> {
				mainController.resolverAcaoInvocacao(atorAtual, fn, essenciaEscolhida);
				btnConfirmarAcao.setOnAction(ev2 -> onConfirmarAcaoClick());
			});
		}
	}

	@FXML
	private void onAbaItensClick() {
		estilizarBotaoAba(btnAbaAtaques, false);
		estilizarBotaoAba(btnAbaItens, true);
		estilizarBotaoAba(btnAbaTaticas, false);
		actionsGrid.getChildren().clear();

		int row = 0;
		int col = 0;

		if (atorAtual.getInventario() != null) {
			for (Map.Entry<String, Integer> entry : atorAtual.getInventario().getItensAgrupados().entrySet()) {
				String tipoItem = entry.getKey();
				int qtd = entry.getValue();
				Item itemModelo = mainController.getItem(tipoItem);

				if (itemModelo != null && itemModelo.isUsavelEmCombate()) {
					Button btnItem = criarBotaoAcao(itemModelo.getNome() + "\n(x" + qtd + ")", "-fx-base: #333;");
					btnItem.setOnAction(e -> prepararAcao(itemModelo.getNome(), null, itemModelo, null, false));
					adicionarAoGrid(btnItem, col++, row);
					if (col > 1) {
						col = 0;
						row++;
					}
				}
			}
		}
	}

	@FXML
	private void onAbaTaticasClick() {
		estilizarBotaoAba(btnAbaAtaques, false);
		estilizarBotaoAba(btnAbaItens, false);
		estilizarBotaoAba(btnAbaTaticas, true);
		actionsGrid.getChildren().clear();

		int row = 0;
		int col = 0;

		Button btnPassar = criarBotaoAcao("Passar a Vez\n(Recupera Mana)", "-fx-base: #224422;");
		btnPassar.setOnAction(e -> mainController
				.resolverAcaoPassarVez(new AcaoMestreInput(atorAtual, new ArrayList<>(), (Habilidade) null)));
		adicionarAoGrid(btnPassar, col++, row);

		Button btnMover = criarBotaoAcao("Movimentar", "-fx-base: #004488;");
		btnMover.setOnAction(e -> {
			// Nota para o futuro Controller: verificar se isso consome TU ou encerra o turno lá
			mainController.iniciarMovimentoTatico(atorAtual);
		});
		adicionarAoGrid(btnMover, col++, row);

		Button btnFugir = criarBotaoAcao("Fugir\n(Sair do Combate)", "-fx-base: #442222;");
		btnFugir.setOnAction(e -> mainController.resolverAcaoFugir(atorAtual));
		adicionarAoGrid(btnFugir, col++, row);

		if (atorAtual.getArmaEquipada() != null && atorAtual.getArmaEquipada().isRequerMunicao()) {
			if (col > 1) {
				col = 0;
				row++;
			}
			Button btnRecarregar = criarBotaoAcao("Recarregar Arma", "-fx-base: #333;");
			btnRecarregar.setOnAction(e -> {
				mainController.getCombatManager().resolverAcaoRecarregar(atorAtual);
				mainController.avancarTurnoAposAcao();
			});
			adicionarAoGrid(btnRecarregar, col++, row);
		}
	}

	// --- PREPARAÇÃO DA AÇÃO (Coluna 3) ---

	private void prepararAcao(String titulo, Habilidade hab, Item item, FantasmaNobre fn, boolean isBasicAttack) {
		// Configura Estado
		this.habilidadeSelecionada = hab;
		this.itemSelecionado = item;
		this.fantasmaNobreSelecionado = fn;
		this.isAtaqueBasico = isBasicAttack;
		this.alvosNoMapa.clear();
		this.epicentroX = -1;

		// Mostra Painel
		actionDetailsColumn.setVisible(true);
		actionDetailsColumn.setManaged(true);
		lblActionTitle.setText(titulo);

		// Descrição
		if (hab != null)
			lblActionDesc.setText(hab.getDescricao());
		else if (item != null)
			lblActionDesc.setText(item.getDescricao());
		else if (fn != null)
			lblActionDesc.setText(fn.getDescricao());
		else if (isBasicAttack)
			lblActionDesc.setText("Ataque com a arma equipada.");

		// Opções de Ataque (Apenas para armas)
		if (isBasicAttack) {
			attackOptionsBox.setVisible(true);
			attackOptionsBox.setManaged(true);
			configurarOpcoesArma();
		} else {
			attackOptionsBox.setVisible(false);
			attackOptionsBox.setManaged(false);
		}

		// Inputs de Dados (Dinâmico)
		gerarInputsDeDados();

		// Botão de Alvo
		configurarBotaoAlvo();

		atualizarEstimativaDano();
	}

	private void configurarBotaoAlvo() {
		boolean precisaAlvo = verificaSePrecisaAlvo();

		if (precisaAlvo) {
			btnSelecionarAlvo.setDisable(false);
			btnSelecionarAlvo.setText("Selecionar Alvo (Mapa)");
			btnConfirmarAcao.setDisable(true); // Bloqueia até selecionar
		} else {
			btnSelecionarAlvo.setDisable(true);
			btnSelecionarAlvo.setText("N/A (Auto/Self)");
			btnConfirmarAcao.setDisable(false); // Já pode confirmar
		}
	}

	private boolean verificaSePrecisaAlvo() {
		if (isAtaqueBasico)
			return true;

		TipoAlvo tipo = TipoAlvo.INDIVIDUAL;

		if (habilidadeSelecionada != null)
			tipo = habilidadeSelecionada.getTipoAlvo();
		else if (fantasmaNobreSelecionado != null)
			tipo = fantasmaNobreSelecionado.getTipoAlvo();

		else if (itemSelecionado != null)
			return false;

		return (tipo == TipoAlvo.INDIVIDUAL || tipo == TipoAlvo.MULTIPLOS || tipo == TipoAlvo.AREA_QUADRADA
				|| tipo == TipoAlvo.AREA_CIRCULAR || tipo == TipoAlvo.AREA || tipo == TipoAlvo.LINHA
				|| tipo == TipoAlvo.CONE);
	}

	private void configurarOpcoesArma() {
		Arma arma = atorAtual.getArmaEquipada();

		// usando instanceof ou checa a classe
		boolean isRanged = (arma instanceof br.com.dantesrpg.model.ArmaRanged);

		if (isRanged) {
			// É Ranged
			toggleFraco.setVisible(false);
			toggleFraco.setManaged(false);
			toggleNormal.setVisible(true);
			toggleNormal.setManaged(true);
			toggleForte.setVisible(true);
			toggleForte.setManaged(true);
			btnCoronhada.setVisible(true);
			btnCoronhada.setManaged(true);

			// Atualiza descrição com Munição
			lblActionDesc.setText(
					arma.getDescricao() + "\nMunição: " + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima());
			if (arma.getMunicaoAtual() <= 0) {
				lblActionDesc.setStyle("-fx-text-fill: red;");
			}

			// Slider de Rajada
			if (arma.getMunicaoAtual() > 1) {
				boxRajada.setVisible(true);
				boxRajada.setManaged(true);

				// Configura limites do slider
				int maxTirosExtras = arma.getMunicaoAtual() - 1; // Se tem 5 balas, pode dar +4 tiros extras

				// Limite físico do slider (ex: máx 5 rajadas)
				// maxTirosExtras = Math.min(maxTirosExtras, 5);

				sliderRajada.setMax(maxTirosExtras);
				sliderRajada.setValue(0);

				lblInfoRajada.setText("+0 Tiros"); // Reset texto
			} else {
				boxRajada.setVisible(false);
				boxRajada.setManaged(false);
			}

			// Se sem munição, bloqueia tiro normal/forte
			if (arma.getMunicaoAtual() <= 0) {
				toggleNormal.setDisable(true);
				toggleForte.setDisable(true);
				btnCoronhada.fire(); // Auto-seleciona coronhada ou avisa
			} else {
				toggleNormal.setDisable(false);
				toggleForte.setDisable(false);
			}
			btnCoronhada.setOnAction(e -> {
				// Seleciona modo Coronhada visualmente
				toggleNormal.setSelected(false);
				toggleForte.setSelected(false);

				lblInfoModo.setText("Coronhada: 0.5x Dano, Melee");
				btnCoronhada.setStyle("-fx-base: #AA5500; -fx-border-color: white;");

				this.isModoCoronhadaSelecionado = true;
			});

			// Reset do estado coronhada se clicar nos outros
			toggleNormal.setOnAction(e -> {
				this.isModoCoronhadaSelecionado = false;
				btnCoronhada.setStyle("");
				atualizarTextoModo();
			});
			toggleForte.setOnAction(e -> {
				this.isModoCoronhadaSelecionado = false;
				btnCoronhada.setStyle("");
				atualizarTextoModo();
			});
		} else {
			// É Melee
			toggleFraco.setVisible(true);
			toggleFraco.setManaged(true);
			toggleNormal.setVisible(true);
			toggleNormal.setManaged(true);
			toggleForte.setVisible(true);
			toggleForte.setManaged(true);
			btnCoronhada.setVisible(false);
			btnCoronhada.setManaged(false);
			boxRajada.setVisible(false);
			boxRajada.setManaged(false);
		}

		toggleNormal.setSelected(true);

		atualizarTextoModo();
	}

	private void gerarInputsDeDados() {
		diceInputsBox.getChildren().clear();
		inputsExtras.clear();
		this.toggleGroupOpcoes = null;
		this.inputDadoAtributo = null;

		// Regra: Precisa de dado de atributo se for Ataque Básico OU Habilidade com Dano
		boolean precisaDadoAtributo = (isAtaqueBasico
				|| (habilidadeSelecionada != null && habilidadeSelecionada.getMultiplicadorDeDano() > 0));

		if (habilidadeSelecionada != null) {
			String nome = habilidadeSelecionada.getNome();
			if (nome.equals("Distorted Solo") || nome.equals("Wha-Wha Solo") || nome.equals("Plain Solo")) {
				precisaDadoAtributo = true;
			}
		}

		// Exceções que NÃO usam dado de atributo padrão
		if (habilidadeSelecionada != null) {
			String nome = habilidadeSelecionada.getNome();
			if (nome.equals("Caçada") || nome.equals("Trocado")) {
				precisaDadoAtributo = false; // Esses usam dados específicos
			}
		}

		// Input Principal (Atributo d20/d12 etc)
		if (precisaDadoAtributo) {
			Atributo atr = Atributo.FORCA; // Padrão
			if (atorAtual.getArmaEquipada() != null) {
				atr = atorAtual.getArmaEquipada().getAtributoMultiplicador();
			}
			// Se for habilidade mágica específica (ex: Wha-Wha Solo), força Inspiração
			if (habilidadeSelecionada != null
					&& (habilidadeSelecionada instanceof br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo
							|| habilidadeSelecionada instanceof br.com.dantesrpg.model.habilidades.classe.DistortedSolo
							|| habilidadeSelecionada instanceof br.com.dantesrpg.model.habilidades.classe.PlainSolo)) {
				atr = Atributo.INSPIRACAO;
			}

			int valorAtr = atorAtual.getAtributosFinais().getOrDefault(atr, 1);
			int tipoDado = DiceRoller.getTipoDado(valorAtr);

			Label lbl = new Label("Rolagem " + atr.name() + " (d" + tipoDado + "):");
			lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

			inputDadoAtributo = new TextField();
			inputDadoAtributo.setPromptText("Resultado do d" + tipoDado);
			inputDadoAtributo.setStyle("-fx-background-color: #333; -fx-text-fill: cyan; -fx-font-weight: bold;");
			inputDadoAtributo.textProperty().addListener((o, ov, nv) -> atualizarEstimativaDano());

			diceInputsBox.getChildren().addAll(lbl, inputDadoAtributo);
		}

		if (habilidadeSelecionada != null && habilidadeSelecionada.getOpcoesSelection() != null) {
			Label lbl = new Label("Escolha uma Opção:");
			lbl.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold;");
			diceInputsBox.getChildren().add(lbl);

			toggleGroupOpcoes = new ToggleGroup();

			FlowPane boxOpcoes = new FlowPane();
			boxOpcoes.setHgap(5);
			boxOpcoes.setVgap(5);
			boxOpcoes.setPrefWrapLength(300);

			for (String op : habilidadeSelecionada.getOpcoesSelection()) {
				ToggleButton btn = new ToggleButton(op);
				btn.setToggleGroup(toggleGroupOpcoes);
				btn.setUserData(op);
				btn.setStyle("-fx-base: #444; -fx-text-fill: white;");
				boxOpcoes.getChildren().add(btn);
			}

			if (!habilidadeSelecionada.getOpcoesSelection().isEmpty()) {
				toggleGroupOpcoes.getToggles().get(0).setSelected(true);
			}

			diceInputsBox.getChildren().add(boxOpcoes);
		}

		if (atorAtual.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble")) {
			// Só adiciona se já não adicionou
			if (!inputsExtras.containsKey("DADO_LYRIA_1")) {
				adicionarInputExtra("DADO_LYRIA_1", "Aposta 1 (d3):");
				adicionarInputExtra("DADO_LYRIA_2", "Aposta 2 (d3):");
				adicionarInputExtra("DADO_LYRIA_3", "Aposta 3 (d3):");
			}
		}

		// Inputs Específicos por Habilidade
		if (habilidadeSelecionada != null) {
			String nome = habilidadeSelecionada.getNome();

			if (nome.equals("Fulgor Negro") && !atorAtual.getEfeitosAtivos().containsKey("Restrição Celestial")) {
				adicionarInputExtra("DADO_CHANCE_FULGOR", "Chance Acerto (1d4):");
			} else if (nome.equals("Fulgor Negro") && atorAtual.getEfeitosAtivos().containsKey("Restrição Celestial")) {
				adicionarInputExtra("DADO_CHANCE_RESTRICAO", "Restrição (1=Sucesso):");
			} else if (nome.equals("Trocado")) {
				adicionarInputExtra("DADO_ATRIBUTO", "Rolagem DES/FOR:"); // Trocado precisa do atributo TAMBÉM
				adicionarInputExtra("DADO_CHANCE_TROCADO", "Qtd. Moedas (1d4):");
			} else if (nome.equals("Caçada")) {
				adicionarInputExtra("DADO_ATRIBUTO", "Rolagem DES/SAG:");
				adicionarInputExtra("DADO_CHANCE_CACADA_1D6", "Qtd. Tiros (1d6):");
			}
		}

		// Inputs Especiais de Arma (Terrore)
		if (atorAtual.getArmaEquipada() != null && atorAtual.getArmaEquipada().getNome().equals("Terrore")) {
			adicionarInputExtra("DADO_MEDO_D7", "Dado do Medo (1d7):");
		}

		List<String> listaOpcoes = null;
		if (habilidadeSelecionada != null) {
			listaOpcoes = habilidadeSelecionada.getOpcoesSelection();
		} else if (fantasmaNobreSelecionado != null) {
			listaOpcoes = fantasmaNobreSelecionado.getOpcoesSelection();
		}

		// IMPORTANTE: Só cria o FlowPane se houver opções e o box estiver vazio de opções
		if (listaOpcoes != null && !listaOpcoes.isEmpty()) {
			Label lblHeader = new Label("Configurar Ação:");
			lblHeader.setStyle("-fx-text-fill: #00FFFF; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");

			ToggleGroup group = new ToggleGroup();
			this.toggleGroupOpcoes = group;

			FlowPane flow = new FlowPane(5, 5);
			flow.setPrefWrapLength(300);

			for (String opt : listaOpcoes) {
				ToggleButton tb = new ToggleButton(opt);
				tb.setToggleGroup(group);
				tb.setUserData(opt);
				tb.setPrefWidth(120);
				tb.setStyle("-fx-base: #333;");
				flow.getChildren().add(tb);
			}

			// Listener para atualizar a descrição ao clicar
			group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
				if (newT != null) {
					String val = (String) newT.getUserData();
					atualizarDescricaoPorOpcao(val);

					// Se escolher material, vamos preparar o terreno para o ChoiceDialog
					if (val.equalsIgnoreCase("Material")) {
						btnConfirmarAcao.setText("ESCOLHER MATERIAL");
					} else {
						btnConfirmarAcao.setText("CONFIRMAR AÇÃO");
					}
				}
			});

			// Seleciona a primeira por padrão
			group.getToggles().get(0).setSelected(true);

			// Adiciona ao VBox principal apenas UMA VEZ
			diceInputsBox.getChildren().addAll(lblHeader, flow);
		}

	}

	private void atualizarDescricaoPorOpcao(String escolha) {
		if (fantasmaNobreSelecionado instanceof br.com.dantesrpg.model.fantasmasnobres.InvocacaoSangrenta) {
			switch (escolha) {
			case "Golem":
				lblActionDesc.setText("Golem: Tanque robusto.\nVida: 1.25x | Dano: 0.5x\nCusto: 100TU / 2 Mana");
				break;
			case "Vigilante":
				lblActionDesc.setText("Vigilante: Entidade narrativa.\nSem combate direto.\nCusto: 500TU / 5 Mana");
				break;
			case "Ecos":
				lblActionDesc.setText(
						"Ecos: Ataques rápidos com Sangramento (50%).\nVida: 0.75x | Dano: 0.75x\nCusto: 120TU / 2 Mana");
				break;
			case "Portador de Selo":
				lblActionDesc.setText(
						"Portador: Concede +1 Mana por turno enquanto vivo.\nVida: 0.5x | Dano: 0.01x\nCusto: 80TU / 4 Mana");
				break;
			case "Tecelão":
				lblActionDesc
						.setText("Tecelão: Dano massivo a distância.\nVida: 0.5x | Dano: 1.5x\nCusto: 125TU / 3 Mana");
				break;
			case "Dominus Albus":
				lblActionDesc.setText(
						"DOMINUS ALBUS: Entidade Suprema.\nBloqueia outras invocações.\nVida: 2.5x | Dano: 1.5x\nCusto: 50TU / 8 Mana");
				break;
			}
		}
	}

	private void adicionarInputExtra(String key, String labelText) {
		Label lbl = new Label(labelText);
		lbl.setStyle("-fx-text-fill: yellow;");
		TextField tf = new TextField();
		tf.setPromptText("Valor...");
		tf.setStyle("-fx-background-color: #333; -fx-text-fill: white;");

		inputsExtras.put(key, tf);
		diceInputsBox.getChildren().addAll(lbl, tf);
	}

	@FXML
	private void onConfirmarAcaoClick() {
		// Coleta dados
		AcaoMestreInput input;
		if (fantasmaNobreSelecionado != null) {
			input = new AcaoMestreInput(atorAtual, alvosNoMapa, fantasmaNobreSelecionado);
		} else {
			List<Personagem> alvosFinais = new ArrayList<>(alvosNoMapa);
			if (itemSelecionado != null && alvosFinais.isEmpty()) {
				alvosFinais.add(atorAtual);
			}

			input = new AcaoMestreInput(atorAtual, alvosFinais, habilidadeSelecionada);
			if (itemSelecionado != null)
				input.setItemSendoUsado(itemSelecionado);
		}

		if (toggleGroupOpcoes != null && toggleGroupOpcoes.getSelectedToggle() != null) {
			String escolha = (String) toggleGroupOpcoes.getSelectedToggle().getUserData();
			input.setOpcaoEscolhida(escolha);
		}

		// Epicentro
		if (epicentroX != -1)
			input.setEpicentro(epicentroX, epicentroY);

		// Dados
		if (inputDadoAtributo != null && !inputDadoAtributo.getText().isEmpty()) {
			try {
				input.adicionarResultadoDado("DADO_ATRIBUTO", Integer.parseInt(inputDadoAtributo.getText()));
			} catch (Exception e) {
			}
		}
		for (Map.Entry<String, TextField> entry : inputsExtras.entrySet()) {
			if (!entry.getValue().getText().isEmpty()) {
				try {
					input.adicionarResultadoDado(entry.getKey(), Integer.parseInt(entry.getValue().getText()));
				} catch (Exception e) {
				}
			}
		}

		// Modos de Ataque (Se for básico)
		if (isAtaqueBasico) {
			if (this.isModoCoronhadaSelecionado)
				input.setModoAtaque(ModoAtaque.CORONHADA);
			else if (toggleFraco.isSelected())
				input.setModoAtaque(ModoAtaque.FRACO);
			else if (toggleForte.isSelected())
				input.setModoAtaque(ModoAtaque.FORTE);
			else
				input.setModoAtaque(ModoAtaque.NORMAL);

			// Rajada
			if (boxRajada.isVisible()) {
				input.setTirosExtras((int) sliderRajada.getValue());
			}
		}

		// Envia
		if (fantasmaNobreSelecionado != null)
			mainController.resolverAcaoFantasmaNobre(input);
		else if (itemSelecionado != null)
			mainController.resolverAcaoItem(input);
		else
			mainController.resolverAcaoDoMestre(input);
	}

	// --- AUXILIARES VISUAIS ---

	private void atualizarEstimativaDano() {
		int rolagem = 0;
		try {
			if (inputDadoAtributo != null)
				rolagem = Integer.parseInt(inputDadoAtributo.getText());
		} catch (Exception e) {
		}

		// Passa null para habilidade se for ataque básico
		int dano = mainController.getCombatManager().estimarDano(atorAtual, habilidadeSelecionada, null, rolagem, 0);

		// Aplica Modificadores de UI (que o estimarDano do Manager ainda não sabe ler do FXML)
		// O ideal é passar o 'input' simulado para o manager, mas para simplificar aqui:
		double modVisual = 1.0;
		if (isAtaqueBasico) {
			if (toggleFraco.isSelected())
				modVisual = 0.75;
			if (toggleForte.isSelected())
				modVisual = 1.25;
			// Rajada é complexa de estimar aqui, mostra apenas o base (puta preguiça prç)
		}

		labelEstimativaDano.setText("Dano Base Est.: " + (int) (dano * modVisual));
	}

	private void atualizarTextoModo() {
		if (toggleFraco.isSelected())
			lblInfoModo.setText("0.75x Dano, -20% TU");
		else if (toggleForte.isSelected())
			lblInfoModo.setText("1.25x Dano, +20% TU, +1 Alcance");
		else
			lblInfoModo.setText("Dano Normal, TU Normal");
	}

	private Button criarBotaoAcao(String texto, String style) {
		Button btn = new Button(texto);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setPrefHeight(60);
		btn.setWrapText(true);
		btn.setStyle(style + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
		return btn;
	}

	private void adicionarAoGrid(Node node, int col, int row) {
		actionsGrid.add(node, col, row);
	}

	private void estilizarBotaoAba(Button btn, boolean ativo) {
		if (ativo) {
			btn.setStyle("-fx-background-color: #555; -fx-text-fill: cyan; -fx-border-color: cyan;");
		} else {
			btn.setStyle("-fx-background-color: #333; -fx-text-fill: gray; -fx-border-color: #444;");
		}
	}

	@FXML
	private void onSelecionarAlvoClick() {
		if (mainController == null)
			return;

		if (isAtaqueBasico) {
			Arma arma = atorAtual.getArmaEquipada();
			int alcanceBase = (arma != null) ? arma.getAlcance() : 1;

			// CORONHADA TEM ALCANCE 1 E É INDIVIDUAL
			if (this.isModoCoronhadaSelecionado) {
				alcanceBase = 1;
			} else if (toggleForte.isSelected()) {
				alcanceBase += 1;
			}

			final int alcanceFinal = alcanceBase;

			// Se for coronhada, força tipo INDIVIDUAL, senão usa o da arma (ex: CONE)
			final TipoAlvo tipoAlvoFinal = (this.isModoCoronhadaSelecionado) ? TipoAlvo.INDIVIDUAL
					: (arma != null ? arma.getTipoAlvo() : TipoAlvo.INDIVIDUAL);

			Habilidade dummyBasic = new Habilidade("Ataque Básico", "", TipoHabilidade.ATIVA, 0, 0, 0, tipoAlvoFinal,
					(arma != null ? arma.getTamanhoArea() : 0), 0, 0, null) {
				@Override
				public int getAlcanceMaximo() {
					return alcanceFinal;
				}

				@Override
				public int getAnguloCone() {
					return arma.getAnguloCone();
				}

				@Override
				public void executar(Personagem c, List<Personagem> a, EstadoCombate es, CombatManager m) {
				}
			};

			mainController.iniciarSelecaoDeAlvo(dummyBasic, atorAtual);

		} else if (habilidadeSelecionada != null) {
			mainController.iniciarSelecaoDeAlvo(habilidadeSelecionada, atorAtual);

		} else if (fantasmaNobreSelecionado != null) {
			Habilidade dummyFN = new Habilidade(fantasmaNobreSelecionado.getNome(), "", TipoHabilidade.ATIVA, 0, 0, 0,
					fantasmaNobreSelecionado.getTipoAlvo(), fantasmaNobreSelecionado.getTamanhoArea(), 0, 0, null) {
				@Override
				public int getAlcanceMaximo() {
					return 99;
				}

				@Override
				public void executar(Personagem c, List<Personagem> a, EstadoCombate es, CombatManager m) {
				}
			};
			mainController.iniciarSelecaoDeAlvo(dummyFN, atorAtual);
		}

		btnConfirmarAcao.getScene().getWindow().hide();
	}

	public void adicionarAlvos(List<Personagem> alvos) {
		this.alvosNoMapa = alvos;
		btnSelecionarAlvo.setText("Alvos: " + alvos.size());
		btnSelecionarAlvo.setStyle("-fx-background-color: #004400; -fx-text-fill: white;");
		btnConfirmarAcao.setDisable(false);
	}

	public void adicionarAlvosArea(List<Personagem> alvos, int x, int y) {
		this.epicentroX = x;
		this.epicentroY = y;
		adicionarAlvos(alvos);
	}

	public void adicionarAlvo(Personagem alvo) {
		if (alvo == null)
			return;
		this.alvosNoMapa.clear();
		this.alvosNoMapa.add(alvo);

		// Atualiza a UI da Coluna 3
		btnSelecionarAlvo.setText("Alvo: " + alvo.getNome());
		btnSelecionarAlvo.setStyle("-fx-background-color: #004400; -fx-text-fill: white;");
		btnConfirmarAcao.setDisable(false); // Habilita o confirmar

		atualizarEstimativaDano();
	}

	public void limparAlvosHover() {
		this.alvosNoMapa.clear();
		btnSelecionarAlvo.setText("Selecionar Alvo (Mapa)");
		btnSelecionarAlvo.setStyle(""); // Reseta estilo

		// Se a ação exige alvo, desabilita o confirmar
		boolean precisaAlvo = verificaSePrecisaAlvo();
		if (precisaAlvo) {
			btnConfirmarAcao.setDisable(true);
		}
	}

	public void configurarConfirmacaoSquad(int qtdAtaques) {
		// Força a exibição da coluna de detalhes
		actionDetailsColumn.setVisible(true);
		actionDetailsColumn.setManaged(true);

		lblActionTitle.setText("Ataque Coordenado");
		lblActionDesc.setText(qtdAtaques + " clones posicionados e mirando.");

		// Esconde opções irrelevantes
		attackOptionsBox.setVisible(false);
		attackOptionsBox.setManaged(false);
		diceInputsBox.getChildren().clear();

		// Configura o botão final
		btnConfirmarAcao.setText("EXECUTAR SQUAD");
		btnConfirmarAcao.setStyle("-fx-base: #AA0000; -fx-font-weight: bold;");
		btnConfirmarAcao.setDisable(false);

		// Redireciona a ação do botão
		btnConfirmarAcao.setOnAction(e -> {
			mainController.executarAtaqueSquadFinal();
			// Restaura o comportamento padrão do botão para o próximo turno
			btnConfirmarAcao.setOnAction(ev -> onConfirmarAcaoClick());
		});
	}

	@FXML
	private void onDetalhesClick() {
		boolean isVisible = detailedAttributesScrollPane.isVisible();
		detailedAttributesScrollPane.setVisible(!isVisible);
		detailedAttributesScrollPane.setManaged(!isVisible);

		if (!isVisible) {
			btnDetalhes.setText("Ocultar Detalhes");
			preencherAtributosDetalhados(); // Chama sempre para atualizar
		} else {
			btnDetalhes.setText("Ver Atributos Detalhados");
		}
	}

	private void preencherAtributosDetalhados() {
		if (detailedAttributesPane == null)
			return;

		// Se o FXML falhou em injetar ou o grid foi removido, recria
		if (attributesGrid == null) {
			System.out.println("AVISO: attributesGrid era null. Recriando manualmente.");
			attributesGrid = new GridPane();
			attributesGrid.setHgap(10);
			attributesGrid.setVgap(5);
			ColumnConstraints col1 = new ColumnConstraints();
			col1.setPercentWidth(40);
			ColumnConstraints col2 = new ColumnConstraints();
			col2.setPercentWidth(60);
			attributesGrid.getColumnConstraints().addAll(col1, col2);

			// Adiciona de volta ao painel, logo após o título ou no início
			detailedAttributesPane.getChildren().add(1, attributesGrid); // assume que index 0 é Label
		} else {
			// Limpa filhos do grid existente
			attributesGrid.getChildren().clear();
			attributesGrid.getRowConstraints().clear();
		}

		// Limpa labels antigos do Pane (Stats derivados), mas preserva o Grid e o Título
		detailedAttributesPane.getChildren()
				.removeIf(node -> node != attributesGrid
						&& !(node instanceof Label && ((Label) node).getText().startsWith("Atributos"))
						&& !(node instanceof Separator));

		// Popula o Grid
		br.com.dantesrpg.model.enums.Atributo[] ordem = { Atributo.FORCA, Atributo.PERCEPCAO, Atributo.ENDURANCE,
				Atributo.CARISMA, Atributo.INTELIGENCIA, Atributo.DESTREZA, Atributo.SORTE, Atributo.INSPIRACAO,
				Atributo.SAGACIDADE, Atributo.TOPOR };

		int row = 0;
		for (Atributo atr : ordem) {
			RowConstraints rc = new RowConstraints();
			rc.setMinHeight(25);
			rc.setPrefHeight(25);
			attributesGrid.getRowConstraints().add(rc);

			int valor = atorAtual.getAtributosFinais().getOrDefault(atr, 1);
			int dado = DiceRoller.getTipoDado(valor);

			Label lblNome = new Label(atr.name().substring(0, 3));
			lblNome.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold; -fx-font-size: 12px;");

			Label lblValor = new Label(valor + " (d" + dado + ")");
			lblValor.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

			attributesGrid.add(lblNome, 0, row);
			attributesGrid.add(lblValor, 1, row);
			row++;
		}

		boolean temSeparador = detailedAttributesPane.getChildren().stream().anyMatch(n -> n instanceof Separator);
		if (!temSeparador)
			detailedAttributesPane.getChildren().add(new Separator());

		// Adiciona Status Derivados
		addStatLabel("Movimento:", atorAtual.getMovimento() + " células");
		addStatLabel("Armadura:", String.valueOf(atorAtual.getArmaduraTotal()));
		addStatLabel("Red. Dano:", String.format("%.1f%%", atorAtual.getReducaoDanoArmadura() * 100));
		addStatLabel("Crítico:", String.format("%.1f%%", atorAtual.getTaxaCritica() * 100));
		addStatLabel("Dano Crítico:", String.format("+%.1f%%", atorAtual.getDanoCritico() * 100));
		addStatLabel("Bônus Dano:", String.format("+%.1f%%", atorAtual.getBonusDanoPercentual() * 100));

		detailedAttributesPane.requestLayout();
	}

	private void addStatLabel(String titulo, String valor) {
		HBox row = new HBox(10);
		Label t = new Label(titulo);
		t.setStyle("-fx-text-fill: cyan;");
		Label v = new Label(valor);
		v.setStyle("-fx-text-fill: white;");
		row.getChildren().addAll(t, v);
		detailedAttributesPane.getChildren().add(row);
	}

	private void atualizarInfoPersonagem() {
		labelNomeAtor.setText(atorAtual.getNome());
		String raca = (atorAtual.getRaca() != null) ? atorAtual.getRaca().getNome() : "N/A";
		String classe = (atorAtual.getClasse() != null) ? atorAtual.getClasse().getNome() : "N/A";
		labelClasseRaca.setText(raca + " / " + classe);

		// FORMATAÇÃO 1k aplicada
		String hpAtual = formatarNumero(atorAtual.getVidaAtual());
		String hpMax = formatarNumero(atorAtual.getVidaMaxima());
		labelHP.setText(hpAtual + "/" + hpMax);

		labelMP.setText((int) atorAtual.getManaAtual() + "/" + (int) atorAtual.getManaMaxima());
		labelTU.setText("TU: " + atorAtual.getContadorTU());

		updateEffects();
	}

	private void updateEffects() {
		if (effectsContainer == null)
			return;

		effectsContainer.getChildren().clear(); // Limpeza vital!

		if (atorAtual.getEfeitosAtivos() != null) {
			// Cria uma cópia segura da lista de efeitos
			List<Efeito> listaEfeitos = new ArrayList<>(atorAtual.getEfeitosAtivos().values());

			for (Efeito efeito : listaEfeitos) {
				// Criação do Label
				String texto = efeito.getNome();
				if (efeito.getStacks() > 0)
					texto += " (" + efeito.getStacks() + ")";

				Label lblEfeito = new Label(texto + " [" + efeito.getDuracaoTURestante() + "]");
				lblEfeito.setMaxWidth(Double.MAX_VALUE);

				// Estilização baseada no tipo
				lblEfeito.getStyleClass().clear();
				if (efeito.getTipo() == TipoEfeito.BUFF) {
					lblEfeito.setStyle(
							"-fx-background-color: #004466; -fx-text-fill: cyan; -fx-padding: 3; -fx-background-radius: 3;");
				} else if (efeito.getTipo() == TipoEfeito.DEBUFF) {
					lblEfeito.setStyle(
							"-fx-background-color: #660000; -fx-text-fill: #ffaaaa; -fx-padding: 3; -fx-background-radius: 3;");
				} else { // DOT
					lblEfeito.setStyle(
							"-fx-background-color: #440044; -fx-text-fill: violet; -fx-padding: 3; -fx-background-radius: 3;");
				}

				// Tooltip detalhado
				String desc = efeito.getNome() + "\nDuração: " + efeito.getDuracaoTURestante() + " TU";
				if (efeito.getDanoPorTick() > 0)
					desc += "\nDano: " + efeito.getDanoPorTick() + "/tick";
				// Adicionarei aqui descrições específicas se for necessario (switch case do nom para cada)

				Tooltip.install(lblEfeito, new Tooltip(desc));

				effectsContainer.getChildren().add(lblEfeito);
			}
		}
	}

	private String formatarNumero(double valor) {
		if (valor >= 1000) {
			return String.format("%.1fk", valor / 1000.0).replace(",", ".");
		}
		return String.format("%.0f", valor);
	}
}