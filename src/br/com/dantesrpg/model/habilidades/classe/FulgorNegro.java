package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.AcaoMestreInput; // Import necessário
import br.com.dantesrpg.model.Arma; // Import necessário
import br.com.dantesrpg.model.CombatManager; // Import necessário
import br.com.dantesrpg.model.EstadoCombate; // Import necessário
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class FulgorNegro extends Habilidade {

	public FulgorNegro() {
		super("Fulgor Negro", "Dá um soco em falso, parando no ultimo segundo...", TipoHabilidade.ATIVA, 1, // Custo de
																											// Mana
																											// (será
																											// tratado)
				50, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.INDIVIDUAL, 1.0, // Multiplicador base (será ignorado)
				1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " prepara o " + getNome() + "...");
	}

	public void executarFulgorNegro(AcaoMestreInput input, EstadoCombate estado, CombatManager manager) {
		Personagem ator = input.getAtor();
		List<Personagem> alvos = input.getAlvos();

		int rolagemDadoAtributo = input.getResultadoDado("DADO_ATRIBUTO");
		if (rolagemDadoAtributo == -1) {
			System.err.println("Erro: Fulgor Negro sem Resultado Dado Atributo.");
			return;
		}

		boolean acertoEspecial;
		double danoCriticoBonus;

		boolean restricaoAtiva = ator.getEfeitosAtivos().containsKey("Restrição Celestial");

		if (restricaoAtiva) {
			int rolagemChanceRestricao = input.getResultadoDado("DADO_CHANCE_RESTRICAO");
			if (rolagemChanceRestricao == -1) {
				System.err.println("Erro: Fulgor Negro (Restrição) sem Resultado Dado Chance.");
				return;
			}

			acertoEspecial = (rolagemChanceRestricao >= 1); // Assume 1 ou mais = Sucesso
			System.out.println(">>> " + ator.getNome() + " tenta um Fulgor Negro (Restrição 50/50): "
					+ (acertoEspecial ? "SUCESSO" : "FALHA"));
			danoCriticoBonus = 1.75;
		} else {
			// Regras Normais
			int rolagemDadoChance = input.getResultadoDado("DADO_CHANCE_FULGOR");
			if (rolagemDadoChance == -1) {
				System.err.println("Erro: Fulgor Negro sem Resultado Dado Chance.");
				return;
			}
			System.out.println(">>> " + ator.getNome() + " tenta um Fulgor Negro!");
			System.out.println(">>> Rolagem do Fulgor Negro (1d4): " + rolagemDadoChance);
			acertoEspecial = (rolagemDadoChance >= 3);
			danoCriticoBonus = 1.00;
		}

		if (acertoEspecial) { // SUCESSO
			System.out.println(">>> SUCESSO! Fulgor Negro causa Acerto Crítico de +" + (danoCriticoBonus * 100) + "%!");

			if (!restricaoAtiva) {
				ator.setManaAtual(ator.getManaAtual() + 2);
				System.out.println(">>> Fulgor Negro recupera 2 de Mana extra!");
			}

			Arma arma = ator.getArmaEquipada();
			if (arma == null) {
				System.out.println("...mas está desarmado!");
				return;
			}

			// Dano normal (sem ST+IS)
			double danoCalculado = arma.getDanoBase() * (1 + (0.075 * rolagemDadoAtributo));
			if (ator.getBonusDanoPercentual() > 0) {
				danoCalculado *= (1.0 + ator.getBonusDanoPercentual());
			}
			if (ator.getRaca() != null) {
				Personagem alvoReferencia = alvos.isEmpty() ? null : alvos.get(0);
				danoCalculado *= ator.getRaca().getMultiplicadorBonusDanoArma(ator, arma, alvoReferencia, estado, input);
			}

			danoCalculado *= this.getMultiplicadorDeDano();

			danoCalculado *= (1 + danoCriticoBonus); // Aplica crítico especial

			int danoFinal = Math.max(0, (int) danoCalculado);

			for (Personagem alvo : alvos) {
				if (alvo == null || !alvo.isAtivoNoCombate())
					continue;

				manager.aplicarDanoAoAlvo(ator, alvo, danoFinal, false,
						br.com.dantesrpg.model.enums.TipoAcao.HABILIDADE, estado);

				if (ator.getEfeitosAtivos().containsKey("Estado Dourado")) {
					manager.aplicarBuffDanoEstadoDourado(ator);
				}
			}
			if (ator.getRaca() != null) {
				ator.getRaca().onDamageDealt(ator, alvos.get(0), danoFinal, estado, manager.getController());
				ator.getRaca().onCriticalHit(ator, alvos.get(0), estado); // É um crítico
			}
		} else {
			System.out.println(">>> FALHA! Fulgor Negro erra e não causa dano.");
		}
	}
}
