package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.boss.*;

import java.util.List;
import java.util.Map;

/**
 * Fantasma Nobre do Justiceiro Cego — "And Justice.. FOR MY SELF!"
 *
 * Custo: 4 mana, 100 TU. Cooldown: 100 TU após o fim da aura.
 *
 * Ao ativar: cobre o mapa em 25x25 com aura de "Falsa Justiça" (1200 TU).
 * Overlay de tiles com 0% de transparência (chão muda temporariamente).
 *
 * Efeitos enquanto ativa:
 * - Dano causado ao boss é refletido como % da vida máxima do atacante
 *   (implementado em DamageApplicator via efeito "Falsa Justiça")
 * - Troca as 4 habilidades do boss:
 *   Sanctum/Purus/Locus/MEAT... → Sentinela/HideAndBuild/PoisonWasLethal/SitInBalance
 *
 * Ao expirar: reverte habilidades e aplica cooldown.
 * A ativação é delayed (padrão "Preparando" via AuraManager.checarEfeitosDeInicioDeTurno).
 */
public class AndJusticeForMySelf extends FantasmaNobre {

	@Override
	public String getNome() {
		return "And Justice.. FOR MY SELF!";
	}

	@Override
	public String getDescricao() {
		return "Cobre o mapa em 25x25 com uma aura de falsa justiça (1200 TU). "
				+ "Dano sofrido é refletido proporcionalmente. "
				+ "Troca habilidades para modo de névoa.";
	}

	@Override
	public int getCustoMana() {
		return 4;
	}

	@Override
	public int getCustoTU() {
		return 100;
	}

	@Override
	public int getCooldownTU() {
		return 1400; // 1200 TU de duração + 100 TU pós-aura + margem
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 25;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome()
				+ " prepara: AND JUSTICE.. FOR MY SELF!");

		// Efeito preparando — ativação no próximo turno via AuraManager
		Efeito preparando = new Efeito("Falsa Justiça (Preparando)",
				TipoEfeito.BUFF, 9999, Map.of(), 0, 0);
		conjurador.adicionarEfeito(preparando);
		conjurador.recalcularAtributosEstatisticas();
	}

	/**
	 * Chamado pelo AuraManager quando o efeito "(Preparando)" é detectado
	 * no início do turno. Ativa de fato a aura e troca as habilidades.
	 */
	public static void ativarAura(Personagem conjurador, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> AND JUSTICE.. FOR MY SELF! ATIVADA!");

		// Marcador de duração da aura (1200 TU)
		Efeito aura = new Efeito("Falsa Justiça", TipoEfeito.BUFF, 1200, Map.of(), 0, 0);
		manager.getEffectProcessor().aplicarEfeito(conjurador, aura);

		// Troca habilidades para modo de névoa
		conjurador.limparHabilidadesExtras();
		conjurador.adicionarHabilidadeExtra(new SentinelaSkill());
		conjurador.adicionarHabilidadeExtra(new HideAndBuild());
		conjurador.adicionarHabilidadeExtra(new PoisonWasLethal());
		conjurador.adicionarHabilidadeExtra(new SitInBalance());

		System.out.println(">>> Habilidades trocadas para modo Falsa Justiça!");

		// Cria o domínio 25x25 com overlay de tile
		if (manager.getMainController() != null) {
			br.com.dantesrpg.model.map.Dominio dominio = new br.com.dantesrpg.model.map.Dominio(
					"falsa_justica", "Falsa Justiça", conjurador,
					conjurador.getPosX(), conjurador.getPosY(), 25,
					"zona-dominio-justiceiro");
			dominio.setTexturePath("/effects/falsa_justica.png");
			dominio.setOverlayOpacity(1.0); // 0% transparente
			manager.getDomainManager().ativarDominioNoMapa(dominio, conjurador, estado);
		}

		conjurador.recalcularAtributosEstatisticas();
	}

	/**
	 * Chamado quando o efeito "Falsa Justiça" expira.
	 * Reverte as habilidades originais e remove o domínio.
	 */
	public static void reverterHabilidades(Personagem conjurador) {
		System.out.println(">>> " + conjurador.getNome()
				+ " — Falsa Justiça se dissipou! Habilidades revertidas.");

		conjurador.limparHabilidadesExtras();
		// As habilidades originais voltam via weapon grants (habilidadesConcedidas)
		// O sistema de combate repopula automaticamente ao recalcular

		conjurador.removerEfeito("Falsa Justiça");
		conjurador.recalcularAtributosEstatisticas();
	}
}
