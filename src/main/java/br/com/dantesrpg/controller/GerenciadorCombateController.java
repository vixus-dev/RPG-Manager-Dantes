package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.util.EffectFactory;
import br.com.dantesrpg.model.util.EffectTooltipBuilder;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.util.ContratoDeVida;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GerenciadorCombateController {

	// ========== SELETOR DE COMBATENTES ==========
	@FXML private VBox combatListContainer;

	// ========== CABECALHO ==========
	@FXML private TabPane tabPaneDetalhes;
	@FXML private Label lblNomeSelecionado;
	@FXML private Label lblInfoSelecionado;

	// ========== ABA PRINCIPAL ==========
	@FXML private TextField inputVida;
	@FXML private TextField inputEscudoNormal;
	@FXML private TextField inputEscudoSangue;
	@FXML private TextField inputEscudoDivino;
	@FXML private TextField inputValorRapido;
	@FXML private ListView<Personagem> listaTurnos;
	@FXML private TextField inputMana;
	@FXML private TextField inputTU;
	@FXML private Label lblVidaMax;
	@FXML private Label lblManaMax;
	@FXML private CheckBox checkAusente;
	@FXML private CheckBox checkProtagonista;
	@FXML private TextField inputOuro;
	@FXML private TextField inputPrata;
	@FXML private TextField inputBronze;
	@FXML private Label lblTotalMoedas;
	@FXML private TextField inputXPManual;
	@FXML private Label lblXPInfo;
	@FXML private TextField inputQtdAlterarMoedas;
	@FXML private TextField inputGrau;
	@FXML private TextField inputPontos;

	// ========== ABA CONTRATOS ==========
	@FXML private ListView<ContratoDeVida> listaContratos;
	@FXML private ComboBox<String> comboFonteContrato;
	@FXML private TextField inputValorContrato;
	@FXML private TextField inputDuracaoContrato;
	@FXML private CheckBox checkPersisteContrato;

	// ========== ABA INVENTARIO ==========
	@FXML private TextField inputBuscaItem;
	@FXML private ComboBox<String> comboFiltroTipoItem;
	@FXML private ComboBox<String> comboFiltroRaridade;
	@FXML private ListView<String> listaItensFiltrados;
	@FXML private TextField inputQtdItem;
	@FXML private ListView<String> listaInventarioAtual;

	// ========== ABA EFEITOS ==========
	@FXML private ComboBox<String> comboPresetEfeito;
	@FXML private TextField inputDuracaoEfeito;
	@FXML private TextField inputValorEfeito;
	@FXML private TextField inputStacksEfeito;
	@FXML private TextField inputIntervaloEfeito;
	@FXML private ListView<String> listaEfeitosAtivos;
	// Editor de efeito
	@FXML private VBox painelEditorEfeito;
	@FXML private Label lblNomeEfeitoEditando;
	@FXML private TextField inputEditDuracao;
	@FXML private TextField inputEditDanoPorTick;
	@FXML private TextField inputEditIntervalo;
	@FXML private TextField inputEditStacks;
	@FXML private TextField inputEditModChave;
	@FXML private TextField inputEditModValor;

	// ========== BARRA SUPERIOR ==========
	@FXML private ComboBox<String> comboEfeitoAndar;
	@FXML private CheckBox checkEfeitoAndarAtivo;
	@FXML private Label lblContadorAndar;

	// ========== ESTADO INTERNO ==========
	private CombatController mainController;
	private EstadoCombate estadoCombate;
	private Personagem selecionado;
	private String efeitoEditandoNome = null;
	private boolean isSyncing = false;

	private ObservableList<String> masterDataItens = FXCollections.observableArrayList();
	private List<ItemInfo> masterItemInfoList = new ArrayList<>();
	private String filtroTipoAtual = null;
	private String filtroRaridadeAtual = null;

	private Timeline refreshTimer;

	// Classe auxiliar para filtros de item
	private static class ItemInfo {
		String chave;
		String tipo; // "Arma", "Armadura", "Amuleto", "Consumivel"
		String raridade; // "COMUM", "RARO", etc.
		ItemInfo(String chave, String tipo, String raridade) {
			this.chave = chave;
			this.tipo = tipo;
			this.raridade = raridade;
		}
	}

	// =====================================================================
	// INICIALIZACAO
	// =====================================================================

	@FXML
	public void initialize() {
		configurarFiltroItens();
		configurarEfeitosAndar();
		configurarComboEfeitos();
		configurarFiltrosInventario();
		configurarSelecaoEfeitos();

		tabPaneDetalhes.setDisable(true);

		if (listaTurnos != null) {
			listaTurnos.setCellFactory(lv -> new ListCell<Personagem>() {
				@Override
				protected void updateItem(Personagem item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setGraphic(null);
					} else {
						setText(String.format("[%d TU] %s (%s)", item.getContadorTU(), item.getNome(), item.getFaccao()));
						setStyle("-fx-text-fill: white; -fx-font-family: 'Oxanium'; -fx-font-size: 12px;");
					}
				}
			});
		}

		if (comboFonteContrato != null) {
			comboFonteContrato.getItems().addAll(
				ContratoDeVida.FONTE_HUMANO,
				ContratoDeVida.FONTE_BARBARO,
				ContratoDeVida.FONTE_RESPIRAR,
				ContratoDeVida.FONTE_SIT_IN_BALANCE
			);
			comboFonteContrato.getSelectionModel().selectFirst();
		}

		if (listaContratos != null) {
			listaContratos.setCellFactory(lv -> new ListCell<ContratoDeVida>() {
				@Override
				protected void updateItem(ContratoDeVida item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setGraphic(null);
					} else {
						String duracao = item.getDuracaoTURestante() == -1 ? "Indefinido" : (item.getDuracaoTURestante() + " TU");
						setText(String.format("Fonte: %s | Dívida: %.1f/%.1f | Duração: %s",
							item.getFonte(), item.getDividaRestante(), item.getValorTotal(), duracao));
						setStyle("-fx-text-fill: white; -fx-font-family: 'Oxanium'; -fx-font-size: 12px;");
					}
				}
			});
		}

		// Timer de auto-refresh a cada 2 segundos
		refreshTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> onTimerRefresh()));
		refreshTimer.setCycleCount(Timeline.INDEFINITE);
		refreshTimer.play();
	}

	// =====================================================================
	// SETUP DO MAIN CONTROLLER
	// =====================================================================

	public void setMainController(CombatController mainController, EstadoCombate estado) {
		this.mainController = mainController;
		this.estadoCombate = estado;
		carregarDados();
		atualizarContadorAndar();
		if (mainController != null) {
			isSyncing = true;
			try {
				String currentEfeito = mainController.getEfeitoAndarAtual();
				if (currentEfeito != null) {
					comboEfeitoAndar.setValue(currentEfeito);
				}
				checkEfeitoAndarAtivo.setSelected(mainController.isEfeitoAndarAtivo());
			} finally {
				isSyncing = false;
			}
		}
	}

	public void setListaItensMestre(List<String> itens) {
		this.masterDataItens.clear();
		if (itens != null)
			this.masterDataItens.addAll(itens);
	}

	/**
	 * Recebe informacao detalhada dos itens para filtros de tipo e raridade.
	 */
	public void setListaItensComInfo(List<String> chaves, CombatController controller) {
		this.masterDataItens.clear();
		this.masterItemInfoList.clear();
		if (chaves == null) return;

		for (String chave : chaves) {
			this.masterDataItens.add(chave);
			Item item = controller.getItem(chave);
			String tipo = "Outro";
			String raridade = "";
			if (item instanceof Arma) {
				tipo = "Arma";
				raridade = ((Arma) item).getRaridade() != null ? ((Arma) item).getRaridade().name() : "";
			} else if (item instanceof Armadura) {
				tipo = "Armadura";
			} else if (item instanceof Amuleto) {
				tipo = "Amuleto";
			} else if (item != null) {
				tipo = "Consumivel";
			}
			this.masterItemInfoList.add(new ItemInfo(chave, tipo, raridade));
		}
	}

	// =====================================================================
	// TIMER DE AUTO-REFRESH (Atualização em tempo real)
	// =====================================================================

	private void onTimerRefresh() {
		if (estadoCombate == null) return;

		// Atualiza o seletor de combatentes
		removerInvocacoesMortas();
		popularListaCombatentes();
		atualizarListaTurnos();

		// Atualiza campos se personagem selecionado
		if (selecionado != null) {
			atualizarCamposInput();
			// Só faz refresh leve nos efeitos (re-renderiza células sem destruir a lista)
			// NÃO chama atualizarListaEfeitosAtivos() aqui para não resetar a seleção/editor
			listaEfeitosAtivos.refresh();
		}

		// Atualiza contador de andar
		atualizarContadorAndar();
	}

	/**
	 * Chamado pelo CombatController após cada turno para garantir sincronização total.
	 */
	public void refreshCompleto() {
		if (estadoCombate == null) return;
		removerInvocacoesMortas();
		carregarDados();
		atualizarListaTurnos();
		if (selecionado != null) {
			atualizarCamposInput();
			atualizarListaInventario();
			// Se o editor de efeito está aberto, não reconstrói a lista para não perder o contexto
			if (painelEditorEfeito.isVisible()) {
				listaEfeitosAtivos.refresh();
			} else {
				atualizarListaEfeitosAtivos();
			}
		}
		atualizarContadorAndar();
		if (mainController != null) {
			isSyncing = true;
			try {
				String currentEfeito = mainController.getEfeitoAndarAtual();
				if (currentEfeito != null && !currentEfeito.equals(comboEfeitoAndar.getValue())) {
					comboEfeitoAndar.setValue(currentEfeito);
				}
				boolean currentAtivo = mainController.isEfeitoAndarAtivo();
				if (currentAtivo != checkEfeitoAndarAtivo.isSelected()) {
					checkEfeitoAndarAtivo.setSelected(currentAtivo);
				}
			} finally {
				isSyncing = false;
			}
		}
	}

	// =====================================================================
	// REMOÇÃO AUTOMÁTICA DE INVOCAÇÕES MORTAS
	// =====================================================================

	private void removerInvocacoesMortas() {
		if (estadoCombate == null) return;
		Iterator<Personagem> it = estadoCombate.getCombatentes().iterator();
		while (it.hasNext()) {
			Personagem p = it.next();
			// Remove se: morto + sem arquivo JSON (invocação/temporário)
			if (!p.isVivo() && (p.getJsonFileName() == null || p.getJsonFileName().isEmpty())) {
				// Não é um personagem com ficha — pode ser removido
				if (p.isClone() && p.getCriador() != null) {
					p.getCriador().removerCloneMorto(p);
				}
				it.remove();
				System.out.println("GM: Invocação/temporário removido: " + p.getNome());
			}
		}
	}

	// =====================================================================
	// CONTADOR DE EFEITO DE ANDAR
	// =====================================================================

	private void atualizarContadorAndar() {
		if (mainController == null || estadoCombate == null) {
			lblContadorAndar.setVisible(false);
			lblContadorAndar.setManaged(false);
			return;
		}

		if (!mainController.isEfeitoAndarAtivo()) {
			lblContadorAndar.setVisible(false);
			lblContadorAndar.setManaged(false);
			return;
		}

		String efeito = mainController.getEfeitoAndarAtual();
		if (efeito == null || efeito.equals("Nenhum")) {
			lblContadorAndar.setVisible(false);
			lblContadorAndar.setManaged(false);
			return;
		}

		int tickAtual = estadoCombate.getTickCounter();
		int intervalo = getIntervaloAndar(efeito);

		if (intervalo <= 0) {
			lblContadorAndar.setVisible(false);
			lblContadorAndar.setManaged(false);
			return;
		}

		int tuRestante = intervalo - (tickAtual % intervalo);
		if (tuRestante == intervalo) tuRestante = 0; // Acabou de ativar

		String nomeEfeitoCurto = getNomeEfeitoCurto(efeito);

		lblContadorAndar.setVisible(true);
		lblContadorAndar.setManaged(true);

		if (tuRestante == 0) {
			lblContadorAndar.setText(nomeEfeitoCurto + ": ATIVANDO AGORA!");
			lblContadorAndar.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff0000; -fx-font-size: 14px; -fx-padding: 4 12; -fx-background-color: rgba(255,0,0,0.25); -fx-background-radius: 5; -fx-border-color: #ff0000; -fx-border-radius: 5; -fx-border-width: 2;");
		} else if (tuRestante <= 50) {
			lblContadorAndar.setText(nomeEfeitoCurto + " em " + tuRestante + " TU!");
			lblContadorAndar.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff4444; -fx-font-size: 14px; -fx-padding: 4 12; -fx-background-color: rgba(255,50,50,0.2); -fx-background-radius: 5; -fx-border-color: #ff4444; -fx-border-radius: 5; -fx-border-width: 2;");
		} else {
			lblContadorAndar.setText(nomeEfeitoCurto + " em " + tuRestante + " TU");
			lblContadorAndar.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff6b6b; -fx-font-size: 14px; -fx-padding: 4 12; -fx-background-color: rgba(255,50,50,0.1); -fx-background-radius: 5; -fx-border-color: #ff6b6b; -fx-border-radius: 5; -fx-border-width: 1;");
		}
	}

	private int getIntervaloAndar(String efeito) {
		if (efeito.startsWith("2º Andar")) return 200;
		if (efeito.startsWith("3º Andar")) return 300; // O Olho
		if (efeito.contains("4º Andar - Dia")) return 100;
		if (efeito.contains("4º Andar")) return 200;
		if (efeito.startsWith("5º Andar")) return 100;
		if (efeito.startsWith("7º Andar")) return 150;
		if (efeito.startsWith("8.1º Andar")) return 200;
		return 0;
	}

	private String getNomeEfeitoCurto(String efeito) {
		if (efeito.startsWith("2º Andar")) return "Arremesso";
		if (efeito.startsWith("3º Andar")) return "O Olho";
		if (efeito.contains("Dia")) return "Vento Escaldante";
		if (efeito.contains("Noite")) return "Vento Congelante";
		if (efeito.startsWith("5º Andar")) return "Tempestade";
		if (efeito.startsWith("7º Andar")) return "Holofotes";
		if (efeito.startsWith("8.1º Andar")) return "Cruzes";
		return efeito;
	}

	// =====================================================================
	// AÇÕES DA ABA PRINCIPAL
	// =====================================================================

	@FXML
	private void onAplicarStatusClick() {
		if (selecionado == null) return;
		try {
			selecionado.setVidaAtual(parseDoubleSeguro(inputVida.getText()), estadoCombate, mainController);
			
			double escNormal = parseDoubleSeguro(inputEscudoNormal.getText());
			selecionado.setEscudoNormalAtual(escNormal);
			if (escNormal > selecionado.getEscudoNormalMaximo()) {
				selecionado.setEscudoNormalMaximo(escNormal);
			}

			double escSangue = parseDoubleSeguro(inputEscudoSangue.getText());
			selecionado.setEscudoSangueAtual(escSangue);
			if (escSangue > selecionado.getEscudoSangueMaximo()) {
				selecionado.setEscudoSangueMaximo(escSangue);
			}

			double escDivino = parseDoubleSeguro(inputEscudoDivino.getText());
			selecionado.setEscudoDivinoAtual(escDivino);
			if (escDivino > selecionado.getEscudoDivinoMaximo()) {
				selecionado.setEscudoDivinoMaximo(escDivino);
			}

			selecionado.setManaAtual(parseDoubleSeguro(inputMana.getText()));
			selecionado.setContadorTU(parseIntSeguro(inputTU.getText()));
			
			if (selecionado.getInventario() != null) {
				int totalOuro = parseIntSeguro(inputOuro.getText());
				int totalPrata = parseIntSeguro(inputPrata.getText());
				int totalBronze = parseIntSeguro(inputBronze.getText());
				selecionado.getInventario().setMoedasOuro(totalOuro);
				selecionado.getInventario().setMoedasPrata(totalPrata);
				selecionado.getInventario().setMoedasBronze(totalBronze);
			}
			
			autoSaveAndRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onReviverClick() {
		if (selecionado != null) {
			selecionado.setVidaAtual(1.0, estadoCombate, mainController);
			atualizarCamposInput();
			autoSaveAndRefresh();
		}
	}

	@FXML
	private void onDescansarClick() {
		if (selecionado != null) {
			selecionado.setVidaAtual(selecionado.getVidaMaxima(), estadoCombate, mainController);
			selecionado.setManaAtual(selecionado.getManaMaxima());
			atualizarCamposInput();
			autoSaveAndRefresh();
		}
	}

	@FXML
	private void onMatarClick() {
		if (selecionado != null) {
			selecionado.setVidaAtual(0.0, estadoCombate, mainController);
			atualizarCamposInput();
			autoSaveAndRefresh();
		}
	}

	@FXML
	private void onAdicionarXPClick() {
		if (selecionado == null) return;
		try {
			int xp = parseIntSeguro(inputXPManual.getText());
			selecionado.ganharExperiencia(xp);
			System.out.println("GM: Adicionado " + xp + " XP para " + selecionado.getNome());
			autoSaveAndRefresh();
			inputXPManual.clear();
		} catch (NumberFormatException e) {
			System.err.println("Erro: Valor de XP invalido.");
		}
	}

	@FXML
	private void onCheckAusenteChange() {
		if (selecionado != null) {
			selecionado.setAusente(checkAusente.isSelected());
			autoSaveAndRefresh();
		}
	}

	@FXML
	private void onCheckProtagonistaChange() {
		if (selecionado != null) {
			selecionado.setProtagonista(checkProtagonista.isSelected());
			autoSaveAndRefresh();
		}
	}

	// =====================================================================
	// FINANCAS — Adicionar / Remover moedas
	// =====================================================================

	@FXML
	private void onAddOuroClick() {
		alterarMoeda("ouro", true);
	}
	@FXML
	private void onRemoveOuroClick() {
		alterarMoeda("ouro", false);
	}
	@FXML
	private void onAddPrataClick() {
		alterarMoeda("prata", true);
	}
	@FXML
	private void onRemovePrataClick() {
		alterarMoeda("prata", false);
	}
	@FXML
	private void onAddBronzeClick() {
		alterarMoeda("bronze", true);
	}
	@FXML
	private void onRemoveBronzeClick() {
		alterarMoeda("bronze", false);
	}

	private void alterarMoeda(String tipo, boolean adicionar) {
		if (selecionado == null) return;
		try {
			int valor = 1;
			if (inputQtdAlterarMoedas != null && !inputQtdAlterarMoedas.getText().trim().isEmpty()) {
				valor = parseIntSeguro(inputQtdAlterarMoedas.getText());
			}
			if (valor <= 0) return;

			switch (tipo) {
				case "ouro":
					if (adicionar) selecionado.getInventario().receberOuro(valor);
					else selecionado.getInventario().gastarOuro(valor);
					break;
				case "prata":
					if (adicionar) selecionado.getInventario().receberPrata(valor);
					else selecionado.getInventario().gastarPrata(valor);
					break;
				case "bronze":
					if (adicionar) selecionado.getInventario().receber(valor);
					else selecionado.getInventario().gastarBronze(valor);
					break;
			}
			atualizarCamposMoedas();
			autoSaveAndRefresh();
		} catch (Exception e) {
			// Input invalido
		}
	}

	private void atualizarCamposMoedas() {
		if (selecionado == null || selecionado.getInventario() == null) return;
		if (!inputOuro.isFocused())
			inputOuro.setText(String.valueOf(selecionado.getInventario().getMoedasOuro()));
		if (!inputPrata.isFocused())
			inputPrata.setText(String.valueOf(selecionado.getInventario().getMoedasPrata()));
		if (!inputBronze.isFocused())
			inputBronze.setText(String.valueOf(selecionado.getInventario().getMoedasBronze()));
		lblTotalMoedas.setText("Total: " + formatarMoedas(selecionado.getInventario().getValorTotalEmBronze()) + " bronze");
	}

	private String formatarMoedas(int valor) {
		if (valor >= 1000000) return String.format("%.2fM", valor / 1000000.0);
		if (valor >= 1000) return String.format("%.1fK", valor / 1000.0);
		return String.valueOf(valor);
	}

	/**
	 * Parse seguro de double que aceita tanto vírgula quanto ponto como separador decimal.
	 * Ex: "10,5" e "10.5" ambos retornam 10.5
	 */
	private double parseDoubleSeguro(String texto) {
		if (texto == null || texto.trim().isEmpty()) return 0.0;
		return Double.parseDouble(texto.trim().replace(',', '.'));
	}

	/**
	 * Parse seguro de int que remove espaços e caracteres decimais.
	 * Ex: "100", "100.0", "100,0" todos retornam 100
	 */
	private int parseIntSeguro(String texto) {
		if (texto == null || texto.trim().isEmpty()) return 0;
		String limpo = texto.trim().replace(',', '.');
		// Se tem ponto, pega só a parte inteira
		if (limpo.contains(".")) {
			return (int) Double.parseDouble(limpo);
		}
		return Integer.parseInt(limpo);
	}

	// =====================================================================
	// AÇÕES DA ABA INVENTARIO
	// =====================================================================

	@FXML
	private void onAdicionarItemClick() {
		String nomeItem = listaItensFiltrados.getSelectionModel().getSelectedItem();
		if (selecionado != null && nomeItem != null && mainController != null) {
			try {
				int qtd = parseIntSeguro(inputQtdItem.getText());
				Item item = mainController.getItem(nomeItem);
				if (item != null) {
					for (int i = 0; i < qtd; i++)
						selecionado.getInventario().adicionarItem(item);
					System.out.println("GM: Adicionado " + qtd + "x " + item.getNome());
					atualizarListaInventario();
					autoSaveAndRefresh();
				}
			} catch (Exception e) {
				// Input invalido
			}
		}
	}

	@FXML
	private void onRemoverItemClick() {
		String selectedString = listaInventarioAtual.getSelectionModel().getSelectedItem();
		if (selecionado != null && selectedString != null && mainController != null) {
			// O item armazena a chave no userData
			String tipoItem = selectedString;
			Item itemModelo = mainController.getItem(tipoItem);
			if (itemModelo != null) {
				selecionado.getInventario().removerItem(itemModelo);
				System.out.println("GM: Removido 1x " + tipoItem);
				atualizarListaInventario();
				autoSaveAndRefresh();
			}
		}
	}

	// =====================================================================
	// AÇÕES DA ABA EFEITOS
	// =====================================================================

	@FXML
	private void onAdicionarEfeitoManualClick() {
		if (selecionado == null) return;
		try {
			String nomePreset = comboPresetEfeito.getValue();
			int duracao = parseIntSeguro(inputDuracaoEfeito.getText());
			int valor = parseIntSeguro(inputValorEfeito.getText());

			Efeito novoEfeito = EffectFactory.criarEfeito(nomePreset, duracao, valor);

			// Aplica customizações opcionais
			String stacksText = inputStacksEfeito.getText();
			if (stacksText != null && !stacksText.trim().isEmpty()) {
				int stacks = parseIntSeguro(stacksText);
				if (stacks > 0) novoEfeito.setStacks(stacks);
			}

			String intervaloText = inputIntervaloEfeito.getText();
			if (intervaloText != null && !intervaloText.trim().isEmpty()) {
				int intervalo = parseIntSeguro(intervaloText);
				if (intervalo > 0) novoEfeito.setIntervaloTickTU(intervalo);
			}

			selecionado.adicionarEfeito(novoEfeito);
			selecionado.recalcularAtributosEstatisticas();

			atualizarListaEfeitosAtivos();
			autoSaveAndRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onLimparEfeitosClick() {
		if (selecionado == null) return;
		List<String> nomes = new ArrayList<>(selecionado.getEfeitosAtivos().keySet());
		for (String n : nomes)
			selecionado.removerEfeito(n);
		selecionado.recalcularAtributosEstatisticas();
		atualizarListaEfeitosAtivos();
		autoSaveAndRefresh();
	}

	@FXML
	private void onSalvarEdicaoEfeitoClick() {
		if (selecionado == null || efeitoEditandoNome == null) return;
		Efeito efeito = selecionado.getEfeitosAtivos().get(efeitoEditandoNome);
		if (efeito == null) return;

		try {
			// Atualiza duracao restante
			String duracaoText = inputEditDuracao.getText();
			if (duracaoText != null && !duracaoText.trim().isEmpty()) {
				efeito.setDuracaoTURestante(parseIntSeguro(duracaoText));
			}

			// Atualiza dano por tick
			String danoText = inputEditDanoPorTick.getText();
			if (danoText != null && !danoText.trim().isEmpty()) {
				efeito.setDanoPorTick(parseIntSeguro(danoText));
			}

			// Atualiza intervalo de tick
			String intervaloText = inputEditIntervalo.getText();
			if (intervaloText != null && !intervaloText.trim().isEmpty()) {
				efeito.setIntervaloTickTU(parseIntSeguro(intervaloText));
			}

			// Atualiza stacks
			String stacksText = inputEditStacks.getText();
			if (stacksText != null && !stacksText.trim().isEmpty()) {
				efeito.setStacks(parseIntSeguro(stacksText));
			}

			// Atualiza/adiciona modificador
			String modChave = inputEditModChave.getText();
			String modValorText = inputEditModValor.getText();
			if (modChave != null && !modChave.trim().isEmpty() && modValorText != null && !modValorText.trim().isEmpty()) {
				double modValor = parseDoubleSeguro(modValorText);
				Map<String, Double> mods = efeito.getModificadores();
				if (mods == null) {
					mods = new HashMap<>();
					efeito.setModificadores(mods);
				}
				mods.put(modChave.toUpperCase(), modValor);
			}

			selecionado.recalcularAtributosEstatisticas();
			atualizarListaEfeitosAtivos();
			autoSaveAndRefresh();
			System.out.println("GM: Efeito '" + efeitoEditandoNome + "' editado com sucesso.");

		} catch (Exception e) {
			System.err.println("Erro ao editar efeito: " + e.getMessage());
		}
	}

	@FXML
	private void onRemoverEfeitoClick() {
		if (selecionado == null || efeitoEditandoNome == null) return;
		selecionado.removerEfeito(efeitoEditandoNome);
		selecionado.recalcularAtributosEstatisticas();
		efeitoEditandoNome = null;
		painelEditorEfeito.setVisible(false);
		painelEditorEfeito.setManaged(false);
		atualizarListaEfeitosAtivos();
		autoSaveAndRefresh();
	}

	// =====================================================================
	// AÇÕES GLOBAIS / SISTEMA
	// =====================================================================

	@FXML
	private void onSalvarTudoClick() {
		if (mainController != null) mainController.salvarEstadoJogadores();
	}

	@FXML
	private void onCarregarTudoClick() {
		if (mainController != null) {
			mainController.carregarEstadoJogadores();
			carregarDados();
		}
	}

	@FXML
	private void onCurarTodosClick() {
		if (estadoCombate == null) return;
		for (Personagem p : estadoCombate.getCombatentes()) {
			if (p.getFaccao() != null && p.getFaccao().equals("JOGADOR") && p.isVivo()) {
				p.setVidaAtual(p.getVidaMaxima(), estadoCombate, mainController);
			}
		}
		atualizarCamposInput();
		autoSaveAndRefresh();
	}

	@FXML
	private void onRefreshClick() {
		refreshCompleto();
	}

	// =====================================================================
	// AUTO-SAVE E REFRESH
	// =====================================================================

	private void autoSaveAndRefresh() {
		if (mainController != null) {
			mainController.atualizarInterfaceTotal();
			mainController.salvarEstadoJogadores();
		}
		popularListaCombatentes();
	}

	// =====================================================================
	// SELECAO DE PERSONAGEM
	// =====================================================================

	private void selecionarPersonagem(Personagem p) {
		this.selecionado = p;
		if (p == null) {
			tabPaneDetalhes.setDisable(true);
			lblNomeSelecionado.setText("Selecione um Personagem");
			lblInfoSelecionado.setText("");
			painelEditorEfeito.setVisible(false);
			painelEditorEfeito.setManaged(false);
			return;
		}

		tabPaneDetalhes.setDisable(false);
		lblNomeSelecionado.setText(p.getNome());

		// Info complementar
		String info = "";
		if (p.getClasse() != null) info += p.getClasse().getClass().getSimpleName();
		if (p.getRaca() != null) info += " | " + p.getRaca().getClass().getSimpleName();
		info += " | Nv." + p.getNivel();
		if (p.getJsonFileName() != null) info += " | " + p.getJsonFileName();
		lblInfoSelecionado.setText(info);

		atualizarCamposInput();
		atualizarListaInventario();
		atualizarListaEfeitosAtivos();
	}

	// =====================================================================
	// ATUALIZAR CAMPOS (Chamado pelo timer e por seleção)
	// =====================================================================

	private void atualizarCamposInput() {
		if (selecionado == null) return;

		// Só atualiza se o campo não está focado (evita conflito com digitação do GM)
		if (!inputVida.isFocused())
			inputVida.setText(String.format("%.1f", selecionado.getVidaAtual()));
		if (inputEscudoNormal != null && !inputEscudoNormal.isFocused())
			inputEscudoNormal.setText(String.format("%.1f", selecionado.getEscudoNormalAtual()));
		if (inputEscudoSangue != null && !inputEscudoSangue.isFocused())
			inputEscudoSangue.setText(String.format("%.1f", selecionado.getEscudoSangueAtual()));
		if (inputEscudoDivino != null && !inputEscudoDivino.isFocused())
			inputEscudoDivino.setText(String.format("%.1f", selecionado.getEscudoDivinoAtual()));
		if (!inputMana.isFocused())
			inputMana.setText(String.format("%.1f", selecionado.getManaAtual()));
		if (!inputTU.isFocused())
			inputTU.setText(String.valueOf(selecionado.getContadorTU()));

		lblVidaMax.setText("/ " + String.format("%.0f", selecionado.getVidaMaxima()));
		lblManaMax.setText("/ " + String.format("%.0f", selecionado.getManaMaxima()));

		checkAusente.setSelected(selecionado.isAusente());
		checkProtagonista.setSelected(selecionado.isProtagonista());

		// Moedas
		atualizarCamposMoedas();

		// XP Info
		if (selecionado.getJsonFileName() != null) {
			lblXPInfo.setText("XP: " + selecionado.getXpAtual() + " / " + selecionado.getXpParaProximoNivel()
					+ " | Grau: " + selecionado.getGrau() + " | Pts: " + selecionado.getPontosParaDistribuir());
			if (inputGrau != null && !inputGrau.isFocused())
				inputGrau.setText(String.valueOf(selecionado.getGrau()));
			if (inputPontos != null && !inputPontos.isFocused())
				inputPontos.setText(String.valueOf(selecionado.getPontosParaDistribuir()));
		} else {
			lblXPInfo.setText("XP Reward: " + selecionado.getXpReward());
			if (inputGrau != null) inputGrau.setText("0");
			if (inputPontos != null) inputPontos.setText("0");
		}
		atualizarListaContratos();
	}

	// =====================================================================
	// LISTA DE EFEITOS ATIVOS
	// =====================================================================

	private void atualizarListaEfeitosAtivos() {
		listaEfeitosAtivos.getItems().clear();
		if (selecionado == null) return;

		Map<String, Efeito> efeitos = selecionado.getEfeitosAtivos();
		if (efeitos == null || efeitos.isEmpty()) {
			painelEditorEfeito.setVisible(false);
			painelEditorEfeito.setManaged(false);
			return;
		}

		for (Map.Entry<String, Efeito> entry : efeitos.entrySet()) {
			listaEfeitosAtivos.getItems().add(entry.getKey());
		}

		// Cell factory com info detalhada
		listaEfeitosAtivos.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String nomeEfeito, boolean empty) {
				super.updateItem(nomeEfeito, empty);
				if (empty || nomeEfeito == null || selecionado == null) {
					setText(null);
					setStyle("");
					return;
				}

				Efeito ef = selecionado.getEfeitosAtivos().get(nomeEfeito);
				if (ef == null) {
					setText(nomeEfeito + " (expirado)");
					return;
				}

				StringBuilder sb = new StringBuilder();
				// Icone por tipo
				switch (ef.getTipo()) {
					case BUFF: sb.append("[BUFF] "); break;
					case DEBUFF: sb.append("[DEBF] "); break;
					case DOT: sb.append("[DOT]  "); break;
				}
				sb.append(nomeEfeito);
				sb.append("  |  ").append(ef.getDuracaoTURestante()).append(" TU rest.");

				if (ef.getDanoPorTick() > 0) {
					sb.append("  |  ").append(ef.getDanoPorTick()).append(" dmg/tick");
				}
				if (ef.getStacks() > 0) {
					sb.append("  |  x").append(ef.getStacks()).append(" stacks");
				}
				if (ef.getModificadores() != null && !ef.getModificadores().isEmpty()) {
					for (Map.Entry<String, Double> mod : ef.getModificadores().entrySet()) {
						sb.append("  |  ").append(mod.getKey()).append(": ");
						sb.append(String.format("%.2f", mod.getValue()));
					}
				}

				setText(sb.toString());

				// Cor por tipo
				switch (ef.getTipo()) {
					case BUFF:
						setStyle("-fx-text-fill: #88ddff;");
						break;
					case DEBUFF:
						setStyle("-fx-text-fill: #ff8888;");
						break;
					case DOT:
						setStyle("-fx-text-fill: #cc88ff;");
						break;
				}
			}
		});
	}

	private void configurarSelecaoEfeitos() {
		listaEfeitosAtivos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == null || selecionado == null) {
				painelEditorEfeito.setVisible(false);
				painelEditorEfeito.setManaged(false);
				efeitoEditandoNome = null;
				return;
			}

			Efeito efeito = selecionado.getEfeitosAtivos().get(newVal);
			if (efeito == null) return;

			efeitoEditandoNome = newVal;
			painelEditorEfeito.setVisible(true);
			painelEditorEfeito.setManaged(true);

			lblNomeEfeitoEditando.setText("Editando: " + newVal + " [" + efeito.getTipo() + "]");
			inputEditDuracao.setText(String.valueOf(efeito.getDuracaoTURestante()));
			inputEditDanoPorTick.setText(String.valueOf(efeito.getDanoPorTick()));
			inputEditIntervalo.setText(String.valueOf(efeito.getIntervaloTickTU()));
			inputEditStacks.setText(String.valueOf(efeito.getStacks()));

			// Carrega primeiro modificador se existir
			if (efeito.getModificadores() != null && !efeito.getModificadores().isEmpty()) {
				Map.Entry<String, Double> first = efeito.getModificadores().entrySet().iterator().next();
				inputEditModChave.setText(first.getKey());
				inputEditModValor.setText(String.format("%.4f", first.getValue()));
			} else {
				inputEditModChave.clear();
				inputEditModValor.clear();
			}
		});
	}

	// =====================================================================
	// LISTA DE INVENTARIO
	// =====================================================================

	private void atualizarListaInventario() {
		listaInventarioAtual.getItems().clear();
		if (selecionado == null || selecionado.getInventario() == null) return;

		Map<String, Integer> itens = selecionado.getInventario().getItensAgrupados();
		listaInventarioAtual.getItems().addAll(itens.keySet());

		listaInventarioAtual.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String tipoItem, boolean empty) {
				super.updateItem(tipoItem, empty);
				if (empty || tipoItem == null) {
					setText(null);
					setStyle("");
					return;
				}

				int qtd = itens.getOrDefault(tipoItem, 0);
				Item modelo = mainController != null ? mainController.getItem(tipoItem) : null;

				if (modelo != null) {
					int ocGrau = selecionado.getInventario().getOverclockDoItem(tipoItem);
					if (ocGrau > 0) modelo.setGrauOverclock(ocGrau);
				}

				String nomeExibicao = (modelo != null) ? modelo.getNome() : tipoItem;
				String tipoStr = "";
				String cor = "-fx-text-fill: white;";

				if (modelo instanceof Arma) {
					tipoStr = " [Arma]";
					Raridade rar = ((Arma) modelo).getRaridade();
					cor = getCorRaridade(rar);
				} else if (modelo instanceof Armadura) {
					tipoStr = " [Armadura]";
					cor = "-fx-text-fill: #88bbff;";
				} else if (modelo instanceof Amuleto) {
					tipoStr = " [Amuleto]";
					cor = "-fx-text-fill: #ffcc44;";
				} else if (modelo != null) {
					tipoStr = " [Item]";
					cor = "-fx-text-fill: #aaffaa;";
				}

				if (modelo != null && modelo.isOverclockado()) {
					setText(modelo.getNomeComOverclock() + " x" + qtd + tipoStr);
				} else {
					setText(nomeExibicao + " x" + qtd + tipoStr);
				}
				setStyle(cor);
			}
		});
	}

	private String getCorRaridade(Raridade rar) {
		if (rar == null) return "-fx-text-fill: white;";
		switch (rar) {
			case COMUM: return "-fx-text-fill: #aaaaaa;";
			case INCOMUM: return "-fx-text-fill: #66ff66;";
			case RARO: return "-fx-text-fill: #6688ff;";
			case EPICO: return "-fx-text-fill: #bb66ff;";
			case LENDARIO: return "-fx-text-fill: #ffaa00;";
			case UNICO: return "-fx-text-fill: #ff4444;";
			case MITICO: return "-fx-text-fill: #ff66cc;";
			default: return "-fx-text-fill: white;";
		}
	}

	// =====================================================================
	// CARREGAR DADOS
	// =====================================================================

	@FXML
	private void atualizarTabela() {
		carregarDados();
	}

	private void carregarDados() {
		if (estadoCombate != null) {
			popularListaCombatentes();
		}
	}

	private void popularListaCombatentes() {
		if (estadoCombate == null || combatListContainer == null) return;

		combatListContainer.getChildren().clear();
		for (Personagem p : estadoCombate.getCombatentes()) {
			VBox btnContent = new VBox(4);
			btnContent.setAlignment(Pos.CENTER_LEFT);
			btnContent.setPadding(new Insets(4, 8, 4, 8));

			// Nome + Faction badge/color
			HBox nameRow = new HBox(8);
			nameRow.setAlignment(Pos.CENTER_LEFT);
			Label lblNome = new Label(p.getNome());
			lblNome.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
			nameRow.getChildren().add(lblNome);

			// Faction indicator
			Label lblFaccao = new Label(p.getFaccao());
			if ("JOGADOR".equalsIgnoreCase(p.getFaccao())) {
				lblFaccao.setStyle("-fx-text-fill: #00ffcc; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: rgba(0, 255, 204, 0.1); -fx-padding: 1 4; -fx-background-radius: 3;");
			} else {
				lblFaccao.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: rgba(255, 68, 68, 0.1); -fx-padding: 1 4; -fx-background-radius: 3;");
			}
			nameRow.getChildren().add(lblFaccao);

			// HP + Shields progress / label info
			Label lblStats = new Label();
			lblStats.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
			if (p.isProtagonista()) {
				lblStats.setText("HP: ? / ?");
			} else {
				StringBuilder statsSb = new StringBuilder();
				statsSb.append("HP: ").append(String.format("%.0f/%.0f", p.getVidaAtual(), p.getVidaMaxima()));
				double normalSh = p.getEscudoNormalAtual();
				double bloodSh = p.getEscudoSangueAtual();
				double divineSh = p.getEscudoDivinoAtual();
				if (normalSh > 0 || bloodSh > 0 || divineSh > 0) {
					statsSb.append(" [");
					boolean first = true;
					if (normalSh > 0) {
						statsSb.append("E:").append(String.format("%.0f", normalSh));
						first = false;
					}
					if (bloodSh > 0) {
						if (!first) statsSb.append("|");
						statsSb.append("S:").append(String.format("%.0f", bloodSh));
						first = false;
					}
					if (divineSh > 0) {
						if (!first) statsSb.append("|");
						statsSb.append("D:").append(String.format("%.0f", divineSh));
					}
					statsSb.append("]");
				}
				lblStats.setText(statsSb.toString());
			}

			// TU + Status info
			HBox statusRow = new HBox(12);
			statusRow.setAlignment(Pos.CENTER_LEFT);
			Label lblTU = new Label("TU: " + p.getContadorTU());
			lblTU.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 11px; -fx-font-weight: bold;");
			statusRow.getChildren().add(lblTU);

			Label lblStatus = new Label();
			if (p.isAusente()) {
				lblStatus.setText("[AUSENTE]");
				lblStatus.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
			} else if (!p.isVivo()) {
				lblStatus.setText("[MORTO]");
				lblStatus.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 10px; -fx-font-weight: bold;");
			} else {
				int activeEffects = p.getEfeitosAtivos() != null ? p.getEfeitosAtivos().size() : 0;
				if (activeEffects > 0) {
					lblStatus.setText("Efeitos: " + activeEffects);
					lblStatus.setStyle("-fx-text-fill: #cc88ff; -fx-font-size: 10px;");
				} else {
					lblStatus.setText("OK");
					lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 10px;");
				}
			}
			statusRow.getChildren().add(lblStatus);

			btnContent.getChildren().addAll(nameRow, lblStats, statusRow);

			Button btnCombatente = new Button();
			btnCombatente.setGraphic(btnContent);
			btnCombatente.setMaxWidth(Double.MAX_VALUE);

			String baseStyleClass = "player-select-button";
			btnCombatente.getStyleClass().add(baseStyleClass);

			if ("JOGADOR".equalsIgnoreCase(p.getFaccao())) {
				if (!p.isVivo()) {
					btnCombatente.setStyle("-fx-background-color: #1a0f12; -fx-border-color: #552222;");
				} else {
					btnCombatente.setStyle("-fx-background-color: #121622; -fx-border-color: #223355;");
				}
			} else {
				if (!p.isVivo()) {
					btnCombatente.setStyle("-fx-background-color: #1a0808; -fx-border-color: #551111;");
				} else {
					btnCombatente.setStyle("-fx-background-color: #1e1115; -fx-border-color: #44222a;");
				}
			}

			if (p == selecionado) {
				btnCombatente.getStyleClass().add("player-select-button-selected");
				if ("JOGADOR".equalsIgnoreCase(p.getFaccao())) {
					btnCombatente.setStyle(btnCombatente.getStyle() + " -fx-border-color: #00f0ff; -fx-border-width: 2px;");
				} else {
					btnCombatente.setStyle(btnCombatente.getStyle() + " -fx-border-color: #ff4444; -fx-border-width: 2px;");
				}
			}

			btnCombatente.setOnAction(e -> {
				selecionarPersonagem(p);
				popularListaCombatentes();
			});

			combatListContainer.getChildren().add(btnCombatente);
		}
	}

	private void configurarFiltroItens() {
		FilteredList<String> filteredData = new FilteredList<>(masterDataItens, p -> true);

		// Listener que combina filtro de texto + tipo + raridade
		inputBuscaItem.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosItem(filteredData));

		listaItensFiltrados.setItems(filteredData);

		// Cell factory com cores de raridade
		listaItensFiltrados.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String chave, boolean empty) {
				super.updateItem(chave, empty);
				if (empty || chave == null) {
					setText(null);
					setStyle("");
					return;
				}

				if (mainController != null) {
					Item item = mainController.getItem(chave);
					if (item != null) {
						String tipoStr = "";
						String cor = "-fx-text-fill: white;";
						if (item instanceof Arma) {
							tipoStr = " [Arma]";
							cor = getCorRaridade(((Arma) item).getRaridade());
						} else if (item instanceof Armadura) {
							tipoStr = " [Armadura]";
							cor = "-fx-text-fill: #88bbff;";
						} else if (item instanceof Amuleto) {
							tipoStr = " [Amuleto]";
							cor = "-fx-text-fill: #ffcc44;";
						} else {
							tipoStr = " [Item]";
							cor = "-fx-text-fill: #aaffaa;";
						}
						setText(item.getNome() + tipoStr);
						setStyle(cor);
						return;
					}
				}
				setText(chave);
				setStyle("-fx-text-fill: white;");
			}
		});
	}

	private void configurarFiltrosInventario() {
		// Combo de tipo
		comboFiltroTipoItem.getItems().addAll("Todos", "Arma", "Armadura", "Amuleto", "Consumivel");
		comboFiltroTipoItem.getSelectionModel().selectFirst();
		comboFiltroTipoItem.valueProperty().addListener((obs, oldVal, newVal) -> {
			filtroTipoAtual = "Todos".equals(newVal) ? null : newVal;
			aplicarFiltrosItem(null);
		});

		// Combo de raridade
		comboFiltroRaridade.getItems().add("Todas");
		for (Raridade r : Raridade.values()) {
			comboFiltroRaridade.getItems().add(r.name());
		}
		comboFiltroRaridade.getSelectionModel().selectFirst();
		comboFiltroRaridade.valueProperty().addListener((obs, oldVal, newVal) -> {
			filtroRaridadeAtual = "Todas".equals(newVal) ? null : newVal;
			aplicarFiltrosItem(null);
		});
	}

	private void aplicarFiltrosItem(FilteredList<String> list) {
		if (list == null) {
			// Re-get from listaItensFiltrados
			if (listaItensFiltrados.getItems() instanceof FilteredList) {
				list = (FilteredList<String>) listaItensFiltrados.getItems();
			} else {
				return;
			}
		}

		final String textoFiltro = inputBuscaItem.getText();
		final String tipoFiltro = filtroTipoAtual;
		final String raridadeFiltro = filtroRaridadeAtual;

		list.setPredicate(chave -> {
			// Filtro de texto
			if (textoFiltro != null && !textoFiltro.isEmpty()) {
				boolean matchTexto = chave.toLowerCase().contains(textoFiltro.toLowerCase());
				if (!matchTexto && mainController != null) {
					Item item = mainController.getItem(chave);
					if (item != null) {
						matchTexto = item.getNome().toLowerCase().contains(textoFiltro.toLowerCase());
					}
				}
				if (!matchTexto) return false;
			}

			// Filtro de tipo
			if (tipoFiltro != null && mainController != null) {
				Item item = mainController.getItem(chave);
				if (item == null) return false;
				switch (tipoFiltro) {
					case "Arma": if (!(item instanceof Arma)) return false; break;
					case "Armadura": if (!(item instanceof Armadura)) return false; break;
					case "Amuleto": if (!(item instanceof Amuleto)) return false; break;
					case "Consumivel": if (item instanceof Arma || item instanceof Armadura || item instanceof Amuleto) return false; break;
				}
			}

			// Filtro de raridade
			if (raridadeFiltro != null && mainController != null) {
				Item item = mainController.getItem(chave);
				if (item instanceof Arma) {
					Raridade rar = ((Arma) item).getRaridade();
					if (rar == null || !rar.name().equals(raridadeFiltro)) return false;
				} else {
					return false; // Só armas têm raridade atualmente
				}
			}

			return true;
		});
	}

	private void configurarEfeitosAndar() {
		comboEfeitoAndar.getItems().addAll("Nenhum", "2º Andar - Arremesso", "3º Andar - O Olho", "4º Andar - Dia",
				"4º Andar - Noite", "4º andar - Eclipse", "5º Andar - Tempestade", "7º Andar - Holofotes",
				"8.1º Andar - Cruzes", "9º Andar - A Sala dos Trovões");
		comboEfeitoAndar.getSelectionModel().selectFirst();

		comboEfeitoAndar.valueProperty().addListener((o, old, newVal) -> {
			if (isSyncing) return;
			if (mainController != null)
				mainController.setEfeitoAndar(newVal, checkEfeitoAndarAtivo.isSelected());
			atualizarContadorAndar();
		});
		checkEfeitoAndarAtivo.selectedProperty().addListener((o, old, isSelected) -> {
			if (isSyncing) return;
			if (mainController != null)
				mainController.setEfeitoAndar(comboEfeitoAndar.getValue(), isSelected);
			atualizarContadorAndar();
		});
	}

	private void configurarComboEfeitos() {
		comboPresetEfeito.getItems().setAll(EffectFactory.getNomesPresets());
		comboPresetEfeito.getSelectionModel().selectFirst();

		comboPresetEfeito.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setTooltip(null);
					setStyle("");
					return;
				}

				setText(item);

				// Cor baseada no tipo do efeito
				Efeito preview = EffectFactory.criarEfeito(item, 300, 100);
				switch (preview.getTipo()) {
					case BUFF:
						setStyle("-fx-text-fill: #88ddff;");
						break;
					case DEBUFF:
						setStyle("-fx-text-fill: #ff8888;");
						break;
					case DOT:
						setStyle("-fx-text-fill: #cc88ff;");
						break;
				}

				Tooltip tip = new Tooltip(EffectTooltipBuilder.buildTooltip(preview));
				tip.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas'; -fx-background-color: #1a1a2e; -fx-text-fill: #e0e0e0; -fx-border-color: #444; -fx-border-width: 1; -fx-padding: 6;");
				tip.setShowDelay(javafx.util.Duration.millis(300));
				tip.setMaxWidth(350);
				tip.setWrapText(true);
				setTooltip(tip);
			}
		});
	}

	private void atualizarListaTurnos() {
		if (listaTurnos == null || estadoCombate == null) return;
		List<Personagem> sorted = new ArrayList<>(estadoCombate.getCombatentes());
		sorted.sort((p1, p2) -> Integer.compare(p1.getContadorTU(), p2.getContadorTU()));
		listaTurnos.setItems(FXCollections.observableArrayList(sorted));
	}

	@FXML
	private void onSubirTurnoClick() {
		moverPersonagemNaFila(-1);
	}

	@FXML
	private void onDescerTurnoClick() {
		moverPersonagemNaFila(1);
	}

	private void moverPersonagemNaFila(int direcao) {
		Personagem p = listaTurnos.getSelectionModel().getSelectedItem();
		if (p == null || estadoCombate == null) return;

		List<Personagem> sorted = new ArrayList<>(estadoCombate.getCombatentes());
		sorted.sort((p1, p2) -> Integer.compare(p1.getContadorTU(), p2.getContadorTU()));

		int index = sorted.indexOf(p);
		if (index == -1) return;

		int targetIndex = index + direcao;
		if (targetIndex < 0 || targetIndex >= sorted.size()) return;

		int novoTU;
		if (targetIndex == 0) {
			novoTU = Math.max(0, sorted.get(0).getContadorTU() - 10);
		} else if (targetIndex == sorted.size() - 1) {
			novoTU = sorted.get(sorted.size() - 1).getContadorTU() + 10;
		} else {
			if (direcao < 0) {
				int prevTU = sorted.get(targetIndex - 1).getContadorTU();
				int nextTU = sorted.get(targetIndex).getContadorTU();
				novoTU = (prevTU + nextTU) / 2;
			} else {
				int prevTU = sorted.get(targetIndex).getContadorTU();
				int nextTU = sorted.get(targetIndex + 1).getContadorTU();
				novoTU = (prevTU + nextTU) / 2;
			}
		}

		p.setContadorTU(novoTU);
		resolverColisoesTU();
		autoSaveAndRefresh();

		listaTurnos.getSelectionModel().select(p);
	}

	private void resolverColisoesTU() {
		if (estadoCombate == null) return;
		List<Personagem> sorted = new ArrayList<>(estadoCombate.getCombatentes());
		sorted.sort((p1, p2) -> Integer.compare(p1.getContadorTU(), p2.getContadorTU()));

		for (int i = 1; i < sorted.size(); i++) {
			Personagem prev = sorted.get(i - 1);
			Personagem curr = sorted.get(i);
			if (curr.getContadorTU() <= prev.getContadorTU()) {
				curr.setContadorTU(prev.getContadorTU() + 1);
			}
		}
	}

	@FXML
	private void onCuraRapidaClick() {
		if (selecionado == null) return;
		try {
			double valor = parseDoubleSeguro(inputValorRapido.getText());
			if (valor <= 0) return;
			selecionado.curarIgnorandoBloqueios(valor, estadoCombate, mainController);
			inputValorRapido.clear();
			autoSaveAndRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onDanoRapidoClick() {
		if (selecionado == null || mainController == null) return;
		try {
			double valor = parseDoubleSeguro(inputValorRapido.getText());
			if (valor <= 0) return;

			// Cria um atacante temporário da facção oposta para mecânica de reações/vampirismo
			Personagem atacante = new Personagem();
			if ("JOGADOR".equalsIgnoreCase(selecionado.getFaccao())) {
				atacante.setFaccao("INIMIGO");
			} else {
				atacante.setFaccao("JOGADOR");
			}
			atacante.setNome("Dano GM");

			mainController.getCombatManager().getDamageApplicator().aplicarDanoAoAlvo(
				atacante, selecionado, valor, false, TipoAcao.AMBIENTE, estadoCombate
			);

			inputValorRapido.clear();
			autoSaveAndRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onSubtrairXPClick() {
		if (selecionado == null) return;
		try {
			int xp = parseIntSeguro(inputXPManual.getText());
			if (xp <= 0) return;
			selecionado.setXpAtual(Math.max(0, selecionado.getXpAtual() - xp));
			System.out.println("GM: Subtraído " + xp + " XP de " + selecionado.getNome());
			autoSaveAndRefresh();
			inputXPManual.clear();
		} catch (Exception e) {
			System.err.println("Erro: Valor de XP inválido.");
		}
	}

	@FXML
	private void onSetGrauClick() {
		if (selecionado == null) return;
		try {
			int grau = parseIntSeguro(inputGrau.getText());
			selecionado.setGrau(grau);
			System.out.println("GM: Grau definido para " + grau + " para " + selecionado.getNome());
			autoSaveAndRefresh();
		} catch (Exception e) {
			System.err.println("Erro: Grau inválido.");
		}
	}

	@FXML
	private void onAddPontosClick() {
		if (selecionado == null) return;
		try {
			int pts = parseIntSeguro(inputPontos.getText());
			if (pts <= 0) return;
			selecionado.setPontosParaDistribuir(selecionado.getPontosParaDistribuir() + pts);
			autoSaveAndRefresh();
			inputPontos.clear();
		} catch (Exception e) {
			System.err.println("Erro: Quantidade de pontos inválida.");
		}
	}

	@FXML
	private void onRemovePontosClick() {
		if (selecionado == null) return;
		try {
			int pts = parseIntSeguro(inputPontos.getText());
			if (pts <= 0) return;
			selecionado.setPontosParaDistribuir(Math.max(0, selecionado.getPontosParaDistribuir() - pts));
			autoSaveAndRefresh();
			inputPontos.clear();
		} catch (Exception e) {
			System.err.println("Erro: Quantidade de pontos inválida.");
		}
	}

	private void atualizarListaContratos() {
		if (listaContratos == null) return;
		if (selecionado == null) {
			listaContratos.getItems().clear();
			return;
		}
		listaContratos.setItems(FXCollections.observableArrayList(selecionado.getContratosDeVida()));
	}

	@FXML
	private void onAdicionarContratoClick() {
		if (selecionado == null) return;
		try {
			String fonte = comboFonteContrato.getValue();
			if (fonte == null || fonte.trim().isEmpty()) {
				fonte = "Custom";
			}
			double valor = parseDoubleSeguro(inputValorContrato.getText());
			if (valor <= 0) return;
			int duracao = parseIntSeguro(inputDuracaoContrato.getText());
			boolean persiste = checkPersisteContrato.isSelected();

			ContratoDeVida contrato = new ContratoDeVida(fonte, valor, duracao, persiste);
			ContratoDeVidaUtils.adicionarContrato(selecionado, contrato);

			inputValorContrato.clear();
			inputDuracaoContrato.setText("-1");
			autoSaveAndRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onRemoverContratoClick() {
		if (selecionado == null) return;
		try {
			ContratoDeVida contrato = listaContratos.getSelectionModel().getSelectedItem();
			if (contrato == null) return;

			selecionado.getContratosDeVida().remove(contrato);
			if (contrato.isBarbaro()) {
				selecionado.removerEfeito("Raiva Imparável");
			}
			ContratoDeVidaUtils.pagarDivida(selecionado, 0.0);

			autoSaveAndRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
