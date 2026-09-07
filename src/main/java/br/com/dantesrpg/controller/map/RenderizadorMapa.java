package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.map.CoordenadaMapa;
import br.com.dantesrpg.model.map.EstadoMapa;
import javafx.scene.Node;
import java.util.Collection;
import java.util.List;

/** Contrato visual; nenhuma operação deste contrato aplica custos ou resolve ações. */
public interface RenderizadorMapa extends AutoCloseable {
    Node getNode();
    void carregar(EstadoMapa estado);
    void atualizarCelula(CoordenadaMapa coordenada);
    void atualizarCombatentes(List<Personagem> combatentes);
    void destacar(Collection<Personagem> personagens);
    void ativar(boolean ativo);
    @Override void close();
}
