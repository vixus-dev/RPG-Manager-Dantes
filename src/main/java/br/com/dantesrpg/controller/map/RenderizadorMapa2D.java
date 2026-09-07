package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.map.CoordenadaMapa;
import br.com.dantesrpg.model.map.EstadoMapa;
import javafx.scene.Node;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** Adaptador do desenho 2D existente, mantido como alternativa de compatibilidade. */
public final class RenderizadorMapa2D implements RenderizadorMapa {
    private final Node raiz;
    private final Consumer<List<Personagem>> desenhar;
    private final Consumer<List<Personagem>> destacar;
    private List<Personagem> personagens = List.of();
    private List<Personagem> alvos = List.of();
    private boolean ativo;

    public RenderizadorMapa2D(Node raiz, Consumer<List<Personagem>> desenhar,
            Consumer<List<Personagem>> destacar) {
        this.raiz = raiz; this.desenhar = desenhar; this.destacar = destacar;
    }
    public Node getNode() { return raiz; }
    public void carregar(EstadoMapa estado) { }
    public void atualizarCelula(CoordenadaMapa c) { }
    public void atualizarCombatentes(List<Personagem> lista) {
        personagens = List.copyOf(lista);
        if (ativo) { desenhar.accept(personagens); destacar.accept(alvos); }
    }
    public void destacar(Collection<Personagem> lista) {
        alvos = List.copyOf(lista);
        if (ativo) destacar.accept(alvos);
    }
    public void ativar(boolean valor) {
        ativo = valor; raiz.setVisible(valor); raiz.setManaged(valor);
        if (valor) { desenhar.accept(personagens); destacar.accept(alvos); }
    }
    public void close() { personagens = List.of(); alvos = List.of(); ativar(false); }
}
