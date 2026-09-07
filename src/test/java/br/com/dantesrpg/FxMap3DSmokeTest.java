package br.com.dantesrpg;

import atlantafx.base.theme.PrimerDark;
import br.com.dantesrpg.controller.*;
import br.com.dantesrpg.controller.map.*;
import br.com.dantesrpg.controller.service.TemaAndarService;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.map.*;
import br.com.dantesrpg.model.enums.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/** QA visual e métricas de pulsos da UI (não são uma medição de apresentação da GPU). */
public final class FxMap3DSmokeTest {
    private static final CountDownLatch FIM=new CountDownLatch(1);
    private static final AtomicReference<Throwable> ERRO=new AtomicReference<>();
    private static boolean contratosRapidos, somenteVisual;
    private static Stage stage;
    private static RenderizadorMapa3D render;
    private static MapController mapa;
    private static TemaAndarService temas;
    private static Parent raiz;
    private static final Path SAIDA=Path.of("target","mapa3d-qa");

    public static void main(String[] args) throws Exception {
        contratosRapidos=Arrays.asList(args).contains("--somente-contratos");
        somenteVisual=Arrays.asList(args).contains("--somente-visual");
        Files.createDirectories(SAIDA);
        if(!contratosRapidos && !somenteVisual)Files.writeString(SAIDA.resolve("metricas.txt"),"Cenários medidos com EmbeddedMapView completo e adaptador 2D retido. Pulsos de UI, não apresentação da GPU.\n");
        Platform.startup(() -> { try { iniciar(); } catch(Throwable t){falhar(t);} });
        if(!FIM.await(180,TimeUnit.SECONDS))throw new AssertionError("Timeout do smoke 3D");
        Platform.exit(); if(ERRO.get()!=null)throw new AssertionError("Smoke 3D falhou",ERRO.get());
    }
    private static void iniciar() throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        TileRegistry.getInstance().load();
        MapaGeometriaContractTest.exigir(Platform.isSupported(ConditionalFeature.SCENE3D),"hardware 3D disponível");
        var loader=new FXMLLoader(FxMap3DSmokeTest.class.getResource("/br/com/dantesrpg/view/CombatView.fxml"));
        FakeCombate combate=new FakeCombate();
        loader.setControllerFactory(tipo -> combate);
        raiz=loader.load();
        // Exercita a composição real sem inicializar catálogos, saves ou combate da campanha.
        var setup=CombatController.class.getDeclaredMethod("setupEmbeddedMap");setup.setAccessible(true);setup.invoke(combate);
        mapa=combate.getPrimaryMap();combate.mapa=mapa;
        stage=new Stage();Scene scene=new Scene(raiz,1100,760);
        scene.getStylesheets().add(FxMap3DSmokeTest.class.getResource("/br/com/dantesrpg/view/grimorio.css").toExternalForm());
        stage.setScene(scene);stage.setTitle("Validação do mapa 3D");stage.show();
        temas=new TemaAndarService(raiz,new Pane(),new Pane());
        TileDefinition[][] tiles=new TileDefinition[16][12];
        var registro=TileRegistry.getInstance();
        for(int x=0;x<16;x++)for(int y=0;y<12;y++)
            tiles[x][y]=(x==0 || y==0 || x==15 || y==11 || x==8 && y>3 && y<9)?registro.getById("wall"):registro.getById(new String[]{"floor","floor4","grass1"}[Math.min(2,x/6)]);
        mapa.carregarMapaProcedural(tiles);
        for(int i=0;i<8;i++)combate.personagens.add(MapaGeometriaContractTest.personagem("Miniatura "+(i+1),2+i%4*3,3+i/4*4));
        combate.personagens.get(6).setPosX(9);combate.personagens.get(6).setTamanhoX(2);combate.personagens.get(6).setTamanhoY(2);
        mapa.desenharPeoes(combate.personagens);
        mapa.aplicarEfeitoNoSolo(4,4,new TerrainData.EfeitoInstance(TerrainData.TipoEfeitoSolo.ACIDO,2,3,null));
        MapaGeometriaContractTest.exigir(mapa.getEfeitoNoSolo(4,4)!=null,"efeito sincronizado");
        mapa.avancarTempoTerreno(1);MapaGeometriaContractTest.exigir(mapa.getEfeitoNoSolo(4,4)!=null,"uma janela não duplica ticks");
        mapa.avancarTempoTerreno(1);MapaGeometriaContractTest.exigir(mapa.getEfeitoNoSolo(4,4)==null,"expiração sincronizada");
        mapa.aplicarTileSomenteVisual(registro.getById("wall"),5,5);
        MapaGeometriaContractTest.exigir(mapa.isParedeem(5,5),"edição sincronizada");
        mapa.atualizarCelulaParaChao(5,5);MapaGeometriaContractTest.exigir(!mapa.isParedeem(5,5),"destruição sincronizada");
        var salvo=mapa.extrairMetadados();
        String json=new com.google.gson.Gson().toJson(salvo);
        var lido=new com.google.gson.Gson().fromJson(json,MapMetadata.class);
        MapaGeometriaContractTest.exigir(lido.getTiles().size()==16*12,"save preserva todos os tiles");
        mapa.aplicarMetadados(lido);
        MapaGeometriaContractTest.exigir(mapa.isParedeem(8,5),"reload mantém paredes no mapa");
        mapa.entrarModoSelecao(null,combate.personagens.getFirst());
        int tu=combate.personagens.getFirst().getContadorTU();
        mapa.definirVisualizacao3D(false);mapa.definirVisualizacao3D(true);
        MapaGeometriaContractTest.exigir(mapa.getAtorAtual()==combate.personagens.getFirst() && combate.personagens.getFirst().getContadorTU()==tu,"alternância mantém ação e TU");
        mapa.aplicarEfeitoNoSolo(6,6,new TerrainData.EfeitoInstance(TerrainData.TipoEfeitoSolo.FOGO,30,3,null));
        depois(1800,()->{
            testarInteracao(combate);
            if(contratosRapidos){mapa.liberarRecursos();stage.close();System.out.println("FX_MAPA_3D_CONTRATOS_OK");FIM.countDown();}
            else depois(300,()->capturarTema(0));
        });
    }
    private static void testarInteracao(FakeCombate combate) throws Exception {
        RenderizadorMapa3D renderer=(RenderizadorMapa3D)raiz.lookup(".mapa3d-raiz").getUserData();
        testarTexturasEControles(renderer);
        Personagem ator=combate.personagens.getFirst();
        Dominio dominioA=new Dominio("teste-a","Teste A",ator,5,5,3,"zona-aura-darrell");
        Dominio dominioB=new Dominio("teste-b","Teste B",ator,5,5,3,"zona-aura-darrell");
        combate.forEachMap(m -> m.registrarDominio(dominioA));
        combate.forEachMap(m -> m.registrarDominio(dominioB));
        combate.forEachMap(m -> m.removerDominio("teste-a"));
        MapaGeometriaContractTest.exigir(mapa.getEstadoMapa().getMarcas(5,5).contains("zona-aura-darrell"),"domínio sobreposto mantém marca");
        combate.forEachMap(m -> m.removerDominio("teste-b"));
        MapaGeometriaContractTest.exigir(!mapa.getEstadoMapa().getMarcas(5,5).contains("zona-aura-darrell"),"último domínio limpa marca no mapa");
        mapa.entrarModoSelecao(null,ator);
        int movimento=ator.getMovimentoRestanteTurno();
        clicar(renderer,new CoordenadaMapa(3,3));
        MapaGeometriaContractTest.exigir(ator.getPosX()==3 && ator.getMovimentoRestanteTurno()==movimento-1,"clique 3D executa movimento uma vez");
        Habilidade multi=new Habilidade("Teste áreas", "Teste",TipoHabilidade.ATIVA,0,0,1,TipoAlvo.MULTI_AOE,3,0,0,List.of()) {
            @Override public int getNumeroDeAreas(){return 2;}
            @Override public TipoAlvo getSubtipoArea(){return TipoAlvo.AREA_QUADRADA;}
            @Override public int getAlcanceMaximo(){return 10;}
            @Override public boolean afetaAliados(){return true;}
            @Override public void executar(Personagem p,List<Personagem> a,EstadoCombate e,CombatManager m){}
        };
        mapa.entrarModoSelecao(multi,ator);
        clicar(renderer,new CoordenadaMapa(5,3));
        mapa.definirVisualizacao3D(false);mapa.definirVisualizacao3D(true);
        clicar(renderer,new CoordenadaMapa(5,3));
        MapaGeometriaContractTest.exigir(combate.areas.size()==2,"duas áreas mantidas durante alternância");
        MapaGeometriaContractTest.exigir(combate.areas.get(0).alvos().equals(combate.areas.get(1).alvos()),"sobreposição preserva impactos");
        MapaGeometriaContractTest.exigir(mapa.getAtorAtual()==null,"conclusão encerra seleção");
        mapa.entrarModoSelecao(null,ator);
        mapa.getEstadoMapa().definirMarcas(5,6,List.of("zona-aura-darrell"));
        System.out.println("MAPA_3D_INTERACAO_OK");
    }
    private static void testarTexturasEControles(RenderizadorMapa3D renderer) {
        var malhas=new ArrayList<javafx.scene.shape.MeshView>();coletarMalhas(renderer.getSubScene().getRoot(),malhas);
        int texturizadas=0;
        for(var malha:malhas)if(malha.getUserData() instanceof GeometriaTabuleiro.Dados d
                && !d.material().startsWith("marca:") && !d.material().startsWith("preview:")) {
            var material=(javafx.scene.paint.PhongMaterial)malha.getMaterial();
            MapaGeometriaContractTest.exigir(material.getDiffuseMap()!=null,"textura aplicada ao material "+d.material());
            MapaGeometriaContractTest.exigir(material.getDiffuseColor().equals(javafx.scene.paint.Color.WHITE),"textura sem tingimento do tema");
            texturizadas++;
        }
        MapaGeometriaContractTest.exigir(texturizadas>=3,"variedade de pisos texturizados");
        for(var tile:TileRegistry.getInstance().getAllTiles())if(tile.getTexturePath()!=null && !tile.getTexturePath().isBlank()) {
            String caminho="/br/com/dantesrpg/textures/"+tile.getTexturePath();
            var imagem=br.com.dantesrpg.model.util.ImageCache.get(caminho,128,128);
            MapaGeometriaContractTest.exigir(imagem!=null,"recurso de textura disponível: "+tile.getId());
            MapaGeometriaContractTest.exigir(imagem==br.com.dantesrpg.model.util.ImageCache.get(caminho,128,128),"textura reutilizada pelo cache");
        }
        var stack=(StackPane)raiz.lookup("#mainCombatStack");
        Node controles=renderer.getControles();
        MapaGeometriaContractTest.exigir(controles.getParent()==stack,"comandos na camada do combate");
        for(String seletor:List.of(".player-list-gradient",".enemy-list-gradient","#playerScrollPane","#enemyScrollPane"))
            MapaGeometriaContractTest.exigir(stack.getChildren().indexOf(controles)>stack.getChildren().indexOf(raiz.lookup(seletor)),"comandos acima de "+seletor);
        mapa.definirVisualizacao3D(false);
        MapaGeometriaContractTest.exigir(!controles.isVisible() && !controles.isManaged(),"controles 3D ocultos no 2D");
        mapa.definirVisualizacao3D(true);
        MapaGeometriaContractTest.exigir(controles.isVisible(),"controles restaurados no 3D");
        System.out.println("MAPA_3D_TEXTURAS_CAMADAS_OK");
    }

    private static void clicar(RenderizadorMapa3D renderer,CoordenadaMapa c) {
        var malhas=new ArrayList<javafx.scene.shape.MeshView>();coletarMalhas(renderer.getSubScene().getRoot(),malhas);
        for(var malha:malhas)if(malha.getUserData() instanceof GeometriaTabuleiro.Dados d && !d.parede()) {
            int face=d.coordenadasFaces().indexOf(c);if(face<0)continue;
            var pick=new javafx.scene.input.PickResult(malha,new javafx.geometry.Point3D(c.x()+.5,0,c.y()+.5),1,face,null);
            var evento=new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,0,0,0,0,
                    javafx.scene.input.MouseButton.PRIMARY,1,false,false,false,false,false,false,false,true,false,true,pick);
            renderer.getSubScene().fireEvent(evento);return;
        }
        throw new AssertionError("Célula 3D não encontrada: "+c);
    }
    private static void coletarMalhas(Parent p,List<javafx.scene.shape.MeshView> resultado) {
        for(Node n:p.getChildrenUnmodifiable()){
            if(n instanceof javafx.scene.shape.MeshView m)resultado.add(m);
            if(n instanceof Parent filho)coletarMalhas(filho,resultado);
        }
    }
    private static void capturarTema(int indice) throws Exception {
        if(indice==3) {mapa.liberarRecursos();if(somenteVisual){stage.close();System.out.println("FX_MAPA_3D_VISUAL_OK");FIM.countDown();}else iniciarBenchmark(0);return;}
        EstadoAndarParty selecao=switch(indice){case 1 -> new EstadoAndarParty(AndarCampanha.ANDAR_1,1);
            case 2 -> new EstadoAndarParty(AndarCampanha.ANDAR_2,1);default -> new EstadoAndarParty(AndarCampanha.NULO,0);};
        temas.aplicarTema(new br.com.dantesrpg.model.theme.CatalogoTemasAndar().buscarPorEstado(selecao));
        if(indice==1)stage.setMaximized(true);
        if(indice==2){stage.setMaximized(false);stage.setWidth(1000);stage.setHeight(700);}
        raiz.applyCss();raiz.layout();
        depois(450,()->{salvar("tema-"+indice,raiz);capturarTema(indice+1);});
    }
    private static void iniciarBenchmark(int indice) throws Exception {
        if(indice==3){stage.close();System.out.println("FX_MAPA_3D_SMOKE_OK");FIM.countDown();return;}
        int lado=new int[]{32,64,128}[indice], quantidade=new int[]{20,50,100}[indice];
        EstadoMapa estado=new EstadoMapa();estado.carregar(new TileDefinition[lado][lado],TileRegistry.getInstance().getDefault());
        List<Personagem> lista=new ArrayList<>();
        for(int i=0;i<quantidade;i++)lista.add(MapaGeometriaContractTest.personagem("Peça "+i,2+i%(lado-4),2+i/(lado-4)*3));
        var loader=new FXMLLoader(FxMap3DSmokeTest.class.getResource("/br/com/dantesrpg/view/EmbeddedMapView.fxml"));
        Parent root=loader.load(); mapa=loader.getController();
        FakeCombate combate=new FakeCombate();combate.mapa=mapa;combate.personagens.addAll(lista);mapa.setMainController(combate);
        stage.setScene(new Scene(root,1100,760));stage.setWidth(1100);stage.setHeight(760);
        render=(RenderizadorMapa3D)root.lookup(".mapa3d-raiz").getUserData();
        long inicio=System.nanoTime();mapa.carregarMapaProcedural(new TileDefinition[lado][lado]);
        mapa.desenharPeoes(lista);
        // Substituição enquanto há tarefas em voo deve descartar a geração anterior.
        render.carregar(mapa.getEstadoMapa());

        esperarMalhas(indice,lado,quantidade,lista,inicio,0);
    }
    private static void esperarMalhas(int indice,int lado,int quantidade,List<Personagem> lista,long inicio,int tentativa) {
        depois(100,()->{
            if(render.isCarregando()) {if(tentativa>300)throw new AssertionError("Malhas não concluíram");esperarMalhas(indice,lado,quantidade,lista,inicio,tentativa+1);return;}
            double carga=(System.nanoTime()-inicio)/1e6;
            List<Double> intervalos=new ArrayList<>();
            AnimationTimer timer=new AnimationTimer(){long anterior;int frames;
                @Override public void handle(long agora){try{
                    if(anterior!=0)intervalos.add((agora-anterior)/1e6);anterior=agora;
                    render.girar(.5,0);frames++;
                    if(frames==120){stop();Collections.sort(intervalos);
                        long entrada=System.nanoTime();lista.getFirst().setPosX(4);render.atualizarCombatentes(lista);
                        render.apresentarPreview(Set.of(new CoordenadaMapa(4,3)),"aoe",List.of());
                        double comando=(System.nanoTime()-entrada)/1e6;
                        String linha=String.format(Locale.ROOT,"%dx%d/%d: carga=%.1fms pulso_p50=%.2fms pulso_p95=%.2fms comando=%.2fms heap=%dMB%n",lado,lado,quantidade,carga,intervalos.get(59),intervalos.get(112),comando,(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576);
                        System.out.print(linha);Files.writeString(SAIDA.resolve("metricas.txt"),linha,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                        salvar("benchmark-"+lado,render.getNode());mapa.liberarRecursos();iniciarBenchmark(indice+1);
                    }
                }catch(Throwable t){stop();falhar(t);}}
            };timer.start();
        });
    }
    private static void salvar(String nome,Node node) throws Exception {
        ImageIO.write(SwingFXUtils.fromFXImage(node.snapshot(null,null),null),"png",SAIDA.resolve(nome+".png").toFile());
    }
    private static void depois(int ms,Acao acao){PauseTransition p=new PauseTransition(Duration.millis(ms));p.setOnFinished(e->{try{acao.executar();}catch(Throwable t){falhar(t);}});p.play();}
    private static void falhar(Throwable t){ERRO.set(t);t.printStackTrace();if(render!=null)render.close();if(mapa!=null)mapa.liberarRecursos();if(stage!=null)stage.close();FIM.countDown();}
    @FunctionalInterface private interface Acao{void executar() throws Exception;}
    private static final class FakeCombate extends CombatController {
        final List<Personagem> personagens=new ArrayList<>(); MapController mapa;
        List<AcaoMestreInput.AreaSelecionada> areas=List.of();
        @Override public List<Personagem> getCombatentes(){return personagens;}
        @Override public boolean isPlayer(Personagem p){return personagens.indexOf(p)<4;}
        @Override public MapController getMapController(){return mapa;}
        @Override public void criarObjetoNoMapa(int x,int y,int hp){}
        @Override public void spawnarMonstro(String id,int x,int y){}
        @Override public void forEachMap(java.util.function.Consumer<MapController> acao){if(mapa!=null)acao.accept(mapa);}
        @Override public void notificarMovimentoRealizado(){}
        @Override public void verificarInteracaoTerreno(Personagem p){}
        @Override public void adicionarAlvosMultiArea(List<AcaoMestreInput.AreaSelecionada> lista){areas=List.copyOf(lista);}
        @Override public void limparSelecaoDeAlvo(){}
        @Override public void alvosIdentificadosNoMapa(List<Personagem> alvos){}
    }
}

