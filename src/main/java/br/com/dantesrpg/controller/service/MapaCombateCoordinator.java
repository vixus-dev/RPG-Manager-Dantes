package br.com.dantesrpg.controller.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.elementos.ObjetoDestrutivel;
import br.com.dantesrpg.model.map.Dominio;
import br.com.dantesrpg.model.map.MapMetadata;
import br.com.dantesrpg.model.racas.Anao;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import br.com.dantesrpg.model.map.TileDefinition;
import br.com.dantesrpg.model.util.FileLoader;

public class MapaCombateCoordinator {

	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<MapController> primaryMapSupplier;
	private final Consumer<Consumer<MapController>> forEachMap;
	private final Supplier<File> arquivoMapaAtualSupplier;
	private final Consumer<File> arquivoMapaAtualSetter;
	private final Supplier<Window> ownerWindowSupplier;
	private final Supplier<Button> btnConfirmarMovimentoSupplier;
	private final Supplier<Stage> detailedTurnHudStageSupplier;
	private final Consumer<Personagem> reabrirHudParaAtor;
	private final Runnable popularListasDeCombatentes;
	private final Runnable atualizarTimelineTU;
	private final Runnable limparEstadoSquadTemporario;
	private final Predicate<Personagem> isPlayer;
	private final Consumer<AcaoMestreInput> resolverAcaoPassarVez;

	private boolean movimentoComRetorno;

	public MapaCombateCoordinator(Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier, Supplier<MapController> primaryMapSupplier,
			Consumer<Consumer<MapController>> forEachMap, Supplier<File> arquivoMapaAtualSupplier,
			Consumer<File> arquivoMapaAtualSetter, Supplier<Window> ownerWindowSupplier,
			Supplier<Button> btnConfirmarMovimentoSupplier, Supplier<Stage> detailedTurnHudStageSupplier,
			Consumer<Personagem> reabrirHudParaAtor, Runnable popularListasDeCombatentes,
			Runnable atualizarTimelineTU, Runnable limparEstadoSquadTemporario, Predicate<Personagem> isPlayer,
			Consumer<AcaoMestreInput> resolverAcaoPassarVez) {
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.primaryMapSupplier = primaryMapSupplier;
		this.forEachMap = forEachMap;
		this.arquivoMapaAtualSupplier = arquivoMapaAtualSupplier;
		this.arquivoMapaAtualSetter = arquivoMapaAtualSetter;
		this.ownerWindowSupplier = ownerWindowSupplier;
		this.btnConfirmarMovimentoSupplier = btnConfirmarMovimentoSupplier;
		this.detailedTurnHudStageSupplier = detailedTurnHudStageSupplier;
		this.reabrirHudParaAtor = reabrirHudParaAtor;
		this.popularListasDeCombatentes = popularListasDeCombatentes;
		this.atualizarTimelineTU = atualizarTimelineTU;
		this.limparEstadoSquadTemporario = limparEstadoSquadTemporario;
		this.isPlayer = isPlayer;
		this.resolverAcaoPassarVez = resolverAcaoPassarVez;
	}

	public void carregarArenaComSeletor() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/SeletorMapaView.fxml"));
			Parent root = loader.load();
			
			br.com.dantesrpg.controller.SeletorMapaController controller = loader.getController();
			controller.initData(this);
			
