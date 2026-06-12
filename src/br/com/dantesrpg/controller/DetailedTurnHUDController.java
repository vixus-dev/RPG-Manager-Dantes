package br.com.dantesrpg.controller;

import br.com.dantesrpg.controller.hud.ActionGridBuilder;
import br.com.dantesrpg.controller.hud.CharacterInfoRenderer;
import br.com.dantesrpg.controller.hud.DiceInputsBuilder;
import br.com.dantesrpg.controller.hud.DiceInputsBuilder.DiceInputsResult;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.CombatManager.PacoteMunicao;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.DiceRoller;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
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
	private VBox municaoContainer;
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
	private Button btnAbaAtaques, btnAbaItens;
	@FXML
	private GridPane actionsGrid;
	@FXML
	private HBox tacticalButtonsBox;
	@FXML
	private Button btnMovimentar, btnPassarVez, btnRecarregar;

	// --- Coluna 3 (Detalhes) ---
	@FXML
	private VBox actionDetailsColumn;
	@FXML
	private Label lblActionTitle, lblActionDesc;
	@FXML
	private HBox costInfoBox;
	@FXML
	private Label lblCustoTU, lblCustoMana;

	// Opções de Ataque
	@FXML
	private VBox attackOptionsBox;
	@FXML
	private VBox boxSelecaoArmas;
	@FXML
	private ComboBox<String> comboArmasAtaque;
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

	// --- Coluna 4 (Rolagem de Dados) ---
	@FXML
	private VBox diceRollColumn;
	@FXML
	private Label lblDiceType, lblDiceResult, lblCritRate, lblCritResult;
	@FXML
	private Button btnRolarDado, btnRolarCritico;

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
	private List<List<Arma>> opcoesArmasAtaque = new ArrayList<>();

	private record SelecaoRecarga(Arma arma, PacoteMunicao pacote) {
	}

	// Inputs Dinâmicos
	private TextField inputDadoAtributo;
	private Map<String, TextField> inputsExtras = new HashMap<>();

	// Estado de Rolagem Manual
	private int tipoDadoAtual = 20;
	private boolean criticoFoiRolado = false;
	private boolean criticoManualRolado = false;

	// Alvos selecionados no mapa
	private List<Personagem> alvosNoMapa = new ArrayList<>();
	private int epicentroX = -1, epicentroY = -1;

	// --- Subsystems (instanciados em initialize()) ---
	private ActionGridBuilder actionGridBuilder;
	private DiceInputsBuilder diceInputsBuilder;
	private CharacterInfoRenderer characterInfoRenderer;

	@FXML
	public void initialize() {
		// Listener para o Slider de Rajada
		sliderRajada.valueProperty().addListener((obs, oldVal, newVal) -> {
			int tiros = newVal.intValue();
			lblInfoRajada.setText("+" + tiros + " Tiros (+" + (tiros * 10) + "% TU)");
			atualizarEstimativaDano();
			atualizarCustosExibidos();
			enviarTUPreview();
		});

		// Listener para o Grupo de Modos
		grupoModo.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == null) {
				toggleNormal.setSelected(true);
			} else {
				atualizarTextoModo();
				atualizarEstimativaDano();
			}
		});

		// Instancia subsystems com referências aos nós FXML
		comboArmasAtaque.valueProperty().addListener((obs, oldVal, newVal) -> {
			configurarOpcoesArma();
			atualizarCustosExibidos();
			atualizarEstimativaDano();
			enviarTUPreview();
		});
		actionGridBuilder = new ActionGridBuilder(actionsGrid, null); // mainController definido em setAtor
		diceInputsBuilder = new DiceInputsBuilder(diceInputsBox, diceRollColumn,
				lblDiceType, lblDiceResult, lblCritRate, lblCritResult);
		characterInfoRenderer = new CharacterInfoRenderer(labelNomeAtor, labelClasseRaca,
				labelHP, labelMP, labelTU,
				effectsContainer, detailedAttributesScrollPane,
				detailedAttributesPane, attributesGrid);
	}

	public void setAtor(Personagem ator, CombatController controller) {
		this.atorAtual = ator;
		this.mainController = controller;
		this.alvosNoMapa.clear();

		// Reconstrói ActionGridBuilder com o controller correto
		actionGridBuilder = new ActionGridBuilder(actionsGrid, mainController);

		atualizarInfoPersonagem();

		// Abre na aba de ataques por padrão
		onAbaAtaquesClick();

		// Esconde a coluna de detalhes até selecionar algo
		actionDetailsColumn.setVisible(false);
		actionDetailsColumn.setManaged(false);
		diceRollColumn.setVisible(false);
		diceRollColumn.setManaged(false);

		// Limpa TU preview anterior
		if (mainController != null) mainController.limparTUPreview();

		// Configura botão Recarregar (só visível se arma usa munição)
		boolean mostrarRecarregar = ator.getArmasEquipadas().stream().anyMatch(Arma::isRequerMunicao);
		btnRecarregar.setVisible(mostrarRecarregar);
		btnRecarregar.setManaged(mostrarRecarregar);
	}

	// --- ABAS ---

	@FXML
	private void onAbaAtaquesClick() {
		estilizarBotaoAba(btnAbaAtaques, true);
		estilizarBotaoAba(btnAbaItens, false);
		actionGridBuilder.popularAbaAtaques(atorAtual,
				(titulo, hab, item, fn, isBasic) -> prepararAcao(titulo, hab, item, fn, isBasic),
				fn -> lidarComInvocacaoAyame(fn));
	}

	@FXML
	private void onAbaItensClick() {
		estilizarBotaoAba(btnAbaAtaques, false);
		estilizarBotaoAba(btnAbaItens, true);
		actionGridBuilder.popularAbaItens(atorAtual,
				(titulo, hab, item, fn, isBasic) -> prepararAcao(titulo, hab, item, fn, isBasic));
	}

	private void lidarComInvocacaoAyame(FantasmaNobre fn) {
		// Busca essências no inventário
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

		ChoiceDialog<br.com.dantesrpg.model.items.EssenciaInimigo> dialog =
				new ChoiceDialog<>(essencias.get(0), essencias);
		dialog.setTitle("Invocação Profana");
		dialog.setHeaderText("Escolha a alma para invocar:");
		dialog.setContentText("Essência:");
		dialog.setResultConverter(bt -> bt == ButtonType.OK ? dialog.getSelectedItem() : null);
		dialog.getDialogPane().setStyle("-fx-background-color: #222;");
		dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");

		Optional<br.com.dantesrpg.model.items.EssenciaInimigo> result = dialog.showAndWait();
		if (result.isPresent()) {
			br.com.dantesrpg.model.items.EssenciaInimigo essenciaEscolhida = result.get();
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
	private void onMovimentarClick() {
		mainController.iniciarMovimentoTaticoComRetorno(atorAtual);
	}

	@FXML
	private void onPassarVezClick() {
		mainController.resolverAcaoPassarVez(new AcaoMestreInput(atorAtual, new ArrayList<>(), (Habilidade) null));
	}

	@FXML
	private void onRecarregarClick() {
		CombatManager manager = mainController.getCombatManager();
		List<Arma> armas = manager.getArmasDeFogoParaRecarregar(atorAtual);
		List<PacoteMunicao> pacotes = manager.getPacotesMunicaoDisponiveis(atorAtual);

		if (armas.isEmpty()) {
			mostrarAvisoRecarga("Nenhuma arma precisa de recarga.",
					"As armas de fogo equipadas já estão com munição cheia.");
			return;
		}
		if (pacotes.isEmpty()) {
			mostrarAvisoRecarga("Recarga bloqueada.",
					atorAtual.getNome() + " não possui Caixa de Munição ou Pacote de Munição no inventário.");
			return;
		}

		Optional<SelecaoRecarga> selecao = obterSelecaoRecarga(armas, pacotes);
		if (selecao.isEmpty()) {
			return;
		}

		boolean recarregou = manager.resolverAcaoRecarregar(atorAtual, selecao.get().arma(), selecao.get().pacote());
		if (!recarregou) {
			mostrarAvisoRecarga("Recarga não realizada.", "Verifique arma, munição disponível e inventário.");
			atualizarInfoPersonagem();
			return;
		}

		atualizarInfoPersonagem();
		onAbaAtaquesClick();
		mainController.avancarTurnoAposAcao();
	}

	private Optional<SelecaoRecarga> obterSelecaoRecarga(List<Arma> armas, List<PacoteMunicao> pacotes) {
		if (armas.size() == 1 && pacotes.size() == 1) {
			return Optional.of(new SelecaoRecarga(armas.get(0), pacotes.get(0)));
		}

		Dialog<SelecaoRecarga> dialog = new Dialog<>();
		dialog.setTitle("Recarregar");
		dialog.setHeaderText("Escolha a arma e o pacote de munição.");

		ButtonType btnConfirmar = new ButtonType("Recarregar", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

		ComboBox<Arma> comboArmas = new ComboBox<>();
		comboArmas.getItems().addAll(armas);
		comboArmas.setMaxWidth(Double.MAX_VALUE);
		comboArmas.setConverter(new StringConverter<>() {
			@Override
			public String toString(Arma arma) {
				return arma == null ? "" : arma.getNome() + " [" + arma.getMunicaoAtual() + "/"
						+ arma.getMunicaoMaxima() + "]";
			}

			@Override
			public Arma fromString(String string) {
				return null;
			}
		});
		comboArmas.getSelectionModel().selectFirst();

		ComboBox<PacoteMunicao> comboPacotes = new ComboBox<>();
		comboPacotes.getItems().addAll(pacotes);
		comboPacotes.setMaxWidth(Double.MAX_VALUE);
		comboPacotes.getSelectionModel().selectFirst();

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.add(new Label("Arma:"), 0, 0);
		grid.add(comboArmas, 1, 0);
		grid.add(new Label("Pacote:"), 0, 1);
		grid.add(comboPacotes, 1, 1);
		ColumnConstraints labelColumn = new ColumnConstraints();
		labelColumn.setMinWidth(70);
		ColumnConstraints inputColumn = new ColumnConstraints();
		inputColumn.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(labelColumn, inputColumn);

		dialog.getDialogPane().setContent(grid);
		Node botaoConfirmar = dialog.getDialogPane().lookupButton(btnConfirmar);
		botaoConfirmar.disableProperty().bind(comboArmas.valueProperty().isNull()
				.or(comboPacotes.valueProperty().isNull()));
		dialog.setResultConverter(button -> button == btnConfirmar
				? new SelecaoRecarga(comboArmas.getValue(), comboPacotes.getValue())
				: null);

		return dialog.showAndWait();
	}

	private void mostrarAvisoRecarga(String titulo, String mensagem) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Recarregar");
		alert.setHeaderText(titulo);
		alert.setContentText(mensagem);
		alert.showAndWait();
	}

	// --- PREPARAÇÃO DA AÇÃO (Coluna 3) ---

	private void prepararAcao(String titulo, Habilidade hab, Item item, FantasmaNobre fn, boolean isBasicAttack) {
		this.habilidadeSelecionada = hab;
		this.itemSelecionado = item;
		this.fantasmaNobreSelecionado = fn;
		this.isAtaqueBasico = isBasicAttack;
		this.alvosNoMapa.clear();
		this.epicentroX = -1;

		// Mostra painel
		actionDetailsColumn.setVisible(true);
		actionDetailsColumn.setManaged(true);
		lblActionTitle.setText(titulo);

		// Visual ciano para arma overclockada
		if (isBasicAttack && atorAtual.getArmaEquipada() != null && atorAtual.getArmaEquipada().isOverclockado()) {
			Arma armaOC = atorAtual.getArmaEquipada();
			lblActionTitle.setText(armaOC.getNomeComOverclock());
			lblActionTitle.setStyle("-fx-text-fill: cyan; -fx-effect: dropshadow(gaussian, cyan, 4, 0.3, 0, 0);");
		} else {
			lblActionTitle.setStyle("");
		}

		// Descrição
		if (hab != null) lblActionDesc.setText(hab.getDescricao());
		else if (item != null) lblActionDesc.setText(item.getDescricao());
		else if (fn != null) lblActionDesc.setText(fn.getDescricao());
		else if (isBasicAttack) lblActionDesc.setText("Ataque com a arma equipada.");

		atualizarCustosExibidos();

		// Opções de Ataque (apenas para armas)
		if (isBasicAttack) {
			attackOptionsBox.setVisible(true);
			attackOptionsBox.setManaged(true);
			configurarSelecaoArmas();
			configurarOpcoesArma();
		} else {
			attackOptionsBox.setVisible(false);
			attackOptionsBox.setManaged(false);
			boxSelecaoArmas.setVisible(false);
			boxSelecaoArmas.setManaged(false);
		}

		// Inputs de Dados (delegado ao builder)
		gerarInputsDeDados();

		configurarBotaoAlvo();
		atualizarEstimativaDano();
		enviarTUPreview();
	}

	private int obterCustoTUAtual() {
		int base = 0;
		if (isAtaqueBasico) {
			List<Arma> armas = obterArmasSelecionadasAtaque();
			base = armas.stream().mapToInt(Arma::getCustoTU).sum();
			if (armas.size() >= 2) base = (int) (base * 0.65);
			if (toggleFraco.isSelected()) base = (int)(base * 0.80);
			else if (toggleForte.isSelected()) base = (int)(base * 1.20);
			if (boxRajada.isVisible()) {
				int tirosExtras = (int) sliderRajada.getValue();
				base = (int)(base * (1.0 + tirosExtras * 0.10));
			}
		} else if (habilidadeSelecionada != null) {
			base = habilidadeSelecionada.getCustoTU();
		} else if (fantasmaNobreSelecionado != null) {
			base = fantasmaNobreSelecionado.getCustoTU();
		} else if (itemSelecionado != null) {
			base = itemSelecionado.getCustoTU();
		}
		return base;
	}

	private int obterCustoManaAtual() {
		if (isAtaqueBasico) return 0;
		if (habilidadeSelecionada != null) return habilidadeSelecionada.getCustoMana();
		if (fantasmaNobreSelecionado != null) return fantasmaNobreSelecionado.getCustoMana();
		return 0;
	}

	private void atualizarCustosExibidos() {
		int custoTU = obterCustoTUAtual();
		int custoMana = obterCustoManaAtual();

		lblCustoTU.setText(String.valueOf(custoTU));
		if (custoMana > 0) {
			lblCustoMana.setText("-" + custoMana);
			lblCustoMana.setStyle("-fx-text-fill: #66bbff; -fx-font-weight: bold; -fx-font-size: 13px;");
		} else if (custoMana == 0 && isAtaqueBasico) {
			lblCustoMana.setText("+0");
			lblCustoMana.setStyle("-fx-text-fill: #888; -fx-font-weight: bold; -fx-font-size: 13px;");
		} else {
			lblCustoMana.setText("0");
			lblCustoMana.setStyle("-fx-text-fill: #888; -fx-font-weight: bold; -fx-font-size: 13px;");
		}
	}

	private void enviarTUPreview() {
		if (mainController == null || atorAtual == null) return;
		int tuPreview = atorAtual.getContadorTU() + obterCustoTUAtual();
		mainController.mostrarTUPreview(atorAtual, tuPreview);
	}

	private void configurarBotaoAlvo() {
		boolean precisaAlvo = verificaSePrecisaAlvo();
		if (precisaAlvo) {
			btnSelecionarAlvo.setDisable(false);
			btnSelecionarAlvo.setText("Selecionar Alvo (Mapa)");
			btnConfirmarAcao.setDisable(true);
		} else {
			btnSelecionarAlvo.setDisable(true);
			btnSelecionarAlvo.setText("N/A (Auto/Self)");
			btnConfirmarAcao.setDisable(false);
		}
	}

	private boolean verificaSePrecisaAlvo() {
		if (isAtaqueBasico) return true;

		TipoAlvo tipo = TipoAlvo.INDIVIDUAL;
		if (habilidadeSelecionada != null) tipo = habilidadeSelecionada.getTipoAlvo();
		else if (fantasmaNobreSelecionado != null) tipo = fantasmaNobreSelecionado.getTipoAlvo();
		else if (itemSelecionado != null) return false;

		return (tipo == TipoAlvo.INDIVIDUAL || tipo == TipoAlvo.MULTIPLOS || tipo == TipoAlvo.AREA_QUADRADA
				|| tipo == TipoAlvo.AREA_CIRCULAR || tipo == TipoAlvo.AREA || tipo == TipoAlvo.LINHA
				|| tipo == TipoAlvo.CONE);
	}

	private void configurarSelecaoArmas() {
		opcoesArmasAtaque.clear();
		comboArmasAtaque.getItems().clear();

		List<Arma> armas = atorAtual != null ? atorAtual.getArmasEquipadas() : new ArrayList<>();
		if (armas.size() < 2) {
			boxSelecaoArmas.setVisible(false);
			boxSelecaoArmas.setManaged(false);
			if (!armas.isEmpty()) {
				opcoesArmasAtaque.add(new ArrayList<>(armas));
			}
			return;
		}

		boxSelecaoArmas.setVisible(true);
		boxSelecaoArmas.setManaged(true);
		int totalCombinacoes = 1 << armas.size();
		for (int mascara = 1; mascara < totalCombinacoes; mascara++) {
			List<Arma> combinacao = new ArrayList<>();
			for (int i = 0; i < armas.size(); i++) {
				if ((mascara & (1 << i)) != 0) {
					combinacao.add(armas.get(i));
				}
			}
			opcoesArmasAtaque.add(combinacao);
			comboArmasAtaque.getItems().add(formatarOpcaoArmas(combinacao));
		}
		comboArmasAtaque.getSelectionModel().select(comboArmasAtaque.getItems().size() - 1);
	}

	private List<Arma> obterArmasSelecionadasAtaque() {
		if (!isAtaqueBasico || atorAtual == null) {
			return new ArrayList<>();
		}
		if (opcoesArmasAtaque.isEmpty()) {
			return new ArrayList<>(atorAtual.getArmasEquipadas());
		}
		int indice = comboArmasAtaque.getSelectionModel().getSelectedIndex();
		if (indice < 0 || indice >= opcoesArmasAtaque.size()) {
			indice = opcoesArmasAtaque.size() - 1;
		}
		return new ArrayList<>(opcoesArmasAtaque.get(indice));
	}

	private String formatarOpcaoArmas(List<Arma> armas) {
		return armas.stream()
				.map(Arma::getNome)
				.reduce((a, b) -> a + " + " + b)
				.orElse("Desarmado");
	}

	private void configurarOpcoesArma() {
		List<Arma> armasSelecionadas = obterArmasSelecionadasAtaque();
		Arma arma = armasSelecionadas.isEmpty() ? atorAtual.getArmaEquipada() : armasSelecionadas.get(0);
		boolean temAtaqueAlternativo = arma != null && arma.hasAtaqueAlternativoBasico();
		boolean isRanged = armasSelecionadas.stream().anyMatch(a -> a instanceof br.com.dantesrpg.model.ArmaRanged || "Ranged".equalsIgnoreCase(a.getTipo()));

		this.isModoCoronhadaSelecionado = false;
		btnCoronhada.setText((arma != null) ? arma.getNomeAtaqueAlternativoBasico() : "Coronhada");
		btnCoronhada.setStyle("");
		lblActionDesc.setStyle("");

		if (isRanged) {
			toggleFraco.setVisible(false);
			toggleFraco.setManaged(false);
			toggleNormal.setVisible(true);
			toggleNormal.setManaged(true);
			toggleForte.setVisible(true);
			toggleForte.setManaged(true);
			btnCoronhada.setVisible(temAtaqueAlternativo);
			btnCoronhada.setManaged(temAtaqueAlternativo);

			StringBuilder desc = new StringBuilder();
			boolean semMunicao = false;
			for (Arma a : armasSelecionadas) {
				if (desc.length() > 0) desc.append("\n");
				desc.append(a.getNome()).append(": ").append(a.getDescricao());
				if (a.isRequerMunicao()) {
					desc.append(" (Munição: ").append(a.getMunicaoAtual()).append("/").append(a.getMunicaoMaxima()).append(")");
					if (a.getMunicaoAtual() <= 0) semMunicao = true;
				}
			}
			lblActionDesc.setText(desc.toString());
			if (semMunicao) lblActionDesc.setStyle("-fx-text-fill: red;");

			int maxTirosExtras = 0;
			for (Arma a : armasSelecionadas) {
				if (a.isRequerMunicao() && (a instanceof br.com.dantesrpg.model.ArmaRanged || "Ranged".equalsIgnoreCase(a.getTipo()))) {
					maxTirosExtras = Math.max(maxTirosExtras, a.getMunicaoAtual() - 1);
				}
			}

			if (maxTirosExtras > 0) {
				boxRajada.setVisible(true);
				boxRajada.setManaged(true);
				sliderRajada.setMax(maxTirosExtras);
				sliderRajada.setValue(0);
				lblInfoRajada.setText("+0 Tiros");
			} else {
				boxRajada.setVisible(false);
				boxRajada.setManaged(false);
			}

			boolean armaPrincipalSemMunicao = arma != null && arma.isRequerMunicao() && arma.getMunicaoAtual() <= 0;
			if (armaPrincipalSemMunicao) {
				toggleNormal.setDisable(true);
				toggleForte.setDisable(true);
				if (temAtaqueAlternativo) btnCoronhada.fire();
			} else {
				toggleNormal.setDisable(false);
				toggleForte.setDisable(false);
			}

			btnCoronhada.setOnAction(e -> {
				btnCoronhada.setStyle("-fx-base: #AA5500; -fx-border-color: white;");
				this.isModoCoronhadaSelecionado = true;
				atualizarTextoModo();
				atualizarEstimativaDano();
			});
			toggleNormal.setOnAction(e -> resetarCoronhada());
			toggleForte.setOnAction(e -> resetarCoronhada());
		} else {
			// Melee
			toggleFraco.setVisible(true);  toggleFraco.setManaged(true);
			toggleNormal.setVisible(true); toggleNormal.setManaged(true);
			toggleForte.setVisible(true);  toggleForte.setManaged(true);
			btnCoronhada.setVisible(temAtaqueAlternativo);
			btnCoronhada.setManaged(temAtaqueAlternativo);
			boxRajada.setVisible(false);
			boxRajada.setManaged(false);

			StringBuilder desc = new StringBuilder();
			for (Arma a : armasSelecionadas) {
				if (desc.length() > 0) desc.append("\n");
				desc.append(a.getNome()).append(": ").append(a.getDescricao());
			}
			lblActionDesc.setText(desc.toString());
			toggleNormal.setDisable(false);
			toggleForte.setDisable(false);
			toggleFraco.setDisable(false);
		}

		toggleFraco.setOnAction(e -> resetarCoronhada());
		toggleNormal.setOnAction(e -> resetarCoronhada());
		toggleForte.setOnAction(e -> resetarCoronhada());
		btnCoronhada.setOnAction(e -> {
			this.isModoCoronhadaSelecionado = true;
			btnCoronhada.setStyle("-fx-base: #AA5500; -fx-border-color: white;");
			atualizarTextoModo();
			atualizarEstimativaDano();
		});

		toggleNormal.setSelected(true);
		atualizarTextoModo();
	}

	private void resetarCoronhada() {
		this.isModoCoronhadaSelecionado = false;
		btnCoronhada.setStyle("");
		atualizarTextoModo();
		atualizarEstimativaDano();
	}

	// --- INPUTS DE DADOS (delegado ao DiceInputsBuilder) ---

	private void gerarInputsDeDados() {
		this.criticoFoiRolado = false;
		this.criticoManualRolado = false;

		DiceInputsResult result = diceInputsBuilder.build(
				atorAtual,
				habilidadeSelecionada,
				fantasmaNobreSelecionado,
				isAtaqueBasico,
				() -> atualizarEstimativaDano(),
				val -> {
					atualizarDescricaoPorOpcao(val);
					btnConfirmarAcao.setText(val.equalsIgnoreCase("Material") ? "ESCOLHER MATERIAL" : "CONFIRMAR AÇÃO");
				});

		this.inputDadoAtributo = result.inputDadoAtributo;
		this.inputsExtras = result.inputsExtras;
		this.tipoDadoAtual = result.tipoDado;
		this.toggleGroupOpcoes = result.toggleGroupOpcoes;
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
				lblActionDesc.setText("Ecos: Ataques rápidos com Sangramento (50%).\nVida: 0.75x | Dano: 0.75x\nCusto: 120TU / 2 Mana");
				break;
			case "Portador de Selo":
				lblActionDesc.setText("Portador: Concede +1 Mana por turno enquanto vivo.\nVida: 0.5x | Dano: 0.01x\nCusto: 80TU / 4 Mana");
				break;
			case "Tecelão":
				lblActionDesc.setText("Tecelão: Dano massivo a distância.\nVida: 0.5x | Dano: 1.5x\nCusto: 125TU / 3 Mana");
				break;
			case "Dominus Albus":
				lblActionDesc.setText("DOMINUS ALBUS: Entidade Suprema.\nBloqueia outras invocações.\nVida: 2.5x | Dano: 1.5x\nCusto: 50TU / 8 Mana");
				break;
			}
		}
	}

	private int obterRolagemAtributoAtual() {
		if (inputDadoAtributo == null || inputDadoAtributo.getText().isEmpty()) return 0;
		try {
			return Integer.parseInt(inputDadoAtributo.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	private ModoAtaque obterModoAtaqueAtual() {
		if (!isAtaqueBasico) return ModoAtaque.NORMAL;
		if (this.isModoCoronhadaSelecionado) return ModoAtaque.CORONHADA;
		if (toggleFraco.isSelected()) return ModoAtaque.FRACO;
		if (toggleForte.isSelected()) return ModoAtaque.FORTE;
		return ModoAtaque.NORMAL;
	}

	private int obterTirosExtrasAtuais() {
		if (!isAtaqueBasico || !boxRajada.isVisible()) return 0;
		return (int) sliderRajada.getValue();
	}

	@FXML
	private void onConfirmarAcaoClick() {
		if (atorAtual != null && atorAtual.isClone() && habilidadeSelecionada != null && !verificaSePrecisaAlvo()) {
			mainController.executarAcaoClonesSemAlvo(habilidadeSelecionada, obterRolagemAtributoAtual());
			return;
		}

		AcaoMestreInput input;
		if (fantasmaNobreSelecionado != null) {
			input = new AcaoMestreInput(atorAtual, alvosNoMapa, fantasmaNobreSelecionado);
		} else {
			List<Personagem> alvosFinais = new ArrayList<>(alvosNoMapa);
			if (itemSelecionado != null && alvosFinais.isEmpty()) alvosFinais.add(atorAtual);
			input = new AcaoMestreInput(atorAtual, alvosFinais, habilidadeSelecionada);
			if (itemSelecionado != null) input.setItemSendoUsado(itemSelecionado);
		}

		if (toggleGroupOpcoes != null && toggleGroupOpcoes.getSelectedToggle() != null) {
			input.setOpcaoEscolhida((String) toggleGroupOpcoes.getSelectedToggle().getUserData());
		}

		if (epicentroX != -1) input.setEpicentro(epicentroX, epicentroY);

		// Dado de atributo principal
		if (inputDadoAtributo != null && !inputDadoAtributo.getText().isEmpty()) {
			try {
				int rolagemBruta = Integer.parseInt(inputDadoAtributo.getText());
				Atributo atr = DiceInputsBuilder.resolverAtributo(atorAtual, habilidadeSelecionada);
				int valorAtr = atorAtual.getAtributosFinais().getOrDefault(atr, 1);
				int rolagemFinal = DiceRoller.aplicarBonusDeRank(rolagemBruta, valorAtr);
				input.adicionarResultadoDado("DADO_ATRIBUTO", rolagemFinal);
			} catch (Exception ignored) {}
		}

		// Inputs extras
		for (Map.Entry<String, TextField> entry : inputsExtras.entrySet()) {
			if (!entry.getValue().getText().isEmpty()) {
				try {
					input.adicionarResultadoDado(entry.getKey(), Integer.parseInt(entry.getValue().getText()));
				} catch (Exception ignored) {}
			}
		}

		// Crítico manual
		if (criticoFoiRolado) input.setCriticoManual(criticoManualRolado);

		// Modos de ataque
		if (isAtaqueBasico) {
			input.setArmasSelecionadas(obterArmasSelecionadasAtaque());
			input.setModoAtaque(obterModoAtaqueAtual());
			if (boxRajada.isVisible()) input.setTirosExtras((int) sliderRajada.getValue());
		}

		// Despacha para o controller principal
		if (fantasmaNobreSelecionado != null) mainController.resolverAcaoFantasmaNobre(input);
		else if (itemSelecionado != null) mainController.resolverAcaoItem(input);
		else mainController.resolverAcaoDoMestre(input);
	}

	// --- ROLAGEM MANUAL DE DADOS ---

	@FXML
	private void onRolarDadoClick() {
		int resultado = DiceRoller.rolarDado(tipoDadoAtual);
		lblDiceResult.setText(String.valueOf(resultado));

		if (resultado == tipoDadoAtual) {
			lblDiceResult.setStyle("-fx-text-fill: gold; -fx-font-size: 28px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, gold, 6, 0.4, 0, 0);");
		} else {
			lblDiceResult.setStyle("-fx-text-fill: cyan; -fx-font-size: 28px; -fx-font-weight: bold;");
		}

		if (inputDadoAtributo != null) inputDadoAtributo.setText(String.valueOf(resultado));
	}

	@FXML
	private void onRolarCriticoClick() {
		double rolagem = Math.random();
		boolean critico = rolagem < atorAtual.getTaxaCritica();

		this.criticoFoiRolado = true;
		this.criticoManualRolado = critico;

		if (critico) {
			lblCritResult.setText("CRÍTICO!");
			lblCritResult.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 18px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, red, 6, 0.4, 0, 0);");
		} else {
			lblCritResult.setText("Normal");
			lblCritResult.setStyle("-fx-text-fill: #888888; -fx-font-size: 18px; -fx-font-weight: bold;");
		}

		atualizarEstimativaDano();
	}

	// --- AUXILIARES VISUAIS ---

	private void atualizarEstimativaDano() {
		int rolagem = 0;
		try {
			if (inputDadoAtributo != null && !inputDadoAtributo.getText().isEmpty()) {
				int rolagemBruta = Integer.parseInt(inputDadoAtributo.getText());
				Atributo atr = DiceInputsBuilder.resolverAtributo(atorAtual, habilidadeSelecionada);
				int valorAtr = atorAtual.getAtributosFinais().getOrDefault(atr, 1);
				rolagem = DiceRoller.aplicarBonusDeRank(rolagemBruta, valorAtr);
			}
		} catch (Exception ignored) {}

		int dano = mainController.getCombatManager().estimarDano(atorAtual, habilidadeSelecionada, null, rolagem, 0);

		double modVisual = 1.0;
		if (isAtaqueBasico) {
			Arma arma = atorAtual != null ? atorAtual.getArmaEquipada() : null;
			if (this.isModoCoronhadaSelecionado && arma != null)
				modVisual = arma.getMultiplicadorAtaqueAlternativoBasico();
			else if (toggleFraco.isSelected()) modVisual = 0.75;
			else if (toggleForte.isSelected()) modVisual = 1.25;
		}

		int danoFinal = (int) (dano * modVisual);

		if (criticoFoiRolado && criticoManualRolado && atorAtual != null) {
			danoFinal = (int) (danoFinal * (1 + atorAtual.getDanoCritico()));
			labelEstimativaDano.setText("Dano Est. (CRIT): " + danoFinal);
			labelEstimativaDano.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff4444;");
		} else {
			labelEstimativaDano.setText("Dano Base Est.: " + danoFinal);
			labelEstimativaDano.setStyle("-fx-font-weight: bold; -fx-text-fill: lightgreen;");
		}
	}

	private void atualizarTextoModo() {
		Arma arma = atorAtual != null ? atorAtual.getArmaEquipada() : null;
		if (this.isModoCoronhadaSelecionado && arma != null)
			lblInfoModo.setText(arma.getNomeAtaqueAlternativoBasico() + ": " + arma.getDescricaoAtaqueAlternativoBasico());
		else if (toggleFraco.isSelected())  lblInfoModo.setText("0.75x Dano, -20% TU");
		else if (toggleForte.isSelected())  lblInfoModo.setText("1.25x Dano, +20% TU, +1 Alcance");
		else                                lblInfoModo.setText("Dano Normal, TU Normal");

		atualizarCustosExibidos();
		enviarTUPreview();
	}

	private Button criarBotaoAcao(String texto, String style) {
		Button btn = new Button(texto);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setPrefHeight(60);
		btn.setWrapText(true);
		btn.setStyle(style + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
		return btn;
	}

	private void estilizarBotaoAba(Button btn, boolean ativo) {
		if (ativo) btn.setStyle("-fx-background-color: #555; -fx-text-fill: cyan; -fx-border-color: cyan;");
		else       btn.setStyle("-fx-background-color: #333; -fx-text-fill: gray; -fx-border-color: #444;");
	}

	private Habilidade criarHabilidadeSelecaoAtaqueBasico() {
		List<Arma> armasSelecionadas = obterArmasSelecionadasAtaque();
		Arma arma = armasSelecionadas.isEmpty() ? atorAtual.getArmaEquipada() : armasSelecionadas.get(0);
		int alcanceBase = (arma != null) ? arma.getAlcance() : 1;
		TipoAlvo tipoAlvoBase = (arma != null) ? arma.getTipoAlvo() : TipoAlvo.INDIVIDUAL;
		int anguloConeBase = (arma != null) ? arma.getAnguloCone() : 0;

		if (this.isModoCoronhadaSelecionado && arma != null) {
			alcanceBase = arma.getAlcanceAtaqueAlternativoBasico();
			tipoAlvoBase = arma.getTipoAlvoAtaqueAlternativoBasico();
			anguloConeBase = arma.getAnguloAtaqueAlternativoBasico();
		} else if (toggleForte.isSelected()) {
			alcanceBase += 1;
		}

		final int alcanceFinal = alcanceBase;
		final TipoAlvo tipoAlvoFinal = tipoAlvoBase;
		final int anguloConeFinal = anguloConeBase;
		int areaFinal = (arma != null) ? arma.getTamanhoArea() : 0;
		if (this.isModoCoronhadaSelecionado && arma != null) {
			areaFinal = arma.getTamanhoAreaAtaqueAlternativoBasico();
		}

		return new Habilidade("Ataque Basico", "", TipoHabilidade.ATIVA, 0, 0, 0, tipoAlvoFinal,
				areaFinal, 0, 0, null) {
			@Override public int getAlcanceMaximo() { return alcanceFinal; }
			@Override public int getAnguloCone() { return anguloConeFinal; }
			@Override
			public List<Integer> getAngulosDesvio() {
				if (arma != null && "cadete Estelar".equalsIgnoreCase(arma.getNome())) {
					return java.util.Arrays.asList(-30, 0, 30);
				}
				return super.getAngulosDesvio();
			}
			@Override public void executar(Personagem c, List<Personagem> a, EstadoCombate es, CombatManager m) {}
		};
	}

	@FXML
	private void onSelecionarAlvoClick() {
		if (mainController == null) return;

		Habilidade habilidadeParaExecutar = habilidadeSelecionada;
		Habilidade habilidadeParaSelecionar = null;

		if (isAtaqueBasico) {
			habilidadeParaExecutar = null;
			habilidadeParaSelecionar = criarHabilidadeSelecaoAtaqueBasico();
		} else if (habilidadeSelecionada != null) {
			habilidadeParaSelecionar = habilidadeSelecionada;
		} else if (fantasmaNobreSelecionado != null) {
			FantasmaNobre fnRef = fantasmaNobreSelecionado;
			Habilidade dummyFN = new Habilidade(fnRef.getNome(), "", TipoHabilidade.ATIVA, 0, 0, 0,
					fnRef.getTipoAlvo(), fnRef.getTamanhoArea(), 0, 0, null) {
				@Override public int getAlcanceMaximo() { return 99; }
				@Override public void executar(Personagem c, List<Personagem> a, EstadoCombate es, CombatManager m) {}
			};
			habilidadeParaSelecionar = dummyFN;
		}

		if (atorAtual != null && atorAtual.isClone() && habilidadeParaSelecionar != null) {
			mainController.iniciarAtaqueSquad(habilidadeParaExecutar, habilidadeParaSelecionar,
					obterRolagemAtributoAtual(), obterModoAtaqueAtual(), obterTirosExtrasAtuais());
		} else if (habilidadeParaSelecionar != null) {
			mainController.iniciarSelecaoDeAlvo(habilidadeParaSelecionar, atorAtual);
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
		if (alvo == null) return;
		this.alvosNoMapa.clear();
		this.alvosNoMapa.add(alvo);
		btnSelecionarAlvo.setText("Alvo: " + alvo.getNome());
		btnSelecionarAlvo.setStyle("-fx-background-color: #004400; -fx-text-fill: white;");
		btnConfirmarAcao.setDisable(false);
		atualizarEstimativaDano();
	}

	public void limparAlvosHover() {
		this.alvosNoMapa.clear();
		btnSelecionarAlvo.setText("Selecionar Alvo (Mapa)");
		btnSelecionarAlvo.setStyle("");
		if (verificaSePrecisaAlvo()) btnConfirmarAcao.setDisable(true);
	}

	public void configurarConfirmacaoSquad(int qtdAtaques) {
		actionDetailsColumn.setVisible(true);
		actionDetailsColumn.setManaged(true);
		lblActionTitle.setText("Ataque Coordenado");
		lblActionDesc.setText(qtdAtaques + " clones posicionados e mirando.");
		attackOptionsBox.setVisible(false);
		attackOptionsBox.setManaged(false);
		diceInputsBox.getChildren().clear();
		btnConfirmarAcao.setText("EXECUTAR SQUAD");
		btnConfirmarAcao.setStyle("-fx-base: #AA0000; -fx-font-weight: bold;");
		btnConfirmarAcao.setDisable(false);
		btnConfirmarAcao.setOnAction(e -> {
			mainController.executarAtaqueSquadFinal();
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
			preencherAtributosDetalhados();
		} else {
			btnDetalhes.setText("Ver Atributos Detalhados");
		}
	}

	// --- DELEGAÇÃO AO CharacterInfoRenderer ---

	private void preencherAtributosDetalhados() {
		characterInfoRenderer.renderDetailedAttributes(atorAtual);
	}

	private void atualizarInfoPersonagem() {
		characterInfoRenderer.renderInfo(atorAtual);
		renderizarMunicaoEquipadas();
	}

	private void updateEffects() {
		characterInfoRenderer.renderEffects(atorAtual);
	}

	private void renderizarMunicaoEquipadas() {
		municaoContainer.getChildren().clear();
		if (atorAtual == null) {
			municaoContainer.setVisible(false);
			municaoContainer.setManaged(false);
			return;
		}

		List<Arma> armasComMunicao = atorAtual.getArmasEquipadas().stream()
				.filter(Arma::isRequerMunicao)
				.toList();
		boolean mostrar = !armasComMunicao.isEmpty();
		municaoContainer.setVisible(mostrar);
		municaoContainer.setManaged(mostrar);
		if (!mostrar) {
			return;
		}

		Label titulo = new Label("Munição");
		titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
		municaoContainer.getChildren().add(titulo);

		for (Arma arma : armasComMunicao) {
			Label nomeArma = new Label(arma.getNome());
			nomeArma.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

			ProgressBar barra = new ProgressBar(calcularProgressoMunicao(arma));
			barra.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(barra, Priority.ALWAYS);

			Label contador = new Label("[" + arma.getMunicaoAtual() + " / " + arma.getMunicaoMaxima() + "]");
			contador.setMinWidth(58);
			contador.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-family: monospace;");

			HBox linha = new HBox(8, barra, contador);
			linha.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
			VBox bloco = new VBox(3, nomeArma, linha);
			municaoContainer.getChildren().add(bloco);
		}
	}

	private double calcularProgressoMunicao(Arma arma) {
		if (arma.getMunicaoMaxima() <= 0) {
			return 0;
		}
		return Math.max(0.0, Math.min(1.0, (double) arma.getMunicaoAtual() / arma.getMunicaoMaxima()));
	}

	// --- UTILITÁRIO ---

	private String formatarNumero(double valor) {
		if (valor >= 1000) return String.format("%.1fk", valor / 1000.0).replace(",", ".");
		return String.format("%.0f", valor);
	}
}
