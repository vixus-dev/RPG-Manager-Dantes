package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.items.Consumivel;
import br.com.dantesrpg.model.util.CharacterImageResolver;
import br.com.dantesrpg.controller.hud.VidasPortraitRenderer;
import br.com.dantesrpg.controller.util.ItemVisualUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class EditorJogadorController {

    private static final double TAMANHO_RETRATO_LISTA = 42;

    @FXML
    private StackPane editorThemeRoot;

    @FXML
    private ImageView editorThemeBackground;

    // --- Header ---
    @FXML
    private HBox characterHeader;

    @FXML
    private ImageView imgPortrait;

    @FXML
    private Label labelNomePersonagem;

    @FXML
    private Label labelClassePersonagem;

    @FXML
    private Label labelRacaPersonagem;

    @FXML
    private Label labelNivelXP;

    @FXML
    private ProgressBar progressXp;

    @FXML
    private Label labelXp;

    @FXML
    private Label labelPontosParaDistribuir;

    // --- Sidebar ---
    @FXML
    private VBox playerListContainer;

    @FXML
    private Button btnSalvar;

    @FXML
    private Button btnResetar;

    // --- Coluna 1: Atributos ---
    @FXML
    private GridPane attributesGrid;

    @FXML
    private Pane radarChartPlaceholder;

    @FXML
    private Label labelRanks;

    // --- Coluna 2: Equipamento + Stats ---
    @FXML
    private VBox equipmentContainer;

    @FXML
    private ScrollPane detailedAttributesScrollPane;

    @FXML
    private VBox detailedAttributesPane;

    @FXML
    private StackPane vidasContainer;

    // --- Coluna 3: Inventário + Habilidades ---
    @FXML
    private ListView<Item> inventarioListView;

    @FXML
    private ListView<Habilidade> habilidadesListView;

    @FXML
    private Button btnDoarItem;

    @FXML
    private TextField txtPesquisaInventario;

    @FXML
    private Button btnFiltroTudo;

    @FXML
    private Button btnFiltroArmas;

    @FXML
    private Button btnFiltroDefesas;

    @FXML
    private Button btnFiltroConsumiveis;

    private String categoriaSelecionada = "Tudo";

    // --- Painéis do Dossiê ---
    @FXML
    private TitledPane painelInfoClasse;

    @FXML
    private TitledPane painelInfoRaca;

    @FXML
    private Label labelInfoClasse;

    @FXML
    private Label labelInfoRaca;

    // --- Inspetor de Habilidades ---
    @FXML
    private VBox skillWelcomePane;

    @FXML
    private VBox skillPreviewPane;

    @FXML
    private Label lblSkillPreviewNome;

    @FXML
    private Label lblSkillPreviewDetalhes;

    @FXML
    private Label lblSkillPreviewTU;

    @FXML
    private Label lblSkillPreviewMana;

    @FXML
    private Label lblSkillPreviewDescricao;

    @FXML
    private Label lblSkillPreviewDano;

    @FXML
    private GridPane gridSkillPreviewAOE;

    // --- Lógica ---
    private List<Personagem> todosOsJogadores;
    private Personagem jogadorSelecionado;
    private CombatController mainController;
    private Button botaoSelecionadoAtual;

    private record ModificadorAtributoExibicao(
        int valor,
        String classeVisual,
        String origem
    ) {}

    @FXML
    public void initialize() {
        editorThemeBackground.fitWidthProperty().bind(editorThemeRoot.widthProperty());
        editorThemeBackground.fitHeightProperty().bind(editorThemeRoot.heightProperty());

        configurarPaineisDeOrigem();

        inventarioListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Item itemSelecionado = inventarioListView
                    .getSelectionModel()
                    .getSelectedItem();
                if (itemSelecionado != null) {
                    equiparItemDoInventario(itemSelecionado);
                }
            }
        });

        inventarioListView.setCellFactory(lv ->
            new ListCell<Item>() {
                @Override
                protected void updateItem(Item item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
						ItemVisualUtils.pararAnimacao(getGraphic());
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    } else {
						ItemVisualUtils.pararAnimacao(getGraphic());
                        setGraphic(criarCelulaInventario(item));
                        setText(null);

                        Tooltip tp = new Tooltip(gerarTextoDetalhesItem(item));
                        tp.setShowDelay(javafx.util.Duration.millis(200));
                        setTooltip(tp);
                    }
                }
            }
        );

        habilidadesListView.setCellFactory(lv ->
            new ListCell<Habilidade>() {
                @Override
                protected void updateItem(Habilidade hab, boolean empty) {
                    super.updateItem(hab, empty);
                    if (empty || hab == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    } else {
                        setGraphic(criarCelulaHabilidade(hab));
                        setText(null);
                        setTooltip(new Tooltip(hab.getDescricao()));
                    }
                }
            }
        );

        radarChartPlaceholder
            .widthProperty()
            .addListener((obs, oldVal, newVal) -> desenharGraficoRadar());
        radarChartPlaceholder
            .heightProperty()
            .addListener((obs, oldVal, newVal) -> desenharGraficoRadar());

        habilidadesListView
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                mostrarPreviewHabilidade(newVal);
            });

        if (txtPesquisaInventario != null) {
            txtPesquisaInventario
                .textProperty()
                .addListener((obs, oldVal, newVal) -> atualizarInventario());
        }
    }

    public void inicializar(
        CombatController mainController,
        EstadoCombate estado
    ) {
        this.mainController = mainController;
        this.todosOsJogadores = estado
            .getCombatentes()
            .stream()
            .filter(p -> p.getFaccao().equals("JOGADOR"))
            .collect(Collectors.toList());

        popularListaJogadores();

        if (!todosOsJogadores.isEmpty()) {
            selecionarJogador(todosOsJogadores.get(0));
        }
    }

    // =============================================
    // === SIDEBAR: Lista de Jogadores
    // =============================================

    private void popularListaJogadores() {
        playerListContainer.getChildren().clear();
        for (Personagem p : todosOsJogadores) {
            HBox btnContent = new HBox(10);
            btnContent.setAlignment(Pos.CENTER_LEFT);

            ImageView imgJogador = new ImageView(
                CharacterImageResolver.getPortrait(
                    p,
                    TAMANHO_RETRATO_LISTA,
                    TAMANHO_RETRATO_LISTA
                )
            );
            imgJogador.setFitWidth(TAMANHO_RETRATO_LISTA);
            imgJogador.setFitHeight(TAMANHO_RETRATO_LISTA);
            imgJogador.setPreserveRatio(false);
            imgJogador.setSmooth(true);

            Rectangle recorteRetrato = new Rectangle(
                TAMANHO_RETRATO_LISTA,
                TAMANHO_RETRATO_LISTA
            );
            recorteRetrato.setArcWidth(10);
            recorteRetrato.setArcHeight(10);

            StackPane imagemRecortada = new StackPane(imgJogador);
            imagemRecortada.setClip(recorteRetrato);

            StackPane cardRetrato = new StackPane(imagemRecortada);
            cardRetrato.getStyleClass().add("player-select-portrait");

            Region bordaRetrato = new Region();
            bordaRetrato.setMouseTransparent(true);
            bordaRetrato.getStyleClass().add("player-select-portrait-border");
            cardRetrato.getChildren().add(bordaRetrato);

            Label lblNome = new Label(p.getNome());
            lblNome.getStyleClass().add("player-select-name");
            lblNome.setWrapText(true);

            btnContent.getChildren().addAll(cardRetrato, lblNome);

            Button btnJogador = new Button();
            btnJogador.setGraphic(btnContent);
            btnJogador.setMaxWidth(Double.MAX_VALUE);
            btnJogador.getStyleClass().add("player-select-button");
            btnJogador.getStyleClass().add("editor-player-select-button");
            btnJogador.setOnAction(e -> {
                selecionarJogador(p);
                atualizarSelecaoBotao(btnJogador);
            });
            playerListContainer.getChildren().add(btnJogador);
        }
    }

    private void atualizarSelecaoBotao(Button novoBotao) {
        if (botaoSelecionadoAtual != null) {
            botaoSelecionadoAtual
                .getStyleClass()
                .remove("player-select-button-selected");
        }
        novoBotao.getStyleClass().add("player-select-button-selected");
        botaoSelecionadoAtual = novoBotao;
    }

    private void selecionarJogador(Personagem p) {
        this.jogadorSelecionado = p;
        atualizarTudo();

        // Selecionar botão correspondente se chamado programaticamente
        if (
            botaoSelecionadoAtual == null &&
            !playerListContainer.getChildren().isEmpty()
        ) {
            int idx = todosOsJogadores.indexOf(p);
            if (idx >= 0 && idx < playerListContainer.getChildren().size()) {
                atualizarSelecaoBotao(
                    (Button) playerListContainer.getChildren().get(idx)
                );
            }
        }
    }

    private void atualizarTudo() {
        if (jogadorSelecionado == null) return;

        jogadorSelecionado.recalcularAtributosEstatisticas();

        atualizarHeader();
        atualizarAtributosBase();
        atualizarEquipamento();
        atualizarAtributosDetalhados();
        atualizarInventario();
        atualizarHabilidades();
        atualizarInformacoesDeOrigem();
    }

    private void configurarPaineisDeOrigem() {
        painelInfoClasse.expandedProperty().addListener((obs, estavaExpandido, expandido) -> {
            if (expandido) {
                painelInfoRaca.setExpanded(false);
            }
        });
        painelInfoRaca.expandedProperty().addListener((obs, estavaExpandido, expandido) -> {
            if (expandido) {
                painelInfoClasse.setExpanded(false);
            }
        });
    }

    private void atualizarInformacoesDeOrigem() {
        Classe classe = jogadorSelecionado.getClasse();
        Raça raca = jogadorSelecionado.getRaca();

        painelInfoClasse.setDisable(classe == null);
        painelInfoRaca.setDisable(raca == null);
        painelInfoClasse.setText(classe == null ? "Classe indisponível" : "Classe — " + classe.getNome());
        painelInfoRaca.setText(raca == null ? "Raça indisponível" : "Raça — " + raca.getNome());
        labelInfoClasse.setText(classe == null ? "Este personagem não possui uma classe definida." : criarDossieClasse());
        labelInfoRaca.setText(raca == null ? "Este personagem não possui uma raça definida." : criarDossieRaca());
    }

    // =============================================
    // === HEADER DO PERSONAGEM
    // =============================================

    private void atualizarHeader() {
        labelNomePersonagem.setText(jogadorSelecionado.getNome());

        String classeNome =
            jogadorSelecionado.getClasse() != null
                ? jogadorSelecionado.getClasse().getNome()
                : "—";
        String racaNome =
            jogadorSelecionado.getRaca() != null
                ? jogadorSelecionado.getRaca().getNome()
                : "—";

        labelClassePersonagem.setText(classeNome);
        labelRacaPersonagem.setText(racaNome);

        int xpAtual = jogadorSelecionado.getXpAtual();
        int xpProx = jogadorSelecionado.getXpParaProximoNivel();
        double progressoXp = xpProx > 0
            ? Math.min(Math.max((double) xpAtual / xpProx, 0), 1)
            : 0;
        labelNivelXP.setText("Nível " + jogadorSelecionado.getNivel());
        progressXp.setProgress(progressoXp);
        labelXp.setText("XP " + xpAtual + "/" + xpProx);

        // Portrait
        carregarPortrait();

        // Pontos para distribuir
        int pontos = jogadorSelecionado.getPontosParaDistribuir();
        boolean possuiPontosParaDistribuir = pontos > 0;
        labelPontosParaDistribuir.setVisible(possuiPontosParaDistribuir);
        labelPontosParaDistribuir.setManaged(possuiPontosParaDistribuir);
        labelPontosParaDistribuir
            .getStyleClass()
            .removeAll("points-none", "points-available");
        if (pontos > 0) {
            labelPontosParaDistribuir.setText(
                "+" + pontos + " pontos para distribuir!"
            );
            labelPontosParaDistribuir.getStyleClass().add("points-available");
        } else {
            labelPontosParaDistribuir.setText("");
        }
    }

    private void carregarPortrait() {
        if (imgPortrait == null) return;
        imgPortrait.setImage(
            CharacterImageResolver.getPortrait(jogadorSelecionado, 120, 120)
        );
    }

    // =============================================
    // === COLUNA 1: Atributos
    // =============================================

    private void atualizarAtributosBase() {
        attributesGrid.getChildren().clear();
        attributesGrid.getColumnConstraints().clear();

        // Configurar colunas: Nome | Valor | Diff | Botão+ | Badge Rank | Progress
        javafx.scene.layout.ColumnConstraints colNome =
            new javafx.scene.layout.ColumnConstraints(45);
        javafx.scene.layout.ColumnConstraints colValor =
            new javafx.scene.layout.ColumnConstraints(30);
        javafx.scene.layout.ColumnConstraints colFontes =
            new javafx.scene.layout.ColumnConstraints(54, 76, Double.MAX_VALUE);
        colFontes.setHgrow(Priority.ALWAYS);
        javafx.scene.layout.ColumnConstraints colBtn =
            new javafx.scene.layout.ColumnConstraints(30);
        javafx.scene.layout.ColumnConstraints colRank =
            new javafx.scene.layout.ColumnConstraints(50);
        javafx.scene.layout.ColumnConstraints colDado =
            new javafx.scene.layout.ColumnConstraints(40);
        attributesGrid
            .getColumnConstraints()
            .addAll(colNome, colValor, colFontes, colBtn, colRank, colDado);

        int i = 0;
        for (Atributo atr : Atributo.values()) {
            int valorFinal = jogadorSelecionado
                .getAtributosFinais()
                .getOrDefault(atr, 1);
            int valorBase = jogadorSelecionado
                .getAtributosBase()
                .getOrDefault(atr, 1);
            String rank = getRank(valorFinal);

            // Col 0: Nome do atributo
            Label lblNome = new Label(atr.name().substring(0, 3));
            lblNome.getStyleClass().add("attr-name-label");
            Tooltip.install(lblNome, new Tooltip(atr.name()));
            attributesGrid.add(lblNome, 0, i);

            // Col 1: Valor final
            Label lblValor = new Label(String.valueOf(valorFinal));
            if (valorFinal > valorBase) {
                lblValor.getStyleClass().add("attr-value-buffed");
            } else if (valorFinal < valorBase) {
                lblValor.getStyleClass().add("attr-value-debuffed");
            } else {
                lblValor.getStyleClass().add("attr-value-normal");
            }
            attributesGrid.add(lblValor, 1, i);

            // Col 2: Fontes de modificadores, reveladas somente no hover.
            HBox caixaModificadores = criarCaixaModificadores(atr);
            GridPane.setHgrow(caixaModificadores, Priority.ALWAYS);
            attributesGrid.add(caixaModificadores, 2, i);

            // Col 3: Botão +
            Button btnMais = new Button("+");
            btnMais.getStyleClass().add("editor-btn-plus");
            btnMais.setDisable(
                jogadorSelecionado.getPontosParaDistribuir() <= 0
            );
            btnMais.setOnAction(e -> {
                boolean sucesso = jogadorSelecionado.aumentarAtributoBase(atr);
                if (sucesso) atualizarTudo();
            });
            attributesGrid.add(btnMais, 3, i);

            // Col 4: Rank badge
            Label lblRank = new Label(rank);
            lblRank
                .getStyleClass()
                .addAll("rank-badge", getRankStyleClass(valorFinal));
            attributesGrid.add(lblRank, 4, i);

            // Col 5: Dado
            int dado = br.com.dantesrpg.model.util.DiceRoller.getTipoDado(
                valorFinal
            );
            Label lblDado = new Label("d" + dado);
            lblDado.setStyle("-fx-text-fill: #606070; -fx-font-size: 11px;");
            attributesGrid.add(lblDado, 5, i);

            i++;
        }

        desenharGraficoRadar();
    }

    private HBox criarCaixaModificadores(Atributo atributo) {
        HBox caixa = new HBox(4);
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.setMaxWidth(Double.MAX_VALUE);
        caixa.getStyleClass().add("attribute-modifier-box");

        List<ModificadorAtributoExibicao> modificadores = listarModificadoresDoAtributo(atributo);
        Label total = criarLabelTotalModificadores(modificadores);
        caixa.getChildren().add(total);

        caixa.setOnMouseEntered(event -> caixa.getChildren().setAll(criarCaixasPorCategoria(modificadores)));
        caixa.setOnMouseExited(event -> caixa.getChildren().setAll(total));
        return caixa;
    }

    private Label criarLabelTotalModificadores(List<ModificadorAtributoExibicao> modificadores) {
        int total = modificadores.stream().mapToInt(ModificadorAtributoExibicao::valor).sum();
        Label label = new Label(formatarModificador(total));
        label.getStyleClass().add(
            total > 0
                ? "attribute-modifier-total-positive"
                : total < 0 ? "attribute-modifier-total-negative" : "attribute-modifier-total-neutral"
        );
        return label;
    }

    private List<StackPane> criarCaixasPorCategoria(List<ModificadorAtributoExibicao> modificadores) {
        List<StackPane> caixas = List.of(
            criarCaixaCategoria(modificadores, "attribute-modifier-class", "Classe"),
            criarCaixaCategoria(modificadores, "attribute-modifier-equipment", "Equipamentos"),
            criarCaixaCategoria(modificadores, "attribute-modifier-racial", "Raça e efeitos")
        );
        for (StackPane caixa : caixas) {
            caixa.setPrefWidth(0);
            caixa.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(caixa, Priority.ALWAYS);
        }
        return caixas;
    }

    private StackPane criarCaixaCategoria(
        List<ModificadorAtributoExibicao> modificadores,
        String classeVisual,
        String categoria
    ) {
        List<ModificadorAtributoExibicao> fontes = modificadores
            .stream()
            .filter(modificador -> modificador.classeVisual().equals(classeVisual))
            .toList();
        int total = fontes.stream().mapToInt(ModificadorAtributoExibicao::valor).sum();

        Label label = new Label(formatarModificador(total));
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add(classeVisual);

        StackPane caixa = new StackPane(label);
        caixa.getStyleClass().addAll("attribute-modifier-category-box", classeVisual + "-box");
        Tooltip tooltip = new Tooltip(montarTooltipCategoria(categoria, fontes));
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(caixa, tooltip);
        return caixa;
    }

    private String montarTooltipCategoria(
        String categoria,
        List<ModificadorAtributoExibicao> fontes
    ) {
        if (fontes.isEmpty()) {
            return categoria + ": nenhum modificador";
        }

        StringBuilder tooltip = new StringBuilder(categoria).append(":");
        for (ModificadorAtributoExibicao fonte : fontes) {
            tooltip
                .append("\n")
                .append(fonte.origem())
                .append(" ")
                .append(formatarModificador(fonte.valor()));
        }
        return tooltip.toString();
    }

    private List<ModificadorAtributoExibicao> listarModificadoresDoAtributo(Atributo atributo) {
        List<ModificadorAtributoExibicao> modificadores = new ArrayList<>();
        Classe classe = jogadorSelecionado.getClasse();
        adicionarModificador(
            modificadores,
            obterModificador(classe == null ? null : classe.getModificadoresDeAtributo(), atributo),
            "attribute-modifier-class",
            "Classe: " + (classe == null ? "Sem classe" : classe.getNome())
        );

        adicionarModificadoresRaciais(modificadores, atributo);
        adicionarModificadoresDeEfeitos(modificadores, atributo);
        adicionarModificadoresDeEquipamento(modificadores, atributo);
        return modificadores;
    }

    private void adicionarModificadoresRaciais(List<ModificadorAtributoExibicao> modificadores, Atributo atributo) {
        Raça raca = jogadorSelecionado.getRaca();
        if (raca == null) return;

        adicionarModificador(
            modificadores,
            obterModificador(raca.getAttributeModifiers(jogadorSelecionado), atributo),
            "attribute-modifier-racial",
            "Raça: " + raca.getNome()
        );
        adicionarModificador(
            modificadores,
            obterModificador(raca.getTemporaryAttributeModifiers(jogadorSelecionado), atributo),
            "attribute-modifier-racial",
            "Raça: " + raca.getNome() + " (bônus temporário)"
        );
    }

    private void adicionarModificadoresDeEfeitos(List<ModificadorAtributoExibicao> modificadores, Atributo atributo) {
        for (Efeito efeito : jogadorSelecionado.getEfeitosAtivos().values()) {
            if (efeito == null || efeito.getModificadores() == null) continue;

            adicionarModificador(
                modificadores,
                obterModificadorDeEfeito(efeito, atributo),
                "attribute-modifier-racial",
                "Efeito ativo: " + efeito.getNome()
            );
        }
    }

    public void setFundoTema(javafx.scene.image.Image imagem) {
        editorThemeBackground.setImage(imagem);
        editorThemeBackground.setVisible(imagem != null);
    }

    private void adicionarModificadoresDeEquipamento(List<ModificadorAtributoExibicao> modificadores, Atributo atributo) {
        for (Arma arma : jogadorSelecionado.getArmasEquipadas()) {
            adicionarModificador(
                modificadores,
                obterModificador(arma.getModificadoresDeAtributo(), atributo),
                "attribute-modifier-equipment",
                "Arma: " + arma.getNome()
            );
        }

        Armadura armadura = jogadorSelecionado.getArmaduraEquipada();
        adicionarModificador(
            modificadores,
            obterModificador(armadura == null ? null : armadura.getModificadoresDeAtributo(), atributo),
            "attribute-modifier-equipment",
            "Armadura: " + (armadura == null ? "" : armadura.getNome())
        );

        adicionarModificadorDeAmuleto(modificadores, jogadorSelecionado.getAmuleto1(), atributo);
        adicionarModificadorDeAmuleto(modificadores, jogadorSelecionado.getAmuleto2(), atributo);
    }

    private void adicionarModificadorDeAmuleto(
        List<ModificadorAtributoExibicao> modificadores,
        Amuleto amuleto,
        Atributo atributo
    ) {
        adicionarModificador(
            modificadores,
            obterModificador(amuleto == null ? null : amuleto.getModificadoresDeAtributo(), atributo),
            "attribute-modifier-equipment",
            "Amuleto: " + (amuleto == null ? "" : amuleto.getNome())
        );
    }

    private void adicionarModificador(
        List<ModificadorAtributoExibicao> modificadores,
        int valor,
        String classeVisual,
        String origem
    ) {
        if (valor != 0) {
            modificadores.add(new ModificadorAtributoExibicao(valor, classeVisual, origem));
        }
    }

    private int obterModificador(Map<Atributo, Integer> modificadores, Atributo atributo) {
        return modificadores == null ? 0 : modificadores.getOrDefault(atributo, 0);
    }

    private int obterModificadorDeEfeito(Efeito efeito, Atributo atributo) {
        Double valor = efeito.getModificadores().get(atributo.name());
        return valor == null ? 0 : valor.intValue();
    }

    private String formatarModificador(int valor) {
        return valor > 0 ? "+" + valor : String.valueOf(valor);
    }

    private String getRankStyleClass(int valor) {
        if (valor >= 20) return "rank-SSS";
        if (valor >= 18) return "rank-SS";
        if (valor >= 15) return "rank-S";
        if (valor >= 12) return "rank-A";
        if (valor >= 9) return "rank-B";
        if (valor >= 6) return "rank-C";
        if (valor >= 3) return "rank-D";
        return "rank-E";
    }

    // =============================================
    // === COLUNA 2: Equipamento (Cards)
    // =============================================

    private void atualizarEquipamento() {
        equipmentContainer
            .getChildren()
            .forEach(ItemVisualUtils::pararAnimacao);
        equipmentContainer.getChildren().clear();

        // Arma
        for (Arma arma : jogadorSelecionado.getArmasEquipadas()) {
            equipmentContainer
                .getChildren()
                .add(
                    criarCardEquipamento(
                        "Arma (" + arma.getWielding() + "W)",
                        arma,
                        e -> onDesequiparArmaClick(arma)
                    )
                );
            if (arma instanceof Grimorio) {
                equipmentContainer
                    .getChildren()
                    .add(criarGrimorioSection((Grimorio) arma));
            }
        }
        if (jogadorSelecionado.getWieldingDisponivel() > 0) {
            equipmentContainer
                .getChildren()
                .add(
                    criarCardEquipamento(
                        "Arma Livre (" +
                            jogadorSelecionado.getWieldingDisponivel() +
                            "W)",
                        null,
                        e -> {}
                    )
                );
        }

        // Conteúdo extra da arma (Grimorio)
        // Armadura
        Armadura armadura = jogadorSelecionado.getArmaduraEquipada();
        equipmentContainer
            .getChildren()
            .add(
                criarCardEquipamento("Armadura", armadura, e ->
                    onDesequiparArmaduraClick()
                )
            );

        // Amuleto 1
        Amuleto am1 = jogadorSelecionado.getAmuleto1();
        equipmentContainer
            .getChildren()
            .add(
                criarCardEquipamento("Amuleto 1", am1, e ->
                    onDesequiparAmuleto1Click()
                )
            );

        // Amuleto 2
        Amuleto am2 = jogadorSelecionado.getAmuleto2();
        equipmentContainer
            .getChildren()
            .add(
                criarCardEquipamento("Amuleto 2", am2, e ->
                    onDesequiparAmuleto2Click()
                )
            );
    }

    private VBox criarCardEquipamento(
        String slotName,
        Item item,
        javafx.event.EventHandler<javafx.event.ActionEvent> onUnequip
    ) {
        VBox card = new VBox(3);

        if (item == null) {
            // Slot vazio
            card.getStyleClass().add("equipment-card-empty");
            Label lblSlot = new Label(slotName);
            lblSlot.getStyleClass().add("equipment-slot-label");
            Label lblVazio = new Label("— Slot Vazio —");
            lblVazio.setStyle(
                "-fx-text-fill: #404050; -fx-font-style: italic; -fx-font-size: 11px;"
            );
            card.getChildren().addAll(lblSlot, lblVazio);
            return card;
        }

        // Slot preenchido
        card.getStyleClass().add("equipment-card");
        Raridade raridade = ItemVisualUtils.obterRaridade(item);
        if (raridade != null) {
            card.getStyleClass().add("rarity-" + raridade.name().toLowerCase());
        }

        // Header do card: slot label + nome + botão desequipar
        Label lblSlot = new Label(slotName);
        lblSlot.getStyleClass().add("equipment-slot-label");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label lblNome = new Label(item.getNomeComOverclock());
        lblNome.getStyleClass().add("equipment-item-name");
        if (raridade != null) {
            lblNome
                .getStyleClass()
                .add("name-" + raridade.name().toLowerCase());
        } else {
            lblNome.setStyle("-fx-text-fill: white;");
        }
        HBox.setHgrow(lblNome, Priority.ALWAYS);

        Button btnRemover = new Button("X");
        btnRemover.getStyleClass().add("btn-unequip");
        btnRemover.setOnAction(onUnequip);

        headerRow.getChildren().addAll(lblNome, btnRemover);

        card.getChildren().addAll(lblSlot, headerRow);
		adicionarDescricaoItem(card, item);

        // Stats do item
        String detalhes = gerarTextoDetalhesItemCompacto(item);
        if (!detalhes.isEmpty()) {
            for (String linha : detalhes.split("\n")) {
                if (linha.isEmpty()) continue;
                Label lblStat = new Label(linha);
                lblStat.getStyleClass().add("equipment-stat");
                if (linha.startsWith("Dano:") || linha.startsWith("Defesa:")) {
                    lblStat.getStyleClass().add("equipment-stat-primary");
                }
                if (linha.startsWith("+") || linha.contains("Hab:")) {
                    lblStat.getStyleClass().add("equipment-stat-positive");
                } else if (linha.startsWith("-")) {
                    lblStat.getStyleClass().add("equipment-stat-negative");
                }
                card.getChildren().add(lblStat);
            }
        }

        ItemVisualUtils.aplicarEfeitosNoCard(card, item);
        return card;
    }

    private void adicionarDescricaoItem(VBox card, Item item) {
        String descricao = item.getDescricao();
        if (descricao == null || descricao.isBlank()) return;

        Label lblDescricao = new Label(descricao);
        lblDescricao.setWrapText(true);
        lblDescricao.setMaxWidth(Double.MAX_VALUE);
        lblDescricao.getStyleClass().add("equipment-item-description");
        card.getChildren().add(lblDescricao);
    }

    private VBox criarGrimorioSection(Grimorio grimorio) {
        VBox section = new VBox(3);
        section.setPadding(new Insets(0, 0, 0, 10));

        int slotsOcupados = grimorio.getMagiasArmazenadas().size();
        int slotsTotal = grimorio.getMaxSlots();

        Label lblSlots = new Label(
            "Magias (" + slotsOcupados + "/" + slotsTotal + ")"
        );
        lblSlots.setStyle(
            "-fx-text-fill: cyan; -fx-font-weight: bold; -fx-font-size: 11px;"
        );
        section.getChildren().add(lblSlots);

        for (Habilidade magia : grimorio.getMagiasArmazenadas()) {
            HBox row = new HBox(5);
            row.setAlignment(Pos.CENTER_LEFT);

            Button btnRemover = new Button("x");
            btnRemover.setStyle(
                "-fx-background-color: #3a1a1a; -fx-text-fill: #ff6b6b; -fx-font-size: 8px; -fx-padding: 1 4; -fx-background-radius: 3;"
            );
            btnRemover.setOnAction(e -> {
                grimorio.esquecerMagia(magia);
                atualizarTudo();
            });

            Label lblMagia = new Label(magia.getNome());
            lblMagia.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px;");

            row.getChildren().addAll(btnRemover, lblMagia);
            section.getChildren().add(row);
        }

        if (slotsOcupados < slotsTotal) {
            Button btnAddMagia = new Button("+ Adicionar Magia");
            btnAddMagia.setStyle(
                "-fx-background-color: #1a1a3a; -fx-text-fill: cyan; -fx-font-size: 10px; -fx-background-radius: 4; -fx-border-color: #2a2a5a; -fx-border-width: 1; -fx-border-radius: 4; -fx-cursor: hand;"
            );
            btnAddMagia.setMaxWidth(Double.MAX_VALUE);
            btnAddMagia.setOnAction(e -> abrirDialogoAdicionarMagia(grimorio));
            section.getChildren().add(btnAddMagia);
        }

        return section;
    }

    private String gerarTextoDetalhesItemCompacto(Item item) {
        StringBuilder sb = new StringBuilder();

        if (item instanceof Arma) {
            Arma a = (Arma) item;
            sb.append("Dano: ").append(a.getDanoBase()).append("\n");
            if (a.getPenetracaoArmadura() > 0.0) {
                sb.append(String.format("Penetração de armadura: %.0f%%\n", a.getPenetracaoArmadura() * 100.0));
            }
            List<String> habs = a.getHabilidadesConcedidasNomes();
            if (habs != null) {
                for (String hab : habs) {
                    sb.append("Hab: ").append(hab).append("\n");
                }
            }
            appendModificadoresStatus(sb, a.getModificadoresStatus());
        } else if (item instanceof Armadura) {
            Armadura a = (Armadura) item;
            sb.append("Defesa: ").append(a.getArmaduraBase()).append("\n");
            if (
                a.getNomeEfeitoOnDamageTaken() != null &&
                !a.getNomeEfeitoOnDamageTaken().isEmpty()
            ) {
                sb.append("Reação: ")
                    .append(a.getNomeEfeitoOnDamageTaken())
                    .append(" (")
                    .append((int) (a.getChanceEfeitoOnDamageTaken() * 100))
                    .append("% para ")
                    .append(a.getAlvoEfeitoOnDamageTaken())
                    .append(")\n");
            }
            List<String> habs = a.getHabilidadesConcedidasNomes();
            if (habs != null) {
                for (String hab : habs) {
                    sb.append("Hab: ").append(hab).append("\n");
                }
            }
            appendModificadoresAtributo(sb, a.getModificadoresDeAtributo());
            appendModificadoresStatus(sb, a.getModificadoresStatus());
        } else if (item instanceof Amuleto) {
            Amuleto a = (Amuleto) item;
            if (a.getArmaduraBonus() > 0) sb.append("Defesa: +")
                .append(a.getArmaduraBonus())
                .append("\n");
            List<String> habs = a.getHabilidadesConcedidasNomes();
            if (habs != null) {
                for (String hab : habs) {
                    sb.append("Hab: ").append(hab).append("\n");
                }
            }
            appendModificadoresAtributo(sb, a.getModificadoresDeAtributo());
            appendModificadoresStatus(sb, a.getModificadoresStatus());
        }

        return sb.toString().trim();
    }

    private void appendModificadoresAtributo(
        StringBuilder sb,
        Map<Atributo, Integer> modAtr
    ) {
        if (modAtr == null || modAtr.isEmpty()) return;
        modAtr.forEach((atr, val) -> {
            sb.append(val > 0 ? "+" : "")
                .append(val)
                .append(" ")
                .append(atr.name().substring(0, 3))
                .append("\n");
        });
    }

    private void appendModificadoresStatus(
        StringBuilder sb,
        Map<String, Double> modStatus
    ) {
        if (modStatus == null || modStatus.isEmpty()) return;
        modStatus.forEach((key, val) -> {
            String nome = traduzirNomeStatus(key);
            double valor = val;
            String sufixo = "";
            if (
                key.contains("PERCENTUAL") ||
                key.contains("MODIFICADOR") ||
                key.contains("CRITICA") ||
                key.contains("CRITICO")
            ) {
                valor = val * 100;
                sufixo = "%";
            }
            sb.append(valor > 0 ? "+" : "")
                .append(String.format("%.0f", valor))
                .append(sufixo)
                .append(" ")
                .append(nome)
                .append("\n");
        });
    }

    private String traduzirNomeStatus(String key) {
        switch (key) {
            case "HP_MAXIMO":
                return "Vida Máx";
            case "MP_MAXIMO":
                return "Mana Máx";
            case "MOVIMENTO":
                return "Movimento";
            case "REDUCAO_DANO_MODIFICADOR":
                return "Red. Dano";
            case "DANO_BONUS_PERCENTUAL":
                return "Dano";
            case "TAXA_CRITICA":
                return "Taxa Crit";
            case "DANO_CRITICO":
                return "Dano Crit";
            default:
                return key.replace("_", " ");
        }
    }

    // =============================================
    // === COLUNA 2: Atributos Detalhados (Barras)
    // =============================================

    private void atualizarAtributosDetalhados() {
        detailedAttributesPane.getChildren().clear();

        // HP Bar
        detailedAttributesPane
            .getChildren()
            .add(
                criarBarraStat(
                    "HP",
                    jogadorSelecionado.getVidaAtual(),
                    jogadorSelecionado.getVidaMaxima(),
                    "stat-bar-hp"
                )
            );

        // MP Bar
        detailedAttributesPane
            .getChildren()
            .add(
                criarBarraStat(
                    "MP",
                    jogadorSelecionado.getManaAtual(),
                    jogadorSelecionado.getManaMaxima(),
                    "stat-bar-mp"
                )
            );

        // Separador
        Region sep = new Region();
        sep.getStyleClass().add("editor-separator");
        sep.setPrefHeight(1);
        detailedAttributesPane.getChildren().add(sep);

        // Stats de combate
        adicionarStatRow(
            "Movimento",
            String.valueOf(jogadorSelecionado.getMovimento()),
            null
        );
        adicionarStatRow(
            "Taxa Crítica",
            String.format("%.1f%%", jogadorSelecionado.getTaxaCritica() * 100),
            "yellow"
        );
        adicionarStatRow(
            "Dano Crítico",
            String.format("+%.1f%%", jogadorSelecionado.getDanoCritico() * 100),
            "yellow"
        );
        adicionarStatRow(
            "Armadura Total",
            String.valueOf(jogadorSelecionado.getArmaduraTotal()),
            null
        );
        adicionarStatRow(
            "Red. Armadura",
            String.format(
                "%.1f%%",
                jogadorSelecionado.getReducaoDanoArmadura() * 100
            ),
            "green"
        );
        adicionarStatRow(
            "Red. Topor",
            String.format(
                "%.1f%%",
                jogadorSelecionado.getReducaoDanoTopor() * 100
            ),
            "green"
        );
        adicionarStatRow(
            "Bônus Dano",
            String.format(
                "+%.1f%%",
                jogadorSelecionado.getBonusDanoPercentual() * 100
            ),
            "red"
        );
        adicionarStatRow(
            "Pecado",
            String.valueOf(jogadorSelecionado.getPecado()),
            "purple"
        );
        VidasPortraitRenderer.atualizar(vidasContainer, jogadorSelecionado.getVidas());
    }

    private StackPane criarBarraStat(
        String nome,
        double atual,
        double max,
        String fillStyleClass
    ) {
        double largura = 250;
        double altura = 20;

        StackPane container = new StackPane();
        container.setPrefHeight(altura);
        container.setMaxWidth(Double.MAX_VALUE);

        // Track (fundo)
        Region track = new Region();
        track.getStyleClass().add("stat-bar-track");
        track.setMaxWidth(Double.MAX_VALUE);
        track.setPrefHeight(altura);

        // Fill
        double percentual = max > 0 ? Math.min(atual / max, 1.0) : 0;
        Region fill = new Region();
        fill.getStyleClass().add(fillStyleClass);
        fill.setPrefHeight(altura - 4);
        fill.setMaxHeight(altura - 4);
        fill.maxWidthProperty().bind(
            container.widthProperty().multiply(percentual).subtract(4)
        );
        fill.setMinWidth(0);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        StackPane.setMargin(fill, new Insets(2));

        // Texto
        Label texto = new Label(
            nome + ": " + formatarNumero(atual) + " / " + formatarNumero(max)
        );
        texto.getStyleClass().add("stat-bar-text");

        container.getChildren().addAll(track, fill, texto);
        return container;
    }

    private void adicionarStatRow(String nome, String valor, String corTipo) {
        HBox row = new HBox(10);
        row.getStyleClass().add("editor-stat-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblNome = new Label(nome);
        lblNome.getStyleClass().add("editor-stat-label");
        HBox.setHgrow(lblNome, Priority.ALWAYS);

        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("editor-stat-value");
        if ("green".equals(corTipo)) lblValor
            .getStyleClass()
            .add("editor-stat-value-green");
        else if ("red".equals(corTipo)) lblValor
            .getStyleClass()
            .add("editor-stat-value-red");
        else if ("yellow".equals(corTipo)) lblValor
            .getStyleClass()
            .add("editor-stat-value-yellow");
        else if ("purple".equals(corTipo)) lblValor
            .setStyle("-fx-text-fill: #b56cff;");

        row.getChildren().addAll(lblNome, lblValor);
        detailedAttributesPane.getChildren().add(row);
    }

    // =============================================
    // === COLUNA 3: Inventário + Habilidades
    // =============================================

    private StackPane criarCelulaInventario(Item item) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
		row.setMaxWidth(Double.MAX_VALUE);

        // Ícone de tipo
        String icone;
        if (item instanceof Arma) icone = "\u2694";
        else if (item instanceof Armadura) icone = "\uD83D\uDEE1";
        else if (item instanceof Amuleto) icone = "\uD83D\uDCAE";
        else icone = "\uD83E\uDDEA";

        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 14px;");

        // Nome com cor de raridade
        int quantidade = 0;
        if (jogadorSelecionado != null) {
            quantidade = jogadorSelecionado
                .getInventario()
                .getItensAgrupados()
                .getOrDefault(item.getTipo(), 0);
        }

        Label lblNome = new Label(item.getNomeComOverclock());
        String corRaridade = getCorRaridadeItem(item);
        lblNome.setStyle(
            "-fx-text-fill: " + corRaridade + "; -fx-font-size: 12px;"
        );
        HBox.setHgrow(lblNome, Priority.ALWAYS);

        Label lblQtd = new Label("x" + quantidade);
        lblQtd.setStyle("-fx-text-fill: #808090; -fx-font-size: 11px;");

        row.getChildren().addAll(lblIcone, lblNome, lblQtd);

		StackPane card = new StackPane(row);
		card.getStyleClass().add("editor-inventory-item-card");
		card.setMaxWidth(Double.MAX_VALUE);
		ItemVisualUtils.aplicarEfeitosNoCard(card, item);
		return card;
    }

    private String getCorRaridadeItem(Item item) {
        if (item instanceof Arma) {
            Raridade r = ((Arma) item).getRaridade();
            if (r != null) return getCorRaridade(r);
        }
		if (item instanceof Armadura) {
			Raridade r = ((Armadura) item).getRaridade();
			if (r != null) return getCorRaridade(r);
		}
		if (item instanceof Amuleto) {
			Raridade r = ((Amuleto) item).getRaridade();
			if (r != null) return getCorRaridade(r);
		}
        return "#c0c0c0";
    }

    private String getCorRaridade(Raridade r) {
        switch (r) {
            case COMUM:
                return "#c0c0c0";
            case INCOMUM:
                return "#2ecc71";
            case RARO:
                return "#3498db";
            case EPICO:
                return "#9b59b6";
            case LENDARIO:
                return "#f39c12";
            case UNICO:
                return "#e74c3c";
            case MITICO:
                return "#ff00ff";
            default:
                return "#c0c0c0";
        }
    }

    private HBox criarCelulaHabilidade(Habilidade hab) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblNome = new Label(hab.getNome());
        lblNome.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        HBox.setHgrow(lblNome, Priority.ALWAYS);

        // Custo TU
        if (hab.getCustoTU() > 0) {
            Label lblTU = new Label(hab.getCustoTU() + " TU");
            lblTU.setStyle(
                "-fx-text-fill: #f39c12; -fx-font-size: 10px; -fx-font-weight: bold;"
            );
            row.getChildren().add(lblNome);
            row.getChildren().add(lblTU);
        } else {
            row.getChildren().add(lblNome);
        }

        // Custo Mana
        if (hab.getCustoMana() > 0) {
            Label lblMana = new Label(hab.getCustoMana() + " MP");
            lblMana.setStyle(
                "-fx-text-fill: #3498db; -fx-font-size: 10px; -fx-font-weight: bold;"
            );
            row.getChildren().add(lblMana);
        }

        return row;
    }

    private void atualizarInventario() {
        atualizarBotoesFiltro();
        inventarioListView.getItems().clear();

        if (
            jogadorSelecionado != null &&
            jogadorSelecionado.getInventario() != null &&
            mainController != null
        ) {
            Map<String, Integer> inventarioAgrupado = jogadorSelecionado
                .getInventario()
                .getItensAgrupados();
            String filtro =
                txtPesquisaInventario != null
                    ? txtPesquisaInventario.getText().toLowerCase().trim()
                    : "";

            for (String tipoItem : inventarioAgrupado.keySet()) {
                Item itemModelo = mainController.getItem(tipoItem);
                if (itemModelo != null) {
                    int ocGrau = jogadorSelecionado
                        .getInventario()
                        .getOverclockDoItem(tipoItem);
                    if (ocGrau > 0) {
                        itemModelo.setGrauOverclock(ocGrau);
                    }
                    // Filtro de pesquisa pelo nome do item
                    if (
                        filtro.isEmpty() ||
                        itemModelo.getNome().toLowerCase().contains(filtro)
                    ) {
                        boolean correspondeCategoria = false;
                        if ("Tudo".equals(categoriaSelecionada)) {
                            correspondeCategoria = true;
                        } else if ("Armas".equals(categoriaSelecionada)) {
                            correspondeCategoria = itemModelo instanceof Arma;
                        } else if ("Defesas".equals(categoriaSelecionada)) {
                            correspondeCategoria =
                                itemModelo instanceof Armadura ||
                                itemModelo instanceof Amuleto;
                        } else if ("Consumiveis".equals(categoriaSelecionada)) {
                            correspondeCategoria =
                                itemModelo instanceof Consumivel;
                        }

                        if (correspondeCategoria) {
                            inventarioListView.getItems().add(itemModelo);
                        }
                    }
                }
            }
        }
    }

    @FXML
    private void onFiltrarTudo() {
        this.categoriaSelecionada = "Tudo";
        atualizarBotoesFiltro();
        atualizarInventario();
    }

    @FXML
    private void onFiltrarArmas() {
        this.categoriaSelecionada = "Armas";
        atualizarBotoesFiltro();
        atualizarInventario();
    }

    @FXML
    private void onFiltrarDefesas() {
        this.categoriaSelecionada = "Defesas";
        atualizarBotoesFiltro();
        atualizarInventario();
    }

    @FXML
    private void onFiltrarConsumiveis() {
        this.categoriaSelecionada = "Consumiveis";
        atualizarBotoesFiltro();
        atualizarInventario();
    }

    private void atualizarBotoesFiltro() {
        if (btnFiltroTudo == null) return;

        btnFiltroTudo
            .getStyleClass()
            .removeAll("editor-btn-filter", "editor-btn-filter-active");
        btnFiltroArmas
            .getStyleClass()
            .removeAll("editor-btn-filter", "editor-btn-filter-active");
        btnFiltroDefesas
            .getStyleClass()
            .removeAll("editor-btn-filter", "editor-btn-filter-active");
        btnFiltroConsumiveis
            .getStyleClass()
            .removeAll("editor-btn-filter", "editor-btn-filter-active");

        btnFiltroTudo
            .getStyleClass()
            .add(
                "Tudo".equals(categoriaSelecionada)
                    ? "editor-btn-filter-active"
                    : "editor-btn-filter"
            );
        btnFiltroArmas
            .getStyleClass()
            .add(
                "Armas".equals(categoriaSelecionada)
                    ? "editor-btn-filter-active"
                    : "editor-btn-filter"
            );
        btnFiltroDefesas
            .getStyleClass()
            .add(
                "Defesas".equals(categoriaSelecionada)
                    ? "editor-btn-filter-active"
                    : "editor-btn-filter"
            );
        btnFiltroConsumiveis
            .getStyleClass()
            .add(
                "Consumiveis".equals(categoriaSelecionada)
                    ? "editor-btn-filter-active"
                    : "editor-btn-filter"
            );
    }

    private void atualizarHabilidades() {
        habilidadesListView.getItems().clear();
        if (
            jogadorSelecionado != null &&
            jogadorSelecionado.getHabilidadesDeClasse() != null
        ) {
            habilidadesListView
                .getItems()
                .addAll(jogadorSelecionado.getHabilidadesDeClasse());
        }
    }

    // =============================================
    // === DIÁLOGOS
    // =============================================

    private void abrirDialogoAdicionarMagia(Grimorio grimorio) {
        List<String> opcoes = new ArrayList<>(
            br.com.dantesrpg.model.util.HabilidadeFactory.getNomesDisponiveis()
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(null, opcoes);
        dialog.setTitle("Escrever no Grimório");
        dialog.setHeaderText("Escolha uma magia para adicionar:");
        dialog.setContentText("Magia:");

        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");
        dialog
            .getDialogPane()
            .lookup(".label")
            .setStyle("-fx-text-fill: white;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nomeMagia -> {
            Habilidade novaMagia =
                br.com.dantesrpg.model.util.HabilidadeFactory.criarHabilidadePorNome(
                    nomeMagia
                );
            if (novaMagia != null) {
                boolean aprendeu = grimorio.aprenderMagia(novaMagia);
                if (aprendeu) {
                    atualizarTudo();
                }
            }
        });
    }

    // =============================================
    // === EQUIPAR / DESEQUIPAR
    // =============================================

    private void equiparItemDoInventario(Item item) {
        if (item == null || jogadorSelecionado == null) return;

        Inventario inv = jogadorSelecionado.getInventario();

        if (item instanceof Arma) {
            Arma novaArma = (Arma) item;
            if (!jogadorSelecionado.equiparArma(novaArma)) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Wielding insuficiente");
                alert.setHeaderText(
                    "Nao foi possivel equipar " + novaArma.getNome()
                );
                alert.setContentText(
                    "Wielding livre: " +
                        jogadorSelecionado.getWieldingDisponivel() +
                        " | arma exige: " +
                        novaArma.getWielding()
                );
                alert.showAndWait();
                return;
            }
            inv.removerItem(novaArma);
        } else if (item instanceof Armadura) {
            Armadura armaduraAntiga = jogadorSelecionado.getArmaduraEquipada();
            if (armaduraAntiga != null) inv.adicionarItem(armaduraAntiga);
            jogadorSelecionado.setArmaduraEquipada((Armadura) item);
            inv.removerItem(item);
        } else if (item instanceof Amuleto) {
            Amuleto novoAmuleto = (Amuleto) item;
            Amuleto slot1 = jogadorSelecionado.getAmuleto1();
            Amuleto slot2 = jogadorSelecionado.getAmuleto2();

            if (slot1 == null) {
                jogadorSelecionado.setAmuleto1(novoAmuleto);
                inv.removerItem(novoAmuleto);
            } else if (slot2 == null) {
                jogadorSelecionado.setAmuleto2(novoAmuleto);
                inv.removerItem(novoAmuleto);
            } else {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Slots de Amuleto Cheios");
                alert.setHeaderText(
                    "Onde você deseja equipar " + novoAmuleto.getNome() + "?"
                );
                alert.setContentText("Escolha qual amuleto substituir:");

                ButtonType btnSlot1 = new ButtonType(
                    "Slot 1:\n" + slot1.getNome()
                );
                ButtonType btnSlot2 = new ButtonType(
                    "Slot 2:\n" + slot2.getNome()
                );
                ButtonType btnCancelar = new ButtonType(
                    "Cancelar",
                    ButtonData.CANCEL_CLOSE
                );

                alert.getButtonTypes().setAll(btnSlot1, btnSlot2, btnCancelar);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == btnSlot1) {
                        inv.adicionarItem(slot1);
                        jogadorSelecionado.setAmuleto1(novoAmuleto);
                        inv.removerItem(novoAmuleto);
                    } else if (result.get() == btnSlot2) {
                        inv.adicionarItem(slot2);
                        jogadorSelecionado.setAmuleto2(novoAmuleto);
                        inv.removerItem(novoAmuleto);
                    }
                }
            }
        } else {
            return;
        }

        atualizarTudo();
    }

    @FXML
    private void onDesequiparArmaClick() {
        onDesequiparArmaClick(jogadorSelecionado.getArmaEquipada());
    }

    private void onDesequiparArmaClick(Arma arma) {
        if (arma != null) {
            jogadorSelecionado.getInventario().adicionarItem(arma);
            jogadorSelecionado.desequiparArma(arma);
            atualizarTudo();
        }
    }

    @FXML
    private void onDesequiparArmaduraClick() {
        Armadura armadura = jogadorSelecionado.getArmaduraEquipada();
        if (armadura != null) {
            jogadorSelecionado.getInventario().adicionarItem(armadura);
            jogadorSelecionado.setArmaduraEquipada(null);
            atualizarTudo();
        }
    }

    @FXML
    private void onDesequiparAmuleto1Click() {
        Amuleto am = jogadorSelecionado.getAmuleto1();
        if (am != null) {
            jogadorSelecionado.getInventario().adicionarItem(am);
            jogadorSelecionado.setAmuleto1(null);
            atualizarTudo();
        }
    }

    @FXML
    private void onDesequiparAmuleto2Click() {
        Amuleto am = jogadorSelecionado.getAmuleto2();
        if (am != null) {
            jogadorSelecionado.getInventario().adicionarItem(am);
            jogadorSelecionado.setAmuleto2(null);
            atualizarTudo();
        }
    }

    @FXML
    private void onDoarItemClick() {
        if (jogadorSelecionado == null || mainController == null) return;

        Item itemSelecionado = inventarioListView
            .getSelectionModel()
            .getSelectedItem();
        if (itemSelecionado == null) {
            Alert aviso = new Alert(
                AlertType.INFORMATION,
                "Selecione um item do inventário para doar."
            );
            aviso.setHeaderText(null);
            aviso.showAndWait();
            return;
        }

        List<Personagem> destinatariosPossiveis = todosOsJogadores
            .stream()
            .filter(p -> p != jogadorSelecionado)
            .collect(Collectors.toList());

        if (destinatariosPossiveis.isEmpty()) {
            Alert aviso = new Alert(
                AlertType.INFORMATION,
                "Não há outros jogadores para receber o item."
            );
            aviso.setHeaderText(null);
            aviso.showAndWait();
            return;
        }

        List<String> nomesAlvos = destinatariosPossiveis
            .stream()
            .map(Personagem::getNome)
            .collect(Collectors.toList());
        ChoiceDialog<String> dlgAlvo = new ChoiceDialog<>(
            nomesAlvos.get(0),
            nomesAlvos
        );
        dlgAlvo.setTitle("Doar Item");
        dlgAlvo.setHeaderText(
            "Doar \"" + itemSelecionado.getNome() + "\" para:"
        );
        dlgAlvo.setContentText("Destinatário:");
        Optional<String> alvoNomeOpt = dlgAlvo.showAndWait();
        if (!alvoNomeOpt.isPresent()) return;
        Personagem alvo = destinatariosPossiveis
            .stream()
            .filter(p -> p.getNome().equals(alvoNomeOpt.get()))
            .findFirst()
            .orElse(null);
        if (alvo == null) return;

        String tipo = itemSelecionado.getTipo();
        int quantidadeDisponivel = jogadorSelecionado
            .getInventario()
            .getItensAgrupados()
            .getOrDefault(tipo, 0);
        if (quantidadeDisponivel <= 0) return;

        int qtd = 1;
        if (quantidadeDisponivel > 1) {
            TextInputDialog dlgQtd = new TextInputDialog("1");
            dlgQtd.setTitle("Doar Item");
            dlgQtd.setHeaderText(
                "Quantos \"" +
                    itemSelecionado.getNome() +
                    "\" doar? (máx " +
                    quantidadeDisponivel +
                    ")"
            );
            dlgQtd.setContentText("Quantidade:");
            Optional<String> qtdOpt = dlgQtd.showAndWait();
            if (!qtdOpt.isPresent()) return;
            try {
                qtd = Integer.parseInt(qtdOpt.get().trim());
            } catch (NumberFormatException ex) {
                qtd = 1;
            }
            qtd = Math.max(1, Math.min(qtd, quantidadeDisponivel));
        }

        int ocGrau = jogadorSelecionado
            .getInventario()
            .getOverclockDoItem(tipo);
        Inventario origem = jogadorSelecionado.getInventario();
        Inventario destino = alvo.getInventario();

        for (int i = 0; i < qtd; i++) {
            Item modelo = mainController.getItem(tipo);
            if (modelo == null) break;
            if (ocGrau > 0) modelo.setGrauOverclock(ocGrau);
            origem.removerItem(modelo);
            destino.adicionarItem(modelo);
        }

        mainController.salvarPersonagem(jogadorSelecionado);
        mainController.salvarPersonagem(alvo);

        atualizarTudo();

        Alert confirm = new Alert(
            AlertType.INFORMATION,
            qtd +
                "x \"" +
                itemSelecionado.getNome() +
                "\" doado(s) para " +
                alvo.getNome() +
                "."
        );
        confirm.setHeaderText(null);
        confirm.showAndWait();
    }

    @FXML
    private void onSalvarClick() {
        if (jogadorSelecionado == null || mainController == null) return;
        mainController.salvarPersonagem(jogadorSelecionado);
    }

    @FXML
    private void onResetarClick() {
        if (jogadorSelecionado == null || mainController == null) return;

        String nomeArquivo = jogadorSelecionado
            .getJsonFileName()
            .replace(".json", "");
        Personagem personagemRecarregado = mainController.recarregarPersonagem(
            nomeArquivo
        );
        if (personagemRecarregado != null) {
            todosOsJogadores.remove(jogadorSelecionado);
            todosOsJogadores.add(personagemRecarregado);
            selecionarJogador(personagemRecarregado);
            popularListaJogadores();
        }
    }

    // =============================================
    // === GRÁFICO RADAR
    // =============================================

    private void desenharGraficoRadar() {
        radarChartPlaceholder.getChildren().clear();

        if (jogadorSelecionado == null) return;

        double width = radarChartPlaceholder.getWidth();
        double height = radarChartPlaceholder.getHeight();

        if (width <= 0) width = 280;
        if (height <= 0) height = 220;

        double centerX = width / 2;
        double centerY = height / 2;

        // Escala dinâmica: encontrar o maior valor para ajustar
        int maxValor = 20;
        for (int v : jogadorSelecionado.getAtributosFinais().values()) {
            if (v > maxValor) maxValor = v;
        }
        maxValor = Math.max(maxValor, 20); // Mínimo 20
        double escalaMax = maxValor + 2; // Margem

        double maxRadius = (Math.min(width, height) / 2) * 0.65;

        int numAtributos = Atributo.values().length;
        double angleStep = (2 * Math.PI) / numAtributos;

        // Teia de fundo
        int[] ranksValues = { 5, 10, 15, 20 };
        if (maxValor > 20) {
            ranksValues = new int[] { 5, 10, 15, 20, maxValor };
        }

        for (int val : ranksValues) {
            Polygon rankPoly = new Polygon();
            rankPoly.setFill(Color.TRANSPARENT);
            rankPoly.setStroke(Color.web("#2a2a3a"));
            if (val == 20) {
                rankPoly.setStrokeWidth(2);
                rankPoly.setStroke(Color.web("#3a3a4a"));
            } else {
                rankPoly.getStrokeDashArray().addAll(4.0, 4.0);
            }

            for (int i = 0; i < numAtributos; i++) {
                double angle = i * angleStep - Math.PI / 2;
                double r = (val / escalaMax) * maxRadius;
                double x = centerX + Math.cos(angle) * r;
                double y = centerY + Math.sin(angle) * r;
                rankPoly.getPoints().addAll(x, y);
            }
            radarChartPlaceholder.getChildren().add(rankPoly);
        }

        // Eixos e labels
        for (int i = 0; i < numAtributos; i++) {
            double angle = i * angleStep - Math.PI / 2;
            double x = centerX + Math.cos(angle) * maxRadius;
            double y = centerY + Math.sin(angle) * maxRadius;

            Line axis = new Line(centerX, centerY, x, y);
            axis.setStroke(Color.web("#1a1a2a"));
            radarChartPlaceholder.getChildren().add(axis);

            // Label com fonte Oxanium
            Atributo atr = Atributo.values()[i];
            String nomeAtr = atr.name().substring(0, 3);
            int valorAtr = jogadorSelecionado
                .getAtributosFinais()
                .getOrDefault(atr, 1);

            double labelX = centerX + Math.cos(angle) * (maxRadius + 22);
            double labelY = centerY + Math.sin(angle) * (maxRadius + 22);

            Text text = new Text(nomeAtr + " " + valorAtr);
            text.setFill(Color.web("#a0a0b0"));
            text.setFont(Font.font("Oxanium", FontWeight.BOLD, 9));
            text.setX(labelX - text.getLayoutBounds().getWidth() / 2);
            text.setY(labelY + text.getLayoutBounds().getHeight() / 4);

            radarChartPlaceholder.getChildren().add(text);
        }

        // Polígono dos atributos
        Polygon statsPoly = new Polygon();
        statsPoly.setFill(Color.web("#e9456030"));
        statsPoly.setStroke(Color.web("#e94560"));
        statsPoly.setStrokeWidth(2);

        Map<Atributo, Integer> atributosAtuais =
            jogadorSelecionado.getAtributosFinais();

        for (int i = 0; i < numAtributos; i++) {
            Atributo atr = Atributo.values()[i];
            int valor = atributosAtuais.getOrDefault(atr, 1);
            double valorVisual = Math.max(0, valor);

            double angle = i * angleStep - Math.PI / 2;
            double r = (valorVisual / escalaMax) * maxRadius;

            double x = centerX + Math.cos(angle) * r;
            double y = centerY + Math.sin(angle) * r;

            statsPoly.getPoints().addAll(x, y);

            Circle dot = new Circle(x, y, 3, Color.web("#e94560"));
            dot.setStroke(Color.web("#ff6b8a"));
            dot.setStrokeWidth(1);
            radarChartPlaceholder.getChildren().add(dot);
        }

        radarChartPlaceholder.getChildren().add(statsPoly);

        // Atualizar label de ranks
        atualizarLabelRanks();
    }

    private void atualizarLabelRanks() {
        if (labelRanks == null || jogadorSelecionado == null) return;

        StringBuilder sb = new StringBuilder();
        for (Atributo atr : Atributo.values()) {
            int val = jogadorSelecionado
                .getAtributosFinais()
                .getOrDefault(atr, 1);
            String rank = getRank(val);
            if (sb.length() > 0) sb.append("  ");
            sb.append(atr.name().substring(0, 3)).append(":").append(rank);
        }
        labelRanks.setText(sb.toString());
    }

    // =============================================
    // === GERADOR DE TOOLTIP (completo)
    // =============================================

    private String gerarTextoDetalhesItem(Item item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getNomeComOverclock()).append("\n");
        sb.append(item.getDescricao()).append("\n\n");

        Map<Atributo, Integer> modAtr = null;
        Map<String, Double> modStatus = null;

        if (item instanceof Armadura) {
            Armadura a = (Armadura) item;
            sb.append("Defesa: ").append(a.getArmaduraBase()).append("\n");
            if (
                a.getNomeEfeitoOnDamageTaken() != null &&
                !a.getNomeEfeitoOnDamageTaken().isEmpty()
            ) {
                sb.append("Efeito ao Tomar Dano: ")
                    .append(a.getNomeEfeitoOnDamageTaken())
                    .append(" (")
                    .append((int) (a.getChanceEfeitoOnDamageTaken() * 100))
                    .append("% para ")
                    .append(a.getAlvoEfeitoOnDamageTaken())
                    .append(")\n");
            }
            if (!a.getHabilidadesConcedidasNomes().isEmpty()) {
                sb.append("\nHabilidades Concedidas:\n");
                for (String hab : a.getHabilidadesConcedidasNomes()) {
                    sb.append("  * ").append(hab).append("\n");
                }
            }
            modAtr = a.getModificadoresDeAtributo();
            modStatus = a.getModificadoresStatus();
        } else if (item instanceof Amuleto) {
            Amuleto a = (Amuleto) item;
            if (a.getArmaduraBonus() > 0) sb.append("Defesa: +")
                .append(a.getArmaduraBonus())
                .append("\n");
            if (!a.getHabilidadesConcedidasNomes().isEmpty()) {
                sb.append("\nHabilidades Concedidas:\n");
                for (String hab : a.getHabilidadesConcedidasNomes()) {
                    sb.append("  * ").append(hab).append("\n");
                }
            }
            modAtr = a.getModificadoresDeAtributo();
            modStatus = a.getModificadoresStatus();
        } else if (item instanceof Arma) {
            Arma a = (Arma) item;
            sb.append("Dano: ").append(a.getDanoBase()).append("\n");
            sb.append("Wielding: ").append(a.getWielding()).append("\n");
            if (a.getPenetracaoArmadura() > 0.0) {
                sb.append(String.format("Penetração de armadura: %.0f%%\n", a.getPenetracaoArmadura() * 100.0));
            }
            sb.append("Atributo: ")
                .append(a.getAtributoMultiplicador())
                .append("\n");
            modAtr = a.getModificadoresDeAtributo();
            modStatus = a.getModificadoresStatus();
            if (!a.getHabilidadesConcedidasNomes().isEmpty()) {
                sb.append("\nHabilidades:\n");
                for (String hab : a.getHabilidadesConcedidasNomes()) {
                    sb.append("  ").append(hab).append("\n");
                }
            }
        }

        if (modAtr != null && !modAtr.isEmpty()) {
            modAtr.forEach((atr, val) -> {
                sb.append(atr.name().substring(0, 3))
                    .append(": ")
                    .append(val > 0 ? "+" : "")
                    .append(val)
                    .append("\n");
            });
        }

        if (modStatus != null && !modStatus.isEmpty()) {
            modStatus.forEach((key, val) -> {
                String nomeAmigavel = traduzirNomeStatus(key);
                double valor = val;
                String sufixo = "";
                if (
                    key.contains("PERCENTUAL") ||
                    key.contains("MODIFICADOR") ||
                    key.contains("CRITICA") ||
                    key.contains("CRITICO")
                ) {
                    valor = val * 100;
                    sufixo = "%";
                }
                sb.append(nomeAmigavel)
                    .append(": ")
                    .append(valor > 0 ? "+" : "")
                    .append(String.format("%.1f", valor))
                    .append(sufixo)
                    .append("\n");
            });
        }

        return sb.toString();
    }

    // =============================================
    // === RANKS
    // =============================================

    private String getRank(int pontos) {
        if (pontos >= 46) return "PP+";
        if (pontos == 45) return "PP";
        if (pontos == 44) return "PP-";
        if (pontos == 43) return "O+";
        if (pontos == 42) return "O";
        if (pontos == 41) return "O-";
        if (pontos >= 22) return "P";
        if (pontos == 21) return "SSS+";
        if (pontos == 20) return "SSS";
        if (pontos == 19) return "SS+";
        if (pontos == 18) return "SS";
        if (pontos == 17) return "S+";
        if (pontos == 16) return "S";
        if (pontos == 15) return "S-";
        if (pontos == 14) return "A+";
        if (pontos == 13) return "A";
        if (pontos == 12) return "A-";
        if (pontos == 11) return "B+";
        if (pontos == 10) return "B";
        if (pontos == 9) return "B-";
        if (pontos == 8) return "C+";
        if (pontos == 7) return "C";
        if (pontos == 6) return "C-";
        if (pontos == 5) return "D+";
        if (pontos == 4) return "D";
        if (pontos == 3) return "D-";
        if (pontos == 2) return "E+";
        return "E";
    }

    private String formatarNumero(double valor) {
        double valorArredondado = Math.round(valor * 10.0) / 10.0;
        if (valorArredondado == (long) valorArredondado) {
            return String.format("%d", (long) valorArredondado);
        } else {
            return String.format("%.1f", valorArredondado);
        }
    }

    // =============================================
    // === INSPETOR DINÂMICO DE HABILIDADES
    // =============================================

    private void mostrarPreviewHabilidade(Habilidade hab) {
        if (skillWelcomePane == null || skillPreviewPane == null) return;

        if (hab == null) {
            skillWelcomePane.setVisible(true);
            skillWelcomePane.setManaged(true);
            skillPreviewPane.setVisible(false);
            skillPreviewPane.setManaged(false);
            return;
        }

        skillWelcomePane.setVisible(false);
        skillWelcomePane.setManaged(false);
        skillPreviewPane.setVisible(true);
        skillPreviewPane.setManaged(true);

        lblSkillPreviewNome.setText(hab.getNome());
        lblSkillPreviewDetalhes.setText(hab.getTipo().name());

        // Atualizar badge de tipo
        lblSkillPreviewDetalhes.getStyleClass().clear();
        lblSkillPreviewDetalhes.getStyleClass().add("rank-badge");
        if (
            hab.getTipo() == br.com.dantesrpg.model.enums.TipoHabilidade.ATIVA
        ) {
            lblSkillPreviewDetalhes.getStyleClass().add("rank-A");
        } else {
            lblSkillPreviewDetalhes.getStyleClass().add("rank-C");
        }

        lblSkillPreviewTU.setText(hab.getCustoTU() + " TU");
        lblSkillPreviewMana.setText(hab.getCustoMana() + " MP");
        lblSkillPreviewDescricao.setText(hab.getDescricao());

        // Calcular e setar estimativa de dano
        if (hab.getMultiplicadorDeDano() <= 0) {
            lblSkillPreviewDano.setText("Suporte / Defesa");
            lblSkillPreviewDano.setStyle(
                "-fx-text-fill: #808090; -fx-font-size: 15px; -fx-font-weight: bold;"
            );
        } else {
            String faixaDano = calcularFaixaDeDano(hab);
            lblSkillPreviewDano.setText(faixaDano);
            lblSkillPreviewDano.setStyle(
                "-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;"
            );
        }

        // Desenhar área de efeito
        desenharAOEPreview(hab.getTipoAlvo(), hab.getTamanhoArea());
    }

    private void desenharAOEPreview(
        br.com.dantesrpg.model.enums.TipoAlvo tipo,
        int tamanho
    ) {
        if (gridSkillPreviewAOE == null) return;

        gridSkillPreviewAOE.getChildren().clear();

        int size = 5; // Grid de 5x5
        int centro = 2; // Índice do meio (0-indexed)

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(18, 18);
                cell.setStyle(
                    "-fx-background-color: #151522; -fx-background-radius: 3; -fx-border-color: #252538; -fx-border-width: 0.5;"
                );

                boolean afetada = false;
                boolean isCentro = r == centro && c == centro;

                int distR = r - centro;
                int distC = c - centro;

                if (tipo != null) {
                    switch (tipo) {
                        case SI_MESMO:
                            afetada = isCentro;
                            break;
                        case INDIVIDUAL:
                            afetada =
                                isCentro || (r == centro && c == centro + 1);
                            break;
                        case AREA:
                        case AREA_QUADRADA:
                            afetada =
                                Math.abs(distR) <= tamanho &&
                                Math.abs(distC) <= tamanho;
                            break;
                        case AREA_CIRCULAR:
                            afetada =
                                distR * distR + distC * distC <=
                                tamanho * tamanho + 0.5;
                            break;
                        case LINHA:
                            afetada =
                                r == centro && distC >= 0 && distC <= tamanho;
                            break;
                        case CONE:
                            afetada =
                                distC >= 0 &&
                                distC <= tamanho &&
                                Math.abs(distR) <= distC;
                            break;
                        case EQUIPE:
                        case MULTIPLOS:
                            afetada =
                                isCentro ||
                                (Math.abs(distR) == 1 && Math.abs(distC) == 1);
                            break;
                    }
                }

                if (isCentro) {
                    cell.setStyle(
                        "-fx-background-color: #00f0ff; -fx-background-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,240,255,0.6), 4, 0, 0, 0);"
                    );
                } else if (afetada) {
                    cell.setStyle(
                        "-fx-background-color: #e94560; -fx-background-radius: 3; -fx-effect: dropshadow(gaussian, rgba(233,69,96,0.6), 4, 0, 0, 0);"
                    );
                }

                gridSkillPreviewAOE.add(cell, c, r);
            }
        }
    }

    private String calcularFaixaDeDano(Habilidade hab) {
        if (jogadorSelecionado == null) return "—";
        Arma arma = jogadorSelecionado.getArmaEquipada();
        if (arma == null) {
            List<Arma> armas = jogadorSelecionado.getArmasEquipadas();
            if (!armas.isEmpty()) {
                arma = armas.get(0);
            }
        }

        double danoBase = arma != null ? arma.getDanoBase() : 2.0; // Padrão soco
        Atributo atrMult =
            arma != null ? arma.getAtributoMultiplicador() : Atributo.FORCA;

        int atrValor = jogadorSelecionado
            .getAtributosFinais()
            .getOrDefault(atrMult, 1);
        int tipoDado = br.com.dantesrpg.model.util.DiceRoller.getTipoDado(
            atrValor
        );

        double multHab = hab.getMultiplicadorDeDano();
        double multBonus = 1.0 + jogadorSelecionado.getBonusDanoPercentual();
        double sorteMult = 1.0 + jogadorSelecionado.getSortePercentual();

        // Mínimo (rolagem = 1)
        double danoMin =
            danoBase * (1.0 + 0.075 * 1.0) * sorteMult * multBonus * multHab;
        // Máximo (rolagem = tipoDado)
        double danoMax =
            danoBase *
            (1.0 + 0.075 * tipoDado) *
            sorteMult *
            multBonus *
            multHab;

        // Golpe perfeito aplica multiplicador de 1.25x no maior valor do dado
        danoMax *= 1.25;

        int ticks = hab.getTicksDeDano();
        if (ticks > 1) {
            return String.format(
                "%d - %d (%dx hits)",
                (int) Math.ceil(danoMin * ticks),
                (int) Math.ceil(danoMax * ticks),
                ticks
            );
        } else {
            return String.format(
                "%d - %d",
                (int) Math.ceil(danoMin),
                (int) Math.ceil(danoMax)
            );
        }
    }

    // =============================================
    // === DOSSIES DE ORIGEM
    // =============================================

    private String criarDossieClasse() {
        if (jogadorSelecionado == null) return "";
        Classe classe = jogadorSelecionado.getClasse();
        if (classe == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("CLASSE: ")
            .append(classe.getNome().toUpperCase())
            .append("\n");
        sb.append("=========================================\n\n");
        sb.append(classe.getDescricao()).append("\n\n");

        sb.append("MODIFICADORES DE ATRIBUTO:\n");
        Map<Atributo, Integer> mods = classe.getModificadoresDeAtributo();
        if (mods != null && !mods.isEmpty()) {
            mods.forEach((atr, val) -> {
                sb.append("  * ")
                    .append(atr.name())
                    .append(": ")
                    .append(val > 0 ? "+" : "")
                    .append(val)
                    .append("\n");
            });
        } else {
            sb.append("  * Nenhum modificador de atributo.\n");
        }
        sb.append("\n");

        sb.append("HABILIDADES DA CLASSE:\n");
        // Hack elegante: instanciar um dummy de nível 99 para obter todas as técnicas registradas
        Personagem dummy = new Personagem(
            "Dummy",
            null,
            classe,
            99,
            new java.util.HashMap<>(),
            100,
            10
        );
        List<Habilidade> todas = classe.getHabilidades(dummy);

        if (todas != null && !todas.isEmpty()) {
            for (Habilidade hab : todas) {
                boolean desbloqueada =
                    jogadorSelecionado.getNivel() >= hab.getNivelNecessario();
                sb.append(desbloqueada ? "  [x] " : "  [ ] ");
                sb.append(hab.getNome());
                if (!desbloqueada) {
                    sb.append(" (Bloqueado - Requer Nv. ")
                        .append(hab.getNivelNecessario())
                        .append(")");
                } else {
                    sb.append(" (Nv. ")
                        .append(hab.getNivelNecessario())
                        .append(")");
                }
                sb.append("\n    L> ")
                    .append(hab.getDescricao())
                    .append("\n\n");
            }
        } else {
            sb.append("  * Nenhuma habilidade listada.\n");
        }

        return sb.toString();
    }

    private String criarDossieRaca() {
        if (jogadorSelecionado == null) return "";
        Raça raca = jogadorSelecionado.getRaca();
        if (raca == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("RACA: ").append(raca.getNome().toUpperCase()).append("\n");
        sb.append("----------------------------------\n\n");
        sb.append("EFEITO:\n");
        sb.append("  L> ").append(raca.getDescricaoPassiva()).append("\n\n");

        // Verificar transformação V2
        String nomeV2 = raca.getNomeV2();
        if (nomeV2 != null) {
            sb.append("DESPERTAR RACIAL (V2):\n");
            boolean desbloqueada = raca.isV2();
            if (desbloqueada) {
                sb.append("  [x] ").append(nomeV2).append(" (Desbloqueado)\n");
                sb.append("    L> ")
                    .append(raca.getDescricaoPassiva())
                    .append("\n\n");
            } else {
                sb.append("  [ ] ").append(nomeV2).append(" (Bloqueado)\n");
                sb.append(
                    "    L> [Descricao oculta. Requer Desbloqueio da raça V2.]\n\n"
                );
            }
        }

        sb.append("HABILIDADES RACIAIS:\n");
        List<Habilidade> habilidadesRaciais = raca.getRacialAbilities(
            jogadorSelecionado
        );
        if (habilidadesRaciais != null && !habilidadesRaciais.isEmpty()) {
            for (Habilidade hab : habilidadesRaciais) {
                sb.append("  * ").append(hab.getNome()).append("\n");
                sb.append("    L> ").append(hab.getDescricao()).append("\n\n");
            }
        } else {
            sb.append("  * Nenhuma tecnica racial ativa.\n");
        }

        return sb.toString();
    }
}
