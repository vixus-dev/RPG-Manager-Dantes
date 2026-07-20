package br.com.dantesrpg.controller.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoAndarParty;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.KnockbackResult;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * Coordena as regras ambientais dos andares sem acoplar o loop de TU do modelo
 * aos componentes JavaFX usados pelo mestre.
 */
public class EfeitosAndarService {

	private static final int INTERVALO_VENTOS_TU = 300;
	private static final int INTERVALO_OLHO_TU = 300;
	private static final int INTERVALO_CORAL_TU = 200;
	private static final int INTERVALO_PRESENCA_TU = 200;
	private static final int INTERVALO_TEMPESTADE_TU = 150;
	private static final int INTERVALO_PESO_TU = 50;
	private static final int INTERVALO_CIDADE_TU = 100;
	private static final int INTERVALO_HOLOFOTES_TU = 125;
	private static final int DISTANCIA_VENTOS_TILES = 5;
	private static final double DANO_COLISAO_POR_TILE = 5.0;
	private static final int DURACAO_MALDICAO_CORAL_TU = 300;
	private static final String EFEITO_MALDICAO_CORAL = "Maldição de Coral";
	private static final String EFEITO_PESO_DOS_PECADOS = "Peso dos Pecados";

	private final CombatController controller;
	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<EstadoAndarParty> andarSupplier;
	private final BooleanSupplier efeitoAtivoSupplier;
	private final Map<Personagem, EscudoCidade> escudosCidade = new IdentityHashMap<>();

	private int tuDecorridoNoEfeito;
	private boolean combateEmAndamento;

	public EfeitosAndarService(CombatController controller, Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier, Supplier<EstadoAndarParty> andarSupplier,
			BooleanSupplier efeitoAtivoSupplier) {
		this.controller = controller;
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.andarSupplier = andarSupplier;
		this.efeitoAtivoSupplier = efeitoAtivoSupplier;
	}

	public void processarTick() {
		EstadoCombate estado = estadoSupplier.get();
		CombatManager manager = combatManagerSupplier.get();
		if (estado == null || manager == null || !combateEmAndamento
				|| !efeitoAtivoSupplier.getAsBoolean()) {
			return;
		}

		tuDecorridoNoEfeito++;
		TipoEfeitoAndar tipo = resolverTipoAtual();
		switch (tipo) {
		case VENTOS_INFERNAIS -> executarSeChegou(INTERVALO_VENTOS_TU,
				() -> abrirVentosInfernais(estado, manager));
		case OLHO_DA_GULA -> executarSeChegou(INTERVALO_OLHO_TU,
				() -> abrirOlhoDaGula(estado, manager));
		case GANANCIA_DIA -> executarSeChegou(35, () -> aplicarSolDaGanancia(estado, manager, 2.5));
		case GANANCIA_NOITE -> executarSeChegou(100, () -> aplicarVentosGelados(estado));
		case GANANCIA_ECLIPSE -> executarSeChegou(30, () -> aplicarSolDaGanancia(estado, manager, 1.3));
		case INTOXICACAO_CORAL -> executarSeChegou(INTERVALO_CORAL_TU,
				() -> abrirIntoxicacaoDeCoral(estado));
		case RADAR_PRESENCA -> executarSeChegou(INTERVALO_PRESENCA_TU,
				() -> abrirAvisoPresenca("Radar de Presença"));
		case TEMPESTADE_INFINITA -> {
			executarSeChegou(INTERVALO_TEMPESTADE_TU, () -> abrirTempestadeInfinita(estado, manager));
			executarSeChegou(INTERVALO_CORAL_TU, () -> abrirIntoxicacaoDeCoral(estado));
		}
		case PESO_DOS_PECADOS -> executarSeChegou(INTERVALO_PESO_TU,
				() -> aplicarPesoDosPecados(estado));
		case CIDADE_DE_DIZ -> executarSeChegou(INTERVALO_CIDADE_TU,
				() -> concederEscudoPeriodicoCidade(estado));
		case HOLOFOTES -> executarSeChegou(INTERVALO_HOLOFOTES_TU,
				() -> abrirAvisoPresenca("Holofotes"));
		case WAR -> executarSeChegou(INTERVALO_HOLOFOTES_TU,
				() -> abrirAvisoPresenca("War..."));
		case NENHUM -> {
			// O andar selecionado não possui efeito mecânico.
		}
		}

	}