			Stage stage = new Stage();
			stage.setTitle("Seleção de Arena Tática");
			stage.initOwner(ownerWindowSupplier.get());
			stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
			stage.setScene(new Scene(root));
			stage.show();
		} catch (Exception e) {
			System.err.println("Erro ao abrir SeletorMapaView.fxml:");
			e.printStackTrace();
		}
	}

	public void carregarArenaProcedural(TileDefinition[][] matriz) {

EstadoCombate estado = estadoSupplier.get();
		encerrarEmprestimosOvertime();
		encerrarContratosBarbaros();
		limparClonesDoCombate();
		
		arquivoMapaAtualSetter.accept(null); // Sem arquivo associado
		
		forEachMap.accept(m -> m.carregarMapaProcedural(matriz));
		
		estado.resetarIniciativa();

		for (Personagem personagem : estado.getCombatentes()) {
			personagem.setMovimentoRestanteTurno(personagem.getMovimento());
		}

		popularListasDeCombatentes.run();
		atualizarTimelineTU.run();
		System.out.println("NOVA ARENA PROCEDURAL CARREGADA COM SUCESSO.");
	}

	private File resolverDiretorioInicialMapas(String mapPath) {
		File initialDirectory = new File(mapPath);
		if (!initialDirectory.exists() || !initialDirectory.isDirectory()) {
			System.err.println("Aviso: Pasta de Mapas não encontrada em " + mapPath + ". Usando diretório padrão.");
			initialDirectory = new File(System.getProperty("user.dir"));
			if (!initialDirectory.exists()) {
				initialDirectory = new File(System.getProperty("user.home"));
			}
		}
		return initialDirectory;
	}

	public void carregarNovaArenaLogic() {
		System.out.println(">>> Viajando para nova área...");
		EstadoCombate estado = estadoSupplier.get();
		encerrarEmprestimosOvertime();
		encerrarContratosBarbaros();
		encerrarPosturasAnao();
		limparClonesDoCombate();
		estado.resetarIniciativa();
		popularListasDeCombatentes.run();
		atualizarTimelineTU.run();
		forEachMap.accept(m -> m.desenharPeoes(estado.getCombatentes()));
	}

	public void carregarNovaArenaLogic(File mapaFile) {

EstadoCombate estado = estadoSupplier.get();
		encerrarEmprestimosOvertime();
		encerrarContratosBarbaros();
		limparClonesDoCombate();
		forEachMap.accept(m -> m.carregarMapaDeImagem(mapaFile));
		estado.resetarIniciativa();

		for (Personagem personagem : estado.getCombatentes()) {
			personagem.setMovimentoRestanteTurno(personagem.getMovimento());
		}

		popularListasDeCombatentes.run();
		atualizarTimelineTU.run();
		System.out.println("NOVA ARENA CARREGADA COM SUCESSO.");
	}

	public void carregarNovaArenaLogic(String caminhoRecurso) {
		EstadoCombate estado = estadoSupplier.get();
		encerrarEmprestimosOvertime();
		encerrarContratosBarbaros();
		limparClonesDoCombate();

		try (InputStream is = FileLoader.carregarArquivo(caminhoRecurso)) {
			if (is != null) {
				String nomeMapa = caminhoRecurso.contains("/") ? caminhoRecurso.substring(caminhoRecurso.lastIndexOf('/') + 1) : caminhoRecurso;
				forEachMap.accept(m -> m.carregarMapaDeImagem(is, nomeMapa));
			} else {
				System.err.println("Erro: Não foi possível carregar a imagem do mapa " + caminhoRecurso);
			}
		} catch (Exception e) {
			System.err.println("Erro ao carregar mapa a partir do recurso: " + e.getMessage());
			e.printStackTrace();
		}

		// Carrega metadados JSON do recurso correspondente
		carregarMetadadosDoMapa(caminhoRecurso);

		estado.resetarIniciativa();

		for (Personagem personagem : estado.getCombatentes()) {
			personagem.setMovimentoRestanteTurno(personagem.getMovimento());
		}

		popularListasDeCombatentes.run();
		atualizarTimelineTU.run();
		System.out.println("NOVA ARENA CARREGADA COM SUCESSO A PARTIR DE RECURSO.");
	}

	public void limparClonesDoCombate() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}

		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem != null && !personagem.isClone()) {
				personagem.limparClonesAtivos();
			}
		}

		estado.getCombatentes().removeIf(Personagem::isClone);
		limparEstadoSquadTemporario.run();

		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		forEachMap.accept(MapController::sairModoSelecao);
	}

	public void encerrarContratosBarbaros() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}
		for (Personagem personagem : estado.getCombatentes()) {
			ContratoDeVidaUtils.limparEfemeros(personagem);
		}
	}

	public void encerrarEmprestimosOvertime() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}
		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem.getRaca() instanceof Humano && personagem.getRaca().isV2()) {
				((Humano) personagem.getRaca()).encerrarCombateOvertime(personagem);
			}
		}
	}

	public void encerrarPosturasAnao() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}
		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem.getRaca() instanceof Anao) {
				((Anao) personagem.getRaca()).encerrarPostura(personagem);
			}
		}
	}

	public void carregarMetadadosDoMapa(File imagemFile) {
		String pathJson = imagemFile.getAbsolutePath().substring(0, imagemFile.getAbsolutePath().lastIndexOf('.'))
				+ ".json";
		File jsonFile = new File(pathJson);
		if (jsonFile.exists()) {
			System.out.println("MAPA: Metadados encontrados: " + jsonFile.getName());
			try (FileReader reader = new FileReader(jsonFile)) {
				MapMetadata meta = new Gson().fromJson(reader, MapMetadata.class);
				forEachMap.accept(m -> m.aplicarMetadados(meta));
			} catch (Exception e) {
				System.err.println("Erro ao ler JSON do mapa: " + e.getMessage());
			}
		} else {
			System.out.println("MAPA: Nenhum JSON de metadados encontrado. Usando imagem pura.");
		}
	}

	public void carregarMetadadosDoMapa(String caminhoRecurso) {
		String pathJson = caminhoRecurso.substring(0, caminhoRecurso.lastIndexOf('.')) + ".json";
		try (InputStream is = FileLoader.carregarArquivo(pathJson)) {
			if (is != null) {
				System.out.println("MAPA: Metadados encontrados no recurso: " + pathJson);
				try (InputStreamReader reader = new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
					MapMetadata meta = new Gson().fromJson(reader, MapMetadata.class);
					forEachMap.accept(m -> m.aplicarMetadados(meta));
				}
			} else {
				System.out.println("MAPA: Nenhum JSON de metadados encontrado no recurso. Usando imagem pura.");
			}
		} catch (Exception e) {
			System.err.println("Erro ao ler JSON do recurso de mapa: " + e.getMessage());
		}
	}

	public void salvarMapaJson() {
		MapController source = primaryMapSupplier.get();
		File arquivoMapaAtual = arquivoMapaAtualSupplier.get();
		if (source == null || arquivoMapaAtual == null) {
			System.err.println("Erro: Nenhum mapa carregado para salvar.");
			return;
		}

		MapMetadata meta = source.extrairMetadados();
		String pathJson = arquivoMapaAtual.getAbsolutePath().substring(0,
				arquivoMapaAtual.getAbsolutePath().lastIndexOf('.')) + ".json";

		try (Writer writer = new FileWriter(pathJson, StandardCharsets.UTF_8)) {
			new GsonBuilder().setPrettyPrinting().create().toJson(meta, writer);
			System.out.println("MAPA SALVO COM SUCESSO: " + pathJson);

			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Editor de Mapa");
			alert.setHeaderText(null);
			alert.setContentText("Metadados do mapa salvos em:\n" + pathJson);
			alert.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void registrarDominio(Dominio dominio) {
		forEachMap.accept(m -> m.registrarDominio(dominio));
	}

	public void removerDominio(String dominioId) {
		forEachMap.accept(m -> m.removerDominio(dominioId));
	}

	public boolean isPersonagemNoDominio(Personagem personagem, String dominioId) {
		MapController source = primaryMapSupplier.get();
		if (source == null) {
			return false;
		}
		return source.isPersonagemNoDominio(personagem, dominioId);
	}

	public java.util.Map<String, Dominio> getDominiosAtivos() {
		MapController source = primaryMapSupplier.get();
		if (source == null) {
			return java.util.Collections.emptyMap();
		}
		return source.getDominiosAtivos();
	}

	public Dominio getDominio(String dominioId) {
		MapController source = primaryMapSupplier.get();
		if (source == null) {
			return null;
		}
		return source.getDominio(dominioId);
	}

	public void desenharRingueDoMapa(Personagem centro, int tamanho) {
		forEachMap.accept(m -> m.desenharRingueAlexei(centro, tamanho));
	}

	public void desenharDominioLyriaNoMapa(Personagem centro, int tamanho) {
		forEachMap.accept(m -> m.desenharDominioLyria(centro, tamanho));
	}

	public void acionarTransicaoDeMapa(Personagem jogadorQuePisou) {
		System.out.println(">>> TRANSICAO: " + jogadorQuePisou.getNome() + " encontrou a saída!");

		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Transição de Mapa");
		alert.setHeaderText(jogadorQuePisou.getNome() + " pisou na Zona de Saída.");
		alert.setContentText("Deseja encerrar o combate atual, distribuir XP e carregar a próxima arena?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {

combatManagerSupplier.get().distribuirXpAposCombate(estadoSupplier.get());

			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Selecione a Próxima Arena");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg"));

			String mapPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "mapas";
			File initialDir = new File(mapPath);
			if (initialDir.exists()) {
				fileChooser.setInitialDirectory(initialDir);
			}

			File selectedFile = fileChooser.showOpenDialog(ownerWindowSupplier.get());
			if (selectedFile != null) {
				carregarNovaArenaLogic(selectedFile);
			}
		}
	}

	public void iniciarMovimentoTatico(Personagem ator) {
		movimentoComRetorno = false;
		iniciarMovimentoNoMapa(ator);
	}

	public void iniciarMovimentoTaticoComRetorno(Personagem ator) {
		movimentoComRetorno = true;
		iniciarMovimentoNoMapa(ator);
	}

	private void iniciarMovimentoNoMapa(Personagem ator) {
		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		forEachMap.accept(m -> m.entrarModoSelecao(null, ator));
		Button btnConfirmarMovimento = btnConfirmarMovimentoSupplier.get();
		if (btnConfirmarMovimento != null) {
			btnConfirmarMovimento.setVisible(true);
			btnConfirmarMovimento.setManaged(true);
			btnConfirmarMovimento.setText("CONCLUIR MOVIMENTO (" + ator.getNome() + ")");
			btnConfirmarMovimento.toFront();
		}
	}

	public void confirmarMovimento() {
		forEachMap.accept(MapController::sairModoSelecao);
		forEachMap.accept(m -> {
			if (m.isModoMovimentoLivre()) {
				m.toggleModoMovimentoLivre(false);
			}
		});

		Button btnConfirmarMovimento = btnConfirmarMovimentoSupplier.get();
		if (btnConfirmarMovimento != null) {
			btnConfirmarMovimento.setVisible(false);
			btnConfirmarMovimento.setManaged(false);
		}

		EstadoCombate estado = estadoSupplier.get();
		Personagem ator = estado.getAtorAtual();
		if (ator != null) {
			ator.setContadorTU(ator.getContadorTU() + 100);
			System.out.println(">>> Movimento Tático concluído. +100 TU.");
			atualizarAuras();

			if (movimentoComRetorno) {
				movimentoComRetorno = false;
				reabrirHudParaAtor.accept(ator);
			} else {
				resolverAcaoPassarVez.accept(new AcaoMestreInput(ator, new ArrayList<>(), (Habilidade) null));
			}
		}
	}

	public void notificarMovimentoRealizado() {
		EstadoCombate estado = estadoSupplier.get();
		Personagem ator = estado.getAtorAtual();
		if (ator == null) {
			return;
		}

		if (movimentoComRetorno) {
			forEachMap.accept(MapController::sairModoSelecao);
			forEachMap.accept(m -> {
				if (m.isModoMovimentoLivre()) {
					m.toggleModoMovimentoLivre(false);
				}
			});

			Button btnConfirmarMovimento = btnConfirmarMovimentoSupplier.get();
			if (btnConfirmarMovimento != null) {
				btnConfirmarMovimento.setVisible(false);
				btnConfirmarMovimento.setManaged(false);
			}

			atualizarAuras();
			movimentoComRetorno = false;
			reabrirHudParaAtor.accept(ator);
		} else {
			Button btnConfirmarMovimento = btnConfirmarMovimentoSupplier.get();
			if (btnConfirmarMovimento != null) {
				btnConfirmarMovimento.setText("CONCLUIR MOVIMENTO (" + ator.getMovimentoRestanteTurno() + " restos)");
			}
		}
	}

	private void atualizarAuras() {
		CombatManager combatManager = combatManagerSupplier.get();
		if (combatManager != null) {
			combatManager.atualizarAuras(estadoSupplier.get());
		}
	}

	public void criarObjetoNoMapa(int x, int y) {
		removerObjetoNoMapa(x, y);
		ObjetoDestrutivel barreira = new ObjetoDestrutivel("Barricada de Madeira", 50, 5, true);
		barreira.setPosX(x);
		barreira.setPosY(y);
		estadoSupplier.get().getCombatentes().add(barreira);
		System.out.println("EDITOR: Objeto criado em (" + x + "," + y + ")");
	}

	public void removerObjetoNoMapa(int x, int y) {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}
		estado.getCombatentes()
				.removeIf(p -> p instanceof ObjetoDestrutivel && p.getPosX() == x && p.getPosY() == y);
	}

	public void toggleMovimentoLivre() {
		System.out.println("Botão 'Movimento Livre' clicado no Painel Mestre.");
		MapController primaryMap = primaryMapSupplier.get();
		forEachMap.accept(m -> m.toggleModoMovimentoLivre(!primaryMap.isModoMovimentoLivre()));
	}

	public void toggleEditorMapa() {
		System.out.println("Botão 'Editor de Mapa' clicado no Painel Mestre.");
		boolean ativar = !primaryMapSupplier.get().isModoEditor();
		forEachMap.accept(m -> m.setModoEditor(ativar));
	}
}
