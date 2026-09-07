package br.com.dantesrpg.model.map;

import br.com.dantesrpg.model.map.TerrainData.EfeitoInstance;
import br.com.dantesrpg.model.map.TerrainData.TipoTerreno;
import java.util.*;
import java.util.function.Consumer;

/** Estado compartilhado do tabuleiro. Não contém nodes, imagens ou dependências de JavaFX. */
public final class EstadoMapa {
    private TileDefinition[][] tiles = new TileDefinition[0][0];
    private boolean[][] paredes = new boolean[0][0];
    private TipoTerreno[][] terrenos = new TipoTerreno[0][0];
    private EfeitoInstance[][] efeitos = new EfeitoInstance[0][0];
    private final Map<CoordenadaMapa, List<String>> marcas = new HashMap<>();
    private final List<Consumer<CoordenadaMapa>> ouvintes = new ArrayList<>();
    private long versao;

    public int getLargura() { return tiles.length; }
    public int getAltura() { return tiles.length == 0 ? 0 : tiles[0].length; }
    public long getVersao() { return versao; }
    public boolean contem(int x, int y) { return x >= 0 && y >= 0 && x < getLargura() && y < getAltura(); }
    public TileDefinition getTile(int x, int y) { return tiles[x][y]; }
    public TipoTerreno getTerreno(int x, int y) { return terrenos[x][y]; }
    public boolean isParede(int x, int y) { return !contem(x, y) || paredes[x][y]; }
    public EfeitoInstance getEfeito(int x, int y) { return efeitos[x][y]; }
    // Matrizes de leitura para os algoritmos legados. Mutações pertencem aos métodos abaixo.
    public boolean[][] paredesParaConsulta() { return paredes; }
    public TipoTerreno[][] terrenosParaConsulta() { return terrenos; }
    public EfeitoInstance[][] efeitosParaConsulta() { return efeitos; }
    public List<String> getMarcas(int x, int y) { return marcas.getOrDefault(new CoordenadaMapa(x, y), List.of()); }
    public void observar(Consumer<CoordenadaMapa> ouvinte) { ouvintes.add(ouvinte); }
    public void desobservar(Consumer<CoordenadaMapa> ouvinte) { ouvintes.remove(ouvinte); }

    public void carregar(TileDefinition[][] matriz, TileDefinition padrao) {
        Objects.requireNonNull(padrao, "Tile padrão ausente");
        if (matriz == null || matriz.length == 0 || matriz[0] == null || matriz[0].length == 0)
            throw new IllegalArgumentException("Mapa vazio");
        int largura = matriz.length, altura = matriz[0].length;
        for (TileDefinition[] coluna : matriz)
            if (coluna == null || coluna.length != altura) throw new IllegalArgumentException("Mapa irregular");
        tiles = new TileDefinition[largura][altura];
        paredes = new boolean[largura][altura];
        terrenos = new TipoTerreno[largura][altura];
        efeitos = new EfeitoInstance[largura][altura];
        marcas.clear();
        for (int x = 0; x < largura; x++) for (int y = 0; y < altura; y++)
            atribuirTile(x, y, matriz[x][y] == null ? padrao : matriz[x][y]);
        notificar(null);
    }

    private void atribuirTile(int x, int y, TileDefinition tile) {
        tiles[x][y] = tile;
        paredes[x][y] = !tile.isWalkable();
        terrenos[x][y] = TipoTerreno.valueOf(tile.getTerrainType());
        efeitos[x][y] = null;
        TileDefinition.EffectConfig config = tile.getEffect();
        if (config != null) {
            EfeitoInstance efeito = new EfeitoInstance(TerrainData.TipoEfeitoSolo.valueOf(config.getTipo()),
                    config.getDuracao(), config.getDano(), null);
            efeito.setPermanente(config.isPermanente());
            efeitos[x][y] = efeito;
        }
    }

    public void aplicarTile(int x, int y, TileDefinition tile) {
        if (!contem(x, y) || tile == null) return;
        atribuirTile(x, y, tile);
        notificar(new CoordenadaMapa(x, y));
    }

    public void definirEfeito(int x, int y, EfeitoInstance efeito) {
        if (!contem(x, y)) return;
        efeitos[x][y] = efeito;
        notificar(new CoordenadaMapa(x, y));
    }

    public void definirBloqueio(int x, int y, boolean bloqueado) {
        if (!contem(x, y) || paredes[x][y] == bloqueado) return;
        paredes[x][y] = bloqueado;
        notificar(new CoordenadaMapa(x, y));
    }

    public void definirMarcas(int x, int y, Collection<String> valores) {
        if (!contem(x, y)) return;
        List<String> copia = List.copyOf(new LinkedHashSet<>(valores));
        CoordenadaMapa coordenada = new CoordenadaMapa(x, y);
        if (getMarcas(x, y).equals(copia)) return;
        if (copia.isEmpty()) marcas.remove(coordenada); else marcas.put(coordenada, copia);
        notificar(coordenada);
    }

    private void notificar(CoordenadaMapa coordenada) {
        versao++;
        for (Consumer<CoordenadaMapa> ouvinte : List.copyOf(ouvintes)) ouvinte.accept(coordenada);
    }
}