	public void aoAlterarConfiguracao() {
		tuDecorridoNoEfeito = 0;
		limparEfeitosTransitorios();
		removerEscudosCidade();
		atualizarSobreposicaoAgua();
	}

	public void aoIniciarCombate() {
		combateEmAndamento = true;
		tuDecorridoNoEfeito = 0;
		limparEfeitosTransitorios();
		removerEscudosCidade();
		atualizarSobreposicaoAgua();
		if (efeitoAtivoSupplier.getAsBoolean() && resolverTipoAtual() == TipoEfeitoAndar.CIDADE_DE_DIZ) {
			concederEscudoInicialCidade(estadoSupplier.get());
		}
	}

	public void aoEncerrarCombate() {
		combateEmAndamento = false;
		tuDecorridoNoEfeito = 0;
		limparEfeitosTransitorios();
		removerEscudosCidade();
		controller.forEachMap(map -> map.setSobreposicaoAguaTempestadeAtiva(false));
	}

	public boolean isSobreposicaoAguaAtiva() {
		return combateEmAndamento && efeitoAtivoSupplier.getAsBoolean()
				&& resolverTipoAtual() == TipoEfeitoAndar.TEMPESTADE_INFINITA;
	}

	public Optional<ContadorEfeitoAndar> getContadorAtual() {
		if (!efeitoAtivoSupplier.getAsBoolean()) {
			return Optional.empty();
		}
		return switch (resolverTipoAtual()) {
		case VENTOS_INFERNAIS -> contador("Ventos Infernais", INTERVALO_VENTOS_TU);
		case OLHO_DA_GULA -> contador("Olho da Gula", INTERVALO_OLHO_TU);
		case GANANCIA_DIA -> contador("Sol", 35);
		case GANANCIA_NOITE -> contador("Ventos Gelados", 100);
		case GANANCIA_ECLIPSE -> contador("Sol do Eclipse", 30);
		case INTOXICACAO_CORAL -> contador("Intoxicação de Coral", INTERVALO_CORAL_TU);
		case RADAR_PRESENCA -> contador("Radar de Presença", INTERVALO_PRESENCA_TU);
		case TEMPESTADE_INFINITA -> contadorTempestade();
		case PESO_DOS_PECADOS -> contador("Peso dos Pecados", INTERVALO_PESO_TU);
		case CIDADE_DE_DIZ -> contador(":)", INTERVALO_CIDADE_TU);
		case HOLOFOTES -> contador("Holofotes", INTERVALO_HOLOFOTES_TU);
		case WAR -> contador("War...", INTERVALO_HOLOFOTES_TU);
		case NENHUM -> Optional.empty();
		};
	}

	private Optional<ContadorEfeitoAndar> contador(String nome, int intervalo) {
		return Optional.of(new ContadorEfeitoAndar(nome, calcularTURestante(intervalo)));
	}

	private Optional<ContadorEfeitoAndar> contadorTempestade() {
		int restanteRaio = calcularTURestante(INTERVALO_TEMPESTADE_TU);
		int restanteCoral = calcularTURestante(INTERVALO_CORAL_TU);
		return restanteRaio <= restanteCoral
				? Optional.of(new ContadorEfeitoAndar("Relâmpagos", restanteRaio))
				: Optional.of(new ContadorEfeitoAndar("Intoxicação de Coral", restanteCoral));
	}

