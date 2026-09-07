package br.com.dantesrpg;

import br.com.dantesrpg.controller.map.*;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.map.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/** Harness de contratos, sem dependência do toolkit gráfico. Execute o main explicitamente. */
public final class MapaGeometriaContractTest {
    public static void main(String[] args) {
        TileRegistry registro=TileRegistry.getInstance(); registro.load();
        EstadoMapa estado=new EstadoMapa(); estado.carregar(new TileDefinition[8][8],registro.getDefault());
        int[] notificacoes={0,0}; estado.observar(c -> notificacoes[0]++);estado.observar(c -> notificacoes[1]++);
        estado.aplicarTile(3,3,registro.getById("wall"));
        exigir(estado.isParede(3,3) && notificacoes[0]==1 && notificacoes[1]==1,"mutação única, duas notificações");
        estado.definirMarcas(2,2,List.of("movimento-alcance"));
        estado.definirMarcas(2,2,List.of("movimento-alcance"));
        exigir(notificacoes[0]==2,"marca idempotente");
        AoEShapeCalculator calc=new AoEShapeCalculator(estado.paredesParaConsulta(),8,8,(x,y)->null,List::of);
        exigir(calc.calcularCelulasMovimento(2,3,1,Map.of()).equals(Set.of(
                new CoordenadaMapa(1,3),new CoordenadaMapa(2,2),new CoordenadaMapa(2,4))),"BFS e parede");
        exigir(calc.calcularDistancia(2,3,4,3)==4,"contorno da parede");
        exigir(!calc.temLinhaDeVisao(2,3,4,3),"parede bloqueia visão");
        exigir(calc.temLinhaDeVisao(2,3,3,3),"parede de destino preserva contrato");
        exigir(calc.calcularDistancia(-1,0,1,1)==-1 && !calc.temLinhaDeVisao(1,1,99,99),"limites seguros");
        Personagem ator=personagem("Ator",1,1), grande=personagem("Alvo grande",3,3);
        grande.setTamanhoX(2);grande.setTamanhoY(3);grande.setFaccao("INIMIGO");
        Habilidade area=new Habilidade("Teste", "Teste", TipoHabilidade.ATIVA,0,0,1,TipoAlvo.AREA_QUADRADA,1,0,0,List.of()) {
            @Override public void executar(Personagem p,List<Personagem> alvos,EstadoCombate e,CombatManager m) { }
        };
        AoEShapeCalculator areaCalc=new AoEShapeCalculator(new boolean[8][8],8,8,(x,y)->null,()->List.of(ator,grande));
        exigir(areaCalc.encontrarAlvosNaForma(4,5,area,ator).equals(List.of(grande)),"interseção em extremidade do token grande");
        List<GeometriaTabuleiro.Celula> paredes=List.of(
                new GeometriaTabuleiro.Celula(15,0,"wall",true,new boolean[]{false,false,false,true},List.of()),
                new GeometriaTabuleiro.Celula(16,0,"wall",true,new boolean[]{false,false,true,false},List.of()));
        var dados=GeometriaTabuleiro.construir(paredes);
        var muro=dados.stream().filter(GeometriaTabuleiro.Dados::parede).findFirst().orElseThrow();
        exigir(muro.faces().length/6==16,"faces internas eliminadas inclusive em limite de bloco");
        exigir(muro.coordenadasFaces().size()==16,"uma coordenada de picking por triângulo");
        for(var malha:dados)for(int i=0;i<malha.faces().length;i+=2)
            exigir(malha.faces()[i]>=0 && malha.faces()[i]<malha.pontos().length/3,"índices da malha válidos");
        Set<CoordenadaMapa> forma=areaCalc.calcularForma(4,5,area,ator);
        exigir(new HashSet<>(GeometriaTabuleiro.marcacao(forma,"aoe").coordenadasFaces()).equals(forma),"prévia 3D coincide com geometria mecânica");
        System.out.println("MAPA_GEOMETRIA_CONTRACT_OK");
    }
    static Personagem personagem(String nome,int x,int y) {
        Personagem p=new Personagem();p.setNome(nome);p.setVidaMaximaBase(100);p.recalcularAtributosEstatisticas();
        p.setVidaAtual(100);p.setMovimentoRestanteTurno(5);p.setPosX(x);p.setPosY(y);p.setFaccao("JOGADOR");return p;
    }
    static void exigir(boolean condicao,String nome) { if(!condicao)throw new AssertionError(nome); }
}
