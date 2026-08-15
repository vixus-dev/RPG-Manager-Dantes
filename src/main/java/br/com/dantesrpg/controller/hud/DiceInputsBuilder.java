package br.com.dantesrpg.controller.hud;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.util.DiceRoller;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Constrói dinamicamente os inputs de dados na coluna 3 do HUD de turno.
 * Extraído de DetailedTurnHUDController.gerarInputsDeDados().
 */
public class DiceInputsBuilder {

	// ========== RESULTADO ==========

	public static class DiceInputsResult {
		public final TextField inputDadoAtributo;          // null se ação não usa dado de atributo
		public final Map<String, TextField> inputsExtras;
		public final int tipoDado;
		public final ToggleGroup toggleGroupOpcoes;        // null se ação não tem opções

		public DiceInputsResult(TextField inputDadoAtributo, Map<String, TextField> inputsExtras,
				int tipoDado, ToggleGroup toggleGroupOpcoes) {
			this.inputDadoAtributo = inputDadoAtributo;
			this.inputsExtras = inputsExtras;
			this.tipoDado = tipoDado;
			this.toggleGroupOpcoes = toggleGroupOpcoes;
		}
	}

	// ========== DEPENDÊNCIAS ==========

	private final VBox diceInputsBox;
	private final VBox diceRollColumn;
	private final Label lblDiceType;
	private final Label lblDiceResult;
	private final Label lblCritRate;
	private final Label lblCritResult;

	public DiceInputsBuilder(VBox diceInputsBox, VBox diceRollColumn,
			Label lblDiceType, Label lblDiceResult,
			Label lblCritRate, Label lblCritResult) {
		this.diceInputsBox = diceInputsBox;
		this.diceRollColumn = diceRollColumn;
		this.lblDiceType = lblDiceType;
		this.lblDiceResult = lblDiceResult;
		this.lblCritRate = lblCritRate;
		this.lblCritResult = lblCritResult;
	}

	// ========== BUILD ==========

