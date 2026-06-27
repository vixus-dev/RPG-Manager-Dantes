package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ArremessoDeSol extends Habilidade {

    public ArremessoDeSol() {
        super("Arremesso de Sol", "Lança um projétil solar que explode em uma área quadrada causando dano massivo.",
                TipoHabilidade.ATIVA,
                2,   // Custo de Mana
                120, // Custo de TU
                1,   // Nível Necessário
                TipoAlvo.AREA_QUADRADA, // Formato da área
                3,   // Tamanho da área (3x3)
                2.25, // Multiplicador de dano base
                1,   // Ticks de dano
                Collections.emptyList());
    }

    @Override
    public int getAlcanceMaximo() {
        return 6;
    }

    @Override
    public boolean afetaAliados() {
        return false;
    }

    // --- LÓGICA DE EXECUÇÃO ---
    @Override
    public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
        System.out.println(">>> " + conjurador.getNome() + " usa Arremesso de Sol!");
    }
}