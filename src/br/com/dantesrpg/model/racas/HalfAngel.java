package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HalfAngel extends Raça {

	public HalfAngel() {
		this.maxStacks = 10;
		this.currentStacks = 0;
	}

	@Override
	public String getNome() {
		return "Half-Angel";
	}

	@Override
	public String getDescricaoPassiva() {
		if (isV2) {
			return "λογοσ: Benevolência sem limite. A cada 5 acúmulos, +20% dano. Transformado com >10 stacks gasta 2/turno.";
		}
		return "Ascensão: Golpes geram Benevolência (Críticos geram 2). Max 10. Acima de 10, gera Sobrecarga Angelical (+Dano).";
	}

	@Override
	public String getNomeV2() {
		return "λογοσ";
	}

	@Override
	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatController controller) {
		if (danoCausado > 0) {
			gerarAcumulo(personagem, 1);
		}
	}

	@Override
	public void onCriticalHit(Personagem personagem, Personagem alvo, EstadoCombate estado) {
		gerarAcumulo(personagem, 1);
	}

	private void gerarAcumulo(Personagem p, int quantidade) {
		for (int i = 0; i < quantidade; i++) {
			if (isV2) {
				// V2 (λογοσ): Sem cap de stacks
				this.currentStacks++;
				System.out.println(">>> λογοσ: +1 Benevolência. Total: " + this.currentStacks);

				if (this.currentStacks == 5) {
					System.out.println(">>> λογοσ: Ascensão Desbloqueada!");
				}

				// A cada 5 acúmulos, atualiza o bônus de dano
				if (this.currentStacks % 5 == 0) {
					aplicarBonusDanoLogos(p);
				}
			} else {
				if (this.currentStacks < this.maxStacks) {
					this.currentStacks++;
					System.out.println(
							">>> Half-Angel: +1 Benevolência. Total: " + this.currentStacks + "/" + this.maxStacks);

					if (this.currentStacks == 5) {
						System.out.println(">>> Half-Angel: Ascensão Desbloqueada (5/10)!");
					}
				} else {
					aplicarSobrecargaAngelical(p);
				}
			}
		}
	}

	private void aplicarBonusDanoLogos(Personagem p) {
		int blocosDe5 = this.currentStacks / 5;
		double bonusTotal = blocosDe5 * 0.20;

		String nomeEfeito = "Logos - Poder Divino";
		Efeito existente = p.getEfeitosAtivos().get(nomeEfeito);

		if (existente != null) {
			existente.getModificadores().put("DANO_BONUS_PERCENTUAL", bonusTotal);
			existente.setStacks(blocosDe5);
			System.out.println(">>> λογοσ: Poder Divino atualizado! +" + (int) (bonusTotal * 100) + "% Dano (" + blocosDe5 + " blocos).");
		} else {
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", bonusTotal);
			Efeito novoEfeito = new Efeito(nomeEfeito, TipoEfeito.BUFF, 99999, mods, 0, 0);
			novoEfeito.setStacks(blocosDe5);
			p.adicionarEfeito(novoEfeito);
			System.out.println(">>> λογοσ: Poder Divino ativado! +" + (int) (bonusTotal * 100) + "% Dano.");
		}

		p.recalcularAtributosEstatisticas();
	}

	private void aplicarSobrecargaAngelical(Personagem p) {
		String nomeEfeito = "Sobrecarga Angelical";
		Efeito efeitoExistente = p.getEfeitosAtivos().get(nomeEfeito);
		int duracaoBase = 300;
		double bonusPorStack = 0.05; // 5%

		if (efeitoExistente != null) {
			// Atualiza existente
			int novosStacks = efeitoExistente.getStacks() + 1;
			efeitoExistente.setStacks(novosStacks);
			efeitoExistente.setDuracaoTURestante(duracaoBase); // Reseta duração

			// Atualiza o modificador
			double novoBonus = novosStacks * bonusPorStack;
			efeitoExistente.getModificadores().put("DANO_BONUS_PERCENTUAL", novoBonus);

			System.out.println(">>> SOBRECARGA ANGELICAL! Acúmulos: " + novosStacks + " (Bônus: +"
					+ (int) (novoBonus * 100) + "%)");
		} else {
			// Cria novo
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", bonusPorStack); // Começa com 5%

			Efeito novoEfeito = new Efeito(nomeEfeito, TipoEfeito.BUFF, duracaoBase, mods, 0, 0);
			novoEfeito.setStacks(1);

			p.adicionarEfeito(novoEfeito);
			System.out.println(">>> SOBRECARGA ANGELICAL INICIADA! (+5% Dano)");
		}

		p.recalcularAtributosEstatisticas();
	}

	@Override
	public void onTurnStart(Personagem personagem, EstadoCombate estado) {
		// Lógica de manutenção da forma Ascendida
		if (this.isTransformed) {
			// V2 (λογοσ): Gasta 2 stacks/turno quando acima de 10
			int custo = (isV2 && this.currentStacks > 10) ? 2 : 1;
			this.currentStacks -= custo;

			System.out.println(">>> Half-Angel (Ascendido): -" + custo + " Benevolência (Manutenção). Restam: "
					+ this.currentStacks);

			if (this.currentStacks <= 0) {
				this.currentStacks = 0;
				sairAscensao(personagem);
			}
		}
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		br.com.dantesrpg.model.habilidades.raciais.Ascensao asc = new br.com.dantesrpg.model.habilidades.raciais.Ascensao();
		if (isV2) {
			asc.setDescricao("Consome 5 de Benevolência para ascender (λογοσ). Stacks sem cap, +20% dano a cada 5 acúmulos. Gasta 2/turno acima de 10.");
		}
		return java.util.Arrays.asList(asc);
	}
	
	public void ativarAscensaoManual(Personagem p) {
		this.isTransformed = true;
		System.out.println(">>> " + p.getNome() + " ativou a ASCENSÃO MANUALMENTE!");

		int strBonus = (int) (p.getAtributosFinais().get(Atributo.FORCA) * 0.20);
		int sagBonus = (int) (p.getAtributosFinais().get(Atributo.SAGACIDADE) * 0.20);
		int insBonus = (int) (p.getAtributosFinais().get(Atributo.INSPIRACAO) * 0.20);

		Map<String, Double> mods = new HashMap<>();
		mods.put("FORCA", (double) strBonus);
		mods.put("SAGACIDADE", (double) sagBonus);
		mods.put("INSPIRACAO", (double) insBonus);
		mods.put("TAXA_CRITICA", 0.10);

		// Aplica o buff de transformação
		Efeito ascensao = new Efeito("Ascensão", TipoEfeito.BUFF, 9999, mods, 0, 0);
		p.adicionarEfeito(ascensao);
		p.recalcularAtributosEstatisticas();
	}

	private void sairAscensao(Personagem p) {
		this.isTransformed = false;
		System.out.println(">>> " + p.getNome() + " perdeu a Ascensão.");
		p.removerEfeito("Ascensão");
		p.recalcularAtributosEstatisticas();
	}
}