	private int calcularTURestante(int intervalo) {
		if (tuDecorridoNoEfeito == 0) {
			return intervalo;
		}
		int resto = tuDecorridoNoEfeito % intervalo;
		return resto == 0 ? 0 : intervalo - resto;
	}

	private void executarSeChegou(int intervalo, Runnable acao) {
		if (tuDecorridoNoEfeito > 0 && tuDecorridoNoEfeito % intervalo == 0) {
			acao.run();
		}
	}

	private void abrirVentosInfernais(EstadoCombate estado, CombatManager manager) {
		List<Personagem> ativos = listarCombatentesAtivos(estado);
		if (ativos.isEmpty()) {
			return;
		}

		Dialog<ResultadoVentos> dialog = criarDialogoBase(
				"Ventos Infernais", "Proteja alguns alvos e escolha a direção dos ventos.");
		ButtonType arremessar = new ButtonType("Arremessar", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(arremessar, ButtonType.CANCEL);

		List<CheckBox> seletores = criarSeletoresDeAlvos(ativos);
		VBox lista = new VBox(8);
		lista.getChildren().addAll(seletores);
		ScrollPane rolagem = new ScrollPane(lista);
		rolagem.setFitToWidth(true);
		rolagem.setPrefViewportHeight(Math.min(360, 42 * ativos.size()));

		ToggleGroup grupoDirecao = new ToggleGroup();
		GridPane roda = criarRodaDosVentos(grupoDirecao);
		Label instrucao = new Label("Selecionados: protegidos. Não selecionados: arremessados 5 tiles.");
		instrucao.setWrapText(true);
		VBox conteudo = new VBox(14, instrucao, rolagem, new Label("Roda dos Ventos"), roda);
		conteudo.setPadding(new Insets(8));
		dialog.getDialogPane().setContent(conteudo);

		Node botaoArremessar = dialog.getDialogPane().lookupButton(arremessar);
		botaoArremessar.setDisable(true);
		grupoDirecao.selectedToggleProperty().addListener((obs, anterior, atual) ->
				botaoArremessar.setDisable(atual == null));

		dialog.setResultConverter(botao -> {
			if (botao != arremessar || grupoDirecao.getSelectedToggle() == null) {
				return null;
			}
			List<Personagem> protegidos = seletores.stream().filter(CheckBox::isSelected)
					.map(seletor -> (Personagem) seletor.getUserData()).toList();
			DirecaoVento direcao = (DirecaoVento) grupoDirecao.getSelectedToggle().getUserData();
			return new ResultadoVentos(protegidos, direcao);
		});

		dialog.showAndWait().ifPresent(resultado -> {
			MapController mapa = controller.getPrimaryMap();
			if (mapa == null) {
				System.err.println("ANDAR: Ventos Infernais cancelados porque nenhum mapa está disponível.");
				return;
			}
			for (Personagem alvo : ativos) {
				if (resultado.protegidos().contains(alvo)) {
					continue;
				}
				KnockbackResult empuxo = manager.getKnockbackProcessor().calcularEmpuxoDirecional(
						alvo, resultado.direcao().dx, resultado.direcao().dy,
						DISTANCIA_VENTOS_TILES, mapa);
				manager.getKnockbackProcessor().executarEmpuxo(alvo, empuxo);
				if (empuxo.isColidiu() && empuxo.getColidiuCom() == null) {
					double dano = (DISTANCIA_VENTOS_TILES - empuxo.getDistanciaReal())
							* DANO_COLISAO_POR_TILE;
					aplicarDanoFixoResolvido(manager, estado, alvo, dano);
				}
			}
			controller.atualizarInterfaceTotal();
		});
	}

	private void abrirOlhoDaGula(EstadoCombate estado, CombatManager manager) {
		selecionarVariosAlvos("O Olho da Gula",
				"Selecione quem será observado. Cada alvo recebe 50 de dano fixo e STUN.",
				"Observar", listarCombatentesAtivos(estado)).ifPresent(alvos -> {
			for (Personagem alvo : alvos) {
				aplicarDanoFixo(manager, estado, alvo, 50.0);
				alvo.adicionarEfeito(new Efeito("STUN", TipoEfeito.DEBUFF, 100, null, 0, 0));
			}
			controller.atualizarInterfaceTotal();
		});
	}

	private void aplicarSolDaGanancia(EstadoCombate estado, CombatManager manager, double dano) {
		for (Personagem alvo : listarCombatentesAtivos(estado)) {
			if (!alvo.isProtagonista()) {
				aplicarDanoFixo(manager, estado, alvo, dano);
			}
		}
		controller.atualizarInterfaceTotal();
	}

	private void aplicarVentosGelados(EstadoCombate estado) {
		for (Personagem alvo : listarCombatentesAtivos(estado)) {
			if (!alvo.isProtagonista()) {
				alvo.adicionarEfeito(new Efeito("Vento Congelante", TipoEfeito.DEBUFF, 100, null, 0, 0));
			}
		}
		controller.atualizarInterfaceTotal();
	}

	private void abrirIntoxicacaoDeCoral(EstadoCombate estado) {
		escolherUmAlvo("Intoxicação por Coral",
				"Escolha o alvo que receberá a Maldição de Coral por 300 TU.",
				listarCombatentesAtivos(estado)).ifPresent(alvo -> {
			List<Habilidade> candidatas = listarHabilidadesBloqueaveis(alvo);
			if (candidatas.isEmpty()) {
				aplicarMaldicaoDeCoral(alvo, null);
				return;
			}
			escolherHabilidadeParaBloquear(alvo, candidatas)
					.ifPresent(habilidade -> aplicarMaldicaoDeCoral(alvo, habilidade));
		});
	}

	private List<Habilidade> listarHabilidadesBloqueaveis(Personagem alvo) {
		return alvo.getHabilidadesDeClasse().stream()
				.filter(habilidade -> habilidade != null && habilidade.getTipo() == TipoHabilidade.ATIVA)
				.filter(habilidade -> habilidade.getNome() != null
						&& !habilidade.getNome().equalsIgnoreCase("Ataque Básico"))
				.collect(LinkedHashMap<String, Habilidade>::new,
						(mapa, habilidade) -> mapa.putIfAbsent(
								habilidade.getNome().toLowerCase(Locale.ROOT), habilidade),
						Map::putAll)
				.values().stream().toList();
	}

	private void aplicarMaldicaoDeCoral(Personagem alvo, Habilidade bloqueada) {
		Efeito maldicao = new Efeito(EFEITO_MALDICAO_CORAL, TipoEfeito.DEBUFF,
				DURACAO_MALDICAO_CORAL_TU, null, 0, 0);
		alvo.adicionarEfeito(maldicao);
		Efeito efeitoAplicado = alvo.getEfeitosAtivos().get(EFEITO_MALDICAO_CORAL);
		if (efeitoAplicado != null) {
			efeitoAplicado.setDuracaoTUInicial(DURACAO_MALDICAO_CORAL_TU);
			efeitoAplicado.setDuracaoTURestante(DURACAO_MALDICAO_CORAL_TU);
		}

		if (bloqueada == null) {
			alvo.limparHabilidadeBloqueadaPorCoral();
			System.out.println(">>> CORAL: " + alvo.getNome() + " não possui habilidade ativa bloqueável.");
		} else {
			alvo.bloquearHabilidadePorCoral(bloqueada.getNome());
			System.out.println(">>> CORAL: " + alvo.getNome() + " teve [" + bloqueada.getNome()
					+ "] bloqueada por 300 TU.");
		}
		controller.atualizarInterfaceTotal();
	}

	private void abrirTempestadeInfinita(EstadoCombate estado, CombatManager manager) {
		selecionarVariosAlvos("Tempestade Infinita",
				"Selecione os personagens que receberão 20 de dano, Choque e Queimação.",
				"Relampejar", listarCombatentesAtivos(estado)).ifPresent(alvos -> {
			for (Personagem alvo : alvos) {
				aplicarDanoFixoResolvido(manager, estado, alvo, 20.0);
				alvo.adicionarEfeito(EffectFactory.criarEfeito("Choque", 1, 20));
				Efeito queimacao = new Efeito("Queimação", TipoEfeito.DOT, 200, null, 5, 100);
				alvo.adicionarEfeito(queimacao);
				Efeito aplicada = alvo.getEfeitosAtivos().get("Queimação");
				if (aplicada != null) {
					aplicada.setDuracaoTUInicial(200);
					aplicada.setDuracaoTURestante(200);
					aplicada.setDanoPorTick(5);
					aplicada.setIntervaloTickTU(100);
				}
			}
			controller.atualizarInterfaceTotal();
		});
	}

	private void aplicarPesoDosPecados(EstadoCombate estado) {
		for (Personagem jogador : listarJogadoresAtivos(estado)) {
			int pecado = jogador.getPecado();
			jogador.setContadorTU(jogador.getContadorTU() + pecado);
			jogador.removerEfeito(EFEITO_PESO_DOS_PECADOS);
			if (pecado > 0) {
				Map<String, Double> modificadores = Map.of("REDUCAO_DANO_MODIFICADOR", -(pecado / 100.0));
				jogador.adicionarEfeito(new Efeito(EFEITO_PESO_DOS_PECADOS, TipoEfeito.DEBUFF,
						Integer.MAX_VALUE / 4, modificadores, 0, 0));
			}
			System.out.println(">>> PESO DOS PECADOS: " + jogador.getNome() + " recebeu +" + pecado
					+ " TU e -" + pecado + "% de redução de dano.");
		}
		controller.atualizarInterfaceTotal();
	}

	private void concederEscudoInicialCidade(EstadoCombate estado) {
		if (estado == null) {
			return;
		}
		for (Personagem jogador : listarJogadoresAtivos(estado)) {
			registrarEscudoCidade(jogador, jogador.getVidaMaxima());
		}
		controller.atualizarInterfaceTotal();
	}

	private void concederEscudoPeriodicoCidade(EstadoCombate estado) {
		for (Personagem jogador : listarJogadoresAtivos(estado)) {
			registrarEscudoCidade(jogador, jogador.getPecado());
		}
		controller.atualizarInterfaceTotal();
	}

	private void registrarEscudoCidade(Personagem jogador, double valor) {
		if (valor <= 0) {
			return;
		}
		EscudoCidade atual = escudosCidade.computeIfAbsent(jogador,
				p -> new EscudoCidade(p.getEscudoSangueAtual(), 0));
		jogador.adicionarEscudoSangue(valor);
		escudosCidade.put(jogador, new EscudoCidade(atual.escudoAnterior(), atual.valorConcedido() + valor));
	}

	private void removerEscudosCidade() {
		for (Map.Entry<Personagem, EscudoCidade> entrada : new ArrayList<>(escudosCidade.entrySet())) {
			Personagem jogador = entrada.getKey();
			EscudoCidade registro = entrada.getValue();
			double excedenteAtual = Math.max(0, jogador.getEscudoSangueAtual() - registro.escudoAnterior());
			double valorARemover = Math.min(registro.valorConcedido(), excedenteAtual);
			if (valorARemover > 0) {
				jogador.setEscudoSangueAtual(jogador.getEscudoSangueAtual() - valorARemover);
			}
		}
		escudosCidade.clear();
	}

	private void abrirAvisoPresenca(String nomeEfeito) {
		Alert alerta = new Alert(Alert.AlertType.WARNING);
		alerta.setTitle(nomeEfeito);
		alerta.setHeaderText("Realize a presença dos jogadores");
		alerta.setContentText("Confira a presença agora. Caso alguém falhe, adicione manualmente os novos inimigos.");
		alerta.getButtonTypes().setAll(new ButtonType("Presença realizada", ButtonData.OK_DONE));
		prepararDialogo(alerta);
		alerta.showAndWait();
	}

	private Optional<List<Personagem>> selecionarVariosAlvos(String titulo, String cabecalho,
			String textoConfirmacao, List<Personagem> candidatos) {
		if (candidatos.isEmpty()) {
			return Optional.empty();
		}
		Dialog<List<Personagem>> dialog = criarDialogoBase(titulo, cabecalho);
		ButtonType confirmar = new ButtonType(textoConfirmacao, ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(confirmar, ButtonType.CANCEL);
		List<CheckBox> seletores = criarSeletoresDeAlvos(candidatos);
		VBox lista = new VBox(8);
		lista.setPadding(new Insets(8));
		lista.getChildren().addAll(seletores);
		ScrollPane rolagem = new ScrollPane(lista);
		rolagem.setFitToWidth(true);
		rolagem.setPrefViewportHeight(Math.min(420, 42 * candidatos.size()));
		dialog.getDialogPane().setContent(rolagem);
		dialog.setResultConverter(botao -> botao == confirmar
				? seletores.stream().filter(CheckBox::isSelected)
						.map(seletor -> (Personagem) seletor.getUserData()).toList()
				: null);
		return dialog.showAndWait();
	}

	private Optional<Personagem> escolherUmAlvo(String titulo, String cabecalho, List<Personagem> candidatos) {
		if (candidatos.isEmpty()) {
			return Optional.empty();
		}
		Dialog<Personagem> dialog = criarDialogoBase(titulo, cabecalho);
		ButtonType aplicar = new ButtonType("Escolher alvo", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(aplicar, ButtonType.CANCEL);

		ListView<Personagem> lista = new ListView<>();
		lista.getItems().setAll(candidatos);
		lista.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		lista.setPrefHeight(Math.min(420, 46 * candidatos.size()));
		lista.setCellFactory(view -> new ListCell<>() {
			@Override
			protected void updateItem(Personagem personagem, boolean vazio) {
				super.updateItem(personagem, vazio);
				setText(vazio || personagem == null ? null : textoDoAlvo(personagem));
			}
		});
		dialog.getDialogPane().setContent(lista);
		Node botaoAplicar = dialog.getDialogPane().lookupButton(aplicar);
		botaoAplicar.setDisable(true);
		lista.getSelectionModel().selectedItemProperty().addListener((obs, anterior, atual) ->
				botaoAplicar.setDisable(atual == null));
		dialog.setResultConverter(botao -> botao == aplicar
				? lista.getSelectionModel().getSelectedItem() : null);
		return dialog.showAndWait();
	}

	private Optional<Habilidade> escolherHabilidadeParaBloquear(Personagem alvo,
			List<Habilidade> candidatas) {
		Dialog<Habilidade> dialog = criarDialogoBase("Maldição de Coral — " + alvo.getNome(),
				"Escolha qual habilidade será bloqueada por 300 TU.");
		ButtonType bloquear = new ButtonType("Bloquear habilidade", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(bloquear, ButtonType.CANCEL);

		ListView<Habilidade> lista = new ListView<>();
		lista.getItems().setAll(candidatas);
		lista.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		lista.setPrefHeight(Math.min(420, 46 * candidatas.size()));
		lista.setCellFactory(view -> new ListCell<>() {
			@Override
			protected void updateItem(Habilidade habilidade, boolean vazio) {
				super.updateItem(habilidade, vazio);
				setText(vazio || habilidade == null ? null
						: habilidade.getNome() + " — " + habilidade.getCustoMana()
								+ " MP / " + habilidade.getCustoTU() + " TU");
			}
		});
		dialog.getDialogPane().setContent(lista);
		Node botaoBloquear = dialog.getDialogPane().lookupButton(bloquear);
		botaoBloquear.setDisable(true);
		lista.getSelectionModel().selectedItemProperty().addListener((obs, anterior, atual) ->
				botaoBloquear.setDisable(atual == null));
		dialog.setResultConverter(botao -> botao == bloquear
				? lista.getSelectionModel().getSelectedItem() : null);
		return dialog.showAndWait();
	}

	private List<CheckBox> criarSeletoresDeAlvos(List<Personagem> personagens) {
		List<CheckBox> seletores = new ArrayList<>();
		for (Personagem personagem : personagens) {
			CheckBox seletor = new CheckBox(textoDoAlvo(personagem));
			seletor.setUserData(personagem);
			seletor.setMaxWidth(Double.MAX_VALUE);
			seletores.add(seletor);
		}
		return seletores;
	}

	private GridPane criarRodaDosVentos(ToggleGroup grupo) {
		GridPane roda = new GridPane();
		roda.setAlignment(Pos.CENTER);
		roda.setHgap(7);
		roda.setVgap(7);
		for (DirecaoVento direcao : DirecaoVento.values()) {
			ToggleButton botao = new ToggleButton(direcao.rotulo);
			botao.setToggleGroup(grupo);
			botao.setUserData(direcao);
			botao.setMinSize(72, 48);
			roda.add(botao, direcao.dx + 1, direcao.dy + 1);
		}
		Label centro = new Label("VENTOS");
		centro.setAlignment(Pos.CENTER);
		centro.setMinSize(72, 48);
		roda.add(centro, 1, 1);
		return roda;
	}

	private <T> Dialog<T> criarDialogoBase(String titulo, String cabecalho) {
		Dialog<T> dialog = new Dialog<>();
		dialog.setTitle(titulo);
		dialog.setHeaderText(cabecalho);
		prepararDialogo(dialog);
		return dialog;
	}

	private void prepararDialogo(Dialog<?> dialog) {
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.getDialogPane().setMinWidth(520);
		controller.aplicarTemaEmRaiz(dialog.getDialogPane());
		dialog.setOnHidden(evento -> controller.removerTemaDeRaiz(dialog.getDialogPane()));
		dialog.setOnShown(evento -> Platform.runLater(() -> posicionarNoPrimeiroMonitor(
				dialog.getDialogPane().getScene().getWindow())));
	}

	private void posicionarNoPrimeiroMonitor(Window janela) {
		if (janela == null || Screen.getScreens().isEmpty()) {
			return;
		}
		Rectangle2D limites = Screen.getScreens().get(0).getVisualBounds();
		janela.setX(limites.getMinX() + (limites.getWidth() - janela.getWidth()) / 2.0);
		janela.setY(limites.getMinY() + (limites.getHeight() - janela.getHeight()) / 2.0);
	}

	private String textoDoAlvo(Personagem personagem) {
		String lado = controller.isPlayer(personagem) ? "Jogador" : "Inimigo";
		return personagem.getNome() + " — " + lado + " — HP " + (int) personagem.getVidaAtual()
				+ "/" + (int) personagem.getVidaMaxima();
	}

	private List<Personagem> listarCombatentesAtivos(EstadoCombate estado) {
		return estado.getCombatentes().stream().filter(Personagem::isAtivoNoCombate)
				.sorted(Comparator.comparing(Personagem::getNome, String.CASE_INSENSITIVE_ORDER)).toList();
	}

	private List<Personagem> listarJogadoresAtivos(EstadoCombate estado) {
		return listarCombatentesAtivos(estado).stream().filter(controller::isPlayer).toList();
	}

	private void aplicarDanoFixo(CombatManager manager, EstadoCombate estado, Personagem alvo, double dano) {
		if (dano > 0 && alvo.isAtivoNoCombate()) {
			manager.getDamageApplicator().aplicarDanoAoAlvo(null, alvo, dano, true, TipoAcao.AMBIENTE, estado);
		}
	}

	private void aplicarDanoFixoResolvido(CombatManager manager, EstadoCombate estado,
			Personagem alvo, double dano) {
		if (dano > 0 && alvo.isAtivoNoCombate()) {
			manager.getDamageApplicator().aplicarDanoAoAlvoResolvido(
					null, alvo, dano, true, TipoAcao.AMBIENTE, estado);
		}
	}

	private void limparEfeitosTransitorios() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}
		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem == null) {
				continue;
			}
			personagem.removerEfeito(EFEITO_MALDICAO_CORAL);
			personagem.limparHabilidadeBloqueadaPorCoral();
			personagem.removerEfeito(EFEITO_PESO_DOS_PECADOS);
		}
	}

