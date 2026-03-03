package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class PresencaDoZero extends Habilidade {

	public PresencaDoZero() {
		super("Presença do Zero", "Emite uma aura de 6x6 que enfraquece inimigos (-50% Dano, +30% Dano Recebido).",
				TipoHabilidade.ATIVA, 1, 80, 1, TipoAlvo.SI_MESMO, 6, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> Zeraphon emana o VAZIO!");

		// Efeito "Marker" no Zeraphon para o CombatManager saber que a aura está ativa
		Efeito auraMarker = new Efeito("Aura do Zero", TipoEfeito.BUFF, 9999, null, 0, 0);
		conjurador.adicionarEfeito(auraMarker);

		// Força atualização imediata
		manager.atualizarAuras(estado);
	}
}