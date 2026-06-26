package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.map.Dominio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InefavelSol extends FantasmaNobre {

    @Override
    public String getNome() {
        return "Inefavel sol";
    }

    @Override
    public String getDescricao() {
        return "Invoca um Sol que irradia calor, causando Queimadura em inimigos e concedendo Meio Dia ao Escanor.";
    }

    @Override
    public int getCustoMana() {
        return 0;
    }

    @Override
    public int getCustoTU() {
        return 50;
    }

    @Override
    public int getCooldownTU() {
        return 100;
    }

    @Override
    public TipoAlvo getTipoAlvo() {
        return TipoAlvo.AREA_QUADRADA;
    }

    @Override
    public int getTamanhoArea() {
        return 3;
    }

    @Override
    public int getNumeroDeAlvos() {
        return 1;
    }

    @Override
    public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
                         CombatManager manager) {
        
        Personagem sol = criarSol(conjurador);
        sol.setPosX(input.getEpicentroX());
        sol.setPosY(input.getEpicentroY());
        estado.getCombatentes().add(sol);

        manager.getMainController().atualizarInterfaceTotal();
        System.out.println(">>> " + conjurador.getNome() + " invocou o Inefavel Sol!");
    }

    private Personagem criarSol(Personagem mestre) {
        Map<Atributo, Integer> stats = new HashMap<>();
        for (Atributo attr : Atributo.values()) {
            stats.put(attr, 1);
        }

        Personagem sol = new Personagem("Sol", new br.com.dantesrpg.model.racas.RaçaPlaceholder("Entidade"),
                new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, 500.0, 0);

        sol.setFaccao(mestre.getFaccao());
        sol.setTamanhoX(3);
        sol.setTamanhoY(3);
        sol.setPesoEntidade(PesoEntidade.IMOVEL);
        
        sol.adicionarPropriedade("AURA_INEFAVEL_SOL:" + mestre.getNome());
        
        sol.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.boss.OndaDeCalor());
        
        // Equipamento vazio para evitar nulls
        ArmaMelee armaNula = new ArmaMelee("Nulo", "Nulo", "", Raridade.COMUM, 0, 0, 1, Atributo.FORCA, 100, 1);
        sol.setArmaEquipada(armaNula);

        sol.recalcularAtributosEstatisticas();
        sol.setVidaAtual(sol.getVidaMaxima());

        return sol;
    }
}
