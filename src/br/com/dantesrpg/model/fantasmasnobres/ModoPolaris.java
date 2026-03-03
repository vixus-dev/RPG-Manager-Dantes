package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.classe.*;
import java.util.*;

public class ModoPolaris extends FantasmaNobre {

	@Override
	public String getNome() {
		return "...And Justice For All";
	}

	@Override
	public String getDescricao() {
		return "Buffa aliados em 6x6 e troca suas habilidades para o modo Justiça por 300 TU.";
	}

	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 80;
	}

	@Override
	public int getCooldownTU() {
		return 1000;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 6;
	} // Para desenhar a aura

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		System.out.println(">>> DARRELL ATIVA: ...AND JUSTICE FOR ALL!");

		// Troca as Habilidades
		conjurador.limparHabilidadesExtras();
		conjurador.adicionarHabilidadeExtra(new AngryAgain());
		conjurador.adicionarHabilidadeExtra(new PoisonWasTheCure());
		conjurador.adicionarHabilidadeExtra(new SeekAndDestroy());
		conjurador.adicionarHabilidadeExtra(new KickTheChair());

		System.out.println(">>> Postura alterada para Justiça!");

		// Aplica Buffs Pessoais no Darrell (+10% Crit Rate/20% Dano)
		Map<String, Double> modsDarrell = new HashMap<>();
		modsDarrell.put("TAXA_CRITICA", 0.10);
		modsDarrell.put("DANO_CRITICO", 0.20);

		Efeito modoJustica = new Efeito("Modo Justiça", TipoEfeito.BUFF, 600, modsDarrell, 0, 0);
		conjurador.adicionarEfeito(modoJustica);

		// Aplica Buff nos Aliados em Área (Raio 3 = ~6x6/7x7)
		int raio = 3;
		int cx = conjurador.getPosX();
		int cy = conjurador.getPosY();

		// Definição do Buff dos Aliados (+25% Dano, +15% Redução)
		Map<String, Double> modsAliado = new HashMap<>();
		modsAliado.put("DANO_BONUS_PERCENTUAL", 0.25);
		modsAliado.put("REDUCAO_DANO_MODIFICADOR", 0.15);

		for (Personagem p : estado.getCombatentes()) {
			// Verifica se é Aliado (mesma facção) e não é o próprio Darrell
			if (p.isAtivoNoCombate() && p.getFaccao().equals(conjurador.getFaccao()) && !p.equals(conjurador)) {

				// Cálculo de Distância (Chebyshev - Quadrado)
				int dist = Math.max(Math.abs(p.getPosX() - cx), Math.abs(p.getPosY() - cy));

				if (dist <= raio) {
					Efeito bencaoJustica = new Efeito("Bênção da Justiça", TipoEfeito.BUFF, 500, modsAliado, 0, 0);
					p.adicionarEfeito(bencaoJustica);
					p.recalcularAtributosEstatisticas();
					System.out.println(">>> " + p.getNome() + " recebeu a Bênção da Justiça!");
				}
			}
		}

		conjurador.recalcularAtributosEstatisticas();
	}

	public static void reverterParaPolaris(Personagem p) {
		System.out.println(">>> " + p.getNome() + " retorna ao Modo Polaris!");

		// Limpa Habilidades de Justiça
		p.limparHabilidadesExtras();

		// Adiciona os Solos Originais
		p.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.DistortedSolo());
		p.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo());
		p.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.PlainSolo());

		// Remove o Buff "Modo Justiça" (se ainda existir)
		p.removerEfeito("Modo Justiça");

		p.recalcularAtributosEstatisticas();
	}
}