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
	private List<DamageCell> celulasDeDano = new ArrayList<>();

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
		damageGrid.setHgap(15);
		damageGrid.setVgap(15);
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
		celulasDeDano.clear();
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

		int maxColunas = 0;
		for (List<DamageEvent> lista : mapaDanos.values()) {
			if (lista.size() > maxColunas)
				maxColunas = lista.size();
		}

		// --- CABEÇALHO ---
		Label lblCorner = new Label("Alvo");
		lblCorner.setTextFill(Color.LIGHTGRAY);
		damageGrid.add(lblCorner, 0, 0);

		for (int i = 0; i < maxColunas; i++) {
			Label lblTick = new Label("Evento " + (i + 1));
			lblTick.setTextFill(Color.CYAN);
			lblTick.setFont(Font.font("System", FontWeight.BOLD, 14));
			lblTick.setAlignment(Pos.CENTER);
			damageGrid.add(lblTick, i + 1, 0);
		}

		// --- LINHAS (ALVOS) ---
		int row = 1;
		for (Map.Entry<Personagem, List<DamageEvent>> entry : mapaDanos.entrySet()) {
			Personagem alvo = entry.getKey();
			List<DamageEvent> eventos = entry.getValue();

			Label lblNome = new Label(alvo.getNome());
			lblNome.setTextFill(Color.WHITE);
			lblNome.setFont(Font.font("System", FontWeight.BOLD, 14));

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

				VBox cabecalhoAlvo = new VBox(3);
				cabecalhoAlvo.setAlignment(Pos.CENTER_LEFT);
				cabecalhoAlvo.getChildren().addAll(lblNome, cbContra);
				damageGrid.add(cabecalhoAlvo, 0, row);
			} else {
				this.alvoUnico = alvo;
				damageGrid.add(lblNome, 0, row);
			}

			for (int col = 0; col < eventos.size(); col++) {
				DamageEvent evento = eventos.get(col);
				DamageCell cell = new DamageCell(alvo, evento, col);
				damageGrid.add(cell.getNode(), col + 1, row);
				celulasDeDano.add(cell);
			}
			row++;
		}
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
		for (DamageCell cell : celulasDeDano) {
			danoTotalCausado += cell.aplicarDanoFinal();
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

	// --- CÉLULA ---
	private class DamageCell {
		private VBox container;
		private Label lblTituloTick;
		private Label lblDanoOriginal;
		private ComboBox<String> cmbReacao;
		private TextField txtInputDado;
		private Label lblResultado;
		private Label lblInfoExtra;

		private Personagem alvo;
		private DamageEvent evento;

		public DamageCell(Personagem alvo, DamageEvent evento, int tickIndex) {
			this.alvo = alvo;
			this.evento = evento;
			createUI();
		}

		private void createUI() {
			container = new VBox(5);
			container.setPadding(new Insets(8));
			String bordaColor = evento.isCritico() ? "red" : "#555";
			String borderStyle = evento.isCritico() ? "-fx-border-width: 2;" : "";
			container.setStyle("-fx-border-color: " + bordaColor + "; " + borderStyle
					+ " -fx-border-radius: 5; -fx-background-color: #333; -fx-background-radius: 5;");
			container.setPrefWidth(180);
			container.setAlignment(Pos.CENTER_LEFT);

			lblTituloTick = new Label(evento.getLabel());
			lblTituloTick.setStyle("-fx-font-size: 10px; -fx-text-fill: lightgrey;");

			lblDanoOriginal = new Label("Dano: " + Math.round(evento.getValorDano()));
			lblDanoOriginal.setTextFill(Color.web("#ff6666"));
			lblDanoOriginal.setFont(Font.font("System", FontWeight.BOLD, 12));

			cmbReacao = new ComboBox<>();
			cmbReacao.getItems().addAll("Nada", "Esquiva", "Bloqueio");
			cmbReacao.setValue("Nada");
			cmbReacao.setPrefWidth(160);

			txtInputDado = new TextField();
			txtInputDado.setPromptText("Valor do Dado");
			txtInputDado.setVisible(false);
			txtInputDado.setManaged(false);

			lblInfoExtra = new Label("");
			lblInfoExtra.setTextFill(Color.YELLOW);
			lblInfoExtra.setFont(Font.font("System", 10));

			lblResultado = new Label("Final: " + Math.round(evento.getValorDano()));
			lblResultado.setTextFill(Color.WHITE);

			cmbReacao.setOnAction(e -> atualizarEstadoInterface());
			txtInputDado.textProperty().addListener((obs, oldVal, newVal) -> calcularResultadoTempoReal(newVal));

			container.getChildren().addAll(lblTituloTick, lblDanoOriginal, cmbReacao, txtInputDado, lblInfoExtra,
					lblResultado);
		}

		public Node getNode() {
			return container;
		}

		private void atualizarEstadoInterface() {
			String selecao = cmbReacao.getValue();
			txtInputDado.clear();
			lblInfoExtra.setText("");

			if (selecao.equals("Nada")) {
				txtInputDado.setVisible(false);
				txtInputDado.setManaged(false);
				lblResultado.setText("Final: " + Math.round(evento.getValorDano()));
				lblResultado.setTextFill(Color.WHITE);
			} else {
				txtInputDado.setVisible(true);
				txtInputDado.setManaged(true);

				if (selecao.equals("Esquiva")) {
					int grau = (atacante != null) ? atacante.getGrau() : 0;
					int dificuldade = 2 + (grau * 2);
					lblInfoExtra.setText("Dif: " + dificuldade + " (Grau " + grau + ")");
				} else if (selecao.equals("Bloqueio")) {
					double bonus = 0;
					if (alvo != null && alvo.getEfeitosAtivos().containsKey("Manto Divino")) {
						br.com.dantesrpg.model.Efeito manto = alvo.getEfeitosAtivos().get("Manto Divino");
						if (manto.getModificadores() != null) {
							bonus = manto.getModificadores().getOrDefault("BONUS_BLOQUEIO", 2.0);
						} else {
							bonus = 2.0;
						}
					}
					if (bonus > 0) {
						lblInfoExtra.setText(String.format("Dado + %.0f (Manto Divino) x 4.5%% = Redução", bonus));
					} else {
						lblInfoExtra.setText("Dado x 4.5% = Redução");
					}
				}
			}
		}

		private void calcularResultadoTempoReal(String input) {
			if (input == null || input.isEmpty() || !input.matches("\\d+")) {
				lblResultado.setText("...");
				return;
			}
			int valorInput = Integer.parseInt(input);
			String selecao = cmbReacao.getValue();

			if (selecao.equals("Esquiva")) {
				int grau = (atacante != null) ? atacante.getGrau() : 0;
				int dificuldade = 2 + (grau * 2);

				if (valorInput >= dificuldade) {
					lblResultado.setText("SUCESSO (0 Dano)");
					lblResultado.setTextFill(Color.GREEN);
				} else {
					lblResultado.setText("FALHA (" + Math.round(evento.getValorDano()) + ")");
					lblResultado.setTextFill(Color.RED);
				}

			} else if (selecao.equals("Bloqueio")) {
				double bonus = 0;
				if (alvo != null && alvo.getEfeitosAtivos().containsKey("Manto Divino")) {
					br.com.dantesrpg.model.Efeito manto = alvo.getEfeitosAtivos().get("Manto Divino");
					if (manto.getModificadores() != null) {
						bonus = manto.getModificadores().getOrDefault("BONUS_BLOQUEIO", 2.0);
					} else {
						bonus = 2.0;
					}
				}
				double valorFinal = valorInput + bonus;
				double percentualReducao = valorFinal * 0.045;
				if (percentualReducao > 1.0)
					percentualReducao = 1.0;
				double danoReduzido = evento.getValorDano() * (1.0 - percentualReducao);
				String pctTexto = String.format("%.1f%%", percentualReducao * 100);
				if (bonus > 0) {
					lblResultado.setText(String.format("Reduz %s (Dado+%.0f) -> %d", pctTexto, bonus, Math.round(danoReduzido)));
				} else {
					lblResultado.setText("Reduz " + pctTexto + " -> " + Math.round(danoReduzido));
				}
				lblResultado.setTextFill(Color.ORANGE);
			}
		}

		public double aplicarDanoFinal() {
			double danoFinal = evento.getValorDano();
			String selecao = cmbReacao.getValue();

			try {
				int valorInput = 0;
				if (txtInputDado.isVisible() && !txtInputDado.getText().isEmpty()) {
					valorInput = Integer.parseInt(txtInputDado.getText());
				}

				if (selecao.equals("Esquiva")) {
					int grau = (atacante != null) ? atacante.getGrau() : 0;
					if (valorInput >= (2 + (grau * 2))) {
						danoFinal = 0;
						System.out.println(">>> " + alvo.getNome() + " ESQUIVOU!");
					}
				} else if (selecao.equals("Bloqueio")) {
					double bonus = 0;
					if (alvo != null && alvo.getEfeitosAtivos().containsKey("Manto Divino")) {
						br.com.dantesrpg.model.Efeito manto = alvo.getEfeitosAtivos().get("Manto Divino");
						if (manto.getModificadores() != null) {
							bonus = manto.getModificadores().getOrDefault("BONUS_BLOQUEIO", 2.0);
						} else {
							bonus = 2.0;
						}
						alvo.removerEfeito("Manto Divino");
						alvo.recalcularAtributosEstatisticas();
					}
					double percentual = Math.min(1.0, (valorInput + bonus) * 0.045);
					danoFinal = danoFinal * (1.0 - percentual);
					System.out.println(">>> " + alvo.getNome() + " BLOQUEOU (Manto Divino Bônus: +" + bonus + "). Dano: " + danoFinal);
				}
			} catch (Exception e) {
			}

			// SE HOUVER DANO, CHAMAMOS O COMBAT MANAGER PARA APLICAR (COM LOGICA DE MORTE, ETC)
			if (mainController != null) {
				// Aplica o dano no HP (Só se for > 0 para não poluir log)
				if (danoFinal > 0) {
					TipoAcao tipo = (habilidade != null) ? TipoAcao.HABILIDADE : TipoAcao.ATAQUE_BASICO;
					if (evento.getLabel().contains("Eco"))
						tipo = TipoAcao.ECO;

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
