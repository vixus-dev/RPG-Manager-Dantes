package br.com.dantesrpg.model.habilidades.raciais;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class Transformar extends Habilidade {

	public Transformar() {
		super("Transformar", "Consome 5 acúmulos para liberar a Forma Lupina: +100% HP máxima, +10% dano por acúmulo.",
				TipoHabilidade.ATIVA, 0, 0, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.Lobisomem) {
			br.com.dantesrpg.model.racas.Lobisomem lobo = (br.com.dantesrpg.model.racas.Lobisomem) conjurador
					.getRaca();

			if (lobo.podeTransformar()) {
				lobo.ativarTransformacao(conjurador);
			} else {
				if (lobo.isTransformed()) {
					System.out.println(">>> Falha: Já está transformado.");
				} else {
					System.out.println(">>> Falha: Precisa de 5 acúmulos para transformar.");
				}
			}
		}
	}
}
