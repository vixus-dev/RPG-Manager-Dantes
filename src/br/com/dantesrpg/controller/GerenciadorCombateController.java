package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.util.EffectFactory;
import br.com.dantesrpg.model.Efeito;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GerenciadorCombateController {

	@FXML
	private TableView<Personagem> tabelaCombatentes;
	@FXML
	private TableColumn<Personagem, String> colNome;
	@FXML
	private TableColumn<Personagem, String> colHP;
	@FXML
	private TableColumn<Personagem, String> colTU;
	@FXML
	private TableColumn<Personagem, String> colFaccao;
	@FXML
	private TableColumn<Personagem, String> colStatus;

	@FXML
	private TabPane tabPaneDetalhes;
	@FXML
	private Label lblNomeSelecionado;

	// Aba Principal
	@FXML
	private TextField inputVida;
	@FXML
	private TextField inputEscudo;
	@FXML
	private TextField inputMana;
	@FXML
	private TextField inputTU;
	@FXML
	private CheckBox checkAusente;
	@FXML
	private CheckBox checkProtagonista;
	@FXML
	private TextField inputOuro;
	@FXML
	private TextField inputPrata;
	@FXML
	private TextField inputBronze;
	@FXML
	private TextField inputXPManual;

	// Aba Inventário
	@FXML
	private TextField inputBuscaItem;
	@FXML
	private ListView<String> listaItensFiltrados;
	@FXML
	private TextField inputQtdItem;
	@FXML
	private ListView<String> listaInventarioAtual;

	// Aba Efeitos
	@FXML
	private ComboBox<String> comboPresetEfeito;
	@FXML
	private TextField inputDuracaoEfeito;
	@FXML
	private TextField inputValorEfeito;

	// Global
	@FXML
	private ComboBox<String> comboEfeitoAndar;
	@FXML
	private CheckBox checkEfeitoAndarAtivo;

	private CombatController mainController;
	private EstadoCombate estadoCombate;
	private Personagem selecionado;
	private ObservableList<String> masterDataItens = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		configurarColunasTabela();
		configurarFiltroItens();
		configurarEfeitosAndar();

		// Inicializa combo de efeitos da Factory
		comboPresetEfeito.getItems().setAll(EffectFactory.getNomesPresets());
		comboPresetEfeito.getSelectionModel().selectFirst();

		tabelaCombatentes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			selecionarPersonagem(newVal);
		});

		tabPaneDetalhes.setDisable(true);
	}

	@FXML
	private void onAdicionarXPClick() {
		if (selecionado == null)
			return;
		try {
			int xp = Integer.parseInt(inputXPManual.getText());
			selecionado.ganharExperiencia(xp);

			// Feedback
			System.out.println("GM: Adicionado " + xp + " XP para " + selecionado.getNome());

			// Salva e Atualiza tudo (incluindo nível se upar)
			autoSaveAndRefresh();
			inputXPManual.clear();

		} catch (NumberFormatException e) {
			System.err.println("Erro: Valor de XP inválido.");
		}
	}

	public void setMainController(CombatController mainController, EstadoCombate estado) {
		this.mainController = mainController;
		this.estadoCombate = estado;
		carregarDados();
	}

	public void setListaItensMestre(List<String> itens) {
		this.masterDataItens.clear();
		if (itens != null)
			this.masterDataItens.addAll(itens);
	}

	// --- MÉTODO DE AUTO-SAVE E REFRESH ---
	private void autoSaveAndRefresh() {
		if (mainController != null) {
			mainController.atualizarInterfaceTotal();
			mainController.salvarEstadoJogadores();
			System.out.println("GM: Auto-Save executado.");
		}
		tabelaCombatentes.refresh();
	}
	
	// --- AÇÕES DA ABA PRINCIPAL ---
	@FXML
	private void onAplicarStatusClick() {
		if (selecionado == null)
			return;
		try {
			selecionado.setVidaAtual(Double.parseDouble(inputVida.getText()), estadoCombate, mainController);
			selecionado.setEscudoAtual(Double.parseDouble(inputEscudo.getText())); // NOVO: Salva Escudo
			selecionado.setManaAtual(Double.parseDouble(inputMana.getText()));
			selecionado.setContadorTU(Integer.parseInt(inputTU.getText()));
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

	@FXML
	private void onSetarMoedasClick() {
		if (selecionado == null)
			return;
		try {
			selecionado.getInventario().setMoedasOuro(Integer.parseInt(inputOuro.getText()));
			selecionado.getInventario().setMoedasPrata(Integer.parseInt(inputPrata.getText()));
			selecionado.getInventario().setMoedasBronze(Integer.parseInt(inputBronze.getText()));
			autoSaveAndRefresh();
		} catch (Exception e) {
		}
	}

	// --- AÇÕES DA ABA INVENTÁRIO ---

	@FXML
	private void onAdicionarItemClick() {
		String nomeItem = listaItensFiltrados.getSelectionModel().getSelectedItem();
		if (selecionado != null && nomeItem != null && mainController != null) {
			try {
				int qtd = Integer.parseInt(inputQtdItem.getText());
				Item item = mainController.getItem(nomeItem);
				if (item != null) {
					for (int i = 0; i < qtd; i++)
						selecionado.getInventario().adicionarItem(item);
					System.out.println("GM: Adicionado " + nomeItem);
					atualizarListaInventario();
					autoSaveAndRefresh();
				}
			} catch (Exception e) {
			}
		}
	}

	@FXML
	private void onRemoverItemClick() {
		String selectedString = listaInventarioAtual.getSelectionModel().getSelectedItem();
		if (selecionado != null && selectedString != null && mainController != null) {
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

	private void atualizarListaInventario() {
		listaInventarioAtual.getItems().clear();
		if (selecionado != null && selecionado.getInventario() != null) {
			Map<String, Integer> itens = selecionado.getInventario().getItensAgrupados();

			// Popula com os IDs dos itens
			listaInventarioAtual.getItems().addAll(itens.keySet());

			// Configura a célula para mostrar "Nome (xQtd)"
			listaInventarioAtual.setCellFactory(lv -> new ListCell<String>() {
				@Override
				protected void updateItem(String tipoItem, boolean empty) {
					super.updateItem(tipoItem, empty);
					if (empty || tipoItem == null) {
						setText(null);
					} else {
						int qtd = itens.getOrDefault(tipoItem, 0);
						// Tenta pegar o nome bonito
						Item modelo = mainController.getItem(tipoItem);
						String nomeExibicao = (modelo != null) ? modelo.getNome() : tipoItem;
						setText(nomeExibicao + " (x" + qtd + ")");
					}
				}
			});
		}
	}

	// --- AÇÕES DA ABA EFEITOS ---

	@FXML
	private void onAdicionarEfeitoManualClick() {
		if (selecionado == null)
			return;
		try {
			String nomePreset = comboPresetEfeito.getValue();
			int duracao = Integer.parseInt(inputDuracaoEfeito.getText());
			int valor = Integer.parseInt(inputValorEfeito.getText());

			Efeito novoEfeito = EffectFactory.criarEfeito(nomePreset, duracao, valor);
			selecionado.adicionarEfeito(novoEfeito);
			selecionado.recalcularAtributosEstatisticas();

			autoSaveAndRefresh();
		} catch (Exception e) {
		}
	}

	@FXML
	private void onLimparEfeitosClick() {
		if (selecionado == null)
			return;
		List<String> nomes = new ArrayList<>(selecionado.getEfeitosAtivos().keySet());
		for (String n : nomes)
			selecionado.removerEfeito(n);
		selecionado.recalcularAtributosEstatisticas();
		autoSaveAndRefresh();
	}

	// --- AÇÕES GLOBAIS / SISTEMA ---

	@FXML
	private void onSalvarTudoClick() {
		if (mainController != null)
			mainController.salvarEstadoJogadores();
	}

	@FXML
	private void onCarregarTudoClick() {
		if (mainController != null) {
			mainController.carregarEstadoJogadores();
			carregarDados(); // Recarrega a tabela local
		}
	}

	@FXML
	private void onCurarTodosClick() {
		if (estadoCombate == null)
			return;
		for (Personagem p : estadoCombate.getCombatentes()) {
			if (p.getFaccao().equals("JOGADOR") && p.isVivo()) {
				p.setVidaAtual(p.getVidaMaxima(), estadoCombate, mainController);
			}
		}
		atualizarCamposInput();
		autoSaveAndRefresh();
	}

	// --- MÉTODOS AUXILIARES ---

	private void selecionarPersonagem(Personagem p) {
		this.selecionado = p;
		if (p == null) {
			tabPaneDetalhes.setDisable(true);
			lblNomeSelecionado.setText("Selecione um Personagem");
			return;
		}

		tabPaneDetalhes.setDisable(false);
		lblNomeSelecionado.setText(p.getNome());
		atualizarCamposInput();
		atualizarListaInventario();
	}

	private void atualizarCamposInput() {
		if (selecionado == null)
			return;
		inputVida.setText(String.valueOf(selecionado.getVidaAtual()));
		inputEscudo.setText(String.valueOf(selecionado.getEscudoAtual()));
		inputMana.setText(String.valueOf(selecionado.getManaAtual()));
		inputTU.setText(String.valueOf(selecionado.getContadorTU()));
		checkAusente.setSelected(selecionado.isAusente());
		checkProtagonista.setSelected(selecionado.isProtagonista());

		if (selecionado.getInventario() != null) {
			inputOuro.setText(String.valueOf(selecionado.getInventario().getMoedasOuro()));
			inputPrata.setText(String.valueOf(selecionado.getInventario().getMoedasPrata()));
			inputBronze.setText(String.valueOf(selecionado.getInventario().getMoedasBronze()));
		}
	}

	@FXML
	private void atualizarTabela() {
		carregarDados();
	}

	private void carregarDados() {
		if (estadoCombate != null) {
			tabelaCombatentes.getItems().setAll(estadoCombate.getCombatentes());
		}
	}

	private void configurarColunasTabela() {
		colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
		colHP.setCellValueFactory(data -> {
			Personagem p = data.getValue();
			if (p.isProtagonista())
				return new SimpleStringProperty("?");
			return new SimpleStringProperty(String.format("%.1f/%.1f", p.getVidaAtual(), p.getVidaMaxima()));
		});
		colTU.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getContadorTU())));
		colFaccao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFaccao()));
		colStatus.setCellValueFactory(data -> {
			Personagem p = data.getValue();
			if (p.isAusente())
				return new SimpleStringProperty("[AUSENTE]");
			if (!p.isVivo())
				return new SimpleStringProperty("[MORTO]");
			if (p.isProtagonista())
				return new SimpleStringProperty("[PROTAG]");
			return new SimpleStringProperty("OK");
		});
	}

	private void configurarFiltroItens() {
		FilteredList<String> filteredData = new FilteredList<>(masterDataItens, p -> true);
		inputBuscaItem.textProperty().addListener((observable, oldValue, newValue) -> {
			filteredData.setPredicate(item -> {
				if (newValue == null || newValue.isEmpty())
					return true;
				return item.toLowerCase().contains(newValue.toLowerCase());
			});
		});
		listaItensFiltrados.setItems(filteredData);
	}

	private void configurarEfeitosAndar() {
		comboEfeitoAndar.getItems().addAll("Nenhum", "2º Andar - Arremesso", "3º Andar - O Olho", "4º Andar - Dia",
				"4º Andar - Noite", "5º Andar - Tempestade", "7º Andar - Holofotes", "8.1º Andar - Cruzes");
		comboEfeitoAndar.getSelectionModel().selectFirst();

		comboEfeitoAndar.valueProperty().addListener((o, old, newVal) -> {
			if (mainController != null)
				mainController.setEfeitoAndar(newVal, checkEfeitoAndarAtivo.isSelected());
		});
		checkEfeitoAndarAtivo.selectedProperty().addListener((o, old, isSelected) -> {
			if (mainController != null)
				mainController.setEfeitoAndar(comboEfeitoAndar.getValue(), isSelected);
		});
	}
}