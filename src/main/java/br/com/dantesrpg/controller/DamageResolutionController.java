package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.habilidades.classe.BashStrike;
import br.com.dantesrpg.model.util.DamageEvent;
import br.com.dantesrpg.model.enums.TipoAcao;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DamageResolutionController {

	@FXML
	private ScrollPane mainScrollPane;
	@FXML
	private Label lblTituloHabilidade;
	@FXML
	private Button btnConfirmar;
	@FXML
	private CheckBox cbContraAtaqueUnico;

	private GridPane damageGrid;
	private Personagem atacante;
	private Habilidade habilidade;
	private EstadoCombate estado;
	private List<DamageTargetRow> linhasDeDano = new ArrayList<>();

	// --- CONTRA-ATAQUE ---
	// Ordem de seleção dos alvos que contra-atacarão (o 1º selecionado age primeiro).
	private List<Personagem> filaContraAtaque = new ArrayList<>();
	// Referência aos checkboxes por alvo (usado para atualizar a numeração #N na UI).
	private Map<Personagem, CheckBox> checkBoxesContraAtaque = new LinkedHashMap<>();
	// True quando há mais de um alvo na resolução — define onde o checkbox aparece.
	private boolean multiAlvo = false;
	// Guarda o único alvo para o caso de alvo único (checkbox ao lado do botão).
	private Personagem alvoUnico;

	// REFERÊNCIA NECESSÁRIA PARA APLICAR DANO CORRETAMENTE
	private CombatController mainController;

	@FXML
	public void initialize() {
		damageGrid = new GridPane();
		damageGrid.setPadding(new Insets(10));
		damageGrid.setHgap(10);
		damageGrid.setVgap(8);
		damageGrid.setStyle("-fx-background-color: #2b2b2b;");
		damageGrid.setAlignment(Pos.TOP_CENTER);

		// Configura o ScrollPane para rolar suavemente
		mainScrollPane.setContent(damageGrid);
		mainScrollPane.setFitToWidth(true); // Estica horizontalmente
		mainScrollPane.setPannable(true);
	}

	public void setMainController(CombatController controller) {
		this.mainController = controller;
	}

	public void setupResolution(Personagem atacante, Habilidade habilidade,
			Map<Personagem, List<DamageEvent>> mapaDanos, EstadoCombate estadoCombate) {
		this.atacante = atacante;
		this.habilidade = habilidade;
		this.estado = estadoCombate;

		lblTituloHabilidade.setText("Resolução: " + (habilidade != null ? habilidade.getNome() : "Ataque Básico"));
		damageGrid.getChildren().clear();
		linhasDeDano.clear();
		filaContraAtaque.clear();
		checkBoxesContraAtaque.clear();
		this.alvoUnico = null;

		this.multiAlvo = mapaDanos.size() > 1;

		// Configura o checkbox único (ao lado do botão Confirmar) apenas quando houver 1 alvo.
		if (cbContraAtaqueUnico != null) {
			cbContraAtaqueUnico.setSelected(false);
			cbContraAtaqueUnico.setVisible(!multiAlvo);
			cbContraAtaqueUnico.setManaged(!multiAlvo);
		}

		// --- CABEÇALHO ---
		adicionarCabecalho("Alvo", 0);
		adicionarCabecalho("Ticks", 1);
		adicionarCabecalho("Reação", 2);
		adicionarCabecalho("Dado", 3);
		adicionarCabecalho("Ticks afetados", 4);
		adicionarCabecalho("Resumo", 5);

		// --- LINHAS (ALVOS) ---
		int row = 1;
		for (Map.Entry<Personagem, List<DamageEvent>> entry : mapaDanos.entrySet()) {
			Personagem alvo = entry.getKey();
			List<DamageEvent> eventos = entry.getValue();

			// Multi-alvo: cada linha ganha seu próprio checkbox logo abaixo do nome.
			// Alvo único: só guardamos a referência do alvo (o checkbox está ao lado do botão).
			if (multiAlvo) {
				CheckBox cbContra = new CheckBox("Contra-Ataque");
				cbContra.setStyle("-fx-text-fill: #ffcc00; -fx-font-size: 11px;");
				final Personagem alvoFinal = alvo;
				cbContra.selectedProperty().addListener((obs, oldV, newV) -> {
					if (newV) {
						if (!filaContraAtaque.contains(alvoFinal)) {
							filaContraAtaque.add(alvoFinal);
						}
					} else {
						filaContraAtaque.remove(alvoFinal);
					}
					atualizarLabelsFilaContraAtaque();
				});
				checkBoxesContraAtaque.put(alvo, cbContra);

				// A linha compacta abaixo mantém o checkbox junto do nome do alvo.
				DamageTargetRow linha = new DamageTargetRow(alvo, eventos, cbContra);
				adicionarLinha(linha, row);
			} else {
				this.alvoUnico = alvo;
				DamageTargetRow linha = new DamageTargetRow(alvo, eventos, null);
				adicionarLinha(linha, row);
			}
			row++;
		}
	}

	private void adicionarCabecalho(String texto, int coluna) {
		Label label = new Label(texto);
		label.setTextFill(Color.CYAN);
		label.setFont(Font.font("System", FontWeight.BOLD, 12));
		damageGrid.add(label, coluna, 0);
	}

	private void adicionarLinha(DamageTargetRow linha, int row) {
		linhasDeDano.add(linha);
		damageGrid.add(linha.getAlvoNode(), 0, row);
		damageGrid.add(linha.getQuantidadeTicksNode(), 1, row);
		damageGrid.add(linha.getReacaoNode(), 2, row);
		damageGrid.add(linha.getDadoNode(), 3, row);
		damageGrid.add(linha.getSliderNode(), 4, row);
		damageGrid.add(linha.getResumoNode(), 5, row);
	}

	/**
	 * Atualiza o rótulo de cada checkbox de contra-ataque (multi-alvo) com a
	 * posição ordinal na fila — ex.: "Contra-Ataque #2". Facilita ver ao GM
	 * a ordem exata em que os contra-ataques ocorrerão.
	 */
	private void atualizarLabelsFilaContraAtaque() {
		for (Map.Entry<Personagem, CheckBox> e : checkBoxesContraAtaque.entrySet()) {
			Personagem p = e.getKey();
			CheckBox cb = e.getValue();
			int idx = filaContraAtaque.indexOf(p);
			if (idx >= 0) {
				cb.setText("Contra-Ataque #" + (idx + 1));
			} else {
				cb.setText("Contra-Ataque");
			}
		}
	}

	@FXML
	private void onConfirmarAction() {
		// Confirma consumo de munição que foi adiado até este momento
		if (mainController != null) {
			mainController.getCombatManager().confirmarMunicaoPendente();
		}

		// Aplica os danos
		double danoTotalCausado = 0;
		for (DamageTargetRow linha : linhasDeDano) {
			danoTotalCausado += linha.aplicarDanos();
		}

		// Hook: Bash Strike — retorno de dano ao atacante
		if (habilidade instanceof BashStrike && atacante != null && danoTotalCausado > 0 && mainController != null) {
			BashStrike.aplicarRetornoDeDano(atacante, danoTotalCausado, estado,
					mainController.getCombatManager());
		}

		// --- CONTRA-ATAQUE ---
		// Consolida a fila: alvo único usa o checkbox único; multi-alvo usa a ordem de seleção.
		List<Personagem> filaFinal = new ArrayList<>();
		if (!multiAlvo) {
			if (cbContraAtaqueUnico != null && cbContraAtaqueUnico.isSelected() && alvoUnico != null) {
				filaFinal.add(alvoUnico);
			}
		} else {
			filaFinal.addAll(filaContraAtaque);
		}

		if (!filaFinal.isEmpty() && mainController != null && estado != null) {
			mainController.getCombatManager().processarContraAtaques(filaFinal, estado);
		}

		// Força atualização visual geral após fechar a janela
		if (mainController != null) {
			mainController.atualizarInterfaceTotal();
		}

		btnConfirmar.getScene().getWindow().hide();
	}

	// --- LINHA COMPACTA POR ALVO ---
	private class DamageTargetRow {
		private final Personagem alvo;
		private final List<DamageEvent> eventos;
		private final ComboBox<String> cmbReacao = new ComboBox<>();
		private final TextField txtInputDado = new TextField();
		private final Slider sliderTicksAfetados = new Slider();
		private final Label lblQuantidadeTicks = new Label();
		private final Label lblQuantidadeTotal = new Label();
		private final Label lblResumo = new Label();
		private final Label lblInfoReacao = new Label();
		private final VBox alvoNode = new VBox(2);
		private final VBox sliderNode = new VBox(2);

		DamageTargetRow(Personagem alvo, List<DamageEvent> eventos, CheckBox checkboxContraAtaque) {
			this.alvo = alvo;
			this.eventos = eventos != null ? eventos : List.of();
			configurarInterface(checkboxContraAtaque);
		}

		private void configurarInterface(CheckBox checkboxContraAtaque) {
			Label lblNome = new Label(alvo.getNome());
			lblNome.setTextFill(Color.WHITE);
			lblNome.setFont(Font.font("System", FontWeight.BOLD, 13));
			alvoNode.setAlignment(Pos.CENTER_LEFT);
			alvoNode.getChildren().add(lblNome);
			if (checkboxContraAtaque != null) {
				alvoNode.getChildren().add(checkboxContraAtaque);
			}

			lblQuantidadeTotal.setText(eventos.size() + " ataques");
			lblQuantidadeTotal.setTextFill(Color.LIGHTGRAY);
			lblQuantidadeTotal.setFont(Font.font("System", 11));

			cmbReacao.getItems().addAll("Nada", "Esquiva", "Bloqueio");
			cmbReacao.setValue("Nada");
			cmbReacao.setPrefWidth(110);

			txtInputDado.setPromptText("Dado");
			txtInputDado.setPrefWidth(70);
			txtInputDado.setVisible(false);
			txtInputDado.setManaged(false);

			sliderTicksAfetados.setMin(0);
			sliderTicksAfetados.setMax(eventos.size());
			sliderTicksAfetados.setValue(eventos.size());
			sliderTicksAfetados.setMajorTickUnit(Math.max(1, eventos.size()));
			sliderTicksAfetados.setMinorTickCount(Math.max(0, Math.min(4, eventos.size() - 1)));
			sliderTicksAfetados.setSnapToTicks(true);
			sliderTicksAfetados.setShowTickMarks(false);
			sliderTicksAfetados.setPrefWidth(165);
			sliderTicksAfetados.valueProperty().addListener((obs, antigo, novo) -> atualizarResumo());
			lblQuantidadeTicks.setText(eventos.size() + "/" + eventos.size());
			lblQuantidadeTicks.setTextFill(Color.CYAN);
			lblQuantidadeTicks.setFont(Font.font("System", FontWeight.BOLD, 11));
			sliderNode.setAlignment(Pos.CENTER_LEFT);
			sliderNode.getChildren().addAll(sliderTicksAfetados, lblQuantidadeTicks);

			lblInfoReacao.setTextFill(Color.YELLOW);
			lblInfoReacao.setFont(Font.font("System", 10));
			lblResumo.setTextFill(Color.WHITE);
			lblResumo.setFont(Font.font("System", 11));
			cmbReacao.setOnAction(e -> atualizarEstadoInterface());
			txtInputDado.textProperty().addListener((obs, antigo, novo) -> atualizarResumo());
			atualizarResumo();
		}

		Node getAlvoNode() {
			return alvoNode;
		}

		Node getQuantidadeTicksNode() {
			return lblQuantidadeTotal;
		}

		Node getReacaoNode() {
			return cmbReacao;
		}

		Node getDadoNode() {
			return txtInputDado;
		}

		Node getSliderNode() {
			return sliderNode;
		}

		Node getResumoNode() {
			VBox resumo = new VBox(2, lblInfoReacao, lblResumo);
			resumo.setPrefWidth(205);
			return resumo;
		}

		private void atualizarEstadoInterface() {
			String selecao = cmbReacao.getValue();
			txtInputDado.clear();
			txtInputDado.setVisible(!"Nada".equals(selecao));
			txtInputDado.setManaged(!"Nada".equals(selecao));

			if ("Esquiva".equals(selecao)) {
				int grau = atacante != null ? atacante.getGrau() : 0;
				lblInfoReacao.setText("Dif: " + (2 + grau * 2) + " - afeta os primeiros ticks");
			} else if ("Bloqueio".equals(selecao)) {
				lblInfoReacao.setText("Dado x 4,5% - afeta os primeiros ticks");
			} else {
				lblInfoReacao.setText("");
			}
			atualizarResumo();
		}

		private void atualizarResumo() {
			int quantidadeAfetada = obterQuantidadeTicksAfetados();
			lblQuantidadeTicks.setText(quantidadeAfetada + "/" + eventos.size());
			double danoPrevisto = calcularDanoPrevisto(quantidadeAfetada);
			lblResumo.setText("Dano previsto: " + Math.round(danoPrevisto));
			lblResumo.setTextFill("Nada".equals(cmbReacao.getValue()) ? Color.WHITE : Color.ORANGE);
		}

		private int obterQuantidadeTicksAfetados() {
			return (int) Math.round(sliderTicksAfetados.getValue());
		}

		private int obterValorDado() {
			String texto = txtInputDado.getText();
			if (texto == null || !texto.matches("\\d+")) {
				return 0;
			}
			return Integer.parseInt(texto);
		}

		private double obterBonusMantoDivino() {
			if (alvo == null || !alvo.getEfeitosAtivos().containsKey("Manto Divino")) {
				return 0;
			}
			br.com.dantesrpg.model.Efeito manto = alvo.getEfeitosAtivos().get("Manto Divino");
			return manto.getModificadores() != null
					? manto.getModificadores().getOrDefault("BONUS_BLOQUEIO", 2.0)
					: 2.0;
		}

		private double calcularDanoPrevisto(int quantidadeAfetada) {
			String selecao = cmbReacao.getValue();
			int valorDado = obterValorDado();
			int dificuldade = 2 + ((atacante != null ? atacante.getGrau() : 0) * 2);
			double danoTotal = 0;
			for (int i = 0; i < eventos.size(); i++) {
				double dano = eventos.get(i).getValorDano();
				if (i < quantidadeAfetada && "Esquiva".equals(selecao) && valorDado >= dificuldade) {
					dano = 0;
				} else if (i < quantidadeAfetada && "Bloqueio".equals(selecao)) {
					double bonus = i == 0 ? obterBonusMantoDivino() : 0;
					dano *= 1.0 - Math.min(1.0, (valorDado + bonus) * 0.045);
				}
				danoTotal += dano;
			}
			return danoTotal;
		}

		double aplicarDanos() {
			int quantidadeAfetada = obterQuantidadeTicksAfetados();
			double danoTotal = 0;
			for (int i = 0; i < eventos.size(); i++) {
				danoTotal += aplicarDanoFinal(eventos.get(i), i < quantidadeAfetada);
			}
			return danoTotal;
		}

		private double aplicarDanoFinal(DamageEvent evento, boolean aplicarReacao) {
			double danoFinal = evento.getValorDano();
			String selecao = cmbReacao.getValue();
			int valorInput = aplicarReacao && !"Nada".equals(selecao) ? obterValorDado() : 0;

			if (aplicarReacao && "Esquiva".equals(selecao)) {
				int grau = atacante != null ? atacante.getGrau() : 0;
				if (valorInput >= 2 + grau * 2) {
					danoFinal = 0;
					System.out.println(">>> " + alvo.getNome() + " ESQUIVOU do tick " + evento.getLabel() + "!");
				}
			} else if (aplicarReacao && "Bloqueio".equals(selecao)) {
				double bonus = obterBonusMantoDivino();
				if (bonus > 0) {
					alvo.removerEfeito("Manto Divino");
					alvo.recalcularAtributosEstatisticas();
				}
				double percentual = Math.min(1.0, (valorInput + bonus) * 0.045);
				danoFinal *= 1.0 - percentual;
				System.out.println(">>> " + alvo.getNome() + " BLOQUEOU " + evento.getLabel() + ". Dano: " + danoFinal);
			}

			if (mainController != null) {
				if (danoFinal > 0) {
					TipoAcao tipo = habilidade != null ? TipoAcao.HABILIDADE : TipoAcao.ATAQUE_BASICO;
					if (evento.getLabel().contains("Eco")) {
						tipo = TipoAcao.ECO;
					}
					mainController.getCombatManager().aplicarDanoAoAlvoResolvido(atacante, alvo, danoFinal, false,
							tipo, estado, 0);
					evento.aplicarEfeitos(danoFinal);
				} else {
					System.out.println(">>> Dano anulado (Esquiva/Bloqueio total). Efeitos on-hit cancelados.");
				}
			}
			return danoFinal;
		}
	}
}