	/**
	 * Popula o diceInputsBox com os inputs apropriados para a ação selecionada.
	 *
	 * @param ator             Personagem ativo no turno
	 * @param hab              Habilidade selecionada (pode ser null)
	 * @param fn               Fantasma Nobre selecionado (pode ser null)
	 * @param isBasicAttack    True se for ataque básico
	 * @param onEstimateChange Callback disparado quando qualquer input muda (atualiza estimativa)
	 * @param onOpcaoChanged   Callback quando uma opção de toggle é selecionada (null = ignorar)
	 * @return DiceInputsResult com referências para os campos criados
	 */
	public DiceInputsResult build(Personagem ator, Habilidade hab, FantasmaNobre fn,
			boolean isBasicAttack, Runnable onEstimateChange, Consumer<String> onOpcaoChanged) {

		diceInputsBox.getChildren().clear();
		diceRollColumn.getChildren().removeIf(node -> "painelJusticaDourada".equals(node.getId()));
		diceRollColumn.setVisible(false);
		diceRollColumn.setManaged(false);

		Map<String, TextField> inputsExtras = new HashMap<>();
		TextField inputDadoAtributo = null;
		int tipoDado = 20;
		ToggleGroup toggleGroupOpcoes = null;

		// --- 1. Input principal: rolagem de atributo (d20/dX) ---
		if (precisaDadoAtributo(isBasicAttack, hab)) {
			Atributo atr = resolverAtributo(ator, hab);
			int valorAtr = ator.getAtributosFinais().getOrDefault(atr, 1);
			tipoDado = DiceRoller.getTipoDado(valorAtr);

			Label lbl = new Label("Rolagem " + atr.name() + " (d" + tipoDado + "):");
			lbl.getStyleClass().add("hud-field-label");

			inputDadoAtributo = new TextField();
			inputDadoAtributo.setPromptText("Resultado do d" + tipoDado);
			inputDadoAtributo.getStyleClass().add("hud-dice-input");

			Label lblFinalResult = new Label("");
			lblFinalResult.getStyleClass().add("hud-dice-final");

			inputDadoAtributo.textProperty().addListener((o, ov, nv) -> {
				if (nv == null || nv.isEmpty()) {
					lblFinalResult.setText("");
				} else {
					try {
						int rolagemBruta = Integer.parseInt(nv);
						int valorSorte = ator.getAtributosFinais().getOrDefault(Atributo.SORTE, 1);
						int rolagemFinal = DiceRoller.aplicarBonusRankESorte(rolagemBruta, valorAtr, valorSorte);
						lblFinalResult.setText("➔ " + rolagemFinal);
					} catch (NumberFormatException e) {
						lblFinalResult.setText("");
					}
				}
				onEstimateChange.run();
			});

			HBox inputRow = new HBox(10);
			inputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
			inputRow.getChildren().addAll(inputDadoAtributo, lblFinalResult);

			diceInputsBox.getChildren().addAll(lbl, inputRow);

			// Ativa a coluna de rolagem de dados
			diceRollColumn.setVisible(true);
			diceRollColumn.setManaged(true);
			lblDiceType.setText("d" + tipoDado);
			lblDiceResult.setText("—");
			lblDiceResult.getStyleClass().remove("hud-roll-maximum");
			lblCritRate.setText(String.format("%.1f%%", ator.getTaxaCritica() * 100));
			lblCritResult.setText("—");
			lblCritResult.getStyleClass().remove("hud-crit-success");
		}


		// --- 3. Painel especial: Domínio Idle Death Gamble ---
		if (ator.getEfeitosAtivos().containsKey("Domínio: Idle Death Gamble")) {
			adicionarPainelIdleDeathGamble(ator, inputsExtras);
		}

		// --- 4. Inputs específicos por habilidade ---
		if (hab != null) {
			adicionarInputsEspecificosHabilidade(hab.getNome(), ator, inputsExtras);
			if (hab.getNome().equals("Justiça Dourada")) {
				adicionarPainelJusticaDourada(ator, inputsExtras);
			}
		}

		// --- 5. Input especial: arma Terrore ---
		if (ator.getArmaEquipada() != null && ator.getArmaEquipada().getNome().equals("Terrore")) {
			adicionarInputExtra("DADO_MEDO_D7", "Dado do Medo (1d7):", inputsExtras);
		}

		// --- 6. Lista consolidada de opções (habilidade OU fantasma nobre) ---
		List<String> listaOpcoes = null;
		if (hab != null) listaOpcoes = hab.getOpcoesSelection();
		else if (fn != null) listaOpcoes = fn.getOpcoesSelection();

		if (listaOpcoes != null && !listaOpcoes.isEmpty()) {
			ToggleGroup group = new ToggleGroup();
			toggleGroupOpcoes = group;
			javafx.scene.Node seletor;
			boolean usarRosaDosVentos = hab instanceof br.com.dantesrpg.model.habilidades.boss.GravityPunch;
			if (usarRosaDosVentos) {
				seletor = criarRosaDosVentos(group, listaOpcoes);
			} else {
				FlowPane flow = new FlowPane(5, 5);
				flow.setPrefWrapLength(300);
				for (String opt : listaOpcoes) {
					ToggleButton tb = new ToggleButton(formatarNomeOpcao(opt));
					tb.setToggleGroup(group);
					tb.setUserData(opt);
					tb.setPrefWidth(120);
					tb.getStyleClass().add("hud-option-toggle");
					flow.getChildren().add(tb);
				}
				seletor = flow;
			}

			if (onOpcaoChanged != null) {
				group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
					if (newT != null) onOpcaoChanged.accept((String) newT.getUserData());
				});
			}

			group.getToggles().get(0).setSelected(true);

