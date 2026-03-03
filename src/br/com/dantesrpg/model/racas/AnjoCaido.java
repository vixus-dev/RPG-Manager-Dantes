package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.model.*;
import java.util.Collections;
import java.util.List;

public class AnjoCaido extends Raça {

	@Override
	public String getNome() {
		return "Anjo-Caido";
	}

	@Override
	public String getDescricaoPassiva() {
		return "Equilibrium: Devolve dano sofrido baseado no nível.";
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList();
	}

	@Override
	public void onDamageTaken(Personagem personagem, Personagem atacante, double danoRecebido, EstadoCombate estado,
			CombatManager manager) {
		// Só reflete se tiver atacante e não for dano de efeito (atacante != null)
		if (atacante != null && atacante != personagem && danoRecebido > 0) {

			int nivel = personagem.getNivel();
			// 1 acúmulo por nível. 1º acúmulo = 70%. Cada extra = +20%.
			// Fórmula: 70 + ( (Nivel - 1) * 20 )
			double porcentagemReflexao = 0.70 + ((nivel - 1) * 0.20);

			int danoRefletido = (int) (danoRecebido * porcentagemReflexao);

			if (danoRefletido > 0) {
				System.out
						.println(">>> EQUILIBRIUM (" + (int) (porcentagemReflexao * 100) + "%): " + personagem.getNome()
								+ " reflete " + danoRefletido + " de dano em " + atacante.getNome() + "!");

				// Aplica dano direto no atacante (tipo REACAO para evitar loops infinitos de reflexão)
				manager.aplicarDanoAoAlvo(personagem, atacante, danoRefletido, true,
						br.com.dantesrpg.model.enums.TipoAcao.REACAO_FANTASMA, estado);
			}
		}
	}
}