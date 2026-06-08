package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.map.Dominio;
import br.com.dantesrpg.model.util.ContratoDeVida;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;
import java.util.*;

/**
 * Sit in Balance — Cria "Contrato de Vida" em todos os jogadores dentro da névoa.
 * O contrato equivale a 50% da vida máxima dos jogadores.
 * O Justiceiro Cego perde 20% da sua vida máxima e ganha escudo de sangue
 * equivalente a 30% da sua vida até o final da duração da aura.
 * Custo: 4 mana, 200 TU.
 */
public class SitInBalance extends Habilidade {

	public SitInBalance() {
		super("Sit in Balance",
				"Contrato de Vida em todos na névoa (50% HP). Boss: -20% HP, +30% escudo de sangue.",
				TipoHabilidade.ATIVA, 4, 200, 1,
				TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " usa SIT IN BALANCE!");

		// Pega a duração restante da aura
		Efeito aura = conjurador.getEfeitosAtivos().get("Falsa Justiça");
		int duracaoRestante = (aura != null) ? aura.getDuracaoTURestante() : 500;

		// Busca o domínio para checar quem está dentro
		Dominio dominio = null;
		if (manager.getMainController() != null) {
			dominio = manager.getMainController().getDominio("falsa_justica");
		}

		// Aplica Contrato de Vida em todos os oponentes dentro da névoa
		for (Personagem p : estado.getCombatentes()) {
			if (!p.isAtivoNoCombate())
				continue;
			if (p.getFaccao() != null && p.getFaccao().equals(conjurador.getFaccao()))
				continue;

			boolean dentro = true;
			if (dominio != null) {
				dentro = dominio.contemPersonagem(p);
			}

			if (dentro) {
				// Contrato = 50% da vida máxima do jogador, via util central (acumula com outros).
				double valorContrato = p.getVidaMaxima() * 0.50;
				ContratoDeVida contrato = new ContratoDeVida(ContratoDeVida.FONTE_SIT_IN_BALANCE,
						valorContrato, duracaoRestante, false);
				ContratoDeVidaUtils.adicionarContrato(p, contrato);

				System.out.println(">>> CONTRATO DE VIDA: " + p.getNome()
						+ " teve sua vida máxima reduzida em " + (int) valorContrato + "!");
			}
		}

		// Boss perde 20% da vida máxima
		double custoVida = conjurador.getVidaMaxima() * 0.20;
		conjurador.setVidaAtual(Math.max(1, conjurador.getVidaAtual() - custoVida));
		System.out.println(">>> " + conjurador.getNome() + " sacrificou "
				+ (int) custoVida + " HP!");

		// Boss ganha escudo de sangue = 30% da vida máxima (até fim da aura)
		double escudo = conjurador.getVidaMaxima() * 0.30;
		conjurador.adicionarEscudoSangue(escudo);

		System.out.println(">>> " + conjurador.getNome() + " ganhou "
				+ (int) escudo + " de escudo de sangue!");

		conjurador.recalcularAtributosEstatisticas();

		if (manager.getMainController() != null)
			manager.getMainController().atualizarInterfaceTotal();
	}
}
