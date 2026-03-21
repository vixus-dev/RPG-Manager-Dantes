package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
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
import java.util.List;
import java.util.Map;

public class DamageResolutionController {

	@FXML
	private ScrollPane mainScrollPane;
	@FXML
	private Label lblTituloHabilidade;
	@FXML
	private Button btnConfirmar;

	private GridPane damageGrid;
	private Personagem atacante;
	private Habilidade habilidade;
	private EstadoCombate estado;
	private List<DamageCell> celulasDeDano = new ArrayList<>();

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
			damageGrid.add(lblNome, 0, row);

			for (int col = 0; col < eventos.size(); col++) {
				DamageEvent evento = eventos.get(col);
				DamageCell cell = new DamageCell(alvo, evento, col);
				damageGrid.add(cell.getNode(), col + 1, row);
				celulasDeDano.add(cell);
			}
			row++;
		}
	}

	@FXML
	private void onConfirmarAction() {
		// Aplica os danos
		for (DamageCell cell : celulasDeDano) {
			cell.aplicarDanoFinal();
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
					lblInfoExtra.setText("Dado x 4.5% = Redução");
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
				double percentualReducao = valorInput * 0.045;
				if (percentualReducao > 1.0)
					percentualReducao = 1.0;
				double danoReduzido = evento.getValorDano() * (1.0 - percentualReducao);
				String pctTexto = String.format("%.1f%%", percentualReducao * 100);
				lblResultado.setText("Reduz " + pctTexto + " -> " + Math.round(danoReduzido));
				lblResultado.setTextFill(Color.ORANGE);
			}
		}

		public void aplicarDanoFinal() {
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
					double percentual = Math.min(1.0, valorInput * 0.045);
					danoFinal = danoFinal * (1.0 - percentual);
					System.out.println(">>> " + alvo.getNome() + " BLOQUEOU. Dano: " + danoFinal);
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
				} else {
					System.out.println(">>> Dano anulado (Esquiva/Bloqueio total). Efeitos on-hit cancelados.");
				}
				evento.aplicarEfeitos(danoFinal);
			}
		}
	}
}
