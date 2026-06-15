package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.Collections;
import java.util.List;

public class CorteDeApofis extends Habilidade {

	public CorteDeApofis() {
		super("Corte de Apófis", "Desfere um golpe em cone de 135º que reduz a cura recebida dos alvos em 40%.", TipoHabilidade.ATIVA,
				2, // Custo de Mana
				120, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.CONE,
				3, // TamanhoArea (Alcance do Cone)
				1.2, // Multiplicador de Dano
				1, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public int getAnguloCone() {
		return 135;
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
		Efeito cortaCura = EffectFactory.criarEfeito("Corta Cura+", 200, 0); // 200 TU de duração
		for (Personagem alvo : alvos) {
			if (alvo != conjurador) {
				alvo.adicionarEfeito(cortaCura);

}
		}
	}
}
