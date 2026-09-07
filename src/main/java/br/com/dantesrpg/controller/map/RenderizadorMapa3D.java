package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.elementos.ObjetoDestrutivel;
import br.com.dantesrpg.model.map.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.util.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Tabuleiro 3D incremental. O worker recebe apenas cópias dos dados de cada bloco. */
public final class RenderizadorMapa3D implements RenderizadorMapa {
    private final StackPane raiz = new StackPane();
    private final Pane legendas = new Pane();
    private final FlowPane controles = new FlowPane(4,4);
    private final Group mundo = new Group(), terreno = new Group(), pecas = new Group(), preview = new Group();
    private final Group cameraRig = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene cena;
    private final Translate centro = new Translate();
    private final Rotate giro = new Rotate(0, Rotate.Y_AXIS), inclinacao = new Rotate(-45, Rotate.X_AXIS);
    private final Map<CoordenadaMapa, Group> blocos = new HashMap<>();
    private final Set<CoordenadaMapa> sujos = new LinkedHashSet<>();
    private final Map<Personagem, Miniatura> miniaturas = new IdentityHashMap<>();
    private final Map<String, PhongMaterial> materiais = new HashMap<>();
    private final Set<Personagem> alvos = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "malhas-tabuleiro"); t.setDaemon(true); return t;
    });
    private final Map<CoordenadaMapa, Future<?>> pendentes = new HashMap<>();
    private final Map<CoordenadaMapa, Long> revisoes = new HashMap<>();
    private final BiConsumer<CoordenadaMapa, MouseEvent> clique;
    private final Consumer<CoordenadaMapa> hover;
    private final Predicate<Personagem> jogador;
    private final Supplier<Personagem> ator;
    private final Label mensagem = new Label("Selecione uma arena para começar.");
    private final Rectangle amostraFundo = new Rectangle(), amostraChao = new Rectangle();
    private EstadoMapa estado;
    private List<Personagem> combatentes = List.of();
    private long geracao;
    private boolean ativo, fechado, agendado, paredesBaixas, animar = true;
    private double distancia = 30, ancoraX, ancoraY;
    private boolean arrastou;
    private CoordenadaMapa ultimoHover;
    private final AnimationTimer legendasAnimadas = new AnimationTimer() {
        @Override public void handle(long now) {
            posicionarLegendas();
            if (miniaturas.values().stream().noneMatch(m -> m.movimento != null
                    && m.movimento.getStatus() == Animation.Status.RUNNING)) stop();
        }
    };

    public RenderizadorMapa3D(BiConsumer<CoordenadaMapa, MouseEvent> clique,
            Consumer<CoordenadaMapa> hover, Predicate<Personagem> jogador, Supplier<Personagem> ator) {
        this.clique=clique; this.hover=hover; this.jogador=jogador; this.ator=ator;
        raiz.getStyleClass().add("mapa3d-raiz"); raiz.setUserData(this);
        raiz.setMinSize(0,0);
        mundo.getChildren().addAll(terreno, pecas, preview);
        camera.setNearClip(.1); camera.setFarClip(4000); camera.setFieldOfView(40);
        cameraRig.getTransforms().addAll(centro, giro, inclinacao);
        cameraRig.getChildren().add(camera);
        PointLight luz = new PointLight(Color.color(.48,.48,.48));
        luz.setTranslateY(-100); luz.setTranslateZ(-50);
        Group cenaRaiz = new Group(mundo, cameraRig, new AmbientLight(Color.color(.55,.55,.55)), luz);
        cena = new SubScene(cenaRaiz, 800, 600, true, SceneAntialiasing.DISABLED);
        cena.setCamera(camera);
        cena.widthProperty().bind(raiz.widthProperty()); cena.heightProperty().bind(raiz.heightProperty());
        cena.widthProperty().addListener(o -> posicionarLegendas());
        cena.heightProperty().addListener(o -> posicionarLegendas());
        legendas.setMouseTransparent(true); legendas.setPickOnBounds(false);
        preview.setMouseTransparent(true);
        controles.visibleProperty().bind(raiz.visibleProperty());
        controles.managedProperty().bind(controles.visibleProperty());
        controles.getStyleClass().add("mapa3d-controles");
        controles.setMaxWidth(460); controles.setMaxHeight(Region.USE_PREF_SIZE);
        controles.setAlignment(Pos.CENTER_RIGHT);
        controles.getChildren().addAll(botao("Girar ←", () -> girar(-15,0)), botao("Girar →", () -> girar(15,0)),
                botao("Inclinar ↑", () -> girar(0,-10)), botao("Inclinar ↓", () -> girar(0,10)),
                botao("←", () -> deslocar(-1,0)), botao("→", () -> deslocar(1,0)),
                botao("↑", () -> deslocar(0,-1)), botao("↓", () -> deslocar(0,1)),
                botao("+", () -> zoom(.85)), botao("−", () -> zoom(1.15)),
                botao("Ator", this::centralizarAtor), botao("Restaurar", this::restaurar));
        ToggleButton baixas = new ToggleButton("Paredes baixas");
        baixas.setOnAction(e -> { paredesBaixas=baixas.isSelected(); ajustarParedes(); });
        ToggleButton movimento = new ToggleButton("Animações"); movimento.setSelected(true);
        movimento.setOnAction(e -> { animar=movimento.isSelected(); if (!animar) finalizarMovimentos(); });
        controles.getChildren().addAll(baixas,movimento);
        StackPane.setAlignment(controles,Pos.TOP_RIGHT); StackPane.setMargin(controles,new Insets(8));
        mensagem.getStyleClass().add("mapa3d-mensagem"); mensagem.setMouseTransparent(true);
        StackPane.setAlignment(mensagem,Pos.BOTTOM_LEFT); StackPane.setMargin(mensagem,new Insets(8));
        amostraFundo.getStyleClass().add("mapa3d-amostra-fundo");
        amostraChao.getStyleClass().add("mapa3d-amostra-chao");
        for (Rectangle r : List.of(amostraFundo, amostraChao)) { r.setManaged(false); r.setVisible(false); }
        amostraFundo.fillProperty().addListener(o -> cena.setFill(amostraFundo.getFill()));
        amostraChao.fillProperty().addListener(o -> atualizarPaleta());
        raiz.getChildren().addAll(cena,legendas,controles,mensagem,amostraFundo,amostraChao);
        configurarMouse();
        raiz.sceneProperty().addListener((o,a,b) -> { if(b==null) finalizarMovimentos(); });
    }

    private Button botao(String texto, Runnable acao) {
        Button b=new Button(texto); b.setOnAction(e -> acao.run()); b.setAccessibleText(texto);
        b.setTooltip(new Tooltip(switch(texto) {
            case "←" -> "Deslocar câmera à esquerda"; case "→" -> "Deslocar câmera à direita";
            case "↑" -> "Deslocar câmera para cima"; case "↓" -> "Deslocar câmera para baixo";
            case "+" -> "Aproximar câmera"; case "−" -> "Afastar câmera"; default -> texto;
        })); b.setAccessibleText(b.getTooltip().getText()); return b;
    }
    public Node getNode() { return raiz; }
    public Node getControles() { return controles; }
    public SubScene getSubScene() { return cena; }
    public int getQuantidadeMiniaturas() { return miniaturas.size(); }
    public boolean isCarregando() { return !pendentes.isEmpty() || !sujos.isEmpty() || agendado; }

    public void carregar(EstadoMapa novo) {
        geracao++; cancelarPendentes(); estado=novo;
        blocos.clear(); terreno.getChildren().clear(); sujos.clear(); revisoes.clear(); preview.getChildren().clear();
        for(int x=0;x<estado.getLargura();x+=16) for(int y=0;y<estado.getAltura();y+=16)
            sujos.add(new CoordenadaMapa(x/16,y/16));
        restaurar(); agendar();
    }
    public void atualizarCelula(CoordenadaMapa c) {
        if (estado==null || c==null) return;
        for(CoordenadaMapa p : List.of(c,new CoordenadaMapa(c.x()-1,c.y()),new CoordenadaMapa(c.x()+1,c.y()),
                new CoordenadaMapa(c.x(),c.y()-1),new CoordenadaMapa(c.x(),c.y()+1))) {
            if(estado.contem(p.x(),p.y())) {
                CoordenadaMapa bloco = new CoordenadaMapa(p.x()/16,p.y()/16);
                sujos.add(bloco); revisoes.merge(bloco,1L,Long::sum);
            }
        }
        agendar();
    }
    private void agendar() {
        if(!ativo || fechado || agendado || sujos.isEmpty()) return;
        agendado=true;
        Platform.runLater(() -> { agendado=false; if(ativo && !fechado) construirPendentes(); });
    }
    private void construirPendentes() {
        long versao=geracao;
        for(CoordenadaMapa bloco : List.copyOf(sujos)) {
            sujos.remove(bloco);
            Future<?> anterior=pendentes.remove(bloco); if(anterior!=null) anterior.cancel(true);
            List<GeometriaTabuleiro.Celula> copia=new ArrayList<>();
            for(int x=bloco.x()*16;x<Math.min(estado.getLargura(),bloco.x()*16+16);x++)
                for(int y=bloco.y()*16;y<Math.min(estado.getAltura(),bloco.y()*16+16);y++) {
                    List<String> marcas=new ArrayList<>(estado.getMarcas(x,y));
                    var efeito=estado.getEfeito(x,y); if(efeito!=null) marcas.add("efeito:"+efeito.getTipo().name());
                    marcas.removeIf(m -> !(m.contains("alcance") || m.startsWith("zona-") || m.startsWith("efeito:")
                            || m.equals("tempestade-agua-overlay")));
                    boolean parede=estado.isParede(x,y) && estado.getTerreno(x,y)!=TerrainData.TipoTerreno.OBJETO;
                    copia.add(new GeometriaTabuleiro.Celula(x,y,estado.getTile(x,y).getId(),parede,
                            new boolean[]{paredeVisual(x,y-1),paredeVisual(x,y+1),paredeVisual(x-1,y),paredeVisual(x+1,y)},List.copyOf(marcas)));
                }
            long revisao=revisoes.getOrDefault(bloco,0L);
            pendentes.put(bloco,executor.submit(() -> {
                try {
                    var dados=GeometriaTabuleiro.construir(copia);
                    if(Thread.currentThread().isInterrupted()) return;
                    Platform.runLater(() -> {
                        if(fechado || geracao!=versao || revisoes.getOrDefault(bloco,0L)!=revisao) return;
                        pendentes.remove(bloco);
                        Group grupo=blocos.computeIfAbsent(bloco,k -> { Group g=new Group(); terreno.getChildren().add(g); return g; });
                        grupo.getChildren().setAll(dados.stream().map(this::criarMalha).toList());
                        ajustarParedes(); mensagem.setVisible(!pendentes.isEmpty());
                    });
                } catch (RuntimeException erro) {
                    System.err.println("MAPA 3D: Falha no bloco "+bloco+": "+erro.getMessage());
                    Platform.runLater(() -> { if(geracao==versao && !fechado) {
                        pendentes.remove(bloco); mensagem.setText("Falha ao preparar cenário. Use a vista 2D."); mensagem.setVisible(true);
                    }});
                }
            }));
        }
        mensagem.setText("Preparando tabuleiro…"); mensagem.setVisible(!pendentes.isEmpty());
    }
    private boolean paredeVisual(int x,int y) {
        return estado.contem(x,y) && estado.isParede(x,y) && estado.getTerreno(x,y)!=TerrainData.TipoTerreno.OBJETO;
    }
    private MeshView criarMalha(GeometriaTabuleiro.Dados d) {
        TriangleMesh mesh=new TriangleMesh();
        mesh.getPoints().setAll(d.pontos()); mesh.getTexCoords().setAll(0,0,1,0,1,1,0,1); mesh.getFaces().setAll(d.faces());
        MeshView view=new MeshView(mesh); view.setMaterial(material(d.material())); view.setUserData(d);
        if(d.material().startsWith("marca:") || d.material().startsWith("preview:")) {
            view.setMouseTransparent(true); view.setOpacity(cor(d.material()).getOpacity());
        }
        if(d.parede()) view.setScaleY(paredesBaixas?.18:1);
        return view;
    }
    private PhongMaterial material(String chave) {
        return materiais.computeIfAbsent(chave,k -> {
            PhongMaterial m=new PhongMaterial(cor(k));
            if(!k.startsWith("marca:") && !k.startsWith("preview:")) {
                var tile=TileRegistry.getInstance().getById(k.startsWith("parede:")?k.substring(7):k);
                if(tile!=null && tile.getTexturePath()!=null && !tile.getTexturePath().isBlank()) {
                    String caminho=tile.getTexturePath();
                    if(!caminho.startsWith("/")) caminho="/br/com/dantesrpg/textures/"+caminho;
                    var imagem=br.com.dantesrpg.model.util.ImageCache.get(caminho,128,128);
                    if(imagem!=null) {
                        m.setDiffuseMap(imagem);
                        m.setDiffuseColor(Color.WHITE);
                    }
                }
            }
            return m;
        });
    }
    private Color cor(String chave) {
        if(chave.startsWith("marca:") || chave.startsWith("preview:")) {
            if(chave.contains("movimento")) return Color.color(.15,.8,.55,.48);
            if(chave.contains("colisao")) return Color.color(1,.18,.15,.8);
            if(chave.contains("empuxo") || chave.contains("epicentro")) return Color.color(1,.68,.15,.8);
            if(chave.contains("FOGO")) return Color.color(1,.3,.08,.55);
            if(chave.contains("AGUA") || chave.contains("agua")) return Color.color(.1,.55,1,.45);
            if(chave.contains("ACIDO") || chave.contains("GAS")) return Color.color(.65,1,.15,.5);
            return Color.color(.65,.35,1,.5);
        }
        boolean parede=chave.startsWith("parede:");
        var tile=TileRegistry.getInstance().getById(parede?chave.substring(7):chave);
        Color base=amostraChao.getFill() instanceof Color c?c:Color.web("#303a45");
        if(tile!=null) base=switch(TerrainData.TipoTerreno.valueOf(tile.getTerrainType())) {
            case LAVA -> Color.web("#a53b19"); case AGUA -> Color.web("#246d94");
            case SANGUE -> Color.web("#8d293e"); case ACIDO -> Color.web("#6b8426");
            case CARVAO -> Color.web("#343037"); case SAIDA -> Color.web("#30946c");
            case OBJETO -> Color.web("#795536"); default -> base;
        };
        if(base.getBrightness()>.75)base=base.interpolate(Color.web("#65746e"),.3);
        return parede?base.deriveColor(0,.75,base.getBrightness()>.65?.9:1.5,1):base;
    }
    private void atualizarPaleta() {
        materiais.forEach((chave,material) -> { if(!chave.startsWith("peca-"))material.setDiffuseColor(material.getDiffuseMap()!=null?Color.WHITE:cor(chave)); });
    }
    private void ajustarParedes() {
        for(Group bloco:blocos.values()) for(Node n:bloco.getChildren())
            if(n.getUserData() instanceof GeometriaTabuleiro.Dados d && d.parede()) n.setScaleY(paredesBaixas?.18:1);
    }

    public void atualizarCombatentes(List<Personagem> lista) {
        combatentes=lista.stream().filter(Objects::nonNull).toList();
        Set<Personagem> vivos=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Personagem p:combatentes) if(p.isAtivoNoCombate() || p instanceof ObjetoDestrutivel o && o.isIntacto()) {
            vivos.add(p); Miniatura m=miniaturas.computeIfAbsent(p,this::criarMiniatura);
            double x=p.getPosX()+p.getTamanhoX()/2.0, z=p.getPosY()+p.getTamanhoY()/2.0;
            if(m.movimento!=null) m.movimento.stop();
            if(ativo && animar && m.inicializada && (m.raiz.getTranslateX()!=x || m.raiz.getTranslateZ()!=z)) {
                m.movimento=new Timeline(new KeyFrame(Duration.millis(180),
                        new KeyValue(m.raiz.translateXProperty(),x,Interpolator.EASE_OUT),
                        new KeyValue(m.raiz.translateZProperty(),z,Interpolator.EASE_OUT)));
                m.movimento.play(); legendasAnimadas.start();
            } else { m.raiz.setTranslateX(x); m.raiz.setTranslateZ(z); }
            m.inicializada=true;
            m.raiz.setScaleX(Math.max(1,p.getTamanhoX())); m.raiz.setScaleZ(Math.max(1,p.getTamanhoY()));
            m.nome.setText(p.getNome()+"\nHP "+Math.round(p.getVidaAtual())+" · TU "+p.getContadorTU()
                    +(p.getOxigenio()<100?" · O₂ "+p.getOxigenio():""));
            m.nome.setAccessibleText(m.nome.getText());
            m.base.setMaterial(materialPeca(p,alvos.contains(p)));
        }
        for(Personagem p:List.copyOf(miniaturas.keySet())) if(!vivos.contains(p)) {
            Miniatura m=miniaturas.remove(p); if(m.movimento!=null)m.movimento.stop();
            pecas.getChildren().remove(m.raiz); legendas.getChildren().remove(m.nome);
        }
        posicionarLegendas();
    }
    private PhongMaterial materialPeca(Personagem p,boolean alvo) {
        String chave=alvo?"peca-alvo":p instanceof ObjetoDestrutivel?"peca-objeto":jogador.test(p)?"peca-jogador":"peca-inimigo";
        return materiais.computeIfAbsent(chave,k -> new PhongMaterial(alvo?Color.GOLD:
                p instanceof ObjetoDestrutivel?Color.SIENNA:jogador.test(p)?Color.TURQUOISE:Color.SALMON));
    }
    private Miniatura criarMiniatura(Personagem p) {
        Group g=new Group();
        Shape3D base=jogador.test(p)?new Cylinder(.44,.10,12):new Box(.86,.10,.86);
        base.setTranslateY(-.06); base.setMaterial(materialPeca(p,false)); g.getChildren().add(base);
        if(p instanceof ObjetoDestrutivel) {
            Box caixa=new Box(.7,.55,.7); caixa.setTranslateY(-.36); caixa.setMaterial(materialPeca(p,false)); g.getChildren().add(caixa);
        } else {
            Cylinder corpo=new Cylinder(.20,.62,6); corpo.setTranslateY(-.42); corpo.setMaterial(materialPeca(p,false));
            Sphere cabeca=new Sphere(.19,8); cabeca.setTranslateY(-.91); cabeca.setMaterial(new PhongMaterial(Color.BISQUE));
            Box braco=new Box(.60,.12,.16); braco.setTranslateY(-.6); braco.setMaterial(materialPeca(p,false));
            g.getChildren().addAll(corpo,cabeca,braco);
        }
        for(Node n:g.getChildren())n.setUserData(p);
        Label nome=new Label(); nome.getStyleClass().add("mapa3d-legenda"); nome.setMouseTransparent(true);
        nome.layoutBoundsProperty().addListener(o -> posicionarLegendas());
        pecas.getChildren().add(g); legendas.getChildren().add(nome);
        return new Miniatura(g,base,nome);
    }
    public void destacar(Collection<Personagem> lista) {
        alvos.clear(); alvos.addAll(lista);
        miniaturas.forEach((p,m) -> { m.base.setMaterial(materialPeca(p,alvos.contains(p)));
            m.nome.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),alvos.contains(p)); });
    }
    public void apresentarPreview(Collection<CoordenadaMapa> coords, String tipo, Collection<CoordenadaMapa> epicentros) {
        preview.getChildren().clear();
        if(!coords.isEmpty()) preview.getChildren().add(criarMalha(GeometriaTabuleiro.marcacao(coords,"preview:"+tipo)));
        if(!epicentros.isEmpty()) {
            MeshView marcas=criarMalha(GeometriaTabuleiro.marcacao(epicentros,"preview:epicentro"));
            marcas.setTranslateY(-.01); preview.getChildren().add(marcas);
        }
    }
    private void posicionarLegendas() {
        if(!ativo || raiz.getScene()==null)return;
        List<Rectangle2D> ocupados=new ArrayList<>();
        List<Map.Entry<Personagem,Miniatura>> ordem=new ArrayList<>(miniaturas.entrySet());
        ordem.sort(Comparator.comparingInt((Map.Entry<Personagem,Miniatura> e) -> prioridadeLegenda(e.getKey()))
                .thenComparing(e -> e.getKey().getNome()));
        for(var entrada:ordem) {
            Miniatura m=entrada.getValue();Personagem p=entrada.getKey();
            boolean importante=prioridadeLegenda(p)<3;
            if(!importante && raiz.getHeight()/distancia<=20){m.nome.setVisible(false);continue;}
            Point3D ponto=m.raiz.localToScene(new Point3D(0,-1.35,0),true);
            Point2D local=legendas.sceneToLocal(ponto.getX(),ponto.getY());
            double largura=m.nome.prefWidth(-1),altura=m.nome.prefHeight(-1);
            double x=local.getX()-largura/2,y=local.getY()-altura;
            Rectangle2D caixa=new Rectangle2D(x,y,largura+3,altura+3);
            boolean visivel=x>=0 && y>=0 && x+largura<=raiz.getWidth() && y+altura<=raiz.getHeight();
            if(visivel && !importante)for(Rectangle2D outra:ocupados)if(outra.intersects(caixa)){visivel=false;break;}
            m.nome.setVisible(visivel);
            if(visivel){m.nome.relocate(x,y);ocupados.add(caixa);}
        }
    }
    private int prioridadeLegenda(Personagem p) {
        if(p==ator.get())return 0;
        if(ultimoHover!=null && p.ocupa(ultimoHover.x(),ultimoHover.y()))return 1;
        return alvos.contains(p)?2:3;
    }
    private void configurarMouse() {
        cena.setOnMousePressed(e -> { ancoraX=e.getSceneX(); ancoraY=e.getSceneY(); arrastou=false; });
        cena.setOnMouseDragged(e -> {
            double dx=e.getSceneX()-ancoraX,dy=e.getSceneY()-ancoraY;
            if(e.isMiddleButtonDown() || e.isAltDown() && e.isPrimaryButtonDown()) {
                if(Math.abs(dx)+Math.abs(dy)>3)arrastou=true;
                if(e.isMiddleButtonDown()) deslocar(-dx*distancia/700,-dy*distancia/700);
                else girar(dx*.4,dy*.3);
                ancoraX=e.getSceneX(); ancoraY=e.getSceneY(); e.consume();
            }
        });
        cena.setOnMouseClicked(e -> {
            if(!arrastou && !e.isAltDown() && e.getButton()!=MouseButton.MIDDLE) clique.accept(coordenada(e),e);
            e.consume();
        });
        cena.setOnMouseMoved(e -> { CoordenadaMapa c=coordenada(e);
            if(!Objects.equals(c,ultimoHover)) { ultimoHover=c; hover.accept(c); posicionarLegendas(); } });
        cena.setOnMouseExited(e -> { ultimoHover=null; hover.accept(null); });
        cena.setOnScroll(e -> { if(e.isControlDown()) { zoom(e.getDeltaY()>0?.9:1.1);e.consume(); } });
    }
    public CoordenadaMapa coordenada(MouseEvent evento) {
        PickResult r=evento.getPickResult(); if(r==null || r.getIntersectedNode()==null)return null;
        Object objeto=r.getIntersectedNode().getUserData();
        if(objeto instanceof Personagem p)return new CoordenadaMapa(p.getPosX(),p.getPosY());
        if(objeto instanceof GeometriaTabuleiro.Dados d && r.getIntersectedFace()>=0
                && r.getIntersectedFace()<d.coordenadasFaces().size()) return d.coordenadasFaces().get(r.getIntersectedFace());
        return null;
    }
    public void girar(double horizontal,double vertical) {
        giro.setAngle(giro.getAngle()+horizontal);
        inclinacao.setAngle(Math.max(-80,Math.min(-30,inclinacao.getAngle()+vertical))); posicionarLegendas();
    }
    public void deslocar(double x,double z) {
        double a=Math.toRadians(giro.getAngle()); centro.setX(centro.getX()+x*Math.cos(a)+z*Math.sin(a));
        centro.setZ(centro.getZ()-x*Math.sin(a)+z*Math.cos(a)); posicionarLegendas();
    }
    public void zoom(double fator) { distancia=Math.max(4,Math.min(1500,distancia*fator));camera.setTranslateZ(-distancia);posicionarLegendas(); }
    public void restaurar() {
        if(estado!=null) { centro.setX(estado.getLargura()/2.0);centro.setZ(estado.getAltura()/2.0);
            distancia=Math.max(12,Math.max(estado.getLargura(),estado.getAltura())*1.65); }
        giro.setAngle(0); inclinacao.setAngle(-45); zoom(1);
    }
    public void centralizarAtor() { Personagem p=ator.get();if(p!=null){centro.setX(p.getPosX()+.5);centro.setZ(p.getPosY()+.5);posicionarLegendas();} }
    public void ativar(boolean valor) {
        ativo=valor;raiz.setVisible(valor);raiz.setManaged(valor);
        if(valor) { agendar(); atualizarCombatentes(combatentes);Platform.runLater(this::posicionarLegendas); }
        else { geracao++; finalizarMovimentos(); sujos.addAll(pendentes.keySet()); cancelarPendentes(); }
    }
    private void finalizarMovimentos() {
        legendasAnimadas.stop();miniaturas.forEach((p,m)->{if(m.movimento!=null)m.movimento.stop();
            m.raiz.setTranslateX(p.getPosX()+p.getTamanhoX()/2.0);m.raiz.setTranslateZ(p.getPosY()+p.getTamanhoY()/2.0);});
    }
    private void cancelarPendentes() { pendentes.values().forEach(f->f.cancel(true));pendentes.clear(); }
    public void close() { fechado=true;geracao++;finalizarMovimentos();cancelarPendentes();executor.shutdownNow();
        miniaturas.clear();materiais.clear();blocos.clear();sujos.clear();raiz.getChildren().clear(); }
    private static final class Miniatura {
        final Group raiz; final Shape3D base; final Label nome; Timeline movimento; boolean inicializada;
        Miniatura(Group raiz,Shape3D base,Label nome){this.raiz=raiz;this.base=base;this.nome=nome;}
    }
}