			Label lblHeader = new Label("Configurar Ação:");
			lblHeader.getStyleClass().add("hud-section-label");
			diceInputsBox.getChildren().addAll(lblHeader, seletor);
		}

		return new DiceInputsResult(inputDadoAtributo, inputsExtras, tipoDado, toggleGroupOpcoes);
	}

	// ========== PRIVADOS ==========

	private boolean precisaDadoAtributo(boolean isBasicAttack, Habilidade hab) {
		if (isBasicAttack) return true;
		if (hab == null) return false;

		boolean precisa = hab.getMultiplicadorDeDano() > 0;
		String nome = hab.getNome();
		if (nome.equals("Paleta de Cores")) {
			precisa = true;
		}
		if (hab instanceof br.com.dantesrpg.model.habilidades.classe.DistortedSolo
				|| hab instanceof br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo
				|| hab instanceof br.com.dantesrpg.model.habilidades.classe.PlainSolo) {
			precisa = true;
		}
		if (nome.equals("Aprimorar Poção") || nome.equals("Mestre Filosofal")) {
			precisa = true;
		}
		if (nome.equals("Benção Divina") || nome.equals("Visão Divina")) {
			precisa = true;
		}
		if (nome.equals("Caçada") || nome.equals("Trocado")) {
			precisa = false;
		}
		return precisa;
	}

	public static Atributo resolverAtributo(Personagem ator, Habilidade hab) {
		if (hab instanceof br.com.dantesrpg.model.habilidades.classe.AprimorarPocao
				|| hab instanceof br.com.dantesrpg.model.habilidades.classe.MestreFilosofal) {
			return Atributo.SORTE;
		}
		if (hab instanceof br.com.dantesrpg.model.habilidades.VisaoDivina) {
			return Atributo.PERCEPCAO;
		}
		Atributo atr = Atributo.FORCA;
		if (ator.getArmaEquipada() != null) {
			atr = ator.getArmaEquipada().getAtributoMultiplicador();
		}
		if (hab instanceof br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo
				|| hab instanceof br.com.dantesrpg.model.habilidades.classe.DistortedSolo
				|| hab instanceof br.com.dantesrpg.model.habilidades.classe.PlainSolo
				|| hab instanceof br.com.dantesrpg.model.habilidades.BencaoDivina
				|| hab instanceof br.com.dantesrpg.model.habilidades.ProtecaoDosCeus
				|| hab instanceof br.com.dantesrpg.model.habilidades.HolySpirit) {
			atr = Atributo.INSPIRACAO;
		}
		if (hab instanceof br.com.dantesrpg.model.habilidades.PaletaDeCores) {
			atr = Atributo.INSPIRACAO;
		}
		if (hab instanceof br.com.dantesrpg.model.habilidades.fantasmasnobres.Kamehameha) {
			atr = Atributo.INSPIRACAO;
		}
		return atr;
	}

	private void adicionarPainelIdleDeathGamble(Personagem ator, Map<String, TextField> inputsExtras) {
		Label lblAposta = new Label("APOSTA - Idle Death Gamble");
		lblAposta.getStyleClass().addAll("hud-section-label", "hud-value-tactical");
		diceInputsBox.getChildren().add(lblAposta);

		Efeito estrelas = ator.getEfeitosAtivos().get("Estrelas da Sorte");
		int numEstrelas = (estrelas != null) ? estrelas.getStacks() : 0;
		Label lblEstrelas = new Label("Estrelas da Sorte: " + numEstrelas + "/6"
				+ (numEstrelas >= 6 ? " (JACKPOT GARANTIDO!)" : ""));
		lblEstrelas.getStyleClass().addAll("hud-small-label", "hud-gamble-stars");
		alternarClasse(lblEstrelas, "hud-state-jackpot", numEstrelas >= 6);
		diceInputsBox.getChildren().add(lblEstrelas);

		adicionarInputExtra("DADO_LYRIA_1", "Dado 1 (d3):", inputsExtras);
		adicionarInputExtra("DADO_LYRIA_2", "Dado 2 (d3):", inputsExtras);
		adicionarInputExtra("DADO_LYRIA_3", "Dado 3 (d3):", inputsExtras);

		Label lblResultado = new Label("");
		lblResultado.getStyleClass().add("hud-gamble-result");
		lblResultado.setId("lblResultadoAposta");

		Button btnRolar = new Button("Rolar Aposta (3d3)");
		btnRolar.setMaxWidth(Double.MAX_VALUE);
		btnRolar.getStyleClass().addAll("hud-secondary-button", "hud-tactical-primary");
		btnRolar.setOnAction(e -> rolarAposta(inputsExtras, numEstrelas, lblResultado));

		diceInputsBox.getChildren().addAll(btnRolar, lblResultado);
	}

	private void rolarAposta(Map<String, TextField> inputsExtras, int numEstrelas, Label lblResultado) {
		int d1 = DiceRoller.rolarDado(3);
		int d2 = DiceRoller.rolarDado(3);
		int d3 = DiceRoller.rolarDado(3);
		inputsExtras.get("DADO_LYRIA_1").setText(String.valueOf(d1));
		inputsExtras.get("DADO_LYRIA_2").setText(String.valueOf(d2));
		inputsExtras.get("DADO_LYRIA_3").setText(String.valueOf(d3));

		boolean jackpot = (d1 == d2 && d2 == d3) || numEstrelas >= 6;
		if (jackpot) {
			lblResultado.setText("[" + d1 + "] [" + d2 + "] [" + d3 + "]  JACKPOT!");
		} else {
			int soma = d1 + d2 + d3;
			lblResultado.setText("[" + d1 + "] [" + d2 + "] [" + d3 + "]  +" + soma + " TU recuperado");
		}
		alternarClasse(lblResultado, "hud-state-jackpot", jackpot);
		alternarClasse(lblResultado, "hud-text-danger", !jackpot);
	}

	private void adicionarInputsEspecificosHabilidade(String nome, Personagem ator,
			Map<String, TextField> inputsExtras) {
		if (nome.equals("Dissident Aggressor")) {
			adicionarInputExtra(br.com.dantesrpg.model.habilidades.classe.AngryAgain.INPUT_DADO_INSPIRACAO,
					"Dado de Inspira\u00e7\u00e3o:", inputsExtras);
			return;
		}
		if (nome.equals("Black Magic")) {
			adicionarInputExtra(br.com.dantesrpg.model.habilidades.classe.PoisonWasTheCure.INPUT_DADO_CURA,
					"Resultado do dado de cura:", inputsExtras);
			return;
		}
		if (nome.equals("Fight Till Death")) {
			adicionarInputExtra(br.com.dantesrpg.model.habilidades.classe.SeekAndDestroy.INPUT_TESTE_BUFF,
					"Teste de buff dos aliados:", inputsExtras);
			return;
		}
		if (nome.equals("Hell Awaits")) {
			adicionarInputExtra(br.com.dantesrpg.model.habilidades.classe.KickTheChair.INPUT_TESTE_EXPLOSAO,
					"Teste de dano da explos\u00e3o:", inputsExtras);
			return;
		}
		if (nome.equals("Fulgor Negro") && !ator.getEfeitosAtivos().containsKey("Restrição Celestial")) {
			adicionarInputExtra("DADO_CHANCE_FULGOR", "Chance Acerto (1d4):", inputsExtras);
		} else if (nome.equals("Fulgor Negro") && ator.getEfeitosAtivos().containsKey("Restrição Celestial")) {
			adicionarInputExtra("DADO_CHANCE_RESTRICAO", "Restrição (1=Sucesso):", inputsExtras);
		} else if (nome.equals("Trocado")) {
			adicionarInputExtra("DADO_ATRIBUTO", "Rolagem DES/FOR:", inputsExtras);
			adicionarInputExtra("DADO_CHANCE_TROCADO", "Qtd. Moedas (1d4):", inputsExtras);
		} else if (nome.equals("Caçada")) {
			adicionarInputExtra("DADO_ATRIBUTO", "Rolagem DES/SAG:", inputsExtras);
			adicionarInputExtra("DADO_CHANCE_CACADA_1D6", "Qtd. Tiros (1d6):", inputsExtras);
		} else if (nome.equals("Dança Metamoru")) {
			adicionarInputExtra(br.com.dantesrpg.model.habilidades.fantasmasnobres.DancaMetamoru.DADO_DESTREZA_USUARIO,
					"Teste de Destreza do usuário:", inputsExtras);
			adicionarInputExtra(br.com.dantesrpg.model.habilidades.fantasmasnobres.DancaMetamoru.DADO_DESTREZA_ALIADO,
					"Teste de Destreza do aliado:", inputsExtras);
		}
	}

	private GridPane criarRosaDosVentos(ToggleGroup grupo, List<String> direcoes) {
		GridPane rosa = new GridPane();
		rosa.setAlignment(javafx.geometry.Pos.CENTER);
		rosa.setHgap(5);
		rosa.setVgap(5);
		Map<String, int[]> posicoes = Map.of(
				"NO", new int[] { 0, 0 }, "N", new int[] { 1, 0 }, "NE", new int[] { 2, 0 },
				"O", new int[] { 0, 1 }, "L", new int[] { 2, 1 },
				"SO", new int[] { 0, 2 }, "S", new int[] { 1, 2 }, "SE", new int[] { 2, 2 });
		for (String direcao : direcoes) {
			int[] posicao = posicoes.get(direcao);
			if (posicao == null) continue;
			ToggleButton botao = new ToggleButton(direcao);
			botao.setToggleGroup(grupo);
			botao.setUserData(direcao);
			botao.getStyleClass().add("hud-wind-rose-button");
			rosa.add(botao, posicao[0], posicao[1]);
		}
		Label centro = new Label("GRAV.");
		centro.getStyleClass().add("hud-wind-rose-center");
		rosa.add(centro, 1, 1);
		return rosa;
	}

	private void adicionarPainelJusticaDourada(Personagem ator, Map<String, TextField> inputsExtras) {
		int maxInvestimento = ator.getInventario() != null ? ator.getInventario().getPesoTotalMoedas() : 0;

		Label titulo = new Label("JUSTIÇA DOURADA");
		titulo.getStyleClass().addAll("hud-section-label", "hud-value-warning");

		Label resumo = new Label("Investimento disponível: " + maxInvestimento
				+ " pontos (Bronze=1, Prata=2, Ouro=5)");
		resumo.getStyleClass().addAll("hud-text-muted", "hud-small-label");
		resumo.setWrapText(true);

		Slider slider = new Slider(0, Math.max(0, maxInvestimento), 0);
		slider.setMajorTickUnit(Math.max(1, maxInvestimento / 4));
		slider.setMinorTickCount(0);
		slider.setSnapToTicks(true);
		slider.setShowTickMarks(true);
		slider.setShowTickLabels(true);

		TextField input = new TextField("0");
		input.setPromptText("Moedas");
		input.getStyleClass().addAll("hud-dice-input", "hud-warning-field");
		input.setMaxWidth(90);

		slider.valueProperty().addListener((obs, oldValue, newValue) ->
				input.setText(String.valueOf(newValue.intValue())));
		input.textProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == null || newValue.isBlank()) {
				slider.setValue(0);
				return;
			}
			try {
				int valor = Math.max(0, Math.min(maxInvestimento, Integer.parseInt(newValue)));
				if (valor != slider.getValue()) {
					slider.setValue(valor);
				}
			} catch (NumberFormatException e) {
				input.setText(oldValue);
			}
		});

		HBox linha = new HBox(10, slider, input);
		HBox.setHgrow(slider, Priority.ALWAYS);
		linha.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
		VBox painel = new VBox(6, titulo, resumo, linha);
		painel.setId("painelJusticaDourada");
		painel.getStyleClass().addAll("hud-special-panel", "hud-warning-panel");

		inputsExtras.put(br.com.dantesrpg.model.habilidades.classe.JusticaDourada.INPUT_MOEDAS, input);
		diceRollColumn.setVisible(true);
		diceRollColumn.setManaged(true);
		diceRollColumn.getChildren().add(painel);
	}

	private void adicionarInputExtra(String key, String labelText, Map<String, TextField> inputsExtras) {
		Label lbl = new Label(labelText);
		lbl.getStyleClass().add("hud-field-label");
		TextField tf = new TextField();
		tf.setPromptText("Valor...");
		tf.getStyleClass().add("hud-dice-input");
		inputsExtras.put(key, tf);
		diceInputsBox.getChildren().addAll(lbl, tf);
	}

	private static String formatarNomeOpcao(String opt) {
		if (opt != null && opt.startsWith("PocaoAlquimica_")) {
			String[] parts = opt.split("_");
			if (parts.length >= 3) {
				return parts[1] + " (IS: " + parts[2] + ")";
			}
		}
		return opt;
	}

	private static void alternarClasse(javafx.scene.Node node, String classe, boolean ativo) {
		if (ativo) {
			if (!node.getStyleClass().contains(classe)) node.getStyleClass().add(classe);
		} else {
			node.getStyleClass().remove(classe);
		}
	}

}
