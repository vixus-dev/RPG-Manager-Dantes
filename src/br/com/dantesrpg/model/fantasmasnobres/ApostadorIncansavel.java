package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ApostadorIncansavel extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Idle Death Gamble";
	}

	@Override
	public String getDescricao() {
		return "Expansão de Domínio (6x6). Entra no modo de Aposta. Causa -75% dano, mas custa -50% TU até conseguir o JACKPOT.";
	}

	@Override
	public int getCustoMana() {
		return 6;
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
	} // 6x6

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " começa a expandir o DOMÍNIO: Idle Death Gamble!");

		// 1. Efeito de Preparação (Ativa no próximo turno, igual ao Alexei)
		Efeito efeitoPreparando = new Efeito("Domínio: Idle Death Gamble (Preparando)", TipoEfeito.BUFF, 9999, Map.of(),
				0, 0);

		conjurador.adicionarEfeito(efeitoPreparando);
		conjurador.recalcularAtributosEstatisticas();
	}

	// --- MÉTODOS AUXILIARES PARA A LÓGICA DE APOSTA ---

	/**
	 * Chamado quando o Domínio se ativa realmente (no início do turno). Aplica os
	 * debuffs iniciais (-75% dano, -25% TU).
	 */
	public static void ativarEfeitoDominio(Personagem p) {
		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", -0.50); // Reduz dano em 75%
		Efeito dominioAtivo = new Efeito("Domínio: Idle Death Gamble", TipoEfeito.BUFF, 300, // Duração base
				mods, 0, 0);
		// Inicializa contador de estrelas nos stacks do efeito
		dominioAtivo.setStacks(0);

		p.adicionarEfeito(dominioAtivo);
	}

	public static void ativarJackpot(Personagem p) {
		System.out.println(">>> " + p.getNome() + " TIROU O JACKPOT!!!!");
		p.removerEfeito("Domínio: Idle Death Gamble"); // Remove os debuffs

		Map<String, Double> mods = new HashMap<>();
		mods.put("MOVIMENTO", 3.0);
		// Regeneração de Mana e Vida será no avançarTempo

		Efeito jackpot = new Efeito("JACKPOT!", TipoEfeito.BUFF, 411, mods, 0, 0);
		p.adicionarEfeito(jackpot);
	}

	public static void processarAposta(Personagem lyria, int d1, int d2, int d3, EstadoCombate estado) {
		if (d1 <= 0 || d2 <= 0 || d3 <= 0)
			return; // Dados inválidos
		System.out.println(">>> APOSTA DA LYRIA: [" + d1 + "][" + d2 + "][" + d3 + "]");

		// Verifica Estrelas (Pity)
		Efeito efeitoEstrelas = lyria.getEfeitosAtivos().get("Estrelas da Sorte");
		int estrelasAtuais = (efeitoEstrelas != null) ? efeitoEstrelas.getStacks() : 0;
		boolean jackpotGarantido = (estrelasAtuais >= 6);
		boolean trinca = (d1 == d2 && d2 == d3);
		if (jackpotGarantido || trinca) {
			ativarJackpot(lyria);
			if (efeitoEstrelas != null)
				lyria.removerEfeito("Estrelas da Sorte"); // Reseta estrelas
		} else {
			int somaDados = d1 + d2 + d3;
			System.out.println(">>> Aposta Falhou. Redução de TU no próximo ataque: -" + somaDados);
			lyria.setContadorTU(lyria.getContadorTU() - somaDados);
			System.out.println(">>> Lyria recuperou " + somaDados + " TU pela aposta.");

			// Adiciona Estrela
			if (efeitoEstrelas == null) {
				efeitoEstrelas = new Efeito("Estrelas da Sorte", TipoEfeito.BUFF, 9999, Map.of(), 0, 0);
				efeitoEstrelas.setStacks(1);
				lyria.adicionarEfeito(efeitoEstrelas);
			} else {
				efeitoEstrelas.setStacks(efeitoEstrelas.getStacks() + 1);
			}
			System.out.println(">>> Lyria ganhou 1 Estrela da Sorte (Total: " + efeitoEstrelas.getStacks() + ")");
			lyria.recalcularAtributosEstatisticas();
		}
	}
}