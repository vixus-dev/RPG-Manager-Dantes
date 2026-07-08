package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.habilidades.RagnarokDoNorte;

import java.util.List;
import java.util.Map;

/**
 * Fantasma Nobre de Brunhilda — "Fimbulwinter".
 *
 * Custo: 2 mana, 50 TU. Cooldown: 1000 TU.
 *
 * Ao ativar, começa a preparar para que no próximo turno abra um domínio 7x7 por 600 TU.
 * Aliados no domínio: +50 de armadura, +25% de bônus de dano.
 * Inimigos no domínio: +20 TU após cada ação de Brunhilda, -20% de cura e 1 stack de Congelamento a cada 100 TU.
 * Quando faltarem 100 ou menos TU para o domínio acabar, Brunhilda ganha a habilidade temporária Ragnarok do Norte.
 */
public class Fimbulwinter extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Fimbulwinter";
	}

	@Override
	public String getDescricao() {
		return "Prepara por 1 turno. Abre um domínio 7x7 de gelo por 600 TU. "
				+ "Aliados no domínio ganham +50 armadura e +25% bônus de dano. "
				+ "Inimigos ganham +20 TU após cada ação do usuário, recebem -20% de cura e 1 stack de Congelamento a cada 100 TU. "
				+ "Quando restarem <=100 TU, concede a habilidade temporária Ragnarok do Norte.";
	}

	@Override
	public int getCustoMana() {
		return 2;
	}

	@Override
	public int getCustoTU() {
		return 50;
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
		return 7;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " prepara o FANTASMA NOBRE: FIMBULWINTER!");

		Efeito preparando = new Efeito("Fimbulwinter (Preparando)", TipoEfeito.BUFF, 9999, Map.of(), 0, 0);
		conjurador.adicionarEfeito(preparando);
		conjurador.recalcularAtributosEstatisticas();
	}

	@Override
	public void onTurnStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		Efeito fimbul = conjurador.getEfeitosAtivos().get("Fimbulwinter");
		if (fimbul != null) {
			int tuRestante = fimbul.getDuracaoTURestante();
			if (tuRestante <= 100) {
				boolean jaTem = conjurador.getHabilidadesExtras().stream()
						.anyMatch(h -> h.getNome().equalsIgnoreCase("Ragnarok do Norte"));
				if (!jaTem) {
					conjurador.adicionarHabilidadeExtra(new RagnarokDoNorte());
					System.out.println(">>> Ragnarok do Norte: Brunhilda recebe a habilidade temporária (" + tuRestante + " TU restantes de domínio).");
					if (manager != null && manager.getMainController() != null) {
						manager.getMainController().atualizarInterfaceTotal();
					}
				}
			}
		}
	}

	@Override
	public void onCriticalHit(Personagem conjurador, Personagem alvo, EstadoCombate estado, CombatManager manager) {
		if (conjurador != null && conjurador.getUltimaHabilidadeUsada() != null 
				&& "Ragnarok do Norte".equalsIgnoreCase(conjurador.getUltimaHabilidadeUsada().getNome())) {
			Efeito hemorragia = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Hemorragia", 0, 0);
			if (manager != null) {
				manager.getEffectProcessor().aplicarEfeito(alvo, hemorragia);
			} else {
				alvo.adicionarEfeito(hemorragia);
			}
			System.out.println(">>> Ragnarok do Norte: Dano Crítico! Hemorragia aplicada em " + alvo.getNome());
		}
	}

	public static void reverterFimbulwinter(Personagem p) {
		System.out.println(">>> Fimbulwinter: O domínio se dissipou.");
		p.removerHabilidadeExtraPorNome("Ragnarok do Norte");
		p.removerEfeito("Fimbulwinter");
		p.recalcularAtributosEstatisticas();
	}

	public static void aplicarCongelamento(Personagem alvo, CombatManager manager, int quantidade) {
		if (alvo == null) return;
		if (alvo.getEfeitosAtivos().containsKey("Congelado")) {
			return;
		}

		Efeito congelamento = alvo.getEfeitosAtivos().get("Congelamento");
		int stacks = quantidade;
		if (congelamento != null) {
			stacks = congelamento.getStacks() + quantidade;
		}

		// "cada stack aumenta o TU do alvo em +5 e reseta a duração"
		alvo.setContadorTU(alvo.getContadorTU() + (5 * quantidade));
		System.out.println(">>> Congelamento em " + alvo.getNome() + ": +" + (5 * quantidade) + " TU. Stacks atuais: " + stacks);

		if (stacks >= 5) {
			alvo.removerEfeito("Congelamento");
			Efeito congelado = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Congelado", 600, 0);
			if (manager != null) {
				manager.getEffectProcessor().aplicarEfeito(alvo, congelado);
			} else {
				alvo.adicionarEfeito(congelado);
			}
			System.out.println(">>> " + alvo.getNome() + " fica CONGELADO por 600 TU!");
		} else {
			if (congelamento == null) {
				congelamento = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Congelamento", 1000, 0);
				congelamento.setStacks(stacks);
				if (manager != null) {
					manager.getEffectProcessor().aplicarEfeito(alvo, congelamento);
				} else {
					alvo.adicionarEfeito(congelamento);
				}
			} else {
				congelamento.setStacks(stacks);
				congelamento.setDuracaoTURestante(1000); // Reseta a duração
			}
		}
		alvo.recalcularAtributosEstatisticas();
	}
}
