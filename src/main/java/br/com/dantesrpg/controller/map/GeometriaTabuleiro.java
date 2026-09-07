package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.map.CoordenadaMapa;
import java.util.*;

/** Construção de malhas em memória, executável sem toolkit JavaFX e fora da thread de UI. */
public final class GeometriaTabuleiro {
    public static final int TAMANHO_BLOCO = 16;
    public record Celula(int x, int y, String material, boolean parede, boolean[] vizinhos,
            List<String> marcas) { }
    public record Dados(String material, boolean parede, float[] pontos, int[] faces,
            List<CoordenadaMapa> coordenadasFaces) { }

    public static List<Dados> construir(List<Celula> celulas) {
        Map<String, Construtor> grupos = new LinkedHashMap<>();
        for (Celula c : celulas) {
            if (Thread.currentThread().isInterrupted()) return List.of();
            CoordenadaMapa coord = new CoordenadaMapa(c.x(), c.y());
            Construtor chao = grupos.computeIfAbsent(c.material(), k -> new Construtor(k, false));
            float x = c.x(), z = c.y();
            chao.quad(coord, x+.015f, 0, z+.015f, x+.985f, 0, z+.015f,
                    x+.985f, 0, z+.985f, x+.015f, 0, z+.985f);
            if (c.parede()) {
                Construtor muro = grupos.computeIfAbsent("parede:"+c.material(), k -> new Construtor(k, true));
                muro.quad(coord, x, -1, z, x+1, -1, z, x+1, -1, z+1, x, -1, z+1);
                if (!c.vizinhos()[0]) muro.quad(coord, x, 0, z, x+1, 0, z, x+1, -1, z, x, -1, z);
                if (!c.vizinhos()[1]) muro.quad(coord, x+1, 0, z+1, x, 0, z+1, x, -1, z+1, x+1, -1, z+1);
                if (!c.vizinhos()[2]) muro.quad(coord, x, 0, z+1, x, 0, z, x, -1, z, x, -1, z+1);
                if (!c.vizinhos()[3]) muro.quad(coord, x+1, 0, z, x+1, 0, z+1, x+1, -1, z+1, x+1, -1, z);
            }
            int camada = 0;
            for (String marca : c.marcas()) {
                Construtor overlay = grupos.computeIfAbsent("marca:"+marca, k -> new Construtor(k, false));
                float altura = -.012f - .003f * camada++;
                overlay.quad(coord, x+.05f, altura, z+.05f, x+.95f, altura, z+.05f,
                        x+.95f, altura, z+.95f, x+.05f, altura, z+.95f);
            }
        }
        return grupos.values().stream().map(Construtor::finalizar).toList();
    }

    public static Dados marcacao(Collection<CoordenadaMapa> celulas, String nome) {
        Construtor c = new Construtor(nome, false);
        for (CoordenadaMapa p : celulas) {
            float x=p.x(), z=p.y();
            c.quad(p,x+.08f,-.045f,z+.08f,x+.92f,-.045f,z+.08f,x+.92f,-.045f,z+.92f,x+.08f,-.045f,z+.92f);
        }
        return c.finalizar();
    }

    private static final class Construtor {
        final String material;
        final boolean parede;
        final List<Float> pontos = new ArrayList<>();
        final List<Integer> faces = new ArrayList<>();
        final List<CoordenadaMapa> coords = new ArrayList<>();
        Construtor(String material, boolean parede) { this.material=material; this.parede=parede; }
        void quad(CoordenadaMapa coord, float... vertices) {
            int inicio=pontos.size()/3;
            for (float v : vertices) pontos.add(v);
            for (int indice : new int[]{0,1,2,0,2,3}) { faces.add(inicio+indice); faces.add(indice); }
            coords.add(coord); coords.add(coord);
        }
        Dados finalizar() {
            float[] p = new float[pontos.size()]; int[] f = new int[faces.size()];
            for(int i=0;i<p.length;i++) p[i]=pontos.get(i);
            for(int i=0;i<f.length;i++) f[i]=faces.get(i);
            return new Dados(material,parede,p,f,List.copyOf(coords));
        }
    }
    private GeometriaTabuleiro() { }
}
