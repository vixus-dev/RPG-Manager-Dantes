package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnjoCaido extends Raça {

	private double bonusDanoProximoAtaque = 0.0;

	public AnjoCaido() {
		this.maxStacks = 5;
		this.currentStacks = 0;
	}

	@Override
	public String getNome() {
		return "Anjo-Caido";
	}

	@Override
	public String getDescricaoPassiva() {
		if (isV2) {
			return "Παραγγελία: Equilibrium + Harmonia (2 ao sofrer, 1 ao atacar). Com 5, transforma em Τέλεια αρμονία: ataques 360° + cura + voo + armadura.";
		}
		return "Equilibrium: Devolve dano sofrido baseado no nível.";
	}

	@Override
	public String getNomeV2() {
		return "Παραγγελία";
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		if (isV2) {
			List<Habilidade> abilities = new ArrayList<>();
			abilities.add(new br.com.dantesrpg.model.habilidades.raciais.HarmoniaPerfeita());
			return abilities;
		}
		return Collections.emptyList();
	}

	@Override
	public void onDamageTaken(Personagem personagem, Personagem atacante, double danoRecebido, EstadoCombate estado,
			CombatManager manager) {
		// Equilibrium (V1 e V2)
		if (atacante != null && atacante != personagem && danoRecebido > 0) {
			int nivel = personagem.getNivel();
			double porcentagemReflexao = 0.70 + ((nivel - 1) * 0.20);
			int danoRefletido = (int) (danoRecebido * porcentagemReflexao);

			if (danoRefletido > 0) {
				System.out.println(">>> EQUILIBRIUM (" + (int) (porcentagemReflexao * 100) + "%): "
						+ personagem.getNome() + " reflete " + danoRefletido + " de dano em " + atacante.getNome()
						+ "!");
				manager.aplicarDanoAoAlvo(personagem, atacante, danoRefletido, true, TipoAcao.REACAO_FANTASMA, estado);
			}

			// V2: +2 stacks de Harmonia ao sofrer dano
			if (isV2 && !this.isTransformed) {
				this.currentStacks = Math.min(this.currentStacks + 2, this.maxStacks);
				System.out.println(">>> ΠΑΡΑΓΓΕΛΙΑ: +2 Harmonia (dano recebido). Total: " + this.currentStacks + "/"
						+ this.maxStacks);
			}
		}
	}

	@Override
	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatController controller) {
		if (!isV2 || danoCausado <= 0)
			return;

		// +1 stack de Harmonia ao causar dano (fora da transformação)
		if (!this.isTransformed) {
			this.currentStacks = Math.min(this.currentStacks + 1, this.maxStacks);
			System.out.println(">>> ΠΑΡΑΓΓΕΛΙΑ: +1 Harmonia (dano causado). Total: " + this.currentStacks + "/"
					+ this.maxStacks);
			return;
		}

		// Transformado: ataque 360° ao redor (raio 2)
		if (alvo == null || controller == null || estado == null)
			return;

		int raio = 2;
		int cx = personagem.getPosX();
		int cy = personagem.getPosY();
		String faccao = personagem.getFaccao();
		int inimigosAtingidos = 0;
		double danoArea = danoCausado * 0.50; // 50% do dano causado como dano em área

		// Aplica bônus acumulado do último ataque em área
		double multiplicadorBonus = 1.0 + this.bonusDanoProximoAtaque;
		double danoFinalArea = danoArea * multiplicadorBonus;

		CombatManager manager = controller.getCombatManager();

		for (Personagem combatente : estado.getCombatentes()) {
			if (!combatente.isAtivoNoCombate() || combatente == personagem || combatente == alvo)
				continue;
			if (combatente.getFaccao().equals(faccao))
				continue;

			int dist = Math.max(Math.abs(combatente.getPosX() - cx), Math.abs(combatente.getPosY() - cy));
			if (dist <= raio) {
				manager.aplicarDanoAoAlvo(personagem, combatente, (int) danoFinalArea, false,
						TipoAcao.REACAO_FANTASMA, estado);
				inimigosAtingidos++;
				System.out.println(">>> ΤΕΛΕΙΑ ΑΡΜΟΝΙΑ: Ataque 360° acerta " + combatente.getNome() + " por "
						+ (int) danoFinalArea + "!");
			}
		}

		// Acumula bônus para o PRÓXIMO ataque: +5% por inimigo atingido
		this.bonusDanoProximoAtaque = inimigosAtingidos * 0.05;

		// Cura: 0.1% do HP máximo por inimigo atingido
		if (inimigosAtingidos > 0) {
			double cura = personagem.getVidaMaxima() * 0.001 * inimigosAtingidos;
			personagem.setVidaAtual(personagem.getVidaAtual() + cura, estado, controller);
			System.out.println(">>> ΤΕΛΕΙΑ ΑΡΜΟΝΙΑ: Cura +" + String.format("%.1f", cura) + " HP (" + inimigosAtingidos
					+ " inimigos).");

			if (this.bonusDanoProximoAtaque > 0) {
				System.out.println(">>> ΤΕΛΕΙΑ ΑΡΜΟΝΙΑ: Próximo ataque terá +"
						+ (int) (this.bonusDanoProximoAtaque * 100) + "% dano extra.");
			}
		}
	}

	public void ativarHarmoniaPerfeita(Personagem p) {
		if (this.currentStacks < 5)
			return;

		this.isTransformed = true;
		this.bonusDanoProximoAtaque = 0.0;
		System.out.println(">>> " + p.getNome() + " ativou ΤΕΛΕΙΑ ΑΡΜΟΝΙΑ (Harmonia Perfeita)!");

		// +20% Armadura via efeito
		Map<String, Double> mods = new HashMap<>();
		mods.put("ARMADURA_BONUS_PERCENTUAL", 0.20);

		Efeito harmonia = new Efeito("Τέλεια αρμονία", TipoEfeito.BUFF, 99999, mods, 0, 0);
		p.adicionarEfeito(harmonia);

		// Aplica Voo
		Efeito voo = new Efeito("Voo", TipoEfeito.BUFF, 99999, null, 0, 0);
		p.adicionarEfeito(voo);

		// Aplica Lento (debuff permanente durante transformação)
		Efeito lento = new Efeito("Lento", TipoEfeito.DEBUFF, 99999, null, 0, 0);
		p.adicionarEfeito(lento);

		p.recalcularAtributosEstatisticas();
	}

	private void sairHarmoniaPerfeita(Personagem p) {
		this.isTransformed = false;
		this.bonusDanoProximoAtaque = 0.0;
		System.out.println(">>> " + p.getNome() + " perdeu a Τέλεια αρμονία.");

		p.removerEfeito("Τέλεια αρμονία");
		p.removerEfeito("Voo");
		p.removerEfeito("Lento");
		p.recalcularAtributosEstatisticas();
	}

	@Override
	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		if (!isV2 || !this.isTransformed)
			return;

		// Manutenção: -1 stack a cada 100TU
		int tempoGlobal = estado.getTempoGlobalCombate();
		if (tempoGlobal % 100 == 0 && tempoGlobal > 0) {
			this.currentStacks--;
			System.out.println(">>> ΤΕΛΕΙΑ ΑΡΜΟΝΙΑ: -1 Harmonia (manutenção). Restam: " + this.currentStacks);

			if (this.currentStacks <= 0) {
				this.currentStacks = 0;
				sairHarmoniaPerfeita(personagem);
			}
		}
	}

	@Override
	public double getBonusArmaduraPercentual(Personagem personagem) {
		if (isV2 && this.isTransformed) {
			return 0.20;
		}
		return 0.0;
	}
}