	private void atualizarSobreposicaoAgua() {
		boolean ativa = isSobreposicaoAguaAtiva();
		controller.forEachMap(map -> map.setSobreposicaoAguaTempestadeAtiva(ativa));
	}

	private TipoEfeitoAndar resolverTipoAtual() {
		EstadoAndarParty estado = andarSupplier.get();
		if (estado == null) {
			return TipoEfeitoAndar.NENHUM;
		}
		return switch (estado.andar()) {
		case ANDAR_2 -> TipoEfeitoAndar.VENTOS_INFERNAIS;
		case ANDAR_3 -> TipoEfeitoAndar.OLHO_DA_GULA;
		case ANDAR_4 -> switch (estado.estadoVisual()) {
			case 1 -> TipoEfeitoAndar.GANANCIA_DIA;
			case 2 -> TipoEfeitoAndar.GANANCIA_NOITE;
			default -> TipoEfeitoAndar.GANANCIA_ECLIPSE;
		};
		case ANDAR_5 -> switch (estado.estadoVisual()) {
			case 1 -> TipoEfeitoAndar.INTOXICACAO_CORAL;
			case 3 -> TipoEfeitoAndar.RADAR_PRESENCA;
			default -> TipoEfeitoAndar.TEMPESTADE_INFINITA;
		};
		case ANDAR_6 -> estado.estadoVisual() == 1
				? TipoEfeitoAndar.PESO_DOS_PECADOS : TipoEfeitoAndar.CIDADE_DE_DIZ;
		case ANDAR_7 -> switch (estado.estadoVisual()) {
			case 1 -> TipoEfeitoAndar.HOLOFOTES;
			case 3 -> TipoEfeitoAndar.WAR;
			default -> TipoEfeitoAndar.NENHUM;
		};
		case NULO, ANDAR_1, ANDAR_8, ANDAR_9 -> TipoEfeitoAndar.NENHUM;
		};
	}

