package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.map.Dominio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OndaDeCalor extends Habilidade {

    public OndaDeCalor() {
        super("Onda de calor", "Ativa a aura do Inefavel Sol, aplicando Meio Dia e Queimadura Inefável ao redor por 100 TU.",
                TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.SI_MESMO, 7, 0, 0, new ArrayList<>());
    }

    @Override
    public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
        
        // Aplica o efeito no próprio Sol
        Efeito efeitoAtivo = new Efeito("Onda de Calor (Ativa)", TipoEfeito.BUFF, 10000, new HashMap<>(), 0, 0);
        manager.aplicarEfeito(conjurador, efeitoAtivo);
        
        // Ativa o Domínio de Sobreposição de Chão (Heat Wave)
        if (manager.getMainController() != null) {
            String dominioId = "heatWave_" + conjurador.getNome();
            int cx = conjurador.getPosX() + (conjurador.getTamanhoX() / 2);
            int cy = conjurador.getPosY() + (conjurador.getTamanhoY() / 2);
            
            Dominio heatWave = new Dominio(dominioId, "Heat Wave", conjurador,
                    cx, cy, 15, "zona-heatwave");
            heatWave.setTexturePath("/effects/sun.png"); // Efeito PNG de chão
            heatWave.setDisputavel(false); // Não entra em disputa de domínios
            
            // Ativa o domínio no mapa (atualizando visual e permitindo sobreposição)
            manager.getDomainManager().ativarDominioNoMapa(heatWave, conjurador, estado);
        }
        
        manager.atualizarAuras(estado);
    }
}