	public record ContadorEfeitoAndar(String nome, int tuRestante) {
	}

	private record ResultadoVentos(List<Personagem> protegidos, DirecaoVento direcao) {
	}

	private record EscudoCidade(double escudoAnterior, double valorConcedido) {
	}

	private enum DirecaoVento {
		NOROESTE(-1, -1, "NO"), NORTE(0, -1, "N"), NORDESTE(1, -1, "NE"),
		OESTE(-1, 0, "O"), LESTE(1, 0, "L"),
		SUDOESTE(-1, 1, "SO"), SUL(0, 1, "S"), SUDESTE(1, 1, "SE");

		private final int dx;
		private final int dy;
		private final String rotulo;

		DirecaoVento(int dx, int dy, String rotulo) {
			this.dx = dx;
			this.dy = dy;
			this.rotulo = rotulo;
		}
	}

	private enum TipoEfeitoAndar {
		NENHUM,
		VENTOS_INFERNAIS,
		OLHO_DA_GULA,
		GANANCIA_DIA,
		GANANCIA_NOITE,
		GANANCIA_ECLIPSE,
		INTOXICACAO_CORAL,
		RADAR_PRESENCA,
		TEMPESTADE_INFINITA,
		PESO_DOS_PECADOS,
		CIDADE_DE_DIZ,
		HOLOFOTES,
		WAR
	}
}
